package com.shinapri.bulkexcavation.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ExcavationConfigIO {
    private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = Path.of("config", "bulk-excavation.json");
    private static ExcavationConfig INSTANCE = load();

    private ExcavationConfigIO() {}

    public static ExcavationConfig get() { return INSTANCE; }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, G.toJson(INSTANCE));
        } catch (IOException e) { e.printStackTrace(); }
    }

    private static ExcavationConfig load() {
        try {
            if (Files.exists(FILE)) {
                return G.fromJson(Files.readString(FILE), ExcavationConfig.class);
            }
        } catch (IOException ignored) {}
        return new ExcavationConfig();
    }
}
