package me.srrapero720.embeddiumplus.foundation.embeddium.pages;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.options.*;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import me.srrapero720.embeddiumplus.EmbyConfig;
import me.srrapero720.embeddiumplus.EmbyMixinConfig;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class MixinsPage extends OptionPage {
    private static final SodiumOptionsStorage mixinsOptionsStorage = new SodiumOptionsStorage();

    public MixinsPage() {
        super(Component.literal("Mixins"), create());
    }

    private static ImmutableList<OptionGroup> create() {
        final List<OptionGroup> groups = new ArrayList<>();

        groups.add(OptionGroup.createBuilder()
                .add(OptionImpl.createBuilder(boolean.class, mixinsOptionsStorage)
                        .setName(Component.translatable("embeddium.plus.mixins.borderless.f11.title"))
                        .setTooltip(Component.translatable("embeddium.plus.mixins.borderless.f11.desc"))
                        .setControl(TickBoxControl::new)
                        .setBinding((options, value) -> EmbyMixinConfig.mixin$Borderless$F11.set(value),
                                (options) -> EmbyMixinConfig.mixin$Borderless$F11.get())
                        .setFlags(OptionFlag.REQUIRES_GAME_RESTART)
                        .build()
                ).build())
        ;

        return ImmutableList.copyOf(groups);
    }
}
