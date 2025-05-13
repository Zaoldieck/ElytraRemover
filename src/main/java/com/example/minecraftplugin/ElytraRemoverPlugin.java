package com.example.minecraftplugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Boat;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Enum for the different actions that can be taken when finding an elytra
 */
enum ElytraAction {
    REMOVE_ELYTRA,    // Just remove the elytra from the item frame
    REMOVE_FRAME,     // Remove the entire item frame
    REPLACE_WITH_APPLE // Replace the elytra with an apple
}

/**
 * Main plugin class that removes elytras from item frames in End boats
 */
public class ElytraRemoverPlugin extends JavaPlugin implements Listener {
    
    // Settings that will be loaded from config
    private int endShipMinY = 60;
    private int endShipMaxY = 100;
    private boolean removeAllEndElytras = true;
    private int scanIntensity = 2;
    private ElytraAction elytraAction = ElytraAction.REPLACE_WITH_APPLE; // Default to replacing with apple
    
    // Set to track processed chunks to avoid excessive duplicate checks
    private final Set<String> recentlyProcessedChunks = new HashSet<>();
    
    @Override
    public void onEnable() {
        // Plugin startup logic
        getLogger().info("ElytraRemover has been enabled!");
        getLogger().info("Created by Zaoldieck - Thanks for using my plugin!");
        
        // Save default config
        saveDefaultConfig();
        
        // Load configuration
        loadConfiguration();
        
        // Register event listeners
        getServer().getPluginManager().registerEvents(new ElytraFrameListener(this), this);
        
        // Also register this class as a listener for chunk load events
        getServer().getPluginManager().registerEvents(this, this);
        
        // Start a periodic task to check for elytras in item frames in end ships
        scheduleElytraRemovalTask();
        
        // Run an immediate check when the plugin starts for all loaded chunks
        getServer().getScheduler().runTaskLater(this, () -> {
            getLogger().info("Running initial scan for elytras in The End...");
            for (World world : Bukkit.getWorlds()) {
                if (world.getEnvironment() == Environment.THE_END) {
                    for (Chunk chunk : world.getLoadedChunks()) {
                        scanChunkForEndShips(chunk);
                    }
                }
            }
        }, 40L); // Wait 2 seconds after server start to ensure worlds are fully loaded
        
        // Periodically clean up the chunk tracking set
        getServer().getScheduler().runTaskTimer(this, () -> {
            recentlyProcessedChunks.clear();
            getLogger().fine("Cleared recently processed chunks cache");
        }, 20L * 60 * 5, 20L * 60 * 5); // Every 5 minutes
    }
    
    /**
     * Loads configuration settings from config.yml
     */
    private void loadConfiguration() {
        // Reload the config from disk
        reloadConfig();
        
        // Load end ship detection settings
        endShipMinY = getConfig().getInt("end-ship.min-y", 60);
        endShipMaxY = getConfig().getInt("end-ship.max-y", 100);
        removeAllEndElytras = getConfig().getBoolean("end-ship.remove-all-end-elytras", true);
        scanIntensity = getConfig().getInt("end-ship.scan-intensity", 2);
        
        // Load the action to take when finding an elytra
        String actionString = getConfig().getString("end-ship.action", "REPLACE_WITH_APPLE");
        try {
            elytraAction = ElytraAction.valueOf(actionString);
        } catch (IllegalArgumentException e) {
            getLogger().warning("Invalid elytra action in config: " + actionString + ". Using default: REPLACE_WITH_APPLE");
            elytraAction = ElytraAction.REPLACE_WITH_APPLE;
        }
        
        getLogger().info("Configuration loaded successfully:");
        getLogger().info("- End ship Y range: " + endShipMinY + " to " + endShipMaxY);
        getLogger().info("- Remove all End elytras: " + removeAllEndElytras);
        getLogger().info("- Scan intensity: " + scanIntensity);
        getLogger().info("- Elytra action: " + elytraAction);
    }
    
