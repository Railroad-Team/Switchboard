package dev.railroadide.switchboard;

import net.sourceforge.argparse4j.inf.Namespace;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class Environment {
    private static final AtomicInteger PORT = new AtomicInteger(7000);
    private static final AtomicReference<Path> PARCHMENT_CLONE_PATH = new AtomicReference<>();

    private static boolean loaded = false;

    public static void load(Namespace arguments) {
        if (loaded)
            throw new IllegalStateException("Environment has already been loaded.");

        loaded = true;
        PORT.set(arguments.getInt("port"));
        PARCHMENT_CLONE_PATH.set(arguments.get("parchmentclonepath"));

        Main.LOGGER.info("Environment loaded!");
    }

    public static int getPort() {
        return PORT.get();
    }

    public static Path getParchmentClonePath() {
        return PARCHMENT_CLONE_PATH.get();
    }
}
