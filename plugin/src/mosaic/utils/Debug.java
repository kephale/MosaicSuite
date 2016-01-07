package mosaic.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


/**
 * Debug printouts for troubleshooting. Contains some convenient methods
 * for usual style of printing out on console.
 *
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class Debug { // NO_UCD (code used only for debugging)

    public static void print(Object... aObjects) {
        System.out.println(getString(aObjects));
        System.out.println();
    }

    public static String getString(Object... aObjects) {
        String str = "";
        for (final Object o : aObjects) {
            str += "[" + o + "] ";
        }
        return str;
    }

    public static String getJsonString(Object... aObjects) {
        String str = "";
        for (final Object obj : aObjects) {
            final Gson gson = new GsonBuilder().setPrettyPrinting().create();
            final String json = gson.toJson(obj);
            str += "[" + json + "] ";
        }
        return str;
    }
}