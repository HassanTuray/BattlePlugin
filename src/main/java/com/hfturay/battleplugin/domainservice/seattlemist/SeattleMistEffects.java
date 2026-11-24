package com.hfturay.battleplugin.domainservice.seattlemist;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.hfturay.battleplugin.cutsceneservice.CameraPath;
import com.hfturay.battleplugin.cutsceneservice.Scene;
import com.hfturay.battleplugin.cutsceneservice.Scenes;
import com.hfturay.battleplugin.domainservice.DomainService.Behavior;
import com.hfturay.battleplugin.domainservice.DomainService.Domain;
import com.hfturay.battleplugin.domainservice.DomainService.Phase;
import com.hfturay.battleplugin.shikigamiservice.Shikigami;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.title.Title;

/**
 * Domain: Seattle Mist. Once the dome closes, a cutscene holds the camera still, high up near the dome's back wall and
 * looking at the middle, while a mist rises until the camera can barely see; then "The Mist is Here" appears in black.
 * The mist stays over everyone in the dome for the rest of the domain, and the Mist Stalker is let loose in it.
 *
 * The mist is drawn by the MistFog resource pack (resourcepack/mistfog: a shader that replaces the game's fog). The
 * server switches it on for one player by telling only that player's game that they (or, in the cutscene, the camera
 * they look through) have Blindness. The shader recognises blindness's fog and draws white mist instead, as thick as
 * the ticks of blindness left say: n ticks (up to 20) is n / 20 of full thickness. A fresh count is sent every tick,
 * always even (the shader reads 2k and 2k - 1 the same, so the mist does not flicker). The server never applies the
 * blindness itself, and takes it off each player's game when they leave the dome or the domain ends.
 *
 * One of these is made for every cast, with the numbers read from abilities.yml at that moment.
 */
public final class SeattleMistEffects implements Behavior {

    /**
     * The numbers. thickness: how thick the mist is during the domain (0 = none, 1 = the thickest the pack draws, where
     * anything 10 blocks away is gone). affectsCaster: false leaves the caster seeing clearly.
     */
    public record Settings(double thickness, boolean affectsCaster) { }

    /** The cutscene: the mist rises over MIST_TICKS, then the words appear; the scene ends at SCENE_TICKS. */
    private static final int MIST_TICKS = 160;
    private static final int WORDS_TICK = 165;
    private static final int SCENE_TICKS = 230;
    /** Blindness ticks for full thickness (the game treats 20 or more as fully blind). */
    private static final int FULL = 20;

    /**
     * "The Mist is Here": black, bold, with a space between letters (three between words) so they do not run together,
     * and no text shadow (a title always gets one, and a dark shadow under black letters smudges them). The shadow is
     * switched off by giving it a see-through colour, shadow_color 0, which Minecraft 1.21.4 and later understand. The
     * plugin is built against an older API with no way to say that, so the words are written in Minecraft's own text
     * format and read by the server, whose newer Adventure library knows shadow_color.
     */
    private static final Component WORDS = words(spaced("The Mist is Here"));

    /** The words without a shadow, or (if the server cannot read that) plain black bold words with one. */
    private static Component words(String text) {
        try {
            return GsonComponentSerializer.gson().deserialize(
                "{\"text\":\"" + text + "\",\"color\":\"black\",\"bold\":true,\"shadow_color\":0}");
        } catch (RuntimeException e) {
            return Component.text(text, NamedTextColor.BLACK, TextDecoration.BOLD);
        }
    }

    /** One space between letters and three between words: "T h e   M i s t". */
    private static String spaced(String words) {
        StringBuilder out = new StringBuilder();
        for (String word : words.split(" ")) {
            if (out.length() > 0) {
                out.append("   ");
            }
            out.append(String.join(" ", word.split("")));
        }
        return out.toString();
    }

    private final Settings settings;
    /** Lets the Mist Stalker loose (SeattleMist knows how to make one). */
    private final Function<Location, Shikigami> stalker;

    private Domain domain;
    /** Everyone whose game currently has the mist switched on. */
    private final Set<UUID> misted = new HashSet<>();
    private Shikigami released;

    public SeattleMistEffects(Settings settings, Function<Location, Shikigami> stalker) {
        this.settings = settings;
        this.stalker = stalker;
    }

    // ---- the cutscene ----

