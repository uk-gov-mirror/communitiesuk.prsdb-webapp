#!/usr/bin/env node
import assert from 'node:assert/strict';
import { writeFile, readFile } from 'node:fs/promises';
import { pathToFileURL } from 'node:url';

export function syntheticRegistrationCertificate() {
  const content = 'BT /F1 14 Tf 50 780 Td (SYNTHETIC LOCAL PERFORMANCE CERTIFICATE) Tj 0 -25 Td (Not valid for any real property or compliance purpose.) Tj ET\n';
  const objects = [
    '<< /Type /Catalog /Pages 2 0 R >>',
    '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
    '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>',
    '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>',
    `<< /Length ${Buffer.byteLength(content)} >>\nstream\n${content}endstream`,
  ];
  let document = '%PDF-1.4\n';
  const offsets = [0];
  for (const [index, object] of objects.entries()) {
    offsets.push(Buffer.byteLength(document));
    document += `${index + 1} 0 obj\n${object}\nendobj\n`;
  }
  const xref = Buffer.byteLength(document);
  document += `xref\n0 ${objects.length + 1}\n0000000000 65535 f \n`;
  document += offsets.slice(1).map(offset => `${String(offset).padStart(10, '0')} 00000 n \n`).join('');
  document += `trailer\n<< /Size ${objects.length + 1} /Root 1 0 R >>\nstartxref\n${xref}\n%%EOF\n`;
  return Buffer.from(document);
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const certificate = syntheticRegistrationCertificate();
  const document = certificate.toString();
  const xref = Number(document.match(/startxref\n(\d+)/)[1]);
  assert.equal(document.substring(xref, xref + 4), 'xref');
  for (const match of document.matchAll(/^(\d{10}) 00000 n $/gm)) {
    assert.match(document.substring(Number(match[1])), /^\d 0 obj\n/);
  }
  if (process.argv[2] === '--self-test') console.log('Synthetic PDF object offsets and xref verified');
  else {
    assert.equal(process.argv.length, 3, 'Provide one explicit output PDF path');
    assert.ok(process.argv[2].endsWith('.pdf'));
    await writeFile(process.argv[2], certificate, { flag: 'wx' });
    assert.deepEqual(await readFile(process.argv[2]), certificate);
    console.log(`Created genuine synthetic PDF: ${certificate.length} bytes`);
  }
}
