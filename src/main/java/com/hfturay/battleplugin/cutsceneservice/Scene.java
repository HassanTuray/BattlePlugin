package com.hfturay.battleplugin.cutsceneservice;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 * One cutscene: a camera route plus things that happen at set moments (titles, sounds, particles, anything).
 *
 * Build one with Scene.builder("name", path).at(40, ctx -> ...).build(). The scene is just data; CutsceneService
 * plays it for a group of viewers around an anchor (the caster's position and facing).
 */
public final class Scene {

    /**
     * What a cue can use while it runs. anchor: where the scene is centred; camera: where the camera is right now (for
     * things that must face it); cameraEntity: the entity the viewers are looking through.
     */
    public record Context(Location anchor, List<Player> viewers, int tick, Location camera, Entity cameraEntity) { }

    /** Something that happens when the scene reaches a tick. */
    public record Cue(int tick, Consumer<Context> action) { }

    private final String name;
    private final CameraPath path;
    private final List<Cue> cues;
    private final double lockRadius;
    private final boolean livingCamera;

    private Scene(String name, CameraPath path, List<Cue> cues, double lockRadius, boolean livingCamera) {
        this.name = name;
        this.path = path;
        this.cues = cues;
        this.lockRadius = lockRadius;
        this.livingCamera = livingCamera;
    }

    public String name() {
        return name;
    }

    public CameraPath path() {
        return path;
    }

    public List<Cue> cues() {
        return cues;
    }

    /** Mobs (including shikigami) within this many blocks of the anchor are frozen and protected during the scene. */
    public double lockRadius() {
        return lockRadius;
    }

    public int totalTicks() {
        return path.totalTicks();
    }

    /** True if the camera is a living entity (see Builder.livingCamera), false for the usual smooth display camera. */
    public boolean livingCamera() {
        return livingCamera;
    }

    public static Builder builder(String name, CameraPath path) {
        return new Builder(name, path);
    }

    public static final class Builder {
        private final String name;
        private final CameraPath path;
        private final List<Cue> cues = new ArrayList<>();
        private double lockRadius = 35;
        private boolean livingCamera = false;

        private Builder(String name, CameraPath path) {
            this.name = name;
            this.path = path;
        }

        /** Runs the action when the scene reaches this tick (20 ticks = 1 second). */
        public Builder at(int tick, Consumer<Context> action) {
            cues.add(new Cue(tick, action));
            return this;
        }

        public Builder lockRadius(double blocks) {
            this.lockRadius = blocks;
            return this;
        }

        /**
         * Looks through an invisible marker armor stand instead of the usual display entity. A living camera can be
         * given effects the viewers see the world through (the game draws fog from the camera's effects, so the
         * Seattle Mist fog uses this). Its movement is not smoothed, so use it for cameras that hold still or move
         * slowly.
         */
        public Builder livingCamera() {
            this.livingCamera = true;
            return this;
        }

        public Scene build() {
            return new Scene(name, path, List.copyOf(cues), lockRadius, livingCamera);
        }
    }
}
