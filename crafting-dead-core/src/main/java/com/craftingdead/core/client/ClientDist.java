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

package com.craftingdead.core.client;

import java.util.Collection;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.glfw.GLFW;
import com.craftingdead.core.CraftingDead;
import com.craftingdead.core.ModDist;
import com.craftingdead.core.client.crosshair.CrosshairManager;
import com.craftingdead.core.client.gui.IngameGui;
import com.craftingdead.core.client.gui.screen.inventory.EquipmentScreen;
import com.craftingdead.core.client.gui.screen.inventory.GenericContainerScreen;
import com.craftingdead.core.client.model.C4ExplosiveModel;
import com.craftingdead.core.client.model.CylinderGrenadeModel;
import com.craftingdead.core.client.model.FragGrenadeModel;
import com.craftingdead.core.client.model.SlimGrenadeModel;
import com.craftingdead.core.client.model.geom.ModModelLayers;
import com.craftingdead.core.client.particle.GrenadeSmokeParticle;
import com.craftingdead.core.client.particle.RGBFlashParticle;
import com.craftingdead.core.client.renderer.CameraManager;
import com.craftingdead.core.client.renderer.entity.grenade.C4ExplosiveRenderer;
import com.craftingdead.core.client.renderer.entity.grenade.CylinderGrenadeRenderer;
import com.craftingdead.core.client.renderer.entity.grenade.FragGrenadeRenderer;
import com.craftingdead.core.client.renderer.entity.grenade.SlimGrenadeRenderer;
import com.craftingdead.core.client.renderer.entity.layers.ClothingLayer;
import com.craftingdead.core.client.renderer.entity.layers.EquipmentLayer;
import com.craftingdead.core.client.renderer.entity.layers.ParachuteLayer;
import com.craftingdead.core.client.renderer.item.CustomItemRenderer;
import com.craftingdead.core.client.renderer.item.GunRenderer;
import com.craftingdead.core.client.renderer.item.ItemRendererManager;
import com.craftingdead.core.client.sounds.EffectsManager;
import com.craftingdead.core.client.tutorial.ModTutorialStepInstance;
import com.craftingdead.core.client.tutorial.ModTutorialSteps;
import com.craftingdead.core.client.util.RenderUtil;
import com.craftingdead.core.event.RenderArmClothingEvent;
import com.craftingdead.core.network.NetworkChannel;
import com.craftingdead.core.network.message.play.OpenEquipmentMenuMessage;
import com.craftingdead.core.particle.ModParticleTypes;
import com.craftingdead.core.util.MutableVector2f;
import com.craftingdead.core.world.effect.ModMobEffects;
import com.craftingdead.core.world.entity.ModEntityTypes;
import com.craftingdead.core.world.entity.extension.LivingExtension;
import com.craftingdead.core.world.entity.extension.PlayerExtension;
import com.craftingdead.core.world.entity.grenade.FlashGrenadeEntity;
import com.craftingdead.core.world.inventory.ModEquipmentSlotType;
import com.craftingdead.core.world.inventory.ModMenuTypes;
import com.craftingdead.core.world.item.ArbitraryTooltips;
import com.craftingdead.core.world.item.ArbitraryTooltips.TooltipFunction;
import com.craftingdead.core.world.item.ModItems;
import com.craftingdead.core.world.item.RegisterGunColour;
import com.craftingdead.core.world.item.clothing.Clothing;
import com.craftingdead.core.world.item.gun.Gun;
import com.craftingdead.core.world.item.gun.GunItem;
import com.craftingdead.core.world.item.gun.skin.Paint;
import com.craftingdead.core.world.item.gun.skin.Skins;
import com.craftingdead.core.world.item.scope.Scope;
import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.math.Vector3f;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.HumanoidModel.ArmPose;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.tutorial.Tutorial;
import net.minecraft.client.tutorial.TutorialSteps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ColorHandlerEvent;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.client.event.FOVModifierEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ParticleFactoryRegisterEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.client.event.ScreenOpenEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

public class ClientDist implements ModDist {

