import assert from 'node:assert/strict';
import {mkdtemp, readFile, rm, stat, writeFile, symlink, mkdir} from 'node:fs/promises';
import {tmpdir} from 'node:os';
import {join} from 'node:path';
import {test} from 'node:test';
import * as preparation from '../../../scripts/performance-tests/local-seed-files.mjs';

const fixtures = [{
  subject: 'synthetic-owner', propertyOwnershipId: 100, address: '1 Fixture Road',
  files: [
    {objectKey: 'certificateUpload.seed-100.gas-safety-certificate-file-upload.member-120', extension: 'png'},
    {objectKey: 'certificateUpload.seed-100.eic-file-upload.member-120', extension: 'pdf'},
  ],
}];

test('prepares actual synthetic PNG and PDF bodies only in explicitly isolated storage', async () => {
  assert.equal(typeof preparation.prepareLocalSeedFiles, 'function');
  const root = await mkdtemp(join(tmpdir(), 'prsdb-seed-fixture-'));
  try {
    assert.equal(await preparation.prepareLocalSeedFiles(fixtures, root), 2);
    for (const file of fixtures[0].files) {
      const path = join(root, '.local-uploads', 'safe', file.objectKey);
      const body = await readFile(path);
      if (process.platform !== 'win32') assert.equal((await stat(path)).mode & 0o777, 0o600);
      if (file.extension === 'png') {
        assert.deepEqual([...body.subarray(0, 8)], [137, 80, 78, 71, 13, 10, 26, 10]);
        assert.match(body.toString('latin1'), /SYNTHETIC PERFORMANCE TEST/);
      } else {
        assert.match(body.toString(), /^%PDF-1\.4/);
        assert.match(body.toString(), /SYNTHETIC PERFORMANCE TEST/);
        assert.match(body.toString(), /startxref\n\d+\n%%EOF/);
      }
    }
    assert.equal(await preparation.prepareLocalSeedFiles(fixtures, root), 2);
  } finally {
    await rm(root, {recursive: true});
  }
});

for (const [description, invalid] of [
  ['non-seed key', [{...fixtures[0], files: [{objectKey: 'private-key', extension: 'png'}]}]],
  ['path traversal', [{...fixtures[0], files: [{objectKey: '../escape', extension: 'png'}]}]],
  ['unsupported extension', [{...fixtures[0], files: [{...fixtures[0].files[0], extension: 'exe'}]}]],
  ['unbounded actor pool', Array.from({length: 102}, () => fixtures[0])],
  ['shared actor', [fixtures[0], {...fixtures[0], propertyOwnershipId: 101}]],
]) {
  test(`rejects ${description} before creating any file`, async () => {
    assert.equal(typeof preparation.prepareLocalSeedFiles, 'function');
    let writes = 0;
    await assert.rejects(preparation.prepareLocalSeedFiles(invalid, tmpdir(), {
      mkdir: async () => { writes++; }, writeFile: async () => { writes++; },
    }));
    assert.equal(writes, 0);
  });
}

test('rejects repository and non-absolute storage locations', async () => {
  assert.equal(typeof preparation.prepareLocalSeedFiles, 'function');
  for (const root of [process.cwd(), '.', '/', join(process.cwd(), 'uploads')]) {
    await assert.rejects(preparation.prepareLocalSeedFiles(fixtures, root), /isolated|absolute/);
  }
});

test('preserves different existing objects and rejects symlinked storage', async () => {
  const root = await mkdtemp(join(tmpdir(), 'prsdb-seed-fixture-'));
  const external = await mkdtemp(join(tmpdir(), 'prsdb-seed-target-'));
  try {
    await mkdir(join(root, '.local-uploads'));
    if (process.platform !== 'win32') {
      await symlink(external, join(root, '.local-uploads', 'safe'));
      await assert.rejects(preparation.prepareLocalSeedFiles(fixtures, root), /symlinks/);
      await rm(join(root, '.local-uploads', 'safe'));
    }
    await mkdir(join(root, '.local-uploads', 'safe'));
    const path = join(root, '.local-uploads', 'safe', fixtures[0].files[0].objectKey);
    await writeFile(path, 'existing object');
    await assert.rejects(preparation.prepareLocalSeedFiles(fixtures, root), /overwrite/);
    assert.equal(await readFile(path, 'utf8'), 'existing object');
    assert.deepEqual(await import('node:fs/promises').then(fs => fs.readdir(external)), []);
  } finally {
    await rm(root, {recursive: true});
    await rm(external, {recursive: true});
  }
});
