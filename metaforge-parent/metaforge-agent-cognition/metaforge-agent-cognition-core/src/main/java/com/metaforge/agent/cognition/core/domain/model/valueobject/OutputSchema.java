package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.*;

public class OutputSchema {

    private String type;
    private List<String> formats = new ArrayList<>();

    public OutputSchema() {
    }

    public OutputSchema(String type, List<String> formats) {
        this.type = type;
        this.formats = formats != null ? formats : new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public List<String> getFormats() {
        return formats;
    }

    public void setFormats(List<String> formats) {
        this.formats = formats != null ? formats : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutputSchema that)) return false;
        return Objects.equals(type, that.type) && Objects.equals(formats, that.formats);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, formats);
    }

    @Override
    public String toString() {
        return "OutputSchema{type=" + type + ", formats=" + formats + '}';
    }
}
