package com.prismcore.survival.manager;

import com.h2ph.PrismSurvival;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.util.*;

public class CarouselManager {

    private final PrismSurvival plugin;
    private final Map<UUID, BukkitTask> activeTasks = new HashMap<>();
    private final Map<UUID, ItemStack> pendingRewards = new HashMap<>();
    private final Random random = new Random();
    private final Sound[] carouselSounds = {
            Sound.BLOCK_NOTE_BLOCK_PLING,
            Sound.BLOCK_NOTE_BLOCK_HAT,
            Sound.BLOCK_NOTE_BLOCK_BIT
    };

    private final Map<UUID, BukkitTask> backgroundTasks = new HashMap<>();

    public CarouselManager(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public void openCarouselGUI(Player player, String crateName) {
        Inventory gui = Bukkit.createInventory(null, 27,
                ChatColor.translateAlternateColorCodes('&', "&8ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ"));

        // Setup Static Items
        setupStaticItems(gui);

        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (crateFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);

            // 1. Load Configured Items
            if (config.contains("contents")) {
                ConfigurationSection contents = config.getConfigurationSection("contents");
                for (String key : contents.getKeys(false)) {
                    try {
                        int slot = Integer.parseInt(key);
                        ItemStack item = config.getItemStack("contents." + key);
                        if (item != null && item.getType() != Material.AIR) {
                            gui.setItem(slot, item);
                        }
                    } catch (NumberFormatException e) {
                    }
                }
            }

            // 2. Fill Empty Slots with Random Items from Crate
            List<ItemStack> rewardPool = getCrateContents(config);
            if (!rewardPool.isEmpty()) {
                // Fill Carousel Slots (10-16) if empty
                for (int i = 10; i <= 16; i++) {
                    if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                        gui.setItem(i, getRandomItem(rewardPool));
                    }
                }

                // Fill Filler Slots (Background) if empty
                for (int i = 0; i < gui.getSize(); i++) {
                    if (i == 4 || i == 22)
                        continue; // Skip buttons
                    if (i >= 10 && i <= 16)
                        continue; // Skip Carousel
                    if (gui.getItem(i) == null || gui.getItem(i).getType() == Material.AIR) {
                        gui.setItem(i, getRandomItem(rewardPool));
                    }
                }
            }
        }

        player.openInventory(gui);
        player.setMetadata("prism_active_crate_type", new org.bukkit.metadata.FixedMetadataValue(plugin, "CAROUSEL"));
        player.setMetadata("prism_active_crate", new org.bukkit.metadata.FixedMetadataValue(plugin, crateName));

