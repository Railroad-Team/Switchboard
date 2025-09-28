package dev.railroadide.switchboard.routing;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import io.javalin.Javalin;

public class Router {
    private final Javalin server;

    public Router(Javalin server) {
        this.server = server;
    }

    public void initialize() {
        MinecraftVersion.requestMinecraftVersions();

        MappingsRouting.addRoutes(server);
        FabricRouting.addRoutes(server);
        ForgeRouting.addRoutes(server);
        NeoforgeRouting.addRoutes(server);
        MinecraftRouting.addRoutes(server);

        server.get("/", ctx -> ctx.result("Switchboard is running."));
        Switchboard.LOGGER.info("Registered endpoint: /");
    }
}