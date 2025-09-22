package dev.railroadide.switchboard.parchment;

import java.time.LocalDate;

public record ParchmentVersion(String version, String minecraftVersion, boolean isStable) implements Comparable<ParchmentVersion> {
    @Override
    public int compareTo(ParchmentVersion other) {
        return parseVersion().compareTo(other.parseVersion());
    }

    private LocalDate parseVersion() {
        return LocalDate.parse(version.replace(".", "-"));
    }
}
