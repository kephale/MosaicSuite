package mosaic.bregman;

import java.util.ArrayList;


/* An inner class to make the results list sortable. */
public class Region implements Comparable 
{
	boolean colocpositive=false;

	Region(int value, String materialName, int points, boolean sameValue) 
	{
		//	byteImage = true;
		this.value = value;
		//this.materialName = materialName;
		this.points = points;
		//	this.sameValue = sameValue;
	}

	Region(int points, boolean sameValue) 
	{
		//	byteImage = false;
		this.points = points;
		//	this.sameValue = sameValue;
	}

	ArrayList<Pix> pixels = new ArrayList<Pix>();
	//boolean byteImage;
	public int points;
	float rsize;
	//String materialName;
	int value;
	double perimeter;
	double length;
	Region rvoronoi;
	//boolean sameValue;
	double intensity;
	float cx,cy,cz;
	float overlap;
	float over_int;
	float over_size;
	float beta_in;
	float beta_out;
	boolean singlec;
	double coloc_o_int;
	public int compareTo(Object otherRegion) 
	{
		Region o = (Region) otherRegion;
		return (value < o.value) ? 1 : ((value  > o.value) ? -1 : 0);
	}
		
	public double getcx()
	{
		return cx;
	}

	public double getcy()
	{
		return cy;
	}

	public double getcz()
	{
		return cz;
	}

	public double getintensity()
	{
		return intensity;
	}
	
	public double getrsize()
	{
		return rsize;
	}
	
	public double getperimeter()
	{
		return perimeter;
	}
	
	public double getoverlap_with_ch()
	{
		return overlap;
	}
	
	public double getcoloc_object_size()
	{
		return over_size;
	}
	
	public double getcoloc_object_intensity()
	{
		return over_int;
	}
	
	public boolean getsingle_coloc()
	{
		return singlec;
	}
	
	public double getcoloc_image_intensity()
	{
		return coloc_o_int;
	}
	
}
