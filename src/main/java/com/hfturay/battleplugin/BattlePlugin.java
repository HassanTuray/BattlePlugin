package com.hfturay.battleplugin;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.PluginManager;

import com.hfturay.battleplugin.ability.AbilityFactory;
import com.hfturay.battleplugin.abilityschedulerservice.AbilityScheduler;
import com.hfturay.battleplugin.combatservice.DamageService;
import com.hfturay.battleplugin.combatservice.FallImmunityService;
import com.hfturay.battleplugin.glideservice.GlideService;
import com.hfturay.battleplugin.combatservice.ImmunityService;
import com.hfturay.battleplugin.chaosservice.ChaosService;
import com.hfturay.battleplugin.commandservice.AbilityCommands;
import com.hfturay.battleplugin.commandservice.ChaosCommand;
import com.hfturay.battleplugin.commandservice.CutsceneCommand;
import com.hfturay.battleplugin.commandservice.SpawnMobsCommand;
import com.hfturay.battleplugin.cutsceneservice.CutsceneService;
import com.hfturay.battleplugin.domainservice.DomainService;
import com.hfturay.battleplugin.ability.abilities.hassanabilities.MalwareService;
import com.hfturay.battleplugin.domainservice.neverendingnight.NeverendingNightService;
import com.hfturay.battleplugin.commandservice.KitCommands;
import com.hfturay.battleplugin.configservice.AbilityTuning;
import com.hfturay.battleplugin.configservice.PluginSettings;
import com.hfturay.battleplugin.kitservice.KitService;
import com.hfturay.battleplugin.cooldownservice.CooldownService;
import com.hfturay.battleplugin.cooldownservice.SilenceService;
import com.hfturay.battleplugin.inputservice.InputService;
import com.hfturay.battleplugin.items.ItemProtection;
import com.hfturay.battleplugin.loadoutservice.LoadoutStore;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.loggerservice.DeathLogger;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;
import com.hfturay.battleplugin.resourcepackservice.ResourcePackService;
import com.hfturay.battleplugin.particleservice.ParticleService;
import com.hfturay.battleplugin.projectileservice.ProjectileService;
import com.hfturay.battleplugin.serversetup.HungerLogger;
import com.hfturay.battleplugin.ability.abilities.shikigamiabilities.GuyHelper;
import com.hfturay.battleplugin.combatservice.AbilityUseLog;
import com.hfturay.battleplugin.shikigamiservice.ShikigamiService;
import com.hfturay.battleplugin.stunservice.StunService;
import com.hfturay.battleplugin.visualservice.VisualService;

import com.hfturay.battleplugin.tempblockservice.BukkitBlocks;
import com.hfturay.battleplugin.tempblockservice.TempBlockProtection;
import com.hfturay.battleplugin.tempblockservice.TempBlockService;
import org.bukkit.Bukkit;

import de.slikey.effectlib.EffectManager;

import java.io.File;


public class BattlePlugin extends org.bukkit.plugin.java.JavaPlugin {
    private static BattlePlugin instance;
    private AbilityFactory abilityFactory;
    private LoadoutStore loadoutStore;
    private PluginSettings settings;
    private ResourcePackService resourcePackService;
    private ChaosService chaosService;
    private CutsceneService cutsceneService;
    private DomainService domainService;
    private NeverendingNightService neverendingNightService;
    private MalwareService malwareService;
    private GuyHelper guyHelper;
    private final AbilityUseLog abilityUseLog = new AbilityUseLog();
    private KitService kitService;
    private AbilityTuning abilityTuning;
    private CooldownService cooldownService;
    private ProjectileService projectileService;
    private VisualService visualService;
    private EffectManager effectManager;
    private ParticleService particleService;
    private FallImmunityService fallImmunityService;
    private AbilityScheduler abilityScheduler;
    private ShikigamiService shikigamiService;
    private InputService inputService;
    private SilenceService silenceService;
    private ImmunityService immunityService;
    private DamageService damageService;
    private StunService stunService;
    private HungerLogger hungerLogger;
    private TempBlockService tempBlockService;
    private GlideService glideService;

