package com.hfturay.battleplugin.domainservice.neverendingnight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Enderman;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Warden;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.ability.abilities.domainabilities.TitansWrath;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.cutsceneservice.CameraPath;
import com.hfturay.battleplugin.cutsceneservice.CameraPath.Easing;
import com.hfturay.battleplugin.cutsceneservice.CameraPath.Keyframe;
import com.hfturay.battleplugin.cutsceneservice.Scene;
import com.hfturay.battleplugin.cutsceneservice.Scenes;
import com.hfturay.battleplugin.domainservice.DomainService.Behavior;
import com.hfturay.battleplugin.domainservice.DomainService.Domain;
import com.hfturay.battleplugin.domainservice.DomainService.Phase;
import com.hfturay.battleplugin.loggerservice.PlayerLogger;
import com.hfturay.battleplugin.targetingservice.Targets;
import com.hfturay.battleplugin.tempblockservice.BlockPos;
import com.hfturay.battleplugin.tempblockservice.TempBlocks;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

/**
 * What makes the domain "Neverending Night" special.
 *
 * The cutscene (see scene()) tells the story: the camera circles the inside of the dome, a column of blue fire
 * bursts up from the centre, an obsidian tower builds itself around it (in random order, from the ground up), ender
 * particles gather at its top and a giant eye opens there, facing the camera. When the scene ends the domain
 * proper begins: creatures of darkness hunt the caster's enemies, who are slowed and blinded, the eye watches the
 * arena, and the caster gains Titan's Wrath (which fires from the eye). Everything is taken back when it ends.
 *
 * One of these is made for every cast, with the numbers read from abilities.yml at that moment. The cutscene's cues
 * only flip switches here (startColumn, startTower, ...); the work itself happens in onTick, every tick.
 */
public final class NeverendingNightEffects implements Behavior {

    /**
     * The numbers. flameHeight is how tall the column of fire is, towerHeight how tall the obsidian tower is.
     * mobsTargetCaster is for testing alone: the creatures attack the caster too.
     */
    public record Settings(int flameHeight, int towerHeight, int endermen, int phantoms, int wardens,
                           boolean mobsTargetCaster, int slowAmplifier) { }

    /** Local time that is night. */
    private static final long NIGHT = 18000;
    private static final int RETARGET_INTERVAL = 20;
    private static final int COLUMN_RISE_TICKS = 8;        // the fire shoots up this fast
    private static final int TOWER_BUILD_TICKS = 60;
    private static final int EYE_GROW_TICKS = 20;
    /** How far the eye hangs above the top of the tower (its middle), in blocks. */
    private static final int EYE_LIFT = 3;
    /** The camera stays this far from the middle of the dome, at most (the dome's inside is about 23 blocks). */
    private static final double EDGE = 19;

    private final NeverendingNightService service;
    private final Settings settings;

    private Domain domain;
    private UUID casterId;
    private World world;
    private int cx, cy, cz;
    private Location eyeCenter;

    // the pillar at the centre: blue fire, which abruptly turns to smoke and souls, until the obsidian takes over
    private enum Pillar { NONE, FIRE, SMOKE }
    private Pillar pillar = Pillar.NONE;
    private int columnSince = -1;              // domain age when the fire burst up; -1 = not yet

    // the obsidian tower: planned in a random-but-rising order, placed a few blocks a tick
    private List<BlockPos> towerPlan = List.of();
    private int towerPlaced = 0;
    private int towerPerTick = 0;
    private boolean towerBuilding = false;
    private final List<TempBlocks> tower = new ArrayList<>();

    // the ender particles at the top of the tower, and the eye
    private int enderUntil = -1;               // domain age until which the particles run
    private EyeModel eye;
    private int eyePlacedAge = -1;
    private Vector sceneEyeFacing = new Vector(0, -0.3, 1);
    private boolean eyeBusy = false;           // Titan's Wrath is steering the eye

    private final Set<UUID> mobs = new HashSet<>();
    private boolean wrathGranted = false;

    public NeverendingNightEffects(NeverendingNightService service, Settings settings) {
        this.service = service;
        this.settings = settings;
    }

    // ---- what Titan's Wrath and the listener ask ----

    /** Where the giant eye hangs: above the top of the tower. */
    public Location eyeCenter() {
        return eyeCenter == null ? null : eyeCenter.clone();
    }

