package categorizer.constant;

public enum Category {
    DIFF_VARNAMES_AND_LITERALS("Exact match except variable names, constants and literals"),
    NOT_RESOLVABLE("Unresolvable category");

    private String description;

    Category(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
