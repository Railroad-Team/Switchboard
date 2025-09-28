package dev.railroadide.switchboard.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class FileUtils {
    private FileUtils() {}

    public static void writeUrlBody(String url, Path path) {
        try (var in = new URI(url).toURL().openStream()) {
            Files.createDirectories(path.getParent());
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new RuntimeException("Failed to write body from URL: " + url + " to path: " + path, exception);
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Invalid URL: " + url, exception);
        }
    }
}