    @Override
    public void onDisable() {
        // Plugin shutdown logic
        getLogger().info("ElytraRemover has been disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("elytraremover")) {
            if (args.length == 0) {
                sender.sendMessage("§aElytraRemover §7v1.0.0 §f- Created by §6Zaoldieck");
                sender.sendMessage("§7/elytraremover reload §f- Reload configuration");
                sender.sendMessage("§7/elytraremover scan §f- Force scan all loaded chunks");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("reload")) {
                // Reload config
                reloadConfig();
                sender.sendMessage("§aElytraRemover configuration reloaded!");
                return true;
            }
            
            if (args[0].equalsIgnoreCase("scan")) {
                sender.sendMessage("§aStarting AGGRESSIVE manual scan for elytras in The End...");
                
                // Do this immediately and directly on the main thread for best results
                recentlyProcessedChunks.clear(); // Clear cache for a full rescan
                int count = 0;
                int elytraFound = 0;
                int itemFramesFound = 0;
                
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() == Environment.THE_END) {
                        sender.sendMessage("§7Scanning end world: " + world.getName());
                        
                        // First scan chunks
                        for (Chunk chunk : world.getLoadedChunks()) {
                            scanChunkForEndShips(chunk);
                            count++;
                        }
                        
                        // Then do a DIRECT entity scan (most reliable)
                        for (Entity entity : world.getEntities()) {
                            if (entity.getType() == EntityType.ITEM_FRAME) {
                                itemFramesFound++;
                                ItemFrame frame = (ItemFrame) entity;
                                ItemStack item = frame.getItem();
                                
                                if (item != null) {
                                    sender.sendMessage("§7Item frame found at " + 
                                                    frame.getLocation().getBlockX() + ", " + 
                                                    frame.getLocation().getBlockY() + ", " + 
                                                    frame.getLocation().getBlockZ() + 
                                                    " contains: " + item.getType().name());
                                    
                                    if (item.getType() == Material.ELYTRA) {
                                        elytraFound++;
                                        removeElytraFromFrame(frame);
                                        sender.sendMessage("§c→ ELYTRA REMOVED from item frame at: " + 
                                                        frame.getLocation().getBlockX() + ", " + 
                                                        frame.getLocation().getBlockY() + ", " + 
                                                        frame.getLocation().getBlockZ());
                                    }
                                }
                            }
                        }
                    }
                }
                
                sender.sendMessage("§aScan complete! Checked " + count + " chunks.");
                sender.sendMessage("§aFound " + itemFramesFound + " item frames, found " + elytraFound + " elytras.");
                
                // Show what action was taken
                switch (elytraAction) {
                    case REMOVE_ELYTRA:
                        sender.sendMessage("§aAction taken: Removed elytras from item frames");
                        break;
                    case REMOVE_FRAME:
                        sender.sendMessage("§aAction taken: Removed item frames containing elytras near boats");
                        break;
                    case REPLACE_WITH_APPLE:
                        sender.sendMessage("§aAction taken: Replaced elytras with apples in item frames");
                        break;
                }
                
                return true;
            }
            
