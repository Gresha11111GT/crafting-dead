/*
 * Crafting Dead
 * Copyright (C) 2021  NexusNode LTD
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.craftingdead.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.craftingdead.core.capability.SerializableCapabilityProvider;
import com.craftingdead.core.client.ClientDist;
import com.craftingdead.core.data.ModItemTagsProvider;
import com.craftingdead.core.data.ModRecipeProvider;
import com.craftingdead.core.event.CombatPickupEvent;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.SyncLivingMessage;
import com.craftingdead.core.particle.ModParticleTypes;
import com.craftingdead.core.server.ServerDist;
import com.craftingdead.core.sounds.ModSoundEvents;
import com.craftingdead.core.world.action.ActionTypes;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.ModEntityTypes;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.inventory.ModMenuTypes;
import com.craftingdead.core.world.inventory.storage.Storage;
import com.craftingdead.core.world.item.ArbitraryTooltips;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.clothing.Clothing;
import com.craftingdead.core.world.item.combatslot.CombatSlot;
import com.craftingdead.core.world.item.combatslot.CombatSlotProvider;
import com.craftingdead.core.world.item.crafting.ModRecipeSerializers;
import com.craftingdead.core.world.item.enchantment.ModEnchantments;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.ammoprovider.AmmoProviderTypes;
import com.craftingdead.core.world.item.gun.attachment.Attachments;
import com.craftingdead.core.world.item.gun.magazine.Magazine;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.hat.Hat;
import com.craftingdead.core.world.item.scope.Scope;
import io.netty.buffer.Unpooled;
import net.minecraft.data.DataGenerator;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.brewing.BrewingRecipeRegistry;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.JarVersionLookupHandler;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.network.PacketDistributor;

@Mod(CraftingDead.ID)
public class CraftingDead {

  public static final String ID = "craftingdead";

  public static final String VERSION =
      JarVersionLookupHandler.getImplementationVersion(CraftingDead.class).orElse("[version]");

  /**
   * Logger.
   */
  private static final Logger logger = LogManager.getLogger();

  /**
   * Singleton.
   */
  private static CraftingDead instance;

  /**
   * Mod distribution.
   */
  private final ModDist modDist;

  public CraftingDead() {
    instance = this;

    this.modDist = DistExecutor.unsafeRunForDist(() -> ClientDist::new, () -> ServerDist::new);

    final var modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
    modEventBus.addListener(this::handleCommonSetup);
    modEventBus.addListener(this::handleGatherData);
    modEventBus.addListener(this::handleRegisterCapabilities);

    ModEntityTypes.ENTITY_TYPES.register(modEventBus);
    ModItems.ITEMS.register(modEventBus);
    ModSoundEvents.SOUND_EVENTS.register(modEventBus);
    ModMenuTypes.MENUS.register(modEventBus);
    ModMobEffects.MOB_EFFECTS.register(modEventBus);
    ModEnchantments.ENCHANTMENTS.register(modEventBus);
    ModParticleTypes.PARTICLE_TYPES.register(modEventBus);
    ModRecipeSerializers.RECIPE_SERIALIZERS.register(modEventBus);

    // Custom registries
    ActionTypes.ACTION_TYPES.register(modEventBus);
    AmmoProviderTypes.AMMO_PROVIDER_TYPES.register(modEventBus);
    Attachments.ATTACHMENTS.register(modEventBus);

    modEventBus.addGenericListener(Item.class, ArbitraryTooltips::registerAll);

    MinecraftForge.EVENT_BUS.register(this);
  }

  public ModDist getModDist() {
    return this.modDist;
  }

  public ClientDist getClientDist() {
    if (this.modDist instanceof ClientDist clientDist) {
      return clientDist;
    }
    throw new IllegalStateException("Accessing client dist on wrong side");
  }

  public static CraftingDead getInstance() {
    return instance;
  }

  // ================================================================================
  // Mod Events
  // ================================================================================

  private void handleCommonSetup(FMLCommonSetupEvent event) {
    logger.info("Starting Crafting Dead, version {}", VERSION);
    NetworkChannel.loadChannels();
    event.enqueueWork(() -> BrewingRecipeRegistry.addRecipe(Ingredient.of(ModItems.SYRINGE.get()),
        Ingredient.of(Items.REDSTONE),
        new ItemStack(ModItems.ADRENALINE_SYRINGE.get())));
  }

  private void handleGatherData(GatherDataEvent event) {
    DataGenerator dataGenerator = event.getGenerator();
    if (event.includeServer()) {
      dataGenerator.addProvider(new ModItemTagsProvider(dataGenerator,
          new ForgeBlockTagsProvider(dataGenerator, event.getExistingFileHelper()),
          event.getExistingFileHelper()));
      dataGenerator.addProvider(new ModRecipeProvider(dataGenerator));
    }
  }

  private void handleRegisterCapabilities(RegisterCapabilitiesEvent event) {
    event.register(LivingExtension.class);
    event.register(Storage.class);
    event.register(Clothing.class);
    event.register(Hat.class);
    event.register(CombatSlotProvider.class);
    event.register(Gun.class);
    event.register(Magazine.class);
    event.register(Scope.class);
    event.register(Paint.class);
  }

  // ================================================================================
  // Common Forge Events
  // ================================================================================

  @SubscribeEvent
  public void handleEntityItemPickup(EntityItemPickupEvent event) {
    event.getPlayer().getCapability(LivingExtension.CAPABILITY)
        .<PlayerExtension<?>>cast()
        .filter(PlayerExtension::isCombatModeEnabled).ifPresent(living -> {
          final ItemStack itemStack = event.getItem().getItem();
          CombatSlot combatSlot = CombatSlot.getSlotType(itemStack).orElse(null);
          CombatPickupEvent combatPickupEvent = new CombatPickupEvent(itemStack, combatSlot);
          if (MinecraftForge.EVENT_BUS.post(combatPickupEvent)) {
            event.setCanceled(true);
          } else if (combatSlot != null) {
            if (combatSlot.addToInventory(itemStack, event.getPlayer().getInventory(), false)) {
              // Allows normal processing of item pickup but prevents item being added to inventory
              // because we've already added it.
              event.setResult(Event.Result.ALLOW);
            } else {
              event.setCanceled(true);
            }
          }
        });
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingSetTarget(LivingSetAttackTargetEvent event) {
    if (event.getTarget() != null && event.getEntityLiving() instanceof Mob) {
      Mob mobEntity = (Mob) event.getEntityLiving();
      if (mobEntity.hasEffect(ModMobEffects.FLASH_BLINDNESS.get())) {
        mobEntity.setTarget(null);
      }
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDeath(LivingDeathEvent event) {
    if (event.getEntity()
        .getCapability(LivingExtension.CAPABILITY)
        .map(living -> living.handleDeath(event.getSource()))
        .orElse(false)
        || (event.getSource().getEntity() != null && event
            .getSource()
            .getEntity()
            .getCapability(LivingExtension.CAPABILITY)
            .map(living -> living.handleKill(event.getEntity()))
            .orElse(false))) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDrops(LivingDropsEvent event) {
    event.getEntity()
        .getCapability(LivingExtension.CAPABILITY)
        .ifPresent(living -> event.setCanceled(
            living.handleDeathLoot(event.getSource(), event.getDrops())));
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingAttack(LivingAttackEvent event) {
    event.getEntity()
        .getCapability(LivingExtension.CAPABILITY)
        .ifPresent(living -> event.setCanceled(
            living.handleHurt(event.getSource(), event.getAmount())));
  }

  @SubscribeEvent(priority = EventPriority.LOWEST)
  public void handleLivingDamage(LivingDamageEvent event) {
    event.getEntity()
        .getCapability(LivingExtension.CAPABILITY)
        .ifPresent(living -> event.setAmount(
            living.handleDamaged(event.getSource(), event.getAmount())));
  }

  @SubscribeEvent
  public void handlePlayerClone(PlayerEvent.Clone event) {
    event.getOriginal().reviveCaps();
    PlayerExtension.getOrThrow(event.getPlayer())
        .copyFrom(PlayerExtension.getOrThrow(event.getOriginal()), event.isWasDeath());
  }

  @SubscribeEvent
  public void handleLivingUpdate(LivingUpdateEvent event) {
    event.getEntityLiving().getCapability(LivingExtension.CAPABILITY).ifPresent(living -> {
      living.tick();
      if (!living.getLevel().isClientSide() && living.requiresSync()) {
        FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
        living.encode(data, false);
        NetworkChannel.PLAY.getSimpleChannel().send(
            PacketDistributor.TRACKING_ENTITY_AND_SELF.with(living::getEntity),
            new SyncLivingMessage(living.getEntity().getId(), data));
      }
    });
  }

  @SubscribeEvent
  public void handlePlayerTick(TickEvent.PlayerTickEvent event) {
    switch (event.phase) {
      case END:
        event.player.getCapability(LivingExtension.CAPABILITY)
            .filter(living -> living instanceof PlayerExtension)
            .map(living -> (PlayerExtension<?>) living)
            .ifPresent(PlayerExtension::playerTick);
        break;
      default:
        break;
    }
  }

  @SubscribeEvent
  public void handleAttachEntityCapabilities(AttachCapabilitiesEvent<Entity> event) {
    if (event.getObject() instanceof LivingEntity) {
      LivingExtension<?, ?> living = event.getObject() instanceof Player
          ? PlayerExtension.create((Player) event.getObject())
          : LivingExtension.create((LivingEntity) event.getObject());
      event.addCapability(LivingExtension.CAPABILITY_KEY, new SerializableCapabilityProvider<>(
          LazyOptional.of(() -> living), () -> LivingExtension.CAPABILITY, CompoundTag::new));
      living.load();
    }
  }

  @SubscribeEvent
  public void handlePlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
    FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
    PlayerExtension.getOrThrow(event.getPlayer()).encode(data, true);
    NetworkChannel.PLAY.getSimpleChannel().send(
        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(event::getPlayer),
        new SyncLivingMessage(event.getPlayer().getId(), data));
  }

  @SubscribeEvent
  public void handlePlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
    startTracking(event.getPlayer(), (ServerPlayer) event.getPlayer());
  }

  @SubscribeEvent
  public void handlePlayerStartTracking(PlayerEvent.StartTracking event) {
    startTracking(event.getTarget(), (ServerPlayer) event.getPlayer());
  }

  private static void startTracking(Entity targetEntity, ServerPlayer playerEntity) {
    targetEntity.getCapability(LivingExtension.CAPABILITY).ifPresent(trackedLiving -> {
      trackedLiving.handleStartTracking(playerEntity);
      FriendlyByteBuf data = new FriendlyByteBuf(Unpooled.buffer());
      trackedLiving.encode(data, true);
      NetworkChannel.PLAY.getSimpleChannel().send(
          PacketDistributor.PLAYER.with(() -> playerEntity),
          new SyncLivingMessage(trackedLiving.getEntity().getId(), data));
    });
  }
}
