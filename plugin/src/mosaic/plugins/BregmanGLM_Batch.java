package mosaic.plugins;

import java.awt.GraphicsEnvironment;
import java.io.File;

import org.apache.log4j.Logger;

import ij.IJ;
import ij.ImagePlus;
import ij.Macro;
import ij.process.ImageProcessor;
import mosaic.bregman.BLauncher;
import mosaic.bregman.Parameters;
import mosaic.bregman.GUI.GenericGUI;
import mosaic.bregman.output.CSVOutput;
import mosaic.core.utils.MosaicUtils;
import mosaic.core.utils.Segmentation;
import mosaic.utils.io.serialize.DataFile;
import mosaic.utils.io.serialize.SerializedDataFile;

public class BregmanGLM_Batch implements Segmentation {
    private static final Logger logger = Logger.getLogger(BregmanGLM_Batch.class);
    
    private boolean gui_use_cluster = false;
    public static boolean test_mode = false;
    public static String test_path = null;
    
    @Override
    public int setup(String arg0, ImagePlus active_img) {
        // if is a macro get the arguments from macro arguments
        if (IJ.isMacro()) {
            arg0 = Macro.getOptions();
        }

        final String dir = IJ.getDirectory("temp");
        String savedSettings = dir + "spb_settings.dat";
        BLauncher.iParameters = getConfigHandler().LoadFromFile(savedSettings, Parameters.class, new Parameters()); //BLauncher.iParameters);

        final String path = MosaicUtils.parseString("config", arg0);
        if (path != null) {
            BLauncher.iParameters = getConfigHandler().LoadFromFile(path, Parameters.class, BLauncher.iParameters);
        }

        String normmin = MosaicUtils.parseString("min", arg0);
        if (normmin != null) {
            BLauncher.norm_min = Double.parseDouble(normmin);
            System.out.println("min norm " + BLauncher.norm_min);
        }

        String normmax = MosaicUtils.parseString("max", arg0);
        if (normmax != null) {
            BLauncher.norm_max = Double.parseDouble(normmax);
            System.out.println("max norm " + BLauncher.norm_max);
        }

        // Initialize CSV format
        CSVOutput.initCSV(1 /* oc_s */);

        // Check the argument
        final boolean batch = GraphicsEnvironment.isHeadless();

        logger.debug("isHeadless = " + batch);
        logger.debug("gui_use_cluster = " + gui_use_cluster);
        logger.debug("settings dir = [" + dir + "]");
        logger.debug("config path = [" + path + "]");
        logger.debug("norm min = [" + normmin + "]");
        logger.debug("norm max = [" + normmax + "]");
        logger.debug("input img = [" + (active_img != null ? active_img.getTitle() : "<no img>") + "]");
        
        GenericGUI window = new GenericGUI(batch, active_img);
        window.setUseCluster(gui_use_cluster);
        window.run(active_img);

        saveConfig(savedSettings, BLauncher.iParameters);

        // Re-set the arguments
        Macro.setOptions(arg0);

        return DONE;
    }

    @Override
    public void run(ImageProcessor imp) {}

    /**
     * Returns handler for (un)serializing Parameters objects.
     */
    public static DataFile<Parameters> getConfigHandler() {
        return new SerializedDataFile<Parameters>();
    }

    /**
     * Saves Parameters objects with additional handling of unserializable PSF
     * object. TODO: It (PSF) should be verified and probably corrected.
     *
     * @param aFullFileName - absolute path and file name
     * @param aParams - object to be serialized
     */
    public static void saveConfig(String aFullFileName, Parameters aParams) {
        getConfigHandler().SaveToFile(aFullFileName, aParams);
    }

    // =================== Stuff below is used only by tests

    /**
     * Unfortunately where is not way to hide the GUI in test mode, set the
     * plugin to explicitly bypass the GUI
     */
    public void bypass_GUI() {
        GenericGUI.bypass_GUI = true;
    }

    public void setUseCluster(boolean bl) {
        gui_use_cluster = bl;
    }

    // =================== Implementation of Segmentation interface

    private enum outputF {
        MASK(2), OBJECT(0);

        private final int numVal;

        outputF(int numVal) {
            this.numVal = numVal;
        }

        public int getNumVal() {
            return numVal;
        }
    }
    
    /**
     * Get Mask images name output
     *
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getMask(ImagePlus aImp) {
        final String[] gM = new String[2];
        gM[0] = new String(BLauncher.outSuffixesCluster[outputF.MASK.getNumVal()].replace("*", "_") + File.separator
                + BLauncher.outSuffixesCluster[outputF.MASK.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(BLauncher.outSuffixesCluster[outputF.MASK.getNumVal() + 1].replace("*", "_") + File.separator
                + BLauncher.outSuffixesCluster[outputF.MASK.getNumVal() + 1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        return gM;
    }

    /**
     * Get CSV regions list name output
     *
     * @param aImp image
     * @return set of possible output
     */
    @Override
    public String[] getRegionList(ImagePlus aImp) {
        final String[] gM = new String[4];
        gM[0] = new String(BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal()].replace("*", "_") + File.separator + BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal()].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));
        gM[1] = new String(BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal() + 1].replace("*", "_") + File.separator + BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal() + 1].replace("*", MosaicUtils.removeExtension(aImp.getTitle())));

        // This is produced if there is a stitch operation
        gM[2] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal()].replace("*", "_"));
        gM[3] = new String(MosaicUtils.removeExtension(aImp.getTitle()) + BLauncher.outSuffixesCluster[outputF.OBJECT.getNumVal() + 1].replace("*", "_"));

        return gM;
    }

    /**
     * Get name of the plugins
     */
    @Override
    public String getName() {
        return "Squassh";
    }
}
