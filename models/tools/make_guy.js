// Generates models/guy.bbmodel (a Blockbench model for BetterModel): a small floating AI helper in the style of EVE
// from WALL-E. A rounded white head with a black visor and two light blue eyes floats above a white cone body that
// points its tip at the ground (the flat base of the cone faces the underside of the head).
//
//   node make_guy.js <out.bbmodel>
//
// Everything is plain cubes (Minecraft cannot draw meshes). The round shapes are stacked slabs, each slab made of two
// squares turned 45 degrees to each other, which reads as an octagon. Units are Blockbench pixels (16 = one block).
// The model faces -Z. Animation rotations follow Blockbench: a NEGATIVE x rotation leans the top forward.
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');

const uuid = (seed) => {
  const h = crypto.createHash('md5').update(String(seed)).digest('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20, 32)}`;
};

// ---------------------------------------------------------------- palette texture (64x64, 8x8 swatches)
const MATS = {
  shell: [244, 246, 250],
  shellShade: [206, 212, 222],
  visor: [14, 16, 22],
  eye: [120, 214, 255],
  trim: [150, 160, 176],
};
const MAT_NAMES = Object.keys(MATS);
const SHADES = [1.0, 0.95, 0.82];        // top, sides facing the camera, sides facing away / underside
const FACE_SHADE = { up: 0, north: 1, south: 2, east: 1, west: 2, down: 2 };
const TEX = 64, SWATCH = 8;

function swatchOrigin(mat, shade) {
  const idx = MAT_NAMES.indexOf(mat) * 3 + shade;
  return [(idx % 8) * SWATCH, Math.floor(idx / 8) * SWATCH];
}

function makeTexture() {
  const px = Buffer.alloc(TEX * TEX * 4);
  MAT_NAMES.forEach((mat) => {
    for (let s = 0; s < 3; s++) {
      const [x0, y0] = swatchOrigin(mat, s);
      const c = MATS[mat].map((v) => Math.max(0, Math.min(255, Math.round(v * SHADES[s]))));
      for (let y = 0; y < SWATCH; y++) {
        for (let x = 0; x < SWATCH; x++) {
          const o = ((y0 + y) * TEX + (x0 + x)) * 4;
          px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255;
        }
      }
    }
  });
  return encodePng(TEX, TEX, px);
}

function encodePng(w, h, rgba) {
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) {
    raw[y * (w * 4 + 1)] = 0;
    rgba.copy(raw, y * (w * 4 + 1) + 1, y * w * 4, (y + 1) * w * 4);
  }
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(zlib.crc32(td) >>> 0);
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  return Buffer.concat([Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]), chunk('IHDR', ihdr),
    chunk('IDAT', zlib.deflateSync(raw)), chunk('IEND', Buffer.alloc(0))]);
}

// ---------------------------------------------------------------- model building
const elements = [];
const groups = {};

function facesFor(mat) {
  const f = {};
  for (const dir of Object.keys(FACE_SHADE)) {
    const [x, y] = swatchOrigin(mat, FACE_SHADE[dir]);
    f[dir] = { uv: [x, y, x + SWATCH, y + SWATCH], texture: 0 };
  }
  return f;
}

function cube(name, mat, from, to, rot) {
  const id = uuid('el:' + name + ':' + elements.length);
  const origin = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
  elements.push({
    name, box_uv: false, rescale: false, locked: false, light_emission: 0, render_order: 'default',
    allow_mirror_modeling: true, from, to, autouv: 0, color: 0, origin,
    ...(rot ? { rotation: rot } : {}),
    faces: facesFor(mat), type: 'cube', uuid: id,
  });
  return id;
}

/** A cube that is never drawn: BetterModel reads its size (hitbox, shadow, name tag position) but nothing is rendered. */
function marker(name, from, to) {
  const id = cube(name, 'visor', from, to);
  const el = elements.find((e) => e.uuid === id);
  el.faces = {};
  for (const dir of ['north', 'east', 'south', 'west', 'up', 'down']) el.faces[dir] = { uv: [0, 0, 2, 2] };
  return id;
}

function group(name, origin, children, opt = {}) {
  const g = {
    name, origin, color: 0, uuid: uuid('grp:' + name), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: opt.visibility !== false, autouv: 0, children,
  };
  groups[name] = g;
  return g;
}

/** One round slab: a square and the same square turned 45 degrees, centred on (0, z=0). Radius is the slab's rough radius. */
function slab(name, mat, y0, y1, radius) {
  const a = radius * 1.62;                   // 0.5a..0.707a from the centre, so the average is close to the radius
  return [
    cube(name + '_a', mat, [-a / 2, y0, -a / 2], [a / 2, y1, a / 2]),
    cube(name + '_b', mat, [-a / 2, y0, -a / 2], [a / 2, y1, a / 2], [0, 45, 0]),
  ];
}

