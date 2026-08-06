package com.metaforge.computeengine.domain.model.valueobject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 图模式匹配路径模式值对象。
 *
 * <p>封装模式字符串的解析结果——路径段序列。支持通配符 '*'（匹配任意 EntitySchema FQN）
 * 和 '?'（匹配任意 RelationSchema FQN）。模式长度上限 4 段（3 条关系边）。
 *
 * @author metaforge
 */
public final class GraphPattern {

    private static final Pattern RELATION = Pattern.compile("\\[([^\\]]*)\\]");
    private static final Pattern SEGMENT = Pattern.compile("-\\s*\\[[^\\]]*\\]\\s*->");

    private final List<PatternSegment> segments;
    private final String rawPattern;

    public GraphPattern(String rawPattern) {
        if (rawPattern == null || rawPattern.isBlank()) {
            throw new IllegalArgumentException("图模式字符串不能为空");
        }
        this.rawPattern = rawPattern;
        this.segments = parsePattern(rawPattern);
        if (segments.size() > 4) {
            throw new IllegalArgumentException("图模式路径段数超过最大限制（4 段），当前段数: " + segments.size());
        }
    }

    private static List<PatternSegment> parsePattern(String pattern) {
        String[] entityParts = SEGMENT.split(pattern.trim());
        List<String> entities = new ArrayList<>();
        for (String part : entityParts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                entities.add(normalizeWildcard(trimmed));
            }
        }

        Matcher relationMatcher = RELATION.matcher(pattern);
        List<String> relationTypes = new ArrayList<>();
        while (relationMatcher.find()) {
            String relation = relationMatcher.group(1).trim();
            if (!relation.isEmpty()) {
                relationTypes.add(normalizeWildcard(relation));
            }
        }

        if (entities.size() != relationTypes.size() + 1) {
            throw new IllegalArgumentException("图模式格式非法（实体段与关系段数量不匹配）");
        }

        List<PatternSegment> result = new ArrayList<>();
        for (int i = 0; i < relationTypes.size(); i++) {
            result.add(new PatternSegment(entities.get(i), relationTypes.get(i), entities.get(i + 1)));
        }
        return Collections.unmodifiableList(result);
    }

    private static String normalizeWildcard(String s) {
        if (s == null) return null;
        s = s.trim();
        if ("*".equals(s)) return "*";
        if ("?".equals(s)) return "?";
        return s;
    }

    public List<PatternSegment> getSegments() {
        return segments;
    }

    public String getRawPattern() {
        return rawPattern;
    }

    public int getPatternLength() {
        return segments.size();
    }

    public boolean isValid() {
        return !segments.isEmpty() && segments.size() <= 4;
    }
}
