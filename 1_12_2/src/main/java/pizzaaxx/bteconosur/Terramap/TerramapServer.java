package pizzaaxx.bteconosur.Terramap;

import com.sun.net.httpserver.HttpServer;
import pizzaaxx.bteconosur.BTEConoSur;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class TerramapServer {

    private final BTEConoSur plugin;
    private HttpServer server;

    public TerramapServer(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    public void init() throws Exception {

        server = HttpServer.create(
                new InetSocketAddress(
                        "0.0.0.0",
                        19255
                ),
                0
        );
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.createContext("/terramap", new TerramapRequestHandler(plugin));
        server.start();

    }

    public void stop() {
        if (server != null) {
            server.stop(5);
        }
    }

    public HttpServer getServer() {
        return server;
    }
}