    @Override
    public void onEnable() {
        instance = this;
        abilityFactory = new AbilityFactory();

        // config.yml and kits.yml are copied into the plugin folder the first time, then read from there.
        saveDefaultConfig();
        settings = new PluginSettings();
        settings.load(getConfig(), getLogger());
        resourcePackService = new ResourcePackService(this);
        chaosService = new ChaosService(this, new File(getDataFolder(), "chaos.yml"));
        chaosService.load();
        chaosService.reload(getConfig());
        File kitsFile = new File(getDataFolder(), "kits.yml");
        if (!kitsFile.exists()) {
            saveResource("kits.yml", false);
        }

        File abilitiesFile = new File(getDataFolder(), "abilities.yml");
        if (!abilitiesFile.exists()) {
            saveResource("abilities.yml", false);
        }
        abilityTuning = new AbilityTuning(abilitiesFile, getLogger());
        AbilityTuning.LoadResult tuned = abilityTuning.reload();
        getLogger().info("Loaded ability numbers for " + tuned.abilities() + " abilities (" + tuned.values() + " values)");

        loadoutStore = new LoadoutStore(new File(getDataFolder(), "playerdata"), getLogger());
        kitService = new KitService(kitsFile, loadoutStore, getLogger());
        KitService.LoadResult kits = kitService.reload();
        getLogger().info("Loaded " + kits.kits() + " kits and " + kits.players() + " listed players");
        cooldownService = new CooldownService();
        projectileService = new ProjectileService(getLogger());
        visualService = new VisualService(this, getLogger());
        effectManager = new EffectManager(this);
        particleService = new ParticleService(this, effectManager);
        fallImmunityService = new FallImmunityService();
        glideService = new GlideService();
        abilityScheduler = new AbilityScheduler(getLogger());
        shikigamiService = new ShikigamiService(getLogger());
        silenceService = new SilenceService();
        immunityService = new ImmunityService();
        damageService = new DamageService(immunityService);
        stunService = new StunService(immunityService, getLogger());
        tempBlockService = new TempBlockService(new BukkitBlocks(),
            TempBlockService.journalFor(new File(getDataFolder(), "tempblocks.yml"), getLogger()), Bukkit::getCurrentTick, getLogger());
        int recovered = tempBlockService.recover();     // puts back anything a crash left in the world
        if (recovered > 0) {
            getLogger().warning("Restored " + recovered + " temporary blocks left behind by the last run");
        }
        hungerLogger = new HungerLogger(settings::foodLevel);
        inputService = new InputService(PlayerLogger::getBattlePlayer, damageService::isDealing, getLogger());

        // load config files
        // Initialize services like, cooldown, effects, click timer
        // register listeners like click handler
        // register commands
        getLogger().info("BattlePlugin enabled!");

        particleService.start();
        projectileService.start(this);
        visualService.start();      // after the projectile service, so it sees where projectiles are this tick
        abilityScheduler.start(this);
        shikigamiService.start(this);
        stunService.start(this);
        getServer().getScheduler().runTaskTimer(this, tempBlockService::tick, 1L, 1L);

        PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerLogger(), this);
        pm.registerEvents(inputService, this);
        pm.registerEvents(shikigamiService, this);
        pm.registerEvents(resourcePackService, this);
        pm.registerEvents(chaosService, this);
        cutsceneService = new CutsceneService(this, new File(getDataFolder(), "cutscenes.yml"));
        pm.registerEvents(cutsceneService, this);
        cutsceneService.recover();
        domainService = new DomainService(this);
        neverendingNightService = new NeverendingNightService(this);
        pm.registerEvents(neverendingNightService, this);
        malwareService = new MalwareService();
        pm.registerEvents(malwareService, this);
        guyHelper = new GuyHelper();
        pm.registerEvents(guyHelper, this);
        resourcePackService.reload(getConfig());
        pm.registerEvents(visualService, this);
        pm.registerEvents(new DeathLogger(), this);
        pm.registerEvents(new ItemProtection(), this);
        pm.registerEvents(fallImmunityService, this);
        pm.registerEvents(glideService, this);
        pm.registerEvents(damageService, this);
        pm.registerEvents(stunService, this);
        pm.registerEvents(hungerLogger, this);
        pm.registerEvents(new TempBlockProtection(tempBlockService), this);

        // testing commands: /load, /remove, /clearabilities
        AbilityCommands abilityCommands = AbilityCommands.forServer(this);
        for (String name : new String[] { "load", "remove", "clearabilities", "clearcooldowns" }) {
            PluginCommand command = getCommand(name);
            if (command == null) {
                getLogger().warning("Command /" + name + " is missing from plugin.yml");
                continue;
            }
            command.setExecutor(abilityCommands);
            command.setTabCompleter(abilityCommands);
        }

        PluginCommand chaosCommand = getCommand("chaos");
        if (chaosCommand == null) {
            getLogger().warning("Command /chaos is missing from plugin.yml");
        } else {
            ChaosCommand chaosHandler = new ChaosCommand(chaosService);
            chaosCommand.setExecutor(chaosHandler);
            chaosCommand.setTabCompleter(chaosHandler);
        }

        PluginCommand spawnMobsCommand = getCommand("spawnmobs");
        if (spawnMobsCommand != null) {
            SpawnMobsCommand spawnMobsHandler = new SpawnMobsCommand(this);
            spawnMobsCommand.setExecutor(spawnMobsHandler);
            spawnMobsCommand.setTabCompleter(spawnMobsHandler);
        }

        PluginCommand cutsceneCommand = getCommand("cutscene");
        if (cutsceneCommand == null) {
            getLogger().warning("Command /cutscene is missing from plugin.yml");
        } else {
            CutsceneCommand cutsceneHandler = new CutsceneCommand(cutsceneService);
            cutsceneCommand.setExecutor(cutsceneHandler);
            cutsceneCommand.setTabCompleter(cutsceneHandler);
        }

