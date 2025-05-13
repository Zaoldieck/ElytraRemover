package com.example.minecraftplugin;

import org.bukkit.Material;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.world.ChunkPopulateEvent;
import org.bukkit.inventory.ItemStack;

import java.util.logging.Level;

/**
 * Listener for item frame related events to prevent placing elytras in item frames in End ships
 */
public class ElytraFrameListener implements Listener {
    
    private final ElytraRemoverPlugin plugin;
    
    public ElytraFrameListener(ElytraRemoverPlugin plugin) {
        this.plugin = plugin;
    }
    
    /**
     * Event handler for when a player interacts with an entity, including item frames
     * Prevents placing elytras in item frames in the End
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        // Check if the entity is an item frame
        if (event.getRightClicked().getType() == EntityType.ITEM_FRAME) {
            // Check if we're in the End
            if (event.getPlayer().getWorld().getEnvironment() == Environment.THE_END) {
                ItemFrame frame = (ItemFrame) event.getRightClicked();
                
                // Check if the player is holding an elytra
                ItemStack heldItem = event.getPlayer().getInventory().getItemInMainHand();
                if (heldItem != null && heldItem.getType() == Material.ELYTRA) {
                    // Cancel the event in all cases when trying to place an elytra in an item frame in The End
                    event.setCancelled(true);
                    plugin.getLogger().log(Level.INFO, 
                        "Prevented " + event.getPlayer().getName() + " from placing an elytra in an item frame in The End");
                    
                    // Notify the player
                    event.getPlayer().sendMessage("§cElytras cannot be placed in item frames in The End.");
                    return;
                }
                
                // Also check off-hand
                ItemStack offHandItem = event.getPlayer().getInventory().getItemInOffHand();
                if (offHandItem != null && offHandItem.getType() == Material.ELYTRA) {
                    // Cancel the event
                    event.setCancelled(true);
                    plugin.getLogger().log(Level.INFO, 
                        "Prevented " + event.getPlayer().getName() + " from placing an elytra in an item frame in The End (off-hand)");
                    
                    // Notify the player
                    event.getPlayer().sendMessage("§cElytras cannot be placed in item frames in The End.");
                }
            }
        }
    }
    
    /**
     * Event handler for when a hanging entity (like an item frame) is placed
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onHangingPlace(HangingPlaceEvent event) {
        // Check if the hanging entity is an item frame and we're in the End
        if (event.getEntity().getType() == EntityType.ITEM_FRAME &&
            event.getEntity().getWorld().getEnvironment() == Environment.THE_END) {
            
            Player player = event.getPlayer();
            if (player != null) {
                // If a player is holding an elytra, don't let them place an item frame in The End
                if ((player.getInventory().getItemInMainHand() != null && 
                     player.getInventory().getItemInMainHand().getType() == Material.ELYTRA) ||
                    (player.getInventory().getItemInOffHand() != null && 
                     player.getInventory().getItemInOffHand().getType() == Material.ELYTRA) ||
                    (player.getItemOnCursor() != null && 
                     player.getItemOnCursor().getType() == Material.ELYTRA)) {
                    
                    event.setCancelled(true);
                    plugin.getLogger().log(Level.INFO, 
                        "Prevented " + player.getName() + " from placing an item frame in The End while holding an elytra");
                    player.sendMessage("§cYou cannot place item frames in The End while holding an elytra.");
                }
            }
        }
    }
    
    /**
     * Handle chunk populate events to check new chunks
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onChunkPopulate(ChunkPopulateEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            // Schedule a delayed check to make sure all entities are properly loaded
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                plugin.scanChunkForEndShips(event.getChunk());
            }, 20L); // Check 1 second after chunk population
        }
    }
    
    /**
     * Track when item frames spawn in The End
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.ITEM_FRAME && 
            event.getLocation().getWorld().getEnvironment() == Environment.THE_END) {
            
            // Schedule a check shortly after the entity spawns to see if it's an elytra frame
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (event.getEntity() instanceof ItemFrame) {
                    ItemFrame frame = (ItemFrame) event.getEntity();
                    ItemStack item = frame.getItem();
                    
                    if (item != null && item.getType() == Material.ELYTRA) {
                        // Always remove elytras from item frames in The End
                        frame.setItem(null);
                        plugin.getLogger().info("Removed elytra from newly spawned item frame in The End at " + 
                                                frame.getLocation().toString());
                    }
                }
            }, 5L); // Check 5 ticks after spawn
        }
    }
    
    /**
     * Determines if an area is likely part of an end ship
     * @param location The location to check
     * @return true if the location is likely part of an end ship
     */
    public boolean isEndShipArea(Block block) {
        // Check common end ship materials
        Material type = block.getType();
        return type == Material.PURPUR_BLOCK || 
               type == Material.PURPUR_PILLAR || 
               type == Material.PURPUR_STAIRS || 
               type == Material.END_ROD;
    }
}