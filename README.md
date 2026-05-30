# FalconH2

FalconH2 is a custom Minecraft server plugin codebase for the **Falcon** network.  
It targets modern Paper/Folia-compatible servers and bundles gameplay, economy, moderation, utility, and GUI-driven systems in one plugin.

## Plugin metadata

- **Name:** Falcon
- **Version:** 5.5
- **Main class:** `com.h2ph.Falcon`
- **API version:** 1.26
- **Folia supported:** yes
- **Hard dependencies:** Vault, packetevents
- **Soft dependencies:** PlaceholderAPI, WorldEdit, FastAsyncWorldEdit, LuckPerms

Source: `src/main/resources/plugin.yml`

## Included systems (high level)

- Economy and balance commands (`/balance`, `/pay`, `/baltop`, `/economy`)
- Homes, warps, spawn, and teleport request flows (`/home`, `/warp`, `/spawn`, `/tpa`)
- Moderation/admin tools (ban, mute, vanish, spectator, maintenance, inventory inspection)
- GUI-driven features (shop, auction, crates, settings, sell/worth, profile, order systems)
- Team and social utilities (messages/reply, ignore, team, stats, disguise, bounty)

## Repository structure

- `src/main/java/com/h2ph` – core plugin modules
- `src/main/java/com/falconcore/survival` – survival/economy-related components
- `src/main/resources` – plugin resources and feature configs
- `mappings-cloud` – mapping data
- `libs` – local dependency jars (e.g., `server.jar`)

## Build/test status in this snapshot

This repository snapshot does **not** currently include a Maven/Gradle project file (`pom.xml`/`build.gradle`), so standard build/test commands cannot be run directly from this root checkout.
