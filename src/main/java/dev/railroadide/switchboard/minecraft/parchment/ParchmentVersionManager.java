package dev.railroadide.switchboard.minecraft.parchment;

import dev.railroadide.switchboard.Environment;
import dev.railroadide.switchboard.Switchboard;
import dev.railroadide.switchboard.util.Cache;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.stream.Stream;

public class ParchmentVersionManager {
    protected final Cache<String, List<ParchmentVersion>> cache = new Cache<>(Duration.ofHours(3));

    private static List<ParchmentVersion> fetchAllVersions() {
        Path parchmentClonePath = Environment.getParchmentClonePath();
        if(!deleteDirectory(parchmentClonePath))
            return Collections.emptyList();

        try {
            Files.createDirectories(parchmentClonePath);
            Files.createDirectories(parchmentClonePath.resolve(".git/objects"));
        } catch (IOException exception) {
            Switchboard.LOGGER.error("Failed to create Parchment clone directory", exception);
            return Collections.emptyList();
        }

        try (Git git = Git.cloneRepository()
                .setURI("https://github.com/ParchmentMC/Parchment.git")
                .setDirectory(parchmentClonePath.toFile())
                .setNoCheckout(true)
                .call()) {

            List<ParchmentVersion> tags = new ArrayList<>();
            for (Ref ref : git.tagList().call().stream().toList()) {
                String parchmentName = ref.getName().replace("refs/tags/releases/", "");
                String[] split = parchmentName.split("-");

                if (split.length < 2) {
                    Switchboard.LOGGER.warn("Skipping invalid Parchment tag: " + parchmentName);
                    continue;
                }

                String minecraftVersion = split[0];
                String versionPart = split[1];
                tags.add(new ParchmentVersion(versionPart, minecraftVersion, true));
            }

            return tags;
        } catch (GitAPIException exception) {
            Switchboard.LOGGER.error("Failed to clone Parchment repository", exception);
            return Collections.emptyList();
        }
    }

    private static boolean deleteDirectory(Path path) {
        if (Files.notExists(path)) return true;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception exception) {
                            Switchboard.LOGGER.warn("Failed to delete {}", p, exception);
                        }
                    });
            return true;
        } catch (Exception exception) {
            Switchboard.LOGGER.error("Error walking {}", path, exception);
            return false;
        }
    }

    public List<ParchmentVersion> listAllVersions() {
        return cache.get("all", ParchmentVersionManager::fetchAllVersions).stream().sorted().toList();
    }

    public List<ParchmentVersion> listVersionsFor(String minecraftVersion) {
        List<ParchmentVersion> allVersions = listAllVersions();
        List<ParchmentVersion> filtered = new ArrayList<>();
        for (ParchmentVersion version : allVersions) {
            if (version.minecraftVersion().equals(minecraftVersion)) {
                filtered.add(version);
            }
        }

        return filtered;
    }

    public Map<String, List<ParchmentVersion>> getAllVersionsGroupedByMinecraftVersion() {
        List<ParchmentVersion> allVersions = listAllVersions();
        Map<String, List<ParchmentVersion>> grouped = new TreeMap<>();
        for (ParchmentVersion version : allVersions) {
            grouped.computeIfAbsent(version.minecraftVersion(), _ -> new ArrayList<>()).add(version);
        }

        return grouped;
    }

    public Optional<ParchmentVersion> latestFor(String minecraftVersion) {
        return listVersionsFor(minecraftVersion).stream().max(Comparator.comparing(ParchmentVersion::version));
    }

    public ParchmentVersion latestOverall() {
        return listAllVersions().stream()
                .max(Comparator.comparing(ParchmentVersion::version))
                .orElse(null);
    }

    public void forceRefresh() {
        cache.clear();
        listAllVersions();
    }
}
