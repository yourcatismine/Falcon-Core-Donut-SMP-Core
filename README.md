# Falcon Core (Donut SMP Core)

Falcon Core is an all-in-one server plugin built for modern Minecraft servers such as Donut SMP-style networks. It bundles economy, moderation, utility, and GUI-driven gameplay systems into a single, easy-to-deploy plugin so server owners don't need to stitch together a dozen separate plugins.

**Created by Kiarerss | h2ph**

## Features

- **Economy** — Balance, pay, baltop, sell, worth, and full economy management.
- **Homes, Warps & Teleportation** — Homes, warps, spawn, TPA requests, and random teleport.
- **Moderation & Admin Tools** — Bans, mutes, vanish, spectator, maintenance mode, inventory/ender chest inspection, and alt detection.
- **GUI-Driven Systems** — Shop, auction house, crates, orders, settings, profiles, and Billford trade GUI.
- **Social & Team Utilities** — Private messaging, ignore lists, teams, stats, disguises, and bounties.
- **Spawner System** — Give, sell, and manage mob spawners as tradeable items, with built-in dupe protection.
- **MySQL & YAML Support** — Choose the storage backend that fits your server; switch between lightweight local YAML files or a full MySQL database right from the config.
- **Many More Features** — This README only covers the highlights; Falcon Core includes many more systems beyond what's listed here.

## Plugin Metadata

| Key | Value |
|---|---|
| **Name** | Falcon |
| **Version** | 7.0 |
| **Main class** | `com.h2ph.Falcon` |
| **API version** | 1.13 |
| **Folia supported** | Yes |
| **Hard dependencies** | Vault, packetevents |
| **Soft dependencies** | PlaceholderAPI, WorldEdit, FastAsyncWorldEdit, LuckPerms, Simple Voice Chat |

Source: `src/main/resources/plugin.yml`

## Compatibility

- **Supports:** Paper and Folia
- **API Version:** 1.13

## Dependencies

**Required:**
- Vault
- packetevents

**Optional:**
- PlaceholderAPI
- WorldEdit
- FastAsyncWorldEdit
- LuckPerms
- Simple Voice Chat

## Installation

1. Download the latest Falcon Core jar.
2. Install the required dependencies (Vault, packetevents) on your server.
3. Drop the jar into your server's `/plugins` folder.
4. Restart the server and configure via the generated config files.

## Repository Structure

- `src/main/java/com/h2ph` – core plugin modules
- `src/main/java/com/falconcore/survival` – survival/economy-related components
- `src/main/resources` – plugin resources and feature configs
- `mappings-cloud` – mapping data
- `libs` – local dependency jars (e.g., `server.jar`)

## Commands

### Economy
| Command | Description |
|---|---|
| `/balance [player]` (`/bal`) | View balance |
| `/pay <player> <amount>` | Send money |
| `/baltop` | Top balances leaderboard |
| `/economy <give\|set\|remove> <player> <amount>` | Admin: manage balances |
| `/sell` | Sell GUI |
| `/sellhistory` (`/history`) | View sell history |
| `/worth` | Item price GUI |
| `/shop` | Shop GUI |

### Homes, Warps & Teleport
| Command | Description |
|---|---|
| `/home` | Homes GUI |
| `/spawn [name]` | Teleport to spawn |
| `/setspawn <name>` | Admin: set spawn point |
| `/warp <name>` | Teleport to warp |
| `/tp <player>` | Teleport to player |
| `/tpa`, `/tpahere`, `/tpaccept`, `/tpacancel`, `/tpadeny` | Teleport request flow |
| `/tpauto` | Auto-accept teleport requests |
| `/otp <player>` | Teleport to offline player's last logout spot |
| `/rtp` | Random teleport GUI |
| `/whereami [player]` | Check location |

### Moderation & Admin
| Command | Description |
|---|---|
| `/offend <player> <reason> [duration]` (`/ban`) | Ban a player |
| `/unban <player>` (`/pardon`) | Unban a player |
| `/checkban <player/ID>` | Check ban status |
| `/checkalt <player>` | Check alt accounts by IP |
| `/checkplayers` | List online players + worlds |
| `/checktotem <player>` | Remove offhand totem |
| `/whowashere` | Chunk player history |
| `/invsee <player>` / `/endersee <player>` | Inspect inventory/ender chest |
| `/vanish` | Toggle visibility |
| `/gmc` `/gms` `/gma` `/gmsp` | Quick gamemode switches |
| `/fly` | Toggle flight |
| `/nv` (`/nightvision`) | Toggle night vision |
| `/speed <amount\|normal>` | Manage flying speed |
| `/hide` | Hide nametag |
| `/disguise <player> [skin]` (`/dis`) | Disguise as another player |
| `/sus` | Suspicious activity monitor |
| `/maintenance` | Toggle maintenance mode |
| `/announce <message> [repeat <count>]` | Broadcast announcement |
| `/mute` / `/unmute` / `/checkmute <player>` | Chat mute system |

### GUI Systems
| Command | Description |
|---|---|
| `/ah <search>` | Auction house |
| `/auction <reload>` | Admin: reload auction system |
| `/crate <create\|edit\|get\|delete\|effects>` | Manage crates |
| `/key <give\|set\|remove\|reset>` | Manage crate keys |
| `/order` (`/orders`) | Orders GUI |
| `/billford [admin]` | Billford trade GUI |
| `/settings` | Settings GUI |
| `/profile <player>` | View player profile |
| `/redstone` | Redstone manager GUI |
| `/anvil` `/craftingtable` `/smithingtable` | Virtual crafting stations |
| `/echest` (`/enderchest`) | Personal ender chest |

### Social & Team
| Command | Description |
|---|---|
| `/msg <player> <message>` (`/tell`, `/w`, `/wisper`) | Private message |
| `/reply <message>` (`/r`) | Reply to last message |
| `/team <subcommand>` | Team management |
| `/ignore` / `/unignore <player>` | Block a player |
| `/bounty [add <player> <amount>]` (`/bounties`) | Manage bounties |
| `/stats [player]` | View player stats |

### Other Utility
| Command | Description |
|---|---|
| `/duel <player\|create\|settings>` | Duel system |
| `/spawner give <player> <type> [amount]` (`/ss`) | Manage spawners |
| `/amethyst <type> <player> [duration]` | Manage amethyst tools |
| `/afk` | AFK areas GUI |
| `/tab <reload\|refresh\|rankings\|setranking\|toggle\|info>` | TAB list management |
| `/rules` `/media` `/advisor` `/discord` `/store` | Server info commands |
| `/minigames` | Transfer to minigame server |
| `/update` | Create server update book |
| `/falcon <reload\|auction\|order\|shards\|...>` | Main admin/utility hub |
| `/shards <give\|set\|remove> <player> <amount>` | Admin: manage shards |

---

**Created by Kiarerss | h2ph**