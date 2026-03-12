package com.zaext.simple.item.oilmod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.fabricmc.fabric.api.registry.FuelRegistryEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.EnchantedCountIncreaseFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class OilMod implements ModInitializer {
	public static final String MOD_ID = "oilmod";

	// register the oil bottle item
	public static final Item OIL_BOTTLE = registerItem("oil_bottle", OilBottleItem::new, new Item.Properties().stacksTo(16));

	private static Item registerItem(String path, Function<Item.Properties, Item> factory, Item.Properties properties) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);

		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	// register oil fat item
	public static final Item OIL_FAT = registerItem("oil_fat", Item::new, new Item.Properties().stacksTo(64));

	// register the oil bottle entity type

	public static final EntityType<OilBottleEntity> OIL_BOTTLE_ENTITY_TYPE = registerEntity("oil_bottle_entity", EntityType.Builder.<OilBottleEntity>of(OilBottleEntity::new, MobCategory.MISC).sized(0.25f, 0.25f));

	private static <T extends net.minecraft.world.entity.Entity> EntityType<T> registerEntity(String path, EntityType.Builder<T> builder) {
		Identifier id = Identifier.fromNamespaceAndPath(MOD_ID, path);
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);

		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	// register oil soaked effect
	public static final Holder<MobEffect> OIL_SOAKED = Registry.registerForHolder(BuiltInRegistries.MOB_EFFECT, Identifier.fromNamespaceAndPath(MOD_ID, "oil_soaked"), new OilSoakedEffect());

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Initializing OilBottleMod");
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES).register(content -> content.accept(OIL_BOTTLE));
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(content -> content.accept(OIL_FAT));

		// add dispenser behavior
		DispenserBlock.registerProjectileBehavior(OIL_BOTTLE);

		// add oil fat to fuel reg
		FuelRegistryEvents.BUILD.register((builder, context) -> builder.add(OIL_FAT, 600));


		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin() &&
					(isResourceKeyMatching(key, EntityType.PIG) ||
					 isResourceKeyMatching(key, EntityType.SHEEP) ||
					 isResourceKeyMatching(key, EntityType.COW))) {

				LootPool.Builder poolBuilder = LootPool.lootPool()
						.setRolls(ConstantValue.exactly(1f))
						.add(LootItem.lootTableItem(OIL_FAT))
						.apply(SetItemCountFunction.setCount(UniformGenerator.between(1.0f, 2.0f)))
						.apply(EnchantedCountIncreaseFunction.lootingMultiplier(registries, UniformGenerator.between(1.0f, 2.0f)));

				tableBuilder.withPool(poolBuilder);

			}
		});
	}

	private boolean isResourceKeyMatching(ResourceKey<LootTable> key, EntityType<?> type) {
		return type.getDefaultLootTable().map(key::equals).orElse(false);
	}
}