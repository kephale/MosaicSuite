package mosaic.core.psf;

import ij.IJ;
import ij.gui.GenericDialog;

import java.awt.Button;
import java.awt.Choice;
import java.awt.GridBagConstraints;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import net.imglib2.Cursor;
import net.imglib2.img.Img;
import net.imglib2.img.ImgFactory;
import net.imglib2.img.array.ArrayImgFactory;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

/**
 * 
 * This class generate PSF images from the list of all implemented
 * PSF. or from a File
 * 
 * @see psfList.java
 * 
 * Just create the a new GeneratePSF class and call generate
 * 
 * @author Pietro Incardona
 *
 */

class PSFSettings implements Serializable
{
	private static final long serialVersionUID = 3757876543608904166L;
	
	int dim;
	String clist;
}

public class GeneratePSF
{	
	PSFSettings settings = new PSFSettings();
	
	int sz[];
	
	Choice PSFc;
	
	psf<FloatType> psfc;
	TextField dimF;
	
	/**
	 * 
	 * Get the parameters for the Psf
	 * 
	 * @param dim Dimension of the PSF
	 * @param psf String that identify the PSF like "Gauss ... "
	 */
	
	private void selectPSF(int dim,String psf)
	{	
		if (dim == 0)
		{
			IJ.error("Dimension must be a valid integer != 0");
		}
		psfc = psfList.factory(psf,dim,FloatType.class);
		psfc.getParamenters();
	}
	
	/**
	 * 
	 * Get the parameters for the PSF
	 * 
	 * @param dim dimension of the psf
	 */
	
	private void selectPSF(int dim)
	{
		String psf = PSFc.getSelectedItem();
		
		if (dim == 0)
		{
			IJ.error("Dimension must be a valid integer != 0");
		}
		psfc = psfList.factory(psf,dim,FloatType.class);
		psfc.getParamenters();
	}
	
	/**
	 * 
	 * Generate a 2D array image from the PSF
	 * 
	 * @param psf
	 * @return 2D array image
	 */
	
	public static <T extends RealType<T>> double[][] generateImage2DAsDoubleArray(psf<T> psf) 
	{
		if (psf.getSuggestedImageSize().length != 2)
			return null;
		
		int sz[] = psf.getSuggestedImageSize();
		
		double [][] img = new double[sz[0]][sz[1]];
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		// If is file psf
		
		int old_mid[] = null;
		if (psf.isFile() == false)
		{
			old_mid = psf.getCenter();
			psf.setCenter(mid);
		}
		
		// 
		
		int loc[] = new int[sz.length]; 
		
		// Create an img
		
		for (int i = 0 ; i < sz[0]; i++)
		{
			for (int j = 0 ; j < sz[1] ; j++)
			{
				loc[0] = i;
				loc[1] = j;
				psf.setPosition(loc);
				double f = psf.get().getRealFloat();
				img[i][j] = f;
			}
		}
		
		// Reset the center to previous one
		
		psf.setCenter(old_mid);
		
		return img;
	}

	public static <T extends RealType<T>> float[][] generateImage2DAsFloatArray(psf<T> psf) 
	{
		if (psf.getSuggestedImageSize().length != 2)
			return null;
		
		int sz[] = psf.getSuggestedImageSize();
		
		float [][] img = new float[sz[0]][sz[1]];
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		// If is file psf
		
		int old_mid[] = null;
		if (psf.isFile() == false)
		{
			old_mid = psf.getCenter();
			psf.setCenter(mid);
		}
		
		// 
		
		int loc[] = new int[sz.length]; 
		
		// Create an imglib2
		
		for (int i = 0 ; i < sz[0]; i++)
		{
			for (int j = 0 ; j < sz[1] ; j++)
			{
				loc[0] = i;
				loc[1] = j;
				psf.setPosition(loc);
				float f = psf.get().getRealFloat();
				img[i][j] = f;
			}
		}
		
		// Reset the center to previous one
		
		psf.setCenter(old_mid);
		
		return img;
	}
	
	/**
	 * 
	 * Generate a 3D array image from the PSF
	 * 
	 * @param psf
	 * @return 3D array image
	 */
	
	public static <T extends RealType<T>> double[][][] generateImage3DAsDoubleArray(psf<T> psf) 
	{
		if (psf.getSuggestedImageSize().length != 3)
			return null;
		
		int sz[] = psf.getSuggestedImageSize();
		
		double [][][] img = new double[sz[2]][sz[1]][sz[0]];
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		// If is file psf
		
		int old_mid[] = null;
		if (psf.isFile() == false)
		{
			old_mid = psf.getCenter();
			psf.setCenter(mid);
		}
		
		// 
		
		int loc[] = new int[sz.length]; 
		
		// Create an imglib2
		
		for (int i = 0 ; i < sz[0]; i++)
		{
			for (int j = 0 ; j < sz[1] ; j++)
			{
				for (int k = 0 ; k < sz[2] ; k++)
				{
					loc[0] = i;
					loc[1] = j;
					loc[2] = k;
					psf.setPosition(loc);
					float f = psf.get().getRealFloat();
					img[k][j][i] = f;
				}
			}
		}
		
		// Reset the center to previous one
		
		psf.setCenter(old_mid);
		
		return img;
	}

