package dev.railroadide.switchboard.routing;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.forge.ForgeVersionService;
import io.javalin.Javalin;

public class ForgeRouting {
    public static void addRoutes(Javalin server) {
        var forgeVersionService = new ForgeVersionService();
        server.get("/forge/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(forgeVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /forge/versions");

        server.get("/forge/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(forgeVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /forge/versions/{minecraftVersion}");

        server.get("/forge/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);

            forgeVersionService.latestFor(minecraftVersion, includePrereleases)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /forge/latest/{minecraftVersion}");

        server.get("/forge/latest", ctx -> {
            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);
            forgeVersionService.listAllVersions(includePrereleases)
                    .stream()
                    .reduce((_, second) -> second)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /forge/latest");
    }
}
