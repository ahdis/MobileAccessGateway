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
}
