package dev.railroadide.switchboard.routing;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.fabric.FabricApiVersionService;
import dev.railroadide.switchboard.minecraft.fabric.FabricLoaderVersionService;
import io.javalin.Javalin;

public class FabricRouting {
    public static void addRoutes(Javalin server) {
        var fabricApiVersionService = new FabricApiVersionService();
        var fabricLoaderVersionService = new FabricLoaderVersionService();

        server.get("/fabric/api/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(fabricApiVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /fabric/api/versions");

        server.get("/fabric/api/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(fabricApiVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/api/versions/{minecraftVersion}");

        server.get("/fabric/api/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);

            fabricApiVersionService.latestFor(minecraftVersion, includePrereleases)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/api/latest/{minecraftVersion}");

        server.get("/fabric/api/latest", ctx -> {
            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);
            fabricApiVersionService.listAllVersions(includePrereleases)
                    .stream()
                    .findFirst()
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/api/latest");

        server.get("/fabric/loader/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(fabricLoaderVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /fabric/loader/versions");

        server.get("/fabric/loader/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(fabricLoaderVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/loader/versions/{minecraftVersion}");

        server.get("/fabric/loader/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);

            fabricLoaderVersionService.latestFor(minecraftVersion, includePrereleases)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/loader/latest/{minecraftVersion}");

        server.get("/fabric/loader/latest", ctx -> {
            boolean includePrereleases = ctx.queryParamAsClass("includePrereleases", boolean.class).getOrDefault(false);
            fabricLoaderVersionService.listAllVersions(includePrereleases)
                    .stream()
                    .findFirst()
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /fabric/loader/latest");
    }
}
