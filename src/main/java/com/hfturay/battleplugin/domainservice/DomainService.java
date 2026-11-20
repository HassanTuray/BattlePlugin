package com.hfturay.battleplugin.domainservice;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.cutsceneservice.Scene;
import com.hfturay.battleplugin.tempblockservice.BlockPos;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

/**
 * Domain expansions: the dome, who is caught in it, and the order things happen in.
 *
 * 1. FORMING   the dome (a floor and a half sphere) appears block by block in random order. Everyone inside gets
 *              night vision, and everyone except the caster is silenced until the domain ends.
 * 2. SCENE     once the dome is closed, a cutscene plays for everyone caught (they are locked and protected for it).
 * 3. ACTIVE    the domain lasts for its duration.
 * 4. ENDING    the dome comes down in random order. Ends early if the caster dies or leaves.
 *
 * A Spec says what one kind of domain is like; a Behavior adds what makes it special (a tower, creatures, effects)
 * through hooks at each step. All the blocks go through TempBlockService, so they are put back even after a crash.
 */
public final class DomainService {

    /** How far below the caster's feet the wall reaches to close gaps on uneven ground (only over open air). */
    private static final int RIM_DEPTH = 3;
    private static final int TEAR_DOWN_TICKS = 20;
    private static final int SCENE_MARGIN_TICKS = 60;

    public enum Phase { FORMING, SCENE, ACTIVE, ENDING }

    /** What one kind of domain is like. scene and behavior may be null. */
    public record Spec(String name, double radius, double thickness, int formTicks, int durationTicks,
                       Material block, int nightVisionAmplifier, Supplier<Scene> scene, Behavior behavior) { }

    /**
     * What makes a domain special. Every hook has an empty default; override the ones you need. Hooks run on the
     * main thread. A hook that throws is logged and skipped; it never breaks the domain.
     */
    public interface Behavior {
        /** The moment of casting, before the dome starts to form. */
        default void onCast(Domain domain) { }

        /** The dome has just closed, before the cutscene starts. */
        default void onClosed(Domain domain) { }

        /** The cutscene is over and the domain proper begins. */
        default void onActive(Domain domain) { }

        /** Every tick, in every phase (check domain.phase()). */
        default void onTick(Domain domain) { }

        /** The domain is ending (timer, caster died, caster left): take back everything it gave. */
        default void onEnd(Domain domain) { }

        /** The very last step, once the dome is fully gone (also on plugin stop). Clean up anything left. */
        default void onFinished(Domain domain) { }
    }

    /** One running domain. Behaviors get this in every hook. */
    public static final class Domain {
        private final Player caster;
        private final Spec spec;
        private final Location center;
        private final Location anchor;
        private final Set<UUID> caught = new LinkedHashSet<>();
        private final List<BlockPos> remaining = new ArrayList<>();
        private final List<BlockPos> clearing = new ArrayList<>();      // blocks inside the dome still to be turned to air
        private final List<TempBlocks> placed = new ArrayList<>();
        private final BlockData data;
        private final BlockData air = Material.AIR.createBlockData();
        private int clearPerTick;
        private Phase phase = Phase.FORMING;
        private BukkitTask task;
        private int tick = 0;          // ticks inside the current phase
        private int age = 0;           // ticks since casting
        private int perTick;
        private boolean endHookRun = false;

        Domain(Player caster, Spec spec) {
            this.caster = caster;
            this.spec = spec;
            this.center = caster.getLocation().getBlock().getLocation();
            this.anchor = center.clone().add(0.5, 0, 0.5);
            this.anchor.setYaw(caster.getLocation().getYaw());
            this.data = spec.block().createBlockData();
        }

        public Player caster() {
            return caster;
        }

        public Spec spec() {
            return spec;
        }

        public World world() {
            return center.getWorld();
        }

        /** The block the caster stood in when casting (its corner). The floor is one block below. */
        public Location center() {
            return center.clone();
        }