        KitCommands kitCommands = new KitCommands(this);
        PluginCommand kitsCommand = getCommand("kits");
        if (kitsCommand == null) {
            getLogger().warning("Command /kits is missing from plugin.yml");
        } else {
            kitsCommand.setExecutor(kitCommands);
            kitsCommand.setTabCompleter(kitCommands);
        }

        // After a /reload the players are already online and no join event will come, so set them up now.
        int existing = PlayerLogger.registerOnline();
        if (existing > 0) {
            getLogger().info("Set up " + existing + " players who were already online");
        }
    }

    @Override 
    public void onDisable() {
        // End domains first of all: it takes back their effects, creatures and the temporary move, before loadouts are saved.
        if (domainService != null) {
            domainService.shutdown();
        }

        // Remember everyone's loadout, since players are still online when the server stops or reloads.
        if (loadoutStore != null) {
            for (BattlePlayer battlePlayer : PlayerLogger.getAll()) {
                loadoutStore.save(battlePlayer.getPlayer().getUniqueId(), battlePlayer.getLoadout());
            }
        }
        PlayerLogger.clear();
        if (chaosService != null) {
            chaosService.save();
        }

        // Put cutscene viewers back (domains were already ended at the top).
        if (cutsceneService != null) {
            cutsceneService.shutdown();
        }

        // Cancel ability tasks first so temporary buffs are undone and their visuals end.
        if (abilityScheduler != null) {
            abilityScheduler.shutdown();
        }

        if (projectileService != null) {
            projectileService.shutdown();
        }

        // After the abilities have ended their own blocks, put back anything still standing.
        if (tempBlockService != null) {
            tempBlockService.restoreAll();
        }

        if (visualService != null) {
            visualService.shutdown();
        }

        if (shikigamiService != null) {
            shikigamiService.shutdown();
        }

        if (stunService != null) {
            stunService.shutdown();
        }

        if (particleService != null) {
            particleService.shutdown();
        }

        if (effectManager != null) {
            effectManager.dispose();
        }

        instance = null;
        getLogger().info("BattlePlugin Disabled");
    }

    public static BattlePlugin getInstance() {
        return instance;
    }

    public LoadoutStore getLoadoutStore() {
        return loadoutStore;
    }

    public PluginSettings getSettings() {
        return settings;
    }

    public AbilityTuning getAbilityTuning() {
        return abilityTuning;
    }

    public MalwareService getMalwareService() {
        return malwareService;
    }

    public GuyHelper getGuyHelper() {
        return guyHelper;
    }

    public AbilityUseLog getAbilityUseLog() {
        return abilityUseLog;
    }

    public NeverendingNightService getNeverendingNightService() {
        return neverendingNightService;
    }

    public DomainService getDomainService() {
        return domainService;
    }

    public CutsceneService getCutsceneService() {
        return cutsceneService;
    }

    public ChaosService getChaosService() {
        return chaosService;
    }

    public KitService getKitService() {
        return kitService;
    }

    /** Re-reads config.yml. Health and food apply the next time a player joins or respawns. */
    public void reloadSettings() {
        reloadConfig();
        settings.load(getConfig(), getLogger());
        resourcePackService.reload(getConfig());
        chaosService.reload(getConfig());
    }

    /**
     * Call after a player's abilities were changed by a command: stops anything a removed ability still had
     * running (buffs, combos, projectiles) and remembers the new loadout so it survives a rejoin.
     */
    public void onLoadoutChanged(BattlePlayer player) {
        abilityScheduler.cancelWhere(task ->
            task.getAbility() != null
                && task.getAbility().getOwner() == player
                && !player.getLoadout().contains(task.getAbility()));
        loadoutStore.save(player.getPlayer().getUniqueId(), player.getLoadout());
    }

    public AbilityFactory getAbilityFactory() {
        return abilityFactory;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public ProjectileService getProjectileService() {
        return projectileService;
    }

    public VisualService getVisualService() {
        return visualService;
    }

    public EffectManager getEffectManager() {
        return effectManager;
    }

    public ParticleService getParticleService() {
        return particleService;
    }

    public FallImmunityService getFallImmunityService() {
        return fallImmunityService;
    }

    public GlideService getGlideService() {
        return glideService;
    }

    public AbilityScheduler getAbilityScheduler() {
        return abilityScheduler;
    }

    public ShikigamiService getShikigamiService() {
        return shikigamiService;
    }

    public InputService getInputService() {
        return inputService;
    }

    public SilenceService getSilenceService() {
        return silenceService;
    }

    public ImmunityService getImmunityService() {
        return immunityService;
    }

    public DamageService getDamageService() {
        return damageService;
    }

    public TempBlockService getTempBlockService() {
        return tempBlockService;
    }

    public StunService getStunService() {
        return stunService;
    }
}
