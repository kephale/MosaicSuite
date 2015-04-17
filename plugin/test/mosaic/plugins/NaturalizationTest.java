package mosaic.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import ij.plugin.filter.PlugInFilter;
import mosaic.test.framework.CommonBase;

import org.apache.log4j.Logger;
import org.junit.Test;

/**
 * {@link Naturalization} plugin tests.  
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class NaturalizationTest extends CommonBase {
    private static final Logger logger = Logger.getLogger(NaturalizationTest.class);

    @Test
    public void testColorRgb() {
        // Define test data
        String tcDirName          = "Naturalization/flower/";
        String setupString        = "run";
        int expectedSetupRetValue = PlugInFilter.DONE;
        String[] inputFiles       = {"x.png"};
        String[] expectedFiles    = {"x.png_naturalized"};
        String[] referenceFiles   = {"x_nat.tif"};
           
        // Create tested plugin
        Naturalization nt = new Naturalization();
        
        // Test it
        testPlugin2(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, expectedSetupRetValue);
    }
    
    @Test
    public void testGrey8() {
        // Define test data
        String tcDirName          = "Naturalization/flower/";
        String setupString        = "run";
        int expectedSetupRetValue = PlugInFilter.DONE;
        String[] inputFiles       = {"x8bit.png"};
        String[] expectedFiles    = {"x8bit.png_naturalized"};
        String[] referenceFiles   = {"x8bit_nat.tif"};
           
        // Create tested plugin
        Naturalization nt = new Naturalization();
    
        // Test it
        testPlugin2(nt, tcDirName, 
                   inputFiles, expectedFiles, referenceFiles, 
                   setupString, expectedSetupRetValue);    
    }
    
    @Test
    public void testPSNR() {
        // Create tested plugin
        Naturalization nt = new Naturalization();
        
        // Check values
        logger.debug("Testting PSNR for different ranges of input values");
        assertTrue("x >=  0 && x <= 0.934", nt.calculate_PSNR(0).startsWith("6.100"));
        assertTrue("x >=  0 && x <= 0.934", nt.calculate_PSNR(0.9).startsWith("38.568"));
        
        assertEquals("x > 0.934 && x < 1.07", "> 40", nt.calculate_PSNR(1.00));
        
        assertTrue("x >= 1.07 && x < 1.9", nt.calculate_PSNR(1.08).startsWith("40.284"));
        assertTrue("x >= 1.07 && x < 1.9", nt.calculate_PSNR(1.8).startsWith("31.957"));

        assertTrue("x >= 1.9", nt.calculate_PSNR(1.95).startsWith("29.765"));
        assertTrue("x >= 1.9", nt.calculate_PSNR(10.0).startsWith("44444."));
    }
    
}