// Layout, bottom to top: tip at y = 6 (a little above the ground), cone 14 tall, a 3 gap, then the head.
const TIP_Y = 6, LAYER = 2;
const CONE_RADII = [0.9, 1.7, 2.5, 3.3, 4.0, 4.5, 4.9];          // from the tip up to the base
const CONE_TOP = TIP_Y + CONE_RADII.length * LAYER;               // 20
const HEAD_BOTTOM = CONE_TOP + 3;                                  // 23
const HEAD_LAYER = 1.7;
const HEAD_RADII = [2.2, 3.7, 4.6, 4.6, 3.7, 2.2];
const HEAD_TOP = HEAD_BOTTOM + HEAD_RADII.length * HEAD_LAYER;    // 33.2
const HEAD_MID = (HEAD_BOTTOM + HEAD_TOP) / 2;                     // 28.1

function buildOutliner() {
  const coneParts = [];
  CONE_RADII.forEach((r, i) => {
    coneParts.push(...slab('cone_' + i, i % 3 === 2 ? 'shellShade' : 'shell', TIP_Y + i * LAYER, TIP_Y + (i + 1) * LAYER, r));
  });
  coneParts.push(cube('cone_rim', 'trim', [-4.2, CONE_TOP - 0.5, -4.2], [4.2, CONE_TOP + 0.1, 4.2]));
  const cone = group('cone', [0, CONE_TOP, 0], coneParts);

  const headParts = [];
  HEAD_RADII.forEach((r, i) => {
    headParts.push(...slab('head_' + i, 'shell', HEAD_BOTTOM + i * HEAD_LAYER, HEAD_BOTTOM + (i + 1) * HEAD_LAYER, r));
  });
  // the black visor on the front, with two light blue eyes
  headParts.push(
    cube('visor_mid', 'visor', [-3.5, HEAD_MID - 1.8, -5.9], [3.5, HEAD_MID + 3.0, -4.3]),
    cube('visor_top', 'visor', [-2.4, HEAD_MID + 3.0, -5.7], [2.4, HEAD_MID + 3.8, -4.3]),
    cube('visor_bottom', 'visor', [-2.4, HEAD_MID - 2.6, -5.7], [2.4, HEAD_MID - 1.8, -4.3]),
    cube('eye_r', 'eye', [-3.0, HEAD_MID + 0.0, -6.15], [-1.0, HEAD_MID + 1.6, -5.85]),
    cube('eye_l', 'eye', [1.0, HEAD_MID + 0.0, -6.15], [3.0, HEAD_MID + 1.6, -5.85]));
  const head = group('h_head', [0, HEAD_MID, 0], headParts);

  const body = group('body', [0, 0, 0], [cone, head]);

  return [
    group('hitbox', [0, 0, 0], [marker('hitbox', [-5, 0, -5], [5, 34, 5])], { visibility: false }),
    group('shadow', [0, 0, 0], [marker('shadow', [-6, 0, -6], [6, 0, 6])]),
    group('tag_name', [0, 40, 0], [marker('tag_name', [-1, 39, -1], [1, 41, 1])]),
    body,
  ];
}

// ---------------------------------------------------------------- animations
const anims = [];
function anim(name, loop, length, tracks) {
  const animators = {};
  for (const [bone, channel, rows, interp] of tracks) {
    if (!groups[bone]) throw new Error('animation ' + name + ' uses unknown bone ' + bone);
    const a = animators[groups[bone].uuid] || (animators[groups[bone].uuid] = { name: bone, type: 'bone', keyframes: [] });
    rows.forEach(([t, x, y, z], i) => a.keyframes.push({
      channel, data_points: [{ x: String(x), y: String(y), z: String(z) }],
      uuid: uuid(`kf:${name}:${bone}:${channel}:${i}`), time: t, color: -1, interpolation: interp || 'catmullrom',
    }));
  }
  anims.push({
    uuid: uuid('anim:' + name), name, loop, override: false, length, snapping: 24, selected: false,
    anim_time_update: '', blend_weight: '', start_delay: '', loop_delay: '', animators,
  });
}
const R = (bone, rows, interp) => [bone, 'rotation', rows, interp];
const P = (bone, rows, interp) => [bone, 'position', rows, interp];

