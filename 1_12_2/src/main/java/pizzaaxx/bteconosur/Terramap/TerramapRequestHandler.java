package pizzaaxx.bteconosur.Terramap;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class TerramapRequestHandler implements HttpHandler {

    private final BTEConoSur plugin;

    private final Random random = new Random();

    private final String[] osmServers = new String[] {"a", "b", "c"};

    public TerramapRequestHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    @Override
    public void handle(@NotNull HttpExchange exchange) {

        Runnable runnable = () -> {
            try {
                Map<String, Integer> values = new HashMap<>();
                for (String part : exchange.getRequestURI().getQuery().split("&")) {
                    String[] sides = part.split("=");
                    values.put(sides[0], Integer.parseInt(sides[1]));
                }

                int x = values.get("x");
                int y = values.get("y");
                int zoom = values.get("z");

                if (zoom > 19) {
                    exchange.sendResponseHeaders(404, 0);
                    exchange.close();
                    return;
                }

                if (zoom < 12) {
                    exchange.getResponseHeaders().set("Location", "https://" + osmServers[random.nextInt(3)] + ".tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png");
                    exchange.sendResponseHeaders(301, 0);
                    exchange.close();
                } else {
                    double tiles = Math.pow(2, zoom);
                    if (x < 0 || y < 0 || x >= tiles || y >= tiles) {
                        exchange.sendResponseHeaders(404, 0);
                        exchange.close();
                        return;
                    }

                    File image = new File(plugin.getDataFolder(), "terramap/final/" + zoom + "/" + x + "_" + y + ".png");
                    if (image.exists()) {
                        InputStream is = Files.newInputStream(image.toPath());
                        OutputStream os = exchange.getResponseBody();
                        byte[] bytes = IOUtils.toByteArray(is);
                        exchange.sendResponseHeaders(200, bytes.length);
                        os.write(bytes);
                        exchange.close();
                    } else {
                        exchange.getResponseHeaders().set("Location", "https://" + osmServers[random.nextInt(3)] + ".tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png");
                        exchange.sendResponseHeaders(301, 0);
                        exchange.close();
                    }
                }
            } catch (IOException ignored) {}
        };
        CompletableFuture.runAsync(runnable);
    }
}
