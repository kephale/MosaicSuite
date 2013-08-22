package mosaic.plugins;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import mosaic.bregman.Analysis;
import mosaic.bregman.GUIold;
import mosaic.bregman.GenericGUI;
import mosaic.bregman.Parameters;
import mosaic.region_competition.Settings;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.io.Opener;


public class BregmanGLM_Batch implements PlugInFilter {

	private ImagePlus OriginalImagePlus = null;
	private String savedSettings;

	public int setup(String arg0, ImagePlus active_img) 
	{
		String dir = IJ.getDirectory("temp");
		savedSettings = dir+"spb_settings.dat";
		
		try 
		{
			LoadConfig(Analysis.p);
		} 
		catch (IOException e1) 
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} 
		catch (ClassNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		this.OriginalImagePlus = active_img;
		//IJ.log("arg0 " + arg0);
		String[] args =ImageJ.getArgs();
		int l=args.length;
		//IJ.log("args" + l);
		boolean batch=false;
		for(int i=0; i<l; i++)
		{
			//IJ.log("arg" + i+ args[i]);
			if(args[i]!=null)
			{
				if (args[i].endsWith("batch")) batch=true;
			}
		}
		
		//IJ.log("batchmode" + batch);
		try 
		{
			GenericGUI window = new GenericGUI(batch);
			window.run("");
			
			SaveConfig(Analysis.p);
			
			//IJ.log("Plugin exiting");
			//Headless hd= new Headless("/Users/arizk/tmpt/");
			//Headless hd= new Headless("/gpfs/home/rizk_a/Rab7_b/");
			//window.frame.setVisible(true);
		}
		catch (Exception e) 
		{
			e.printStackTrace();
		}
		return DONE;
	}

	public void run(ImageProcessor imp) 
	{


	}

	void LoadConfig(Parameters p) throws IOException, ClassNotFoundException
	{
		FileInputStream fin = new FileInputStream(savedSettings);
		ObjectInputStream ois = new ObjectInputStream(fin);
		p = (Parameters)ois.readObject();
		ois.close();
	}
	
	void SaveConfig(Parameters p) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(savedSettings);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(p);
		oos.close();
	}
}