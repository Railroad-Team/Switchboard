package dev.railroadide.switchboard;

import dev.railroadide.switchboard.parchment.ParchmentVersion;
import dev.railroadide.switchboard.parchment.ParchmentVersionManager;
import io.javalin.Javalin;

import java.util.Map;

public class Router {
    private final ParchmentVersionManager parchmentVersionManager = new ParchmentVersionManager();

    private final Javalin server;

    public Router(Javalin server) {
        this.server = server;
    }

    public void initialize() {
        server.get("/parchment/versions",
                ctx -> ctx.json(Main.GSON.toJsonTree(parchmentVersionManager.listAllVersions())));
        Main.LOGGER.info("Registered endpoint: /parchment/versions");

        server.get("/parchment/versions/{minecraftVersion}", ctx -> {
            String minecraftVersion = ctx.pathParam("minecraftVersion");
            ctx.json(Main.GSON.toJsonTree(parchmentVersionManager.listVersionsFor(minecraftVersion)));
        });
        Main.LOGGER.info("Registered endpoint: /parchment/versions/{minecraftVersion}");

        server.get("/parchment/latest/{minecraftVersion}", ctx -> {
            String minecraftVersion = ctx.pathParam("minecraftVersion");
            parchmentVersionManager.latestFor(minecraftVersion)
                    .ifPresentOrElse(
                            ctx::json,
                            () -> ctx.status(404).json("Not Found")
                    );
        });
        Main.LOGGER.info("Registered endpoint: /parchment/latest/{minecraftVersion}");

        server.get("/parchment/latest", ctx -> {
            ParchmentVersion latest = parchmentVersionManager.latestOverall();
            if (latest != null) {
                ctx.json(latest);
            } else {
                ctx.status(404).json("Not Found");
            }
        });
        Main.LOGGER.info("Registered endpoint: /parchment/latest");

        server.get("/parchment/exists/{minecraftVersion}", ctx -> {
            String minecraftVersion = ctx.pathParam("minecraftVersion");
            boolean exists = parchmentVersionManager.doesParchmentExist(minecraftVersion);
            ctx.json(Map.of("exists", exists));
        });
        Main.LOGGER.info("Registered endpoint: /parchment/exists/{minecraftVersion}");

        server.get("/parchment/grouped",
                ctx -> ctx.json(parchmentVersionManager.getAllVersionsGroupedByMinecraftVersion()));
        Main.LOGGER.info("Registered endpoint: /parchment/grouped");
    }
}
