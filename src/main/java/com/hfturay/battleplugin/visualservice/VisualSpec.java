package com.hfturay.battleplugin.visualservice;

/**
 * Immutable description of a visual made of entities. Define it once (a static constant per ability) and hand it
 * to the VisualService. Every play creates its own instance, so two rockets never share entities.
 */
@FunctionalInterface
public interface VisualSpec {

    /** Spawns the entities. If this throws, it must not leave any of them behind. */
    VisualInstance create(VisualContext context);
}
