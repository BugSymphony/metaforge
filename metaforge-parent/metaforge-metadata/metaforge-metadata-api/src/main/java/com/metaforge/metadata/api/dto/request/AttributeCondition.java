package com.metaforge.metadata.api.dto.request;

import com.metaforge.metadata.api.enums.MatchMode;

public class AttributeCondition {
    private String field;
    private String value;
    private MatchMode matchMode;

    public AttributeCondition() {}

    public String getField() { return field; }
    public void setField(String field) { this.field = field; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public MatchMode getMatchMode() { return matchMode; }
    public void setMatchMode(MatchMode matchMode) { this.matchMode = matchMode; }
}