function buildAnimations() {
  // idle: hovers up and down, the head turns a little from side to side, the cone sways
  anim('idle', 'loop', 3, [
    P('body', [[0, 0, 0, 0], [0.75, 0, 1.2, 0], [1.5, 0, 0, 0], [2.25, 0, -1.2, 0], [3, 0, 0, 0]]),
    R('h_head', [[0, 0, 0, 0], [0.75, 2, -8, 0], [1.5, 0, 0, 0], [2.25, 2, 8, 0], [3, 0, 0, 0]]),
    R('cone', [[0, 0, 0, 2], [1.5, 0, 0, -2], [3, 0, 0, 2]]),
  ]);

  // walk: leans into its flight and bobs faster
  anim('walk', 'loop', 0.8, [
    R('body', [[0, -10, 0, 0], [0.8, -10, 0, 0]]),
    P('body', [[0, 0, 0, 0], [0.2, 0, 1, 0], [0.4, 0, 0, 0], [0.6, 0, 1, 0], [0.8, 0, 0, 0]]),
    R('h_head', [[0, 8, 0, 0], [0.4, 10, 0, 0], [0.8, 8, 0, 0]]),
    R('cone', [[0, 0, 0, 4], [0.4, 0, 0, -4], [0.8, 0, 0, 4]]),
  ]);

  // attack: the head rears back, then lunges forward; the hit lands at about 0.3 s (6 ticks), which is what
  // attack-hit-delay-ticks in abilities.yml should match
  anim('attack', 'once', 0.7, [
    P('h_head', [[0, 0, 0, 0], [0.18, 0, 0, 2], [0.3, 0, -1, -5], [0.7, 0, 0, 0]]),
    R('h_head', [[0, 0, 0, 0], [0.18, 14, 0, 0], [0.3, -22, 0, 0], [0.7, 0, 0, 0]]),
    R('body', [[0, 0, 0, 0], [0.18, 8, 0, 0], [0.3, -14, 0, 0], [0.7, 0, 0, 0]]),
    P('body', [[0, 0, 0, 0], [0.3, 0, 0, -3], [0.7, 0, 0, 0]]),
  ]);

  // death: loses power, tips over and drops to the ground
  anim('death', 'once', 1.4, [
    R('body', [[0, 0, 0, 0], [0.5, 15, 0, 20], [1.1, 70, 0, 55], [1.4, 85, 0, 60]]),
    P('body', [[0, 0, 0, 0], [0.5, 0, 1, 0], [1.1, 0, -5, 0], [1.4, 0, -5, 0]]),
    R('h_head', [[0, 0, 0, 0], [1.1, 25, 0, 10], [1.4, 25, 0, 10]]),
  ]);
}

// ---------------------------------------------------------------- assemble the .bbmodel
function build() {
  const outliner = buildOutliner();
  buildAnimations();
  const png = makeTexture();
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name: 'guy', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '',
    variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX, height: TEX },
    elements, outliner,
    textures: [{
      path: '', name: 'guy.png', folder: '', namespace: '', id: '0', width: TEX, height: TEX,
      uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false,
      sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1,
      frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
      saved: false, uuid: uuid('texture'), source: 'data:image/png;base64,' + png.toString('base64'),
    }],
    animations: anims,
  };
}

function validate(model) {
  const problems = [];
  const names = new Set(Object.keys(groups));
  for (const need of ['hitbox', 'shadow', 'tag_name', 'h_head', 'body', 'cone']) {
    if (!names.has(need)) problems.push('missing bone ' + need);
  }
  const have = new Set(model.animations.map((a) => a.name));
  for (const need of ['idle', 'walk', 'attack', 'death']) {
    if (!have.has(need)) problems.push('missing animation ' + need);
  }
  const ids = new Set(model.elements.map((e) => e.uuid));
  const used = new Set();
  const walk = (list) => list.forEach((c) => (typeof c === 'string' ? used.add(c) : walk(c.children)));
  walk(model.outliner);
  for (const id of ids) if (!used.has(id)) problems.push('cube not in any bone: ' + id);
  for (const id of used) if (!ids.has(id)) problems.push('bone lists missing cube: ' + id);
  for (const e of model.elements) {
    for (let i = 0; i < 3; i++) if (!(e.from[i] <= e.to[i])) problems.push('cube ' + e.name + ' has from > to');
  }
  return problems;
}

const [, , outFile] = process.argv;
if (!outFile) { console.error('usage: node make_guy.js <out.bbmodel>'); process.exit(1); }
const model = build();
const problems = validate(model);
if (problems.length) { console.error('PROBLEMS:\n' + problems.join('\n')); process.exit(2); }
fs.writeFileSync(outFile, JSON.stringify(model));
console.log(`wrote ${outFile}: ${model.elements.length} cubes, ${Object.keys(groups).length} bones, ` +
  `animations: ${model.animations.map((a) => a.name + '(' + a.length + 's)').join(', ')}`);
