package pizzaaxx.bteconosur.Inventory;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class InventoryDataSet {

    private final Map<String, Object> data;

    public InventoryDataSet(Map<String, Object> data) {
        this.data = data;
    }

    public InventoryDataSet(String key, Object value) {
        this.data = new HashMap<>();
        data.put(key, value);
    }

    public InventoryDataSet() {
        this.data = new HashMap<>();
    }

    public void setData(String key, Object value) {
        data.put(key, value);
    }

    public String getString(String key) {
        return data.get(key).toString();
    }

    public int getInt(String key) {
        return (int) data.get(key);
    }

    public boolean getBoolean(String key) {
        return (boolean) data.get(key);
    }

    public <T> T get(String key, @NotNull Class<T> clazz) {
        return clazz.cast(data.get(key));
    }

}
