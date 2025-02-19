package pizzaaxx.bteconosur.SQL;

/**
 * Defines a class that can be parsed into a JSON string on a specific way.
 * <p></p>
 * Classes that implement this interface will automatically be parsed when passed as a value.
 */
public interface JSONParsable {

    /**
     *
     * @param insideJSON Whether the object is inside a JSON object or not.
     * @return The {@link String} representation of this object.
     */
    String getJSON(boolean insideJSON);

}
