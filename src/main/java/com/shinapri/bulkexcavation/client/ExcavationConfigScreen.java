package com.shinapri.bulkexcavation.client;

import com.shinapri.bulkexcavation.config.ExcavationConfig;
import com.shinapri.bulkexcavation.config.ExcavationConfigIO;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.BooleanListEntry;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public final class ExcavationConfigScreen {
    private ExcavationConfigScreen() {}

    public static Screen create(Screen parent) {
        ExcavationConfig cfg = ExcavationConfigIO.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Bulk Excavation"))
                .setSavingRunnable(ExcavationConfigIO::save);

        ConfigCategory cat = builder.getOrCreateCategory(Text.literal("General"));
        ConfigEntryBuilder eb = builder.entryBuilder();

        cat.addEntry(
                eb.startIntField(Text.literal("Max volume per job"), cfg.maxVolume)
                        .setMin(1).setMax(500000).setDefaultValue(ExcavationConfig.DEFAULT_MAX_VOLUME)
                        .setSaveConsumer(v -> cfg.maxVolume = v)
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Use existed tools"), cfg.requireTool)
                        .setDefaultValue(ExcavationConfig.DEFAULT_REQUIRE_TOOL)
                        .setSaveConsumer(v -> cfg.requireTool = v)
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Prevent tools from breaking"), cfg.preventToolsFromBreaking)
                        .setDefaultValue(ExcavationConfig.DEFAULT_PREVENT_TOOLS_FROM_BREAKING)
                        .setSaveConsumer(v -> cfg.preventToolsFromBreaking = v)
                        .build()
        );

        cat.addEntry(
                eb.startIntField(Text.literal("Prevent break threshold"), cfg.preventBreakThreshold)
                        .setMin(1).setMax(2050).setDefaultValue(ExcavationConfig.DEFAULT_PREVENT_BREAK_THRESHOLD)
                        .setSaveConsumer(v -> cfg.preventBreakThreshold = v)
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Drop loot when breaking"), cfg.dropLoot)
                        .setDefaultValue(ExcavationConfig.DEFAULT_DROP_LOOT)
                        .setSaveConsumer(v -> cfg.dropLoot = v)
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Display console log"), cfg.consoleLog)
                        .setDefaultValue(ExcavationConfig.DEFAULT_CONSOLE_LOG)
                        .setSaveConsumer(v -> cfg.consoleLog = v)
                        .build()
        );

        cat.addEntry(
                eb.startStrList(Text.literal("Skip blocks"), cfg.skipBlocks)
                        .setDefaultValue(ExcavationConfig.DEFAULT_SKIP_BLOCKS)
                        .setSaveConsumer(v -> cfg.skipBlocks = new ArrayList<>(v))
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Display preview box"), cfg.preview)
                        .setDefaultValue(ExcavationConfig.DEFAULT_PREVIEW)
                        .setSaveConsumer(v -> cfg.preview = v)
                        .build()
        );

        cat.addEntry(
                eb.startBooleanToggle(Text.literal("Enable remote excavation"), cfg.remoteExcavation)
                        .setDefaultValue(ExcavationConfig.DEFAULT_REMOTE_EXCAVATION)
                        .setSaveConsumer(v -> cfg.remoteExcavation = v)
                        .build()
        );

        return builder.build();
    }
}
