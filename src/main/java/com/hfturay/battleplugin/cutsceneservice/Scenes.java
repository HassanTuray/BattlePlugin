package com.hfturay.battleplugin.cutsceneservice;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;

import com.hfturay.battleplugin.cutsceneservice.CameraPath.Easing;
import com.hfturay.battleplugin.cutsceneservice.CameraPath.Keyframe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

/** The scenes we have. For now just the test scene used to check the camera. */
public final class Scenes {
    private Scenes() { }

    /**
     * 10 seconds: the camera orbits three quarters of a circle (radius 9) around the anchor while rising, then pulls
     * up and back. Titles, sounds and particles mark the beats.
     */
    public static Scene test() {
        double radius = 9;
        List<Keyframe> frames = new ArrayList<>();
        // Points every 45 degrees make a smooth circle. Yaw is minus the angle so the camera keeps facing the
        // centre; yaw is continuous (never wrapped).
        for (int i = 0; i <= 5; i++) {
            double angle = Math.toRadians(45 * i);
            Easing easing = i == 0 ? Easing.EASE_IN : Easing.LINEAR;
            double height = 3 + i * 0.6;
            frames.add(new Keyframe(radius * Math.sin(angle), height, -radius * Math.cos(angle),
                -45 * i, lookDown(height, radius), 26, easing));
        }
        // the sixth orbit point (270 degrees); the leg leaving it is the long pull up and away, still circling
        double last = Math.toRadians(270);
        frames.add(new Keyframe(radius * Math.sin(last), 6.6, -radius * Math.cos(last), -270, lookDown(6.6, radius), 44, Easing.EASE_OUT));
        // ends at 300 degrees round the circle, further out and higher, so the yaw (-300) still faces the centre
        double end = Math.toRadians(300);
        frames.add(Keyframe.of(14 * Math.sin(end), 13, -14 * Math.cos(end), -300, lookDown(13, 14), 1));

        CameraPath path = new CameraPath(frames);   // 6 * 26 + 44 = 200 ticks = 10 seconds

        return Scene.builder("test", path)
            .at(0, ctx -> {
                title(ctx.viewers(), "DOMAIN EXPANSION", "Cutscene test", NamedTextColor.DARK_PURPLE);
                sound(ctx.viewers(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f);
            })
            .at(50, ctx -> {
                ring(ctx.anchor(), 6, Particle.SOUL_FIRE_FLAME);
                sound(ctx.viewers(), Sound.ENTITY_WARDEN_HEARTBEAT, 1.0f);
            })
            .at(100, ctx -> {
                ring(ctx.anchor(), 10, Particle.END_ROD);
                sound(ctx.viewers(), Sound.BLOCK_END_PORTAL_SPAWN, 0.6f);
            })
            .at(150, ctx -> {
                ring(ctx.anchor(), 14, Particle.SOUL_FIRE_FLAME);
                sound(ctx.viewers(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.8f);
            })
            .at(195, ctx -> sound(ctx.viewers(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f))
            .build();
    }

    /** One line of text across the screen (the subtitle size, so a long sentence fits), shown for about 4 seconds. */
    public static void line(List<Player> viewers, String text, NamedTextColor color) {
        Title title = Title.title(Component.empty(), Component.text(text, color),
            Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(1000)));
        for (Player player : viewers) {
            player.showTitle(title);
        }
    }

    /** The pitch that aims a camera at chest height (1.2 blocks) at the centre, from this height and distance. */
    private static double lookDown(double cameraHeight, double distance) {
        return lookAt(cameraHeight, 1.2, distance);
    }

    /** The pitch that aims a camera at a point this high, this far away (down is positive, so looking up is negative). */
    public static double lookAt(double cameraHeight, double aimHeight, double distance) {
        return Math.toDegrees(Math.atan2(cameraHeight - aimHeight, distance));
    }

    public static void title(List<Player> viewers, String main, String sub, NamedTextColor color) {
        Title title = Title.title(Component.text(main, color), Component.text(sub, NamedTextColor.GRAY),
            Title.Times.times(Duration.ofMillis(300), Duration.ofSeconds(2), Duration.ofMillis(600)));
        for (Player player : viewers) {
            player.showTitle(title);
        }
    }

    public static void sound(List<Player> viewers, Sound sound, float pitch) {
        for (Player player : viewers) {
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, 1.0f, pitch);
        }
    }

    private static void ring(Location center, double radius, Particle particle) {
        for (int i = 0; i < 48; i++) {
            double angle = Math.PI * 2 * i / 48;
            center.getWorld().spawnParticle(particle,
                center.getX() + Math.cos(angle) * radius, center.getY() + 0.5, center.getZ() + Math.sin(angle) * radius,
                2, 0.1, 0.8, 0.1, 0.01);
        }
    }
}
