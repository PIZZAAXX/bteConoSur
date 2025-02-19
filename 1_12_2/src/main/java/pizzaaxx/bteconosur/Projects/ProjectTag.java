package pizzaaxx.bteconosur.Projects;

import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.SQL.JSONParsable;

public enum ProjectTag implements JSONParsable {

    EDIFICIOS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWY2YmIzYWQ4ZGFmMGMxNDk5YjVlNDZkY2Y0MTc2YzgzNDU0MzU1M2ExYTgxODAwOWU3Njc1ZTg5NjI5NWUxYSJ9fX0="),
    CASAS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvY2Y3Y2RlZWZjNmQzN2ZlY2FiNjc2YzU4NGJmNjIwODMyYWFhYzg1Mzc1ZTlmY2JmZjI3MzcyNDkyZDY5ZiJ9fX0="),
    DEPARTAMENTOS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDk5MTBjZjYwMGQyMWEwNDA0ZDlkZjRiMGQ2NTllZDQ4NDE4NmFlMDYxNDI3MGY3YTY0MjlmNzA0ZDBiZGJjOSJ9fX0="),
    SHOPPING("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYTAzYmQwMDQyMTcyOWNkNjM1Y2QzYjQ4MjQzNDMwYWQ0N2NmNzA3MDE4YTU5MTZmZjU5NTQ5ZDVlY2Q2Zjg3OSJ9fX0="),
    ESTABLECIMIENTOS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWVlOGQ2ZjVjYjdhMzVhNGRkYmRhNDZmMDQ3ODkxNWRkOWViYmNlZjkyNGViOGNhMjg4ZTkxZDE5YzhjYiJ9fX0="),
    PARQUES("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWExZmJlZjNkMGM1MWFkNmM3MTNhYTIwYzQyZGIxODM0MzRjZWM0ZmI2M2E1YTNlYWExMDFhZDNjNWY3NWQxNSJ9fX0="),
    CARRETERAS("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGRjMGJmZmZiNTg1YjNkNGU2ZThkM2Y5Y2JiMzAzZGUyZjUyZjIwMTQ4OGQ4MjEwZmE4Y2RjNDBiYmFkNTg4ZCJ9fX0=");

    private final String headValue;

    ProjectTag(String headValue) {
        this.headValue = headValue;
    }

    public String getHeadValue() {
        return headValue;
    }

    @NotNull
    @Override
    public String getJSON(boolean insideJSON) {
        return (insideJSON?"\"":"'") + this.toString() + (insideJSON?"\"":"'");
    }
}
