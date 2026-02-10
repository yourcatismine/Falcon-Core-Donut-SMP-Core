/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.bukkit.ChatColor
 *  org.bukkit.entity.Entity
 *  org.bukkit.entity.Player
 *  org.bukkit.event.EventHandler
 *  org.bukkit.event.Listener
 *  org.bukkit.event.player.AsyncPlayerChatEvent
 *  org.bukkit.plugin.Plugin
 */
package com.prismcore.survival.orders.input;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import com.prismcore.survival.orders.OrdersModule;
import com.prismcore.survival.orders.gui.NewOrderMenu;
import com.prismcore.survival.orders.gui.OrdersMainMenu;
import com.prismcore.survival.orders.gui.SelectItemMenu;
import com.prismcore.survival.orders.util.TaskUtil;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

public class ChatInputManager
        implements Listener {
    private final OrdersModule module;
    private final Map<UUID, Prompt> prompts = new HashMap<UUID, Prompt>();
    private final Map<UUID, NewOrderSession> sessions = new HashMap<UUID, NewOrderSession>();

    public ChatInputManager(OrdersModule module) {
        this.module = module;
    }

    public void prompt(Player p, Kind kind, String messageToPlayer) {
        this.prompts.put(p.getUniqueId(), new Prompt(kind));
        p.closeInventory();
        p.sendMessage(messageToPlayer);
    }

    public NewOrderSession session(UUID u) {
        return this.sessions.computeIfAbsent(u, k -> new NewOrderSession());
    }

    public void clearSession(UUID u) {
        this.sessions.remove(u);
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        UUID u = p.getUniqueId();
        Prompt pr = this.prompts.remove(u);
        if (pr == null) {
            return;
        }
        e.setCancelled(true);
        String msg = e.getMessage().trim();
        switch (pr.kind.ordinal()) {
            case 0: {
                this.module.state().main((UUID) u).search = msg;
                p.sendMessage(this.module.cfg().msg("chat.search_set", "&aSearch set: &f" + msg));
                TaskUtil.runEntity((Plugin) this.module.getPlugin(), (Entity) p,
                        () -> new OrdersMainMenu(this.module, p).open());
                break;
            }
            case 1: {
                this.module.state().items((UUID) u).search = msg;
                p.sendMessage(this.module.cfg().msg("chat.search_set", "&aSearch set: &f" + msg));
                TaskUtil.runEntity((Plugin) this.module.getPlugin(), (Entity) p,
                        () -> new SelectItemMenu(this.module, p).open());
                break;
            }
            case 2: {
                try {
                    int amt = Integer.parseInt(msg);
                    if (amt <= 0) {
                        throw new NumberFormatException();
                    }
                    this.session((UUID) u).amount = amt;
                    p.sendMessage(this.module.cfg().msg("chat.amount_ok", "&aAmount set: &f" + amt));
                    TaskUtil.runEntity((Plugin) this.module.getPlugin(), (Entity) p,
                            () -> new NewOrderMenu(this.module, p).open());
                } catch (NumberFormatException ex) {
                    p.sendMessage(String.valueOf(ChatColor.RED) + "Invalid amount.");
                }
                break;
            }
            case 3: {
                try {
                    double price = Double.parseDouble(msg);
                    if (price <= 0.0) {
                        throw new NumberFormatException();
                    }
                    this.session((UUID) u).priceEach = price;
                    p.sendMessage(this.module.cfg().msg("chat.price_ok", "&aPrice set: &f$" + price));
                    TaskUtil.runEntity((Plugin) this.module.getPlugin(), (Entity) p,
                            () -> new NewOrderMenu(this.module, p).open());
                    break;
                } catch (NumberFormatException ex) {
                    p.sendMessage(String.valueOf(ChatColor.RED) + "Invalid price.");
                }
            }
        }
    }

    public static class Prompt {
        public final Kind kind;

        public Prompt(Kind k) {
            this.kind = k;
        }
    }

    public static enum Kind {
        SEARCH_MAIN,
        SEARCH_SELECT,
        AMOUNT,
        PRICE;

    }

    public static class NewOrderSession {
        public String chosenItem;
        public Integer amount;
        public Double priceEach;
    }
}
