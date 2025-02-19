package pizzaaxx.bteconosur.Utils;

public class NumberUtils {

    public static double getInNewRange(double oldMin, double oldMax, double newMin, double newMax, double number) {
        double oldRange = oldMax - oldMin;
        double newRange = newMax - newMin;
        return (((number - oldMin) * newRange) / oldRange) + newMin;
    }

}
