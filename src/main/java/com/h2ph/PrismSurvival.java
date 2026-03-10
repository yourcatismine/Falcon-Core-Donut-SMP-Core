package com.h2ph;

import com.prismcore.survival.manager.PlayerDataManager;
import com.prismcore.survival.manager.DatabaseManager;
import com.prismcore.survival.scheduler.SchedulerAdapter;
import com.prismcore.survival.utils.PlayerNameCache;
import com.h2ph.commands.economy.ShopCommand;
import com.h2ph.commands.player.QuickGameMode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class PrismSurvival extends JavaPlugin {

    private PlayerDataManager playerDataManager;
    private DatabaseManager databaseManager;
    private SchedulerAdapter schedulerAdapter;
    private PlayerNameCache playerNameCache;
    private ShopCommand shopCommand;
    private com.h2ph.commands.player.RulesCommand rulesCommand;
    private com.h2ph.commands.player.MediaCommand mediaCommand;

    private static PrismSurvival instance;

    public static PrismSurvival getInstance() {
        return instance;
    }

    private com.h2ph.afk.AFKManager afkManager;
    private org.bukkit.configuration.file.FileConfiguration survivalConfig;
    private com.h2ph.commands.admin.moderations.OffendPlugin offendPlugin;
    private com.h2ph.api.ApiServer apiServer;
    private com.prismcore.survival.manager.KeyAllManager keyAllManager;
    private com.prismcore.survival.manager.CarouselManager carouselManager;
    private com.prismcore.survival.manager.CrateLocationRegistry crateLocationRegistry;
    private com.prismcore.survival.manager.CrateEffectsManager crateEffectsManager;
    private com.prismcore.survival.manager.SpawnManager spawnManager;
    private com.prismcore.survival.manager.TeleportManager teleportManager;
    private com.prismcore.survival.shards.ShardsManager shardsManager;
    private com.prismcore.survival.auction.AuctionController auctionController;

    private com.h2ph.rtp.RTPQueueManager rtpQueueManager;

    private com.h2ph.commands.admin.duels.DuelStatsManager duelStatsManager;
    private com.h2ph.commands.admin.duels.DuelArenaManager duelArenaManager;

    private com.h2ph.maintenance.MaintenanceManager maintenanceManager;
    private com.prismcore.survival.orders.OrdersModule ordersModule;
    private com.prismcore.survival.sell.PrismSell prismSell;
    private com.h2ph.managers.RedstoneManager redstoneManager;
    private com.h2ph.managers.PrivateMessageManager privateMessageManager;
    private com.prismcore.survival.manager.BountyManager bountyManager;
    private com.h2ph.utils.SignInput signInput;
    private com.h2ph.managers.EnderChestManager enderChestManager;
    private com.h2ph.managers.HomeManager homeManager;
    private com.prismcore.survival.manager.WarpManager warpManager;
    private com.h2ph.managers.ScoreboardManager scoreboardManager;
    private com.h2ph.managers.TabListManager tabListManager;
    private com.prismcore.survival.manager.VoidManager voidManager;
    private com.prismcore.survival.manager.PvPSafeZoneManager pvpSafeZoneManager;
    private com.prismcore.survival.manager.BlockRestorationManager blockRestorationManager;
    private com.h2ph.managers.VanishManager vanishManager;
    private com.h2ph.managers.DeathMessageManager deathMessageManager;
    private com.h2ph.managers.RespawnGearManager respawnGearManager;
    private com.h2ph.gui.RespawnGearGUI respawnGearGUI;
    private com.h2ph.teams.TeamManager teamManager;
    private com.h2ph.teams.TeamInviteManager teamInviteManager;
    private com.h2ph.managers.GamertagManager gamertagManager;
    private com.h2ph.managers.DamageManager damageManager;

    private com.prismcore.survival.limiter.LimiterConfig limiterConfig;
    private com.prismcore.survival.limiter.LimiterManager limiterManager;
    private com.prismcore.survival.survival.ChatFilter chatFilter;
    private com.h2ph.listeners.CommandHideListener commandHideListener;
    private com.prismcore.survival.manager.DiscordWebhookManager discordWebhookManager;

    @Override
    public void onLoad() {

    }

    @Override
    public void onEnable() {
        instance = this;
        saveAllResources();

        getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

        loadSurvivalConfig();

        this.discordWebhookManager = new com.prismcore.survival.manager.DiscordWebhookManager(this);

        this.commandHideListener = new com.h2ph.listeners.CommandHideListener(this);
        getServer().getPluginManager().registerEvents(commandHideListener, this);

        new com.prismcore.survival.survival.MessageHider(this);

        this.chatFilter = new com.prismcore.survival.survival.ChatFilter(this);
        getServer().getPluginManager().registerEvents(chatFilter, this);

        getServer().getPluginManager().registerEvents(new com.prismcore.survival.survival.ChatFormatter(this), this);

        loadUpdateFromConfig();

        getCommand("update").setExecutor(new com.h2ph.commands.admin.updates.UpdateCommand(this));
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.UpdateBookListener(this), this);

        loadAdvisorFromConfig();

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.RTPListener(), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveCommandListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.AutoRTPListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        this.playerDataManager = new PlayerDataManager(this);
        this.databaseManager = new DatabaseManager(this, getSurvivalConfig());
        this.schedulerAdapter = new SchedulerAdapter(this);
        this.playerNameCache = new PlayerNameCache(this.schedulerAdapter);
        this.keyAllManager = new com.prismcore.survival.manager.KeyAllManager(this);
        this.carouselManager = new com.prismcore.survival.manager.CarouselManager(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateLocationRegistry = new com.prismcore.survival.manager.CrateLocationRegistry(this);
        this.crateEffectsManager = new com.prismcore.survival.manager.CrateEffectsManager(this);
        this.spawnManager = new com.prismcore.survival.manager.SpawnManager(this);
        this.teleportManager = new com.prismcore.survival.manager.TeleportManager(this);
        this.privateMessageManager = new com.h2ph.managers.PrivateMessageManager();
        this.bountyManager = new com.prismcore.survival.manager.BountyManager(this);

        this.deathMessageManager = new com.h2ph.managers.DeathMessageManager(this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.LiveSignListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CrateListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.DeathMessageListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.PlayerConnectionListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.InventorySyncListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.ChunkTrackingListener(this), this);

        getSchedulerAdapter().runTaskTimer(() -> {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                final org.bukkit.inventory.ItemStack[] contents = player.getInventory().getContents().clone();
                final org.bukkit.inventory.ItemStack[] armor = player.getInventory().getArmorContents().clone();
                final java.util.UUID uuid = player.getUniqueId();
                final String name = player.getName();

                getSchedulerAdapter().runTaskAsynchronously(() -> {
                    try {
                        String invBase64 = com.prismcore.survival.utils.ItemSerializationManager
                                .itemStackArrayToBase64(contents);
                        String armorBase64 = com.prismcore.survival.utils.ItemSerializationManager
                                .itemStackArrayToBase64(armor);
                        getDatabaseManager().saveInventory(uuid, invBase64, armorBase64);
                    } catch (Exception e) {
                        getLogger().log(java.util.logging.Level.SEVERE,
                                "Failed to serialize inventory for auto-save of " + name, e);
                    }
                });
            }
        }, 12000L, 12000L);

        getServer().getPluginManager()
                .registerEvents(new com.prismcore.survival.listeners.PlayerNameCacheListener(this), this);

        this.redstoneManager = new com.h2ph.managers.RedstoneManager(this);

        this.shopCommand = new ShopCommand(this);
        getCommand("shop").setExecutor(shopCommand);
        getServer().getPluginManager().registerEvents(shopCommand, this);

        getCommand("redstone").setExecutor(new com.h2ph.commands.admin.RedstoneCommand(this));

        this.offendPlugin = new com.h2ph.commands.admin.moderations.OffendPlugin(this);

        new com.prismcore.survival.tools.ToolsManager(this);

        this.signInput = new com.h2ph.utils.SignInput(this);
        getServer().getPluginManager().registerEvents(this.signInput, this);
        this.signInput = new com.h2ph.utils.SignInput(this);
        getServer().getPluginManager().registerEvents(this.signInput, this);

        com.h2ph.commands.admin.economy.ShardsCommand shardsCommand = new com.h2ph.commands.admin.economy.ShardsCommand(
                this);
        getCommand("shards").setExecutor(shardsCommand);
        getCommand("shards").setTabCompleter(shardsCommand);

        com.h2ph.commands.admin.crates.KeyCommand keyCommand = new com.h2ph.commands.admin.crates.KeyCommand(this);
        getCommand("key").setExecutor(keyCommand);
        getCommand("key").setTabCompleter(keyCommand);

        com.h2ph.commands.admin.economy.BillfordCommand billfordCommand = new com.h2ph.commands.admin.economy.BillfordCommand(
                this);
        getCommand("billford").setExecutor(billfordCommand);
        getCommand("billford").setTabCompleter(billfordCommand);
        getServer().getPluginManager().registerEvents(billfordCommand, this);

        com.h2ph.commands.admin.economy.BaltopCommand baltopCommand = new com.h2ph.commands.admin.economy.BaltopCommand(
                this);
        getCommand("baltop").setExecutor(baltopCommand);
        getServer().getPluginManager().registerEvents(baltopCommand, this);

        com.h2ph.commands.economy.BalanceCommand balanceCommand = new com.h2ph.commands.economy.BalanceCommand(this);
        getCommand("balance").setExecutor(balanceCommand);
        getCommand("balance").setTabCompleter(balanceCommand);

        com.h2ph.commands.economy.PayCommand payCommand = new com.h2ph.commands.economy.PayCommand(this);
        getCommand("pay").setExecutor(payCommand);
        getCommand("pay").setTabCompleter(payCommand);

        com.h2ph.commands.economy.SellHistoryCommand sellHistoryCommand = new com.h2ph.commands.economy.SellHistoryCommand(
                this);
        getCommand("sellhistory").setExecutor(sellHistoryCommand);

        getCommand("worth").setExecutor(new com.h2ph.commands.economy.WorthCommand(this));

        getCommand("anvil").setExecutor(new com.h2ph.commands.player.AnvilCommand());

        getCommand("craftingtable").setExecutor(new com.h2ph.commands.player.CraftingTableCommand());

        getCommand("smithingtable").setExecutor(new com.h2ph.commands.player.SmithingTableCommand());

        getCommand("minigames").setExecutor(new com.h2ph.commands.player.MinigamesCommand(this));

        java.io.File ecoConfig = new java.io.File(getDataFolder(), "economy/config.yml");
        org.bukkit.configuration.file.FileConfiguration ecoConfigYaml = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(ecoConfig);
        boolean vaultEnabled = ecoConfigYaml.getBoolean("vault-enabled", true);

        if (vaultEnabled) {
            com.h2ph.commands.admin.economy.EconomyCommand economyCommand = new com.h2ph.commands.admin.economy.EconomyCommand(
                    this);
            getCommand("economy").setExecutor(economyCommand);
            getCommand("economy").setTabCompleter(economyCommand);
        } else {
            getLogger().info("Economy Command disabled in config (vault-enabled: false).");
            getCommand("economy").setExecutor((sender, command, label, args) -> {
                sender.sendMessage(org.bukkit.ChatColor.RED + "PrismEconomy is currently disabled.");
                return true;
            });
            getCommand("economy").setTabCompleter((sender, command, label, args) -> java.util.Collections.emptyList());
        }

        this.afkManager = new com.h2ph.afk.AFKManager(this);
        this.afkManager.startTask();

        this.shardsManager = new com.prismcore.survival.shards.ShardsManager(this);

        if (getServer().getPluginManager().getPlugin("WorldEdit") != null ||
                getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            getCommand("setafk").setExecutor(new com.h2ph.commands.admin.afk.SetAFKCommand(this));

            com.h2ph.commands.player.afk.AFKCommand afkCommand = new com.h2ph.commands.player.afk.AFKCommand(this);
            getCommand("afk").setExecutor(afkCommand);
            getCommand("afk").setTabCompleter(afkCommand);
            getServer().getPluginManager().registerEvents(afkCommand, this);

            getLogger().info("WorldEdit found! AFK region features enabled.");
        } else {
            getLogger().warning("WorldEdit/FAWE not found! /setafk and /afk commands disabled.");
        }

        this.auctionController = new com.prismcore.survival.auction.AuctionController(this);
        this.auctionController.enable();

        com.h2ph.commands.admin.moderations.SusCommand susCommand = new com.h2ph.commands.admin.moderations.SusCommand(
                this);
        getCommand("sus").setExecutor(susCommand);
        getServer().getPluginManager().registerEvents(susCommand, this);

        com.h2ph.commands.admin.moderations.SpectatorMode spectatorMode = new com.h2ph.commands.admin.moderations.SpectatorMode(
                this);
        getCommand("gmsp").setExecutor(spectatorMode);
        getServer().getPluginManager().registerEvents(spectatorMode, this);

        getCommand("mute").setExecutor(new com.h2ph.commands.admin.moderations.MuteCommand(this));
        getCommand("mute").setTabCompleter(new com.h2ph.commands.admin.moderations.MuteCommand(this));

        getCommand("unmute").setExecutor(new com.h2ph.commands.admin.moderations.UnmuteCommand(this));
        getCommand("unmute").setTabCompleter(new com.h2ph.commands.admin.moderations.UnmuteCommand(this));
        getCommand("checkmute").setExecutor(new com.h2ph.commands.admin.moderations.CheckMuteCommand(this));
        getCommand("checkmute").setTabCompleter(new com.h2ph.commands.admin.moderations.CheckMuteCommand(this));


        this.duelStatsManager = new com.h2ph.commands.admin.duels.DuelStatsManager(this);
        this.duelArenaManager = new com.h2ph.commands.admin.duels.DuelArenaManager(this, duelStatsManager);

        if (getServer().getPluginManager().getPlugin("WorldEdit") != null ||
                getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            com.h2ph.commands.admin.duels.DuelCommand duelCommand = new com.h2ph.commands.admin.duels.DuelCommand(this,
                    duelArenaManager);
            getCommand("duel").setExecutor(duelCommand);
            getCommand("duel").setTabCompleter(duelCommand);
            getServer().getPluginManager().registerEvents(new com.h2ph.listeners.DuelGUIListener(), this);
            getServer().getPluginManager().registerEvents(new com.h2ph.listeners.DuelGameListener(duelArenaManager),
                    this);
        } else {
            getLogger().warning("WorldEdit/FAWE not found! /duel command disabled.");
        }

        com.h2ph.commands.player.RTPCommand rtpCmd = new com.h2ph.commands.player.RTPCommand();
        getCommand("rtp").setExecutor(rtpCmd);
        getCommand("rtp").setTabCompleter(rtpCmd);

        this.rtpQueueManager = new com.h2ph.rtp.RTPQueueManager(this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.RTPQueueListener(this), this);

        this.respawnGearManager = new com.h2ph.managers.RespawnGearManager(this);
        this.respawnGearGUI = new com.h2ph.gui.RespawnGearGUI(this);

        this.rulesCommand = new com.h2ph.commands.player.RulesCommand(this);
        getCommand("rules").setExecutor(rulesCommand);
        getServer().getPluginManager().registerEvents(rulesCommand, this);

        this.mediaCommand = new com.h2ph.commands.player.MediaCommand(this);
        getCommand("media").setExecutor(mediaCommand);
        getServer().getPluginManager().registerEvents(mediaCommand, this);

        com.h2ph.commands.player.AdvisorCommand advisorCmd = new com.h2ph.commands.player.AdvisorCommand(this);
        getCommand("advisor").setExecutor(advisorCmd);
        getCommand("advisor").setTabCompleter(advisorCmd);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.AdvisorListener(this), this);

        com.h2ph.commands.admin.moderations.SetSpawnCommand setSpawnCmd = new com.h2ph.commands.admin.moderations.SetSpawnCommand(
                this);
        getCommand("setspawn").setExecutor(setSpawnCmd);
        getCommand("setspawn").setTabCompleter(setSpawnCmd);

        com.h2ph.commands.player.SpawnCommand spawnCmd = new com.h2ph.commands.player.SpawnCommand(this);
        getCommand("spawn").setExecutor(spawnCmd);
        getCommand("spawn").setTabCompleter(spawnCmd);
        getServer().getPluginManager().registerEvents(spawnCmd, this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TpaGUIListener(), this);

        getCommand("tpauto").setExecutor(new com.h2ph.commands.player.TpAutoCommand());
        com.h2ph.managers.TpAutoManager tpAutoManager = new com.h2ph.managers.TpAutoManager(this);
        getServer().getPluginManager().registerEvents(tpAutoManager, this);

        com.h2ph.commands.player.TpaCommand tpaCmd = new com.h2ph.commands.player.TpaCommand();
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpa").setTabCompleter(tpaCmd);

        com.h2ph.commands.player.TpaHereCommand tpaHereCmd = new com.h2ph.commands.player.TpaHereCommand();
        getCommand("tpahere").setExecutor(tpaHereCmd);
        getCommand("tpahere").setTabCompleter(tpaHereCmd);

        com.h2ph.commands.admin.TpCommand tpCmd = new com.h2ph.commands.admin.TpCommand();
        getCommand("tp").setExecutor(tpCmd);
        getCommand("tp").setTabCompleter(tpCmd);

        com.h2ph.commands.player.OtpCommand otpCmd = new com.h2ph.commands.player.OtpCommand(this);
        getCommand("otp").setExecutor(otpCmd);
        getCommand("otp").setTabCompleter(otpCmd);

        com.h2ph.commands.admin.moderations.CheckAltCommand checkAltCmd = new com.h2ph.commands.admin.moderations.CheckAltCommand(
                this);
        getCommand("checkalt").setExecutor(checkAltCmd);
        getCommand("checkalt").setTabCompleter(checkAltCmd);

        com.h2ph.commands.admin.moderations.CheckPlayersCommand checkPlayersCmd = new com.h2ph.commands.admin.moderations.CheckPlayersCommand(
                this);
        getCommand("checkplayers").setExecutor(checkPlayersCmd);
        getCommand("checkplayers").setTabCompleter(checkPlayersCmd);

        com.h2ph.commands.player.CheckTotemCommand checkTotemCmd = new com.h2ph.commands.player.CheckTotemCommand();
        getCommand("checktotem").setExecutor(checkTotemCmd);
        getCommand("checktotem").setTabCompleter(checkTotemCmd);

        com.h2ph.commands.player.TpAcceptCommand tpAcceptCmd = new com.h2ph.commands.player.TpAcceptCommand();
        getCommand("tpaccept").setExecutor(tpAcceptCmd);
        getCommand("tpaccept").setTabCompleter(tpAcceptCmd);

        com.h2ph.commands.player.TpaCancelCommand tpaCancelCmd = new com.h2ph.commands.player.TpaCancelCommand();
        getCommand("tpacancel").setExecutor(tpaCancelCmd);
        getCommand("tpacancel").setTabCompleter(tpaCancelCmd);

        com.h2ph.commands.player.TpaDenyCommand tpaDenyCmd = new com.h2ph.commands.player.TpaDenyCommand();
        getCommand("tpadeny").setExecutor(tpaDenyCmd);
        getCommand("tpadeny").setTabCompleter(tpaDenyCmd);

        com.h2ph.commands.player.MsgCommand msgCmd = new com.h2ph.commands.player.MsgCommand();
        getCommand("msg").setExecutor(msgCmd);
        getCommand("msg").setTabCompleter(msgCmd);

        com.h2ph.commands.player.ReplyCommand replyCmd = new com.h2ph.commands.player.ReplyCommand();
        getCommand("reply").setExecutor(replyCmd);
        getCommand("reply").setTabCompleter(replyCmd);

        com.h2ph.commands.player.SettingsCommand settingsCmd = new com.h2ph.commands.player.SettingsCommand(this);
        getCommand("settings").setExecutor(settingsCmd);

        com.h2ph.commands.player.BountyCommand bountyCommand = new com.h2ph.commands.player.BountyCommand(this);
        getCommand("bounty").setExecutor(bountyCommand);
        getCommand("bounty").setTabCompleter(bountyCommand);
        getCommand("bounties").setExecutor(bountyCommand);
        getCommand("bounties").setTabCompleter(bountyCommand);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyConfirmGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.BountyListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.WorthGUIListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.SettingsGUIListener(), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.FastCrystalListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.FastAnchorListener(this), this);

        this.damageManager = new com.h2ph.managers.DamageManager(this, databaseManager);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.CrystalAnchorDamageListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.MobSpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TpaConfirmGUIListener(), this);

        QuickGameMode quickGameMode = new QuickGameMode();
        getCommand("gmc").setExecutor(quickGameMode);
        getCommand("gms").setExecutor(quickGameMode);
        getCommand("gma").setExecutor(quickGameMode);

        com.h2ph.commands.player.FlyCommand flyCommand = new com.h2ph.commands.player.FlyCommand(this);
        getCommand("fly").setExecutor(flyCommand);
        getServer().getPluginManager().registerEvents(flyCommand, this);

        getCommand("nv").setExecutor(new com.h2ph.commands.player.NightVisionCommand(this));

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.NightVisionListener(this), this);

        getCommand("discord").setExecutor(new com.h2ph.commands.player.DiscordCommand(this));

        getCommand("store").setExecutor(new com.h2ph.commands.player.StoreCommand(this));

        this.vanishManager = new com.h2ph.managers.VanishManager(this);

        com.h2ph.commands.admin.moderations.VanishCommand vanishCmd = new com.h2ph.commands.admin.moderations.VanishCommand(
                this);
        getCommand("vanish").setExecutor(vanishCmd);
        getCommand("vanish").setTabCompleter(vanishCmd);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.VanishListener(this), this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new com.h2ph.placeholders.PrismPlaceholders(this).register();
            new com.h2ph.placeholders.RTPPlaceholders(this).register();
            getLogger().info("PlaceholderAPI expansion registered!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            if (vaultEnabled) {
                com.h2ph.economy.PrismEconomy economy = new com.h2ph.economy.PrismEconomy(this);
                getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, economy, this,
                        org.bukkit.plugin.ServicePriority.Highest);
                getLogger().info("Registered PrismEconomy as Vault provider!");
            } else {
                getLogger().info("PrismEconomy Vault hook disabled in config.");
            }
        } else {
            getLogger().warning("Vault not found! Economy features will be disabled.");
        }

        this.prismSell = new com.prismcore.survival.sell.PrismSell(this);
        this.prismSell.onEnable();

        this.warpManager = new com.prismcore.survival.manager.WarpManager(this);

        this.enderChestManager = new com.h2ph.managers.EnderChestManager(this);

        getCommand("echest").setExecutor(new com.h2ph.commands.player.EnderChestCommand(this));

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.EnderChestGUIListener(this), this);

        this.homeManager = new com.h2ph.managers.HomeManager(this);

        com.h2ph.commands.player.HomeCommand homeCmd = new com.h2ph.commands.player.HomeCommand(this);
        getCommand("home").setExecutor(homeCmd);
        getCommand("home").setTabCompleter(homeCmd);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.HomeGUIListener(this), this);

        com.h2ph.commands.player.WhereAmICommand whereAmICmd = new com.h2ph.commands.player.WhereAmICommand(this);
        getCommand("whereami").setExecutor(whereAmICmd);
        getCommand("whereami").setTabCompleter(whereAmICmd);

        com.h2ph.commands.player.WarpCommand warpCmd = new com.h2ph.commands.player.WarpCommand(this);
        getCommand("warp").setExecutor(warpCmd);
        getCommand("warp").setTabCompleter(warpCmd);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.HomeChatListener(this), this);

        this.scoreboardManager = new com.h2ph.managers.ScoreboardManager(this);
        this.scoreboardManager.setup();

        this.tabListManager = new com.h2ph.managers.TabListManager(this);
        this.tabListManager.setup();

        com.h2ph.commands.admin.TabCommand tabCommand = new com.h2ph.commands.admin.TabCommand(this);
        getCommand("tab").setExecutor(tabCommand);
        getCommand("tab").setTabCompleter(tabCommand);

        this.gamertagManager = new com.h2ph.managers.GamertagManager(this);

        this.teamManager = new com.h2ph.teams.TeamManager(this);
        this.teamInviteManager = new com.h2ph.teams.TeamInviteManager(this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TeamPvPListener(this), this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.TeamChatListener(this), this);

        com.h2ph.commands.player.TeamCommand teamCommand = new com.h2ph.commands.player.TeamCommand(this);
        getCommand("team").setExecutor(teamCommand);
        getCommand("team").setTabCompleter(teamCommand);

        this.maintenanceManager = new com.h2ph.maintenance.MaintenanceManager(this);

        getCommand("maintenance").setExecutor(new com.h2ph.maintenance.MaintenanceCommand(this.maintenanceManager));

        getServer().getPluginManager()
                .registerEvents(new com.h2ph.maintenance.MaintenanceListener(this.maintenanceManager), this);

        this.ordersModule = new com.prismcore.survival.orders.OrdersModule(this);
        this.ordersModule.enable();

        this.apiServer = new com.h2ph.api.ApiServer(this);
        this.apiServer.start();

        startTeamChatTask();

        getCommand("prismreload").setExecutor(new com.h2ph.commands.admin.ReloadCommand(this));

        this.voidManager = new com.prismcore.survival.manager.VoidManager(this);
        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.VoidProtectionListener(this), this);

        this.pvpSafeZoneManager = new com.prismcore.survival.manager.PvPSafeZoneManager(this, databaseManager);
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.listeners.PvPSafeZoneListener(this),
                this);

        this.blockRestorationManager = new com.prismcore.survival.manager.BlockRestorationManager(this, databaseManager,
                pvpSafeZoneManager);
        getServer().getPluginManager().registerEvents(blockRestorationManager, this);

        this.limiterConfig = new com.prismcore.survival.limiter.LimiterConfig(this);
        this.limiterManager = new com.prismcore.survival.limiter.LimiterManager(this, this.limiterConfig);
        this.limiterManager.start();
        getServer().getPluginManager().registerEvents(new com.prismcore.survival.limiter.LimiterListener(this), this);

        com.h2ph.commands.admin.FalconCommand falconCommand = new com.h2ph.commands.admin.FalconCommand(this);
        getCommand("falcon").setExecutor(falconCommand);
        getCommand("falcon").setTabCompleter(falconCommand);

        getCommand("stats").setExecutor(new com.h2ph.commands.player.StatsCommand(this));

        getCommand("hide").setExecutor(new com.h2ph.commands.player.HideNameCommand(this));

        com.h2ph.commands.player.DisguiseCommand disguiseCommand = new com.h2ph.commands.player.DisguiseCommand(this);
        getCommand("disguise").setExecutor(disguiseCommand);
        getCommand("disguise").setTabCompleter(disguiseCommand);

        getCommand("ignore").setExecutor(new com.h2ph.commands.player.IgnoreCommand(this));
        getCommand("unignore").setExecutor(new com.h2ph.commands.player.UnignoreCommand(this));
        com.h2ph.commands.admin.moderations.InvSeeCommand invSeeCmd = new com.h2ph.commands.admin.moderations.InvSeeCommand(
                this);
        getCommand("invsee").setExecutor(invSeeCmd);
        getCommand("invsee").setTabCompleter(invSeeCmd);

        com.h2ph.commands.admin.moderations.EnderSeeCommand enderSeeCmd = new com.h2ph.commands.admin.moderations.EnderSeeCommand(
                this);
        getCommand("endersee").setExecutor(enderSeeCmd);
        getCommand("endersee").setTabCompleter(enderSeeCmd);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.InvSeeListener(this), this);

        getServer().getPluginManager().registerEvents(new com.h2ph.listeners.OperatorListener(this), this);

        new com.h2ph.economy.EconomyMonitor(this);

        if (vaultEnabled && getServer().getPluginManager().getPlugin("Vault") != null) {
            getSchedulerAdapter().runTaskLater(() -> {
                org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                        .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
                if (rsp != null) {
                    net.milkbowl.vault.economy.Economy existing = rsp.getProvider();
                    if (!(existing instanceof com.h2ph.economy.EconomyWrapper)) {
                        com.h2ph.economy.EconomyWrapper wrapper = new com.h2ph.economy.EconomyWrapper(existing,
                                com.h2ph.economy.EconomyMonitor.getInstance());

                        getServer().getServicesManager().register(net.milkbowl.vault.economy.Economy.class, wrapper,
                                this,
                                org.bukkit.plugin.ServicePriority.Highest);
                    }
                }
            }, 40);
        }

        printStartupBanner(vaultEnabled);

        this.playerNameCache.initialize();

        getServer().getPluginManager()
                .registerEvents(new com.prismcore.survival.listeners.PlayerNameCacheListener(this), this);

        try {
            org.apache.logging.log4j.core.Logger rootLogger = (org.apache.logging.log4j.core.Logger) org.apache.logging.log4j.LogManager
                    .getRootLogger();
            rootLogger.addFilter(new com.h2ph.logger.Log4jFilter());
        } catch (Exception e) {
            getLogger().warning("Failed to register Log4j Filter: " + e.getMessage());
        }
    }

    @Override
    public void onDisable() {
        if (this.playerDataManager != null) {
            for (org.bukkit.entity.Player player : getServer().getOnlinePlayers()) {
                this.playerDataManager.savePlayer(player.getUniqueId());
            }
        }

        if (this.enderChestManager != null) {
            for (java.util.UUID uuid : this.enderChestManager.getActiveInventories().keySet()) {
                org.bukkit.inventory.Inventory inv = this.enderChestManager.getActiveInventories().get(uuid);
                if (inv != null) {
                    this.enderChestManager.saveEnderChest(uuid, inv.getContents());
                }
            }
        }

        if (this.prismSell != null) {
            this.prismSell.onDisable();
        }

        if (this.apiServer != null) {
            this.apiServer.stop();
        }

        if (this.ordersModule != null) {
            this.ordersModule.disable();
        }

        if (this.rtpQueueManager != null) {
            this.rtpQueueManager.disable();
        }

        if (this.auctionController != null) {
            this.auctionController.disable();
        }

        if (this.limiterManager != null) {
            this.limiterManager.shutdown();
        }

        if (this.signInput != null) {
            this.signInput.cleanup();
        }

        if (this.scoreboardManager != null) {
            this.scoreboardManager.shutdown();
        }

        if (this.tabListManager != null) {
            this.tabListManager.shutdown();
        }

        if (this.bountyManager != null) {
            this.bountyManager.save();
        }

        if (this.databaseManager != null) {
            this.databaseManager.shutdown();
        }

        if (this.schedulerAdapter != null) {
            this.schedulerAdapter.shutdown();
        }

        getLogger().info("Falcon has been disabled!");
        instance = null;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public SchedulerAdapter getSchedulerAdapter() {
        return schedulerAdapter;
    }

    public PlayerNameCache getPlayerNameCache() {
        return playerNameCache;
    }

    public com.h2ph.afk.AFKManager getAfkManager() {
        return afkManager;
    }

    public com.prismcore.survival.shards.ShardsManager getShardsManager() {
        return shardsManager;
    }

    public com.prismcore.survival.auction.AuctionController getAuctionController() {
        return auctionController;
    }

    public com.h2ph.rtp.RTPQueueManager getRTPQueueManager() {
        return rtpQueueManager;
    }

    public com.h2ph.maintenance.MaintenanceManager getMaintenanceManager() {
        return maintenanceManager;
    }

    public com.prismcore.survival.sell.PrismSell getPrismSell() {
        return prismSell;
    }

    public com.h2ph.commands.admin.duels.DuelArenaManager getDuelArenaManager() {
        return duelArenaManager;
    }

    public com.prismcore.survival.orders.OrdersModule getOrdersModule() {
        return ordersModule;
    }

    public com.h2ph.managers.RedstoneManager getRedstoneManager() {
        return redstoneManager;
    }

    public com.h2ph.managers.PrivateMessageManager getPrivateMessageManager() {
        return privateMessageManager;
    }

    public com.prismcore.survival.manager.BountyManager getBountyManager() {
        return bountyManager;
    }

    public com.h2ph.utils.SignInput getSignInput() {
        return signInput;
    }

    public com.h2ph.managers.EnderChestManager getEnderChestManager() {
        return enderChestManager;
    }

    public com.h2ph.managers.HomeManager getHomeManager() {
        return homeManager;
    }

    public com.prismcore.survival.manager.WarpManager getWarpManager() {
        return warpManager;
    }

    public com.h2ph.managers.ScoreboardManager getScoreboardManager() {
        return scoreboardManager;
    }

    public com.h2ph.managers.TabListManager getTabListManager() {
        return tabListManager;
    }

    public com.prismcore.survival.manager.VoidManager getVoidManager() {
        return voidManager;
    }

    public com.prismcore.survival.manager.PvPSafeZoneManager getPvPSafeZoneManager() {
        return pvpSafeZoneManager;
    }

    public com.prismcore.survival.manager.BlockRestorationManager getBlockRestorationManager() {
        return blockRestorationManager;
    }

    public com.h2ph.managers.VanishManager getVanishManager() {
        return vanishManager;
    }

    public com.h2ph.managers.DeathMessageManager getDeathMessageManager() {
        return deathMessageManager;
    }

    public com.h2ph.managers.RespawnGearManager getRespawnGearManager() {
        return respawnGearManager;
    }

    public com.h2ph.gui.RespawnGearGUI getRespawnGearGUI() {
        return respawnGearGUI;
    }

    public com.h2ph.teams.TeamManager getTeamManager() {
        return teamManager;
    }

    public com.h2ph.teams.TeamInviteManager getTeamInviteManager() {
        return teamInviteManager;
    }

    public com.h2ph.managers.GamertagManager getGamertagManager() {
        return gamertagManager;
    }

    public com.prismcore.survival.limiter.LimiterConfig getLimiterConfig() {
        return limiterConfig;
    }

    public com.prismcore.survival.limiter.LimiterManager getLimiterManager() {
        return limiterManager;
    }

    public com.prismcore.survival.manager.KeyAllManager getKeyAllManager() {
        return keyAllManager;
    }

    public com.prismcore.survival.tools.ToolsManager getToolsManager() {
        return com.prismcore.survival.tools.ToolsManager.getInstance();
    }

    public com.h2ph.commands.player.MediaCommand getMediaCommand() {
        return mediaCommand;
    }

    public com.prismcore.survival.survival.ChatFilter getChatFilter() {
        return chatFilter;
    }

    public com.prismcore.survival.manager.DiscordWebhookManager getDiscordWebhookManager() {
        return discordWebhookManager;
    }

    public com.h2ph.listeners.CommandHideListener getCommandHideListener() {
        return commandHideListener;
    }

    public net.milkbowl.vault.economy.Economy getEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return null;
        }
        org.bukkit.plugin.RegisteredServiceProvider<net.milkbowl.vault.economy.Economy> rsp = getServer()
                .getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        return (rsp == null) ? null : rsp.getProvider();
    }

    public String normalizeKeyName(String keyName) {
        return keyName.toLowerCase().replace(" ", "_");
    }

    private void saveAllResources() {

        saveResourceSafely("economy/shop/config.yml");

        saveResourceSafely("economy/shop/categories/end.yml");
        saveResourceSafely("economy/shop/categories/food.yml");
        saveResourceSafely("economy/shop/categories/gear.yml");
        saveResourceSafely("economy/shop/categories/nether.yml");
        saveResourceSafely("economy/shop/categories/redstone.yml");
        saveResourceSafely("economy/shop/categories/redstone.yml");
        saveResourceSafely("economy/shop/categories/shard.yml");
        saveResourceSafely("survival/AFK/config.yml");
        saveResourceSafely("survival/death/config.yml");
        saveResourceSafely("survival/death/messages.yml");
        saveResourceSafely("economy/config.yml");
        saveResourceSafely("rtp/config.yml");
        saveResourceSafely("crates/keys/config.yml");
        saveResourceSafely("scoreboard/config.yml");
    }

    private void startTeamChatTask() {
        getSchedulerAdapter().runTaskTimer(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                com.prismcore.survival.manager.PlayerData data = getPlayerDataManager().get(player.getUniqueId());
                if (data != null && data.isTeamChat() && data.getTeamId() != null) {
                    player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR,
                            new net.md_5.bungee.api.chat.TextComponent(
                                    com.prismcore.survival.orders.Utils.formatColors("&6You have team chat on.")));
                }
            }
        }, 40L, 40L);
    }

    private void saveResourceSafely(String path) {
        if (!new java.io.File(getDataFolder(), path).exists()) {
            try {
                saveResource(path, false);
            } catch (Exception e) {
                getLogger().warning("Failed to save resource: " + path);
            }
        }
    }

    public void loadSurvivalConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/config.yml");
        if (!file.exists()) {
            saveResource("survival/config.yml", false);
        }
        survivalConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getSurvivalConfig() {
        if (survivalConfig == null) {
            loadSurvivalConfig();
        }
        return survivalConfig;
    }

    private final java.util.Set<java.util.UUID> updateWriters = new java.util.HashSet<>();
    private java.util.List<String> activeUpdatePages = new java.util.ArrayList<>();
    private long activeUpdateVersion = 0;

    public void markPlayerAsUpdateWriter(java.util.UUID uuid) {
        updateWriters.add(uuid);
    }

    public void unmarkPlayerAsUpdateWriter(java.util.UUID uuid) {
        updateWriters.remove(uuid);
    }

    public boolean isPlayerMarkedAsUpdateWriter(java.util.UUID uuid) {
        return updateWriters.contains(uuid);
    }

    public void setActiveUpdate(java.util.List<String> pages) {
        this.activeUpdatePages = new java.util.ArrayList<>(pages);
        this.activeUpdateVersion = System.currentTimeMillis();
        saveUpdateToConfig();
    }

    public java.util.List<String> getActiveUpdatePages() {
        return activeUpdatePages;
    }

    public long getActiveUpdateVersion() {
        return activeUpdateVersion;
    }

    public boolean hasActiveUpdate() {
        return activeUpdatePages != null && !activeUpdatePages.isEmpty();
    }

    private void saveUpdateToConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/update.yml");
        org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);
        eConfig.set("pages", activeUpdatePages);
        eConfig.set("version", activeUpdateVersion);
        try {
            eConfig.save(file);
        } catch (Exception e) {
            getLogger().warning("Failed to save update.yml");
        }
    }

    public void loadUpdateFromConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/update.yml");
        if (file.exists()) {
            org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);
            if (eConfig.contains("pages")) {
                this.activeUpdatePages = eConfig.getStringList("pages");
            }
            this.activeUpdateVersion = eConfig.getLong("version", 0);
        }
    }

    private final java.util.Set<java.util.UUID> advisorWriters = new java.util.HashSet<>();
    private java.util.List<String> activeAdvisorPages = new java.util.ArrayList<>();

    public void markPlayerAsAdvisorWriter(java.util.UUID uuid) {
        advisorWriters.add(uuid);
    }

    public void unmarkPlayerAsAdvisorWriter(java.util.UUID uuid) {
        advisorWriters.remove(uuid);
    }

    public boolean isPlayerMarkedAsAdvisorWriter(java.util.UUID uuid) {
        return advisorWriters.contains(uuid);
    }

    public void setActiveAdvisor(java.util.List<String> pages) {
        this.activeAdvisorPages = new java.util.ArrayList<>(pages);
        saveAdvisorToConfig();
    }

    public java.util.List<String> getActiveAdvisorPages() {
        return activeAdvisorPages;
    }

    public boolean hasActiveAdvisor() {
        return activeAdvisorPages != null && !activeAdvisorPages.isEmpty();
    }

    private void saveAdvisorToConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/advisor.yml");
        org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(file);
        eConfig.set("pages", activeAdvisorPages);
        try {
            eConfig.save(file);
        } catch (Exception e) {
            getLogger().warning("Failed to save advisor.yml");
        }
    }

    public void loadAdvisorFromConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/advisor.yml");
        if (file.exists()) {
            org.bukkit.configuration.file.FileConfiguration eConfig = org.bukkit.configuration.file.YamlConfiguration
                    .loadConfiguration(file);
            if (eConfig.contains("pages")) {
                this.activeAdvisorPages = eConfig.getStringList("pages");
            }
        }
    }

    public com.h2ph.commands.admin.moderations.OffendPlugin getOffendPlugin() {
        return offendPlugin;
    }

    public com.h2ph.api.ApiServer getApiServer() {
        return apiServer;
    }

    public com.prismcore.survival.manager.CarouselManager getCarouselManager() {
        return carouselManager;
    }

    public com.prismcore.survival.manager.CrateLocationRegistry getCrateLocationRegistry() {
        return crateLocationRegistry;
    }

    public com.prismcore.survival.manager.CrateEffectsManager getCrateEffectsManager() {
        return crateEffectsManager;
    }

    public com.prismcore.survival.manager.SpawnManager getSpawnManager() {
        return spawnManager;
    }

    public com.prismcore.survival.manager.TeleportManager getTeleportManager() {
        return teleportManager;
    }

    public ShopCommand getShopCommand() {
        return shopCommand;
    }

    public com.h2ph.commands.player.RulesCommand getRulesCommand() {
        return rulesCommand;
    }

    private org.bukkit.configuration.file.FileConfiguration chatFilterConfig;

    public void loadChatFilterConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "survival/chatfilter/config.yml");
        if (!file.exists()) {
            saveResourceSafely("survival/chatfilter/config.yml");
        }
        chatFilterConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getChatFilterConfig() {
        if (chatFilterConfig == null) {
            loadChatFilterConfig();
        }
        return chatFilterConfig;
    }

    private org.bukkit.configuration.file.FileConfiguration rtpConfig;

    public String getRTPRegionName() {
        java.io.File apiFile = new java.io.File(getDataFolder(), "survival/api/config.yml");
        org.bukkit.configuration.file.YamlConfiguration apiCfg = org.bukkit.configuration.file.YamlConfiguration
                .loadConfiguration(apiFile);
        return apiCfg.getString("region", "europe").toLowerCase();
    }

    public void loadRTPConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/" + getRTPRegionName() + "/config.yml");
        rtpConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getRTPConfig() {
        if (rtpConfig == null) {
            loadRTPConfig();
        }
        return rtpConfig;
    }

    public org.bukkit.configuration.file.FileConfiguration getRTPRegionConfig(String regionName) {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/" + regionName + "/config.yml");
        if (file.exists()) {
            return org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        }
        return null;
    }

    private org.bukkit.configuration.file.FileConfiguration rtpGlobalConfig;

    public void loadGlobalRTPConfig() {
        java.io.File file = new java.io.File(getDataFolder(), "rtp/config.yml");
        if (!file.exists()) {
            saveResourceSafely("rtp/config.yml");
        }
        rtpGlobalConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
    }

    public org.bukkit.configuration.file.FileConfiguration getGlobalRTPConfig() {
        if (rtpGlobalConfig == null) {
            loadGlobalRTPConfig();
        }
        return rtpGlobalConfig;
    }

    public com.h2ph.managers.DamageManager getDamageManager() {
        return damageManager;
    }

    private void printStartupBanner(boolean vaultEnabled) {
        org.bukkit.command.ConsoleCommandSender console = getServer().getConsoleSender();
        String version = getDescription().getVersion();

        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l  _____   _    _      ____    ___    _   _ "));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l |  ___|  / \\  | |    / ___|  / _ \\  | \\ | |"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l | |_    / _ \\ | |   | |     | | | | |  \\| |"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l |  _|  / ___ \\| |   | |___  | |_| | | |\\  |"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&b&l |_|   /_/   \\_\\_|    \\____|  \\___/  |_| \\_|"));
        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&7          Running &b&lFalcon &r&7v" + version + " by &bh2ph"));
        console.sendMessage("");
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&', "&f  &lMODULE STATUS:"));

        if (databaseManager != null && databaseManager.isConnected()) {
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [+] &fDatabase System: &a&lONLINE"));
        } else {
            String error = databaseManager.getConnectionError();
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [-] &fDatabase System: &c&lOFFLINE &7(" + (error != null ? error : "Data cannot be fetched")
                            + ")"));
        }

        if (vaultEnabled && getServer().getPluginManager().getPlugin("Vault") != null) {
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [+] &fEconomy System: &a&lONLINE &7(Vault Hooked)"));
        } else {
            console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                    "&b  [-] &fEconomy System: &c&lOFFLINE &7(Vault Missing/Disabled)"));
        }

        if (apiServer != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fWeb API Server: &a&lONLINE"));
        } else {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [-] &fWeb API Server: &c&lOFFLINE"));
        }

        if (offendPlugin != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fModeration Core: &a&lONLINE"));
        }

        if (getServer().getPluginManager().getPlugin("WorldEdit") != null
                || getServer().getPluginManager().getPlugin("FastAsyncWorldEdit") != null) {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [+] &fWorldEdit Hook: &a&lCONNECTED"));
        } else {
            console.sendMessage(
                    org.bukkit.ChatColor.translateAlternateColorCodes('&', "&b  [-] &fWorldEdit Hook: &c&lNOT FOUND"));
        }

        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
        console.sendMessage(
                org.bukkit.ChatColor.translateAlternateColorCodes('&', "&a&l  FALCON SUCCESSFULLY INITIALIZED"));
        console.sendMessage(org.bukkit.ChatColor.translateAlternateColorCodes('&',
                "&8&m--------------------------------------------------"));
    }
}
