package com.hfturay.battleplugin.ability;

import java.util.UUID;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.combatservice.AbilityUseLog;
import com.hfturay.battleplugin.cooldownservice.CooldownService;
import com.hfturay.battleplugin.cooldownservice.SilenceService;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

/**
 * Base class for every ability.
 *
 * To make an ability, implement onActivate() and say what happened (an Activation). Silence, cooldown checks and
 * starting the cooldown are all done here, once, for every ability, so an ability never repeats them.
 */
public abstract class Ability {
    protected String displayName = "Default Display Name";
    protected int cooldownTicks = 0;
    protected double damage = 0;
    protected final BattlePlayer owner;

    protected double horizForce;
    protected double vertForce;

    /** Extra damage a buff (The Ascended) has added to this ability for now. Added to the ability's own damage. */
    private double damageBonus = 0;

    public double getDamageBonus() {
        return damageBonus;
    }

    /** Adds to (or, with a negative number, takes back) the buff on this ability's damage. */
    public void addDamageBonus(double amount) {
        this.damageBonus += amount;
    }

    /**
     * Do the ability. Only called when the player is not silenced, nothing is blocking it, and it is off cooldown.
     * Return USED to start the cooldown, FAILED if nothing happened (a miss), or USED_NO_COOLDOWN if the ability
     * starts its own cooldown later.
     */
    protected abstract Activation onActivate();

    /**
     * Override to refuse activation with a message shown to the player (health requirements, missing prerequisites).
     * Checked after silence and before the cooldown. Return null to allow it.
     */
    protected String blockedReason() {
        return null;
    }

    /** Optional hook for abilities that want their visual in one place. Play it through the ParticleService. */
    public void playParticleEffect() {}

    public Ability(BattlePlayer owner) {
        this.owner = owner;
    }

    /**
     * The one way an ability is used. Order: silenced? -> blocked? -> on cooldown? -> onActivate() -> cooldown.
     * Returns true if the ability did something.
     */
    public final boolean activate() {
        UUID id = owner.getPlayer().getUniqueId();

        int silenced = silenceService().remainingTicks(id);
        if (silenced > 0) {
            actionBar("You are silenced (" + CooldownService.formatSeconds(silenced) + "s)");
            return false;
        }

        String missing = missingSetting();
        if (missing != null) {
            actionBar(displayName + " is not set up: '" + missing + "' is missing in abilities.yml");
            return false;
        }

        String blocked = blockedReason();
        if (blocked != null) {
            actionBar(blocked);
            return false;
        }

        int remaining = cooldownService().remainingTicks(id, getClass());
        if (remaining > 0) {
            actionBar("Cooldown: " + CooldownService.formatSeconds(remaining) + "s");
            return false;
        }

        Activation result = onActivate();
        if (result != Activation.FAILED) {
            AbilityUseLog log = useLog();
            if (log != null) {
                log.record(id, getClass());
            }
        }
        if (result == Activation.USED) {
            startCooldown();
        }
        return result != Activation.FAILED;
    }

    /** Starts this ability's cooldown for its owner. For abilities (and their repeaters) that return USED_NO_COOLDOWN. */
    public void startCooldown() {
        // abilities.yml may set the cooldown (in seconds); otherwise the value in the code is used
        double seconds = tuned("cooldown", Double.NaN);
        int ticks = Double.isNaN(seconds) ? cooldownTicks : (int) Math.round(seconds * 20);
        cooldownService().start(owner.getPlayer().getUniqueId(), getClass(), ticks);
    }

    /**
     * A number for this ability from abilities.yml. The ability must list it in @RequiresSettings, which is what
     * guarantees it is there (activate() refuses to run otherwise). Ask when the ability is used, not in the
     * constructor, so a reload applies straight away.
     */
    protected double setting(String key) {
        double value = tuning().number(getId(), key, Double.NaN);
        if (Double.isNaN(value)) {
            throw new IllegalStateException(getId() + " has no '" + key + "' in abilities.yml (is it in @RequiresSettings?)");
        }
        return value;
    }

    /** A setting that is a whole number (ticks, counts, levels). */
    protected int settingInt(String key) {
        return (int) Math.round(setting(key));
    }

    /** The first setting this ability needs that abilities.yml does not have, or null if it has them all. */
    private String missingSetting() {
        RequiresSettings required = getClass().getAnnotation(RequiresSettings.class);
        if (required == null) {
            return null;
        }
        for (String key : required.value()) {
            if (!tuning().has(getId(), key)) {
                return key;
            }
        }
        return null;
    }

    /** Only used for abilities that have no @RequiresSettings yet, so they can still override their cooldown. */
    private double tuned(String key, double fallback) {
        return tuning().number(getId(), key, fallback);
    }

    protected com.hfturay.battleplugin.configservice.AbilityTuning tuning() {
        return BattlePlugin.getInstance().getAbilityTuning();
    }

    // The services come from the plugin. Overridable so the activation rules can be tested without a server.
    protected CooldownService cooldownService() {
        return BattlePlugin.getInstance().getCooldownService();
    }

    /** Who used what, for Guy to learn from. Null without a running plugin (the tests). */
    protected AbilityUseLog useLog() {
        BattlePlugin plugin = BattlePlugin.getInstance();
        return plugin == null ? null : plugin.getAbilityUseLog();
    }

    protected SilenceService silenceService() {
        return BattlePlugin.getInstance().getSilenceService();
    }

    private void actionBar(String message) {
        owner.getPlayer().sendActionBar(Component.text(message, NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    /** A stable name for this ability: its class name. */
    public String getId() {
        return getClass().getSimpleName();
    }

    /** Cooldown in seconds (rounded down). */
    public long getCooldown() {
        return cooldownTicks / 20;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    /** Sets the cooldown in seconds. */
    public void setCoolDown(long seconds) {
        this.cooldownTicks = (int) (seconds * 20);
    }

    /** Sets the cooldown in server ticks (20 per second). */
    public void setCooldownTicks(int ticks) {
        this.cooldownTicks = ticks;
    }

    public double getHorizForce() {
        return this.horizForce;
    }

    public void setHorizForce(double horizForce) {
        this.horizForce = horizForce;
    }

    public double getVertForce() {
        return this.vertForce;
    }

    public void setVertForce(double vertForce) {
        this.vertForce = vertForce;
    }

    public double getDamage() {
        return this.damage;
    }

    public void setDamage(double damage) {
        this.damage = damage;
    }

    public BattlePlayer getOwner() {
        return this.owner;
    }

    /** True for an ability that is only lent for a while (a domain move). It is never saved with the loadout. */
    public boolean isTemporary() {
        return false;
    }

    public void cancel() {}

}
