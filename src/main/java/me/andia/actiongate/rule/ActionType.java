package me.andia.actiongate.rule;

public enum ActionType {
    BLOCK_BREAK(true),
    BLOCK_PLACE(true),
    FISH(false),
    USE_ITEM(true),
    INTERACT_BLOCK(true),
    ENTITY_INTERACT(true),
    ATTACK_PLAYER(false),
    ATTACK_MOB(false),
    CRAFT(true),
    ENCHANT(false),
    ELYTRA(false),
    PORTAL(true),
    ITEM_DROP(true),
    ITEM_PICKUP(true);

    private final boolean targetsRequired;

    ActionType(boolean targetsRequired) {
        this.targetsRequired = targetsRequired;
    }

    public boolean targetsRequired() {
        return targetsRequired;
    }

    public boolean targetsAreMaterials() {
        return switch (this) {
            case BLOCK_BREAK, BLOCK_PLACE, USE_ITEM, INTERACT_BLOCK, CRAFT, ENCHANT,
                    ITEM_DROP, ITEM_PICKUP -> true;
            default -> false;
        };
    }

    public boolean targetsAreEntities() {
        return this == ENTITY_INTERACT || this == ATTACK_MOB;
    }

    public boolean targetsSupported() {
        return targetsRequired || this == ATTACK_MOB || this == ENCHANT;
    }
}
