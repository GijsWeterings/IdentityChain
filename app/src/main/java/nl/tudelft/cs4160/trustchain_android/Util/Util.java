package nl.tudelft.cs4160.trustchain_android.Util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by rico on 15-9-17.
 */

public class Util {

    /**
     * Read a file from storage
     * @param context The context.
     * @param fileName The file to be read.
     * @return The content of the file
     */
    public static String readFile(Context context, String fileName) {
        try {
            StringBuilder text = new StringBuilder();
            FileInputStream fis = context.openFileInput(fileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(new BufferedInputStream(fis)));

            String line;
            while ((line = br.readLine()) != null) {
                text.append(line);
                text.append('\n');
            }
            br.close();
            return text.toString();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Write to a file
     * @param context The context
     * @param fileName File to be written
     * @param data The data to be written to the file
     * @return True if successful, false if not
     */
    public static boolean writeToFile(Context context, String fileName, String data) {
        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(data.getBytes());
            outputStream.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
