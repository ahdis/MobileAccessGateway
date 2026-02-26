package ch.bfh.ti.i4mi.mag.common;

import jakarta.annotation.Nullable;

import java.util.List;

public class JavaUtils {
    /**
     * This class is not instantiable.
     */
    private JavaUtils() {
    }

    @Nullable
    public static <T> T firstOrNull(final @Nullable List<T> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.getFirst();
    }

    public static String hidePasswordInToString(final @Nullable String password) {
        if (password == null) {
            return "null";
        } else if (password.isBlank()) {
            return "<empty string>";
        }
        return "********";
    }
}
