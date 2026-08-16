package com.metaforge.agent.cognition.core.domain.model.valueobject;

import java.util.*;

public class ScopeBehavior {

    private boolean acceptsScope;
    private boolean scopeRequired;
    private boolean producesUpdatedScope;
    private List<String> scopeFields = new ArrayList<>();

    public ScopeBehavior() {
    }

    public ScopeBehavior(boolean acceptsScope, boolean scopeRequired, boolean producesUpdatedScope, List<String> scopeFields) {
        this.acceptsScope = acceptsScope;
        this.scopeRequired = scopeRequired;
        this.producesUpdatedScope = producesUpdatedScope;
        this.scopeFields = scopeFields != null ? scopeFields : new ArrayList<>();
    }

    /**
     * FR-008: scopeRequired=true 自动修正 acceptsScope=true
     */
    public void validate() {
        if (scopeRequired && !acceptsScope) {
            this.acceptsScope = true;
        }
    }

    public boolean isAcceptsScope() {
        return acceptsScope;
    }

    public void setAcceptsScope(boolean acceptsScope) {
        this.acceptsScope = acceptsScope;
    }

    public boolean isScopeRequired() {
        return scopeRequired;
    }

    public void setScopeRequired(boolean scopeRequired) {
        this.scopeRequired = scopeRequired;
    }

    public boolean isProducesUpdatedScope() {
        return producesUpdatedScope;
    }

    public void setProducesUpdatedScope(boolean producesUpdatedScope) {
        this.producesUpdatedScope = producesUpdatedScope;
    }

    public List<String> getScopeFields() {
        return scopeFields;
    }

    public void setScopeFields(List<String> scopeFields) {
        this.scopeFields = scopeFields != null ? scopeFields : new ArrayList<>();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScopeBehavior that)) return false;
        return acceptsScope == that.acceptsScope && scopeRequired == that.scopeRequired
                && producesUpdatedScope == that.producesUpdatedScope && Objects.equals(scopeFields, that.scopeFields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(acceptsScope, scopeRequired, producesUpdatedScope, scopeFields);
    }

    @Override
    public String toString() {
        return "ScopeBehavior{accepts=" + acceptsScope + ", required=" + scopeRequired
                + ", produces=" + producesUpdatedScope + ", fields=" + scopeFields + '}';
    }
}
