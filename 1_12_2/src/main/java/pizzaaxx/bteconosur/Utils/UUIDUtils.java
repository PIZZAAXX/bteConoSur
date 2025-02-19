package pizzaaxx.bteconosur.Utils;

import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDUtils {

    @NotNull
    public static UUID getFromInputStream(InputStream is) throws IOException {
        byte[] bytes = IOUtils.toByteArray(is);
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        long high = byteBuffer.getLong();
        long low = byteBuffer.getLong();
        return new UUID(high, low);
    }

}