        /** The middle of that block, facing the way the caster faced. Cutscenes are placed relative to this. */
        public Location anchor() {
            return anchor.clone();
        }

        public Phase phase() {
            return phase;
        }

        /** Ticks since the domain was cast. */
        public int age() {
            return age;
        }

        /** Ticks since the current phase began. */
        public int phaseTicks() {
            return tick;
        }

        public Set<UUID> caughtIds() {
            return caught;
        }

        /** Everyone caught who is still online (the caster included). */
        public List<Player> caughtPlayers() {
            List<Player> players = new ArrayList<>();
            for (UUID id : caught) {
                Player player = Bukkit.getPlayer(id);
                if (player != null && player.isOnline()) {
                    players.add(player);
                }
            }
            return players;
        }

        /** Everyone caught except the caster who is online, alive and playing. */
        public List<Player> enemies() {
            List<Player> enemies = new ArrayList<>();
            for (Player player : caughtPlayers()) {
                if (!player.getUniqueId().equals(caster.getUniqueId()) && !player.isDead()
                    && player.getGameMode() != GameMode.SPECTATOR) {
                    enemies.add(player);
                }
            }
            return enemies;
        }
    }

    private final Plugin plugin;
    private final Logger logger;
    private final Map<UUID, Domain> active = new HashMap<>();

    public DomainService(Plugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
    }

    public boolean isActive(UUID caster) {
        return active.containsKey(caster);
    }