    /** The eye, or null before it has been placed. */
    EyeModel eye() {
        return eye;
    }

    /** Titan's Wrath says it is steering the eye, so the idle gaze leaves it alone. */
    void setEyeBusy(boolean busy) {
        this.eyeBusy = busy;
    }

    /**
     * True if the creatures may attack this. Anything inside the dome that is a player or a mob qualifies, except the
     * caster, the domain's own creatures, and anything the caster owns (their shikigami and tamed pets).
     */
    public boolean mayTarget(LivingEntity target) {
        if (domain == null || target == null || !target.isValid() || target.isDead()) {
            return false;
        }
        if (target.getUniqueId().equals(casterId)) {
            return settings.mobsTargetCaster();
        }
        if (service.isCreature(target) || casterId.equals(Targets.shikigamiOwnerOf(target))) {
            return false;
        }
        if (target instanceof Tameable pet && pet.getOwner() != null && casterId.equals(pet.getOwner().getUniqueId())) {
            return false;
        }
        if (target instanceof Player player && player.getGameMode() == GameMode.SPECTATOR) {
            return false;
        }
        if (!(target instanceof Player) && !(target instanceof Mob)) {
            return false;               // armor stands and the like
        }
        return isInDome(target.getLocation());
    }

    private boolean isInDome(Location location) {
        if (location.getWorld() != world) {
            return false;
        }
        double dx = location.getX() - (cx + 0.5);
        double dy = location.getY() - cy;
        double dz = location.getZ() - (cz + 0.5);
        double radius = domain.spec().radius();
        return dy >= -1 && dx * dx + dy * dy + dz * dz <= radius * radius;
    }

    public UUID casterId() {
        return casterId;
    }

    /** True if the point is inside the dome (with a little room), for keeping creatures from teleporting out. */
    public boolean isInside(Location location) {
        if (domain == null || location.getWorld() != world) {
            return false;
        }
        double dx = location.getX() - (cx + 0.5);
        double dy = location.getY() - cy;
        double dz = location.getZ() - (cz + 0.5);
        double limit = domain.spec().radius() - 3;
        return dy >= -1 && dx * dx + dy * dy + dz * dz <= limit * limit;
    }

    // ---- the cutscene ----

    /**
     * The 21-second cutscene. The camera always stays inside the dome, close to its wall, then drifts in.
     *
     *   0-150    the camera circles the dome's edge, looking at the centre; the two lines of text appear
     *   150      the column of blue fire bursts up from the centre, with fire swirling round it
     *   200      the fire abruptly turns into a pillar of smoke and souls
     *   230      the obsidian builds, in random order but rising from the ground
     *   295      ender particles gather at the top of the tower
     *   320      the eye opens there, turned towards the camera
     *   345-370  the camera holds, looking the eye in the face
     *   370-420  the camera pushes in towards the eye until it fills the view
     */
    public Scene scene() {
        double eyeHeight = settings.towerHeight() + EYE_LIFT;
        List<Keyframe> frames = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            frames.add(frame(45 * i, EDGE, 5 + i * 0.6, 10, 25, i == 0 ? Easing.EASE_IN : Easing.LINEAR));
        }
        frames.add(frame(270, EDGE, 8, 10, 35, Easing.LINEAR));          // tick 150: the fire bursts
        frames.add(frame(295, 18, 7, 11, 40, Easing.LINEAR));            // tick 185
        frames.add(frame(315, 17, 6, 11, 65, Easing.LINEAR));            // tick 225: the smoke pillar stands
        frames.add(frame(335, 15, 5.5, 13, 55, Easing.EASE_OUT));        // tick 290: the obsidian is nearly done
        frames.add(frame(340, 14, 6, eyeHeight, 25, Easing.LINEAR));     // tick 345: eye contact, held
        frames.add(frame(340, 14, 6, eyeHeight, 50, Easing.EASE_IN_OUT)); // tick 370: the camera starts to push in
        // the push-in goes straight along the line to the eye (so the eye keeps facing the camera) and stops three
        // quarters of the way there: close enough to fill the screen, still clear of the tower and the eye itself
        double zoom = 0.75;
        frames.add(frame(340, 14 * (1 - zoom), 6 + (eyeHeight - 6) * zoom, eyeHeight, 1, Easing.LINEAR));   // tick 420

