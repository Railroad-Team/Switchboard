package dev.railroadide.switchboard.routing;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.mappings.MCPVersionService;
import dev.railroadide.switchboard.minecraft.mappings.MojmapVersionService;
import dev.railroadide.switchboard.minecraft.mappings.YarnVersionService;
import dev.railroadide.switchboard.minecraft.parchment.ParchmentVersion;
import dev.railroadide.switchboard.minecraft.parchment.ParchmentVersionManager;
import io.javalin.Javalin;

public class MappingsRouting {
    public static void addRoutes(Javalin server) {
        var parchmentVersionManager = new ParchmentVersionManager();
        var mcpVersionService = new MCPVersionService();
        var mojmapVersionService = new MojmapVersionService();
        var yarnVersionService = new YarnVersionService();

        server.get("/parchment/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(parchmentVersionManager.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /parchment/versions");

        server.get("/parchment/versions/{minecraftVersion}", ctx -> {
            String minecraftVersion = ctx.pathParam("minecraftVersion");
            ctx.json(Switchboard.GSON.toJsonTree(parchmentVersionManager.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /parchment/versions/{minecraftVersion}");

        server.get("/parchment/latest/{minecraftVersion}", ctx -> {
            String minecraftVersion = ctx.pathParam("minecraftVersion");
            parchmentVersionManager.latestFor(minecraftVersion)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /parchment/latest/{minecraftVersion}");

        server.get("/parchment/latest", ctx -> {
            ParchmentVersion latest = parchmentVersionManager.latestOverall();
            if (latest != null) {
                ctx.json(latest);
            } else {
                ctx.status(404).json("Not Found");
            }
        });
        Switchboard.LOGGER.info("Registered endpoint: /parchment/latest");

        server.get("/parchment/grouped",
                ctx -> ctx.json(parchmentVersionManager.getAllVersionsGroupedByMinecraftVersion()));
        Switchboard.LOGGER.info("Registered endpoint: /parchment/grouped");

        server.get("/mcp/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(mcpVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /mcp/versions");

        server.get("/mcp/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(mcpVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /mcp/versions/{minecraftVersion}");

        server.get("/mcp/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            mcpVersionService.latestFor(minecraftVersion)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /mcp/latest/{minecraftVersion}");

        server.get("/mcp/latest", ctx -> mcpVersionService.listAllVersions()
                .stream()
                .findFirst()
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json("Not Found")
                ));
        Switchboard.LOGGER.info("Registered endpoint: /mcp/latest");

        server.get("/mojmap/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(mojmapVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /mojmap/versions");

        server.get("/mojmap/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(mojmapVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /mojmap/versions/{minecraftVersion}");

        server.get("/mojmap/latest", ctx -> mojmapVersionService.listAllVersions()
                .stream()
                .findFirst()
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json("Not Found")
                ));
        Switchboard.LOGGER.info("Registered endpoint: /mojmap/latest");

        server.get("/yarn/versions",
                ctx -> ctx.json(Switchboard.GSON.toJsonTree(yarnVersionService.listAllVersions())));
        Switchboard.LOGGER.info("Registered endpoint: /yarn/versions");

        server.get("/yarn/versions/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            ctx.json(Switchboard.GSON.toJsonTree(yarnVersionService.listVersionsFor(minecraftVersion)));
        });
        Switchboard.LOGGER.info("Registered endpoint: /yarn/versions/{minecraftVersion}");

        server.get("/yarn/latest/{minecraftVersion}", ctx -> {
            String minecraftVersionStr = ctx.pathParam("minecraftVersion");
            MinecraftVersion minecraftVersion = MinecraftVersion.fromId(minecraftVersionStr).orElse(null);
            if (minecraftVersion == null) {
                ctx.status(400).json("Invalid Minecraft version");
                return;
            }

            yarnVersionService.latestFor(minecraftVersion)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Switchboard.LOGGER.info("Registered endpoint: /yarn/latest/{minecraftVersion}");

        server.get("/yarn/latest", ctx -> yarnVersionService.listAllVersions()
                .stream()
                .findFirst()
                .ifPresentOrElse(
                        ctx::json,
                        () -> ctx.status(404).json("Not Found")
                ));
        Switchboard.LOGGER.info("Registered endpoint: /yarn/latest");
    }
}
