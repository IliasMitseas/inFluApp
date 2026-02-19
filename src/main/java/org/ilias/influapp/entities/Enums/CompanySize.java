package org.ilias.influapp.entities.Enums;

public enum CompanySize {
    STARTUP("Startup (1-10 employees)"),
    SMALL("Small Business (11-50 employees)"),
    MEDIUM("Medium Business (51-250 employees)"),
    LARGE("Large Enterprise (251-1000 employees)"),
    ENTERPRISE("Enterprise (1000+ employees)");

    private final String displayName;

    CompanySize(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}

