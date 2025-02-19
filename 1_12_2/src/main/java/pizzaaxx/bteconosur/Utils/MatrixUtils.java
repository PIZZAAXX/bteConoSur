package pizzaaxx.bteconosur.Utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class MatrixUtils {

    @NotNull
    @Contract(pure = true)
    public static double[] add(@NotNull double[] v1, double[] v2) {
        double[] result = new double[v1.length];

        for (int i = 0; i < v1.length; i++) {
            result[i] = v1[i] + v2[i];
        }

        return result;
    }

    @NotNull
    @Contract(pure = true)
    public static double[] subtract(@NotNull double[] v1, double[] v2) {
        double[] result = new double[v1.length];

        for (int i = 0; i < v1.length; i++) {
            result[i] = v1[i] - v2[i];
        }

        return result;
    }

    @NotNull
    @Contract(pure = true)
    public static double[] multiply(@NotNull double[][] matrix, double[] vector) {

        double[] result = new double[matrix[0].length];

        for (double[] v : matrix) {
            int i = 0;
            for (double value : v) {
                result[i] += value * vector[i];
                i++;
            }
        }

        return result;

    }

}
