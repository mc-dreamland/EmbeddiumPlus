package me.srrapero720.embeddiumplus.foundation.embeddium.pages;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import me.srrapero720.embeddiumplus.EmbyConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

import java.util.ArrayList;
import java.util.List;

public class QualityPlusPage extends OptionPage {
    private static final SodiumOptionsStorage qualityOptionsStorage = new SodiumOptionsStorage();

    public QualityPlusPage() {
        super(new TranslatableComponent("sodium.options.pages.quality").append("++"), create());
    }

    private static ImmutableList<OptionGroup> create() {
        final List<OptionGroup> groups = new ArrayList<>();

        final var fog = OptionImpl.createBuilder(boolean.class, qualityOptionsStorage)
                .setName(new TranslatableComponent("embeddium.plus.options.fog.title"))
                .setTooltip(new TranslatableComponent("embeddium.plus.options.fog.desc"))
                .setControl(TickBoxControl::new)
                .setBinding((options, value) -> {
                            EmbyConfig.fog.set(value);
                            EmbyConfig.fogCache = value;
                        },
                        (options) -> EmbyConfig.fogCache)
                .setImpact(OptionImpact.LOW)
                .build();

        final var fadeInQuality = OptionImpl.createBuilder(EmbyConfig.ChunkFadeSpeed.class, qualityOptionsStorage)
                .setName(new TranslatableComponent("embeddium.plus.options.fadein.title"))
                .setTooltip(new TranslatableComponent("embeddium.plus.options.fadein.desc"))
                .setControl((option) -> new CyclingControl<>(option, EmbyConfig.ChunkFadeSpeed.class, new Component[]{
                        new TranslatableComponent("options.off"),
                        new TranslatableComponent("options.graphics.fast"),
                        new TranslatableComponent("options.graphics.fancy")
                }))
                .setBinding((opts, value) -> EmbyConfig.chunkFadeSpeed.set(value),
                        (opts) -> EmbyConfig.chunkFadeSpeed.get())
                .setImpact(OptionImpact.LOW)
                .setEnabled(false)
                .build();

        groups.add(OptionGroup.createBuilder()
                .add(fog)
                .add(fadeInQuality)
                .build()
        );

        final var cloudHeight = OptionImpl.createBuilder(int.class, qualityOptionsStorage)
                .setName(new TranslatableComponent("embeddium.plus.options.clouds.height.title"))
                .setTooltip(new TranslatableComponent("embeddium.plus.options.clouds.height.desc"))
                .setControl((option) -> new SliderControl(option, 64, 364, 4, ControlValueFormatter.biomeBlend()))
                .setBinding((options, value) -> {
                            EmbyConfig.cloudsHeight.set(value);
                            EmbyConfig.cloudsHeightCache = value;
                        },
                        (options) -> EmbyConfig.cloudsHeightCache)
                .build();

        groups.add(OptionGroup.createBuilder()
                .add(cloudHeight)
                .build()
        );

        return ImmutableList.copyOf(groups);
    }
}
