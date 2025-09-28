package dev.railroadide.switchboard.minecraft.pistonmeta;

import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;

public record AssetIndex(String id, String sha1, int size, int totalSize, String url) {
    public static AssetIndex fromJson(JsonObject json) {
        return Switchboard.GSON.fromJson(json, AssetIndex.class);
    }
}
