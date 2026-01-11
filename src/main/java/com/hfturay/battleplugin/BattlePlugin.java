package com.hfturay.battleplugin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;

import com.hfturay.battleplugin.ability.AbilityFactory;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityLogger;
import com.hfturay.battleplugin.clickservice.ClickLogger;
import com.hfturay.battleplugin.cooldownservice.CooldownLogger;
import com.hfturay.battleplugin.cooldownservice.SilenceLogger;
import com.hfturay.battleplugin.falldamageservice.FallDamageLogger;
import com.hfturay.battleplugin.loggerservice.DeathLogger;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;
import com.hfturay.battleplugin.projectileservice.ProjectileLogger;
import com.hfturay.battleplugin.serversetup.HungerLogger;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiLogger;
import com.hfturay.battleplugin.statservice.DamageLogger;
import com.hfturay.battleplugin.stunservice.StunLogger;

import de.slikey.effectlib.EffectManager;


public class BattlePlugin extends org.bukkit.plugin.java.JavaPlugin {
    private static BattlePlugin instance;
    private World world; 
    private AbilityFactory abilityFactory;
    private CooldownLogger cooldownLogger;
    private ProjectileLogger projectileLogger;
    private EffectManager effectManager;
    private FallDamageLogger fallDamageLogger;
    private AbilityLogger abilityLogger;
    private ShikigamiLogger shikigamiLogger;
    private SilenceLogger silenceLogger;
    private DamageLogger damageLogger;
    private StunLogger stunLogger;
    private HungerLogger hungerLogger;

    @Override
    public void onEnable() {
        instance = this;
        world = Bukkit.getWorld("world");
        abilityFactory = new AbilityFactory();
        cooldownLogger = new CooldownLogger();
        projectileLogger = new ProjectileLogger();
        effectManager = new EffectManager(this);
        fallDamageLogger = new FallDamageLogger();
        abilityLogger = new AbilityLogger();
        shikigamiLogger = new ShikigamiLogger();
        silenceLogger = new SilenceLogger();
        damageLogger = new DamageLogger();
        stunLogger = new StunLogger();
        hungerLogger = new HungerLogger();

        // load config files
        // Initialize services like, cooldown, effects, click timer
        // register listeners like click handler
        // register commands
        getLogger().info("BattlePlugin enabled!");

        projectileLogger.start();
        abilityLogger.start();
        shikigamiLogger.start();
        damageLogger.start();
        stunLogger.start();

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerLogger(), this);
        pm.registerEvents(new ClickLogger(), this);
        pm.registerEvents(new DeathLogger(), this);
        pm.registerEvents(fallDamageLogger, this);
        pm.registerEvents(damageLogger, this);
        pm.registerEvents(stunLogger, this);
        pm.registerEvents(hungerLogger, this);
    }

    @Override 
    public void onDisable() {
        if (effectManager != null) {
            effectManager.dispose();
        }

        instance = null;
        getLogger().info("BattlePlugin Disabled");
    }

    public static BattlePlugin getInstance() {
        return instance;
    }

    public AbilityFactory getAbilityFactory() {
        return abilityFactory;
    }

    public World getWorld() {
        return world;
    }

    public CooldownLogger getCoolDownLogger() {
        return cooldownLogger;
    }

    public ProjectileLogger getProjectileLogger() {
        return projectileLogger;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public FallDamageLogger getFallDamageLogger() {
        return fallDamageLogger;
    }

    public AbilityLogger getAbilityLogger() {
        return abilityLogger;
    }

    public ShikigamiLogger getShikigamiLogger() {
        return shikigamiLogger;
    }

    public SilenceLogger getSilenceLogger() {
        return silenceLogger;
    }

    public DamageLogger getDamageLogger() {
        return damageLogger;
    }

    public StunLogger getStunLogger() {
        return stunLogger;
    }
}
