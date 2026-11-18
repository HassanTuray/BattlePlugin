package com.hfturay.battleplugin.particleservice;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

/**
 * Cheap particle shapes drawn straight with World.spawnParticle: no EffectLib effect, no scheduler task.
 * Shape points are computed once when the spec is built; each frame only scales and places them.
 *
 * Local space (when orient(true)): +z is the anchor's direction, +y is up. With orient(false) it is world space.
 *
 * Shape meaning of the numbers:
 *   POINT     count = particles per frame in one packet, spread = random jitter box
 *   RING      count = points on a flat circle of `radius` (horizontal)
 *   DISC      count = points on a circle facing the direction of travel
 *   SPHERE    count = points on the surface
 *   CYLINDER  count = points over `layers` rings, `height` tall, standing on the anchor point
 *   CUBE      radius = half edge length, count = points (outline, or filled when fill(true))
 *   LINE      count = particles per block, from anchor to target
 *   HELIX     count = particles per block, spiralling around the anchor-to-target line
 */
public final class DirectSpec implements ParticleSpec {

    public enum Shape { POINT, RING, DISC, SPHERE, CYLINDER, CUBE, LINE, HELIX }

    private static final double GOLDEN_ANGLE = Math.PI * (3.0 - Math.sqrt(5.0));

    private final Particle particle;
    private final Particle.DustOptions[] dust;
    private final BlockData blockData;      // for the particles that show a block (BLOCK, FALLING_DUST, DUST_PILLAR)
    private final Shape shape;
    private final int count;
    private final double radius;
    private final double growth;
    private final double maxRadius;
    private final double height;
    private final double speed;
    private final double turns;
    private final double travel;
    private final double spreadX, spreadY, spreadZ;
    private final double offX, offY, offZ;
    private final boolean orient;
    private final int interval;
    private final int lifetime;
    private final double[] unit;

    private DirectSpec(Builder b) {
        this.particle = b.particle;
        this.dust = b.dust != null ? b.dust
                : (b.particle == Particle.DUST ? new Particle.DustOptions[] { new Particle.DustOptions(Color.WHITE, 1f) } : null);
        this.blockData = b.blockData;
        if (b.particle.getDataType() == BlockData.class && b.blockData == null) {
            throw new IllegalStateException(b.particle + " needs a block: call .block(...) on the builder");
        }
        this.shape = b.shape;
        this.count = Math.max(1, b.count);
        this.radius = b.radius;
        this.growth = b.growth;
        this.maxRadius = b.maxRadius > 0 ? b.maxRadius : Double.MAX_VALUE;
        this.height = b.height;
        this.speed = b.speed;
        this.turns = b.turns;
        this.travel = b.travel;
        this.spreadX = b.spreadX;
        this.spreadY = b.spreadY;
        this.spreadZ = b.spreadZ;
        this.offX = b.offX;
        this.offY = b.offY;
        this.offZ = b.offZ;
        this.orient = b.orient;
        this.interval = Math.max(1, b.interval);
        this.lifetime = Math.max(1, b.lifetime);
        this.unit = buildUnitPoints(b);
    }

    public static Builder of(Particle particle) {
        return new Builder(particle);
    }

    @Override
    public ParticleInstance create(PlayContext context) {
        if ((shape == Shape.LINE || shape == Shape.HELIX) && context.target() == null) {
            throw new IllegalStateException(shape + " needs a target anchor (use ParticleService.beam)");
        }
        return new Instance(context);
    }

    // ---- unit shape generation ----

