package org.opendatakit.briefcase.util;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FontUtils {

    public static Font getCustomFont(String name, Float size) {

        // if we are in a jar, then use the classloader, if not, load from filesystem
        File file = getResourceAsFile(name);
        if (!file.exists()) {
            file = new File("res" + File.separator + name);
        }

        Font customFont = null;
        try {
            customFont = Font.createFont(Font.TRUETYPE_FONT, file).deriveFont(size);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            ge.registerFont(Font.createFont(Font.TRUETYPE_FONT, file));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FontFormatException e) {
            e.printStackTrace();
        }
        return customFont;

    }

    // https://stackoverflow.com/a/35466006/152938
    private static File getResourceAsFile(String resourcePath) {
        try {
            InputStream in = ClassLoader.getSystemClassLoader().getResourceAsStream(resourcePath);
            if (in == null) {
                return null;
            }

            File tempFile = File.createTempFile(String.valueOf(in.hashCode()), ".tmp");
            tempFile.deleteOnExit();

            try (FileOutputStream out = new FileOutputStream(tempFile)) {
                //copy stream
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            return tempFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

}
