package dev.railroadide.switchboard.routing;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.forge.NeoforgeVersionService;
import io.javalin.Javalin;

public class NeoforgeRouting {
    public static void addRoutes(Javalin server) {
        var neoforgeVersionService = new NeoforgeVersionService();

        server.get("/neoforge/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(neoforgeVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /neoforge/versions");

        server.get("/neoforge/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(neoforgeVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /neoforge/versions/{minecraftVersion}");

        server.get("/neoforge/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);

            neoforgeVersionService.latestFor(minecraftVersion, includePrereleases)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /neoforge/latest/{minecraftVersion}");

        server.get("/neoforge/latest", ctx -> {
            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);
            neoforgeVersionService.listAllVersions(includePrereleases)
                    .stream()
                    .reduce((_, second) -> second)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /neoforge/latest");
    }
}
