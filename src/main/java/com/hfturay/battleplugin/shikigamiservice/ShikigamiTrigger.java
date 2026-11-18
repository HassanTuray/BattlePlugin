package com.hfturay.battleplugin.shikigamiservice;

/**
 * "When this is true, do that, then wait this long." How a shikigami uses abilities of its own: owner low on health,
 * target in range, and so on. A shikigami may have any number (the warrior has none).
 */
public record ShikigamiTrigger(Condition when, int cooldownTicks, Action action) {

    @FunctionalInterface
    public interface Condition {
        boolean test(BrainShikigami self);

        default Condition and(Condition other) {
            return self -> test(self) && other.test(self);
        }

        default Condition or(Condition other) {
            return self -> test(self) || other.test(self);
        }
    }

    @FunctionalInterface
    public interface Action {
        void run(BrainShikigami self);
    }

    /** The ready-made conditions. */
    public static final class Conditions {
        private Conditions() { }

        /** The owner has this many health points (half hearts) or fewer. */
        public static Condition ownerHealthAtMost(double hp) {
            return self -> self.getOwner().getPlayer().getHealth() <= hp;
        }

        public static Condition hasTarget() {
            return self -> self.target() != null;
        }

        /** The target is within this many blocks. */
        public static Condition targetWithin(double blocks) {
            return self -> self.target() != null
                && self.target().getLocation().distanceSquared(self.mob().getLocation()) <= blocks * blocks;
        }

        /** The shikigami itself is at or below this fraction of its health (0.5 = half). */
        public static Condition selfHealthBelow(double fraction) {
            return self -> {
                var max = self.mob().getAttribute(org.bukkit.attribute.Attribute.GENERIC_MAX_HEALTH);
                return max != null && self.mob().getHealth() <= max.getValue() * fraction;
            };
        }
    }
}
