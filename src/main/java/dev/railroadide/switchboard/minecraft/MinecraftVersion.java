package dev.railroadide.switchboard.minecraft;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Environment;
import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.pistonmeta.VersionPackage;
import dev.railroadide.switchboard.util.FileUtils;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class MinecraftVersion implements Comparable<MinecraftVersion> {
    private static final String MINECRAFT_VERSIONS_URL = "https://launchermeta.mojang.com/mc/game/version_manifest.json";

    private static final List<MinecraftVersion> MINECRAFT_VERSIONS = new ArrayList<>();
    private static MinecraftVersion latestStable;
    private static MinecraftVersion latestSnapshot;
    private final String id;
    private final VersionType type;
    private final String url;
    private final LocalDateTime time;
    private final LocalDateTime releaseTime;

    private final transient Path pistonMetaPath;

    public MinecraftVersion(String id, VersionType type, String url, LocalDateTime time, LocalDateTime releaseTime) {
        this.id = id;
        this.type = type;
        this.url = url;
        this.time = time;
        this.releaseTime = releaseTime;

        this.pistonMetaPath = Environment.getCacheLocation().resolve("piston-meta").resolve(this.id + ".json");
    }

    public MinecraftVersion getPreviousVersion() {
        int index = MINECRAFT_VERSIONS.indexOf(this);
        if (index == -1 || index == MINECRAFT_VERSIONS.size() - 1)
            return null;

        return MINECRAFT_VERSIONS.get(index + 1);
    }

    public MinecraftVersion getNextVersion() {
        int index = MINECRAFT_VERSIONS.indexOf(this);
        if (index <= 0)
            return null;

        return MINECRAFT_VERSIONS.get(index - 1);
    }

    public static List<MinecraftVersion> getVersionsAfter(MinecraftVersion minecraftVersion) {
        return getVersionsAfter(minecraftVersion, false);
    }

    public static List<MinecraftVersion> getVersionsAfter(MinecraftVersion minecraftVersion, boolean inclusive) {
        int index = MINECRAFT_VERSIONS.indexOf(minecraftVersion);
        if (index == -1 || index == MINECRAFT_VERSIONS.size() - 1)
            return List.of();

        return List.copyOf(MINECRAFT_VERSIONS.subList(0, inclusive ? index + 1 : index));
    }

    public static MinecraftVersion determineBestFit(List<MinecraftVersion> versions) {
        return versions.stream()
                .filter(MinecraftVersion::isRelease)
                .max(Comparator.naturalOrder())
                .orElse(versions.isEmpty() ? null : versions.getFirst());
    }

    public static void requestMinecraftVersions() {
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder().GET().uri(new URI(MINECRAFT_VERSIONS_URL)).build();
        } catch (URISyntaxException exception) {
            throw new RuntimeException("Failed to create HTTP request for Minecraft versions", exception);
        }

        Switchboard.HTTP_CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAcceptAsync(response -> {
            if (response.statusCode() != 200)
                throw new RuntimeException("Failed to request Minecraft versions: HTTP " + response.statusCode());

            String json = response.body();
            if (json.isBlank())
                throw new RuntimeException("Failed to request Minecraft versions: Empty JSON response");

            JsonObject object = Switchboard.GSON.fromJson(json, JsonObject.class);

            List<MinecraftVersion> versions = new ArrayList<>();
            JsonArray versionsArray = object.getAsJsonArray("versions");
            for (JsonElement jsonElement : versionsArray) {
                JsonObject versionObject = jsonElement.getAsJsonObject();
                if (!versionObject.has("id") || !versionObject.has("type") || !versionObject.has("url") || !versionObject.has("time") || !versionObject.has("releaseTime")) {
                    Switchboard.LOGGER.warn("Skipping Minecraft version due to missing fields: " + versionObject);
                    continue;
                }

                String id = versionObject.get("id").getAsString();
                var type = VersionType.fromString(versionObject.get("type").getAsString());
                if (type.isEmpty()) {
                    Switchboard.LOGGER.warn("Unknown Minecraft version type: " + versionObject.get("type").getAsString());
                    continue;
                }

                String url = versionObject.get("url").getAsString();
                String time = versionObject.get("time").getAsString().split("\\+")[0]; // Remove timezone (e.g. +00:00)
                String releaseTime = versionObject.get("releaseTime").getAsString().split("\\+")[0]; // Remove timezone (e.g. +00:00)
                versions.add(new MinecraftVersion(id, type.orElseThrow(), url, LocalDateTime.parse(time), LocalDateTime.parse(releaseTime)));
            }

            MINECRAFT_VERSIONS.clear();
            MINECRAFT_VERSIONS.addAll(versions);

            JsonObject latestObject = object.getAsJsonObject("latest");
            String latestStableId = latestObject.get("release").getAsString();
            String latestSnapshotId = latestObject.get("snapshot").getAsString();

            latestStable = MINECRAFT_VERSIONS.stream()
                    .filter(version -> version.id.equals(latestStableId))
                    .findFirst()
                    .orElse(null);

            latestSnapshot = MINECRAFT_VERSIONS.stream()
                    .filter(version -> version.id.equals(latestSnapshotId))
                    .findFirst()
                    .orElse(null);
        });
    }

    public CompletableFuture<VersionPackage> requestPistonMeta() {
        if (Files.exists(this.pistonMetaPath))
            return CompletableFuture.supplyAsync(() -> VersionPackage.fromFile(this.pistonMetaPath));

        CompletableFuture<VersionPackage> future = new CompletableFuture<>();
        future.completeAsync(() -> {
            try {
                FileUtils.writeUrlBody(this.url, this.pistonMetaPath);
                return VersionPackage.fromFile(this.pistonMetaPath);
            } catch (Exception exception) {
                throw new RuntimeException("Failed to request Piston meta for Minecraft version " + this.id, exception);
            }
        });

        return future;
    }

    public static MinecraftVersion getLatestStableVersion() {
        return latestStable;
    }

    public static MinecraftVersion getLatestSnapshotVersion() {
        return latestSnapshot;
    }

    public static Optional<MinecraftVersion> getLatestVersion() {
        if (latestStable == null && latestSnapshot == null)
            return Optional.empty();

        if (latestStable == null)
            return Optional.of(latestSnapshot);

        if (latestSnapshot == null)
            return Optional.of(latestStable);

        return Optional.of(latestStable.releaseTime.isAfter(latestSnapshot.releaseTime) ? latestStable : latestSnapshot);
    }

    public static Optional<MinecraftVersion> getLatestVersion(VersionType type) {
        return switch (type) {
            case RELEASE -> Optional.ofNullable(latestStable);
            case SNAPSHOT -> Optional.ofNullable(latestSnapshot);
            case OLD_BETA, OLD_ALPHA -> {
                for (int i = MINECRAFT_VERSIONS.size() - 1; i >= 0; i--) {
                    MinecraftVersion version = MINECRAFT_VERSIONS.get(i);
                    if (version.type == type)
                        yield Optional.of(version);
                }

                yield Optional.empty();
            }
        };
    }

    public static Optional<MinecraftVersion> fromId(String id) {
        return MINECRAFT_VERSIONS.stream()
                .filter(version -> version.id.equals(id))
                .findFirst();
    }

    public static boolean isLatest(MinecraftVersion mcVersion) {
        return mcVersion.equals(latestStable) || mcVersion.equals(latestSnapshot);
    }

    private static Optional<MinecraftVersion> findClosestRelease(MinecraftVersion minecraftVersion) {
        if (minecraftVersion.isRelease())
            return Optional.of(minecraftVersion);

        int index = MINECRAFT_VERSIONS.indexOf(minecraftVersion);
        if (index == -1)
            return Optional.empty();

        for (int i = index - 1; i >= 0; i--) {
            MinecraftVersion version = MINECRAFT_VERSIONS.get(i);
            if (version.isRelease())
                return Optional.of(version);
        }

        for (int i = index + 1; i < MINECRAFT_VERSIONS.size(); i++) {
            MinecraftVersion version = MINECRAFT_VERSIONS.get(i);
            if (version.isRelease())
                return Optional.of(version);
        }

        return Optional.empty();
    }

    public static Optional<MinecraftVersion> getMajorVersion(MinecraftVersion minecraftVersion) {
        if (!minecraftVersion.isRelease())
            return findClosestRelease(minecraftVersion).flatMap(minecraftVersion1 -> getMajorVersion(minecraftVersion1));

        String[] split = minecraftVersion.id.split("\\.");
        if (split.length < 2)
            return Optional.empty();

        String majorVersion = split[0] + "." + split[1];
        return fromId(majorVersion);
    }

    public static List<MinecraftVersion> getVersions() {
        return List.copyOf(MINECRAFT_VERSIONS);
    }

    public boolean isRelease() {
        return type == VersionType.RELEASE;
    }

    public String getMajorVersion() {
        if (!isRelease()) {
            Optional<MinecraftVersion> closestRelease = findClosestRelease(this);
            if (closestRelease.isEmpty())
                return id;

            return closestRelease.get().getMajorVersion();
        }

        String[] split = id.split("\\.");
        if (split.length < 2)
            return id;

        return split[0] + "." + split[1];
    }

    @Override
    public @NotNull String toString() {
        return "MinecraftVersion[" +
                "id=" + id + ", " +
                "type=" + type + ", " +
                "url=" + url + ", " +
                "time=" + time + ", " +
                "releaseTime=" + releaseTime + ']';
    }

    @Override
    public int compareTo(@NotNull MinecraftVersion other) {
        return this.releaseTime.compareTo(other.releaseTime);
    }

    public String id() {
        return id;
    }

    public VersionType type() {
        return type;
    }

    public String url() {
        return url;
    }

    public LocalDateTime time() {
        return time;
    }

    public LocalDateTime releaseTime() {
        return releaseTime;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (MinecraftVersion) obj;
        return Objects.equals(this.id, that.id) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.url, that.url) &&
                Objects.equals(this.time, that.time) &&
                Objects.equals(this.releaseTime, that.releaseTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, url, time, releaseTime);
    }

    public enum VersionType {
        RELEASE,
        SNAPSHOT,
        OLD_BETA,
        OLD_ALPHA;

        public static Optional<VersionType> fromString(String type) {
            return Optional.ofNullable(switch (type.toLowerCase(Locale.ROOT)) {
                case "release" -> RELEASE;
                case "snapshot" -> SNAPSHOT;
                case "old_beta" -> OLD_BETA;
                case "old_alpha" -> OLD_ALPHA;
                default -> null;
            });
        }
    }
}
