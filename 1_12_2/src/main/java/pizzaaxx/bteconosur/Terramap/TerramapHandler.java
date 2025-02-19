package pizzaaxx.bteconosur.Terramap;

import com.sk89q.worldguard.util.net.HttpRequest;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import pizzaaxx.bteconosur.BTEConoSur;
import pizzaaxx.bteconosur.Geo.Coords2D;
import pizzaaxx.bteconosur.Utils.NumberUtils;
import pizzaaxx.bteconosur.Utils.WebMercatorUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.*;

public class TerramapHandler {

    private final BTEConoSur plugin;

    Random random = new Random();

    String[] osmServers = new String[] {"a", "b", "c"};

    public TerramapHandler(BTEConoSur plugin) {
        this.plugin = plugin;
    }

    // terramap/final/Z/X,Y.png
    // terramap/layers/Z/X,Y.png

    private final Map<String, BufferedImage> cache = new HashMap<>();
    private final Map<String, Long> deletionCache = new HashMap<>();

    public BufferedImage getTile(int x, int y, int zoom) throws IOException, IllegalArgumentException {

        String key = Integer.toString(x) + y + zoom;
        if (cache.containsKey(key)) {
            return cache.get(key);
        }

        if (zoom > 19) {
            throw new IllegalArgumentException();
        }

        InputStream stream;
        if (zoom < 12) {
            stream = HttpRequest.get(new URL("https://" + osmServers[random.nextInt(3)] + ".tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png")).execute().getInputStream();
        } else {
            double tiles = Math.pow(2, zoom);
            if (x < 0 || y < 0 || x >= tiles || y >= tiles) {
                throw new IllegalArgumentException();
            }

            File image = new File(plugin.getDataFolder(), "terramap/final/" + zoom + "/" + x + "_" + y + ".png");
            if (image.exists()) {
                stream = Files.newInputStream(image.toPath());
            } else {
                HttpRequest request = HttpRequest.get(new URL("https://" + osmServers[random.nextInt(3)] + ".tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png"));
                stream = request.execute().getInputStream();
            }
        }
        BufferedImage image = ImageIO.read(stream);
        cache.put(key, image);
        this.scheduleUnload(key);
        return image;
    }

    private void scheduleUnload(String key) {
        deletionCache.put(key, System.currentTimeMillis());
        new BukkitRunnable() {
            @Override
            public void run() {
                if (deletionCache.containsKey(key) && System.currentTimeMillis() - deletionCache.get(key) > 540000) {
                    deletionCache.remove(key);
                    cache.remove(key);
                }
            }
        }.runTaskLaterAsynchronously(plugin, 12000);
    }

    private void unload(int x, int y, int zoom) {
        String key = Integer.toString(x) + y + zoom;
        deletionCache.remove(key);
        cache.remove(key);
    }

    public void drawPolygon(@NotNull List<Coords2D> coordinates, Color color, String id) throws IOException {
        this.drawPolygon(coordinates, color, 12, id);
        this.drawPolygon(coordinates, color, 13, id);
        this.drawPolygon(coordinates, color, 14, id);
        this.drawPolygon(coordinates, color, 15, id);
        this.drawPolygon(coordinates, color, 16, id);
        this.drawPolygon(coordinates, color, 17, id);
        this.drawPolygon(coordinates, color, 18, id);
        this.drawPolygon(coordinates, color, 19, id);
    }