	public static <T extends RealType<T>> float[][][] generateImage3DAsFloatArray(psf<T> psf) 
	{
		if (psf.getSuggestedImageSize().length != 3)
			return null;
		
		int sz[] = psf.getSuggestedImageSize();
		
		float [][][] img = new float[sz[0]][sz[1]][sz[2]];
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		// If is file psf
		
		int old_mid[] = null;
		if (psf.isFile() == false)
		{
			old_mid = psf.getCenter();
			psf.setCenter(mid);
		}
		
		// 
		
		int loc[] = new int[sz.length]; 
		
		// Create an imglib2
		
		for (int i = 0 ; i < sz[2]; i++)
		{
			for (int j = 0 ; j < sz[1] ; j++)
			{
				for (int k = 0 ; k < sz[0] ; k++)
				{
					loc[0] = i;
					loc[1] = j;
					loc[2] = k;
					psf.setPosition(loc);
					float f = psf.get().getRealFloat();
					img[k][j][i] = f;
				}
			}
		}
		
		// Reset the center to previous one
		
		psf.setCenter(old_mid);
		
		return img;
	}
	
	/**
	 * 
	 * Return a generated PSF image. A GUI is shown ti give the user
	 * the possibility to choose size of the image PSF function parameters
	 * 
	 * @return An image representing the PSF
	 */
	
	public Img< FloatType > generate(int dim)
	{
		settings.clist = psfList.psfList[0];
		LoadConfigFile(IJ.getDirectory("temp")+ File.separator + "psf_settings.dat");
		
		GenericDialog gd = new GenericDialog("PSF Generator");
		
		gd.addNumericField("Dimensions ", dim, 0);
		
		if (IJ.isMacro() == false)
		{
			dimF = (TextField) gd.getNumericFields().lastElement();
		
			gd.addChoice("PSF: ", psfList.psfList, settings.clist);
			PSFc = (Choice)gd.getChoices().lastElement();
			{
				Button optionButton = new Button("Options");
				GridBagConstraints c = new GridBagConstraints();
				int gridx = 2;
				int gridy = 1;
				c.gridx=gridx;
				c.gridy=gridy++; c.anchor = GridBagConstraints.EAST;
				gd.add(optionButton,c);
			
				optionButton.addActionListener(new ActionListener()
				{
					@Override
					public void actionPerformed(ActionEvent e)
					{
						int dim = Integer.parseInt(dimF.getText());
						selectPSF(dim);
					}
				});
			}
		}
		else
		{
			gd.addChoice("PSF: ", psfList.psfList, settings.clist);
		}
		
		gd.showDialog();
		
		// if Batch system
		
		String choice = gd.getNextChoice();
		if (IJ.isMacro() == true)
		{
			dim = (int) gd.getNextNumber();
			selectPSF(dim,choice);
		}
		
		// psf not selected
		
		if (psfc == null)
		{
			dim = (int) gd.getNextNumber();
			selectPSF(dim,choice);
		}
		
		// get the dimension
		
		sz = psfc.getSuggestedImageSize();
		if (sz == null)
			return null;
		
		// center on the middle of the image
		
		int [] mid = new int[sz.length];
		
		for (int i = 0 ; i < mid.length ; i++)
		{
			mid[i] = sz[i]/2;
		}
		
		// If is file psf
		
		if (psfc.isFile() == false)
			psfc.setCenter(mid);
		
		// 
		
		int loc[] = new int[sz.length]; 
		
		// Create an imglib2
		
		ImgFactory< FloatType > imgFactory = new ArrayImgFactory< FloatType >( );
		Img<FloatType> PSFimg = imgFactory.create( sz, new FloatType() );
		
		Cursor<FloatType> cft = PSFimg.cursor();
		
		while (cft.hasNext())
		{
			cft.next();
			cft.localize(loc);
			psfc.setPosition(loc);
			float f = psfc.get().getRealFloat();
			cft.get().set(f);
		}
		
		// Save on settings
		
		settings.dim = dim;
		settings.clist = choice;
		
		try {
			SaveConfigFile(IJ.getDirectory("temp")+ File.separator + "psf_settings.dat",settings);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return PSFimg;
	}
	
	public String getParameters()
	{
		return psfc.getStringParameters();
	}
	
	static public void SaveConfigFile(String sv, PSFSettings settings) throws IOException
	{
		FileOutputStream fout = new FileOutputStream(sv);
		ObjectOutputStream oos = new ObjectOutputStream(fout);
		oos.writeObject(settings);
		oos.close();
	}

	
	private boolean LoadConfigFile(String savedSettings)
	{
		System.out.println(savedSettings);
		
		try
		{
			FileInputStream fin = new FileInputStream(savedSettings);
			ObjectInputStream ois = new ObjectInputStream(fin);
			settings = (PSFSettings)ois.readObject();
			ois.close();
		}
		catch (FileNotFoundException e)
		{
			System.err.println("Settings File not found "+savedSettings);
			return false;
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return true;
	}
}
