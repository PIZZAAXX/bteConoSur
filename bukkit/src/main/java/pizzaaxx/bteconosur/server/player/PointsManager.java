package pizzaaxx.bteconosur.server.player;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import pizzaaxx.bteconosur.country.CountryPlayer;
import pizzaaxx.bteconosur.country.OldCountry;
import pizzaaxx.bteconosur.yaml.Configuration;

import java.util.*;

public class PointsManager {

    private final Map<String, Integer> countriesPoints = new HashMap<>();

    public static String pointsPrefix = "§f[§9PUNTOS§f] §7>>§r ";

    private final ServerPlayer serverPlayer;
    private final DataManager data;

    public PointsManager(ServerPlayer s) {
        data = s.getDataManager();

        serverPlayer = s;

        if (data.contains("points")) {
            ConfigurationSection pointsSection = data.getConfigurationSection("points");
            for (String key : pointsSection.getKeys(false)) {

                countriesPoints.put(key, pointsSection.getInt(key));
            }
        }
    }

    public ServerPlayer getServerPlayer() {
        return serverPlayer;
    }

    public int getPoints(OldCountry country) {
        return countriesPoints.getOrDefault(country.getName(), 0);
    }

    public void setPoints(OldCountry country, int points) {
        int old = countriesPoints.get(country.getName());
        if (old != points) {
            countriesPoints.put(country.getName(), points);
            Map<String, Integer> map = new HashMap<>();
            countriesPoints.forEach(map::put);
            data.set("points", map);
            data.save();
            int diff = Math.abs(points - old);
            country.getLogs().sendMessage((diff > 0 ? "up" : "down") + "wards_trend: Se han " + (diff > 0 ? "añadido" : "quitado") + " `" + diff + "` puntos a **" + serverPlayer.getName() + "**. Total: `" + points + "`.").queue();
        }

        serverPlayer.getGroupsManager().checkGroups();
        serverPlayer.getDiscordManager().checkDiscordBuilder(country);

    }

    public int addPoints(OldCountry country, int points) {
        int newAmount = getPoints(country) + points;
        setPoints(country, newAmount);
        return newAmount;
    }

    public int removePoints(OldCountry country, int points) {
        int newAmount = getPoints(country) - points;
        setPoints(country, newAmount);
        return newAmount;
    }

    public Map.Entry<String, Integer> getMaxPoints() {
        Map.Entry<String, Integer> max = null;
        for (Map.Entry<String, Integer> entry : countriesPoints.entrySet()) {
            if (max == null || entry.getValue().compareTo(max.getValue()) > 0) {
                max = entry;
            }
        }
        return max;
    }

    public LinkedHashMap<OldCountry, Integer> getSorted() {
        LinkedHashMap<OldCountry, Integer> sorted = new LinkedHashMap<>();
        countriesPoints.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEachOrdered(x -> sorted.put(new OldCountry(x.getKey()), x.getValue()));
        return sorted;
    }

    public void checkTop(OldCountry country) {
        CountryPlayer cPlayer = new CountryPlayer(serverPlayer, country);
        Configuration max = new Configuration(Bukkit.getPluginManager().getPlugin("bteConoSur"), "points/max");
        List<CountryPlayer> players = new ArrayList<>();
        max.getList(country.getAbbreviation() + "_max").forEach(uuid -> players.add(new CountryPlayer(new ServerPlayer(Bukkit.getOfflinePlayer(UUID.fromString((String) uuid))), country)));
        if (!players.contains(cPlayer)) {
            players.add(cPlayer);
        }
        Collections.sort(players);
        max.set(country.getAbbreviation() + "_max", players.subList(0, 10));
    }

    public enum BuilderRank {
        BUILDER, AVANZADO, VETERANO, MAESTRO;

        public static BuilderRank getFrom(int points) {
            if (points >= 1000) {
                return MAESTRO;
            } else if (points >= 500) {
                return AVANZADO;
            } else if (points >= 150) {
                return VETERANO;
            }
            return BUILDER;
        }

        @Override
        public String toString() {
            return super.toString().toLowerCase();
        }

        public String getAsPrefix() {
            switch (this) {
                case AVANZADO:
                    return "[§2AVANZADO§f]";
                case VETERANO:
                    return "[§eAVANZADO§f]";
                case MAESTRO:
                    return "[§6MAESTRO§f]";
                default:
                    return "[§9BUILDER§f]";
            }
        }
    }

}
