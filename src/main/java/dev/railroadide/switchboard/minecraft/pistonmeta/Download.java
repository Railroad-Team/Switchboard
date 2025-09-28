package dev.railroadide.switchboard.minecraft.pistonmeta;

import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;

public record Download(String sha1, long size, String url) {
    public static Download fromJson(JsonObject json) {
        return Switchboard.GSON.fromJson(json, Download.class);
    }

    public Path downloadToPath(Path path) {
        String[] split = this.url.split("/");
        String fileName = split[split.length - 1];

        return downloadToPath(path, fileName);
    }

    public Path downloadToPath(Path path, String fileName) {
        Path resolved = path.toAbsolutePath().resolve(fileName);
        Switchboard.LOGGER.debug("Downloading " + this.url + " to " + resolved);

        try {
            Files.createDirectories(resolved.getParent());
            Files.deleteIfExists(resolved);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to create directories for " + resolved + "!", exception);
        }

        try (InputStream inputStream = new URI(this.url).toURL().openStream()) {
            Files.write(resolved, inputStream.readAllBytes());
        } catch (IOException | URISyntaxException exception) {
            throw new RuntimeException("Failed to download " + this.url + "!", exception);
        }

        Switchboard.LOGGER.debug("Downloaded " + this.url + " to " + resolved + "!");
        return resolved;
    }
}
