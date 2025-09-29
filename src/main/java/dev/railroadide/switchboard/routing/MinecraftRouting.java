package dev.railroadide.switchboard.routing;

import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import io.javalin.Javalin;

import java.util.*;
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
                    () -> ctx.status(404).json(Map.of("error", "Not Found"))
            );
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/versions/{id}");

        server.get("/minecraft/latest", ctx ->
                MinecraftVersion.getLatestVersion().ifPresentOrElse(
                        version -> ctx.json(Switchboard.GSON.toJsonTree(version)),
                        () -> ctx.status(404).json(Map.of("error", "Not Found"))
                ));
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/latest");

        server.get("/minecraft/latest/{versionType}", ctx -> {
            String versionType = ctx.pathParam("versionType").toLowerCase(Locale.ROOT);
            MinecraftVersion.VersionType type;
            try {
                type = MinecraftVersion.VersionType.valueOf(versionType.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                ctx.status(400).json(Map.of("error", "Invalid version type. Valid types are: " +
                        Arrays.stream(MinecraftVersion.VersionType.values())
                                .map(MinecraftVersion.VersionType::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.joining(", "))));
                return;
            }

            MinecraftVersion.getLatestVersion(type).ifPresentOrElse(
                    version -> ctx.json(Switchboard.GSON.toJsonTree(version)),
                    () -> ctx.status(404).json(Map.of("error", "Not Found"))
            );
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/latest/{versionType}");

        server.get("/minecraft/piston-meta/{id}", ctx -> {
            String id = ctx.pathParam("id");

            Optional<MinecraftVersion> minecraftVersionOpt = MinecraftVersion.fromId(id);
            if (minecraftVersionOpt.isEmpty()) {
                ctx.status(404).json(Map.of("error", "Not Found"));
                return;
            }

            String requestBody = ctx.body();
            ctx.future(() -> minecraftVersionOpt.get().requestPistonMeta().thenAccept(versionPackage -> {
                // e.g. arguments.game,assets,mainClass,downloads.client.url = ["arguments.game", "assets", "mainClass", "downloads.client.url"]
                List<String> fields = getFieldsFromBody(requestBody);
                JsonObject versionPackageJson = Switchboard.GSON.toJsonTree(versionPackage).getAsJsonObject();
                if (fields.isEmpty() || fields.contains("*")) {
                    ctx.json(versionPackageJson);
                } else {
                    var filtered = new JsonObject();
                    filtered = filterJsonObject(versionPackageJson, fields);

                    ctx.json(filtered);
                }
            }));
        });
        Switchboard.LOGGER.info("Registered endpoint: /minecraft/piston-meta/{id}");
    }

    private static List<String> getFieldsFromBody(String body) {
        if (body == null || body.isBlank())
            return Collections.emptyList();

        return Arrays.stream(body.split("\r?\n"))
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
