package me.srrapero720.embeddiumplus.foundation.fps;

import com.mojang.blaze3d.vertex.PoseStack;
import me.srrapero720.embeddiumplus.EmbeddiumPlus;
import me.srrapero720.embeddiumplus.EmbyConfig;
import me.srrapero720.embeddiumplus.EmbyTools;
import me.srrapero720.embeddiumplus.foundation.fps.accessors.IUsageGPU;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FrameTimer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = EmbeddiumPlus.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DebugOverlayEvent {
    private static final FPSDisplay DISPLAY = new FPSDisplay();

    private static final Component MSG_FPS = Component.translatable("embeddium.plus.options.displayfps.fps");
    private static final Component MSG_MIN = Component.translatable("embeddium.plus.options.displayfps.min");
    private static final Component MSG_AVG = Component.translatable("embeddium.plus.options.displayfps.avg");
    private static final Component MSG_GPU = Component.translatable("embeddium.plus.options.displayfps.gpu");
    private static final Component MSG_MEM = Component.translatable("embeddium.plus.options.displayfps.mem");

    private static final AverageQueue AVERAGE = new AverageQueue();

    private static int fps = -1;
    private static int minFPS = -1;
    private static int lastAvgFps = -1; // NEEDED
    private static int avgFPS = -1;
    private static int gpuPercent = -1;
    private static int memUsage = -1;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderOverlayItem(RenderGuiOverlayEvent.Pre event) {
        if (!event.getOverlay().id().getPath().equals("debug_text")) return;

        // cancel rendering text if chart is displaying
        if (Minecraft.getInstance().options.renderFpsChart) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        renderFPSChar(minecraft, event.getPoseStack(), minecraft.font, event.getWindow().getGuiScale(), event.getWindow().getGuiScaledWidth());
    }

    private static void renderFPSChar(Minecraft mc, PoseStack pose, Font font, double scale, int scaledWidth) {
        if (mc.options.renderDebug || mc.options.renderFpsChart) return; // No render when F3 is open

        final var mode = EmbyConfig.fpsDisplayMode.get();
        final var systemMode = EmbyConfig.fpsDisplaySystemMode.get();

        if (mode.off() && systemMode.off()) return; // NOTHING TO DO HERE, BACK TO WORK

        DISPLAY.release();

        // FPS
        switch (mode) {
            case SIMPLE -> DISPLAY.append(calculateFPS$getColor(mc)).add(fix(fps)).add(" ").add(MSG_FPS.getString()).add(ChatFormatting.RESET);
            case ADVANCED -> {
                DISPLAY.append(calculateFPS$getColor(mc)).add(fix(fps)).add(ChatFormatting.RESET);
                DISPLAY.append(calculateMinFPS$getColor(mc)).add(MSG_MIN).add(" ").add(fix(minFPS)).add(ChatFormatting.RESET);
                DISPLAY.append(calculateAvgFPS$getColor(mc)).add(MSG_AVG).add(" ").add(fix(avgFPS)).add(ChatFormatting.RESET);
            }
        }
        if (!DISPLAY.isEmpty()) DISPLAY.split();

        // GPU AND RAM
        switch (systemMode) {
            case GPU ->
                    DISPLAY.append(calculateGPUPercent$getColor(mc)).add(MSG_GPU).add(" ").add(fix(gpuPercent)).add("%").add(ChatFormatting.RESET);
            case RAM ->
                    DISPLAY.append(calculateMemPercent$getColor()).add(MSG_MEM).add(" ").add(fix(memUsage)).add("%").add(ChatFormatting.RESET);
            case ON -> {
                DISPLAY.append(calculateGPUPercent$getColor(mc)).add(MSG_GPU).add(" ").add(fix(gpuPercent)).add("%").add(ChatFormatting.RESET);
                DISPLAY.append(calculateMemPercent$getColor()).add(MSG_MEM).add(" ").add(fix(memUsage)).add("%").add(ChatFormatting.RESET);
            }
        }

        if (DISPLAY.isEmpty()) DISPLAY.add("FATAL ERROR");

        float margin = (scale > 0) ? EmbyConfig.fpsDisplayMarginCache / (float) scale : EmbyConfig.fpsDisplayMarginCache;

        // Prevent FPS-Display to render outside screenspace
        String displayString = DISPLAY.toString();
        float maxPosX = scaledWidth - font.width(displayString);
        float posX, posY;

        posX = switch (EmbyConfig.fpsDisplayGravity.get()) {
            case LEFT -> margin;
            case CENTER -> (maxPosX / 2);
            case RIGHT -> maxPosX - margin;
        };
        posY = margin;

        pose.pushPose();
        if (EmbyConfig.fpsDisplayShadowCache) {
            GuiComponent.fill(pose,(int) posX - 2, (int) posY - 2, (int) posX + font.width(displayString) + 2, (int) (posY + font.lineHeight) + 1, -1873784752);
        }

        GuiComponent.drawString(pose, font, displayString, (int) posX, (int) posY, EmbyTools.getColorARGB(200, 255, 255, 255));
        DISPLAY.release();
        pose.popPose();
    }

    private static ChatFormatting calculateFPS$getColor(Minecraft mc) {
        fps = Minecraft.fps;
        return EmbyTools.colorByLow(fps);
    }

    private static ChatFormatting calculateMinFPS$getColor(Minecraft mc) {
        FrameTimer timer = mc.getFrameTimer();

        int start = timer.getLogStart();
        int end = timer.getLogEnd();

        if (end == start) return EmbyTools.colorByLow(minFPS);

        int fps = Minecraft.fps;
        if (fps <= 0) fps = 1;

        long[] frames = timer.getLog();
        long maxNS = (long) (1 / (double) fps * 1000000000);
        long totalNS = 0;

        int index = Math.floorMod(end - 1, frames.length);
        while (index != start && (double) totalNS < 1000000000) {
            long timeNs = frames[index];
            if (timeNs > maxNS) {
                maxNS = timeNs;
            }

            totalNS += timeNs;
            index = Math.floorMod(index - 1, frames.length);
        }

        minFPS = (int) (1 / ((double) maxNS / 1000000000));
        return EmbyTools.colorByLow(minFPS);
    }

    private static ChatFormatting calculateAvgFPS$getColor(Minecraft mc) {
        if (Minecraft.fps != lastAvgFps) { // DON'T BLOOD AVG
            AVERAGE.push(lastAvgFps = Minecraft.fps);
            avgFPS = AVERAGE.calculate();
        }
        return EmbyTools.colorByLow(avgFPS);
    }

    private static ChatFormatting calculateGPUPercent$getColor(Minecraft mc) {
        int value = (int) ((IUsageGPU) mc).embPlus$getSyncGpu();
        gpuPercent = (value > 0) ? Math.min(value, 100) : -1;
        return EmbyTools.colorByPercent(gpuPercent);
    }

    private static ChatFormatting calculateMemPercent$getColor() {
        memUsage = (int) ((EmbyTools.ramUsed() * 100) / Runtime.getRuntime().maxMemory());
        return EmbyTools.colorByPercent(memUsage);
    }

    private static String fix(int value) {
        return (value == -1) ? "--" : "" + value;
    }

    public static class AverageQueue {
        private final int[] QUEUE = new int[14];
        private int used = 0;

        void push(int value) {
            if (used == QUEUE.length) used = 0;
            QUEUE[used] = value;
            used++;
        }

        int calculate() {
            int times = 0;
            for (int i = 0; i < used; i++) {
                times += QUEUE[i];
            }

            return times / used;
        }
    }
}