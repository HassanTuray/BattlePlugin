package com.hfturay.battleplugin.ability.abilities.aliabilities;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.AOEAbility;
import com.hfturay.battleplugin.ability.RequiresSettings;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.targetingservice.AOE;

import de.slikey.effectlib.effect.SphereEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import com.hfturay.battleplugin.particleservice.ParticleService;
import com.hfturay.battleplugin.ability.Activation;
import com.hfturay.battleplugin.combatservice.Knockback;

/** Every number comes from abilities.yml (BigBang section). */
@RequiresSettings({ "cooldown", "radius", "damage", "horizontal-force", "lift" })
public class BigBang extends AOEAbility {

    public BigBang(BattlePlayer owner) {
        super(owner);

        this.setDisplayName("Personal Space");
    }

    @Override
    protected Activation onActivate() {
        BattlePlugin.getInstance().getParticleService().follow(owner, AliParticles.BIG_BANG_BURST, owner.getPlayer(), ParticleService.UNTIL_DONE);
        Location center = owner.getPlayer().getLocation();
        
        double damage = setting("damage");
        double horizontal = setting("horizontal-force");
        double lift = setting("lift");

        List<Entity> targets = AOE.getTargets(owner, center, setting("radius"));

        for (Entity target : targets) {
            if (target instanceof Damageable) {
                BattlePlugin.getInstance().getDamageService().damage(owner, target, damage);
            }

            Knockback.awayWithLift(center, target, horizontal, lift);
        }

        return Activation.USED;
    }

    @Override
    public void playParticleEffect() {
      
    }
}
