import {readFile, writeFile, mkdir, realpath, lstat} from 'node:fs/promises';
import {isAbsolute, relative, resolve, join, dirname} from 'node:path';
import {fileURLToPath, pathToFileURL} from 'node:url';
import {deflateSync} from 'node:zlib';

const repository = resolve(dirname(fileURLToPath(import.meta.url)), '../..');
const requireCondition = (condition, message) => { if (!condition) throw new Error(message); };
const contained = (parent, child) => {
  const path = relative(parent, child);
  return path === '' || (!path.startsWith('..') && !isAbsolute(path));
};

function crc32(bytes) {
  let crc = 0xffffffff;
  for (const byte of bytes) {
    crc ^= byte;
    for (let bit = 0; bit < 8; bit++) crc = (crc >>> 1) ^ ((crc & 1) ? 0xedb88320 : 0);
  }
  return (crc ^ 0xffffffff) >>> 0;
}

function pngChunk(type, body) {
  const data = Buffer.concat([Buffer.from(type), body]);
  const size = Buffer.alloc(4);
  size.writeUInt32BE(body.length);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(data));
  return Buffer.concat([size, data, crc]);
}

function syntheticPng() {
  const header = Buffer.alloc(13);
  header.writeUInt32BE(64, 0);
  header.writeUInt32BE(64, 4);
  header[8] = 8;
  header[9] = 2;
  const pixels = Buffer.alloc(64 * (1 + 64 * 3), 255);
  for (let row = 0; row < 64; row++) {
    pixels[row * 193] = 0;
    for (let column = 0; column < 64; column++) {
      if (row === column || row + column === 63) {
        const offset = row * 193 + 1 + column * 3;
        pixels[offset] = 220;
        pixels[offset + 1] = 0;
        pixels[offset + 2] = 0;
      }
    }
  }
  return Buffer.concat([
    Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
    pngChunk('IHDR', header),
    pngChunk('tEXt', Buffer.from('Description\0SYNTHETIC PERFORMANCE TEST - NOT A VALID CERTIFICATE')),
    pngChunk('IDAT', deflateSync(pixels)),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

function syntheticPdf() {
  const stream = 'BT /F1 14 Tf 50 750 Td (SYNTHETIC PERFORMANCE TEST - NOT A VALID CERTIFICATE) Tj ET';
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    `<< /Length ${Buffer.byteLength(stream)} >>\nstream\n${stream}\nendstream`,
  ];
  let pdf = '%PDF-1.4\n';
  const offsets = [];
  objects.forEach((body, index) => {
    offsets.push(Buffer.byteLength(pdf));
    pdf += `${index + 1} 0 obj\n${body}\nendobj\n`;
  });
  const xref = Buffer.byteLength(pdf);
  pdf += `xref\n0 6\n0000000000 65535 f \n${offsets.map(offset => `${String(offset).padStart(10, '0')} 00000 n \n`).join('')}`;
  pdf += `trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(pdf);
}

export async function prepareLocalSeedFiles(fixtures, storageRoot, fs = {readFile, writeFile, mkdir, realpath, lstat}) {
  requireCondition(isAbsolute(storageRoot), 'An absolute isolated storage root is required');
  const root = resolve(storageRoot);
  requireCondition(!contained(repository, root) && !contained(root, repository),
    'Storage must be isolated from the repository and its parent directories');
  requireCondition(Array.isArray(fixtures) && fixtures.length > 0 && fixtures.length <= 101, 'A bounded property fixture pool is required');
  const subjects = new Set();
  const properties = new Set();
  const files = new Map();
  for (const fixture of fixtures) {
    requireCondition(typeof fixture?.subject === 'string' && fixture.subject.trim() &&
      Number.isSafeInteger(fixture.propertyOwnershipId) && fixture.propertyOwnershipId > 0 &&
      !subjects.has(fixture.subject) && !properties.has(fixture.propertyOwnershipId) &&
      Array.isArray(fixture.files) && fixture.files.length <= 20, 'Invalid or shared property fixture');
    subjects.add(fixture.subject);
    properties.add(fixture.propertyOwnershipId);
    for (const file of fixture.files) {
      requireCondition(/^certificateUpload\.seed-[0-9]+\.[a-z-]+\.member-[0-9]+$/.test(file?.objectKey) &&
        ['png', 'pdf'].includes(file.extension), 'Only seed-owned PNG/PDF certificate fixtures are supported');
      requireCondition(!files.has(file.objectKey) || files.get(file.objectKey) === file.extension, 'Conflicting file fixture extensions');
      files.set(file.objectKey, file.extension);
    }
  }
  const canonical = await (fs.realpath ?? realpath)(root);
  requireCondition(!contained(repository, canonical) && !contained(canonical, repository), 'Storage must remain isolated after resolving symlinks');
  const directory = join(canonical, '.local-uploads', 'safe');
  for (const path of [join(canonical, '.local-uploads'), directory, ...[...files.keys()].map(key => join(directory, key))]) {
    try {
      requireCondition(!(await (fs.lstat ?? lstat)(path)).isSymbolicLink(), 'Fixture storage must not contain symlinks');
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }
  const bodies = {png: syntheticPng(), pdf: syntheticPdf()};
  for (const [key, extension] of files) {
    try {
      requireCondition((await (fs.readFile ?? readFile)(join(directory, key))).equals(bodies[extension]),
        'Refusing to overwrite an existing different file');
    } catch (error) {
      if (error.code !== 'ENOENT') throw error;
    }
  }
  await fs.mkdir(directory, {recursive: true, mode: 0o700});
  for (const [key, extension] of files) {
    try {
      await fs.writeFile(join(directory, key), bodies[extension], {mode: 0o600, flag: 'wx'});
    } catch (error) {
      if (error.code !== 'EEXIST') throw error;
      requireCondition((await (fs.readFile ?? readFile)(join(directory, key))).equals(bodies[extension]),
        'Refusing to overwrite an existing different file');
    }
  }
  return files.size;
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  try {
    requireCondition(process.argv.length === 4, 'Usage: node local-seed-files.mjs <property-fixtures.json> <isolated-storage-root>');
    const fixtures = JSON.parse(await readFile(process.argv[2], 'utf8'));
    console.log(`Prepared ${await prepareLocalSeedFiles(fixtures, process.argv[3])} synthetic certificate files; database unchanged.`);
  } catch (error) {
    console.error(error.message);
    process.exitCode = 1;
  }
}