        // Start Background Animation Immediately (Slow: 5 ticks)
        startBackgroundAnimation(player, gui, 5L);
    }

    private void startBackgroundAnimation(Player player, Inventory gui, long period) {
        // Stop existing background task if any
        if (backgroundTasks.containsKey(player.getUniqueId())) {
            backgroundTasks.get(player.getUniqueId()).cancel();
            backgroundTasks.remove(player.getUniqueId());
        }

        // Define path order for "Clockwise Swirl" rotation including all background
        // slots
        // Range updated for 10-16 carousel:
        // Top Row: 0, 1, 2, 3, 5, 6, 7, 8
        // Right Side: 17 (16 is now carousel)
        // Bottom Row (Reverse): 26, 25, 24, 23, 21, 20, 19, 18
        // Left Side: 9 (10 is now carousel)
        List<Integer> backgroundSlots = Arrays.asList(
                0, 1, 2, 3, 5, 6, 7, 8,
                17,
                26, 25, 24, 23, 21, 20, 19, 18,
                9);

        BukkitTask task = plugin.getSchedulerAdapter().runTaskTimer(new Runnable() {
            @Override
            public void run() {
                // Optimize: Check if player is online and viewing THIS crate
                if (!player.isOnline() || !player.getOpenInventory().getTitle().contains("ᴄʟɪᴄᴋ ѕᴛᴀʀᴛ ᴛᴏ ѕᴘɪɴ")) {
                    handleClose(player);
                    return;
                }

                // Shift Background Items
                ItemStack lastItem = gui.getItem(backgroundSlots.get(backgroundSlots.size() - 1));

                // Shift forward
                for (int i = backgroundSlots.size() - 1; i > 0; i--) {
                    int currentSlot = backgroundSlots.get(i);
                    int prevSlot = backgroundSlots.get(i - 1);
                    gui.setItem(currentSlot, gui.getItem(prevSlot));
                }
                gui.setItem(backgroundSlots.get(0), lastItem);
            }
        }, period, period);

        backgroundTasks.put(player.getUniqueId(), task);
    }

    private void setupStaticItems(Inventory gui) {
        ItemStack rewardPointer = new ItemStack(Material.ENDER_EYE);
        ItemMeta pointerMeta = rewardPointer.getItemMeta();
        pointerMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aʏᴏᴜʀ ʀᴇᴡᴀʀᴅ"));
        rewardPointer.setItemMeta(pointerMeta);
        gui.setItem(4, rewardPointer);

        ItemStack startButton = new ItemStack(Material.OAK_SIGN);
        ItemMeta startMeta = startButton.getItemMeta();
        startMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', "&aѕᴛᴀʀᴛ"));
        startButton.setItemMeta(startMeta);
        gui.setItem(22, startButton);
    }

    public void handleStartClick(Player player, String crateName, Inventory gui) {
        if (activeTasks.containsKey(player.getUniqueId()))
            return;

        if (player.getInventory().firstEmpty() == -1) {
            String msg = ChatColor.translateAlternateColorCodes('&', "&cYour inventory is full.");
            player.sendMessage(msg);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                    net.md_5.bungee.api.chat.TextComponent.fromLegacyText(msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        if (!hasKey(player, crateName)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }

        deductKey(player, crateName);
        ItemStack reward = pickReward(crateName);
        pendingRewards.put(player.getUniqueId(), reward);

        // Speed up background animation! (Fast: 2 ticks)
        startBackgroundAnimation(player, gui, 2L);

        // Start Main Carousel Animation (Variable Speed)
        startVariableSpeedAnimation(player, crateName, gui, reward);
    }

    private void startVariableSpeedAnimation(Player player, String crateName, Inventory gui, ItemStack finalReward) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        List<ItemStack> contents = getCrateContents(config);

        // Recursive runnable for variable speed
        Runnable animationStep = new Runnable() {
            int ticksElapsed = 0;
            int currentStep = 0;
            long currentDelay = 2L; // Start Fast

            @Override
            public void run() {
                if (!player.isOnline()) {
                    stopAndClean(player.getUniqueId());
                    return;
                }

                // Shift Main Carousel (10-16)
                for (int i = 16; i > 10; i--) {
                    gui.setItem(i, gui.getItem(i - 1));
                }

                // Inject final reward so it lands on Slot 13 (Center) at the end
                // Total Steps = 50. Last Step = 49.
                // Step 49 ends at Slot 10.
                // Step 48 ends at Slot 11.
                // Step 47 ends at Slot 12.
                // Step 46 ends at Slot 13.
                if (currentStep == 46) {
                    gui.setItem(10, finalReward);
                } else {
                    gui.setItem(10, getRandomItem(contents));
                }
                player.playSound(player.getLocation(), carouselSounds[random.nextInt(carouselSounds.length)], 1f, 2f);

                currentStep++;

                // Done?
                if (currentStep >= 50) { // Total steps ~ 50
                    finish(player, gui, finalReward);
                    return;
                }

                // Adjust Speed
                if (currentStep < 30)
                    currentDelay = 2L; // Fast
                else if (currentStep < 40)
                    currentDelay = 5L; // Slowing
                else if (currentStep < 45)
                    currentDelay = 10L;// Slow
                else
                    currentDelay = 15L; // Very Slow

                // Schedule next step
                BukkitTask nextTask = plugin.getSchedulerAdapter().runTaskLater(this, currentDelay);
                activeTasks.put(player.getUniqueId(), nextTask);
            }
        };

        // Kickoff
        BukkitTask task = plugin.getSchedulerAdapter().runTaskLater(animationStep, 2L);
        activeTasks.put(player.getUniqueId(), task);
    }

    // Kept merely to avoid breaking existing references, though not used by
    // handleStartClick anymore
    private void startAnimation(Player player, String crateName, Inventory gui, ItemStack finalReward) {
        startVariableSpeedAnimation(player, crateName, gui, finalReward);
    }

    private void finish(Player player, Inventory gui, ItemStack reward) {
        stopAndClean(player.getUniqueId());

        // 1. Give Reward Logic (CRITICAL: Do this first)
        if (player.isOnline()) {
            player.getInventory().addItem(reward);
        }
        pendingRewards.remove(player.getUniqueId());

        // Place reward in center for visual
        gui.setItem(13, reward);

        // 2. Celebrate (Aesthetics)
        // 2. Celebrate (Aesthetics)
        try {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 1f, 1f);
            player.playSound(player.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_BLAST, 1f, 1f);

            // Spawn Firework safely on the correct thread (Region Thread)
            // Fixes NPE on Folia when running from Global Task
            plugin.getSchedulerAdapter().runAtLocation(player.getLocation(), () -> {
                try {
                    org.bukkit.entity.Firework fw = (org.bukkit.entity.Firework) player.getWorld().spawnEntity(
                            player.getLocation(),
                            org.bukkit.entity.EntityType.FIREWORK_ROCKET);
                    org.bukkit.inventory.meta.FireworkMeta fwm = fw.getFireworkMeta();
                    fwm.addEffect(org.bukkit.FireworkEffect.builder()
                            .with(org.bukkit.FireworkEffect.Type.BALL_LARGE)
                            .withColor(org.bukkit.Color.GREEN, org.bukkit.Color.LIME, org.bukkit.Color.AQUA)
                            .withFade(org.bukkit.Color.WHITE)
                            .flicker(true)
                            .trail(true)
                            .build());
                    fwm.setPower(1);
                    fw.setFireworkMeta(fwm);

                    // Detonate immediately (approx) using the same thread context or scheduling
                    // carefully
                    // Using runTaskLater on adapter might default to global if not careful, but
                    // SchedulerAdapter
                    // only has runTaskLater (GLOBAL/SYNC).
                    // We should use a region-specific delay if possible, but for now let's just use
                    // runAtLocation again
                    // effectively or just standard adapter delay which might go global.
                    // BUT altering entity (detonate) *usually* requires region thread.
                    // Let's use internal scheduler if available or just skip detonate delay if
                    // risky.
                    // Better: just let it fly for a tick.
                    plugin.getSchedulerAdapter().runAtLocation(player.getLocation(), fw::detonate);
                } catch (Exception ex) {
                    // Ignore errors during spawn
                }
            });
        } catch (Exception e) {
            // Ignore aesthetic errors
        }

        // Slow background down again? Or keep it spinning?
        // User didn't specify, but usually it slows down or stops.
        // Let's keep it running fast or revert to slow?
        // "recycling should start to gain more speed" -> Implies it speeds up.
        // Let's revert to slow after finish implies "calm down".
        startBackgroundAnimation(player, gui, 5L);
    }

    public void handleClose(Player player) {
        UUID uuid = player.getUniqueId();

        // Stop Active Task
        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
        }

        // Stop Background Task
        if (backgroundTasks.containsKey(uuid)) {
            backgroundTasks.get(uuid).cancel();
            backgroundTasks.remove(uuid);
        }

        if (pendingRewards.containsKey(uuid)) {
            // Give pending reward immediately
            ItemStack reward = pendingRewards.remove(uuid);
            if (player.isOnline()) {
                player.getInventory().addItem(reward);
                player.sendMessage(ChatColor.GREEN + "You received your crate reward!");
            }
        }
    }

    private void stopAndClean(UUID uuid) {
        if (activeTasks.containsKey(uuid)) {
            activeTasks.get(uuid).cancel();
            activeTasks.remove(uuid);
        }
        // Background task continues until handleClose is called (e.g. at end of finish
        // when GUI might close?
        // No, finish keeps GUI open usually, but let's leave background running until
        // close).
        // Wait, finish places an item and... does NOT close inventory. So background
        // can keep spinning!
    }

    // Helpers
    private List<ItemStack> getCrateContents(FileConfiguration config) {
        List<ItemStack> items = new ArrayList<>();
        if (config.contains("contents")) {
            ConfigurationSection contents = config.getConfigurationSection("contents");
            for (String key : contents.getKeys(false)) {
                ItemStack item = config.getItemStack("contents." + key);
                if (item != null && !isDecoration(item)) {
                    items.add(item);
                }
            }
        }
        return items;
    }

    private boolean isDecoration(ItemStack item) {
        String type = item.getType().name();
        return type.endsWith("GLASS_PANE");
    }

    private ItemStack getRandomItem(List<ItemStack> items) {
        if (items.isEmpty())
            return new ItemStack(Material.AIR);
        return items.get(random.nextInt(items.size())).clone();
    }

    private ItemStack pickReward(String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        List<ItemStack> contents = getCrateContents(config);
        return getRandomItem(contents);
    }

    private boolean hasKey(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists())
            return false;
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key");

        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        return data != null && data.getKeyCount(keyName) > 0;
    }

    private void deductKey(Player player, String crateName) {
        File crateFile = new File(plugin.getDataFolder(), "crates/crate/" + crateName + "-crate.yml");
        if (!crateFile.exists())
            return;
        FileConfiguration config = YamlConfiguration.loadConfiguration(crateFile);
        String keyName = config.getString("key");

        com.prismcore.survival.manager.PlayerData data = plugin.getPlayerDataManager().get(player.getUniqueId());
        if (data != null)
            data.removeKey(keyName);
    }

    public boolean isSpinning(Player player) {
        return activeTasks.containsKey(player.getUniqueId());
    }
}
