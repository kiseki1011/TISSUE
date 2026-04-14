package com.tissue.feature.wiki.domain.vo;

import com.tissue.feature.wiki.domain.enums.SemanticUpdateType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.Objects;
import lombok.Getter;

@Getter
@Embeddable
public class SnapshotVersion implements Comparable<SnapshotVersion> {

    @Column(name = "major_version", nullable = false)
    private int major;

    @Column(name = "minor_version", nullable = false)
    private int minor;

    @Column(name = "patch_version", nullable = false)
    private int patch;

    @SuppressWarnings("NullAway.Init")
    protected SnapshotVersion() {}

    public SnapshotVersion(int major, int minor, int patch) {
        validate(major, minor, patch);
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public static SnapshotVersion initial() {
        return new SnapshotVersion(1, 0, 0);
    }

    public SnapshotVersion bumpVersion(SemanticUpdateType versionUpdateType) {
        return switch (versionUpdateType) {
            case MAJOR -> increaseMajor();
            case MINOR -> increaseMinor();
            case PATCH -> increasePatch();
        };
    }

    private SnapshotVersion increaseMajor() {
        return new SnapshotVersion(this.major + 1, 0, 0);
    }

    private SnapshotVersion increaseMinor() {
        return new SnapshotVersion(this.major, this.minor + 1, 0);
    }

    private SnapshotVersion increasePatch() {
        return new SnapshotVersion(this.major, this.minor, this.patch + 1);
    }

    private void validate(int major, int minor, int patch) {
        if (major < 0 || minor < 0 || patch < 0) {
            throw new IllegalArgumentException("Version numbers must be non-negative");
        }
    }

    @Override
    public int compareTo(SnapshotVersion other) {
        if (this.major != other.major) {
            return Integer.compare(this.major, other.major);
        }
        if (this.minor != other.minor) {
            return Integer.compare(this.minor, other.minor);
        }
        return Integer.compare(this.patch, other.patch);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SnapshotVersion that = (SnapshotVersion) o;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch);
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
