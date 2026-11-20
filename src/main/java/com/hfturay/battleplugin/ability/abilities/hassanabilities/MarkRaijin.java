package com.hfturay.battleplugin.ability.abilities.hassanabilities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;

import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.ability.ProjectileAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.projectileservice.VirtualProjectile;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Flying Raijin, part one: fires the marking arrow. It does no damage. Hit an entity and that entity is marked; hit a
 * block and that spot is marked. A new mark replaces the old one. UseRaijin (part two) teleports to the mark.
 * Every number comes from abilities.yml (MarkRaijin section), read each time the arrow is fired.
 */
@RequiresSettings({ "cooldown", "speed", "hitbox", "max-ticks", "gravity" })
public class MarkRaijin extends ProjectileAbility {

    private UUID markedEntity;
    private Location markedLocation;
    private final List<VirtualProjectile> arrows = new ArrayList<>();      // arrows still flying

    public MarkRaijin(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Raijin Mark");
        this.particles = HassanParticles.RAIJIN_ARROW;
    }

    @Override
    protected Activation onActivate() {
        double gravity = setting("gravity");

        VirtualProjectile arrow = new VirtualProjectile(
            setting("speed"),
            settingInt("max-ticks"),
            owner,
            setting("hitbox"),
            new MarkRaijinCallback(this),
            particles
        );
        arrow.setVisual(HassanVisuals.RAIJIN_ARROW);       // removed the moment the arrow ends
        arrow.setMotionFunction((v, t) -> v.setY(v.getY() - gravity));

        arrows.removeIf(VirtualProjectile::isFinished);
        arrows.add(arrow);
        this.virtualProjectile = arrow;
        launch();
        return Activation.USED;
    }

    // ---- the mark, read by UseRaijin ----

    /** Marks an entity (replacing any earlier mark). */
    void mark(Entity entity) {
        markedEntity = entity.getUniqueId();
        markedLocation = null;
        confirm(entity.getName());
    }

    /** Marks a spot (replacing any earlier mark). */
    void mark(Location location) {
        markedLocation = location.clone();
        markedEntity = null;
        confirm("location");
    }

    private void confirm(String what) {
        var player = owner.getPlayer();
        player.sendActionBar(Component.text("Raijin marked: " + what, NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
        player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.6f);
    }

    /** Erases the mark (after it has been used). */
    void clear() {
        markedEntity = null;
        markedLocation = null;
    }

    /** The newest arrow that is still flying, or null if none is. */
    private VirtualProjectile flyingArrow() {
        arrows.removeIf(VirtualProjectile::isFinished);
        return arrows.isEmpty() ? null : arrows.get(arrows.size() - 1);
    }

    /** Where the newest flying arrow is right now, or null if none is flying. */
    Location arrowLocation() {
        VirtualProjectile arrow = flyingArrow();
        return arrow == null ? null : arrow.getLocation().clone();
    }

    /** Stops the newest flying arrow (the caster has teleported to it). No mark is made. */
    void stopArrow() {
        VirtualProjectile arrow = flyingArrow();
        if (arrow != null) {
            arrow.cancel();
        }
    }

    boolean hasMark() {
        return markedEntity != null || markedLocation != null;
    }

    /** Where the mark is right now (a marked entity may have moved), or null if there is none or the entity is gone. */
    Location where() {
        if (markedEntity != null) {
            Entity entity = Bukkit.getEntity(markedEntity);
            return entity != null && entity.isValid() ? entity.getLocation() : null;
        }
        return markedLocation == null ? null : markedLocation.clone();
    }
}
