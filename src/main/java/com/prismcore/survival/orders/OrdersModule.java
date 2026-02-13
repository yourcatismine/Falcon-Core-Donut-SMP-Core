package com.prismcore.survival.orders;

import com.h2ph.PrismSurvival;
import com.prismcore.survival.orders.cmd.PrismOrderCommand;
import com.prismcore.survival.orders.cmd.OrdersCommand;
import com.prismcore.survival.orders.gui.MenuListener;
import com.prismcore.survival.orders.input.ChatInputManager;
import com.prismcore.survival.orders.store.ConfigManager;
import com.prismcore.survival.orders.store.EnchantmentsManager;
import com.prismcore.survival.orders.store.FilterManager;
import com.prismcore.survival.orders.store.OrderManager;
import com.prismcore.survival.orders.store.PlayerStateManager;
import com.prismcore.survival.orders.store.VaultHook;
import com.prismcore.survival.orders.store.OfflineNotificationManager;
import com.prismcore.survival.orders.gui.OrdersJoinListener;
import org.bukkit.Bukkit;

public class OrdersModule {

    private final PrismSurvival plugin;
    private static OrdersModule instance;

    private VaultHook vault;
    private ConfigManager configManager;
    private FilterManager filterManager;
    private EnchantmentsManager enchantmentsManager;
    private OrderManager orderManager;
    private PlayerStateManager stateManager;
    private ChatInputManager chatInputManager;
    private OfflineNotificationManager offlineNotifications;

    public OrdersModule(PrismSurvival plugin) {
        this.plugin = plugin;
    }

    public static OrdersModule getInstance() {
        return instance;
    }

    public void enable() {
        instance = this;
        this.configManager = new ConfigManager(plugin);
        this.filterManager = new FilterManager(this);
        this.enchantmentsManager = new EnchantmentsManager(this);

        this.vault = new VaultHook(this);
        if (!this.vault.hooked()) {
            plugin.getLogger().severe("Vault/Economy not found. Orders module disabled.");
            return;
        }

        this.orderManager = new OrderManager(plugin);
        this.orderManager.cleanupExpired();
        this.stateManager = new PlayerStateManager(this);
        this.chatInputManager = new ChatInputManager(this);
        this.offlineNotifications = new OfflineNotificationManager(this);

        Bukkit.getPluginManager().registerEvents(new MenuListener(plugin), plugin);
        Bukkit.getPluginManager().registerEvents(this.chatInputManager, plugin);
        Bukkit.getPluginManager().registerEvents(new OrdersJoinListener(this), plugin);

        plugin.getCommand("orders").setExecutor(new OrdersCommand(plugin));

        PrismOrderCommand adminCmd = new PrismOrderCommand(plugin);
        plugin.getCommand("prismorder").setExecutor(adminCmd);
        plugin.getCommand("prismorder").setTabCompleter(adminCmd);

        plugin.getLogger().info("Orders Module enabled.");
    }

    public void disable() {
        if (this.orderManager != null) {
            this.orderManager.saveAll();
        }
        // if (this.stateManager != null) {
        // this.stateManager.saveAllPrefs();
        // }
        instance = null;
    }

    public PrismSurvival getPlugin() {
        return plugin;
    }

    public VaultHook vault() {
        return this.vault;
    }

    public ConfigManager cfg() {
        return this.configManager;
    }

    public FilterManager filters() {
        return this.filterManager;
    }

    public EnchantmentsManager ench() {
        return this.enchantmentsManager;
    }

    public OrderManager orders() {
        return this.orderManager;
    }

    public PlayerStateManager state() {
        return this.stateManager;
    }

    public ChatInputManager chat() {
        return this.chatInputManager;
    }

    public OfflineNotificationManager getOfflineNotifications() {
        return this.offlineNotifications;
    }
}
