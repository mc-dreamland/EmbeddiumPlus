package me.srrapero720.embeddiumplus.mixins.impl.togglefog;

import com.mojang.blaze3d.systems.RenderSystem;
import me.srrapero720.embeddiumplus.EmbyConfig;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FogRenderer.class, priority = 910)
public abstract class FogMixin {
    @Unique private static final float FOG_START = -8.0F;
    @Unique private static final float FOG_END = 1_000_000.0F;

    @Inject(method = "setupFog(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/FogRenderer$FogMode;FZF)V", at = @At("RETURN"), remap = false)
    private static void inject$FogApply(Camera camera, FogRenderer.FogMode fogType, float viewDistance, boolean thickFog, float partialTicks, CallbackInfo ci) {
        if (EmbyConfig.fogCache) return;

        RenderSystem.setShaderFogStart(FOG_START);
        RenderSystem.setShaderFogEnd(FOG_END);
    }
}