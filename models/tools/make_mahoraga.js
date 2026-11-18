// Generates models/mahoraga.bbmodel (a Blockbench model for BetterModel) and, optionally, a preview image.
//
//   node make_mahoraga.js <out.bbmodel> [preview.png]
//
// Everything is plain cubes grouped into bones, with a small palette texture embedded in the file. Open the result in
// Blockbench to polish it; to change the design, edit this file and run it again.
//
// Units are Blockbench pixels (16 = one block). The model faces -Z. +X is the character's right.
// Animation rotations follow Blockbench/Bedrock: a NEGATIVE x rotation swings an arm forward and up.
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');

const uuid = (seed) => {
  const h = crypto.createHash('md5').update(String(seed)).digest('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20, 32)}`;
};

// ---------------------------------------------------------------- palette texture (64x64, 8x8 swatches)
const MATS = {
  skin: [224, 220, 210],
  skinShade: [190, 184, 174],
  cloth: [46, 48, 62],
  clothLight: [82, 86, 108],
  gold: [218, 174, 62],
  goldDark: [160, 118, 34],
  steel: [184, 192, 202],
  steelDark: [112, 120, 134],
  black: [20, 20, 26],
  bone: [238, 234, 222],
};
const MAT_NAMES = Object.keys(MATS);
const SHADES = [1.1, 1.0, 0.78];
const FACE_SHADE = { up: 0, north: 1, south: 2, east: 1, west: 2, down: 2 };
const TEX = 64, SWATCH = 8;

function swatchOrigin(mat, shade) {
  const idx = MAT_NAMES.indexOf(mat) * 3 + shade;
  return [(idx % 8) * SWATCH, Math.floor(idx / 8) * SWATCH];
}

function makeTexture() {
  const px = Buffer.alloc(TEX * TEX * 4);
  let seed = 12345;
  const rnd = () => ((seed = (seed * 1103515245 + 12345) & 0x7fffffff) / 0x7fffffff);
  MAT_NAMES.forEach((mat) => {
    for (let s = 0; s < 3; s++) {
      const [x0, y0] = swatchOrigin(mat, s);
      for (let by = 0; by < SWATCH / 2; by++) {
        for (let bx = 0; bx < SWATCH / 2; bx++) {
          const n = (rnd() - 0.5) * 14;     // a little grit so flat faces do not look dead
          const c = MATS[mat].map((v) => Math.max(0, Math.min(255, Math.round(v * SHADES[s] + n))));
          for (let dy = 0; dy < 2; dy++) {
            for (let dx = 0; dx < 2; dx++) {
              const o = ((y0 + by * 2 + dy) * TEX + (x0 + bx * 2 + dx)) * 4;
              px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255;
            }
          }
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
const elementMat = new Map();       // uuid -> material (for the preview only)
const groups = {};                  // name -> group object

function facesFor(mat) {
  const f = {};
  for (const dir of Object.keys(FACE_SHADE)) {
    const [x, y] = swatchOrigin(mat, FACE_SHADE[dir]);
    f[dir] = { uv: [x, y, x + SWATCH, y + SWATCH], texture: 0 };
  }
  return f;
}

/** A cube. `rot` (degrees) turns it about `origin` (default: its own centre). Returns its uuid for a group's children. */
function cube(name, mat, from, to, rot) {
  const id = uuid('el:' + name + ':' + elements.length);
  const origin = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
  const el = {
    name, box_uv: false, rescale: false, locked: false, light_emission: 0, render_order: 'default',
    allow_mirror_modeling: true, from, to, autouv: 0, color: 0, origin,
    ...(rot ? { rotation: rot } : {}),
    faces: facesFor(mat), type: 'cube', uuid: id,
  };
  elements.push(el);
  elementMat.set(id, mat);
  return id;
}

/** The mirror image of a box across x = 0. */
const mx = (from, to) => [[-to[0], from[1], from[2]], [-from[0], to[1], to[2]]];
function cubePair(name, mat, from, to) {
  const [lf, lt] = mx(from, to);
  return [cube(name + '_r', mat, from, to), cube(name + '_l', mat, lf, lt)];
}

function group(name, origin, children, opt = {}) {
  const g = {
    name, origin, color: 0, uuid: uuid('grp:' + name), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: opt.visibility !== false, autouv: 0, children,
  };
  groups[name] = g;
  return g;
}

// ---- legs (the hakama): thigh, shin and bare foot
function leg(side) {
  const s = side === 'r' ? 1 : -1;
  const X = (a, b) => (s === 1 ? [a, b] : [-b, -a]);
  const box = (xa, xb, y0, y1, z0, z1) => {
    const [a, b] = X(xa, xb);
    return [[a, y0, z0], [b, y1, z1]];
  };
  const shin = group(side + '_sub_leg', [s * 5.5, 11, 0], [
    cube(side + '_shin', 'cloth', ...box(2.6, 8.4, 3.4, 11, -3.2, 3.2)),
    cube(side + '_cuff', 'gold', ...box(2.3, 8.7, 2.8, 4, -3.6, 3.6)),
    cube(side + '_foot', 'skin', ...box(3, 8, 0, 2.8, -6.5, 3)),
    cube(side + '_toes', 'skinShade', ...box(3.2, 7.8, 0, 1.4, -7.4, -6.4)),
  ]);
  return group(side + '_leg', [s * 5.5, 22, 0], [
    cube(side + '_thigh', 'cloth', ...box(2.2, 8.8, 11, 22.2, -3.8, 3.8)),
    cube(side + '_thigh_trim', 'clothLight', ...box(2.1, 8.9, 10.4, 11.4, -3.9, 3.9)),
    shin,
  ]);
}

// ---- arm: shoulder, upper arm, forearm (with bracer), hand; the right hand also holds the sword
function arm(side) {
  const s = side === 'r' ? 1 : -1;
  const X = (a, b) => (s === 1 ? [a, b] : [-b, -a]);
  const box = (xa, xb, y0, y1, z0, z1) => {
    const [a, b] = X(xa, xb);
    return [[a, y0, z0], [b, y1, z1]];
  };

  const handChildren = [cube(side + '_hand', 'skin', ...box(10.4, 15.1, 15, 19.4, -2.9, 2.9)),
    cube(side + '_fingers', 'skinShade', ...box(10.6, 14.9, 15, 16.4, -3.4, 3.4))];
  const hand = group(side + '_hand', [s * 12.75, 19, 0], handChildren);

  if (side === 'r') {
    // The Sword of Extermination, held forward at the hip: a broad, jagged blade with a gold guard.
    const sword = [
      cube('sword_guard', 'gold', [10.2, 14.6, -4.4], [15.3, 19.6, -3.4]),
      cube('sword_blade', 'steel', [12.05, 14.6, -37], [13.45, 19.4, -4.4]),
      cube('sword_spine', 'steelDark', [12.25, 18.9, -35], [13.25, 19.9, -6]),
      cube('sword_tip', 'steel', [12.3, 15.6, -39], [13.2, 18.6, -37]),
      cube('sword_pommel', 'gold', [11.8, 15.8, 2.9], [13.7, 18.2, 4.6]),
    ];
    [-9, -15, -21, -27, -33].forEach((z, i) => {
      sword.push(cube('sword_tooth_' + i, 'steel', [12.3, 12.8 + (i % 2) * 0.6, z], [13.2, 14.7, z + 2.4]));
    });
    handChildren.push(group('sword', [12.75, 17, 0], sword));
  }

  const forearm = group(side + '_sub_arm', [s * 12.75, 31, 0], [
    cube(side + '_forearm', 'skin', ...box(10.2, 15.3, 19, 31, -3.2, 3.2)),
    cube(side + '_bracer', 'gold', ...box(10, 15.5, 19.2, 23, -3.5, 3.5)),
    cube(side + '_forearm_vein', 'skinShade', ...box(12.3, 13.2, 24, 30, -3.4, -3.1)),
    hand,
  ]);

  return group(side + '_arm', [s * 12.75, 45, 0], [
    cube(side + '_shoulder', 'skin', ...box(9.4, 15.8, 41, 48, -4.6, 4.6)),
    cube(side + '_shoulder_guard', 'gold', ...box(9.2, 16.2, 47.4, 49.2, -4.9, 4.9)),
    cube(side + '_upper_arm', 'skin', ...box(10, 15.5, 31, 41.2, -3.6, 3.6)),
    cube(side + '_bicep', 'skinShade', ...box(10.4, 15.1, 35, 40, -3.9, -3.5)),
    forearm,
  ]);
}

// ---- the wheel: a gold octagonal ring with eight spokes that hovers behind the head
function wheel() {
  const C = [0, 63, 4.5], R = 8.6, parts = [];
  const rad = (d) => (d * Math.PI) / 180;
  for (let i = 0; i < 8; i++) {                      // ring segments, each tangent to the circle
    const phi = i * 45, cx = C[0] + R * Math.cos(rad(phi)), cy = C[1] + R * Math.sin(rad(phi));
    parts.push(cube('wheel_ring_' + i, 'gold', [cx - 4.4, cy - 0.9, C[2] - 1.1], [cx + 4.4, cy + 0.9, C[2] + 1.1],
      [0, 0, phi + 90]));
  }
  for (let i = 0; i < 4; i++) {                      // four bars through the centre = eight spokes
    parts.push(cube('wheel_spoke_' + i, 'goldDark', [C[0] - R, C[1] - 0.7, C[2] - 0.8], [C[0] + R, C[1] + 0.7, C[2] + 0.8],
      [0, 0, i * 45]));
  }
  for (let i = 0; i < 8; i++) {                      // a handle knob at the end of every spoke
    const phi = i * 45, cx = C[0] + (R + 2.2) * Math.cos(rad(phi)), cy = C[1] + (R + 2.2) * Math.sin(rad(phi));
    parts.push(cube('wheel_knob_' + i, 'gold', [cx - 1.1, cy - 1.1, C[2] - 1.3], [cx + 1.1, cy + 1.1, C[2] + 1.3]));
  }
  parts.push(cube('wheel_hub', 'goldDark', [C[0] - 2.2, C[1] - 2.2, C[2] - 1.6], [C[0] + 2.2, C[1] + 2.2, C[2] + 1.6]));
  return group('wheel', C, parts);
}

function buildOutliner() {
  const head = group('h_head', [0, 48, 0], [
    cube('head', 'skin', [-4, 48, -4.5], [4, 58, 3.5]),
    cube('brow', 'skinShade', [-4.3, 55.2, -4.9], [4.3, 56.6, -4.2]),
    cube('eye_band', 'black', [-4.2, 52.8, -4.8], [4.2, 55.2, -4.4]),
    ...cubePair('eye', 'gold', [1.2, 53.4, -4.95], [3, 54.6, -4.75]),
    cube('mask', 'bone', [-3.4, 48.3, -4.95], [3.4, 52.4, -4.45]),
    cube('mouth', 'black', [-2.2, 49.4, -5.05], [2.2, 50.2, -4.9]),
    cube('crest', 'bone', [-0.9, 58, -3.6], [0.9, 60.4, 2.6]),
    ...cubePair('ear', 'skinShade', [4, 51, -1.2], [5.4, 56, 1.8]),
  ]);

  const upper = group('upper_body', [0, 27, 0], [
    cube('torso', 'skin', [-9, 27, -4.6], [9, 45, 4.6]),
    cube('back_flare', 'skinShade', [-10.4, 34, -3], [10.4, 44.5, 3.2]),
    ...cubePair('pec', 'skinShade', [0.4, 36, -5.3], [8.6, 43, -4.6]),
    cube('abs', 'skinShade', [-3.6, 28.2, -5], [3.6, 35.2, -4.6]),
    cube('neck', 'skin', [-2.2, 45, -2.2], [2.2, 48.2, 2.2]),
    head,
    wheel(),
    arm('r'),
    arm('l'),
  ]);

  const under = group('under_body', [0, 22, 0], [
    cube('hips', 'cloth', [-8.8, 20, -4.8], [8.8, 27.4, 4.8]),
    cube('sash', 'clothLight', [-9.1, 24.6, -5.1], [9.1, 27, 5.1]),
    cube('buckle', 'gold', [-1.6, 24.2, -5.4], [1.6, 27.4, -5]),
    group('robe_front', [0, 22, -4.8], [
      cube('robe_front_panel', 'cloth', [-5.2, 8, -5.8], [5.2, 22, -4.9]),
      cube('robe_front_trim', 'gold', [-5.3, 8, -5.9], [5.3, 9.4, -4.8]),
    ]),
    group('robe_back', [0, 22, 4.8], [
      cube('robe_back_panel', 'cloth', [-5.2, 8, 4.9], [5.2, 22, 5.8]),
      cube('robe_back_trim', 'gold', [-5.3, 8, 4.8], [5.3, 9.4, 5.9]),
    ]),
  ]);

  const body = group('body', [0, 0, 0], [leg('r'), leg('l'), under, upper]);

  return [
    group('hitbox', [0, 0, 0], [cube('hitbox', 'black', [-10, 0, -6], [10, 58, 6])], { visibility: false }),
    group('shadow', [0, 0, 0], [cube('shadow', 'black', [-14, 0, -11], [14, 0, 11])]),
    group('tag_name', [0, 76, 4.5], [cube('tag_name', 'black', [-1, 75, 3.5], [1, 77, 5.5])]),
    body,
  ];
}

// ---------------------------------------------------------------- animations
// A track is [time, x, y, z] rows for one channel of one bone. Times in seconds.
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
const L = 'linear';

function buildAnimations() {
  // idle: slow breathing, a little sway, the wheel turning one spoke-width (45 degrees, so the loop is seamless)
  anim('idle', 'loop', 3, [
    R('upper_body', [[0, 0, 0, 0], [1.5, 2, 0, 0], [3, 0, 0, 0]]),
    P('upper_body', [[0, 0, 0, 0], [1.5, 0, 0.5, 0], [3, 0, 0, 0]]),
    R('r_arm', [[0, -4, 0, 3], [1.5, -6, 0, 3], [3, -4, 0, 3]]),
    R('r_hand', [[0, 24, 0, 0], [3, 24, 0, 0]]),
    R('l_arm', [[0, 2, 0, -3], [1.5, 4, 0, -4], [3, 2, 0, -3]]),
    R('h_head', [[0, 0, 0, 0], [0.75, 1, -4, 0], [2.25, 1, 4, 0], [3, 0, 0, 0]]),
    R('wheel', [[0, 0, 0, 0], [3, 0, 0, -45]], L),
    R('robe_front', [[0, 0, 0, 0], [1.5, 2, 0, 0], [3, 0, 0, 0]]),
    R('robe_back', [[0, 0, 0, 0], [1.5, -2, 0, 0], [3, 0, 0, 0]]),
  ]);

  // walk: a heavy, steady stride
  anim('walk', 'loop', 1.2, [
    R('r_leg', [[0, 26, 0, 0], [0.3, 0, 0, 0], [0.6, -26, 0, 0], [0.9, 0, 0, 0], [1.2, 26, 0, 0]]),
    R('l_leg', [[0, -26, 0, 0], [0.3, 0, 0, 0], [0.6, 26, 0, 0], [0.9, 0, 0, 0], [1.2, -26, 0, 0]]),
    R('r_sub_leg', [[0, 4, 0, 0], [0.3, 4, 0, 0], [0.6, 4, 0, 0], [0.9, 34, 0, 0], [1.2, 4, 0, 0]]),
    R('l_sub_leg', [[0, 4, 0, 0], [0.3, 34, 0, 0], [0.6, 4, 0, 0], [0.9, 4, 0, 0], [1.2, 4, 0, 0]]),
    R('r_arm', [[0, -16, 0, 3], [0.3, -6, 0, 3], [0.6, 4, 0, 3], [0.9, -6, 0, 3], [1.2, -16, 0, 3]]),
    R('r_hand', [[0, 30, 0, 0], [1.2, 30, 0, 0]]),
    R('l_arm', [[0, 16, 0, -3], [0.3, 2, 0, -3], [0.6, -12, 0, -3], [0.9, 2, 0, -3], [1.2, 16, 0, -3]]),
    R('upper_body', [[0, 3, -5, 0], [0.3, 3, 0, 0], [0.6, 3, 5, 0], [0.9, 3, 0, 0], [1.2, 3, -5, 0]]),
    P('body', [[0, 0, 0, 0], [0.3, 0, 1.2, 0], [0.6, 0, 0, 0], [0.9, 0, 1.2, 0], [1.2, 0, 0, 0]]),
    R('wheel', [[0, 0, 0, 0], [1.2, 0, 0, -90]], L),
    R('robe_front', [[0, 10, 0, 0], [0.6, 4, 0, 0], [1.2, 10, 0, 0]]),
    R('robe_back', [[0, -8, 0, 0], [0.6, -2, 0, 0], [1.2, -8, 0, 0]]),
  ]);

  // attack: raise the sword, then bring it down. The blade lands at about 0.42 s (8 ticks), which is what
  // attack-hit-delay-ticks in abilities.yml should match.
  anim('attack', 'once', 1.0, [
    R('r_arm', [[0, -5, 0, 3], [0.28, -140, 0, 3], [0.42, -32, 0, 3], [0.6, -24, 0, 3], [1.0, -5, 0, 3]]),
    R('r_sub_arm', [[0, 0, 0, 0], [0.28, -55, 0, 0], [0.42, -4, 0, 0], [1.0, 0, 0, 0]]),
    R('r_hand', [[0, 24, 0, 0], [0.28, -25, 0, 0], [0.42, 72, 0, 0], [0.6, 55, 0, 0], [1.0, 24, 0, 0]]),
    R('upper_body', [[0, 0, 0, 0], [0.28, 10, 14, 0], [0.42, -14, -18, 0], [0.65, -8, -6, 0], [1.0, 0, 0, 0]]),
    R('l_arm', [[0, 2, 0, -3], [0.28, -24, 0, -10], [0.42, 14, 0, -6], [1.0, 2, 0, -3]]),
    R('h_head', [[0, 0, 0, 0], [0.28, -8, 0, 0], [0.42, 12, 0, 0], [1.0, 0, 0, 0]]),
    P('body', [[0, 0, 0, 0], [0.28, 0, 0, 1.5], [0.42, 0, -0.5, -4], [1.0, 0, 0, 0]]),
    R('r_leg', [[0, 0, 0, 0], [0.42, -16, 0, 0], [1.0, 0, 0, 0]]),
    R('l_leg', [[0, 0, 0, 0], [0.42, 10, 0, 0], [1.0, 0, 0, 0]]),
    R('wheel', [[0, 0, 0, 0], [1.0, 0, 0, -180]], L),
  ]);

  // flame: both arms thrown forward to unleash the blast, the wheel spinning up
  anim('flame', 'once', 1.6, [
    R('r_arm', [[0, -5, 0, 3], [0.5, -100, 0, 6], [1.2, -100, 0, 6], [1.6, -5, 0, 3]]),
    R('l_arm', [[0, 2, 0, -3], [0.5, -100, 0, -6], [1.2, -100, 0, -6], [1.6, 2, 0, -3]]),
    R('r_sub_arm', [[0, 0, 0, 0], [0.5, -10, 0, 0], [1.2, -10, 0, 0], [1.6, 0, 0, 0]]),
    R('r_hand', [[0, 24, 0, 0], [0.5, 0, 0, 0], [1.2, 0, 0, 0], [1.6, 24, 0, 0]]),
    R('upper_body', [[0, 0, 0, 0], [0.5, 8, 0, 0], [1.2, 8, 0, 0], [1.6, 0, 0, 0]]),
    R('h_head', [[0, 0, 0, 0], [0.5, -12, 0, 0], [1.2, -12, 0, 0], [1.6, 0, 0, 0]]),
    R('wheel', [[0, 0, 0, 0], [1.6, 0, 0, -540]], L),
  ]);

  // death: the wheel stops turning and the body topples backwards
  anim('death', 'once', 2.4, [
    R('body', [[0, 0, 0, 0], [0.4, -6, 0, 0], [1.4, 60, 0, 0], [1.9, 90, 0, 0], [2.4, 90, 0, 0]]),
    P('body', [[0, 0, 0, 0], [1.9, 0, 4, 0], [2.4, 0, 4, 0]]),
    R('r_arm', [[0, -5, 0, 3], [1.4, 25, 0, 20], [2.4, 25, 0, 20]]),
    R('l_arm', [[0, 2, 0, -3], [1.4, 25, 0, -20], [2.4, 25, 0, -20]]),
    R('r_hand', [[0, 24, 0, 0], [2.4, 24, 0, 0]]),
    R('h_head', [[0, 0, 0, 0], [1.4, 18, 0, 0], [2.4, 18, 0, 0]]),
    R('wheel', [[0, 0, 0, 0], [2.4, 0, 0, -90]], L),
  ]);
}

// ---------------------------------------------------------------- assemble the .bbmodel
function build() {
  const outliner = buildOutliner();
  buildAnimations();
  const png = makeTexture();
  return {
    model: {
      meta: { format_version: '4.10', model_format: 'free', box_uv: false },
      name: 'mahoraga', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '',
      variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
      resolution: { width: TEX, height: TEX },
      elements, outliner,
      textures: [{
        path: '', name: 'mahoraga.png', folder: '', namespace: '', id: '0', width: TEX, height: TEX,
        uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false,
        sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1,
        frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
        saved: false, uuid: uuid('texture'), source: 'data:image/png;base64,' + png.toString('base64'),
      }],
      animations: anims,
    },
    outliner,
  };
}

// ---------------------------------------------------------------- validation
function validate(model) {
  const problems = [];
  const names = new Set(Object.keys(groups));
  for (const need of ['hitbox', 'shadow', 'tag_name', 'h_head', 'body']) {
    if (!names.has(need)) problems.push('missing bone ' + need);
  }
  const have = new Set(model.animations.map((a) => a.name));
  for (const need of ['idle', 'walk', 'attack', 'flame', 'death']) {
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

// ---------------------------------------------------------------- preview (rest and animated poses)
const deg = (d) => (d * Math.PI) / 180;
function rotate(p, o, [rx, ry, rz]) {
  let [x, y, z] = [p[0] - o[0], p[1] - o[1], p[2] - o[2]];
  let c = Math.cos(deg(rx)), s = Math.sin(deg(rx)); [y, z] = [y * c - z * s, y * s + z * c];
  c = Math.cos(deg(ry)); s = Math.sin(deg(ry)); [x, z] = [x * c + z * s, -x * s + z * c];
  c = Math.cos(deg(rz)); s = Math.sin(deg(rz)); [x, y] = [x * c - y * s, x * s + y * c];
  return [x + o[0], y + o[1], z + o[2]];
}

function sample(rows, t) {
  if (!rows || !rows.length) return [0, 0, 0];
  if (t <= rows[0].time) return rows[0].v;
  for (let i = 1; i < rows.length; i++) {
    if (t <= rows[i].time) {
      const a = rows[i - 1], b = rows[i], f = (t - a.time) / (b.time - a.time || 1);
      return a.v.map((v, k) => v + (b.v[k] - v) * f);
    }
  }
  return rows[rows.length - 1].v;
}

function poseOf(animName, t) {
  const pose = {};
  const a = anims.find((x) => x.name === animName);
  if (!a) return pose;
  for (const an of Object.values(a.animators)) {
    const get = (ch) => sample(an.keyframes.filter((k) => k.channel === ch)
      .map((k) => ({ time: k.time, v: [k.data_points[0].x, k.data_points[0].y, k.data_points[0].z].map(Number) })), t);
    pose[an.name] = { rot: get('rotation'), pos: get('position') };
  }
  return pose;
}

function renderPreview(model, outliner, file, views) {
  const SC = 4, PW = 230, PH = 360;
  const W = PW * views.length, H = PH;
  const px = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) { px[i * 4] = 236; px[i * 4 + 1] = 238; px[i * 4 + 2] = 242; px[i * 4 + 3] = 255; }

  // parent chain for each cube
  const chain = new Map();
  const walk = (list, parents) => list.forEach((c) => {
    if (typeof c === 'string') chain.set(c, parents);
    else walk(c.children, [c, ...parents]);       // innermost group first
  });
  walk(outliner, []);

  views.forEach((view, vi) => {
    const pose = poseOf(view.anim, view.t || 0);
    const polys = [];
    for (const e of model.elements) {
      if (['hitbox', 'shadow', 'tag_name'].includes(e.name)) continue;     // not drawn in game
      const [a, b] = [e.from, e.to];
      let pts = [];
      for (const x of [a[0], b[0]]) for (const y of [a[1], b[1]]) for (const z of [a[2], b[2]]) {
        let p = [x, y, z];
        if (e.rotation) p = rotate(p, e.origin, e.rotation);
        for (const g of chain.get(e.uuid)) {
          const pg = pose[g.name];
          if (!pg) continue;
          p = rotate(p, g.origin, [-pg.rot[0], -pg.rot[1], pg.rot[2]]);
          p = [p[0] - pg.pos[0], p[1] + pg.pos[1], p[2] + pg.pos[2]];
        }
        pts.push(p);
      }
      const c = pts.reduce((s, p) => [s[0] + p[0] / 8, s[1] + p[1] / 8, s[2] + p[2] / 8], [0, 0, 0]);
      const front = view.side === 'front';
      const depth = front ? -c[2] : c[0];           // larger = nearer the viewer, drawn later
      const proj = pts.map((p) => (front ? [-p[0], p[1]] : [-p[2], p[1]]));
      polys.push({ depth, hull: hull(proj), mat: elementMat.get(e.uuid) });
    }
    polys.sort((p, q) => p.depth - q.depth);
    const ox = vi * PW + PW / 2 + (view.dx || 0), oy = H - 14;
    for (const p of polys) {
      const base = MATS[p.mat];
      fillPolygon(px, W, H, p.hull.map(([x, y]) => [ox + x * SC, oy - y * SC]), base, base.map((v) => v * 0.55));
    }
  });
  fs.writeFileSync(file, encodePng(W, H, px));
}

function hull(points) {
  const pts = points.map((p) => p.slice()).sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  const cross = (o, a, b) => (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
  const lower = [], upper = [];
  for (const p of pts) { while (lower.length >= 2 && cross(lower[lower.length - 2], lower[lower.length - 1], p) <= 0) lower.pop(); lower.push(p); }
  for (const p of pts.reverse()) { while (upper.length >= 2 && cross(upper[upper.length - 2], upper[upper.length - 1], p) <= 0) upper.pop(); upper.push(p); }
  return lower.slice(0, -1).concat(upper.slice(0, -1));
}

function fillPolygon(px, W, H, poly, color, edge) {
  if (poly.length < 3) return;
  const ys = poly.map((p) => p[1]);
  const y0 = Math.max(0, Math.floor(Math.min(...ys))), y1 = Math.min(H - 1, Math.ceil(Math.max(...ys)));
  const put = (x, y, c) => { if (x >= 0 && x < W && y >= 0 && y < H) { const o = (y * W + x) * 4; px[o] = c[0]; px[o + 1] = c[1]; px[o + 2] = c[2]; px[o + 3] = 255; } };
  for (let y = y0; y <= y1; y++) {
    const xs = [];
    for (let i = 0; i < poly.length; i++) {
      const a = poly[i], b = poly[(i + 1) % poly.length];
      if ((a[1] <= y + 0.5 && b[1] > y + 0.5) || (b[1] <= y + 0.5 && a[1] > y + 0.5)) {
        xs.push(a[0] + ((y + 0.5 - a[1]) / (b[1] - a[1])) * (b[0] - a[0]));
      }
    }
    xs.sort((m, n) => m - n);
    for (let i = 0; i + 1 < xs.length; i += 2) for (let x = Math.round(xs[i]); x <= Math.round(xs[i + 1]); x++) put(x, y, color);
  }
  for (let i = 0; i < poly.length; i++) {          // outline so neighbouring parts stay readable
    const a = poly[i], b = poly[(i + 1) % poly.length];
    const n = Math.ceil(Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])));
    for (let k = 0; k <= n; k++) put(Math.round(a[0] + ((b[0] - a[0]) * k) / (n || 1)), Math.round(a[1] + ((b[1] - a[1]) * k) / (n || 1)), edge.map(Math.round));
  }
}

// ---------------------------------------------------------------- main
const [, , outFile, previewFile] = process.argv;
if (!outFile) { console.error('usage: node make_mahoraga.js <out.bbmodel> [preview.png]'); process.exit(1); }
const { model, outliner } = build();
const problems = validate(model);
if (problems.length) { console.error('PROBLEMS:\n' + problems.join('\n')); process.exit(2); }
fs.writeFileSync(outFile, JSON.stringify(model));
console.log(`wrote ${outFile}: ${model.elements.length} cubes, ${Object.keys(groups).length} bones, ` +
  `animations: ${model.animations.map((a) => a.name + '(' + a.length + 's)').join(', ')}`);
if (previewFile) {
  renderPreview(model, outliner, previewFile, [
    { side: 'front', anim: 'idle', t: 0 }, { side: 'side', anim: 'idle', t: 0 },
    { side: 'side', anim: 'walk', t: 0.15 }, { side: 'side', anim: 'attack', t: 0.28 },
    { side: 'side', anim: 'attack', t: 0.42 }, { side: 'front', anim: 'flame', t: 0.9 },
    { side: 'side', anim: 'death', t: 1.4, dx: -60 },
  ]);
  console.log('wrote preview ' + previewFile);
}
