package com.craftingdead.mod.client.gui.screen.inventory;

import com.craftingdead.mod.inventory.container.GenericContainer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;

public class GenericContainerScreen extends ContainerScreen<GenericContainer>
    implements IHasContainer<GenericContainer> {

  private static final ResourceLocation GENERIC_CONTAINER_TEXTURE =
      new ResourceLocation("textures/gui/container/generic_54.png");

  private static final int TITLE_TEXT_COLOUR = 0x404040;

  public GenericContainerScreen(GenericContainer container, PlayerInventory playerInventory,
      ITextComponent title) {
    super(container, playerInventory, title);
    this.passEvents = false;
    this.ySize = 114 + this.container.getRowCount() * 18;
  }

  @Override
  public void render(int mouseX, int mouseY, float partialTicks) {
    super.render(mouseX, mouseY, partialTicks);
    this.renderHoveredToolTip(mouseX, mouseY);
  }

  @Override
  protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
    this.font.drawString(this.title.getFormattedText(), 8.0F, 6.0F, 4210752);
    this.font
        .drawString(this.playerInventory.getDisplayName().getFormattedText(), 8.0F,
            this.ySize - 96 + 2, TITLE_TEXT_COLOUR);
  }

  @Override
  protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
    this.renderBackground();
    RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
    this.minecraft.getTextureManager().bindTexture(GENERIC_CONTAINER_TEXTURE);
    int x = (this.width - this.xSize) / 2;
    int y = (this.height - this.ySize) / 2;
    this.blit(x, y, 0, 0, this.xSize, this.container.getRowCount() * 18 + 17);
    this.blit(x, y + this.container.getRowCount() * 18 + 17, 0, 126, this.xSize, 96);
  }
}
