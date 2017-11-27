package nl.tudelft.cs4160.trustchain_android.Util;

import android.content.Context;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;

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

    /**
     * Create a ellipsized string
     * @param input - the string to be ellipsized
     * @param maxLength - The maximum length the result string can be, minimum should be 6
     * @return An ellipsized string of the input
     */
    public static String ellipsize(String input, int maxLength) {
        String ellip = "(..)";
        if (input == null || input.length() <= maxLength
                || input.length() < ellip.length()) {
            return input;
        }
        if (maxLength < ellip.length()+2) {
            return input.substring(0,1).concat(ellip).concat(input.substring(input.length()-1,input.length()));
        }
        return input.substring(0, (maxLength - ellip.length())/2)
                .concat(ellip)
                .concat(input.substring(input.length() - (maxLength - ellip.length())/2,input.length()));
    }

}
