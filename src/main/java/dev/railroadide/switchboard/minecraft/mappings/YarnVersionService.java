package dev.railroadide.switchboard.minecraft.mappings;

import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.minecraft.MinecraftVersion;
import dev.railroadide.switchboard.minecraft.MinecraftVersionService;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class YarnVersionService extends MinecraftVersionService<String> {
    private static final String METADATA_URL = "https://maven.fabricmc.net/net/fabricmc/yarn/maven-metadata.xml";

    public YarnVersionService() {
        super("Yarn");
    }

    public YarnVersionService(Duration ttl) {
        super("Yarn", ttl);
    }

    public YarnVersionService(Duration ttl, String userAgent, HttpClient httpClient) {
        super("Yarn", ttl, userAgent, httpClient);
    }

    public YarnVersionService(Duration ttl, String userAgent, Duration httpTimeout) {
        super("Yarn", ttl, userAgent, httpTimeout);
    }

    private static Optional<MinecraftVersion> toMinecraftVersion(String yarnVersion) {
        int plus = yarnVersion.indexOf('+');
        return MinecraftVersion.fromId(plus > 0 ? yarnVersion.substring(0, plus) : yarnVersion);
    }

    @Override
    public Optional<String> latestFor(MinecraftVersion minecraftVersion) {
        return latestFor(minecraftVersion, false);
    }

    @Override
    public Optional<String> latestFor(MinecraftVersion minecraftVersion, boolean includePrereleases) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        List<String> forMc = listVersionsFor(minecraftVersion, includePrereleases);
        return forMc.isEmpty() ? Optional.empty() : Optional.of(forMc.getFirst());
    }

    @Override
    public List<String> listAllVersions() {
        return listAllVersions(false);
    }

    @Override
    public List<String> listAllVersions(boolean includePrereleases) {
        return versions().stream()
                .filter(v -> includePrereleases || toMinecraftVersion(v).map(MinecraftVersion::isRelease).orElse(false))
                .toList()
                .reversed();
    }

    @Override
    public List<String> listVersionsFor(MinecraftVersion minecraftVersion) {
        return listVersionsFor(minecraftVersion, false);
    }

    @Override
    public List<String> listVersionsFor(MinecraftVersion minecraftVersion, boolean includePrereleases) {
        Objects.requireNonNull(minecraftVersion, "minecraftVersion");
        return versions().stream()
                .filter(v -> toMinecraftVersion(v).map(minecraftVersion::equals).orElse(false))
                .filter(v -> includePrereleases || minecraftVersion.isRelease())
                .toList()
                .reversed();
    }

    @Override
    public void forceRefresh(boolean includePrereleases) {
        try {
            List<String> fresh = fetchAllVersionsFromMaven();
            cache.put("all", new CacheEntry<>(fresh, Instant.now().plus(ttl)));
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Failed to refresh Yarn versions", exception);
        }
    }

    private List<String> versions() {
        CacheEntry<List<String>> entry = cache.get("all");
        if (entry != null && entry.isActive())
            return entry.value();

        List<String> fresh = fetchAllVersionsFromMaven();
        cache.put("all", new CacheEntry<>(fresh, Instant.now().plus(ttl)));
        return fresh;
    }

    private List<String> fetchAllVersionsFromMaven() {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(METADATA_URL))
                    .header("User-Agent", userAgent)
                    .timeout(DEFAULT_HTTP_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<byte[]> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200)
                throw new RuntimeException("Maven metadata HTTP " + response.statusCode());

            byte[] xml = response.body();
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            dbf.setExpandEntityReferences(false);
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            Document doc = dbf.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
            NodeList nodes = doc.getElementsByTagName("version");

            List<String> versions = new ArrayList<>(nodes.getLength());
            for (int i = 0; i < nodes.getLength(); i++) {
                String val = nodes.item(i).getTextContent();
                if (val != null && !val.isBlank()) versions.add(val.trim());
            }

            return List.copyOf(versions);
        } catch (Exception exception) {
            throw new RuntimeException("Failed to fetch/parse Maven metadata", exception);
        }
    }
}
