# Falcon Core ( Donut SMP Core )

FalconH2 is a custom Minecraft server plugin codebase for Minecraft Servers like Donut SMP.  
It targets modern Paper/Folia-compatible servers and bundles gameplay, economy, moderation, utility, and GUI-driven systems in one plugin.

## Plugin metadata

- **Name:** Falcon
- **Version:** 7.0
- **Main class:** `com.h2ph.Falcon`
- **API version:** 1.26
- **Folia supported:** yes
- **Hard dependencies:** Vault, packetevents
- **Soft dependencies:** PlaceholderAPI, WorldEdit, FastAsyncWorldEdit, LuckPerms, voicechat

Source: `src/main/resources/plugin.yml`

## Included systems (high level)

- Economy and balance commands (`/balance`, `/pay`, `/baltop`, `/economy`)
- Homes, warps, spawn, and teleport request flows (`/home`, `/warp`, `/spawn`, `/tpa`)
- Moderation/admin tools (ban, vanish, spectator, maintenance, inventory inspection)
- Advanced Muting System (`/mute chat`, `/mute voice` with Simple Voice Chat integration)
- GUI-driven features (shop, auction, crates, settings, sell/worth, profile, order systems)
- Team and social utilities (messages/reply, ignore, team, stats, disguise, bounty)

## Repository structure

- `src/main/java/com/h2ph` – core plugin modules
- `src/main/java/com/falconcore/survival` – survival/economy-related components
- `src/main/resources` – plugin resources and feature configs
- `mappings-cloud` – mapping data
- `libs` – local dependency jars (e.g., `server.jar`)