            sender.sendMessage("§cUnknown command. Use /elytraremover for help.");
            return true;
        }
        
        return false;
    }
    
    /**
     * Handle chunk load events to immediately scan new chunks
     */
    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event) {
        if (event.getWorld().getEnvironment() == Environment.THE_END) {
            // Schedule a delayed task to check the chunk after it's fully loaded
            getServer().getScheduler().runTaskLater(this, () -> {
                scanChunkForEndShips(event.getChunk());
            }, 10L); // Wait half a second after chunk load
        }
    }
    
    /**
     * Schedules a periodic task to check and remove elytras from item frames in End ships
     */
    private void scheduleElytraRemovalTask() {
        // More frequent checks - every 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                // Check all End worlds
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() == Environment.THE_END) {
                        // Instead of checking the entire world, focus on loaded chunks
                        for (Chunk chunk : world.getLoadedChunks()) {
                            scanChunkForEndShips(chunk);
                        }
                    }
                }
            }
        }.runTaskTimer(this, 20L * 5, 20L * 5); // Run every 5 seconds (20 ticks * 5)
        
        // Second task that does a complete scan of all entities every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                getLogger().info("Running full entity scan for elytras in The End...");
                for (World world : Bukkit.getWorlds()) {
                    if (world.getEnvironment() == Environment.THE_END) {
                        // Direct entity scan - more thorough
                        for (Entity entity : world.getEntities()) {
                            if (entity.getType() == EntityType.ITEM_FRAME) {
                                ItemFrame frame = (ItemFrame) entity;
                                ItemStack item = frame.getItem();
                                if (item != null && item.getType() == Material.ELYTRA) {
                                    removeElytraFromFrame(frame);
                                    getLogger().info("Deep scan removed elytra from item frame at: " + 
                                                    frame.getLocation().getBlockX() + ", " + 
                                                    frame.getLocation().getBlockY() + ", " + 
                                                    frame.getLocation().getBlockZ());
                                }
                            }
                        }
                    }
                }
                getLogger().info("Full entity scan complete!");
            }
        }.runTaskTimer(this, 20L * 20, 20L * 60); // Run every minute (first after 20 seconds)
    }
    
    /**
     * Scans a chunk specifically looking for elytras in item frames
     * @param chunk The chunk to scan
     */
    public void scanChunkForEndShips(Chunk chunk) {
        String chunkKey = chunk.getX() + ":" + chunk.getZ() + ":" + chunk.getWorld().getName();
        
        // Skip if we've recently processed this chunk
        if (recentlyProcessedChunks.contains(chunkKey)) {
            return;
        }
        
        // Add to recently processed
        recentlyProcessedChunks.add(chunkKey);
        
        // Find all item frames in the chunk - directly focus on removing elytras
        for (Entity entity : chunk.getEntities()) {
            // Skip if not an item frame
            if (entity.getType() != EntityType.ITEM_FRAME) {
                continue;
            }
            
            ItemFrame itemFrame = (ItemFrame) entity;
            
            // Check if the item frame has an elytra - this is all we care about
            ItemStack displayedItem = itemFrame.getItem();
            if (displayedItem != null && displayedItem.getType() == Material.ELYTRA) {
                // Immediately remove the elytra - don't check for ships, just remove all elytras
                // in all item frames in The End dimension
                removeElytraFromFrame(itemFrame);
                getLogger().info("Removed elytra from item frame at: " + 
                                itemFrame.getLocation().getBlockX() + ", " + 
                                itemFrame.getLocation().getBlockY() + ", " + 
                                itemFrame.getLocation().getBlockZ());
            }
        }
        
        // Also do a second pass to look for any nearby boats to ensure we don't miss anything
        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof Boat) {
                // If we find a boat, check all nearby item frames within 10 blocks
                for (Entity nearbyEntity : entity.getNearbyEntities(10, 10, 10)) {
                    if (nearbyEntity instanceof ItemFrame) {
                        ItemFrame frame = (ItemFrame) nearbyEntity;
                        ItemStack item = frame.getItem();
                        if (item != null && item.getType() == Material.ELYTRA) {
                            // This is definitely in a boat - remove it
                            removeElytraFromFrame(frame);
                            getLogger().info("Removed elytra from item frame near boat at: " + 
                                            frame.getLocation().getBlockX() + ", " + 
                                            frame.getLocation().getBlockY() + ", " + 
                                            frame.getLocation().getBlockZ());
                        }
                    }
                }
            }
        }
    }
    
    /**
     * We no longer need to check if an item frame is in an end ship
     * Just remove ALL elytras from ALL item frames in The End dimension
     * @param itemFrame The item frame to check
     * @return always returns true to remove all elytras
     */
    private boolean isLikelyInEndShip(ItemFrame itemFrame) {
        // Don't bother with any checks, just remove ALL elytras
        return true;
    }
    
    /**
     * Takes action on an item frame with an elytra based on the configured action
     * @param itemFrame The item frame containing the elytra
     */
    private void removeElytraFromFrame(ItemFrame itemFrame) {
        String location = itemFrame.getLocation().getBlockX() + ", " + 
                         itemFrame.getLocation().getBlockY() + ", " + 
                         itemFrame.getLocation().getBlockZ();
        
        // Check if this item frame is near a boat - if so, we're dealing with an end ship boat
        boolean isNearBoat = false;
        for (Entity nearby : itemFrame.getNearbyEntities(5, 5, 5)) {
            if (nearby instanceof Boat) {
                isNearBoat = true;
                break;
            }
        }
        
        // Take action based on configuration
        switch (elytraAction) {
            case REMOVE_ELYTRA:
                // Just remove the elytra (original behavior)
                itemFrame.setItem(null);
                getLogger().info("Removed elytra from item frame at: " + location);
                break;
                
            case REMOVE_FRAME:
                // Remove the entire item frame if it's in a boat
                if (isNearBoat) {
                    itemFrame.remove();
                    getLogger().info("Removed entire item frame (with elytra) at: " + location);
                } else {
                    // If not near a boat, just remove the elytra
                    itemFrame.setItem(null);
                    getLogger().info("Removed elytra from item frame at: " + location);
                }
                break;
                
            case REPLACE_WITH_APPLE:
                // Replace the elytra with an apple
                itemFrame.setItem(new ItemStack(Material.APPLE));
                getLogger().info("Replaced elytra with apple in item frame at: " + location);
                break;
                
            default:
                // Fallback - just remove the elytra
                itemFrame.setItem(null);
                getLogger().info("Removed elytra from item frame at: " + location + " (default action)");
                break;
        }
    }
}