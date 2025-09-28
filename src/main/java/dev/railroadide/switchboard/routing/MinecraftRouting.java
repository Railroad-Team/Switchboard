package dev.railroadide.switchboard.routing;

import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import io.javalin.Javalin;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

public class MinecraftRouting {
    public static void addRoutes(Javalin server) {
        server.get("/minecraft/versions", ctx ->
                ctx.json(Switchboard.GSON.toJsonTree(MinecraftVersion.getVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/versions");

        server.get("/minecraft/versions/{id}", ctx -> {
            String id = ctx.pathParam("id");
            MinecraftVersion.fromId(id).ifPresentOrElse(
                    version -> ctx.json(Switchboard.GSON.toJsonTree(version)),
                    () -> ctx.status(404).json("Not Found")
            );
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/versions/{id}");

        server.get("/minecraft/versions/latest", ctx ->
                MinecraftVersion.getLatestVersion().ifPresentOrElse(
                        version -> ctx.json(Switchboard.GSON.toJsonTree(version)),
                        () -> ctx.status(404).json("Not Found")
                ));
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/versions/latest");

        server.get("/minecraft/versions/latest/{versionType}", ctx -> {
            String versionType = ctx.pathParam("versionType").toLowerCase(Locale.ROOT);
            MinecraftVersion.VersionType type;
            try {
                type = MinecraftVersion.VersionType.valueOf(versionType.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                ctx.status(400).json("Invalid version type. Valid types are: " +
                        Arrays.stream(MinecraftVersion.VersionType.values())
                                .map(MinecraftVersion.VersionType::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.joining(", ")));
                return;
            }

            MinecraftVersion.getLatestVersion(type).ifPresentOrElse(
                    version -> ctx.json(Switchboard.GSON.toJsonTree(version)),
                    () -> ctx.status(404).json("Not Found")
            );
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/versions/latest/{versionType}");

        server.get("/minecraft/piston-meta/{id}", ctx -> {
            String id = ctx.pathParam("id");

            Optional<MinecraftVersion> minecraftVersionOpt = MinecraftVersion.fromId(id);
            if (minecraftVersionOpt.isEmpty()) {
                ctx.status(404).json("Not Found");
                return;
            }

            minecraftVersionOpt.get().requestPistonMeta().thenAcceptAsync(versionPackage -> {
                // e.g. arguments.game,assets,mainClass,downloads.client.url = ["arguments.game", "assets", "mainClass", "downloads.client.url"]
                List<String> fields = getFieldsFromBody(ctx.body());
                if (fields.isEmpty() || fields.contains("*")) {
                    ctx.json(Switchboard.GSON.toJsonTree(versionPackage));
                } else {
                    var filtered = new JsonObject();
                    JsonObject original = Switchboard.GSON.toJsonTree(versionPackage).getAsJsonObject();
                    filtered = filterJsonObject(original, fields);

                    ctx.json(Switchboard.GSON.toJsonTree(filtered));
                }
            });
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/piston-meta/{id}");
    }

    private static List<String> getFieldsFromBody(String body) {
        return Arrays.stream(body.split("\n"))
                .map(String::trim)
                .filter(line -> line.startsWith("fields "))
                .map(line -> line.substring("fields ".length()))
                .flatMap(line -> Arrays.stream(line.split(",")))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .distinct()
                .toList();
    }

    private static JsonObject filterJsonObject(JsonObject original, List<String> fields) {
        var filtered = new JsonObject();
        for (String field : fields) {
            String[] path = field.split("\\.");
            JsonObject currentOriginal = original;
            JsonObject currentFiltered = filtered;
            for (int i = 0; i < path.length; i++) {
                String key = path[i];
                if (currentOriginal.has(key)) {
                    if (i == path.length - 1) {
                        currentFiltered.add(key, currentOriginal.get(key));
                    } else {
                        if (!currentFiltered.has(key) || !currentFiltered.get(key).isJsonObject()) {
                            currentFiltered.add(key, new JsonObject());
                        }
                        currentOriginal = currentOriginal.getAsJsonObject(key);
                        currentFiltered = currentFiltered.getAsJsonObject(key);
                    }
                } else if ("*".equals(key)) {
                    // Wildcard: copy all remaining keys at this level
                    for (String remainingKey : currentOriginal.keySet()) {
                        currentFiltered.add(remainingKey, currentOriginal.get(remainingKey));
                    }

                    break; // No need to continue down this path
                } else {
                    break; // Key not found in original, stop processing this path
                }
            }
        }

        return filtered;
    }
}
