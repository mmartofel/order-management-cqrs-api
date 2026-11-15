package pl.piomin.services.api;

/** A case-insensitive "contains" pattern for {@code LIKE ... ESCAPE '\'}, with the input's wildcards escaped. */
public final class Like {

    public static final char ESCAPE = '\\';

    private Like() {
    }

    public static String contains(String text) {
        String escaped = text.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
