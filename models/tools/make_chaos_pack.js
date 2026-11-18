// Builds resourcepack/ChaosBar.zip: recolors the experience bar red (the chaos bar). Needs only node.
// Usage: node make_chaos_pack.js ../../resourcepack/ChaosBar.zip
const zlib = require('zlib'), fs = require('fs');
function crcTable(){const t=[];for(let n=0;n<256;n++){let c=n;for(let k=0;k<8;k++)c=c&1?0xedb88320^(c>>>1):c>>>1;t[n]=c>>>0;}return t;}
const T=crcTable();
function crc(buf){let c=0xffffffff;for(const b of buf)c=T[(c^b)&255]^(c>>>8);return (c^0xffffffff)>>>0;}
function chunk(type,data){const l=Buffer.alloc(4);l.writeUInt32BE(data.length);const td=Buffer.concat([Buffer.from(type),data]);const c=Buffer.alloc(4);c.writeUInt32BE(crc(td));return Buffer.concat([l,td,c]);}
function png(w,h,px){ // px(x,y) -> [r,g,b,a]
  const raw=Buffer.alloc((w*4+1)*h);
  for(let y=0;y<h;y++){raw[y*(w*4+1)]=0;for(let x=0;x<w;x++){const p=px(x,y);for(let i=0;i<4;i++)raw[y*(w*4+1)+1+x*4+i]=p[i];}}
  const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(w,0);ihdr.writeUInt32BE(h,4);ihdr[8]=8;ihdr[9]=6;
  return Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ihdr),chunk('IDAT',zlib.deflateSync(raw)),chunk('IEND',Buffer.alloc(0))]);
}
const rows=[[255,120,120],[220,20,20],[200,10,10],[150,0,0],[90,0,0]];
const progress=png(182,5,(x,y)=>[...rows[y],255]);
const background=png(182,5,(x,y)=>(y===0||y===4)?[60,20,20,255]:[35,12,12,255]);
// minimal zip writer (stored)
const files={
 'pack.mcmeta':Buffer.from(JSON.stringify({pack:{pack_format:64,description:"Chaos bar"}})),
 'assets/minecraft/textures/gui/sprites/hud/experience_bar_progress.png':progress,
 'assets/minecraft/textures/gui/sprites/hud/experience_bar_background.png':background};
const parts=[],central=[];let off=0;
for(const [name,data] of Object.entries(files)){
  const n=Buffer.from(name),c=crc(data);
  const h=Buffer.alloc(30);h.writeUInt32LE(0x04034b50,0);h.writeUInt16LE(20,4);h.writeUInt16LE(0x0800,6);
  h.writeUInt32LE(0x21,10);h.writeUInt32LE(c,14);h.writeUInt32LE(data.length,18);h.writeUInt32LE(data.length,22);h.writeUInt16LE(n.length,26);
  parts.push(h,n,data);
  const e=Buffer.alloc(46);e.writeUInt32LE(0x02014b50,0);e.writeUInt16LE(20,4);e.writeUInt16LE(20,6);e.writeUInt16LE(0x0800,8);
  e.writeUInt32LE(0x21,12);e.writeUInt32LE(c,16);e.writeUInt32LE(data.length,20);e.writeUInt32LE(data.length,24);e.writeUInt16LE(n.length,28);e.writeUInt32LE(off,42);
  central.push(e,n);off+=30+n.length+data.length;
}
const cd=Buffer.concat(central),end=Buffer.alloc(22);
end.writeUInt32LE(0x06054b50,0);end.writeUInt16LE(3,8);end.writeUInt16LE(3,10);end.writeUInt32LE(cd.length,12);end.writeUInt32LE(off,16);
fs.writeFileSync(process.argv[2],Buffer.concat([...parts,cd,end]));
