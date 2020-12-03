package me.sagan.r1helper;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class SnowboyUtils {
    private SnowboyUtils() {
        // Empty private constructor
    }

    public static File getSnowboyDirectory() {
        File sdDirectory = Environment.getExternalStorageDirectory();
        File snowboyDirectory = new File(sdDirectory, "r1helper");
        snowboyDirectory.mkdirs();

        return snowboyDirectory;
    }

    public static void copyAssets(Context context) {
        try {
            File snowboyDirectory = getSnowboyDirectory();
            String[] paths = context.getAssets().list("snowboy");
            for (String path : paths) {
                File file = new File(snowboyDirectory, path);
                if( file.exists() ) {
                    continue;
                }
                InputStream inputStream = context.getAssets().open("snowboy/" + path);
                OutputStream outputStream = new FileOutputStream(file);

                byte[] buffer = new byte[1024];
                int readSize = inputStream.read(buffer, 0, 1024);
                while (readSize > 0) {
                    outputStream.write(buffer, 0, readSize);
                    readSize = inputStream.read(buffer, 0, 1024);
                }

                inputStream.close();
                outputStream.close();
            }
        } catch (IOException e) {
            Log.e("hotword", e.getMessage(), e);
        }
    }
}
