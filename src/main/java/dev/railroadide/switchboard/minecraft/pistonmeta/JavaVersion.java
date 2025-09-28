package dev.railroadide.switchboard.minecraft.pistonmeta;

import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;

public record JavaVersion(String component, int majorVersion) {
    public static JavaVersion fromJson(JsonObject json) {
        return Switchboard.GSON.fromJson(json, JavaVersion.class);
    }
}
