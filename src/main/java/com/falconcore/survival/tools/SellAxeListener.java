package com.falconcore.survival.tools;

import com.h2ph.Falcon;
import com.falconcore.survival.sell.FalconSell;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class SellAxeListener implements Listener {

    private final ToolsManager manager;

    public SellAxeListener(ToolsManager manager) {
        this.manager = manager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK)
            return;
        handleSell(event.getPlayer(), event.getClickedBlock(), event.getItem(), event);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        if (handleSell(event.getPlayer(), event.getBlock(), event.getPlayer().getInventory().getItemInMainHand(),
                null)) {
            event.setCancelled(true);
        }
    }

    private boolean handleSell(Player player, Block block, ItemStack handItem, PlayerInteractEvent interactEvent) {
        if (block == null || handItem == null || handItem.getType() == Material.AIR)
            return false;

        if (!handItem.hasItemMeta())
            return false;
        if (!handItem.getItemMeta().getPersistentDataContainer().has(ToolsManager.SELL_AXE_KEY,
                PersistentDataType.BYTE))
            return false;

        if (block.getType() != Material.CHEST && block.getType() != Material.TRAPPED_CHEST)
            return false;

        if (!(block.getState() instanceof Container))
            return false;

        Container container = (Container) block.getState();


        double totalValue = 0;
        int itemsSold = 0;
        java.util.Map<com.falconcore.survival.sell.category.Category, Double> categoryProgressToAdd = new java.util.HashMap<>();
        com.falconcore.survival.sell.data.PlayerData data = Falcon.getInstance().getFalconSell()
                .getPlayerDataManager().getPlayerData(player.getUniqueId());

        ItemStack[] contents = container.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack is = contents[i];
            if (is == null || is.getType() == Material.AIR)
                continue;

            if (is.getType().name().endsWith("SHULKER_BOX")) {
                if (is.getItemMeta() instanceof org.bukkit.inventory.meta.BlockStateMeta) {
                    org.bukkit.inventory.meta.BlockStateMeta bsm = (org.bukkit.inventory.meta.BlockStateMeta) is
                            .getItemMeta();
                    if (bsm.getBlockState() instanceof org.bukkit.block.ShulkerBox) {
                        org.bukkit.block.ShulkerBox shulker = (org.bukkit.block.ShulkerBox) bsm.getBlockState();
                        ItemStack[] shulkerContents = shulker.getInventory().getContents();
                        boolean shulkerChanged = false;

                        for (int j = 0; j < shulkerContents.length; j++) {
                            ItemStack sItem = shulkerContents[j];
                            if (sItem == null || sItem.getType() == Material.AIR)
                                continue;

                            double price = Falcon.getInstance().getFalconSell().getPricesManager()
                                    .getPrice(sItem);
                            if (price > 0) {
                                com.falconcore.survival.sell.category.Category category = Falcon.getInstance()
                                        .getFalconSell().getPricesManager().getCategory(sItem);
                                double amount = price * sItem.getAmount();

                                if (category != null) {
                                    double multiplier = data.getMultiplier(category);
                                    amount *= multiplier;
                                    categoryProgressToAdd.put(category,
                                            categoryProgressToAdd.getOrDefault(category, 0.0) + amount);
                                }

                                totalValue += amount;
                                itemsSold += sItem.getAmount();
                                shulker.getInventory().setItem(j, null);
                                shulkerChanged = true;
                            }
                        }

                        if (shulkerChanged) {
                            bsm.setBlockState(shulker);
                            is.setItemMeta(bsm);
                            container.getInventory().setItem(i, is);
                        }
                    }
                }
            }

            double price = Falcon.getInstance().getFalconSell().getPricesManager().getPrice(is);
            if (price > 0) {
                com.falconcore.survival.sell.category.Category category = Falcon.getInstance().getFalconSell()
                        .getPricesManager().getCategory(is);
                double amount = price * is.getAmount();

                if (category != null) {
                    double multiplier = data.getMultiplier(category);
                    amount *= multiplier;
                    categoryProgressToAdd.put(category, categoryProgressToAdd.getOrDefault(category, 0.0) + amount);
                }

                totalValue += amount;
                itemsSold += is.getAmount();
                container.getInventory().setItem(i, null);
            }
        }

        if (totalValue > 0) {
            Falcon.getInstance().getFalconSell().getEconomy().deposit(player.getUniqueId(), totalValue);

            data.setSellMade(data.getSellMade() + totalValue);
            for (java.util.Map.Entry<com.falconcore.survival.sell.category.Category, Double> entry : categoryProgressToAdd
                    .entrySet()) {
                data.addProgress(entry.getKey(), entry.getValue());
                checkLevelUp(player, entry.getKey(), data);
            }
            Falcon.getInstance().getFalconSell().getPlayerDataManager().savePlayerDataAsync(player.getUniqueId());

            org.bukkit.configuration.file.FileConfiguration sellConfig = Falcon.getInstance().getFalconSell()
                    .getConfig();
            String totalFormatted = Utils.formatNumber(totalValue);

            String msgChat = sellConfig.getString("messages.sold-total", "&a+$%amount%");
            if (msgChat != null && !msgChat.isEmpty()) {
                player.sendMessage(Utils.formatColors(msgChat.replace("%amount%", totalFormatted)));
            }

            String msgActionBar = sellConfig.getString("messages.sold-total-action-bar", "&a+$%amount%");
            if (msgActionBar != null && !msgActionBar.isEmpty()) {
                player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(
                                Utils.formatColors(msgActionBar.replace("%amount%", totalFormatted))));
            }

            String soundName = manager.getConfig().getString("sellaxe.sound", "BLOCK_AMETHYST_BLOCK_HIT");
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1f, 2.0f);
            } catch (IllegalArgumentException e) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2.0f);
            }

            if (interactEvent != null) {
                interactEvent.setCancelled(true);
            }
            return true;
        } else {
            org.bukkit.configuration.file.FileConfiguration sellConfig = Falcon.getInstance().getFalconSell()
                    .getConfig();
            String msg = sellConfig.getString("messages.nothing-to-sell", "&cYou have nothing to sell");
            if (msg != null && !msg.isEmpty()) {
                player.sendMessage(Utils.formatColors(msg));
            }

            if (interactEvent != null) {
                interactEvent.setCancelled(true);
            }
            return true;
        }
    }

    private void checkLevelUp(Player player, com.falconcore.survival.sell.category.Category category,
            com.falconcore.survival.sell.data.PlayerData data) {
        com.falconcore.survival.sell.FalconSell sellPlugin = Falcon.getInstance().getFalconSell();
        java.util.List<Double> levelPrices = sellPlugin.getConfig().getDoubleList("level-prices");
        double multiplierIncrement = sellPlugin.getConfig().getDouble("settings.multiplier-increment", 0.1);

        while (true) {
            double currentMultiplier = data.getMultiplier(category);
            int currentLevel = this.getCurrentLevel(currentMultiplier);

            if (currentLevel >= levelPrices.size()) {
                return;
            }

            double required = levelPrices.get(currentLevel);
            double progress = data.getProgress(category);

            if (progress >= required) {
                double newMultiplier = currentMultiplier + multiplierIncrement;
                data.setMultiplier(category, newMultiplier);
            } else {
                break;
            }
        }
    }

    private int getCurrentLevel(double multiplier) {
        com.falconcore.survival.sell.FalconSell sellPlugin = Falcon.getInstance().getFalconSell();
        double baseMultiplier = sellPlugin.getConfig().getDouble("settings.base-multiplier", 1.0);
        double multiplierIncrement = sellPlugin.getConfig().getDouble("settings.multiplier-increment", 0.1);
        int level = (int) Math.round((multiplier - baseMultiplier) / multiplierIncrement);
        return Math.max(0, level);
    }
}
