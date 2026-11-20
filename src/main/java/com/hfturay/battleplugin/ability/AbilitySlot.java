package com.hfturay.battleplugin.ability;

/** Which of a player's four ability items an ability lives on. */
public enum AbilitySlot {
    ATTACK("attack"),
    MOBILITY("mobility"),
    BUFF("buff"),
    ELEMENTAL("elemental"),
    SHIKIGAMI("shikigami"),
    DOMAIN("domain");

    private final String label;

    AbilitySlot(String label) {
        this.label = label;
    }

    /** Lower-case name for messages: "attack", "mobility", "buff". */
    public String label() {
        return label;
    }
}
