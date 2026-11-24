// Generates models/mist_stalker.bbmodel (a Blockbench model for BetterModel) and, optionally, a preview image.
// The Mist Stalker: Seattle Mist's shikigami. A gaunt black thing about 4 blocks tall that walks on six thin, spindly
// legs whose knees rise above its body. Its neck juts forward out of its chest, ending in a narrow skull with two tiny
// white eyes, and a thin segmented tail hangs down behind it.
//
//   node make_mist_stalker.js <out.bbmodel> [preview.png]
//
// Units are Blockbench pixels (16 = one block). The model faces -Z. +X is its right.
// The legs and neck are straight segments aimed from one point to another: each segment is a cube standing along +Y
// inside a bone that is turned (a fixed rotation in the model) to point the right way.
// Blockbench writes rotations as [x, y, z] degrees, applied x first, then y, then z, but with two different signs:
//   - a bone's fixed rotation is in the usual right-hand sense: it turns by +x, +y, +z;
//   - an animation keyframe turns by -x, -y, +z (the convention make_mahoraga.js's animations use).
// (Using the animation sign for the fixed rotations once turned the neck backwards, away from the head.)
const fs = require('fs');
const zlib = require('zlib');
const crypto = require('crypto');

const uuid = (seed) => {
  const h = crypto.createHash('md5').update(String(seed)).digest('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20, 32)}`;
};

// ---------------------------------------------------------------- palette texture (64x64, 8x8 swatches)
const MATS = {
  black: [16, 14, 18],
  shade: [30, 26, 32],
  sinew: [52, 22, 28],          // dark maroon, for the ribs and the joints
  eye: [246, 246, 240],
  mouth: [96, 12, 16],
};
const MAT_NAMES = Object.keys(MATS);
const SHADES = [1.25, 1.0, 0.75];
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
const elementMat = new Map();
const groups = {};

function facesFor(mat) {
  const f = {};
  for (const dir of Object.keys(FACE_SHADE)) {
    const [x, y] = swatchOrigin(mat, FACE_SHADE[dir]);
    f[dir] = { uv: [x, y, x + SWATCH, y + SWATCH], texture: 0 };
  }
  return f;
}

/** A cube. Returns its uuid for a group's children. `glow` makes it ignore darkness (the eyes). */
function cube(name, mat, from, to, glow = false) {
  const id = uuid('el:' + name + ':' + elements.length);
  const origin = [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2];
  elements.push({
    name, box_uv: false, rescale: false, locked: false, light_emission: glow ? 15 : 0, render_order: 'default',
    allow_mirror_modeling: true, from, to, autouv: 0, color: 0, origin,
    faces: facesFor(mat), type: 'cube', uuid: id,
  });
  elementMat.set(id, mat);
  return id;
}

/** A cube that is never drawn: BetterModel reads its size (hitbox, shadow, name tag position). */
function marker(name, from, to) {
  const id = cube(name, 'black', from, to);
  const el = elements.find((e) => e.uuid === id);
  el.faces = {};
  for (const dir of ['north', 'east', 'south', 'west', 'up', 'down']) el.faces[dir] = { uv: [0, 0, 2, 2] };
  return id;
}

function group(name, origin, children, opt = {}) {
  const g = {
    name, origin, color: 0, uuid: uuid('grp:' + name), export: true, mirror_uv: false,
    isOpen: true, locked: false, visibility: opt.visibility !== false, autouv: 0, children,
    ...(opt.rotation ? { rotation: opt.rotation } : {}),
  };
  groups[name] = g;
  return g;
}

/**
 * A straight segment from `a` to `b`, `width` thick: a bone at `a` holding a cube that stands along +Y, turned to
 * point at `b`: x by asin(dz), then z by atan2(-dx, dy) (a bone's fixed rotation, so in the right-hand sense).
 */
function segment(name, mat, a, b, width) {
  const d = [b[0] - a[0], b[1] - a[1], b[2] - a[2]];
  const length = Math.hypot(...d);
  const [dx, dy, dz] = d.map((v) => v / length);
  const ax = Math.asin(dz) * 180 / Math.PI;
  const az = Math.atan2(-dx, dy) * 180 / Math.PI;
  const h = width / 2;
  const id = cube(name, mat, [a[0] - h, a[1], a[2] - h], [a[0] + h, a[1] + length, a[2] + h]);
  return group(name, a, [id], { rotation: [round(ax), 0, round(az)] });
}
const round = (v) => Math.round(v * 100) / 100;

/** A small knob centred on a point (a knee, a hip). */
function knob(name, mat, at, size) {
  const h = size / 2;
  return cube(name, mat, [at[0] - h, at[1] - h, at[2] - h], [at[0] + h, at[1] + h, at[2] + h]);
}

// ---- the legs: three a side, each a thin thigh rising up and out to a knee above the body, then a long shin
// reaching down and out to the ground. The thigh and shin are both fixed inside one bone at the hip, so the walk
// swings the whole leg from there.
const LEGS = [
  { key: 'f', hip: [3, 42, -6], knee: [13, 61, -17], foot: [22, 0, -30] },
  { key: 'm', hip: [3.5, 41, 1], knee: [17, 64, 1], foot: [28, 0, 2] },
  { key: 'b', hip: [3, 40, 8], knee: [13, 61, 18], foot: [22, 0, 32] },
];

function leg(side, spec) {
  const s = side === 'r' ? 1 : -1;
  const m = (p) => [s * p[0], p[1], p[2]];
  const name = 'leg_' + side + spec.key;
  const hip = m(spec.hip), knee = m(spec.knee), foot = m(spec.foot);
  return group(name, hip, [
    knob(name + '_hip', 'sinew', hip, 2.4),
    segment(name + '_thigh', 'black', hip, knee, 1.6),
    knob(name + '_knee', 'sinew', knee, 2.2),
    segment(name + '_shin', 'black', knee, foot, 1.1),
  ]);
}

function buildOutliner() {
  // the gaunt frame: a narrow chest with ribs showing, a waist thinner still, then a hanging, tapering tail
  const torso = [
    cube('chest', 'black', [-3.5, 38, -10], [3.5, 46, -1]),
    cube('chest_keel', 'shade', [-1.2, 36.5, -9], [1.2, 38, -2]),
    cube('waist', 'black', [-1.8, 38.5, -1], [1.8, 42, 6]),
    cube('pelvis', 'black', [-2.6, 35, 6], [2.6, 41, 12]),
    cube('tail_1', 'black', [-1.9, 29, 8.5], [1.9, 35, 12.5]),
    cube('tail_2', 'shade', [-1.3, 23, 9.5], [1.3, 29, 12.5]),
    cube('tail_3', 'black', [-0.8, 17.5, 10.3], [0.8, 23, 12]),
    cube('tail_tip', 'sinew', [-0.4, 14, 10.8], [0.4, 17.5, 11.6]),
  ];
  for (const y of [39, 41.2, 43.4]) {           // ribs on both sides of the chest
    torso.push(cube('rib_r_' + y, 'sinew', [3.5, y, -9], [3.9, y + 0.8, -2]));
    torso.push(cube('rib_l_' + y, 'sinew', [-3.9, y, -9], [-3.5, y + 0.8, -2]));
  }
  for (const z of [-8, -5, -2, 1, 4]) {          // knobs of spine along the back
    torso.push(cube('spine_' + z, 'shade', [-0.5, z < -1 ? 46 : 42, z], [0.5, z < -1 ? 47.4 : 43.2, z + 1.2]));
  }

  // the neck juts forward and a little up out of the front of the chest; the skull sits on the end of it
  const neckBase = [0, 44, -9], neckEnd = [0, 49, -24];
  const head = group('h_head', neckEnd, [
    cube('skull', 'black', [-2.4, 47, -31], [2.4, 53, -23.5]),
    cube('brow', 'shade', [-2.6, 52, -31.2], [2.6, 53, -27]),
    cube('snout', 'black', [-1.7, 46.6, -33.5], [1.7, 50.5, -31]),
    cube('jaw', 'shade', [-1.4, 45.6, -32.5], [1.4, 46.8, -26]),
    cube('mouth', 'mouth', [-1.2, 47.2, -33.7], [1.2, 47.9, -33.4]),
    cube('eye_r', 'eye', [0.8, 51, -31.45], [1.6, 51.8, -31.05], true),
    cube('eye_l', 'eye', [-1.6, 51, -31.45], [-0.8, 51.8, -31.05], true),
  ]);
  const neck = group('neck', neckBase, [
    segment('neck_bone', 'black', neckBase, neckEnd, 1.9),
    knob('neck_joint', 'sinew', neckEnd, 2.2),
    head,
  ]);

  const legs = [];
  for (const side of ['r', 'l']) for (const spec of LEGS) legs.push(leg(side, spec));

  const upper = group('upper', [0, 40, 0], [...torso, neck]);
  const body = group('body', [0, 0, 0], [upper, ...legs]);

  return [
    group('hitbox', [0, 0, 0], [marker('hitbox', [-9, 0, -9], [9, 60, 9])], { visibility: false }),
    group('shadow', [0, 0, 0], [marker('shadow', [-14, 0, -14], [14, 0, 14])]),
    group('tag_name', [0, 68, 0], [marker('tag_name', [-1, 67, -1], [1, 69, 1])]),
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

/**
 * A leg swing in Blockbench numbers: `fwd` degrees of the foot towards the front, `lift` degrees of the foot upwards.
 * Swinging about the vertical axis moves a right and a left foot opposite ways, so the left one is mirrored.
 */
function swing(side, fwd, lift) {
  const s = side === 'r' ? 1 : -1;
  return [0, s * fwd, s * lift];
}

function buildAnimations() {
  // idle: it breathes slowly, its head tilts this way and that, and now and then a leg twitches
  anim('idle', 'loop', 3.2, [
    P('upper', [[0, 0, 0, 0], [1.6, 0, -0.8, 0], [3.2, 0, 0, 0]]),
    R('neck', [[0, 0, 0, 0], [0.8, -4, 6, 10], [1.6, 0, 0, 0], [2.4, -4, -6, -12], [3.2, 0, 0, 0]]),
    R('leg_rf', [[0, 0, 0, 0], [2.0, 0, 0, 0], [2.15, ...swing('r', 4, 6).slice(0)], [2.35, 0, 0, 0], [3.2, 0, 0, 0]], 'linear'),
    R('leg_lb', [[0, 0, 0, 0], [0.9, 0, 0, 0], [1.05, ...swing('l', -4, 6)], [1.25, 0, 0, 0], [3.2, 0, 0, 0]], 'linear'),
  ]);

  // walk: a tripod gait. Right front, left middle and right back step together, then the other three. Each step
  // swings a leg forward with its foot lifted, and back again on the ground. The body sways and bobs with each step.
  const T = 0.8, F = 16, L = 7;
  const tripodA = [['r', 'f'], ['l', 'm'], ['r', 'b']];
  const tracks = [
    P('upper', [[0, 0, 0, 0], [T / 4, 0, 1, 0], [T / 2, 0, 0, 0], [3 * T / 4, 0, 1, 0], [T, 0, 0, 0]]),
    R('upper', [[0, 0, 0, 3], [T / 2, 0, 0, -3], [T, 0, 0, 3]]),
    R('neck', [[0, 6, 0, 0], [T / 4, 2, 0, 0], [T / 2, 6, 0, 0], [3 * T / 4, 2, 0, 0], [T, 6, 0, 0]]),
  ];
  for (const side of ['r', 'l']) {
    for (const key of ['f', 'm', 'b']) {
      const inA = tripodA.some(([s, k]) => s === side && k === key);
      const step = [[0, ...swing(side, -F, 0)], [T / 4, ...swing(side, 0, L)], [T / 2, ...swing(side, F, 0)],
        [3 * T / 4, ...swing(side, 0, 0)], [T, ...swing(side, -F, 0)]];
      // the other tripod is half a step behind
      const rows = inA ? step : step.map(([t, x, y, z]) => [t, x, y, z]).map((r, i, all) => [r[0], ...all[(i + 2) % 4].slice(1)]);
      tracks.push(R('leg_' + side + key, rows));
    }
  }
  anim('walk', 'loop', T, tracks);

  // attack: the neck draws back, then the skull darts forward and down; the hit lands at about 0.3 s (6 ticks),
  // which is what attack-hit-delay-ticks in abilities.yml should match
  anim('attack', 'once', 0.7, [
    P('neck', [[0, 0, 0, 0], [0.15, 0, 1, 4], [0.3, 0, -2, -8], [0.7, 0, 0, 0]]),
    R('neck', [[0, 0, 0, 0], [0.15, -12, 0, 0], [0.3, 22, 0, 0], [0.7, 0, 0, 0]]),
    R('upper', [[0, 0, 0, 0], [0.15, -5, 0, 0], [0.3, 8, 0, 0], [0.7, 0, 0, 0]]),
  ]);

  // death: its legs splay out and give way, and the body drops to the ground
  const deathTracks = [
    P('upper', [[0, 0, 0, 0], [0.6, 0, -10, 0], [1.4, 0, -30, 0]]),
    R('neck', [[0, 0, 0, 0], [1.4, 35, 0, 20]]),
  ];
  for (const side of ['r', 'l']) {
    for (const key of ['f', 'm', 'b']) {
      deathTracks.push(R('leg_' + side + key, [[0, 0, 0, 0], [0.6, ...swing(side, 0, -15)], [1.4, ...swing(side, 0, -40)]]));
    }
  }
  anim('death', 'once', 1.4, deathTracks);
}

// ---------------------------------------------------------------- assemble the .bbmodel
function build() {
  const outliner = buildOutliner();
  buildAnimations();
  const png = makeTexture();
  const model = {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name: 'mist_stalker', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '',
    variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX, height: TEX },
    elements, outliner,
    textures: [{
      path: '', name: 'mist_stalker.png', folder: '', namespace: '', id: '0', width: TEX, height: TEX,
      uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false,
      sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1,
      frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
      saved: false, uuid: uuid('texture'), source: 'data:image/png;base64,' + png.toString('base64'),
    }],
    animations: anims,
  };
  return { model, outliner };
}

function validate(model) {
  const problems = [];
  const names = new Set(Object.keys(groups));
  for (const need of ['hitbox', 'shadow', 'tag_name', 'h_head', 'body', 'upper', 'neck']) {
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

// ---------------------------------------------------------------- preview (rest and animated poses)
const deg = (d) => (d * Math.PI) / 180;
/** Turns a point about `o` by right-hand [x, y, z] degrees, x first, then y, then z. */
function rotate(p, o, [rx, ry, rz]) {
  let [x, y, z] = [p[0] - o[0], p[1] - o[1], p[2] - o[2]];
  let c = Math.cos(deg(rx)), s = Math.sin(deg(rx)); [y, z] = [y * c - z * s, y * s + z * c];
  c = Math.cos(deg(ry)); s = Math.sin(deg(ry)); [x, z] = [x * c + z * s, -x * s + z * c];
  c = Math.cos(deg(rz)); s = Math.sin(deg(rz)); [x, y] = [x * c - y * s, x * s + y * c];
  return [x + o[0], y + o[1], z + o[2]];
}
/** A bone's fixed rotation: right-hand as written. */
const rotateFixed = (p, o, r) => rotate(p, o, r);
/** An animation keyframe's rotation: Blockbench flips x and y. */
const rotateAnimated = (p, o, [rx, ry, rz]) => rotate(p, o, [-rx, -ry, rz]);

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
  const SC = 4, PW = 300, PH = 300;
  const W = PW * views.length, H = PH;
  const px = Buffer.alloc(W * H * 4);
  for (let i = 0; i < W * H; i++) { px[i * 4] = 200; px[i * 4 + 1] = 204; px[i * 4 + 2] = 210; px[i * 4 + 3] = 255; }

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
      if (['hitbox', 'shadow', 'tag_name'].includes(e.name)) continue;
      const [a, b] = [e.from, e.to];
      const pts = [];
      for (const x of [a[0], b[0]]) for (const y of [a[1], b[1]]) for (const z of [a[2], b[2]]) {
        let p = [x, y, z];
        for (const g of chain.get(e.uuid)) {
          if (g.rotation) p = rotateFixed(p, g.origin, g.rotation);
          const pg = pose[g.name];
          if (!pg) continue;
          p = rotateAnimated(p, g.origin, pg.rot);
          p = [p[0] - pg.pos[0], p[1] + pg.pos[1], p[2] + pg.pos[2]];
        }
        pts.push(p);
      }
      const c = pts.reduce((s, p) => [s[0] + p[0] / 8, s[1] + p[1] / 8, s[2] + p[2] / 8], [0, 0, 0]);
      let proj, depth;
      if (view.side === 'front') { proj = pts.map((p) => [-p[0], p[1]]); depth = -c[2]; }
      else if (view.side === 'top') { proj = pts.map((p) => [-p[0], -p[2]]); depth = c[1]; }
      else { proj = pts.map((p) => [-p[2], p[1]]); depth = c[0]; }
      polys.push({ depth, hull: hull(proj), mat: elementMat.get(e.uuid) });
    }
    polys.sort((p, q) => p.depth - q.depth);
    const ox = vi * PW + PW / 2, oy = view.side === 'top' ? H / 2 : H - 20;
    for (const p of polys) {
      const base = MATS[p.mat].map((v) => Math.max(v, 10) * 1.6);
      fillPolygon(px, W, H, p.hull.map(([x, y]) => [ox + x * SC, oy - y * SC]), base, base.map((v) => v * 0.5));
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
  const put = (x, y, c) => { if (x >= 0 && x < W && y >= 0 && y < H) { const o = (y * W + x) * 4; px[o] = Math.min(255, c[0]); px[o + 1] = Math.min(255, c[1]); px[o + 2] = Math.min(255, c[2]); px[o + 3] = 255; } };
  for (let y = y0; y <= y1; y++) {
    const xs = [];
    for (let i = 0; i < poly.length; i++) {
      const a = poly[i], b = poly[(i + 1) % poly.length];
      if ((a[1] <= y + 0.5 && b[1] > y + 0.5) || (b[1] <= y + 0.5 && a[1] > y + 0.5)) {
        xs.push(a[0] + ((y + 0.5 - a[1]) / (b[1] - a[1])) * (b[0] - a[0]));
      }
    }
    xs.sort((m, n) => m - n);
    for (let i = 0; i + 1 < xs.length; i += 2) for (let x = Math.round(xs[i]); x <= Math.round(xs[i + 1]); x++) put(x, y, color.map(Math.round));
  }
  for (let i = 0; i < poly.length; i++) {
    const a = poly[i], b = poly[(i + 1) % poly.length];
    const n = Math.ceil(Math.max(Math.abs(b[0] - a[0]), Math.abs(b[1] - a[1])));
    for (let k = 0; k <= n; k++) put(Math.round(a[0] + ((b[0] - a[0]) * k) / (n || 1)), Math.round(a[1] + ((b[1] - a[1]) * k) / (n || 1)), edge.map(Math.round));
  }
}

// ---------------------------------------------------------------- main
const [, , outFile, previewFile] = process.argv;
if (!outFile) { console.error('usage: node make_mist_stalker.js <out.bbmodel> [preview.png]'); process.exit(1); }
const { model, outliner } = build();
const problems = validate(model);
if (problems.length) { console.error('PROBLEMS:\n' + problems.join('\n')); process.exit(2); }
fs.writeFileSync(outFile, JSON.stringify(model));
console.log(`wrote ${outFile}: ${model.elements.length} cubes, ${Object.keys(groups).length} bones, ` +
  `animations: ${model.animations.map((a) => a.name + '(' + a.length + 's)').join(', ')}`);
if (previewFile) {
  renderPreview(model, outliner, previewFile, [
    { side: 'front', anim: 'idle', t: 0 }, { side: 'side', anim: 'idle', t: 0 }, { side: 'top', anim: 'idle', t: 0 },
    { side: 'side', anim: 'walk', t: 0.2 }, { side: 'side', anim: 'attack', t: 0.3 },
  ]);
  console.log('wrote preview ' + previewFile);
}
