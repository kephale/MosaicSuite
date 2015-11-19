package mosaic.plugins;

import org.junit.Test;

import ij.ImagePlus;
import ij.WindowManager;
import ij.macro.Interpreter;
import mosaic.test.framework.CommonBase;
import mosaic.test.framework.SystemOperations;


public class Region_CompetitionTest extends CommonBase {

    @Test
    public void testDot()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/dot/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics";
        final String inputFile           = "dot.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/dot_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/dot_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testPsf()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/uc_psf/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics";
        final String inputFile           = "uc_data.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/uc_data_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/uc_data_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("psf_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("psf_file_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("uc_psf.tif", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testFusionCheck()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/fusionCheck/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics";
        final String inputFile           = "1thing.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/1thing_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/1thing_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testSphere3D()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/sphere_3d/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics";
        final String inputFile           = "sphere.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/sphere_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/sphere_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testTwoBars()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/twoBars/";
        final String setupString         = "run";
        final String macroOptions        = "show_and_save_statistics";
        final String inputFile           = "twoBars.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/twoBars_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/twoBars_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        
        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
    @Test
    public void testLabelImageFromFile()  {
        
        // Define test data
        final String tcDirName           = "Region_Competition/labelImgFromFile/";
        final String setupString         = "run";
        final String macroOptions        = "inputimage=object.tif labelimage=label.tif show_and_save_statistics";
        final String inputFile           = "object.tif";
        final String[] expectedImgFiles  = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] referenceImgFiles = {"__seg_c1.tif/object_seg_c1.tif"};
        final String[] expectedFiles     = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};
        final String[] referenceFiles    = {"__ObjectsData_c1.csv/object_ObjectsData_c1.csv"};

        // Create tested plugIn
        final Region_Competition plugin = new Region_Competition();
        copyTestResources("rc_settings.dat", SystemOperations.getTestDataPath() + tcDirName, "/tmp");
        copyTestResources("label.tif", SystemOperations.getTestDataPath() + tcDirName, tmpPath);
        
        // A little hack - I have no found the other way to load second image for test purposes.
        Interpreter.batchMode = true;
        Interpreter.addBatchModeImage(loadImagePlus(tmpPath + "/label.tif"));

        // Test it
        testPlugin(plugin, tcDirName,
                   macroOptions, 
                   setupString, inputFile,
                   expectedImgFiles, referenceImgFiles,
                   expectedFiles, referenceFiles);
    }
    
}
