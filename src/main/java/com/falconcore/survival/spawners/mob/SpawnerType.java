package com.falconcore.survival.spawners.mob;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

public enum SpawnerType {
    ZOMBIE(EntityType.ZOMBIE, Material.ZOMBIE_HEAD, "Zombie"),
    SKELETON(EntityType.SKELETON, Material.SKELETON_SKULL, "Skeleton"),
    SPIDER(EntityType.SPIDER, Material.SPIDER_EYE, "Spider"),
    CREEPER(EntityType.CREEPER, Material.CREEPER_HEAD, "Creeper"),
    ENDERMAN(EntityType.ENDERMAN, Material.ENDER_PEARL, "Enderman"),
    BLAZE(EntityType.BLAZE, Material.BLAZE_ROD, "Blaze"),
    GHAST(EntityType.GHAST, Material.GHAST_TEAR, "Ghast"),
    WITCH(EntityType.WITCH, Material.STICK, "Witch"),
    PIGLIN(EntityType.PIGLIN, Material.GOLD_INGOT, "Piglin"),
    PIGLIN_BRUTE(EntityType.PIGLIN_BRUTE, Material.GOLDEN_SWORD, "Piglin Brute"),
    HOGLIN(EntityType.HOGLIN, Material.COOKED_PORKCHOP, "Hoglin"),
    ZOGLIN(EntityType.ZOGLIN, Material.ROTTEN_FLESH, "Zoglin"),
    STRIDER(EntityType.STRIDER, Material.WARPED_FUNGUS_ON_A_STICK, "Strider"),
    MAGMA_CUBE(EntityType.MAGMA_CUBE, Material.MAGMA_CREAM, "Magma Cube"),
    SLIME(EntityType.SLIME, Material.SLIME_BALL, "Slime"),
    PHANTOM(EntityType.PHANTOM, Material.PHANTOM_MEMBRANE, "Phantom"),
    DROWNED(EntityType.DROWNED, Material.TRIDENT, "Drowned"),
    HUSK(EntityType.HUSK, Material.SAND, "Husk"),
    STRAY(EntityType.STRAY, Material.BONE, "Stray"),
    WITHER_SKELETON(EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL, "Wither Skeleton"),
    PILLAGER(EntityType.PILLAGER, Material.CROSSBOW, "Pillager"),
    VINDICATOR(EntityType.VINDICATOR, Material.IRON_AXE, "Vindicator"),
    EVOKER(EntityType.EVOKER, Material.TOTEM_OF_UNDYING, "Evoker"),
    RAVAGER(EntityType.RAVAGER, Material.SADDLE, "Ravager"),
    VEX(EntityType.VEX, Material.IRON_SWORD, "Vex"),
    GUARDIAN(EntityType.GUARDIAN, Material.PRISMARINE_SHARD, "Guardian"),
    ELDER_GUARDIAN(EntityType.ELDER_GUARDIAN, Material.PRISMARINE_CRYSTALS, "Elder Guardian"),
    SILVERFISH(EntityType.SILVERFISH, Material.STONE, "Silverfish"),
    ENDERMITE(EntityType.ENDERMITE, Material.ENDER_PEARL, "Endermite"),
    CAVE_SPIDER(EntityType.CAVE_SPIDER, Material.STRING, "Cave Spider"),
    SHULKER(EntityType.SHULKER, Material.SHULKER_SHELL, "Shulker"),
    IRON_GOLEM(EntityType.IRON_GOLEM, Material.IRON_BLOCK, "Iron Golem"),
    SNOW_GOLEM(EntityType.SNOW_GOLEM, Material.SNOWBALL, "Snow Golem"),
    WOLF(EntityType.WOLF, Material.BONE, "Wolf"),
    OCELOT(EntityType.OCELOT, Material.STRING, "Ocelot"),
    CAT(EntityType.CAT, Material.STRING, "Cat"),
    HORSE(EntityType.HORSE, Material.APPLE, "Horse"),
    DONKEY(EntityType.DONKEY, Material.CHEST, "Donkey"),
    MULE(EntityType.MULE, Material.LEATHER, "Mule"),
    SKELETON_HORSE(EntityType.SKELETON_HORSE, Material.BONE, "Skeleton Horse"),
    ZOMBIE_HORSE(EntityType.ZOMBIE_HORSE, Material.ROTTEN_FLESH, "Zombie Horse"),
    LLAMA(EntityType.LLAMA, Material.LEATHER, "Llama"),
    TRADER_LLAMA(EntityType.TRADER_LLAMA, Material.LEATHER, "Trader Llama"),
    PARROT(EntityType.PARROT, Material.FEATHER, "Parrot"),
    BAT(EntityType.BAT, Material.COAL, "Bat"),
    COD(EntityType.COD, Material.COD, "Cod"),
    SALMON(EntityType.SALMON, Material.SALMON, "Salmon"),
    PUFFERFISH(EntityType.PUFFERFISH, Material.PUFFERFISH, "Pufferfish"),
    TROPICAL_FISH(EntityType.TROPICAL_FISH, Material.TROPICAL_FISH, "Tropical Fish"),
    SQUID(EntityType.SQUID, Material.INK_SAC, "Squid"),
    GLOW_SQUID(EntityType.GLOW_SQUID, Material.GLOW_INK_SAC, "Glow Squid"),
    TURTLE(EntityType.TURTLE, Material.TURTLE_EGG, "Turtle"),
    DOLPHIN(EntityType.DOLPHIN, Material.COD, "Dolphin"),
    PANDA(EntityType.PANDA, Material.BAMBOO, "Panda"),
    FOX(EntityType.FOX, Material.SWEET_BERRIES, "Fox"),
    BEE(EntityType.BEE, Material.HONEYCOMB, "Bee"),
    CHICKEN(EntityType.CHICKEN, Material.FEATHER, "Chicken"),
    COW(EntityType.COW, Material.LEATHER, "Cow"),
    PIG(EntityType.PIG, Material.PORKCHOP, "Pig"),
    SHEEP(EntityType.SHEEP, Material.WHITE_WOOL, "Sheep"),
    RABBIT(EntityType.RABBIT, Material.RABBIT_FOOT, "Rabbit"),
    POLAR_BEAR(EntityType.POLAR_BEAR, Material.SNOWBALL, "Polar Bear"),
    AXOLOTL(EntityType.AXOLOTL, Material.WATER_BUCKET, "Axolotl"),
    GOAT(EntityType.GOAT, Material.GOAT_HORN, "Goat"),
    FROG(EntityType.FROG, Material.LILY_PAD, "Frog"),
    ALLAY(EntityType.ALLAY, Material.AMETHYST_SHARD, "Allay"),
    TADPOLE(EntityType.TADPOLE, Material.WATER_BUCKET, "Tadpole"),
    WARDEN(EntityType.WARDEN, Material.SCULK, "Warden"),
    CAMEL(EntityType.CAMEL, Material.SAND, "Camel"),
    SNIFFER(EntityType.SNIFFER, Material.TORCHFLOWER, "Sniffer"),
    ARMADILLO(EntityType.ARMADILLO, Material.ARMADILLO_SCUTE, "Armadillo");

