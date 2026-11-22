// Generates models/pink.bbmodel (a Blockbench model for BetterModel): Pink!, a shikigami with the body of a normal
// player (classic 4-pixel arms) wearing the skin in models/skins/pink.png.
//
//   node make_pink.js <skin.png> <out.bbmodel>
//
// Each body part is one cube, plus a slightly bigger cube for the skin's outer layer (hat, jacket, sleeves, trousers).
// The UVs follow the standard 64x64 skin layout. Units are Blockbench pixels (16 = one block); a player is 32 tall.
// The model faces -Z, so the player's right side is +X. Animation rotations follow Blockbench: a NEGATIVE x rotation
// leans the top forward, so a POSITIVE x rotation swings a hanging arm or leg forward.
const fs = require('fs');
const crypto = require('crypto');

const uuid = (seed) => {
  const h = crypto.createHash('md5').update(String(seed)).digest('hex');
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20, 32)}`;
};

const TEX = 64;

// ---------------------------------------------------------------- model building
const elements = [];
const groups = {};

/**
 * The faces of a box-UV cube of size w x h x d whose net starts at (u, v) on the skin, the same layout Blockbench
 * (and Minecraft) use for player skins. up and down are flipped the way Blockbench flips them.
 */
function skinFaces(u, v, w, h, d) {
  return {
    north: { uv: [u + d, v + d, u + d + w, v + d + h], texture: 0 },
    east: { uv: [u, v + d, u + d, v + d + h], texture: 0 },
    south: { uv: [u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h], texture: 0 },
    west: { uv: [u + d + w, v + d, u + 2 * d + w, v + d + h], texture: 0 },
    up: { uv: [u + d + w, v + d, u + d, v], texture: 0 },
    down: { uv: [u + d + 2 * w, v, u + d + w, v + d], texture: 0 },
  };
}

/** A skin cube: from/to in model space, its net at (u, v), grown by `inflate` on every side (outer layer) without changing its UVs. */
function part(name, from, to, u, v, inflate = 0) {
  const w = to[0] - from[0], h = to[1] - from[1], d = to[2] - from[2];
  const id = uuid('el:' + name);
  elements.push({
    name, box_uv: false, rescale: false, locked: false, light_emission: 0, render_order: 'default',
    allow_mirror_modeling: true,
    from: from.map((c) => c - inflate), to: to.map((c) => c + inflate),
    autouv: 0, color: 0, origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2],
    faces: skinFaces(u, v, w, h, d), type: 'cube', uuid: id,
  });
  return id;
}

/** A cube that is never drawn: BetterModel reads its size (hitbox, shadow, name tag position) but nothing is rendered. */
function marker(name, from, to) {
  const id = uuid('el:' + name);
  const faces = {};
  for (const dir of ['north', 'east', 'south', 'west', 'up', 'down']) faces[dir] = { uv: [0, 0, 2, 2] };
  elements.push({
    name, box_uv: false, rescale: false, locked: false, light_emission: 0, render_order: 'default',
    allow_mirror_modeling: true, from, to, autouv: 0, color: 0,
    origin: [(from[0] + to[0]) / 2, (from[1] + to[1]) / 2, (from[2] + to[2]) / 2], faces, type: 'cube', uuid: id,
  });
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

// Where each part's net starts on the skin: [base layer u, v, outer layer u, v].
const SKIN = {
  head: [0, 0, 32, 0],
  body: [16, 16, 16, 32],
  rightArm: [40, 16, 40, 32],
  leftArm: [32, 48, 48, 48],
  rightLeg: [0, 16, 0, 32],
  leftLeg: [16, 48, 0, 48],
};
const HAT = 0.5, LAYER = 0.25;      // how much bigger the outer layer is, as in Minecraft

/** A body part and its outer layer, in one bone pivoting at `pivot`. */
function limb(bone, key, from, to, pivot, inflate) {
  const [u, v, ou, ov] = SKIN[key];
  return group(bone, pivot, [part(key, from, to, u, v), part(key + '_layer', from, to, ou, ov, inflate)]);
}

function buildOutliner() {
  const head = limb('h_head', 'head', [-4, 24, -4], [4, 32, 4], [0, 24, 0], HAT);
  const torso = limb('torso', 'body', [-4, 12, -2], [4, 24, 2], [0, 24, 0], LAYER);
  const rightArm = limb('right_arm', 'rightArm', [4, 12, -2], [8, 24, 2], [5, 22, 0], LAYER);
  const leftArm = limb('left_arm', 'leftArm', [-8, 12, -2], [-4, 24, 2], [-5, 22, 0], LAYER);
  const rightLeg = limb('right_leg', 'rightLeg', [0, 0, -2], [4, 12, 2], [2, 12, 0], LAYER);
  const leftLeg = limb('left_leg', 'leftLeg', [-4, 0, -2], [0, 12, 2], [-2, 12, 0], LAYER);

  // the upper body (head, torso, arms) can lean as one; the legs stay planted
  const upper = group('upper', [0, 12, 0], [torso, head, rightArm, leftArm]);
  const body = group('body', [0, 0, 0], [upper, rightLeg, leftLeg]);

  return [
    group('hitbox', [0, 0, 0], [marker('hitbox', [-4.8, 0, -4.8], [4.8, 32, 4.8])], { visibility: false }),
    group('shadow', [0, 0, 0], [marker('shadow', [-5, 0, -5], [5, 0, 5])]),
    group('tag_name', [0, 35, 0], [marker('tag_name', [-1, 34, -1], [1, 36, 1])]),
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
  // idle: breathes, the arms sway a little away from the body
  anim('idle', 'loop', 3, [
    R('right_arm', [[0, 0, 0, 3], [1.5, 2, 0, 6], [3, 0, 0, 3]]),
    R('left_arm', [[0, 0, 0, -3], [1.5, 2, 0, -6], [3, 0, 0, -3]]),
    P('upper', [[0, 0, 0, 0], [1.5, 0, 0.3, 0], [3, 0, 0, 0]]),
  ]);

  // walk: the usual player stride, arms swinging against the legs
  anim('walk', 'loop', 0.8, [
    R('right_leg', [[0, 35, 0, 0], [0.4, -35, 0, 0], [0.8, 35, 0, 0]]),
    R('left_leg', [[0, -35, 0, 0], [0.4, 35, 0, 0], [0.8, -35, 0, 0]]),
    R('right_arm', [[0, -30, 0, 0], [0.4, 30, 0, 0], [0.8, -30, 0, 0]]),
    R('left_arm', [[0, 30, 0, 0], [0.4, -30, 0, 0], [0.8, 30, 0, 0]]),
  ]);

  // attack: the right arm comes up and chops down, with the body turning into it; the hit lands at about
  // 0.3 s (6 ticks), which is what attack-hit-delay-ticks in abilities.yml should match
  anim('attack', 'once', 0.6, [
    R('right_arm', [[0, 0, 0, 0], [0.15, 120, 0, 0], [0.3, 30, 0, 0], [0.6, 0, 0, 0]]),
    R('upper', [[0, 0, 0, 0], [0.15, 0, -15, 0], [0.3, -8, 15, 0], [0.6, 0, 0, 0]]),
  ]);

  // death: falls over sideways, as a player does
  anim('death', 'once', 1.0, [
    R('body', [[0, 0, 0, 0], [0.5, 0, 0, 60], [1.0, 0, 0, 90]]),
  ]);
}

// ---------------------------------------------------------------- assemble the .bbmodel
function build(skin) {
  const outliner = buildOutliner();
  buildAnimations();
  return {
    meta: { format_version: '4.10', model_format: 'free', box_uv: false },
    name: 'pink', model_identifier: '', visible_box: [1, 1, 0], variable_placeholders: '',
    variable_placeholder_buttons: [], timeline_setups: [], unhandled_root_fields: {},
    resolution: { width: TEX, height: TEX },
    elements, outliner,
    textures: [{
      path: '', name: 'pink.png', folder: '', namespace: '', id: '0', width: TEX, height: TEX,
      uv_width: TEX, uv_height: TEX, particle: false, use_as_default: false, layers_enabled: false,
      sync_to_project: '', render_mode: 'default', render_sides: 'auto', pbr_channel: 'color', frame_time: 1,
      frame_order_type: 'loop', frame_order: '', frame_interpolate: false, visible: true, internal: true,
      saved: false, uuid: uuid('texture'), source: 'data:image/png;base64,' + skin.toString('base64'),
    }],
    animations: anims,
  };
}

function validate(model, skin) {
  const problems = [];
  if (skin.readUInt32BE(16) !== TEX || skin.readUInt32BE(20) !== TEX) problems.push('the skin must be 64x64');
  const names = new Set(Object.keys(groups));
  for (const need of ['hitbox', 'shadow', 'tag_name', 'h_head', 'body', 'right_arm', 'left_arm', 'right_leg', 'left_leg']) {
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

const [, , skinFile, outFile] = process.argv;
if (!skinFile || !outFile) { console.error('usage: node make_pink.js <skin.png> <out.bbmodel>'); process.exit(1); }
const skin = fs.readFileSync(skinFile);
const model = build(skin);
const problems = validate(model, skin);
if (problems.length) { console.error('PROBLEMS:\n' + problems.join('\n')); process.exit(2); }
fs.writeFileSync(outFile, JSON.stringify(model));
console.log(`wrote ${outFile}: ${model.elements.length} cubes, ${Object.keys(groups).length} bones, ` +
  `animations: ${model.animations.map((a) => a.name + '(' + a.length + 's)').join(', ')}`);
