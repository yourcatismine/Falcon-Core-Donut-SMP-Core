package com.prismcore.survival.spawners.mob;

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

    public static SpawnerType fromString(String name) {
        for (SpawnerType type : values()) {
            if (type.name().equalsIgnoreCase(name) || type.getDisplayName().equalsIgnoreCase(name)) {
                return type;
            }
        }
        return null;
    }
}
