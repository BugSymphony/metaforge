package com.metaforge.agent.cognition.core.domain.model.valueobject;

public record Priority(int value) {

    public static final Priority DEFAULT = new Priority(0);

    public Priority {
        if (value < 0) {
            throw new IllegalArgumentException("priority 必须 >= 0，当前值: " + value);
        }
    }

    public static Priority of(int value) {
        return value == 0 ? DEFAULT : new Priority(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