    private void drawPolygon(@NotNull List<Coords2D> coordinates, Color color, int zoom, String id) throws IOException {

        // 1. Find tile coordinates of bounding box.
        // 2. Create Buffered of size according to bounding box.
        // 3. Draw polygon on image.
        // 4. Cut image into tiles.
        // 5. Save tiles.
        // 6. Update tiles.

        Coords2D firstCoordinate = coordinates.get(0);
        double maxLat = firstCoordinate.getLat();
        double minLat = firstCoordinate.getLat();
        double maxLon = firstCoordinate.getLon();
        double minLon = firstCoordinate.getLon();
        for (Coords2D coordinate : coordinates) {
            if (maxLat < coordinate.getLat()) {
                maxLat = coordinate.getLat();
            }
            if (maxLon < coordinate.getLon()) {
                maxLon = coordinate.getLon();
            }
            if (minLat > coordinate.getLat()) {
                minLat = coordinate.getLat();
            }
            if (minLon > coordinate.getLon()) {
                minLon = coordinate.getLon();
            }


        } // FIND MAXs AND MINs

        int maxTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(maxLon, zoom), 256);
        int minTileX = Math.floorDiv((int) WebMercatorUtils.getXFromLongitude(minLon, zoom), 256);
        int maxTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(maxLat, zoom), 256);
        int minTileY = Math.floorDiv((int) WebMercatorUtils.getYFromLatitude(minLat, zoom), 256);

        int xSize = (Math.abs(maxTileX - minTileX) + 1) * 256;
        int ySize = (Math.abs(maxTileY - minTileY) + 1) * 256;

        BufferedImage image = new BufferedImage(xSize, ySize, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();

        int[] xCoordinates = new int[coordinates.size()];
        int[] yCoordinates = new int[coordinates.size()];

        double maxImageLon = WebMercatorUtils.getLongitudeFromX((maxTileX + 1) * 256, zoom);
        double minImageLon = WebMercatorUtils.getLongitudeFromX(minTileX * 256, zoom);
        double maxImageLat = WebMercatorUtils.getLatitudeFromY((minTileY + 1) * 256, zoom);
        double minImageLat = WebMercatorUtils.getLatitudeFromY(maxTileY * 256, zoom);

        int i = 0;
        for (Coords2D coordinate : coordinates) {
            xCoordinates[i] = (int) NumberUtils.getInNewRange(minImageLon, maxImageLon, 0, xSize, coordinate.getLon());
            yCoordinates[i] = (int) NumberUtils.getInNewRange(minImageLat, maxImageLat, 0, ySize, coordinate.getLat());
            i++;
        }
        Polygon polygon = new Polygon(xCoordinates, yCoordinates, coordinates.size());

        Color fillColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), 95);
        graphics.setColor(fillColor);
        graphics.fillPolygon(polygon);

        graphics.setColor(color);
        graphics.setStroke(new BasicStroke(5));
        graphics.drawPolygon(polygon);

        for (int x = 0; x < xSize; x += 256) {
            for (int y = 0; y < ySize; y += 256) {
                BufferedImage tile = image.getSubimage(x, y, 256, 256);

                boolean empty = true;
                outer:
                for (int subX = 0; subX < 256; subX++) {
                    for (int subY = 0; subY < 256; subY++) {
                        Color c = new Color(tile.getRGB(subX, subY));
                        if (c.getRed() != 0 || c.getGreen() != 0 || c.getBlue() != 0 || c.getAlpha() != 255) {
                            empty = false;
                            break outer;
                        }
                    }
                }

                if (!empty) {
                    int tileX = minTileX + (x / 256);
                    int tileY = maxTileY + (y / 256);

                    File tileFile = new File(plugin.getDataFolder(), "terramap/layers/" + zoom + "/" + tileX + "_" + tileY + "_" + id + ".png");
                    tileFile.createNewFile();

                    ImageIO.write(tile, "png", tileFile);

                    this.updateTile(tileX, tileY, zoom);
                }
            }
        }
    }

    public void deletePolygon(String id) throws IOException {
        this.deletePolygon(id, 12);
        this.deletePolygon(id, 13);
        this.deletePolygon(id, 14);
        this.deletePolygon(id, 15);
        this.deletePolygon(id, 16);
        this.deletePolygon(id, 17);
        this.deletePolygon(id, 18);
        this.deletePolygon(id, 19);
    }

    private void deletePolygon(String id, int zoom) throws IOException {
        File layersFolder = new File(plugin.getDataFolder(), "terramap/layers/" + zoom);
        File[] layers = layersFolder.listFiles((FilenameFilter) new RegexFileFilter("-?\\d{1,12}_-?\\d{1,12}_" + id + "\\.png"));
        if (layers == null) {
            return;
        }
        for (File layerFile: layers) {
            String name = layerFile.getName().replace(".png", "");
            String[] parts = name.split("_");
            int tileX = Integer.parseInt(parts[0]);
            int tileY = Integer.parseInt(parts[1]);
            if (layerFile.delete()) {
                this.updateTile(tileX, tileY, zoom);
            }
        }
    }

    private void updateTile(int x, int y, int zoom) throws IOException {
        File layersFolder = new File(plugin.getDataFolder(), "terramap/layers/" + zoom);
        File[] layers = layersFolder.listFiles((FilenameFilter) new RegexFileFilter(x + "_" + y + "_[a-zA-Z]{1,32}\\.png"));
        if (layers ==  null) {
            File finalTileFile = new File(plugin.getDataFolder(), "terramap/final/" + zoom + "/" + x + "_" + y + ".png");
            if (finalTileFile.exists()) {
                finalTileFile.delete();
                this.unload(x, y, zoom);
            }
            return;
        }

        BufferedImage base = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = base.createGraphics();

        HttpRequest request = HttpRequest.get(new URL("https://a.tile.openstreetmap.org/" + zoom + "/" + x + "/" + y + ".png"));
        InputStream inputStream = request.execute().getInputStream();
        BufferedImage baseTile = ImageIO.read(inputStream);
        graphics.drawImage(baseTile, 0, 0, null);

        List<Map.Entry<File, FileTime>> fileTimes = new ArrayList<>();
        for (File layerFile : layers) {
            BasicFileAttributes attributes = Files.readAttributes(layerFile.toPath(), BasicFileAttributes.class);
            fileTimes.add(
                    new AbstractMap.SimpleEntry<>(
                            layerFile,
                            attributes.creationTime()
                    )
            );
        }
        fileTimes.sort(Map.Entry.comparingByValue());

        for (Map.Entry<File, FileTime> entry : fileTimes) {
            File layerFile = entry.getKey();

            BufferedImage layer = ImageIO.read(layerFile);
            graphics.drawImage(layer, 0, 0, null);
        }

        File tile = new File(plugin.getDataFolder(), "terramap/final/" + zoom + "/" + x + "_" + y + ".png");
        ImageIO.write(base, "png", tile);
        this.unload(x, y, zoom);
    }

}
