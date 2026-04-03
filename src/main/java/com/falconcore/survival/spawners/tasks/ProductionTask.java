package com.falconcore.survival.spawners.tasks;

import com.falconcore.survival.spawners.storage.SpawnerData;
import com.falconcore.survival.spawners.storage.SpawnerManager;
import com.falconcore.survival.spawners.mob.SpawnerType;
import org.bukkit.Material;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ProductionTask extends BukkitRunnable {
    private final SpawnerManager manager;
    private final Random random = new Random();

    public ProductionTask(SpawnerManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        for (SpawnerData data : manager.getSpawners().values()) {
            double multiplier = manager.getPlugin().getConfig().getDouble("multipliers." + data.getType().name(), 1.0);
            if (manager.isIsolated(data.getLocation())) {
                multiplier += manager.getPlugin().getConfig().getDouble("isolated_bonus", 0.5);
            }
            int amount = (int) (data.getStackSize() * multiplier);
            if (amount > 0) {
                Map<Material, Long> drops = getDropsForType(data, amount);
                data.addDrops(drops);
                data.addXP(amount * manager.getXPAmount());
            }
        }
    }

    private Map<Material, Long> getDropsForType(SpawnerData data, int amount) {
        Map<Material, Long> drops = new HashMap<>();
        SpawnerType type = data.getType();
        java.util.Set<Material> blacklist = data.getBlacklistedLoot();

        switch (type) {
            case SKELETON:
                if (!blacklist.contains(Material.BONE)) drops.put(Material.BONE, (long) amount);
                if (!blacklist.contains(Material.ARROW)) drops.put(Material.ARROW, (long) amount);
                break;
            case ZOMBIE:
                if (!blacklist.contains(Material.ROTTEN_FLESH)) drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case SPIDER:
                if (!blacklist.contains(Material.STRING)) drops.put(Material.STRING, (long) amount);
                if (!blacklist.contains(Material.SPIDER_EYE)) drops.put(Material.SPIDER_EYE, (long) (amount / 2));
                break;
            case CREEPER:
                if (!blacklist.contains(Material.GUNPOWDER)) drops.put(Material.GUNPOWDER, (long) amount);
                break;
            case ENDERMAN:
                if (!blacklist.contains(Material.ENDER_PEARL)) drops.put(Material.ENDER_PEARL, (long) amount);
                break;
            case BLAZE:
                if (!blacklist.contains(Material.BLAZE_ROD)) drops.put(Material.BLAZE_ROD, (long) amount);
                break;
            case GHAST:
                if (!blacklist.contains(Material.GHAST_TEAR)) drops.put(Material.GHAST_TEAR, (long) amount);
                break;
            case WITCH:
                if (!blacklist.contains(Material.STICK)) drops.put(Material.STICK, (long) amount);
                if (!blacklist.contains(Material.GLASS_BOTTLE)) drops.put(Material.GLASS_BOTTLE, (long) (amount / 2));
                break;
            case PIGLIN:
                if (!blacklist.contains(Material.GOLD_INGOT)) drops.put(Material.GOLD_INGOT, (long) amount);
                break;
            case PIGLIN_BRUTE:
                if (!blacklist.contains(Material.GOLDEN_SWORD)) drops.put(Material.GOLDEN_SWORD, (long) amount);
                break;
            case HOGLIN:
                if (!blacklist.contains(Material.COOKED_PORKCHOP)) drops.put(Material.COOKED_PORKCHOP, (long) amount);
                break;
            case ZOGLIN:
                if (!blacklist.contains(Material.ROTTEN_FLESH)) drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case STRIDER:
                if (!blacklist.contains(Material.STRING)) drops.put(Material.STRING, (long) amount);
                break;
            case MAGMA_CUBE:
                if (!blacklist.contains(Material.MAGMA_CREAM)) drops.put(Material.MAGMA_CREAM, (long) amount);
                break;
            case SLIME:
                if (!blacklist.contains(Material.SLIME_BALL)) drops.put(Material.SLIME_BALL, (long) amount);
                break;
            case PHANTOM:
                if (!blacklist.contains(Material.PHANTOM_MEMBRANE)) drops.put(Material.PHANTOM_MEMBRANE, (long) amount);
                break;
            case DROWNED:
                if (!blacklist.contains(Material.ROTTEN_FLESH)) drops.put(Material.ROTTEN_FLESH, (long) amount);
                if (!blacklist.contains(Material.TRIDENT)) drops.put(Material.TRIDENT, (long) (amount / 10));
                break;
            case HUSK:
                if (!blacklist.contains(Material.ROTTEN_FLESH)) drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case STRAY:
                if (!blacklist.contains(Material.BONE)) drops.put(Material.BONE, (long) amount);
                if (!blacklist.contains(Material.ARROW)) drops.put(Material.ARROW, (long) amount);
                break;
            case WITHER_SKELETON:
                if (!blacklist.contains(Material.BONE)) drops.put(Material.BONE, (long) amount);
                if (!blacklist.contains(Material.COAL)) drops.put(Material.COAL, (long) amount);
                break;
            case PILLAGER:
                if (!blacklist.contains(Material.ARROW)) drops.put(Material.ARROW, (long) amount);
                break;
            case VINDICATOR:
                if (!blacklist.contains(Material.EMERALD)) drops.put(Material.EMERALD, (long) amount);
                break;
            case EVOKER:
                if (!blacklist.contains(Material.TOTEM_OF_UNDYING)) drops.put(Material.TOTEM_OF_UNDYING, (long) (amount / 10));
                break;
            case RAVAGER:
                if (!blacklist.contains(Material.SADDLE)) drops.put(Material.SADDLE, (long) amount);
                break;
            case GUARDIAN:
                if (!blacklist.contains(Material.PRISMARINE_SHARD)) drops.put(Material.PRISMARINE_SHARD, (long) amount);
                break;
            case ELDER_GUARDIAN:
                if (!blacklist.contains(Material.PRISMARINE_CRYSTALS)) drops.put(Material.PRISMARINE_CRYSTALS, (long) amount);
                break;
            case CAVE_SPIDER:
                if (!blacklist.contains(Material.STRING)) drops.put(Material.STRING, (long) amount);
                if (!blacklist.contains(Material.SPIDER_EYE)) drops.put(Material.SPIDER_EYE, (long) (amount / 2));
                break;
            case SHULKER:
                if (!blacklist.contains(Material.SHULKER_SHELL)) drops.put(Material.SHULKER_SHELL, (long) amount);
                break;
            case IRON_GOLEM:
                if (!blacklist.contains(Material.IRON_INGOT)) drops.put(Material.IRON_INGOT, (long) amount);
                break;
            case SNOW_GOLEM:
                if (!blacklist.contains(Material.SNOWBALL)) drops.put(Material.SNOWBALL, (long) amount);
                break;
            case CAT:
                if (!blacklist.contains(Material.STRING)) drops.put(Material.STRING, (long) amount);
                break;
            case HORSE:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                break;
            case DONKEY:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                break;
            case MULE:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                break;
            case SKELETON_HORSE:
                if (!blacklist.contains(Material.BONE)) drops.put(Material.BONE, (long) amount);
                break;
            case ZOMBIE_HORSE:
                if (!blacklist.contains(Material.ROTTEN_FLESH)) drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case LLAMA:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                break;
            case TRADER_LLAMA:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                break;
            case PARROT:
                if (!blacklist.contains(Material.FEATHER)) drops.put(Material.FEATHER, (long) amount);
                break;
            case COD:
                if (!blacklist.contains(Material.COD)) drops.put(Material.COD, (long) amount);
                break;
            case SALMON:
                if (!blacklist.contains(Material.SALMON)) drops.put(Material.SALMON, (long) amount);
                break;
            case PUFFERFISH:
                if (!blacklist.contains(Material.PUFFERFISH)) drops.put(Material.PUFFERFISH, (long) amount);
                break;
            case TROPICAL_FISH:
                if (!blacklist.contains(Material.TROPICAL_FISH)) drops.put(Material.TROPICAL_FISH, (long) amount);
                break;
            case SQUID:
                if (!blacklist.contains(Material.INK_SAC)) drops.put(Material.INK_SAC, (long) amount);
                break;
            case GLOW_SQUID:
                if (!blacklist.contains(Material.GLOW_INK_SAC)) drops.put(Material.GLOW_INK_SAC, (long) amount);
                break;
            case TURTLE:
                if (!blacklist.contains(Material.SEAGRASS)) drops.put(Material.SEAGRASS, (long) amount);
                break;
            case PANDA:
                if (!blacklist.contains(Material.BAMBOO)) drops.put(Material.BAMBOO, (long) amount);
                break;
            case BEE:
                if (!blacklist.contains(Material.HONEYCOMB)) drops.put(Material.HONEYCOMB, (long) amount);
                break;
            case CHICKEN:
                if (!blacklist.contains(Material.FEATHER)) drops.put(Material.FEATHER, (long) amount);
                if (!blacklist.contains(Material.CHICKEN)) drops.put(Material.CHICKEN, (long) amount);
                break;
            case COW:
                if (!blacklist.contains(Material.LEATHER)) drops.put(Material.LEATHER, (long) amount);
                if (!blacklist.contains(Material.BEEF)) drops.put(Material.BEEF, (long) amount);
                break;
            case PIG:
                if (!blacklist.contains(Material.PORKCHOP)) drops.put(Material.PORKCHOP, (long) amount);
                break;
            case SHEEP:
                if (!blacklist.contains(Material.WHITE_WOOL)) drops.put(Material.WHITE_WOOL, (long) amount);
                if (!blacklist.contains(Material.MUTTON)) drops.put(Material.MUTTON, (long) amount);
                break;
            case RABBIT:
                if (!blacklist.contains(Material.RABBIT)) drops.put(Material.RABBIT, (long) amount);
                if (!blacklist.contains(Material.RABBIT_FOOT)) drops.put(Material.RABBIT_FOOT, (long) (amount / 10));
                break;
            case WARDEN:
                if (!blacklist.contains(Material.SCULK)) drops.put(Material.SCULK, (long) amount);
                break;
            case VEX:
            case SILVERFISH:
            case ENDERMITE:
            case WOLF:
            case OCELOT:
            case BAT:
            case DOLPHIN:
            case FOX:
            case POLAR_BEAR:
            case AXOLOTL:
            case GOAT:
            case FROG:
            case ALLAY:
            case TADPOLE:
            case CAMEL:
            case SNIFFER:
            case ARMADILLO:
                break;
            default:
                if (!blacklist.contains(type.getMaterial())) drops.put(type.getMaterial(), (long) amount);
                break;
        }
        return drops;
    }
}
