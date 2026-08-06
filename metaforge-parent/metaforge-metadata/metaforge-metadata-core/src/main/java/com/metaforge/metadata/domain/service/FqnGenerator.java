package com.metaforge.metadata.domain.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class FqnGenerator {

    private static final Pattern SEGMENT_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_-]*$");
    private static final String RESERVED_CHAR = ".";

    public String generateChildFqn(String parentFqn, String segment) {
        if (parentFqn == null || parentFqn.isEmpty()) {
            return segment;
        }
        return parentFqn + "." + segment;
    }

    public String extractParentFqn(String fqn) {
        if (fqn == null || fqn.isEmpty()) return null;
        int lastDot = fqn.lastIndexOf('.');
        return lastDot > 0 ? fqn.substring(0, lastDot) : null;
    }

    public List<String> splitSegments(String fqn) {
        if (fqn == null || fqn.isEmpty()) return List.of();
        return Arrays.asList(fqn.split("\\."));
    }

    public String extractRootFqn(String fqn) {
        if (fqn == null || fqn.isEmpty()) return fqn;
        int firstDot = fqn.indexOf('.');
        return firstDot > 0 ? fqn.substring(0, firstDot) : fqn;
    }

    public boolean isValidSegment(String segment) {
        return segment != null && SEGMENT_PATTERN.matcher(segment).matches();
    }

    public boolean isReservedCharInSegment(String segment) {
        return segment != null && segment.contains(RESERVED_CHAR);
    }
}