    /** Starts a domain around the caster. Returns false if they already have one running. */
    public boolean cast(Player caster, Spec spec) {
        if (active.containsKey(caster.getUniqueId())) {
            return false;
        }
        Domain domain = new Domain(caster, spec);
        active.put(caster.getUniqueId(), domain);

        // who is caught: everyone inside the radius right now
        World world = domain.world();
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distanceSquared(domain.center) <= spec.radius() * spec.radius()) {
                domain.caught.add(player.getUniqueId());
            }
        }
        domain.caught.add(caster.getUniqueId());

        domain.remaining.addAll(shell(domain));
        Collections.shuffle(domain.remaining, ThreadLocalRandom.current());
        domain.perTick = Math.max(1, (int) Math.ceil(domain.remaining.size() / (double) Math.max(1, spec.formTicks())));

        // everything inside the dome is turned to air as it forms, and put back when the domain ends
        domain.clearing.addAll(interior(domain));
        Collections.shuffle(domain.clearing, ThreadLocalRandom.current());
        domain.clearPerTick = Math.max(1, (int) Math.ceil(domain.clearing.size() / (double) Math.max(1, spec.formTicks())));

        // effects that last until the domain ends (plus a margin for the scene); ending clears them
        int total = spec.formTicks() + sceneTicks(spec) + spec.durationTicks() + SCENE_MARGIN_TICKS;
        for (UUID id : domain.caught) {
            Player player = Bukkit.getPlayer(id);
            if (player == null) {
                continue;
            }
            player.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, total, spec.nightVisionAmplifier(), false, false, false));
            if (!id.equals(caster.getUniqueId())) {
                BattlePlugin.getInstance().getSilenceService().silence(id, total);
            }
        }

        world.playSound(domain.center, Sound.ENTITY_WARDEN_EMERGE, SoundCategory.MASTER, 4.0f, 0.6f);
        hook(domain, b -> b.onCast(domain));
        domain.task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> tick(domain), 1L, 1L);
        return true;
    }

    private static int sceneTicks(Spec spec) {
        Scene scene = spec.scene() == null ? null : spec.scene().get();
        return scene == null ? 0 : scene.totalTicks();
    }

    /**
     * Every solid or liquid block inside the dome (above the floor, inside the wall): terrain, trees, water, plants.
     * Blocks that hold things (chests, furnaces, signs ...) are left alone, because turning them to air and back would
     * lose what is stored in them.
     */
    private List<BlockPos> interior(Domain domain) {
        World world = domain.world();
        int cx = domain.center.getBlockX();
        int cy = domain.center.getBlockY();
        int cz = domain.center.getBlockZ();
        int reach = (int) Math.ceil(domain.spec.radius()) + 1;
        double inner = domain.spec.radius() - domain.spec.thickness();
        double innerSq = inner * inner;

        List<BlockPos> blocks = new ArrayList<>();
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = 0; dy <= reach; dy++) {
                    if (dx * dx + dy * dy + dz * dz > innerSq) {
                        continue;
                    }
                    Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                    if (block.getType().isAir() || block.getState(false) instanceof org.bukkit.block.TileState) {
                        continue;
                    }
                    blocks.add(BlockPos.of(block));
                }
            }
        }
        return blocks;
    }

    /** Every block position of the dome: a one-block floor, and the top half of a hollow sphere. */
    private List<BlockPos> shell(Domain domain) {
        Spec spec = domain.spec;
        World world = domain.world();
        int cx = domain.center.getBlockX();
        int cy = domain.center.getBlockY();
        int cz = domain.center.getBlockZ();
        int reach = (int) Math.ceil(spec.radius()) + 1;
        double inner = spec.radius() - spec.thickness();
        double outerSq = spec.radius() * spec.radius();
        double innerSq = inner * inner;

        // a set, because the floor and the rim of the wall can both want the same block
        Set<BlockPos> shell = new LinkedHashSet<>();

        // the floor: one layer of blocks under the caster's feet, across the whole circle (replacing the ground)
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                if (dx * dx + dz * dz <= outerSq) {
                    shell.add(new BlockPos(world.getUID(), cx + dx, cy - 1, cz + dz));
                }
            }
        }

        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int dy = -RIM_DEPTH; dy <= reach; dy++) {
                    double distanceSq = dx * dx + dy * dy + dz * dz;
                    if (distanceSq > outerSq || distanceSq <= innerSq) {
                        continue;
                    }
                    if (dy >= 0) {
                        shell.add(new BlockPos(world.getUID(), cx + dx, cy + dy, cz + dz));
                    } else {
                        Block block = world.getBlockAt(cx + dx, cy + dy, cz + dz);
                        if (!block.getType().isSolid()) {       // below the feet it only fills open air, never digs into ground
                            shell.add(BlockPos.of(block));
                        }
                    }
                }
            }
        }
        return new ArrayList<>(shell);
    }

    private void tick(Domain domain) {
        try {
            domain.age++;
            if (!domain.caster.isOnline() || domain.caster.isDead()) {
                if (domain.phase != Phase.ENDING) {
                    beginEnding(domain);
                }
            }
            switch (domain.phase) {
                case FORMING -> form(domain);
                case SCENE -> watchScene(domain);
                case ACTIVE -> {
                    domain.tick++;
                    if (domain.tick >= domain.spec.durationTicks()) {
                        beginEnding(domain);
                    }
                }
                case ENDING -> tearDown(domain);
            }
            if (active.get(domain.caster.getUniqueId()) == domain) {
                hook(domain, b -> b.onTick(domain));
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "A domain failed; ending it", e);
            finish(domain);
        }
    }

    private void form(Domain domain) {
        World world = domain.world();
        int count = Math.min(domain.perTick, domain.remaining.size());
        if (count > 0) {
            List<BlockPos> batch = new ArrayList<>(domain.remaining.subList(0, count));
            domain.remaining.subList(0, count).clear();
            domain.placed.add(BattlePlugin.getInstance().getTempBlockService().place(batch, domain.data, 0));

            BlockPos sample = batch.get(ThreadLocalRandom.current().nextInt(batch.size()));
            Location at = new Location(world, sample.x() + 0.5, sample.y() + 0.5, sample.z() + 0.5);
            world.spawnParticle(Particle.ASH, at, 12, 0.6, 0.6, 0.6, 0.0);
            if (domain.tick % 2 == 0) {
                world.playSound(at, Sound.BLOCK_DEEPSLATE_PLACE, SoundCategory.MASTER, 3.0f, 0.7f);
            }
        }

        int clear = Math.min(domain.clearPerTick, domain.clearing.size());
        if (clear > 0) {
            List<BlockPos> batch = new ArrayList<>(domain.clearing.subList(0, clear));
            domain.clearing.subList(0, clear).clear();
            domain.placed.add(BattlePlugin.getInstance().getTempBlockService().place(batch, domain.air, 0));
        }
        domain.tick++;

        if (domain.remaining.isEmpty() && domain.clearing.isEmpty()) {
            hook(domain, b -> b.onClosed(domain));
            startScene(domain);
        }
    }

    /** The dome is closed: play the cutscene for everyone caught, then the domain proper begins. */
    private void startScene(Domain domain) {
        domain.phase = Phase.SCENE;
        domain.tick = 0;
        Scene scene = domain.spec.scene() == null ? null : domain.spec.scene().get();
        if (scene == null) {
            enterActive(domain);
            return;
        }
        boolean started = BattlePlugin.getInstance().getCutsceneService().play(scene, domain.anchor, domain.caughtPlayers(), () -> {
            if (domain.phase == Phase.SCENE) {
                enterActive(domain);
            }
        });
        if (!started) {
            enterActive(domain);
        }
    }

    /** While the cutscene plays. If it was cut short (everyone left it) the domain carries on without it. */
    private void watchScene(Domain domain) {
        domain.tick++;
        if (domain.tick < 10) {
            return;
        }
        for (UUID id : domain.caught) {
            if (BattlePlugin.getInstance().getCutsceneService().isWatching(id)) {
                return;
            }
        }
        enterActive(domain);
    }

    private void enterActive(Domain domain) {
        domain.phase = Phase.ACTIVE;
        domain.tick = 0;
        hook(domain, b -> b.onActive(domain));
    }

    private void beginEnding(Domain domain) {
        if (domain.phase == Phase.ENDING) {
            return;
        }
        domain.phase = Phase.ENDING;
        domain.tick = 0;
        BattlePlugin.getInstance().getCutsceneService().cancel(domain.caster.getUniqueId());   // no-op unless mid-scene
        runEndHook(domain);
        Collections.shuffle(domain.placed, ThreadLocalRandom.current());
        domain.perTick = Math.max(1, (int) Math.ceil(domain.placed.size() / (double) TEAR_DOWN_TICKS));
        domain.world().playSound(domain.center, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.MASTER, 4.0f, 0.5f);
    }

    private void runEndHook(Domain domain) {
        if (!domain.endHookRun) {
            domain.endHookRun = true;
            hook(domain, b -> b.onEnd(domain));
        }
    }

    private void tearDown(Domain domain) {
        int released = 0;
        while (!domain.placed.isEmpty() && released < domain.perTick) {
            TempBlocks group = domain.placed.remove(domain.placed.size() - 1);
            group.remove();
            released++;
        }
        if (domain.placed.isEmpty()) {
            finish(domain);
        }
    }

    /** Takes away everything the domain gave or did and forgets it. Safe to call twice. */
    private void finish(Domain domain) {
        boolean first = active.remove(domain.caster.getUniqueId(), domain);
        if (domain.task != null) {
            domain.task.cancel();
        }
        runEndHook(domain);
        for (TempBlocks group : domain.placed) {
            group.remove();
        }
        domain.placed.clear();
        for (UUID id : domain.caught) {
            BattlePlugin.getInstance().getSilenceService().clear(id);
            Player player = Bukkit.getPlayer(id);
            if (player != null) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }
        if (first) {
            hook(domain, b -> b.onFinished(domain));
        }
    }

    /** Plugin stop: ends every domain now. */
    public void shutdown() {
        for (Domain domain : new ArrayList<>(active.values())) {
            finish(domain);
        }
    }

    private void hook(Domain domain, Consumer<Behavior> call) {
        Behavior behavior = domain.spec.behavior();
        if (behavior == null) {
            return;
        }
        try {
            call.accept(behavior);
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "A hook of the domain '" + domain.spec.name() + "' failed", e);
        }
    }
}
