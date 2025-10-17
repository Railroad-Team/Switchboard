package dev.railroadide.switchboard.minecraft.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.MinecraftVersionService;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

// TODO: Rewrite because we cant get minecraft version
public class FabricLoaderVersionService extends MinecraftVersionService<FabricLoaderVersionService.FabricLoaderVersion> {
    private static final String LOADER_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/loader/%s";
    private static final String LOADER_VERSION_URL = "https://meta.fabricmc.net/v2/versions/loader/%s/%s";
    private static final String ALL_LOADER_VERSIONS_URL = "https://meta.fabricmc.net/v2/versions/loader";

    private FabricLoaderVersion latestCached;

    public FabricLoaderVersionService() {
        super("FabricLoader");
    }

    private static String cacheKeyFor(MinecraftVersion minecraftVersion) {
        return "mc:" + minecraftVersion.id();
    }

    public FabricLoaderVersion latest() {
        if (latestCached != null)
            return latestCached;

        List<MinecraftVersion> minecraftVersions = MinecraftVersion.getVersions();
        for (int i = minecraftVersions.size() - 1; i >= 0; i--) {
            MinecraftVersion minecraftVersion = minecraftVersions.get(i);
            FabricLoaderVersion latestVersion = getLatestVersion(minecraftVersion);
            if (latestVersion != null) {
                latestCached = latestVersion;
                return latestVersion;
            }
        }

        return null;
    }

    public FabricLoaderVersion getLatestVersion(MinecraftVersion minecraftVersion) {
        return latestFor(minecraftVersion).orElse(null);
    }

    public Optional<FabricLoaderVersion> findVersion(MinecraftVersion minecraftVersion, String version) {
        if (version == null || version.isEmpty())
            return Optional.empty();

        return listVersionsFor(minecraftVersion, true).stream()
                .filter(loaderVersion -> version.equals(loaderVersion.version()))
                .findFirst()
                .or(() -> fetchSingleVersion(minecraftVersion, version));
    }

    @Override
    public Optional<FabricLoaderVersion> latestFor(MinecraftVersion minecraftVersion) {
        return latestFor(minecraftVersion, false);
    }

    @Override
    public Optional<FabricLoaderVersion> latestFor(MinecraftVersion minecraftVersion, boolean includePrereleases) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        return listVersionsFor(minecraftVersion, includePrereleases).stream().findFirst();
    }

    @Override
    public List<FabricLoaderVersion> listAllVersions() {
        return listAllVersions(false);
    }

    @Override
    public List<FabricLoaderVersion> listAllVersions(boolean includePrereleases) {
        return allVersions();
    }

    @Override
    public List<FabricLoaderVersion> listVersionsFor(MinecraftVersion minecraftVersion) {
        return listVersionsFor(minecraftVersion, false);
    }

    @Override
    public List<FabricLoaderVersion> listVersionsFor(MinecraftVersion minecraftVersion, boolean includePrereleases) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        return versionsFor(minecraftVersion);
    }

    @Override
    public void forceRefresh(boolean includePrereleases) {
        try {
            cache.remove("all");
            allVersions();
            MinecraftVersion.getVersions().forEach(version -> cache.remove(cacheKeyFor(version)));
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Failed to refresh Fabric loader versions", exception);
        }
    }

    private List<FabricLoaderVersion> allVersions() {
        CacheEntry<List<FabricLoaderVersion>> entry = cache.get("all");
        if (entry != null && entry.isActive())
            return entry.value();

        try {
            List<FabricLoaderVersion> fresh = fetchAllVersions();
            cache.put("all", new CacheEntry<>(fresh, Instant.now().plus(ttl)));
            return fresh;
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Failed to load Fabric loader versions", exception);
            return entry != null ? entry.value() : List.of();
        }
    }

    private List<FabricLoaderVersion> versionsFor(MinecraftVersion minecraftVersion) {
        String cacheKey = cacheKeyFor(minecraftVersion);
        CacheEntry<List<FabricLoaderVersion>> entry = cache.get(cacheKey);
        if (entry != null && entry.isActive())
            return entry.value();

        try {
            List<FabricLoaderVersion> fresh = fetchVersionsFor(minecraftVersion);
            cache.put(cacheKey, new CacheEntry<>(fresh, Instant.now().plus(ttl)));
            return fresh;
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Failed to load Fabric versions for Minecraft {}", minecraftVersion.id(), exception);
            return entry != null ? entry.value() : List.of();
        }
    }

    private Optional<FabricLoaderVersion> fetchSingleVersion(MinecraftVersion minecraftVersion, String version) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LOADER_VERSION_URL.formatted(minecraftVersion.id(), version)))
                    .header("User-Agent", userAgent)
                    .timeout(DEFAULT_HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200)
                return Optional.empty();

            JsonObject versionObject = Switchboard.GSON.fromJson(response.body(), JsonObject.class);
            if (versionObject == null || !versionObject.isJsonObject())
                return Optional.empty();

            FabricLoaderVersion loaderVersion = Switchboard.GSON.fromJson(versionObject, FabricLoaderVersion.class);
            return Optional.ofNullable(loaderVersion);
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Failed to load Fabric loader version {} for Minecraft {}", version, minecraftVersion.id(), exception);
            return Optional.empty();
        }
    }

    private List<FabricLoaderVersion> fetchVersionsFor(MinecraftVersion minecraftVersion) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LOADER_VERSIONS_URL.formatted(minecraftVersion.id())))
                .header("User-Agent", userAgent)
                .timeout(DEFAULT_HTTP_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Fabric loader versions HTTP " + response.statusCode());

        JsonArray jsonVersions = Switchboard.GSON.fromJson(response.body(), JsonArray.class);
        return parseArray(jsonVersions); // TODO: Represent the entire json structure properly
    }

    private List<FabricLoaderVersion> fetchAllVersions() throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(ALL_LOADER_VERSIONS_URL))
                .header("User-Agent", userAgent)
                .timeout(DEFAULT_HTTP_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Fabric loader versions HTTP " + response.statusCode());

        JsonArray jsonVersions = Switchboard.GSON.fromJson(response.body(), JsonArray.class);
        return parseArray(jsonVersions);
    }

    private List<FabricLoaderVersion> parseArray(JsonArray jsonArray) {
        if (jsonArray == null)
            return List.of();

        List<FabricLoaderVersion> versions = new ArrayList<>();
        for (JsonElement element : jsonArray) {
            if (!element.isJsonObject())
                continue;

            JsonObject obj = element.getAsJsonObject();
            if(obj.has("loader")) {
                JsonElement loaderElement = obj.get("loader");
                if (!loaderElement.isJsonObject())
                    continue;

                obj = loaderElement.getAsJsonObject();
            }

            versions.add(Switchboard.GSON.fromJson(obj, FabricLoaderVersion.class));
        }

        return List.copyOf(versions);
    }

    public record FabricLoaderVersion(String separator, int build, String maven, String version, boolean stable) {
    }
}
