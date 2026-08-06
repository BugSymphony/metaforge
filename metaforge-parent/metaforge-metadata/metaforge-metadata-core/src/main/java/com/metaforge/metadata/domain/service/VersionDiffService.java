package com.metaforge.metadata.domain.service;

import com.metaforge.metadata.api.dto.response.FieldDiff;
import com.metaforge.metadata.api.enums.DiffType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

@Component
public class VersionDiffService {

    public List<FieldDiff> compare(Map<String, Object> contentA, Map<String, Object> contentB) {
        Map<String, Object> flatA = flatten(contentA, "");
        Map<String, Object> flatB = flatten(contentB, "");

        Set<String> allKeys = new TreeSet<>();
        allKeys.addAll(flatA.keySet());
        allKeys.addAll(flatB.keySet());

        List<FieldDiff> diffs = new ArrayList<>();

        for (String key : allKeys) {
            boolean inA = flatA.containsKey(key);
            boolean inB = flatB.containsKey(key);

            if (inA && !inB) {
                FieldDiff diff = new FieldDiff();
                diff.setPath(key);
                diff.setDiffType(DiffType.DELETED);
                diff.setOldValue(flatA.get(key));
                diff.setNewValue(null);
                diffs.add(diff);
            } else if (!inA && inB) {
                FieldDiff diff = new FieldDiff();
                diff.setPath(key);
                diff.setDiffType(DiffType.ADDED);
                diff.setOldValue(null);
                diff.setNewValue(flatB.get(key));
                diffs.add(diff);
            } else if (!Objects.equals(flatA.get(key), flatB.get(key))) {
                FieldDiff diff = new FieldDiff();
                diff.setPath(key);
                diff.setDiffType(DiffType.MODIFIED);
                diff.setOldValue(flatA.get(key));
                diff.setNewValue(flatB.get(key));
                diffs.add(diff);
            }
        }

        return diffs;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(Map<String, Object> map, String prefix) {
        Map<String, Object> result = new HashMap<>();
        if (map == null) {
            return result;
        }

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                result.putAll(flatten((Map<String, Object>) value, key));
            } else {
                result.put(key, value);
            }
        }

        return result;
    }
}
