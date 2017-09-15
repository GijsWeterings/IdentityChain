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
