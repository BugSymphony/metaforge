package com.metaforge.metamodel.domain.model.valueobject;

import com.metaforge.metamodel.api.enums.UpgradeLevel;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语义化版本号值对象（SemVer 2.0）。
 * 支持解析、比较与 bump（递增）操作。
 */
public final class SemanticVersion implements Comparable<SemanticVersion> {

    private static final Pattern SEMVER_PATTERN =
            Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-(.+))?$");

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    private SemanticVersion(int major, int minor, int patch, String preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }

    /**
     * 解析 SemVer 2.0 格式字符串。
     *
     * @param version 版本字符串（如 "1.0.0" 或 "1.0.0-alpha"）
     * @return SemanticVersion 实例
     * @throws IllegalArgumentException 如果格式不合法
     */
    public static SemanticVersion parse(String version) {
        if (version == null) {
            throw new IllegalArgumentException("版本号不能为 null");
        }
        Matcher matcher = SEMVER_PATTERN.matcher(version.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("版本号格式不合法: " + version);
        }
        return new SemanticVersion(
                Integer.parseInt(matcher.group(1)),
                Integer.parseInt(matcher.group(2)),
                Integer.parseInt(matcher.group(3)),
                matcher.group(4)
        );
    }

    /**
     * 创建初始版本 0.0.0。
     */
    public static SemanticVersion initial() {
        return new SemanticVersion(0, 0, 0, null);
    }

    /**
     * 按升级等级递增版本号。
     * MAJOR: major+1, minor/patch 归零；MINOR: minor+1, patch 归零；PATCH: patch+1。
     * preRelease 在 bump 时清空。
     */
    public SemanticVersion bump(UpgradeLevel level) {
        return switch (level) {
            case MAJOR -> new SemanticVersion(major + 1, 0, 0, null);
            case MINOR -> new SemanticVersion(major, minor + 1, 0, null);
            case PATCH -> new SemanticVersion(major, minor, patch + 1, null);
        };
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String getPreRelease() {
        return preRelease;
    }

    /**
     * 返回标准版本字符串格式 MAJOR.MINOR.PATCH。
     */
    public String toVersionString() {
        if (preRelease != null) {
            return major + "." + minor + "." + patch + "-" + preRelease;
        }
        return major + "." + minor + "." + patch;
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int cmp = Integer.compare(this.major, other.major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.minor, other.minor);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.patch, other.patch);
        if (cmp != 0) return cmp;
        if (this.preRelease == null && other.preRelease == null) return 0;
        if (this.preRelease == null) return 1;
        if (other.preRelease == null) return -1;
        return this.preRelease.compareTo(other.preRelease);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticVersion that)) return false;
        return major == that.major && minor == that.minor && patch == that.patch
                && Objects.equals(preRelease, that.preRelease);
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, patch, preRelease);
    }

    @Override
    public String toString() {
        return toVersionString();
    }
}
