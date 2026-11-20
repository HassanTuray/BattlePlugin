package com.hfturay.battleplugin.inputservice;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.hfturay.battleplugin.ability.AbilitySlot;
import com.hfturay.battleplugin.battleplayer.BattlePlayer;
import com.hfturay.battleplugin.items.AbilityItemFactory;

/**
 * Turns clicks with an ability item into ability actions. This is the only place that does it.
 *
 *   left-click  (air, block, or punching something)  -> use the selected ability on that item
 *   right-click (air, block, or on an entity)        -> select the next ability on that item
 *
 * Rules:
 *  - Only the main hand counts, so an item in the off hand cannot fire twice.
 *  - Only real ability items count (checked by their tag, not by material).
 *  - Holding an ability item cancels what the click would normally do: a punch does no vanilla damage,
 *    and a click on a block neither breaks it nor opens it. The ability is the whole effect.
 *  - The punch trigger runs before every other damage listener, so protection, immunity or another plugin
 *    cancelling the hit cannot stop the ability from firing.
 *  - The same click is never handled twice in one tick, whichever events the server sends for it.
 *  - Damage dealt by an ability looks like a punch to the server. It is ignored (see DamageService.isDealing()).
 *  - A throwing ability is logged and does not escape into the event.
 */
public final class InputService implements Listener {

    public enum Click { USE, CYCLE, CYCLE_BACK }

    private final Function<UUID, BattlePlayer> players;
    private final BooleanSupplier dealingDamage;
    private final Function<ItemStack, AbilitySlot> slotOf;
    private final IntSupplier clock;
    private final Logger logger;

    /** Per player: the tick each (slot, click) was last handled. */
    private final Map<UUID, int[]> lastHandled = new HashMap<>();

    public InputService(Function<UUID, BattlePlayer> players, BooleanSupplier dealingDamage, Logger logger) {
        this(players, dealingDamage, InputService::slotOfItem, Bukkit::getCurrentTick, logger);
    }

    public InputService(Function<UUID, BattlePlayer> players, BooleanSupplier dealingDamage,
                        Function<ItemStack, AbilitySlot> slotOf, IntSupplier clock, Logger logger) {
        this.players = players;
        this.dealingDamage = dealingDamage;
        this.slotOf = slotOf;
        this.clock = clock;
        this.logger = logger;
    }

    /** Which item this is, or null if it is not an ability item. */
    public static AbilitySlot slotOfItem(ItemStack item) {
        if (AbilityItemFactory.isAttackItem(item)) {
            return AbilitySlot.ATTACK;
        }
        if (AbilityItemFactory.isMobilityItem(item)) {
            return AbilitySlot.MOBILITY;
        }
        if (AbilityItemFactory.isElementalItem(item)) {
            return AbilitySlot.ELEMENTAL;
        }
        if (AbilityItemFactory.isBuffItem(item)) {
            return AbilitySlot.BUFF;
        }
        if (AbilityItemFactory.isShikigamiItem(item)) {
            return AbilitySlot.SHIKIGAMI;
        }
        if (AbilityItemFactory.isDomainItem(item)) {
            return AbilitySlot.DOMAIN;
        }
        return null;
    }

    /** Forget a player who left. */
    public void forget(UUID id) {
        lastHandled.remove(id);
    }

    // ---- server events: thin wrappers around the decisions below ----

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (handleInteract(event.getPlayer(), event.getHand(), event.getAction(), event.getItem())) {
            event.setCancelled(true);
        }
    }

    /** Lowest priority on purpose: the ability must fire before anything else can cancel the hit. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPunch(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        if (handlePunch(player, event.getCause(), player.getInventory().getItemInMainHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (event instanceof PlayerInteractAtEntityEvent) {
            return;     // armor stands send both events for one click; handle the plain one only
        }
        Player player = event.getPlayer();
        if (handleEntityInteract(player, event.getHand(), player.getInventory().getItem(event.getHand()))) {
            event.setCancelled(true);
        }
    }

    // ---- decisions. Each returns true when the original event should be cancelled ----

    public boolean handleInteract(Player player, EquipmentSlot hand, Action action, ItemStack item) {
        if (hand != EquipmentSlot.HAND || action == null || action == Action.PHYSICAL) {
            return false;
        }
        AbilitySlot slot = slotOf.apply(item);
        if (slot == null) {
            return false;
        }

        switch (action) {
            case LEFT_CLICK_AIR, LEFT_CLICK_BLOCK -> run(player, slot, Click.USE);
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> run(player, slot, player.isSneaking() ? Click.CYCLE_BACK : Click.CYCLE);
            default -> { }
        }
        return true;
    }

    public boolean handlePunch(Player damager, DamageCause cause, ItemStack mainHand) {
        if (dealingDamage.getAsBoolean()) {
            return false;       // this "punch" is damage an ability is dealing
        }
        if (cause != DamageCause.ENTITY_ATTACK && cause != DamageCause.ENTITY_SWEEP_ATTACK) {
            return false;
        }
        AbilitySlot slot = slotOf.apply(mainHand);
        if (slot == null) {
            return false;
        }

        if (cause == DamageCause.ENTITY_ATTACK) {
            run(damager, slot, Click.USE);
        }
        return true;
    }

    public boolean handleEntityInteract(Player player, EquipmentSlot hand, ItemStack item) {
        if (hand != EquipmentSlot.HAND) {
            return false;
        }
        AbilitySlot slot = slotOf.apply(item);
        if (slot == null) {
            return false;
        }

        run(player, slot, player.isSneaking() ? Click.CYCLE_BACK : Click.CYCLE);
        return true;
    }

    // ---- doing it ----

    private void run(Player player, AbilitySlot slot, Click click) {
        if (player.isDead() || player.getGameMode() == GameMode.SPECTATOR) {
            return;
        }
        BattlePlayer battlePlayer = players.apply(player.getUniqueId());
        if (battlePlayer == null) {
            return;     // no ability data (for example right after a reload)
        }
        if (!firstThisTick(player.getUniqueId(), slot, click)) {
            return;
        }

        try {
            if (click == Click.USE) {
                battlePlayer.activateCurrent(slot);
            } else if (click == Click.CYCLE_BACK) {
                battlePlayer.previousAbility(slot);
            } else {
                battlePlayer.nextAbility(slot);
            }
        } catch (RuntimeException e) {
            logger.log(Level.WARNING, "Ability input failed for " + player.getUniqueId() + " (" + slot + ", " + click + ")", e);
        }
    }

    private boolean firstThisTick(UUID id, AbilitySlot slot, Click click) {
        int now = clock.getAsInt();
        int[] last = lastHandled.computeIfAbsent(id, k -> {
            int[] fresh = new int[AbilitySlot.values().length * Click.values().length];
            java.util.Arrays.fill(fresh, Integer.MIN_VALUE);
            return fresh;
        });
        int index = slot.ordinal() * Click.values().length + click.ordinal();
        if (last[index] == now) {
            return false;
        }
        last[index] = now;
        return true;
    }
}
