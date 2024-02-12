package me.srrapero720.embeddiumplus.mixins.impl.jei;

import com.mojang.blaze3d.vertex.PoseStack;
import me.srrapero720.embeddiumplus.EmbyConfig;
import mezz.jei.gui.elements.GuiIconToggleButton;
import mezz.jei.gui.input.GuiTextFieldFilter;
import mezz.jei.gui.overlay.IngredientListOverlay;
import mezz.jei.gui.overlay.ScreenPropertiesCache;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = IngredientListOverlay.class, remap = false)
@Pseudo
public class JeiOverlayMixin {
    @Shadow @Final private GuiTextFieldFilter searchField;
    @Shadow @Final private GuiIconToggleButton configButton;
    @Shadow @Final private ScreenPropertiesCache screenPropertiesCache;

    @Dynamic
    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lmezz/jei/gui/overlay/IngredientGridWithNavigation;draw(Lnet/minecraft/client/Minecraft;Lcom/mojang/blaze3d/vertex/PoseStack;IIF)V"), cancellable = true)
    public void inject$renderOverlay(Minecraft minecraft, PoseStack guiGraphics, int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (!EmbyConfig.hideJREICache) return;

        String value = searchField.getValue();
        if (value.isEmpty()) {
            if (screenPropertiesCache.hasValidScreen()) {
                configButton.draw(guiGraphics, mouseX, mouseY, partialTicks);
            }
            ci.cancel();
        }
    }

    @Inject(method = "drawTooltips", at = @At(value = "HEAD"), cancellable = true)
    public void inject$renderOverlay(Minecraft minecraft, PoseStack guiGraphics, int mouseX, int mouseY, CallbackInfo ci) {
        if (!EmbyConfig.hideJREICache) return;

        String value = searchField.getValue();
        if (value.isEmpty()) {
            ci.cancel();
        }
    }
}