  public static final KeyMapping RELOAD =
      new KeyMapping("key.reload", GLFW.GLFW_KEY_R, "key.categories.gameplay");
  public static final KeyMapping REMOVE_MAGAZINE =
      new KeyMapping("key.remove_magazine", GLFW.GLFW_KEY_J, "key.categories.gameplay");
  public static final KeyMapping TOGGLE_FIRE_MODE =
      new KeyMapping("key.toggle_fire_mode", GLFW.GLFW_KEY_V, "key.categories.gameplay");
  public static final KeyMapping OPEN_EQUIPMENT_MENU =
      new KeyMapping("key.equipment_menu", GLFW.GLFW_KEY_Z, "key.categories.inventory");
  public static final KeyMapping TOGGLE_COMBAT_MODE =
      new KeyMapping("key.toggle_combat_mode", KeyConflictContext.UNIVERSAL, KeyModifier.ALT,
          InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_C), "key.categories.inventory");

  public static final ClientConfig clientConfig;
  public static final ForgeConfigSpec clientConfigSpec;

  static {
    final Pair<ClientConfig, ForgeConfigSpec> clientConfigPair =
        new ForgeConfigSpec.Builder().configure(ClientConfig::new);
    clientConfigSpec = clientConfigPair.getRight();
    clientConfig = clientConfigPair.getLeft();
  }

  private static final ResourceLocation ADRENALINE_SHADER =
      new ResourceLocation(CraftingDead.ID, "shaders/post/adrenaline.json");

  private static final Vector3f MUTABLE_CAMERA_ROTATIONS = new Vector3f();
  private static final MutableVector2f FOV = new MutableVector2f();

  private static final int DOUBLE_CLICK_DURATION = 500;

  private final Minecraft minecraft;

  private final CrosshairManager crosshairManager;

  private final IngameGui ingameGui;

  private final ItemRendererManager itemRendererManager;

  private final CameraManager cameraManager;

  private EffectsManager effectsManager;

  private TutorialSteps lastTutorialStep;

  private long adrenalineShaderStartTime = 0L;

  private float lastTime = 0F;

  private boolean wasAdrenalineActive;

  private boolean wasSneaking;
  private long lastSneakPressTime;

  private float lastPitch;
  private float lastYaw;
  private float lastRoll;

  public ClientDist() {
    final var modBus = FMLJavaModLoadingContext.get().getModEventBus();
    modBus.addListener(this::handleClientSetup);
    modBus.addListener(this::handleParticleFactoryRegisterEvent);
    modBus.addListener(this::handleItemColor);
    modBus.addListener(this::handleTextureStitch);
    modBus.addListener(this::handleSoundLoad);
    modBus.addListener(this::handleConfigReloading);
    modBus.addListener(this::handleEntityRenderers);
    modBus.addListener(this::handleEntityRenderersAddLayers);
    modBus.addListener(this::handleEntityRenderersLayerDefinitions);
    modBus.addListener(this::handleRegisterClientReloadListeners);

    MinecraftForge.EVENT_BUS.register(this);
    ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientConfigSpec);

    this.minecraft = Minecraft.getInstance();
    this.crosshairManager = new CrosshairManager();
    this.itemRendererManager = new ItemRendererManager();

    this.ingameGui =
        new IngameGui(this.minecraft, this, new ResourceLocation(clientConfig.crosshair.get()));
    this.cameraManager = new CameraManager();
  }

  public void setTutorialStep(ModTutorialSteps step) {
    clientConfig.tutorialStep.set(step);
    Tutorial tutorial = this.minecraft.getTutorial();
    tutorial.setStep(TutorialSteps.NONE);
    tutorial.instance = step.create(this);
  }

  public CrosshairManager getCrosshairManager() {
    return this.crosshairManager;
  }

  public IngameGui getIngameGui() {
    return this.ingameGui;
  }

  public CameraManager getCameraManager() {
    return this.cameraManager;
  }

  public ItemRendererManager getItemRendererManager() {
    return this.itemRendererManager;
  }

  /**
   * Get the {@link Minecraft} instance. If accessing {@link Minecraft} from a common class
   * (contains both client and server code) don't access fields directly from {@link Minecraft} as
   * it will cause class loading problems. To safely access {@link ClientPlayerEntity} in a
   * multi-sided environment, use {@link #getPlayerExtension()}.
   * 
   * @return {@link Minecraft}
   */
  public Minecraft getMinecraft() {
    return this.minecraft;
  }

  public Optional<PlayerExtension<LocalPlayer>> getPlayerExtension() {
    return Optional.ofNullable(this.minecraft.player)
        .flatMap(player -> player.getCapability(LivingExtension.CAPABILITY).resolve())
        .filter(PlayerExtension.class::isInstance)
        .map(PlayerExtension.class::cast);
  }

  public boolean isRightMouseDown() {
    return this.minecraft.options.keyUse.isDown();
  }

  public boolean isLocalPlayer(Entity entity) {
    return entity == this.minecraft.player;
  }

  public void handleHit(Vec3 hitPos, boolean dead) {
    clientConfig.hitMarkerMode.get().createHitMarker(hitPos, dead)
        .ifPresent(this.ingameGui::displayHitMarker);
    if (dead && ClientDist.clientConfig.killSoundEnabled.get()) {
      // Plays a sound that follows the player
      SoundEvent soundEvent = ForgeRegistries.SOUND_EVENTS.getValue(
          new ResourceLocation(ClientDist.clientConfig.killSound.get()));
      if (soundEvent != null) {
        this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(soundEvent, 5.0F, 1.5F));
      }
    }
  }

  @Nullable
  public PlayerExtension<AbstractClientPlayer> getCameraPlayer() {
    return this.minecraft.getCameraEntity() instanceof AbstractClientPlayer
        ? this.minecraft.getCameraEntity().getCapability(LivingExtension.CAPABILITY)
            .<PlayerExtension<AbstractClientPlayer>>cast()
            .orElse(null)
        : null;
  }

  // ================================================================================
  // Mod Events
  // ================================================================================

  private void handleRegisterClientReloadListeners(RegisterClientReloadListenersEvent event) {
    event.registerReloadListener(this.crosshairManager);
    event.registerReloadListener(this.itemRendererManager);
  }

  /**
   * This has to be handled on the mod bus and forge bus.
   */
  @SubscribeEvent
  public void handleSoundLoad(SoundLoadEvent event) {
    this.effectsManager = new EffectsManager(event.getEngine());
  }

  private void handleConfigReloading(ModConfigEvent.Reloading event) {
    if (event.getConfig().getSpec() == clientConfigSpec) {
      this.ingameGui.setCrosshairLocation(new ResourceLocation(clientConfig.crosshair.get()));
    }
  }

  private void handleClientSetup(FMLClientSetupEvent event) {
    ItemProperties.registerGeneric(new ResourceLocation("wearing"),
        (itemStack, level, entity, value) -> entity.getCapability(LivingExtension.CAPABILITY)
            .filter(living -> living.getItemHandler()
                .getStackInSlot(ModEquipmentSlotType.HAT.getIndex()) == itemStack)
            .map(__ -> 1.0F)
            .orElse(0.0F));

    StartupMessageManager.addModMessage("Registering tooltips");

    ArbitraryTooltips.registerTooltip(ModItems.SCUBA_MASK,
        (stack, level, tooltipFlags) -> new TranslatableComponent(
            "item_lore.clothing_item.water_breathing")
                .withStyle(ChatFormatting.GRAY));

    ArbitraryTooltips.registerTooltip(ModItems.SCUBA_CLOTHING,
        (stack, level, tooltipFlags) -> new TranslatableComponent(
            "item_lore.clothing_item.water_speed")
                .withStyle(ChatFormatting.GRAY));

    MenuScreens.register(ModMenuTypes.EQUIPMENT.get(), EquipmentScreen::new);
    MenuScreens.register(ModMenuTypes.VEST.get(), GenericContainerScreen::new);

    ClientRegistry.registerKeyBinding(TOGGLE_FIRE_MODE);
    ClientRegistry.registerKeyBinding(RELOAD);
    ClientRegistry.registerKeyBinding(REMOVE_MAGAZINE);
    ClientRegistry.registerKeyBinding(OPEN_EQUIPMENT_MENU);
    ClientRegistry.registerKeyBinding(TOGGLE_COMBAT_MODE);
  }

  private void handleEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
    event.registerEntityRenderer(ModEntityTypes.C4_EXPLOSIVE.get(),
        C4ExplosiveRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.FIRE_GRENADE.get(),
        CylinderGrenadeRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.FRAG_GRENADE.get(),
        FragGrenadeRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.DECOY_GRENADE.get(),
        SlimGrenadeRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.SMOKE_GRENADE.get(),
        CylinderGrenadeRenderer::new);
    event.registerEntityRenderer(ModEntityTypes.FLASH_GRENADE.get(),
        SlimGrenadeRenderer::new);
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private void handleEntityRenderersAddLayers(EntityRenderersEvent.AddLayers event) {
    for (var skin : event.getSkins()) {
      var renderer = event.getSkin(skin);
      renderer.addLayer(new ParachuteLayer(renderer, event.getEntityModels()));
      renderer.addLayer(new ClothingLayer(renderer));
      renderer.addLayer(new EquipmentLayer.Builder()
          .withRenderer(renderer)
          .withSlot(ModEquipmentSlotType.MELEE)
          .withCrouchingOrientation(true)
          .build());
      renderer.addLayer(new EquipmentLayer.Builder()
          .withRenderer(renderer)
          .withSlot(ModEquipmentSlotType.VEST)
          .withCrouchingOrientation(true)
          .build());
      renderer.addLayer(new EquipmentLayer.Builder()
          .withRenderer(renderer)
          .withSlot(ModEquipmentSlotType.HAT)
          .withHeadOrientation(true)
          .withArbitraryTransformation(
              new Transformation(null, null, new Vector3f(-1F, -1F, 1F), null))
          .build());
      renderer.addLayer(new EquipmentLayer.Builder()
          .withRenderer(renderer)
          .withSlot(ModEquipmentSlotType.GUN)
          .withCrouchingOrientation(true)
          .build());
    }
  }

  private void handleEntityRenderersLayerDefinitions(
      EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(ModModelLayers.MUZZLE_FLASH,
        GunRenderer::createMuzzleFlashBodyLayer);
    event.registerLayerDefinition(ModModelLayers.PARACHUTE,
        ParachuteLayer::createParachuteBodyLayer);
    event.registerLayerDefinition(ModModelLayers.C4_EXPLOSIVE,
        C4ExplosiveModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.CYLINDER_GRENADE,
        CylinderGrenadeModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.FRAG_GRENADE,
        FragGrenadeModel::createBodyLayer);
    event.registerLayerDefinition(ModModelLayers.SLIM_GRENADE,
        SlimGrenadeModel::createBodyLayer);
  }

  private void handleParticleFactoryRegisterEvent(ParticleFactoryRegisterEvent event) {
    ParticleEngine particleEngine = this.minecraft.particleEngine;
    particleEngine.register(ModParticleTypes.GRENADE_SMOKE.get(),
        GrenadeSmokeParticle.Factory::new);
    particleEngine.register(ModParticleTypes.RGB_FLASH.get(), RGBFlashParticle.Factory::new);
  }

  private void handleItemColor(ColorHandlerEvent.Item event) {
    ItemColor gunColour =
        (itemStack, tintIndex) -> itemStack.getCapability(Gun.CAPABILITY)
            .resolve()
            .flatMap(gun -> gun.getPaintStack().getCapability(Paint.CAPABILITY).resolve())
            .stream()
            .flatMapToInt(paint -> paint.getColor().stream())
            .findAny()
            .orElse(0xFFFFFFFF);
    ForgeRegistries.ITEMS.getValues().stream()
        .filter(item -> item.getClass().isAnnotationPresent(RegisterGunColour.class))
        .forEach(item -> event.getItemColors().register(gunColour, item));
  }

  private void handleTextureStitch(TextureStitchEvent.Pre event) {
    this.itemRendererManager.getTextures(event.getAtlas().location()).forEach(event::addSprite);
    if (event.getAtlas().location().equals(InventoryMenu.BLOCK_ATLAS)) {
      Skins.REGISTRY.stream()
          .flatMap(skin -> skin.getAcceptedGuns().stream().map(skin::getTextureLocation))
          .forEach(event::addSprite);
    }
  }

  // ================================================================================
  // Client Forge Events
  // ================================================================================

  @SubscribeEvent
  public void handleTooltipEvent(ItemTooltipEvent event) {
    Collection<TooltipFunction> functions =
        ArbitraryTooltips.getFunctions(event.getItemStack().getItem());
    int lineIndex = 1;

    // Applies the arbitrary tooltip
    for (TooltipFunction function : functions) {
      Level world = event.getEntity() != null ? event.getEntity().level : null;
      Component tooltip =
          function.createTooltip(event.getItemStack(), world, event.getFlags());
      if (tooltip != null) {
        event.getToolTip().add(lineIndex++, tooltip);
      }
    }
  }

  @SubscribeEvent
  public void handleClientTick(TickEvent.ClientTickEvent event) {
    switch (event.phase) {
      case START:
        this.lastTime = (float) Math.ceil(this.lastTime);
        PlayerExtension<LocalPlayer> player = this.getPlayerExtension().orElse(null);
        if (player != null) {
          Gun gun = player.getMainHandGun().orElse(null);

          boolean worldFocused = !this.minecraft.isPaused() && this.minecraft.getOverlay() == null
              && (this.minecraft.screen == null);

          this.cameraManager.tick();

          if (!worldFocused || player.getEntity().isSpectator()) {
            // Stop gun actions if world not focused.
            if (gun != null) {
              if (gun.isTriggerPressed()) {
                gun.setTriggerPressed(player, false, true);
              }
              if (gun.isPerformingSecondaryAction()) {
                gun.setPerformingSecondaryAction(player, false, true);
              }
            }
            return;
          }

          while (TOGGLE_COMBAT_MODE.consumeClick()) {
            player.setCombatModeEnabled(!player.isCombatModeEnabled());
          }

          // Update gun input
          if (gun != null) {
            while (TOGGLE_FIRE_MODE.consumeClick()) {
              gun.toggleFireMode(player, true);
            }
            while (RELOAD.consumeClick()) {
              gun.getAmmoProvider().reload(player);
            }
            while (REMOVE_MAGAZINE.consumeClick()) {
              gun.getAmmoProvider().unload(player);
            }
          }

          // Update crouching
          if (this.minecraft.player.isShiftKeyDown() != this.wasSneaking) {
            if (this.minecraft.player.isShiftKeyDown()) {
              final long currentTime = Util.getMillis();
              if (currentTime - this.lastSneakPressTime <= DOUBLE_CLICK_DURATION) {
                player.setCrouching(true, true);
              }
              this.lastSneakPressTime = Util.getMillis();
            } else {
              player.setCrouching(false, true);
            }
            this.wasSneaking = this.minecraft.player.isShiftKeyDown();
          }

          // Update tutorial
          while (OPEN_EQUIPMENT_MENU.consumeClick()) {
            NetworkChannel.PLAY.getSimpleChannel().sendToServer(new OpenEquipmentMenuMessage());
            if (this.minecraft.getTutorial().instance instanceof ModTutorialStepInstance) {
              ((ModTutorialStepInstance) this.minecraft.getTutorial().instance).openEquipmentMenu();
            }
          }
          TutorialSteps currentTutorialStep = this.minecraft.options.tutorialStep;
          if (this.lastTutorialStep != currentTutorialStep) {
            if (currentTutorialStep == TutorialSteps.NONE) {
              this.setTutorialStep(clientConfig.tutorialStep.get());
            }
            this.lastTutorialStep = currentTutorialStep;
          }

          // Update adrenaline effects
          if (this.minecraft.player.hasEffect(ModMobEffects.ADRENALINE.get())) {
            this.wasAdrenalineActive = true;
            this.effectsManager.setHighpassLevels(1.0F, 0.015F);
            this.effectsManager.setDirectHighpassForAll();
          } else if (this.wasAdrenalineActive) {
            this.wasAdrenalineActive = false;
            this.effectsManager.removeFilterForAll();
          }

        }
        break;
      default:
        break;
    }
  }

  @SubscribeEvent
  public void handleRawMouse(InputEvent.RawMouseEvent event) {
    PlayerExtension<LocalPlayer> player = this.getPlayerExtension().orElse(null);
    if (player != null && this.minecraft.getOverlay() == null
        && this.minecraft.screen == null && !player.getEntity().isSpectator()) {
      Gun gun = player.getMainHandGun().orElse(null);
      if (this.minecraft.options.keyAttack.matchesMouse(event.getButton())) {
        boolean triggerPressed = event.getAction() == GLFW.GLFW_PRESS;
        if (gun != null) {
          event.setCanceled(true);
          gun.setTriggerPressed(player, triggerPressed, true);
        }
      } else if (this.minecraft.options.keyUse.matchesMouse(event.getButton())) {
        if (gun != null) {
          switch (gun.getSecondaryActionTrigger()) {
            case HOLD:
              gun.setPerformingSecondaryAction(player, event.getAction() == GLFW.GLFW_PRESS, true);
              break;
            case TOGGLE:
              if (event.getAction() == GLFW.GLFW_PRESS) {
                gun.setPerformingSecondaryAction(player, !gun.isPerformingSecondaryAction(), true);
              }
              break;
            default:
              break;
          }
          event.setCanceled(true);
        }
      }
    }
  }

  @SubscribeEvent
  public void handleRenderLiving(RenderLivingEvent.Pre<?, HumanoidModel<?>> event) {
    ItemStack heldStack = event.getEntity().getMainHandItem();
    // TODO Unpleasant way of setting pose for gun. Introduce nicer system (with better poses).
    if (event.getRenderer().getModel() instanceof HumanoidModel
        && heldStack.getItem() instanceof GunItem) {
      HumanoidModel<?> model = event.getRenderer().getModel();
      switch (event.getEntity().getMainArm()) {
        case LEFT:
          model.leftArmPose = ArmPose.BOW_AND_ARROW;
          break;
        case RIGHT:
          model.rightArmPose = ArmPose.BOW_AND_ARROW;
          break;
        default:
          break;
      }
    }
  }

  @SubscribeEvent
  public void handleRenderGameOverlayPreLayer(RenderGameOverlayEvent.PreLayer event) {
    var player = this.getCameraPlayer();
    if (player == null) {
      return;
    }

    final var overlay = event.getOverlay();
    if (overlay == ForgeIngameGui.PLAYER_HEALTH_ELEMENT
        || overlay == ForgeIngameGui.HOTBAR_ELEMENT
        || overlay == ForgeIngameGui.EXPERIENCE_BAR_ELEMENT
        || overlay == ForgeIngameGui.MOUNT_HEALTH_ELEMENT
        || overlay == ForgeIngameGui.FOOD_LEVEL_ELEMENT
        || overlay == ForgeIngameGui.AIR_LEVEL_ELEMENT
        || overlay == ForgeIngameGui.ARMOR_LEVEL_ELEMENT) {
      event.setCanceled(player.isCombatModeEnabled());
    } else if (overlay == ForgeIngameGui.CROSSHAIR_ELEMENT) {
      var aiming = player.getMainHandItem().getCapability(Scope.CAPABILITY)
          .map(scope -> scope.isScoping(player.getEntity()))
          .orElse(false);
      if (player.hasProgressMonitor() || aiming) {
        event.setCanceled(true);
        return;
      }

      player.getMainHandItem().getCapability(Gun.CAPABILITY).ifPresent(gun -> {
        event.setCanceled(true);
        if (gun.getClient().hasCrosshair()) {
          this.ingameGui.renderCrosshairs(event.getMatrixStack(),
              gun.getAccuracy(player, player.getEntity().getRandom()),
              event.getPartialTicks(), event.getWindow().getGuiScaledWidth(),
              event.getWindow().getGuiScaledHeight());
        }
      });
    }
  }

  @SubscribeEvent
  public void handleRenderGameOverlayPre(RenderGameOverlayEvent.Pre event) {
    var player = this.getCameraPlayer();
    if (player == null) {
      return;
    }

    var heldStack = player.getMainHandItem();
    var gun = heldStack.getCapability(Gun.CAPABILITY).orElse(null);
    switch (event.getType()) {
      case ALL:
        this.ingameGui.renderOverlay(player, heldStack, gun, event.getMatrixStack(),
            event.getWindow().getGuiScaledWidth(), event.getWindow().getGuiScaledHeight(),
            event.getPartialTicks());
        break;
      default:
        break;
    }
  }

  @SubscribeEvent
  public void handleCameraSetup(EntityViewRenderEvent.CameraSetup event) {
    this.cameraManager.getCameraRotations((float) event.getPartialTicks(),
        MUTABLE_CAMERA_ROTATIONS);
    if (this.minecraft.cameraEntity instanceof LivingEntity) {
      LivingEntity livingEntity = (LivingEntity) this.minecraft.cameraEntity;
      ItemStack itemStack = livingEntity.getMainHandItem();
      CustomItemRenderer itemRenderer =
          this.itemRendererManager.getItemRenderer(itemStack.getItem());
      if (itemRenderer != null) {
        itemRenderer.rotateCamera(itemStack, livingEntity, (float) event.getPartialTicks(),
            MUTABLE_CAMERA_ROTATIONS);
      }
    }

    this.lastPitch = Mth.lerp(0.1F, this.lastPitch, MUTABLE_CAMERA_ROTATIONS.x());
    this.lastYaw = Mth.lerp(0.1F, this.lastYaw, MUTABLE_CAMERA_ROTATIONS.y());
    this.lastRoll = Mth.lerp(0.1F, this.lastRoll, MUTABLE_CAMERA_ROTATIONS.z());
    MUTABLE_CAMERA_ROTATIONS.set(0.0F, 0.0F, 0.0F);
    event.setPitch(event.getPitch() + this.lastPitch);
    event.setYaw(event.getYaw() + this.lastYaw);
    event.setRoll(event.getRoll() + this.lastRoll);
  }

  @SubscribeEvent
  public void handeFOVModifier(FOVModifierEvent event) {
    if (this.minecraft.getCameraEntity() instanceof LivingEntity livingEntity) {
      final var scope = livingEntity.getMainHandItem().getCapability(Scope.CAPABILITY).orElse(null);
      if (scope != null && scope.isScoping(livingEntity)) {
        event.setNewfov(1.0F / scope.getZoomMultiplier(livingEntity));
      }
    }
    event.setNewfov(
        event.getNewfov() + this.cameraManager.getFov(Minecraft.getInstance().getFrameTime()));
  }

  @SubscribeEvent
  public void handleRenderTick(TickEvent.RenderTickEvent event) {
    switch (event.phase) {
      case START:
        if (this.minecraft.player != null) {
          this.cameraManager.getLookRotationDelta(FOV);
          this.minecraft.player.turn(FOV.getY(), FOV.getX());
        }
        break;
      case END:
        if (this.minecraft.player != null) {
          this.updateAdrenalineShader(event.renderTickTime);
        }
        break;
      default:
        break;
    }
  }

  private void updateAdrenalineShader(float partialTicks) {
    final GameRenderer gameRenderer = this.minecraft.gameRenderer;
    final boolean shaderLoaded = gameRenderer.currentEffect() != null
        && gameRenderer.currentEffect().getName()
            .equals(ADRENALINE_SHADER.toString());
    if (this.minecraft.player.hasEffect(ModMobEffects.ADRENALINE.get())) {
      final long currentTime = Util.getMillis();
      if (this.adrenalineShaderStartTime == 0L) {
        this.adrenalineShaderStartTime = currentTime;
      }
      float progress = Mth.clamp(
          ((currentTime - this.adrenalineShaderStartTime) - partialTicks) / 5000.0F, 0.0F, 1.0F);
      if (!shaderLoaded) {
        if (gameRenderer.currentEffect() != null) {
          gameRenderer.shutdownEffect();
        }
        gameRenderer.loadEffect(ADRENALINE_SHADER);
      }
      PostChain shaderGroup = gameRenderer.currentEffect();
      RenderUtil.updateUniform("Saturation", progress * 0.25F, shaderGroup);
    } else if (shaderLoaded) {
      this.adrenalineShaderStartTime = 0L;
      gameRenderer.shutdownEffect();
    }
  }

  @SubscribeEvent
  public void handleScreenOpen(ScreenOpenEvent event) {
    // Prevents current screen being closed before new one opens.
    if (this.minecraft.screen instanceof EquipmentScreen screen
        && event.getScreen() == null
        && screen.isTransitioning()) {
      event.setCanceled(true);
    }
  }

  // ================================================================================
  // Client-only helper methods
  // ================================================================================

  public void checkApplyFlashEffects(FlashGrenadeEntity flashGrenadeEntity) {
    // Applies the flash effect at client side for a better delay compensation
    // and better FOV calculation
    int duration = flashGrenadeEntity.calculateDuration(this.minecraft.player,
        RenderUtil.isInsideFrustum(flashGrenadeEntity, false));
    if (duration > 0) {
      MobEffectInstance flashEffect =
          new MobEffectInstance(ModMobEffects.FLASH_BLINDNESS.get(), duration);
      ModMobEffects.applyOrOverrideIfLonger(this.minecraft.player, flashEffect);
    }
  }

  // ================================================================================
  // Hooks
  // ================================================================================

  /**
   * @see com.craftingdead.core.mixin.PlayerRendererMixin
   */
  public static void renderArmWithClothing(PlayerRenderer renderer, PoseStack matrixStack,
      MultiBufferSource renderTypeBuffer,
      int packedLight, AbstractClientPlayer playerEntity, ModelPart armRenderer,
      ModelPart armwearRenderer) {

    var clothingTexture = playerEntity.getCapability(LivingExtension.CAPABILITY)
        .map(LivingExtension::getItemHandler)
        .map(itemHandler -> itemHandler.getStackInSlot(ModEquipmentSlotType.CLOTHING.getIndex()))
        .flatMap(clothingStack -> clothingStack.getCapability(Clothing.CAPABILITY).resolve())
        .map(clothing -> clothing.getTexture(playerEntity.getModelName()))
        .orElse(null);

    RenderArmClothingEvent event = new RenderArmClothingEvent(playerEntity, clothingTexture);
    MinecraftForge.EVENT_BUS.post(event);
    clothingTexture = event.getClothingTexture();

    if (clothingTexture != null) {
      PlayerModel<AbstractClientPlayer> playerModel = renderer.getModel();
      playerModel.attackTime = 0.0F;
      playerModel.crouching = false;
      playerModel.swimAmount = 0.0F;
      playerModel.setupAnim(playerEntity, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);

      armRenderer.visible = true;
      armwearRenderer.visible = true;

      armRenderer.xRot = 0.0F;
      armRenderer.render(matrixStack,
          renderTypeBuffer.getBuffer(RenderType.entityTranslucent(clothingTexture)), packedLight,
          OverlayTexture.NO_OVERLAY);
      armwearRenderer.xRot = 0.0F;
      armwearRenderer.render(matrixStack,
          renderTypeBuffer.getBuffer(RenderType.entityTranslucent(clothingTexture)), packedLight,
          OverlayTexture.NO_OVERLAY);
    }
  }
}
