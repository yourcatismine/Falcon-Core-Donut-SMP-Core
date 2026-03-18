package com.prismcore.survival.spawners.tasks;

import com.prismcore.survival.spawners.storage.SpawnerData;
import com.prismcore.survival.spawners.storage.SpawnerManager;
import com.prismcore.survival.spawners.mob.SpawnerType;
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
                Map<Material, Long> drops = getDropsForType(data.getType(), amount);
                data.addDrops(drops);
                data.addXP(amount * manager.getXPAmount());
            }
        }
    }

    private Map<Material, Long> getDropsForType(SpawnerType type, int amount) {
        Map<Material, Long> drops = new HashMap<>();
        switch (type) {
            case SKELETON:
                drops.put(Material.BONE, (long) amount);
                drops.put(Material.ARROW, (long) amount);
                break;
            case ZOMBIE:
                drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case SPIDER:
                drops.put(Material.STRING, (long) amount);
                drops.put(Material.SPIDER_EYE, (long) (amount / 2));
                break;
            case CREEPER:
                drops.put(Material.GUNPOWDER, (long) amount);
                break;
            case ENDERMAN:
                drops.put(Material.ENDER_PEARL, (long) amount);
                break;
            case BLAZE:
                drops.put(Material.BLAZE_ROD, (long) amount);
                break;
            case GHAST:
                drops.put(Material.GHAST_TEAR, (long) amount);
                break;
            case WITCH:
                drops.put(Material.STICK, (long) amount);
                drops.put(Material.GLASS_BOTTLE, (long) (amount / 2));
                break;
            case PIGLIN:
                drops.put(Material.GOLD_INGOT, (long) amount);
                break;
            case PIGLIN_BRUTE:
                drops.put(Material.GOLDEN_SWORD, (long) amount);
                break;
            case HOGLIN:
                drops.put(Material.COOKED_PORKCHOP, (long) amount);
                break;
            case ZOGLIN:
                drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case STRIDER:
                drops.put(Material.STRING, (long) amount);
                break;
            case MAGMA_CUBE:
                drops.put(Material.MAGMA_CREAM, (long) amount);
                break;
            case SLIME:
                drops.put(Material.SLIME_BALL, (long) amount);
                break;
            case PHANTOM:
                drops.put(Material.PHANTOM_MEMBRANE, (long) amount);
                break;
            case DROWNED:
                drops.put(Material.ROTTEN_FLESH, (long) amount);
                drops.put(Material.TRIDENT, (long) (amount / 10));
                break;
            case HUSK:
                drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case STRAY:
                drops.put(Material.BONE, (long) amount);
                drops.put(Material.ARROW, (long) amount);
                break;
            case WITHER_SKELETON:
                drops.put(Material.BONE, (long) amount);
                drops.put(Material.COAL, (long) amount);
                break;
            case PILLAGER:
                drops.put(Material.ARROW, (long) amount);
                break;
            case VINDICATOR:
                drops.put(Material.EMERALD, (long) amount);
                break;
            case EVOKER:
                drops.put(Material.TOTEM_OF_UNDYING, (long) (amount / 10));
                break;
            case RAVAGER:
                drops.put(Material.SADDLE, (long) amount);
                break;
            case VEX:
                break;
            case GUARDIAN:
                drops.put(Material.PRISMARINE_SHARD, (long) amount);
                break;
            case ELDER_GUARDIAN:
                drops.put(Material.PRISMARINE_CRYSTALS, (long) amount);
                break;
            case SILVERFISH:
                break;
            case ENDERMITE:
                break;
            case CAVE_SPIDER:
                drops.put(Material.STRING, (long) amount);
                drops.put(Material.SPIDER_EYE, (long) (amount / 2));
                break;
            case SHULKER:
                drops.put(Material.SHULKER_SHELL, (long) amount);
                break;
            case IRON_GOLEM:
                drops.put(Material.IRON_INGOT, (long) amount);
                break;
            case SNOW_GOLEM:
                drops.put(Material.SNOWBALL, (long) amount);
                break;
            case WOLF:
                break;
            case OCELOT:
                break;
            case CAT:
                drops.put(Material.STRING, (long) amount);
                break;
            case HORSE:
                drops.put(Material.LEATHER, (long) amount);
                break;
            case DONKEY:
                drops.put(Material.LEATHER, (long) amount);
                break;
            case MULE:
                drops.put(Material.LEATHER, (long) amount);
                break;
            case SKELETON_HORSE:
                drops.put(Material.BONE, (long) amount);
                break;
            case ZOMBIE_HORSE:
                drops.put(Material.ROTTEN_FLESH, (long) amount);
                break;
            case LLAMA:
                drops.put(Material.LEATHER, (long) amount);
                break;
            case TRADER_LLAMA:
                drops.put(Material.LEATHER, (long) amount);
                break;
            case PARROT:
                drops.put(Material.FEATHER, (long) amount);
                break;
            case BAT:
                break;
            case COD:
                drops.put(Material.COD, (long) amount);
                break;
            case SALMON:
                drops.put(Material.SALMON, (long) amount);
                break;
            case PUFFERFISH:
                drops.put(Material.PUFFERFISH, (long) amount);
                break;
            case TROPICAL_FISH:
                drops.put(Material.TROPICAL_FISH, (long) amount);
                break;
            case SQUID:
                drops.put(Material.INK_SAC, (long) amount);
                break;
            case GLOW_SQUID:
                drops.put(Material.GLOW_INK_SAC, (long) amount);
                break;
            case TURTLE:
                drops.put(Material.SEAGRASS, (long) amount);
                break;
            case DOLPHIN:
                break;
            case PANDA:
                drops.put(Material.BAMBOO, (long) amount);
                break;
            case FOX:
                break;
            case BEE:
                drops.put(Material.HONEYCOMB, (long) amount);
                break;
            case CHICKEN:
                drops.put(Material.FEATHER, (long) amount);
                drops.put(Material.CHICKEN, (long) amount);
                break;
            case COW:
                drops.put(Material.LEATHER, (long) amount);
                drops.put(Material.BEEF, (long) amount);
                break;
            case PIG:
                drops.put(Material.PORKCHOP, (long) amount);
                break;
            case SHEEP:
                drops.put(Material.WHITE_WOOL, (long) amount);
                drops.put(Material.MUTTON, (long) amount);
                break;
            case RABBIT:
                drops.put(Material.RABBIT, (long) amount);
                drops.put(Material.RABBIT_FOOT, (long) (amount / 10));
                break;
            case POLAR_BEAR:
                break;
            case AXOLOTL:
                break;
            case GOAT:
                break;
            case FROG:
                break;
            case ALLAY:
                break;
            case TADPOLE:
                break;
            case WARDEN:
                drops.put(Material.SCULK, (long) amount);
                break;
            case CAMEL:
                break;
            case SNIFFER:
                break;
            case ARMADILLO:
                break;
            default:
                drops.put(type.getMaterial(), (long) amount);
                break;
        }
        return drops;
    }
}
