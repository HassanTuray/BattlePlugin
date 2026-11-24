// Zips a resource pack folder (pack.mcmeta at its top) into a .zip players can be sent. Needs only node.
// Usage (from the project folder): node models/tools/zip_pack.js resourcepack/mistfog resourcepack/MistFog.zip
// Files are stored, not compressed, like make_chaos_pack.js; paths in the zip always use forward slashes.
const fs = require('fs'), path = require('path');

function crcTable() { const t = []; for (let n = 0; n < 256; n++) { let c = n; for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1; t[n] = c >>> 0; } return t; }
const T = crcTable();
function crc(buf) { let c = 0xffffffff; for (const b of buf) c = T[(c ^ b) & 255] ^ (c >>> 8); return (c ^ 0xffffffff) >>> 0; }

function listFiles(dir, base = dir) {
  return fs.readdirSync(dir, { withFileTypes: true }).flatMap((e) => {
    const full = path.join(dir, e.name);
    return e.isDirectory() ? listFiles(full, base) : [[path.relative(base, full).split(path.sep).join('/'), full]];
  }).sort(([a], [b]) => (a < b ? -1 : 1));
}

const [, , folder, out] = process.argv;
if (!folder || !out) { console.error('usage: node zip_pack.js <pack folder> <out.zip>'); process.exit(1); }
if (!fs.existsSync(path.join(folder, 'pack.mcmeta'))) { console.error('no pack.mcmeta in ' + folder); process.exit(2); }

const files = listFiles(folder);
const parts = [], central = []; let off = 0;
for (const [name, full] of files) {
  // normalise line endings, so the zip (and its hash) is the same on every computer
  let data = fs.readFileSync(full);
  if (/\.(glsl|fsh|vsh|json|mcmeta)$/.test(name)) data = Buffer.from(data.toString('utf8').replace(/\r\n/g, '\n'));
  const n = Buffer.from(name), c = crc(data);
  const h = Buffer.alloc(30); h.writeUInt32LE(0x04034b50, 0); h.writeUInt16LE(20, 4); h.writeUInt16LE(0x0800, 6);
  h.writeUInt32LE(0x21, 10); h.writeUInt32LE(c, 14); h.writeUInt32LE(data.length, 18); h.writeUInt32LE(data.length, 22); h.writeUInt16LE(n.length, 26);
  parts.push(h, n, data);
  const e = Buffer.alloc(46); e.writeUInt32LE(0x02014b50, 0); e.writeUInt16LE(20, 4); e.writeUInt16LE(20, 6); e.writeUInt16LE(0x0800, 8);
  e.writeUInt32LE(0x21, 12); e.writeUInt32LE(c, 16); e.writeUInt32LE(data.length, 20); e.writeUInt32LE(data.length, 24); e.writeUInt16LE(n.length, 28); e.writeUInt32LE(off, 42);
  central.push(e, n); off += 30 + n.length + data.length;
}
const cd = Buffer.concat(central), end = Buffer.alloc(22);
end.writeUInt32LE(0x06054b50, 0); end.writeUInt16LE(files.length, 8); end.writeUInt16LE(files.length, 10);
end.writeUInt32LE(cd.length, 12); end.writeUInt32LE(off, 16);
fs.writeFileSync(out, Buffer.concat([...parts, cd, end]));
console.log(`wrote ${out}: ${files.map(([n]) => n).join(', ')}`);