        return Scene.builder("neverending-night", new CameraPath(frames))
            .at(0, ctx -> Scenes.sound(ctx.viewers(), Sound.ENTITY_WARDEN_HEARTBEAT, 0.7f))
            .at(10, ctx -> {
                Scenes.line(ctx.viewers(), "A WORLD BASKED IN PUREST LIGHT.", NamedTextColor.GOLD);
                Scenes.sound(ctx.viewers(), Sound.BLOCK_BEACON_AMBIENT, 1.0f);
            })
            .at(80, ctx -> {
                Scenes.line(ctx.viewers(), "BENEATH IT, GREW ETERNAL NIGHT.", NamedTextColor.DARK_PURPLE);
                Scenes.sound(ctx.viewers(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.6f);
            })
            .at(150, ctx -> startColumn())
            .at(200, ctx -> turnToSmoke())
            .at(230, ctx -> startTower())
            .at(295, ctx -> startEnderParticles())
            .at(320, ctx -> placeEye(ctx.camera()))
            .at(355, ctx -> Scenes.sound(ctx.viewers(), Sound.ENTITY_WARDEN_ROAR, 0.8f))
            .at(372, ctx -> Scenes.sound(ctx.viewers(), Sound.ENTITY_WARDEN_SONIC_CHARGE, 0.6f))
            .build();
    }

    /** One camera keyframe on the circle of this radius, aimed at a point on the centre line this high. */
    private static Keyframe frame(double angleDegrees, double distance, double height, double aimHeight, int ticks, Easing easing) {
        double angle = Math.toRadians(angleDegrees);
        // yaw is minus the angle so the camera keeps facing the centre; it is never wrapped
        return new Keyframe(distance * Math.sin(angle), height, -distance * Math.cos(angle),
            -angleDegrees, Scenes.lookAt(height, aimHeight, distance), ticks, easing);
    }

