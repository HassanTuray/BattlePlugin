package com.hfturay.battleplugin.domainservice.seattlemist;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import com.hfturay.battleplugin.BattlePlugin;
import com.hfturay.battleplugin.cutsceneservice.CameraPath;
import com.hfturay.battleplugin.cutsceneservice.Scene;
import com.hfturay.battleplugin.cutsceneservice.Scenes;
import com.hfturay.battleplugin.domainservice.DomainService.Behavior;
import com.hfturay.battleplugin.domainservice.DomainService.Domain;
import com.hfturay.battleplugin.shikigamiservice.Shikigami;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

/**
 * Domain: Seattle Mist. Once the dome closes, a cutscene holds the camera still, high up near the dome's back wall and
 * looking at the middle, while a mist fills the dome until the camera can barely see; then "The Mist is Here" appears
 * in black. The mist stays for the rest of the domain, and the Mist Stalker is let loose in it. Everything is taken
 * away when it ends.
 *
 * The mist: Minecraft has no setting for fog thickness, so the dome is filled with large, faint white squares (text
 * displays with a see-through white background) that always turn to face whoever looks at them. Each one is barely
 * there, but looking through many of them everything fades to white, so the farther away something is, the more mist
 * is in front of it, like real fog. More are added a few at a time during the cutscene, so it thickens.
 *
 * One of these is made for every cast, with the numbers read from abilities.yml at that moment.
 */
public final class SeattleMistEffects implements Behavior {

    /**
     * The numbers. puffs: how many mist squares fill the dome; size: how big each is (blocks); opacity: how solid
     * each is (0 = invisible, 255 = solid white); affectsCaster: false hides the mist from the caster, who then sees
     * clearly.
     */
    public record Settings(int puffs, double size, int opacity, boolean affectsCaster) { }

    /** The cutscene: the mist builds up over MIST_TICKS, then the words appear; the scene ends at SCENE_TICKS. */
    private static final int MIST_TICKS = 160;
    private static final int WORDS_TICK = 165;
    private static final int SCENE_TICKS = 230;
    /** How often during the build-up a batch of mist is added. */
    private static final int MIST_EVERY = 2;
    /** The mist's colour, a pale cool grey-white. */
    private static final int MIST_RED = 228, MIST_GREEN = 232, MIST_BLUE = 236;
    /**
     * A text display's background for one space is about 0.15 blocks wide and 0.25 tall before scaling, so a square
     * of `size` blocks is stretched by size / those.
     */
    private static final float BACKGROUND_WIDTH = 0.15f, BACKGROUND_HEIGHT = 0.25f;

    private final Settings settings;
    /** Lets the Mist Stalker loose (SeattleMist knows how to make one). */
    private final Function<Location, Shikigami> stalker;

    private Domain domain;
    private final List<TextDisplay> mist = new ArrayList<>();
    private Shikigami released;

    public SeattleMistEffects(Settings settings, Function<Location, Shikigami> stalker) {
        this.settings = settings;
        this.stalker = stalker;
    }

    // ---- the cutscene ----

    /**
     * About 11 seconds. The camera does not move: it stays near the back wall of the dome, high up, looking down at the
     * middle so most of the dome can be seen. Mist is added every MIST_EVERY ticks until MIST_TICKS, then the words.
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
            .at(0, ctx -> Scenes.sound(ctx.viewers(), Sound.AMBIENT_CAVE, 0.6f));
        int batches = MIST_TICKS / MIST_EVERY;
        for (int i = 0; i < batches; i++) {
            int tick = i * MIST_EVERY;
            // spread the puffs evenly over the batches, so the mist thickens steadily
            int from = settings.puffs() * i / batches;
            int to = settings.puffs() * (i + 1) / batches;
            scene.at(tick, ctx -> addMist(to - from));
        }
        scene.at(WORDS_TICK, ctx -> {
            Title words = Title.title(Component.text("The Mist is Here", NamedTextColor.BLACK), Component.empty(),
                Title.Times.times(Duration.ofMillis(600), Duration.ofSeconds(3), Duration.ofMillis(1200)));
            for (Player viewer : ctx.viewers()) {
                viewer.showTitle(words);
            }
            Scenes.sound(ctx.viewers(), Sound.BLOCK_BELL_RESONATE, 0.5f);
        });
        return scene.build();
    }

    // ---- the mist ----

    /** Adds this many mist squares at random places inside the dome. */
    private void addMist(int count) {
        if (domain == null) {
            return;
        }
        World world = domain.world();
        Location center = domain.center();
        double reach = domain.spec().radius() - 1.5;
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Player caster = domain.caster();

        for (int i = 0; i < count; i++) {
            // a random point inside the half sphere, a little in from the wall
            double x, y, z;
            do {
                x = random.nextDouble(-reach, reach);
                y = random.nextDouble(0.5, reach);
                z = random.nextDouble(-reach, reach);
            } while (x * x + y * y + z * z > reach * reach);
            Location at = new Location(world, center.getBlockX() + 0.5 + x, center.getBlockY() + y, center.getBlockZ() + 0.5 + z);

            TextDisplay puff = world.spawn(at, TextDisplay.class, display -> {
                display.text(Component.text(" "));
                display.setBackgroundColor(Color.fromARGB(settings.opacity(), MIST_RED, MIST_GREEN, MIST_BLUE));
                display.setBillboard(Display.Billboard.CENTER);            // always faces whoever is looking
                display.setBrightness(new Display.Brightness(15, 15));      // a glowing white, not dark in the dome
                display.setShadowed(false);
                float size = (float) settings.size();
                display.setTransformation(new Transformation(
                    new Vector3f(0, -size / 2, 0),                          // centred on its spot, not standing on it
                    new AxisAngle4f(),
                    new Vector3f(size / BACKGROUND_WIDTH, size / BACKGROUND_HEIGHT, 1),
                    new AxisAngle4f()));
                display.setPersistent(false);                               // never saved with the world
            });
            if (!settings.affectsCaster() && caster.isOnline()) {
                caster.hideEntity(BattlePlugin.getInstance(), puff);
            }
            mist.add(puff);
        }
    }

    private void clearMist() {
        for (TextDisplay puff : mist) {
            if (puff.isValid()) {
                puff.remove();
            }
        }
        mist.clear();
    }

    // ---- hooks ----

    @Override
    public void onCast(Domain d) {
        this.domain = d;
        d.world().playSound(d.center(), Sound.WEATHER_RAIN_ABOVE, SoundCategory.MASTER, 2.0f, 0.5f);
    }

    @Override
    public void onActive(Domain d) {
        // the cutscene may have been cut short: make sure the mist is all there
        int missing = settings.puffs() - mist.size();
        if (missing > 0) {
            addMist(missing);
        }
        released = stalker.apply(d.center().add(0.5, 0, 0.5));
    }

    @Override
    public void onEnd(Domain d) {
        if (released != null) {
            released.kill();                // it fades with the mist
            released = null;
        }
        clearMist();
    }

    @Override
    public void onFinished(Domain d) {
        clearMist();
    }
}
