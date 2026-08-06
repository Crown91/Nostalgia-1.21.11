package net.nostalgia.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.io.File;
import java.io.IOException;

public class EpilepsyWarningScreen extends Screen {

    private static final Identifier FLASH_TEXTURE = Identifier.fromNamespaceAndPath("nostalgia", "textures/environment/flash.png");
    private final Screen parent;
    public static final File CONFIG_FILE = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("nostalgia_warning_accepted.txt").toFile();

    public EpilepsyWarningScreen(Screen parent) {
        super(Component.translatable("gui.nostalgia.warning.title"));
        this.parent = parent;
    }

    public static boolean hasAcceptedWarning() { return CONFIG_FILE.exists(); }

    private void acceptWarning() {
        try {
            if (!CONFIG_FILE.getParentFile().exists()) CONFIG_FILE.getParentFile().mkdirs();
            CONFIG_FILE.createNewFile();
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    protected void init() { super.init(); }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int btnW = 130, btnH = 30, btnX = this.width / 2 - btnW / 2, btnY = this.height / 2 + 45;
        if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
            acceptWarning();
            if (this.minecraft != null) this.minecraft.setScreen(this.parent);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int textureWidth = 256, textureHeight = 256;
        int x = (this.width - textureWidth) / 2, y = (this.height - textureHeight) / 2;
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, FLASH_TEXTURE, x, y, 0, 0, textureWidth, textureHeight, textureWidth, textureHeight);
        Component boldTitle = this.title.copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
        graphics.drawCenteredString(this.font, boldTitle, this.width / 2, y + 25, 0xFFFFFFFF);
        Component warningText = Component.translatable("gui.nostalgia.warning.text").copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
        graphics.drawWordWrap(this.font, warningText, this.width / 2 - 85, y + 108, 170, 0xFFFFFFFF, true);
        int btnW = 130, btnH = 30, btnX = this.width / 2 - btnW / 2, btnY = this.height / 2 + 45;
        boolean hovered = mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH;
        Component continueText = Component.translatable("gui.nostalgia.warning.continue").copy().withStyle(net.minecraft.network.chat.Style.EMPTY.withBold(true));
        graphics.drawCenteredString(this.font, continueText, this.width / 2, btnY + 11, hovered ? 0xFF55FFFF : 0xFFFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean shouldCloseOnEsc() { return false; }
}
