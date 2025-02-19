package pizzaaxx.bteconosur.Utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class StringUtils {

    public static String[] UPPER_CASE = {"A","B","C","D","E","F","G","H","I","J","K","L","M","N","O","P","Q","R","S","T","U","V","W","X","Y","Z"};
    public static String[] LOWER_CASE = {"a","b","c","d","e","f","g","h","i","j","k","l","m","n","o","p","q","r","s","t","u","v","w","x","y","z"};
    public static String[] DIGITS = {"1","2","3","4","5","6","7","8","9","0"};

    @NotNull
    public static String generateCode(int length, Collection<String> notIn, @NotNull String[]... characters) {
        List<String> chars = new ArrayList<>();
        for (String[] charactersList : characters) {
            chars.addAll(Arrays.asList(charactersList));
        }

        Random random = new Random();
        int charsLength = chars.size();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length; i++) {
            builder.append(chars.get(random.nextInt(charsLength)));
        }

        while (notIn.contains(builder.toString())) {
            builder = new StringBuilder();
            for (int i = 0; i < length; i++) {
                builder.append(chars.get(random.nextInt(charsLength)));
            }
        }

        return builder.toString();
    }

    @NotNull
    public static String insert(@NotNull String originalString, String insertedString, Integer... indexes) {

        Set<Integer> index = new HashSet<>(Arrays.asList(indexes));
        StringBuilder result = new StringBuilder(new String());

        for (int i = 0; i < originalString.length(); i++) {
            result.append(originalString.charAt(i));
            if (index.contains(i)) {
                result.append(insertedString);
            }
        }

        return result.toString();
    }

    @NotNull
    @Contract(pure = true)
    public static String getGenericPrefix(String color, String name) {
        return "§f[§" + color + name + "§f] §7>> §f";
    }

    public static Map<String, String> getQuery(String query) {
        Map<String, String> result = new HashMap<>();
        for (String section : query.split("&")) {
            String[] subsections = section.split("=");
            result.put(subsections[0], subsections[1]);
        }
        return result;
    }

    public static String getQuery(@NotNull String value, String key) {
        Map<String, String> query = StringUtils.getQuery(value.split("\\?")[1]);
        return query.get(key);
    }
}