    /**
     * About 11 seconds. The camera does not move: it stays near the back wall of the dome, high up, looking down at the
     * middle so most of the dome can be seen. It is a living camera, so it can carry the mist: every tick the mist is
     * set a little thicker, from none to full over MIST_TICKS, then the words appear.
     */
    public Scene scene(double radius) {
        double back = radius * 0.7;
        double up = radius * 0.45;
        double pitch = Scenes.lookAt(up, 1.2, back);
        // the same spot at the start and the end: the camera holds still
        CameraPath still = new CameraPath(List.of(
            new CameraPath.Keyframe(0, up, -back, 0, pitch, SCENE_TICKS, CameraPath.Easing.LINEAR),
            new CameraPath.Keyframe(0, up, -back, 0, pitch, 1, CameraPath.Easing.LINEAR)));

        Scene.Builder scene = Scene.builder("seattle-mist", still)
            .livingCamera()
            .at(0, ctx -> Scenes.sound(ctx.viewers(), Sound.AMBIENT_CAVE, 0.6f));
        for (int tick = 0; tick <= SCENE_TICKS; tick++) {
            double thickness = Math.min(1.0, tick / (double) MIST_TICKS);
            scene.at(tick, ctx -> {
                for (Player viewer : ctx.viewers()) {
                    if (settings.affectsCaster() || domain == null || !viewer.getUniqueId().equals(domain.caster().getUniqueId())) {
                        mist(viewer, ctx.cameraEntity(), thickness);
                    }
                }
            });
        }
        scene.at(WORDS_TICK, ctx -> {
            Title words = Title.title(WORDS, Component.empty(),
                Title.Times.times(Duration.ofMillis(600), Duration.ofSeconds(3), Duration.ofMillis(1200)));
            for (Player viewer : ctx.viewers()) {
                viewer.showTitle(words);
            }
            Scenes.sound(ctx.viewers(), Sound.BLOCK_BELL_RESONATE, 0.5f);
        });
        return scene.build();
    }

    // ---- the mist ----

    /** Shows this viewer the mist, this thick (0 to 1), when looking through `through` (themselves, or a camera). */
    private static void mist(Player viewer, Entity through, double thickness) {
        if (!(through instanceof LivingEntity living) || !viewer.isOnline()) {
            return;
        }
        // an even number of ticks, at least 2 (see the class comment)
        int ticks = Math.max(2, 2 * (int) Math.round(thickness * FULL / 2));
        viewer.sendPotionEffectChange(living, new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, false, false, false));
    }

    /** Switches the mist off for this player (their own game's idea of their blindness goes back to the truth). */
    private static void clear(Player player) {
        player.sendPotionEffectChangeRemove(player, PotionEffectType.BLINDNESS);
        PotionEffect real = player.getPotionEffect(PotionEffectType.BLINDNESS);
        if (real != null) {
            player.sendPotionEffectChange(player, real);
        }
    }

    private boolean isInDome(Player player) {
        if (!player.getWorld().equals(domain.world())) {
            return false;
        }
        Location center = domain.center().add(0.5, 0, 0.5);
        double radius = domain.spec().radius();
        double dy = player.getLocation().getY() - center.getY();
        return dy >= -1 && player.getLocation().distanceSquared(center) <= radius * radius;
    }

    // ---- hooks ----

    @Override
    public void onCast(Domain d) {
        this.domain = d;
        d.world().playSound(d.center(), Sound.WEATHER_RAIN_ABOVE, SoundCategory.MASTER, 2.0f, 0.5f);
    }

    @Override
    public void onActive(Domain d) {
        released = stalker.apply(d.center().add(0.5, 0, 0.5));
    }

    /** During the domain: the mist over everyone in the dome (the caster too, if affects-caster), off for anyone who left. */
    @Override
    public void onTick(Domain d) {
        if (d.phase() != Phase.ACTIVE) {
            return;
        }
        Set<UUID> inside = new HashSet<>();
        for (Player player : d.world().getPlayers()) {
            boolean caster = player.getUniqueId().equals(d.caster().getUniqueId());
            if ((caster && !settings.affectsCaster()) || !isInDome(player)) {
                continue;
            }
            inside.add(player.getUniqueId());
            mist(player, player, settings.thickness());
        }
        for (UUID id : misted) {
            Player player = Bukkit.getPlayer(id);
            if (!inside.contains(id) && player != null) {
                clear(player);
            }
        }
        misted.clear();
        misted.addAll(inside);
    }

    @Override
    public void onEnd(Domain d) {
        if (released != null) {
            released.kill();                // it fades with the mist
            released = null;
        }
        clearAll();
    }

    @Override
    public void onFinished(Domain d) {
        clearAll();
    }

    private void clearAll() {
        for (UUID id : misted) {
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                clear(player);
            }
        }
        misted.clear();
    }
}
