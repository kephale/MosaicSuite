package mosaic.bregman.GUI;


import ij.gui.GenericDialog;

import java.io.File;
import java.util.Vector;


public class ChooseGUI {

    /**
     * Create a Choose Window
     *
     * @param Window title
     * @param message to show in the selection window
     * @param sel all the options
     * @return chosen String
     */

    public String choose(String title, String message, String sel[]) {
        if (sel.length == 0) {
            return null;
        }

        final GenericDialog gd = new GenericDialog(title);

        final String ad[] = new String[sel.length];
        for (int i = 0; i < sel.length; i++) {
            ad[i] = sel[i];
        }
        gd.addChoice(message, ad, ad[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            return gd.getNextChoice();
        }

        return null;
    }

    /**
     * Create a Choose Window
     *
     * @param Window title
     * @param message to show in the selection window
     * @param sel all the options
     * @return chosen File
     */

    public File choose(String title, String message, Vector<File> sel) {
        if (sel.size() == 0) {
            return null;
        }

        final GenericDialog gd = new GenericDialog(title);

        final String ad[] = new String[sel.size()];
        for (int i = 0; i < sel.size(); i++) {
            ad[i] = sel.get(i).getAbsolutePath();
        }
        gd.addChoice(message, ad, ad[0]);
        gd.showDialog();

        if (!gd.wasCanceled()) {
            final String c = gd.getNextChoice();
            return new File(c);
        }

        return null;
    }
}