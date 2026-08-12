package net.nostalgia.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.nostalgia.client.NostalgiaConfig;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {

    @Shadow @Final private Minecraft minecraft;
    @Shadow private long fadeOutStart;
    @Shadow private long fadeInStart;
    @Shadow @Final private boolean fadeIn;
    @Shadow private float currentProgress;

    // 26.1 had a protected renderProgressBar; 1.21.11 renamed it to
    // drawProgressBar AND made it private, so it cannot be @Shadow-ed as an
    // abstract method. @Invoker generates the accessor instead.
    @Invoker("drawProgressBar")
    protected abstract void callDrawProgressBar(GuiGraphics graphics, int x0, int y0, int x1, int y1, float fade);

    private static final Identifier LEGACY_LOCATION = Identifier.fromNamespaceAndPath("nostalgia", "textures/gui/title/mojang_legacy.png");
    private static final Identifier BETA_LOCATION = Identifier.fromNamespaceAndPath("nostalgia", "textures/gui/title/mojang_beta.png");
    private static final Identifier SPECS_LOCATION = Identifier.fromNamespaceAndPath("nostalgia", "textures/gui/title/mojang_specifications.png");
    private static long splashStartTime = -1L;

    @Inject(method = "registerTextures", at = @At("TAIL"))
    private static void onRegisterTextures(net.minecraft.client.renderer.texture.TextureManager textureManager, CallbackInfo ci) {
        try {
            textureManager.registerAndLoad(LEGACY_LOCATION, new net.minecraft.client.renderer.texture.ReloadableTexture(LEGACY_LOCATION) {
                @Override public net.minecraft.client.renderer.texture.TextureContents loadContents(net.minecraft.server.packs.resources.ResourceManager rm) throws java.io.IOException {
                    try (java.io.InputStream is = java.nio.file.Files.newInputStream(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("nostalgia").get().findPath("assets/nostalgia/textures/gui/title/mojang_legacy.png").orElseThrow())) {
                        return new net.minecraft.client.renderer.texture.TextureContents(com.mojang.blaze3d.platform.NativeImage.read(is), new net.minecraft.client.resources.metadata.texture.TextureMetadataSection(true, true, net.minecraft.client.renderer.texture.MipmapStrategy.MEAN, 0.0F));
                    } catch (Exception e) { return null; }
                }
            });
            textureManager.registerAndLoad(BETA_LOCATION, new net.minecraft.client.renderer.texture.ReloadableTexture(BETA_LOCATION) {
                @Override public net.minecraft.client.renderer.texture.TextureContents loadContents(net.minecraft.server.packs.resources.ResourceManager rm) throws java.io.IOException {
                    try (java.io.InputStream is = java.nio.file.Files.newInputStream(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("nostalgia").get().findPath("assets/nostalgia/textures/gui/title/mojang_beta.png").orElseThrow())) {
                        return new net.minecraft.client.renderer.texture.TextureContents(com.mojang.blaze3d.platform.NativeImage.read(is), new net.minecraft.client.resources.metadata.texture.TextureMetadataSection(true, true, net.minecraft.client.renderer.texture.MipmapStrategy.MEAN, 0.0F));
                    } catch (Exception e) { return null; }
                }
            });
            textureManager.registerAndLoad(SPECS_LOCATION, new net.minecraft.client.renderer.texture.ReloadableTexture(SPECS_LOCATION) {
                @Override public net.minecraft.client.renderer.texture.TextureContents loadContents(net.minecraft.server.packs.resources.ResourceManager rm) throws java.io.IOException {
                    try (java.io.InputStream is = java.nio.file.Files.newInputStream(net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer("nostalgia").get().findPath("assets/nostalgia/textures/gui/title/mojang_specifications.png").orElseThrow())) {
                        return new net.minecraft.client.renderer.texture.TextureContents(com.mojang.blaze3d.platform.NativeImage.read(is), new net.minecraft.client.resources.metadata.texture.TextureMetadataSection(true, true, net.minecraft.client.renderer.texture.MipmapStrategy.MEAN, 0.0F));
                    } catch (Exception e) { return null; }
                }
            });
        } catch (Exception ignored) {}
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
        if (!NostalgiaConfig.get().alphaLoadingScreen) return;
        ci.cancel();
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        long now = Util.getMillis();
        if (this.fadeIn && this.fadeInStart == -1L) this.fadeInStart = now;
        if (splashStartTime == -1L) splashStartTime = now;
        boolean isFadingOut = this.fadeOutStart > -1L;
        float fadeOutAnim = isFadingOut ? (float)(now - this.fadeOutStart) / 1000.0F : -1.0F;
        float fadeInAnim = this.fadeInStart > -1L ? (float)(now - this.fadeInStart) / 500.0F : -1.0F;
        int state = (int)(splashStartTime % 3L);
        boolean shouldDrawBackground = (this.minecraft.screen != null) && (this.fadeIn || isFadingOut);
        if (shouldDrawBackground) this.minecraft.screen.render(graphics, mouseX, mouseY, a);
        if (fadeOutAnim < 1.0F) {
            float progress = Mth.clamp(fadeOutAnim, 0.0F, 1.0F);
            int alphaBase = (this.fadeIn && fadeInAnim < 1.0F) ? Mth.ceil(Mth.clamp((double)fadeInAnim, 0.15, 1.0) * 255.0) : 255;
            int logoColor = ARGB.white(alphaBase);
            int bgColor = (state == 1 || state == 2) ? ARGB.color(alphaBase, 255, 255, 255) : ARGB.color(alphaBase, 239, 50, 61);
            int centerX = width / 2, centerY = height / 2;
            if (progress > 0.0F) {
                int blockSize = 32;
                float maxRadius = (float)Math.sqrt(centerX*centerX+centerY*centerY);
                float currentRadius = maxRadius * progress * 1.5F;
                for (int y = 0; y < height; y += blockSize) {
                    for (int x = 0; x < width; x += blockSize) {
                        float dx = (x+blockSize/2.0F)-centerX, dy=(y+blockSize/2.0F)-centerY;
                        int cellX1=Math.min(x+blockSize,width),cellY1=Math.min(y+blockSize,height);
                        float dist=(float)Math.sqrt(dx*dx+dy*dy);
                        float eatenAmount=currentRadius-dist;
                        if (eatenAmount > 0) { if(eatenAmount<=48.0F) graphics.fill(x,y,cellX1,cellY1,0xFFFFFFFF); continue; }
                        graphics.fill(x,y,cellX1,cellY1,bgColor);
                        if (state==0) {
                            drawChoppedBlit(graphics,RenderPipelines.MOJANG_LOGO,LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,x,y,cellX1,cellY1,centerX-120,centerY-30,120,60,-0.0625f,0.0f,120,60,120,120,logoColor);
                            drawChoppedBlit(graphics,RenderPipelines.MOJANG_LOGO,LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,x,y,cellX1,cellY1,centerX,centerY-30,120,60,0.0625f,60.0f,120,60,120,120,logoColor);
                        } else if (state==1) {
                            drawChoppedBlit(graphics,RenderPipelines.GUI_TEXTURED,LEGACY_LOCATION,x,y,cellX1,cellY1,centerX-128,centerY-128,256,256,0,0,256,256,256,256,logoColor);
                        } else {
                            drawChoppedBlit(graphics,RenderPipelines.GUI_TEXTURED,BETA_LOCATION,x,y,cellX1,cellY1,centerX-128,centerY-128,256,256,0,0,128,128,128,128,logoColor);
                        }
                    }
                }
            } else {
                graphics.fill(0,0,width,height,bgColor);
                if (state==0) {
                    graphics.blit(RenderPipelines.MOJANG_LOGO,LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,centerX-120,centerY-30,-0.0625F,0.0F,120,60,120,60,120,120,logoColor);
                    graphics.blit(RenderPipelines.MOJANG_LOGO,LoadingOverlay.MOJANG_STUDIOS_LOGO_LOCATION,centerX,centerY-30,0.0625F,60.0F,120,60,120,60,120,120,logoColor);
                } else if (state==1) {
                    graphics.blit(RenderPipelines.GUI_TEXTURED,LEGACY_LOCATION,centerX-128,centerY-128,0,0,256,256,256,256,256,256,logoColor);
                } else {
                    graphics.blit(RenderPipelines.GUI_TEXTURED,BETA_LOCATION,centerX-128,centerY-128,0,0,256,256,128,128,128,128,logoColor);
                }
                if (state==0||state==1) {
                    int barY=(int)(height*0.8325);
                    this.callDrawProgressBar(graphics,width/2-120,barY-5,width/2+120,barY+5,1.0F);
                }
            }
        }
        if (fadeOutAnim >= 1.0F) { this.minecraft.setOverlay(null); splashStartTime = -1L; }
    }

    private void drawChoppedBlit(GuiGraphics graphics, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, Identifier location,
                                  int cellX0, int cellY0, int cellX1, int cellY1,
                                  int logoX, int logoY, int logoW, int logoH,
                                  float u0, float v0, float uW, float vH, int texW, int texH, int color) {
        int ix0=Math.max(cellX0,logoX),iy0=Math.max(cellY0,logoY),ix1=Math.min(cellX1,logoX+logoW),iy1=Math.min(cellY1,logoY+logoH);
        if (ix1>ix0&&iy1>iy0) {
            float pL=(float)(ix0-logoX)/logoW,pR=(float)(ix1-logoX)/logoW;
            float pT=(float)(iy0-logoY)/logoH,pB=(float)(iy1-logoY)/logoH;
            graphics.blit(pipeline,location,ix0,iy0,u0+uW*pL,v0+vH*pT,ix1-ix0,iy1-iy0,(int)(uW*(pR-pL)),(int)(vH*(pB-pT)),texW,texH,color);
        }
    }
}