    private static double[] buildUnitPoints(Builder b) {
        int n = Math.max(1, b.count);
        switch (b.shape) {
            case RING: {
                double[] p = new double[n * 3];
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n;
                    p[i * 3] = Math.cos(a);
                    p[i * 3 + 2] = Math.sin(a);
                }
                return p;
            }
            case DISC: {
                double[] p = new double[n * 3];
                for (int i = 0; i < n; i++) {
                    double a = 2 * Math.PI * i / n;
                    p[i * 3] = Math.cos(a);
                    p[i * 3 + 1] = Math.sin(a);
                }
                return p;
            }
            case SPHERE: {
                double[] p = new double[n * 3];
                for (int i = 0; i < n; i++) {
                    double y = 1 - 2 * (i + 0.5) / n;
                    double r = Math.sqrt(Math.max(0, 1 - y * y));
                    double phi = i * GOLDEN_ANGLE;
                    p[i * 3] = Math.cos(phi) * r;
                    p[i * 3 + 1] = y;
                    p[i * 3 + 2] = Math.sin(phi) * r;
                }
                return p;
            }
            case CYLINDER: {
                int layers = Math.max(1, b.layers);
                int perRing = Math.max(3, n / layers);
                int rings = b.fill ? 2 : 1;
                double[] p = new double[layers * perRing * rings * 3];
                int k = 0;
                for (int l = 0; l < layers; l++) {
                    double y = layers == 1 ? 0 : (double) l / (layers - 1);
                    for (int ring = 0; ring < rings; ring++) {
                        double scale = ring == 0 ? 1.0 : 0.5;
                        for (int i = 0; i < perRing; i++) {
                            double a = 2 * Math.PI * i / perRing;
                            p[k++] = Math.cos(a) * scale;
                            p[k++] = y;
                            p[k++] = Math.sin(a) * scale;
                        }
                    }
                }
                return p;
            }
            case CUBE: {
                if (b.fill) {
                    int side = Math.max(2, (int) Math.ceil(Math.cbrt(n)));
                    double[] p = new double[side * side * side * 3];
                    int k = 0;
                    for (int x = 0; x < side; x++) {
                        for (int y = 0; y < side; y++) {
                            for (int z = 0; z < side; z++) {
                                p[k++] = -1 + 2.0 * x / (side - 1);
                                p[k++] = -1 + 2.0 * y / (side - 1);
                                p[k++] = -1 + 2.0 * z / (side - 1);
                            }
                        }
                    }
                    return p;
                }
                int perEdge = Math.max(2, n / 12);
                double[] p = new double[12 * perEdge * 3];
                int k = 0;
                for (int axis = 0; axis < 3; axis++) {
                    for (int corner = 0; corner < 4; corner++) {
                        double s1 = (corner & 1) == 0 ? -1 : 1;
                        double s2 = (corner & 2) == 0 ? -1 : 1;
                        for (int i = 0; i < perEdge; i++) {
                            double t = -1 + 2.0 * i / (perEdge - 1);
                            double[] v = new double[3];
                            v[axis] = t;
                            v[(axis + 1) % 3] = s1;
                            v[(axis + 2) % 3] = s2;
                            p[k++] = v[0];
                            p[k++] = v[1];
                            p[k++] = v[2];
                        }
                    }
                }
                return p;
            }
            default:
                return null;
        }
    }

    // ---- running ----

    private static final class Basis {
        static final Basis IDENTITY = new Basis(1, 0, 0, 0, 1, 0, 0, 0, 1);

        final double rx, ry, rz, ux, uy, uz, fx, fy, fz;

        Basis(double rx, double ry, double rz, double ux, double uy, double uz, double fx, double fy, double fz) {
            this.rx = rx; this.ry = ry; this.rz = rz;
            this.ux = ux; this.uy = uy; this.uz = uz;
            this.fx = fx; this.fy = fy; this.fz = fz;
        }

        static Basis facing(Vector direction) {
            Vector f = direction.clone();
            if (f.lengthSquared() < 1.0E-9) {
                return IDENTITY;
            }
            f.normalize();
            Vector ref = Math.abs(f.getY()) > 0.99 ? new Vector(1, 0, 0) : new Vector(0, 1, 0);
            Vector r = ref.clone().crossProduct(f).normalize();
            Vector u = f.clone().crossProduct(r).normalize();
            return new Basis(r.getX(), r.getY(), r.getZ(), u.getX(), u.getY(), u.getZ(), f.getX(), f.getY(), f.getZ());
        }

        double x(double lx, double ly, double lz) { return rx * lx + ux * ly + fx * lz; }
        double y(double lx, double ly, double lz) { return ry * lx + uy * ly + fy * lz; }
        double z(double lx, double ly, double lz) { return rz * lx + uz * ly + fz * lz; }
    }

    private final class Instance implements ParticleInstance {
        private final PlayContext ctx;
        private final int finishAge;
        private int nextAge = 0;

        Instance(PlayContext ctx) {
            this.ctx = ctx;
            int frames = lifetime;
            if (travel > 0 && (shape == Shape.LINE || shape == Shape.HELIX)) {
                Location a = ctx.anchor().location();
                Location b = ctx.target().location();
                if (a != null && b != null && a.getWorld() == b.getWorld()) {
                    frames = Math.max(1, (int) Math.ceil(a.distance(b) / travel));
                }
            }
            this.finishAge = frames * interval;
        }

        @Override
        public void tick(int age) {
            nextAge = age + 1;
            if (age % interval == 0) {
                render(age / interval);
            }
        }

        @Override
        public boolean isFinished() {
            return ctx.duration() == ParticleService.UNTIL_DONE && nextAge >= finishAge;
        }

        @Override
        public void stop() {
        }

        private void render(int frame) {
            Location a = ctx.anchor().location();
            if (a == null || a.getWorld() == null) {
                return;
            }
            World world = a.getWorld();
            Basis basis = orient ? Basis.facing(ctx.anchor().direction()) : Basis.IDENTITY;

            double bx = a.getX() + basis.x(offX, offY, offZ);
            double by = a.getY() + basis.y(offX, offY, offZ);
            double bz = a.getZ() + basis.z(offX, offY, offZ);

            int stride = Math.max(1, (int) Math.round(1.0 / Math.max(0.1, ctx.density())));

            switch (shape) {
                case POINT -> {
                    int n = Math.max(1, (int) Math.round(count * ctx.density()));
                    emit(world, bx, by, bz, n, spreadX, spreadY, spreadZ, frame);
                }
                case LINE, HELIX -> renderBeam(world, bx, by, bz, frame, stride);
                default -> {
                    double r = Math.min(maxRadius, radius + growth * frame);
                    double sx = r;
                    double sy = shape == Shape.CYLINDER ? height : r;
                    for (int i = 0; i < unit.length / 3; i += stride) {
                        double lx = unit[i * 3] * sx;
                        double ly = unit[i * 3 + 1] * sy;
                        double lz = unit[i * 3 + 2] * sx;
                        emit(world, bx + basis.x(lx, ly, lz), by + basis.y(lx, ly, lz), bz + basis.z(lx, ly, lz),
                                1, 0, 0, 0, i);
                    }
                }
            }
        }

        private void renderBeam(World world, double sx, double sy, double sz, int frame, int stride) {
            Location end = ctx.target().location();
            if (end == null) {
                return;
            }

            double dx = end.getX() - sx;
            double dy = end.getY() - sy;
            double dz = end.getZ() - sz;
            double length = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (length < 1.0E-6) {
                return;
            }
            double nx = dx / length, ny = dy / length, nz = dz / length;

            double from = 0;
            double to = length;
            if (travel > 0) {
                from = frame * travel;
                to = Math.min(length, (frame + 1) * travel);
                if (from >= length) {
                    return;
                }
            }

            // two vectors perpendicular to the beam, for the helix
            double refX = Math.abs(ny) > 0.99 ? 1 : 0;
            double refY = Math.abs(ny) > 0.99 ? 0 : 1;
            double ax = refY * nz;
            double ay = -refX * nz;
            double az = refX * ny - refY * nx;
            double aLen = Math.sqrt(ax * ax + ay * ay + az * az);
            ax /= aLen; ay /= aLen; az /= aLen;
            double bx2 = ny * az - nz * ay;
            double by2 = nz * ax - nx * az;
            double bz2 = nx * ay - ny * ax;

            double step = stride / (double) count;
            int index = 0;
            for (double d = from; d <= to; d += step, index++) {
                double x = sx + nx * d;
                double y = sy + ny * d;
                double z = sz + nz * d;

                if (shape == Shape.HELIX) {
                    double angle = 2 * Math.PI * turns * d + frame * 0.6;
                    double cos = Math.cos(angle) * radius;
                    double sin = Math.sin(angle) * radius;
                    x += ax * cos + bx2 * sin;
                    y += ay * cos + by2 * sin;
                    z += az * cos + bz2 * sin;
                }
                emit(world, x, y, z, 1, 0, 0, 0, index);
            }
        }

        private void emit(World world, double x, double y, double z, int n,
                          double jx, double jy, double jz, int colorIndex) {
            Object data = dust != null ? dust[Math.floorMod(colorIndex, dust.length)] : blockData;
            world.spawnParticle(particle, x, y, z, n, jx, jy, jz, speed, data);
        }
    }

    // ---- builder ----

    public static final class Builder {
        private final Particle particle;
        private Particle.DustOptions[] dust;
        private BlockData blockData;
        private Shape shape = Shape.POINT;
        private int count = 1;
        private double radius = 1;
        private double growth = 0;
        private double maxRadius = 0;
        private double height = 1;
        private double speed = 0;
        private double turns = 1;
        private double travel = 0;
        private double spreadX, spreadY, spreadZ;
        private double offX, offY, offZ;
        private boolean orient = false;
        private boolean fill = false;
        private int layers = 1;
        private int interval = 1;
        private int lifetime = 1;

        private Builder(Particle particle) {
            this.particle = particle;
        }

        /** Dust colour(s). With several colours, points cycle through them. */
        public Builder dust(float size, Color... colors) {
            this.dust = new Particle.DustOptions[colors.length];
            for (int i = 0; i < colors.length; i++) {
                this.dust[i] = new Particle.DustOptions(colors[i], size);
            }
            return this;
        }

        /** The block to show, for the particles made of block fragments (Particle.BLOCK is the "breaking" crumbs). */
        public Builder block(BlockData block) { this.blockData = block; return this; }

        public Builder shape(Shape shape) { this.shape = shape; return this; }
        public Builder count(int count) { this.count = count; return this; }
        public Builder radius(double radius) { this.radius = radius; return this; }
        public Builder height(double height) { this.height = height; return this; }
        public Builder speed(double speed) { this.speed = speed; return this; }
        public Builder spread(double x, double y, double z) { this.spreadX = x; this.spreadY = y; this.spreadZ = z; return this; }
        /** Shift from the anchor, in local space when orient(true) (negative z = behind). */
        public Builder offset(double x, double y, double z) { this.offX = x; this.offY = y; this.offZ = z; return this; }
        public Builder orient(boolean orient) { this.orient = orient; return this; }
        public Builder fill(boolean fill) { this.fill = fill; return this; }
        public Builder layers(int layers) { this.layers = layers; return this; }
        /** Render every N ticks instead of every tick. */
        public Builder interval(int interval) { this.interval = interval; return this; }
        /** Frames to play when the spec has no fixed duration (burst). */
        public Builder lifetime(int frames) { this.lifetime = frames; return this; }
        /** HELIX: full turns per block. */
        public Builder turns(double turns) { this.turns = turns; return this; }
        /** LINE/HELIX: reveal this many blocks per tick instead of drawing the whole beam at once. */
        public Builder travel(double blocksPerTick) { this.travel = blocksPerTick; return this; }

        /** Radius grows from `from` to `to` over `ticks` frames, and the burst ends when it gets there. */
        public Builder expanding(double from, double to, int ticks) {
            this.radius = from;
            this.maxRadius = to;
            this.growth = (to - from) / Math.max(1, ticks);
            this.lifetime = Math.max(1, ticks);
            return this;
        }

        public DirectSpec build() {
            return new DirectSpec(this);
        }
    }
}
