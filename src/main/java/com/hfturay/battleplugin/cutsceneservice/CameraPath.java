package com.hfturay.battleplugin.cutsceneservice;

import java.util.List;

/**
 * A camera route: keyframes the camera flies through, as a smooth curve.
 *
 * Keyframe positions are relative to an anchor, so one path works wherever the scene is played: x is to the anchor's
 * right, y is up, z is forward (the way the anchor faces). The yaw is added to the anchor's yaw. Yaw values are
 * continuous, not wrapped: a full circle is 0, -90, -180, -270, -360, so the camera turns the long way when asked to.
 *
 * Each keyframe says how many ticks the leg that STARTS at it takes (the last keyframe's ticks are ignored) and how
 * that leg is eased. Positions follow a Catmull-Rom curve through the keyframes; yaw and pitch are blended linearly.
 * This class does no server work, so it can be checked without a server.
 */
public final class CameraPath {

    /** How progress along one leg is shaped. Use EASE_IN on the first leg and EASE_OUT on the last for a soft start and stop. */
    public enum Easing {
        LINEAR, EASE_IN, EASE_OUT, EASE_IN_OUT;

        double apply(double t) {
            return switch (this) {
                case LINEAR -> t;
                case EASE_IN -> t * t;
                case EASE_OUT -> 1 - (1 - t) * (1 - t);
                case EASE_IN_OUT -> t * t * (3 - 2 * t);
            };
        }
    }

    /** right/up/forward in blocks from the anchor; yaw in degrees added to the anchor's yaw; pitch in degrees (down is positive). */
    public record Keyframe(double right, double up, double forward, double yaw, double pitch, int ticks, Easing easing) {
        public Keyframe {
            if (ticks < 1) {
                ticks = 1;
            }
            if (easing == null) {
                easing = Easing.LINEAR;
            }
        }

        /** A keyframe with linear easing. */
        public static Keyframe of(double right, double up, double forward, double yaw, double pitch, int ticks) {
            return new Keyframe(right, up, forward, yaw, pitch, ticks, Easing.LINEAR);
        }
    }

    /** Where the camera is at one moment, still relative to the anchor. */
    public record Pose(double right, double up, double forward, double yaw, double pitch) { }

    private final List<Keyframe> frames;
    private final int totalTicks;

    public CameraPath(List<Keyframe> frames) {
        if (frames.size() < 2) {
            throw new IllegalArgumentException("a camera path needs at least two keyframes");
        }
        this.frames = List.copyOf(frames);
        int total = 0;
        for (int i = 0; i < frames.size() - 1; i++) {
            total += frames.get(i).ticks();
        }
        this.totalTicks = total;
    }

    /** How many ticks the whole route takes. */
    public int totalTicks() {
        return totalTicks;
    }

    /** Where the camera is at this tick (0 = start). Past the end it stays on the last keyframe. */
    public Pose pose(int tick) {
        if (tick <= 0) {
            return of(frames.get(0));
        }
        if (tick >= totalTicks) {
            return of(frames.get(frames.size() - 1));
        }

        int start = 0;
        int leg = 0;
        while (leg < frames.size() - 2 && tick >= start + frames.get(leg).ticks()) {
            start += frames.get(leg).ticks();
            leg++;
        }

        Keyframe a = frames.get(leg);
        Keyframe b = frames.get(leg + 1);
        double u = a.easing().apply((tick - start) / (double) a.ticks());

        Keyframe before = leg > 0 ? frames.get(leg - 1) : a;
        Keyframe after = leg + 2 < frames.size() ? frames.get(leg + 2) : b;

        return new Pose(
            spline(before.right(), a.right(), b.right(), after.right(), u),
            spline(before.up(), a.up(), b.up(), after.up(), u),
            spline(before.forward(), a.forward(), b.forward(), after.forward(), u),
            a.yaw() + (b.yaw() - a.yaw()) * u,
            a.pitch() + (b.pitch() - a.pitch()) * u);
    }

    private static Pose of(Keyframe k) {
        return new Pose(k.right(), k.up(), k.forward(), k.yaw(), k.pitch());
    }

    /** Uniform Catmull-Rom: a smooth curve from p1 to p2 (u from 0 to 1) that leans on its neighbours p0 and p3. */
    private static double spline(double p0, double p1, double p2, double p3, double u) {
        double u2 = u * u;
        double u3 = u2 * u;
        return 0.5 * ((2 * p1)
            + (-p0 + p2) * u
            + (2 * p0 - 5 * p1 + 4 * p2 - p3) * u2
            + (-p0 + 3 * p1 - 3 * p2 + p3) * u3);
    }
}