    private final EntityType entityType;
    private final Material material;
    private final String displayName;

    SpawnerType(EntityType entityType, Material material, String displayName) {
        this.entityType = entityType;
        this.material = material;
        this.displayName = displayName;
    }

    public EntityType getEntityType() {
        return entityType;
    }

    public Material getMaterial() {
        return material;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Material getHeadMaterial() {
        switch (this) {
            case SKELETON:
                return Material.SKELETON_SKULL;
            case ZOMBIE:
                return Material.ZOMBIE_HEAD;
            case CREEPER:
                return Material.CREEPER_HEAD;
            case WITHER_SKELETON:
                return Material.WITHER_SKELETON_SKULL;
            default:
                return material;
        }
    }

    public java.util.List<Material> getPossibleDrops() {
        java.util.List<Material> drops = new java.util.ArrayList<>();
        switch (this) {
            case SKELETON:
            case STRAY:
                drops.add(Material.BONE);
                drops.add(Material.ARROW);
                break;
            case ZOMBIE:
            case ZOGLIN:
            case ZOMBIE_HORSE:
            case HUSK:
                drops.add(Material.ROTTEN_FLESH);
                break;
            case SPIDER:
            case CAVE_SPIDER:
                drops.add(Material.STRING);
                drops.add(Material.SPIDER_EYE);
                break;
            case CREEPER:
                drops.add(Material.GUNPOWDER);
                break;
            case ENDERMAN:
            case ENDERMITE:
                drops.add(Material.ENDER_PEARL);
                break;
            case BLAZE:
                drops.add(Material.BLAZE_ROD);
                break;
            case GHAST:
                drops.add(Material.GHAST_TEAR);
                break;
            case WITCH:
                drops.add(Material.STICK);
                drops.add(Material.GLASS_BOTTLE);
                break;
            case PIGLIN:
                drops.add(Material.GOLD_INGOT);
                break;
            case PIGLIN_BRUTE:
                drops.add(Material.GOLDEN_SWORD);
                break;
            case HOGLIN:
                drops.add(Material.COOKED_PORKCHOP);
                break;
            case STRIDER:
            case CAT:
                drops.add(Material.STRING);
                break;
            case MAGMA_CUBE:
                drops.add(Material.MAGMA_CREAM);
                break;
            case SLIME:
                drops.add(Material.SLIME_BALL);
                break;
            case PHANTOM:
                drops.add(Material.PHANTOM_MEMBRANE);
                break;
            case DROWNED:
                drops.add(Material.ROTTEN_FLESH);
                drops.add(Material.TRIDENT);
                break;
            case WITHER_SKELETON:
                drops.add(Material.BONE);
                drops.add(Material.COAL);
                break;
            case PILLAGER:
                drops.add(Material.ARROW);
                break;
            case VINDICATOR:
                drops.add(Material.EMERALD);
                break;
            case EVOKER:
                drops.add(Material.TOTEM_OF_UNDYING);
                break;
            case RAVAGER:
                drops.add(Material.SADDLE);
                break;
            case GUARDIAN:
                drops.add(Material.PRISMARINE_SHARD);
                break;
            case ELDER_GUARDIAN:
                drops.add(Material.PRISMARINE_CRYSTALS);
                break;
            case SHULKER:
                drops.add(Material.SHULKER_SHELL);
                break;
            case IRON_GOLEM:
                drops.add(Material.IRON_INGOT);
                break;
            case SNOW_GOLEM:
            case POLAR_BEAR:
                drops.add(Material.SNOWBALL);
                break;
            case HORSE:
            case DONKEY:
            case MULE:
            case LLAMA:
            case TRADER_LLAMA:
            case COW:
                drops.add(Material.LEATHER);
                if (this == COW) drops.add(Material.BEEF);
                break;
            case SKELETON_HORSE:
                drops.add(Material.BONE);
                break;
            case PARROT:
            case CHICKEN:
                drops.add(Material.FEATHER);
                if (this == CHICKEN) drops.add(Material.CHICKEN);
                break;
            case COD:
            case DOLPHIN:
                drops.add(Material.COD);
                break;
            case SALMON:
                drops.add(Material.SALMON);
                break;
            case PUFFERFISH:
                drops.add(Material.PUFFERFISH);
                break;
            case TROPICAL_FISH:
                drops.add(Material.TROPICAL_FISH);
                break;
            case SQUID:
                drops.add(Material.INK_SAC);
                break;
            case GLOW_SQUID:
                drops.add(Material.GLOW_INK_SAC);
                break;
            case TURTLE:
                drops.add(Material.SEAGRASS);
                break;
            case PANDA:
                drops.add(Material.BAMBOO);
                break;
            case BEE:
                drops.add(Material.HONEYCOMB);
                break;
            case PIG:
                drops.add(Material.PORKCHOP);
                break;
            case SHEEP:
                drops.add(Material.WHITE_WOOL);
                drops.add(Material.MUTTON);
                break;
            case RABBIT:
                drops.add(Material.RABBIT);
                drops.add(Material.RABBIT_FOOT);
                break;
            case WARDEN:
                drops.add(Material.SCULK);
                break;
            default:
                if (material != null && material != Material.AIR) {
                    drops.add(material);
                }
                break;
        }
        return drops;
    }

    public static SpawnerType fromString(String name) {
        for (SpawnerType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
