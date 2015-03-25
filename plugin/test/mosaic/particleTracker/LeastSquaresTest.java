package mosaic.particleTracker;

import static org.junit.Assert.assertTrue;
import ij.IJ;
import ij.ImageJ;
import io.scif.config.SCIFIOConfig;
import io.scif.config.SCIFIOConfig.ImgMode;
import io.scif.img.ImgIOException;
import io.scif.img.ImgOpener;
import mosaic.test.framework.CommonTestBase;


import org.junit.Test;
import org.scijava.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LeastSquaresTest extends CommonTestBase {
    protected static final Logger log = LoggerFactory.getLogger(LeastSquaresTest.class.getSimpleName());
    
    @Test
    public void test1() { 
//        System.out.println("FIRST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
//        log.error("Test started");
//        assertTrue("Whole test suit for LEastSquares is to be implemented", true);
//        System.out.println(this.getClass().getName() + " " + this.getClass().getSimpleName());
//        
//        System.out.println("LAST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
        assertTrue("Whole test suit for LEastSquares is to be implemented", false);
    }

    @Test
    public void test2() { 
        new ImageJ();
        final Context context = (Context)
                IJ.runPlugIn(Context.class.getName(), ""); 
        ImgOpener io = new ImgOpener(context);
//        ImgOpener io = new ImgOpener();
        System.out.println("FIRST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
        
        assertTrue("Whole test suit for LEastSquares is to be implemented", true);
        System.out.println(this.getClass().getName() + " " + this.getClass().getSimpleName());
        
        System.out.println("LAST LINE OF TEST " + new Object(){}.getClass().getEnclosingMethod().getName());
    }
}