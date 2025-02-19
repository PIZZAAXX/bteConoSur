package pizzaaxx.bteconosur.Utils;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ListUtils {

    public static boolean equals(@NotNull List<?> a, @NotNull List<?> b) {

        if (a.size() != b.size()) {
            return false;
        }

        int counter = 0;
        for (Object oA : a) {
            if (!oA.equals(b.get(counter))) {
                return false;
            }
        }
        return true;

    }
}