    private void startColumn() {
        if (domain == null || columnSince >= 0) {
            return;
        }
        columnSince = domain.age();
        pillar = Pillar.FIRE;
        Location base = new Location(world, cx + 0.5, cy, cz + 0.5);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, base, 2, 0.5, 0.2, 0.5, 0);
        for (int i = 0; i < 60; i++) {                                  // a ring of fire racing outwards
            double angle = Math.PI * 2 * i / 60;
            world.spawnParticle(Particle.SOUL_FIRE_FLAME, base.getX() + Math.cos(angle) * 2, cy + 0.3, base.getZ() + Math.sin(angle) * 2,
                0, Math.cos(angle), 0.1, Math.sin(angle), 0.6);
        }
        world.playSound(base, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 5.0f, 0.5f);
        world.playSound(base, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.MASTER, 5.0f, 0.4f);
        world.playSound(base, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.MASTER, 3.0f, 0.5f);
    }

    /** The fire is snuffed out in an instant and the pillar becomes smoke and souls, standing at full height at once. */
    private void turnToSmoke() {
        if (domain == null || pillar != Pillar.FIRE) {
            return;
        }
        pillar = Pillar.SMOKE;
        Location base = new Location(world, cx + 0.5, cy, cz + 0.5);
        world.spawnParticle(Particle.LARGE_SMOKE, base.clone().add(0, 1, 0), 60, 1.5, 0.6, 1.5, 0.15);
        for (double y = cy; y <= cy + settings.flameHeight(); y += 1.0) {         // a puff all the way up, in one go
            world.spawnParticle(Particle.LARGE_SMOKE, cx + 0.5, y, cz + 0.5, 8, 0.7, 0.3, 0.7, 0.05);
            world.spawnParticle(Particle.SOUL, cx + 0.5, y, cz + 0.5, 4, 0.6, 0.3, 0.6, 0.05);
        }
        world.playSound(base, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.MASTER, 5.0f, 0.6f);
        world.playSound(base, Sound.PARTICLE_SOUL_ESCAPE, SoundCategory.MASTER, 5.0f, 0.5f);
    }

    /** Plans every block of the tower and starts placing them. Low blocks come first, with some shuffling in height. */
    private void startTower() {
        if (domain == null || towerBuilding || !towerPlan.isEmpty()) {
            return;
        }
        List<BlockPos> plan = new ArrayList<>();
        for (int y = 0; y < settings.towerHeight(); y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    plan.add(new BlockPos(world.getUID(), cx + dx, cy + y, cz + dz));
                }
            }
        }
        // sort by height plus a random wobble of up to 3 blocks: random-looking, but the tower still rises
        ThreadLocalRandom random = ThreadLocalRandom.current();
        java.util.Map<BlockPos, Double> key = new java.util.HashMap<>();
        for (BlockPos pos : plan) {
            key.put(pos, pos.y() + random.nextDouble() * 3.0);
        }
        plan.sort(Comparator.comparingDouble(key::get));
        towerPlan = plan;
        towerPerTick = Math.max(1, (int) Math.ceil(plan.size() / (double) TOWER_BUILD_TICKS));
        towerBuilding = true;
    }

    private void startEnderParticles() {
        if (domain == null) {
            return;
        }
        enderUntil = domain.age() + 55;
        world.playSound(topOfTower(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.MASTER, 4.0f, 0.5f);
    }

    /** Opens the eye at the top of the tower, turned towards the camera (eye contact). */
    private void placeEye(Location camera) {
        if (domain == null || eye != null) {
            return;
        }
        if (camera != null) {
            sceneEyeFacing = camera.toVector().subtract(eyeCenter.toVector()).normalize();
        }
        eye = new EyeModel(eyeCenter, sceneEyeFacing, 0.1);
        eyePlacedAge = domain.age();
        world.playSound(eyeCenter, Sound.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.MASTER, 4.0f, 0.6f);
        world.spawnParticle(Particle.PORTAL, eyeCenter, 80, 2.0, 2.0, 2.0, 0.8);
    }

    private Location topOfTower() {
        return new Location(world, cx + 0.5, cy + settings.towerHeight() + 0.5, cz + 0.5);
    }

    // ---- hooks ----

    @Override
    public void onCast(Domain d) {
        this.domain = d;
        this.casterId = d.caster().getUniqueId();
        this.world = d.world();
        this.cx = d.center().getBlockX();
        this.cy = d.center().getBlockY();
        this.cz = d.center().getBlockZ();
        this.eyeCenter = new Location(world, cx + 0.5, cy + settings.towerHeight() + EYE_LIFT, cz + 0.5);
        service.register(casterId, this);

        world.playSound(d.center(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 3.0f, 0.5f);
    }

    @Override
    public void onClosed(Domain d) {
        // "Sets the local time to night": only for the people inside, and only until the domain ends.
        for (Player player : d.caughtPlayers()) {
            player.setPlayerTime(NIGHT, false);
        }
    }

    @Override
    public void onTick(Domain d) {
        if (d.phase() == Phase.ENDING) {
            crumble();
            return;
        }
        burn(d);
        buildTower();
        enderParticles(d);
        animateEye(d);
        if (d.phase() == Phase.ACTIVE && d.phaseTicks() % RETARGET_INTERVAL == 0) {
            retarget(d);
        }
    }

    @Override
    public void onActive(Domain d) {
        completeStructures();
        pushOutOfTower();

        int ticks = d.spec().durationTicks() + 40;
        for (Player enemy : d.enemies()) {
            enemy.removePotionEffect(PotionEffectType.NIGHT_VISION);       // the dome's night vision would undo the darkness
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, ticks, settings.slowAmplifier(), false, true, true));
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, ticks, 0, false, true, true));
            enemy.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, ticks, 0, false, true, true));
        }

        spawnCreatures();
        grantTitansWrath(d);
        world.playSound(d.center(), Sound.ENTITY_WARDEN_ROAR, SoundCategory.MASTER, 4.0f, 0.8f);
    }

    @Override
    public void onEnd(Domain d) {
        for (Player enemy : d.enemies()) {
            enemy.removePotionEffect(PotionEffectType.SLOWNESS);
            enemy.removePotionEffect(PotionEffectType.DARKNESS);
            enemy.removePotionEffect(PotionEffectType.BLINDNESS);
        }
        for (Player player : d.caughtPlayers()) {
            player.resetPlayerTime();
        }
        for (UUID id : mobs) {
            Entity entity = Bukkit.getEntity(id);
            if (entity != null) {
                world.spawnParticle(Particle.SOUL, entity.getLocation().add(0, 1, 0), 12, 0.4, 0.6, 0.4, 0.02);
                entity.remove();
            }
        }
        mobs.clear();
        pillar = Pillar.NONE;
        enderUntil = -1;
        if (eye != null) {
            world.spawnParticle(Particle.PORTAL, eyeCenter, 60, 2.0, 2.0, 2.0, 0.8);
            eye.remove();
            eye = null;
        }
        takeBackTitansWrath();
    }

    @Override
    public void onFinished(Domain d) {
        for (TempBlocks group : tower) {
            group.remove();
        }
        tower.clear();
        if (eye != null) {
            eye.remove();
            eye = null;
        }
        service.unregister(casterId, this);
    }

    // ---- the column of fire ----

    /**
     * The pillar at the centre. First blue fire (a thick column with fire swirling round it); after turnToSmoke() a column
     * of smoke and souls. Either way it burns only above the obsidian as that rises, and is gone when the tower is done.
     */
    private void burn(Domain d) {
        if (pillar == Pillar.NONE || columnSince < 0) {
            return;
        }
        double bottom = cy + (towerPlan.isEmpty() ? 0 : settings.towerHeight() * (towerPlaced / (double) towerPlan.size()));
        if (pillar == Pillar.SMOKE && !towerPlan.isEmpty() && towerPlaced >= towerPlan.size()) {
            pillar = Pillar.NONE;           // the obsidian has taken over the whole column
            world.spawnParticle(Particle.SOUL, topOfTower(), 60, 1.0, 1.5, 1.0, 0.1);
            return;
        }

        double x = cx + 0.5;
        double z = cz + 0.5;
        if (pillar == Pillar.FIRE) {
            double rise = Math.min(1.0, (d.age() - columnSince) / (double) COLUMN_RISE_TICKS);
            double top = cy + settings.flameHeight() * rise;
            for (double y = bottom; y <= top; y += 0.4) {
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, x, y, z, 5, 0.45, 0.2, 0.45, 0.03);
            }
            // three arms of blue fire swirling up round the column, closer to it low down and wider higher up
            double turn = (d.age() - columnSince) * 0.45;
            for (double y = bottom; y <= top; y += 0.3) {
                double radius = 1.6 + 0.9 * Math.sin(y * 0.25 + turn * 0.3);
                for (int arm = 0; arm < 3; arm++) {
                    double angle = turn + y * 0.45 + arm * (Math.PI * 2 / 3);
                    world.spawnParticle(Particle.SOUL_FIRE_FLAME, x + Math.cos(angle) * radius, y, z + Math.sin(angle) * radius,
                        1, 0.04, 0.04, 0.04, 0.0);
                }
            }
            if (rise < 1.0) {                // sparks at the very top while the fire is still rising
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, x, top, z, 20, 0.6, 0.3, 0.6, 0.15);
            }
        } else {
            double top = cy + settings.flameHeight();
            for (double y = bottom; y <= top; y += 0.5) {
                world.spawnParticle(Particle.LARGE_SMOKE, x, y, z, 3, 0.5, 0.25, 0.5, 0.01);
                world.spawnParticle(Particle.SOUL, x, y, z, 2, 0.45, 0.25, 0.45, 0.02);
            }
        }
    }

    // ---- the obsidian tower ----

    /** Places the next few blocks of the plan. */
    private void buildTower() {
        if (!towerBuilding) {
            return;
        }
        int end = Math.min(towerPlan.size(), towerPlaced + towerPerTick);
        List<BlockPos> batch = new ArrayList<>(towerPlan.subList(towerPlaced, end));
        if (!batch.isEmpty()) {
            tower.add(BattlePlugin.getInstance().getTempBlockService().place(batch, Material.OBSIDIAN.createBlockData(), 0));
            BlockPos sample = batch.get(batch.size() - 1);
            world.spawnParticle(Particle.LAVA, sample.x() + 0.5, sample.y() + 0.5, sample.z() + 0.5, 3, 0.5, 0.3, 0.5, 0);
            if (towerPlaced % (towerPerTick * 3) == 0) {
                world.playSound(new Location(world, sample.x() + 0.5, sample.y(), sample.z() + 0.5), Sound.BLOCK_DEEPSLATE_PLACE,
                    SoundCategory.MASTER, 3.0f, 0.5f);
            }
        }
        towerPlaced = end;
        if (towerPlaced >= towerPlan.size()) {
            towerBuilding = false;
        }
    }

    /** If the scene was cut short, makes sure the tower and the eye are there anyway. */
    private void completeStructures() {
        pillar = Pillar.NONE;
        if (towerPlan.isEmpty()) {
            startTower();
        }
        if (towerBuilding || towerPlaced < towerPlan.size()) {
            List<BlockPos> rest = new ArrayList<>(towerPlan.subList(towerPlaced, towerPlan.size()));
            if (!rest.isEmpty()) {
                tower.add(BattlePlugin.getInstance().getTempBlockService().place(rest, Material.OBSIDIAN.createBlockData(), 0));
            }
            towerPlaced = towerPlan.size();
            towerBuilding = false;
        }
        if (eye == null) {
            eye = new EyeModel(eyeCenter, sceneEyeFacing, 1.0);
            eyePlacedAge = domain.age() - EYE_GROW_TICKS;
        }
        enderUntil = -1;
    }

    /** Anything standing inside the tower (the caster, after the scene) is moved just outside it, so nobody is buried. */
    private void pushOutOfTower() {
        BoundingBox column = new BoundingBox(cx - 1.3, cy - 0.2, cz - 1.3, cx + 2.3, cy + settings.towerHeight() + 0.2, cz + 2.3);
        for (Entity entity : world.getNearbyEntities(column)) {
            if (!(entity instanceof LivingEntity)) {
                continue;
            }
            Vector away = entity.getLocation().toVector().subtract(new Vector(cx + 0.5, entity.getLocation().getY(), cz + 0.5));
            away.setY(0);
            if (away.lengthSquared() < 1.0E-4) {
                away = new Vector(1, 0, 0);
            }
            away.normalize().multiply(3.4);
            double x = cx + 0.5 + away.getX();
            double z = cz + 0.5 + away.getZ();
            Location to = new Location(world, x, standY((int) Math.floor(x), (int) Math.floor(z)), z,
                entity.getLocation().getYaw(), entity.getLocation().getPitch());
            entity.teleport(to);
        }
    }

    /** The tower coming down, a few batches a tick. */
    private void crumble() {
        int perTick = Math.max(1, (int) Math.ceil(tower.size() / 15.0));
        for (int i = 0; i < perTick && !tower.isEmpty(); i++) {
            TempBlocks batch = tower.remove(ThreadLocalRandom.current().nextInt(tower.size()));
            batch.remove();
        }
        world.spawnParticle(Particle.POOF, cx + 0.5, cy + settings.towerHeight() * 0.5, cz + 0.5, 6, 1.0, 4.0, 1.0, 0.02);
    }

    /** The y a creature can stand at for this column: the first solid block with room above it, near the floor. */
    private int standY(int x, int z) {
        for (int y = cy + 4; y >= cy - 2; y--) {
            if (!world.getBlockAt(x, y, z).isPassable()
                && world.getBlockAt(x, y + 1, z).isPassable() && world.getBlockAt(x, y + 2, z).isPassable()) {
                return y + 1;
            }
        }
        return cy;
    }

    // ---- ender particles and the eye ----

    private void enderParticles(Domain d) {
        if (enderUntil < 0) {
            return;
        }
        if (d.age() > enderUntil) {
            enderUntil = -1;
            return;
        }
        Location top = topOfTower();
        world.spawnParticle(Particle.PORTAL, top, 40, 1.4, 1.2, 1.4, 0.9);
        world.spawnParticle(Particle.REVERSE_PORTAL, top, 12, 1.0, 1.0, 1.0, 0.05);
    }

    /** Grows the eye when it is placed, then keeps it gazing at the nearest enemy (or the caster) between Titan's Wraths. */
    private void animateEye(Domain d) {
        if (eye == null || eyeBusy || !eye.isValid() || d.age() % 2 != 0) {
            return;
        }
        double scale = Math.min(1.0, 0.1 + 0.9 * (d.age() - eyePlacedAge) / (double) EYE_GROW_TICKS);
        Vector facing = sceneEyeFacing;
        if (d.phase() == Phase.ACTIVE) {
            Player watched = nearestEnemy(d);
            if (watched == null && !d.caster().isDead()) {
                watched = d.caster();
            }
            if (watched != null) {
                Vector toward = watched.getLocation().add(0, 1, 0).toVector().subtract(eyeCenter.toVector());
                if (toward.lengthSquared() > 1.0E-6) {
                    facing = toward.normalize();
                }
            }
        }
        eye.update(facing, scale);
    }

    private Player nearestEnemy(Domain d) {
        Player best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Player enemy : d.enemies()) {
            double distance = enemy.getLocation().distanceSquared(eyeCenter);
            if (distance < bestDistance) {
                best = enemy;
                bestDistance = distance;
            }
        }
        return best;
    }

    // ---- the creatures of darkness ----

    private void spawnCreatures() {
        for (int i = 0; i < settings.endermen(); i++) {
            spawn(Enderman.class, randomSpot(0));
        }
        for (int i = 0; i < settings.phantoms(); i++) {
            spawn(Phantom.class, randomSpot(9));
        }
        for (int i = 0; i < settings.wardens(); i++) {
            spawn(Warden.class, randomSpot(0));
        }
    }

    private <T extends Mob> void spawn(Class<T> type, Location at) {
        T mob = world.spawn(at, type, created -> {
            created.setPersistent(false);               // never saved with the world
            created.setRemoveWhenFarAway(false);
            created.setCanPickupItems(false);
            service.tag(created, casterId);
        });
        mobs.add(mob.getUniqueId());
        world.spawnParticle(Particle.SOUL_FIRE_FLAME, at.clone().add(0, 1, 0), 25, 0.5, 1.0, 0.5, 0.05);
    }

    /** A random spot inside the dome, away from the tower, lifted by extra blocks (phantoms fly). */
    private Location randomSpot(int lift) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        double radius = domain.spec().radius() * 0.7;
        double angle = random.nextDouble(Math.PI * 2);
        double distance = 6 + random.nextDouble() * Math.max(1, radius - 6);
        double x = cx + 0.5 + Math.cos(angle) * distance;
        double z = cz + 0.5 + Math.sin(angle) * distance;
        return new Location(world, x, standY((int) Math.floor(x), (int) Math.floor(z)) + lift, z);
    }

    /** Points every creature at the nearest thing it may attack, and keeps the Warden's anger off the caster. */
    private void retarget(Domain d) {
        double reach = d.spec().radius();
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : world.getNearbyEntities(new Location(world, cx + 0.5, cy, cz + 0.5), reach, reach, reach)) {
            if (entity instanceof LivingEntity living && mayTarget(living)) {
                targets.add(living);
            }
        }
        for (UUID id : new ArrayList<>(mobs)) {
            Entity entity = Bukkit.getEntity(id);
            if (!(entity instanceof Mob mob) || !mob.isValid()) {
                mobs.remove(id);
                continue;
            }
            if (mob instanceof Warden warden && !settings.mobsTargetCaster()) {
                warden.clearAnger(d.caster());
            }
            LivingEntity best = null;
            double bestDistance = Double.MAX_VALUE;
            for (LivingEntity candidate : targets) {
                double distance = candidate.getLocation().distanceSquared(mob.getLocation());
                if (distance < bestDistance) {
                    best = candidate;
                    bestDistance = distance;
                }
            }
            if (best == null) {
                if (mob.getTarget() != null && !mayTarget(mob.getTarget())) {
                    mob.setTarget(null);
                }
                continue;
            }
            if (mob instanceof Warden warden) {
                warden.setAnger(best, 150);
            }
            mob.setTarget(best);
        }
    }

    // ---- Titan's Wrath ----

    private void grantTitansWrath(Domain d) {
        BattlePlayer battlePlayer = PlayerLogger.getBattlePlayer(casterId);
        if (battlePlayer == null) {
            return;
        }
        TitansWrath wrath = new TitansWrath(battlePlayer);
        if (battlePlayer.getLoadout().add(AbilitySlot.DOMAIN, wrath)) {
            wrathGranted = true;
            // select it, so the first left-click with the shard fires it
            for (int i = 0; i < battlePlayer.getLoadout().size(AbilitySlot.DOMAIN)
                && battlePlayer.getLoadout().current(AbilitySlot.DOMAIN) != wrath; i++) {
                battlePlayer.getLoadout().next(AbilitySlot.DOMAIN);
            }
            d.caster().sendActionBar(Component.text("Titan's Wrath unlocked", NamedTextColor.AQUA));
        }
    }

    private void takeBackTitansWrath() {
        if (!wrathGranted) {
            return;
        }
        wrathGranted = false;
        BattlePlayer battlePlayer = PlayerLogger.getBattlePlayer(casterId);
        if (battlePlayer == null) {
            return;
        }
        TitansWrath wrath = battlePlayer.getLoadout().find(TitansWrath.class);
        if (wrath != null) {
            BattlePlugin.getInstance().getAbilityScheduler().cancel(wrath);     // stops a beam in progress
        }
        battlePlayer.getLoadout().remove(TitansWrath.class);
    }
}
