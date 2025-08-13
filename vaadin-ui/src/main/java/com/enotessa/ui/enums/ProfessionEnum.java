package com.enotessa.ui.enums;

public enum ProfessionEnum {
    JAVA_JUNIOR("Java Junior"),
    JAVA_MIDDLE("Java Middle"),
    JAVA_SENIOR("Java Senior"),
    KOTLIN_JUNIOR("Kotlin Junior");

    private final String displayName;

    ProfessionEnum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
