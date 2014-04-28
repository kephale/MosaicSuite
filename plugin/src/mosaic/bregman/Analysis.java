package mosaic.bregman;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;

import mosaic.bregman.FindConnectedRegions.Region;
import mosaic.bregman.output.CSVOutput;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mosaic.core.ipc.*;

public class Analysis {

	public static String out[] = {"*_ObjectsData_c1.csv",
	        "*_ObjectsData_c2.csv",
	        "*_mask_c1.zip",
	        "*_mask_c2.zip",
	        "*_ImagesData.csv",
	        "*_outline_overlay_c1.zip",
	        "*_outline_overlay_c2.zip",
	        "*_intensities_c1.zip",
	        "*_intensities_c2.zip",
	        "*_seg_c1_RGB.zip",
	        "*_seg_c2_RGB.zip",
	        "*.tif"};
	
	public static String out_w[] = {"*_ObjectsData_c1.csv",
        "*_ObjectsData_c2.csv",
        "*_mask_c1.zip",
        "*_mask_c2.zip",
        "*_ImagesData.csv",
        "*_outline_overlay_c1.zip",
        "*_outline_overlay_c2.zip",
        "*_intensities_c1.zip",
        "*_intensities_c2.zip",
        "*_seg_c1_RGB.zip",
        "*_seg_c2_RGB.zip"};

	
	public static double meansize_refined;
	public static String currentImage="currentImage";
	public static double bestEnergyX;
	public static double bestEnergyY;
	public static ASplitBregmanSolver solverX;
	public static ASplitBregmanSolver solverY;
	public static ImagePlus imgA;
	public static ImagePlus imgB;
	public static Parameters p=new Parameters();
	public static int  [] [] [] maxMaskA;//deprecated
	public static int  [] [] [] maxMaskB;//deprecated
	public static byte  [] [] [] maskA;//=new double [p.nz][p.ni][p.nj];
	public static byte  [] [] [] maskB;//=new double [p.nz][p.ni][p.nj];
	public static boolean  [] [] [] cellMaskABinary;//=new double [p.nz][p.ni][p.nj];
	public static boolean  [] [] [] cellMaskBBinary;//=new double [p.nz][p.ni][p.nj];
	public static boolean  [] [] [] overallCellMaskBinary;//=new double [p.nz][p.ni][p.nj];
	public static int nb;
	public static int na;
	public static double meana, meanb;
	public static boolean doingbatch=false;
	public static boolean firstbcoloc=true;
	public static String dir;
	
	// Maximum norm, it fix the range of the normalization, useful for video
	// normalization has to be done on all frame video, filled when the plugins
	// is called with the options min=... max=...
	public static double norm_max = 0.0;
	// Minimum norm
	public static double norm_min = 0.0;

	public static boolean exclude_z_edges;
	public static int positiveA, positiveB;
	public static short [][] [] [] regions;
	public static ArrayList<Region> regionslist[];

	public static byte [] imagecolor_c1;
	public static byte [] imagecolor_c2;

	public static CountDownLatch DoneSignala;
	public static CountDownLatch DoneSignalb;
	public static double [] [] [] imageb;//= new double [p.nz][p.ni][p.nj];
	public static double [] [] [] imagea;//= new double [p.nz][p.ni][p.nj];
	public static double maxb=0;
	public static double minb=Double.POSITIVE_INFINITY;
	public static double maxa=0;
	public static double mina=Double.POSITIVE_INFINITY;

	public static double a3b3, a3b2, a2b2, a2b3;

	public static Tools Tools;

	public static void init()
	{
		regions = new short[2][][][];
		regionslist = new ArrayList[2];
	}
	
	public static void load2channels(ImagePlus img2){

		//		IJ.log("inside loading func");
		//		IJ.log("getting sizes");
		//		IJ.log("test" + (img2==null));
		p.ni=img2.getWidth();
		p.nj=img2.getHeight();
		p.nz=img2.getNSlices();
		
		int f = img2.getFrame();

		//		IJ.log("creating a");
		imgA=new ImagePlus();
		int bits = img2.getBitDepth();

		ImageStack imga_s= new ImageStack(p.ni,p.nj);

		//channel 1
		for (int z=0; z<p.nz; z++){  
			img2.setPosition(1,z+1,f);
			ImageProcessor impt;
			if(bits==32)
				impt=img2.getProcessor().convertToShort(false);
			else
				impt = img2.getProcessor();
			imga_s.addSlice("", impt);
		}

		imgA.setStack(img2.getTitle(),imga_s);
		//imgA.setTitle("A2");
		setimagea();

		imgB=new ImagePlus();
		ImageStack imgb_s= new ImageStack(p.ni,p.nj);

		//channel 2
		for (int z=0; z<p.nz; z++){  
			img2.setPosition(2,z+1,f);	
			ImageProcessor impt;
			if(bits==32)
				impt=img2.getProcessor().convertToShort(false);
			else
				impt = img2.getProcessor();
			imgb_s.addSlice("", impt);
		}

		imgB.setStack(img2.getTitle(),imgb_s);
		setimageb();
		//imgB.setTitle("B2");
//		if(p.dispwindows){
//			imgA.setTitle(imgA.getShortTitle() + " ch1");
//			imgB.setTitle(imgB.getShortTitle() + " ch2");
//			imgA.show("");
//			imgB.show("");
//		}
		//IJ.log("imga:"+imgA.getTitle());

	}

	/**
	 * 
	 * Get the objects list and set the frame
	 * 
	 * @param f Frame
	 * @return Vector with objects
	 */
	
	public static Vector<?> getObjectsList(int f)
	{
		@SuppressWarnings("unchecked")
		Vector<? extends ICSVGeneral > v = (Vector<? extends ICSVGeneral>) CSVOutput.getVector(regionslist[0]);
		
		// Set frame
		
		for (int i = 0 ; i < v.size() ; i++)
		{
			v.get(i).setFrame(f);
		}
		
		return v;
	}
	
	public static void load1channel(ImagePlus img2){

		p.ni=img2.getWidth();
		p.nj=img2.getHeight();
		p.nz=img2.getNSlices();

		int f = img2.getFrame();
		
		imgA=new ImagePlus();

		ImageStack imga_s= new ImageStack(p.ni,p.nj);
		int bits = img2.getBitDepth();
		//channel 1
		for (int z=0; z<p.nz; z++){  
			img2.setPosition(1,z+1,f);	
			ImageProcessor impt;
			if(bits==32)
				impt=img2.getProcessor().convertToShort(false);
			else
				impt = img2.getProcessor();
			imga_s.addSlice("", impt);
		}


		imgA.setStack(img2.getTitle(),imga_s);
		setimagea();

//		if(p.dispwindows){
//			imgA.setTitle(imgA.getShortTitle());
//			imgA.show("");
//		}
		//imgA.setTitle("A2");

	}


	public static void loadA()
	{

		imgA=openImage("","");
		imgA.show("Image");

		p.ni=imgA.getWidth();
		p.nj=imgA.getHeight();
		p.nz=imgA.getNSlices();

		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
	}

	public static void loadB()
	{
		//todo check same dimensions than imga
		imgB=openImage("","");
		imgB.show("Image");	

		p.ni=imgB.getWidth();
		p.nj=imgB.getHeight();
		p.nz=imgB.getNSlices();
	}

	
	public static double[] pearson_corr(){

		Pearson ps = new Pearson(imgA, imgB, p);
		return ps.run();
		
	}
	
	/* Segment channel1 */
	
	public static void segmentA()
	{

		//NRxegions nreg= new NRegions(img, p);
		//nreginos
		//PSF only working in two region problem
		//3D only working for two problem

		currentImage=imgA.getTitle();

		DoneSignala = new CountDownLatch(1);
		if (p.usePSF==true || p.nz>1 ||p.nlevels==1 ) new Thread(new TwoRegions(imgA,p,DoneSignala,0)).start();
		else new Thread(new NRegions(imgA,p,DoneSignala, 0)).start();


		//		if(doingbatch){
		//			try {
		//				DoneSignala.await();
		//			}catch (InterruptedException ex) {}
		//			
		//			if(p.usePSF==true || p.nz>1 ||p.nlevels==1 ){
		//				if(p.findregionthresh)compute_connected_regions_a((int) 255*p.thresh,A_solverX);
		//				else compute_connected_regions_a((int) 255*p.thresh,null);
		//			}
		//			else{
		//				if(p.nlevels==2)compute_connected_regions_a(0.5,null);
		//				else compute_connected_regions_a(1.5,null);
		//			}
		//		}
	}

	public static void segmentb(){
		//NRegions nreg= new NRegions(img, p);

		currentImage=imgB.getTitle();
		DoneSignalb = new CountDownLatch(1);
		if (p.usePSF==true || p.nz>1 || p.nlevels==1) new Thread(new TwoRegions(imgB,p,DoneSignalb,1)).start();
		else new Thread(new NRegions(imgB,p,DoneSignalb,1)).start();



		//		if(doingbatch){
		//		try {
		//			DoneSignalb.await();
		//		}catch (InterruptedException ex) {}
		//			if(p.usePSF==true || p.nz>1 ||p.nlevels==1 ){
		//				if(p.findregionthresh)compute_connected_regions_b((int) 255*p.thresh,A_solverY);
		//				else compute_connected_regions_b((int) 255*p.thresh,null);
		//			}
		//			else{
		//				if(p.nlevels==2)compute_connected_regions_b(0.5,null);
		//				else compute_connected_regions_b(1.5,null);
		//			}
		//		}


	}

	/*  */
	
	public static void compute_connected_regions_a(double d, float [][][] RiN)
	{
		//IJ.log("connected ana"+d);
		ImagePlus maska_im= new ImagePlus();
		ImageStack maska_ims= new ImageStack(p.ni,p.nj);

		for (int z=0; z<p.nz; z++)
		{  
			byte[] maska_bytes = new byte[p.ni*p.nj];
			for (int i=0; i<p.ni; i++)
			{  
				for (int j=0;j< p.nj; j++)
				{  
					maska_bytes[j * p.ni + i] =  maskA[z][i][j] ;//(byte) ( (int)(255*maska[z][i][j]));	
					//IJ.log("byte" + (maska[0][i][j]));
					//if(i==277 && j==202 && z==7){IJ.log("test value :" +(maskA[z][i][j] & 0xFF));}
				}
			}
			ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
			bp.setPixels(maska_bytes);
			maska_ims.addSlice("", bp);
		}



		maska_im.setStack("test Ma",maska_ims);
		//IJ.log("float threshold :" + p.min_intensity +" byte threshold :" + (255*p.min_intensity));

		//maska_im.duplicate().show();

		//		if(p.mode_voronoi2 && false){
		//			//project mask on single slice (maximum values)
		//			ZProjector proj = new ZProjector(maska_im);
		//			proj.setImage(maska_im);
		//			proj.setStartSlice(1);proj.setStopSlice(p.nz);
		//			proj.setMethod(ZProjector.MAX_METHOD);
		//			proj.doProjection();
		//			maska_im=proj.getProjection();
		//			
		//			//maska_im.duplicate().show();
		//		}



		FindConnectedRegions fcr= new FindConnectedRegions(maska_im, maskA);//maska_im only
		float [][][] Ri ;
		if(p.mode_voronoi2)
		{
			Ri = new float [p.nz][p.ni][p.nj];
			for(int z=0; z<p.nz; z++)
			{
				for (int i=0; i<p.ni; i++)
				{  
					for (int j=0; j<p.nj; j++) 
					{
						Ri[z][i][j]=(float) p.min_intensity;
					}
				}
			}
		}
		else
		{
			if(RiN==null)
			{
				Ri = new float [p.nz][p.ni][p.nj];
				for(int z=0; z<p.nz; z++)
				{
					for (int i=0; i<p.ni; i++) 
					{
						for (int j=0; j<p.nj; j++) 
						{
							Ri[z][i][j]=(float) d;
						}
					}
				}
			}
			else {Ri=RiN;}
		}
		
		/* */
		
		//fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,false,true);
		if(p.debug)
			fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,true,p.save_images);//&&(!p.refinement)
		else
			fcr.run(d,0,p.maxves_size,p.minves_size,255*p.min_intensity,Ri,p.dispcolors&&(!p.refinement) ,p.save_images&&(!p.refinement));


		regions[0]=fcr.tempres;
		regionslist[0]=fcr.results;
		na=regionslist[0].size();
		if(!p.mode_voronoi2)
		{
			meana=meansize(regionslist[0]);
			if(p.nz>1)
				IJ.log(na + " objects found in X, mean volume : " + Tools.round(meana,2)+ " pixels.");
			else
				IJ.log(na + " objects found in X, mean area : " + Tools.round(meana,2)+ " pixels.");
		}
	}



	public static void compute_connected_regions_b(double d, float [][][] RiN)
	{
		ImagePlus maskb_im= new ImagePlus();
		ImageStack maskb_ims= new ImageStack(p.ni,p.nj);

		boolean cellmask;


		for (int z=0; z<p.nz; z++){  
			byte[] maskb_bytes = new byte[p.ni*p.nj];
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){ 
					cellmask=true;


					if(cellmask)
						maskb_bytes[j * p.ni + i] =  maskB[z][i][j];
					//maskb_bytes[j * p.ni + i] = (byte) ( (int)(255*maskb[z][i][j]));
					else
						maskb_bytes[j * p.ni + i]=0;

				}
			}
			ByteProcessor bp = new ByteProcessor(p.ni, p.nj);
			bp.setPixels(maskb_bytes);
			maskb_ims.addSlice("", bp);
		}



		maskb_im.setStack("",maskb_ims);
		FindConnectedRegions fcr= new FindConnectedRegions(maskb_im);

		float [][][] Ri ;

		if(p.mode_voronoi2){
			Ri = new float [p.nz][p.ni][p.nj];
			for(int z=0; z<p.nz; z++){
				for (int i=0; i<p.ni; i++) {  
					for (int j=0; j<p.nj; j++) {  
						Ri[z][i][j]=(float) p.min_intensityY;
					}
				}
			}
		}
		else{
			if(RiN==null){//==true   for testing with minimum intensity 
				Ri = new float [p.nz][p.ni][p.nj];
				for(int z=0; z<p.nz; z++){
					for (int i=0; i<p.ni; i++) {  
						for (int j=0; j<p.nj; j++) {  
							Ri[z][i][j]=(float) d;
						}
					}
				}
			}
			else {Ri=RiN;}
		}


		fcr.run(d,1,p.maxves_size,p.minves_size,255*p.min_intensityY,Ri,p.dispcolors &&(!p.refinement) ,p.save_images&&(!p.refinement));


		regions[1]=fcr.tempres;
		regionslist[1]=fcr.results;
		nb=regionslist[1].size();
		if(!p.mode_voronoi2){
			meanb=meansize(regionslist[1]);
			if(p.nz>1)
				IJ.log(nb + " objects found in Y, mean volume : " + Tools.round(meanb,2)+ " pixels.");
			else
				IJ.log(nb + " objects found in Y, mean area : " + Tools.round(meanb,2)+ " pixels.");
		}
	}


	public static void bcoloc_levels(){

		try{

			p.livedisplay=false;

			String dir1 = IJ.getDirectory("Select source folder...");
			if (dir1==null) return;

			//FileWriter fstream = new FileWriter(dir1+"result.txt");
			//BufferedWriter out = new BufferedWriter(fstream);
			PrintWriter out = new PrintWriter("resultH2B4" + ".csv");
			//		  
			//		  //Close the output stream
			//		  //out.close();
			//out.write("Hello");

			String[] list = new File(dir1).list();
			if (list==null) return;
			IJ.log("length" + list.length);
			for (int i=0; i<list.length; i++) {
				IJ.log("list i " + list[i]);
				boolean isDir = (new File(dir1+list[i])).isDirectory();
				if (!isDir && !list[i].startsWith(".")){
					//imgt = IJ.openImage(dir1+list[i]);
					imgA= IJ.openImage(dir1+list[i]);
					//imga.show();			
					imgB= IJ.openImage(dir1+list[i+1]);



					p.ni=imgA.getWidth();
					p.nj=imgA.getHeight();
					p.nz=imgA.getNSlices();

					//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);

					//imgb.show();
					IJ.log("imga : " + list[i] + "imgb" +list[i+1]);			
					i=i+1;
					Analysis.segmentA();			 

					try {
						DoneSignala.await();
					}catch (InterruptedException ex) {}


					Analysis.segmentb();			 

					try {
						DoneSignalb.await();
					}catch (InterruptedException ex) {}

					//TODO : why is it needed to reassign p.ni ...??
					p.ni=imgA.getWidth();
					p.nj=imgA.getHeight();
					p.nz=imgA.getNSlices();

					Analysis.coloc_levels();
					IJ.log("Mean vals  " + list[i] + ";" + a3b3 + ";" + a2b2 + ";" + a3b2  );	
					out.print(list[i] + ";" + a3b3 + ";" + a2b2  +";" + a3b2 );
					out.println();
					out.flush();
					//		fstream.newLine();


				}

			}

			out.flush();
			//out.close();
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

	}



	public static double coloc_levels(){

		compute_connected_regions_a(150,null);


		maxb=0;
		minb=Double.POSITIVE_INFINITY;
		IJ.log("imgb test " + imgB.getTitle());
		//		ImageProcessor imp;

		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
		//		for (int z=0; z<p.nz; z++){
		//			imgb.setSlice(z+1);
		//			imp=imgb.getProcessor();
		//			for (int i=0; i<p.ni; i++){  
		//				for (int j=0;j< p.nj; j++){  
		//					imageb[z][i][j]=imp.getPixel(i,j);	
		//					//IJ.log("deb"+ imageb[z][i][j]);
		//					if(imageb[z][i][j]>maxb)maxb=imageb[z][i][j];
		//					if(imageb[z][i][j]<minb)minb=imageb[z][i][j];
		//				}	
		//			}
		//		}

		//IJ.log("maxb " + maxb);
		int kb3a3=0;
		int kb2a3=0;
		int kb3a2=0;
		int kb2a2=0;
		int ka3=0;
		int ka2=0;
		//double sum=0;


		if(p.usePSF){
			for (int z=0; z<p.nz; z++){
				for (int i=0; i<p.ni; i++) {  
					for (int j=0; j<p.nj; j++) {  
						if(maxMaskA[z][i][j]!=0 && maxMaskA[z][i][j]!=1){
							ka3++;
							if(maxMaskB[z][i][j]>1)kb3a3++;
							if(maxMaskB[z][i][j]>2)kb2a3++;
							//k++;
							//sum+=(imageb[z][i][j]-minb)/(maxb-minb);
						}
						if(maxMaskA[z][i][j]>2){
							ka2++;
							if(maxMaskB[z][i][j]>1)kb3a2++;
							if(maxMaskB[z][i][j]>2)kb2a2++;
							//k++;
							//sum+=(imageb[z][i][j]-minb)/(maxb-minb);
						}

					}	
				}
			}	
		}
		else{
			for (int z=0; z<p.nz; z++){
				for (int i=0; i<p.ni; i++) {  
					for (int j=0; j<p.nj; j++) {  
						if(maxMaskA[z][i][j]!=0 && maxMaskA[z][i][j]!=1){
							ka3++;
							if(maxMaskB[z][i][j]>1)kb3a3++;
							if(maxMaskB[z][i][j]>2)kb2a3++;
							//k++;
							//sum+=(imageb[z][i][j]-minb)/(maxb-minb);
						}
						if(maxMaskA[z][i][j]>2){
							ka2++;
							if(maxMaskB[z][i][j]>1)kb3a2++;
							if(maxMaskB[z][i][j]>2)kb2a2++;
							//k++;
							//sum+=(imageb[z][i][j]-minb)/(maxb-minb);
						}

					}	
				}
			}	

			a2b2=((double )kb2a2)/ka2;
			a2b3=((double )kb3a2)/ka2;

			a3b2=((double )kb2a3)/ka3;
			a3b3=((double )kb3a3)/ka3;

		}
		IJ.log("Mean intensity in endosomes image : " + imgA.getTitle() + ";  "+ Tools.round(a3b3,3)+ ";  "+ Tools.round(a2b2,3));

		return 0;
	}
	//mean intensity values // % of pixels that are positive //% of veiscles that are positive //% distance based method 
	//pure  intesity based method


	public static ImagePlus openImage(String title, String path)
	{
		// open image dialog and opens it and returns
		Opener opener=new Opener();
		return opener.openImage(path);
	}


	public static void bcoloc(){
		try{

			doingbatch=true;
			boolean temp = p.dispvesicles;
			p.dispvesicles=false;
			p.livedisplay=false;

			String dir1;
			if (firstbcoloc) {

				dir1 = IJ.getDirectory("Select source folder...");
				if (dir1==null) return;
				dir=dir1;

			}
			else
				dir1=dir;


			IJ.log(dir1);
			//FileWriter fstream = new FileWriter(dir1+"result.txt");
			//BufferedWriter out = new BufferedWriter(fstream);
			//IJ.log("dir is " + dir1);

			String[] list = new File(dir1).list();
			if (list==null) return;

			IJ.log("length" + list.length);


			long Time = new Date().getTime(); //start time
			//PrintWriter out = new PrintWriter(dir1 +"Colocalization"+ Time +  ".csv");
			//PrintWriter out2 = new PrintWriter(dir1 +"Vesicles_data"+ Time +  ".csv");

			PrintWriter out = new PrintWriter(dir1 +"Colocalization"+ Time   + ".csv");
			PrintWriter out2 = new PrintWriter(dir1 +"X_Vesicles_data"+ Time + ".csv");
			PrintWriter out3 = new PrintWriter(dir1 +"Y_Vesicles_data"+ Time + ".csv");
			//PrintWriter out = new PrintWriter(dir1.replaceAll("/", "_") + ".csv");

			Arrays.sort(list);
			out.print(Arrays.toString(list));
			out.println();

			out.print("File"+ ";" + "Objects ch X" + ";" + "Mean size ch X"  +";" 
					+ "Objects ch Y"+";" + "Mean size ch Y" +";" + "Colocalization X in Y"
					+";" + "Colocalization Y in X"
					+";" + "Mean Y intensity in X objects"
					+";" + "Mean X intensity in Y objects");
			out.println();


			//		  
			//		  //Close the output stream
			//		  //out.close();
			//out.write("Hello");


			for (int i=0; i<list.length; i++) {

				IJ.log("list i " + list[i]);

				boolean isDir = (new File(dir1+list[i])).isDirectory();
				if (	
						!isDir &&
						!list[i].startsWith(".") &&
						!list[i].startsWith("Coloc") &&
						!list[i].startsWith("Vesicles")
						){
					//imgt = IJ.openImage(dir1+list[i]);
					imgA= IJ.openImage(dir1+list[i]);		
					imgB= IJ.openImage(dir1+list[i+1]);



					p.ni=imgA.getWidth();
					p.nj=imgA.getHeight();
					p.nz=imgA.getNSlices();

					//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);

					//imgb.show();
					//IJ.log("imga : " + list[i]);


					Analysis.segmentA();			 

					try{
						DoneSignala.await();
					}catch (InterruptedException ex) {}


					//IJ.log("imgb :" +list[i+1]);

					Analysis.segmentb();			 

					try {
						DoneSignalb.await();
					}catch (InterruptedException ex) {}

					//TODO : why is it needed to reassign p.ni ...??
					p.ni=imgA.getWidth();
					p.nj=imgA.getHeight();
					p.nz=imgA.getNSlices();



					//no looptest
					if(!p.looptest){

						computeOverallMask();
						regionslist[0]=removeExternalObjects(regionslist[0]);
						regionslist[1]=removeExternalObjects(regionslist[1]);
						setRegionsLabels(regionslist[0], regions[0]);
						setRegionsLabels(regionslist[1], regions[1]);
						setIntensitiesandCenters(regionslist[0],imagea);
						setIntensitiesandCenters(regionslist[1],imageb);
						na=regionslist[0].size();
						nb=regionslist[1].size();



						//out2.print("Cell"+ list[i]);
						///out2.println();
						if(i==0){out2.print("Image number" + ";" + "Region in X"+ ";" + "Overlap with Y" + ";" + "Size" + ";" +
								"Intensity" + ";" + "MColoc size" + ";"+ "MColoc Intensity" + ";" + "Single Coloc" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out2.println();}
						double colocAB=Tools.round(colocsegAB( i/2),4);

						//out3.print("Cell"+ list[i]);
						//out3.println();
						if(i==0){out3.print("Image number" + ";" + "Region in Y"+ ";" + "Overlap with X" + ";" + "Size" + ";" +
								"Intensity" + ";" + "MColoc size" + ";"+ "MColoc Intensity" + ";" + "Single Coloc" + ";"  + "Coord X"+ ";" + "Coord Y"+ ";" + "Coord Z");
						out3.println();}
						double colocBA=Tools.round(colocsegBA(out3, i/2),4);
						double colocA=Tools.round(colocsegA(null),4);
						double colocB=Tools.round(colocsegB(null),4);
						out.print(list[i] + ";" + na + ";" +
								Tools.round(meana , 4)  +";" + nb +";" + 
								Tools.round(meanb , 4) +";" +
								colocAB +";" + 
								colocBA + ";"+
								colocA+ ";"+
								colocB);
						out.println();
						out.flush();
						out2.flush();
						out3.flush();
					}
					else{
						out.print(list[i] + ";"); 
						p.min_intensity=0.15;
						p.min_intensityY=0.15;
						p.minves_size=5;
						p.maxves_size=500;
						p.colocthreshold=0.5;

						double val;
						int ival;

						computeOverallMask();
						for(val=0.1; val<0.85; val+=0.05){
							p.min_intensity=val;
							//IJ.log("minint" + val);
							out.print(String.format(
									"Min intensity X  %7.2e Min intensity Y %7.2e Min vesicle size %d Max vesicle size %d Overlap threshold %7.2e ,", 
									p.min_intensity,p.min_intensityY,p.minves_size,p.maxves_size,p.colocthreshold));
							//							out.print("Min intensity X " + p.min_intensity +" " +
							//									"Min intensity Y" + p.min_intensityY+" " +
							//									"Min vesicle size " + p.minves_size+" " +
							//									"Max vesicle size " + p.maxves_size+" " +
							//									"Overlap threshold " + p.colocthreshold + ";"
							//									);
							computeRegions();

							regionslist[0]=removeExternalObjects(regionslist[0]);
							regionslist[1]=removeExternalObjects(regionslist[1]);
							setIntensitiesandCenters(regionslist[0],imagea);
							setIntensitiesandCenters(regionslist[1],imageb);
							setRegionsLabels(regionslist[0], regions[0]);
							setRegionsLabels(regionslist[1], regions[1]);
							na=regionslist[0].size();
							nb=regionslist[1].size();

							calcColoc(out,out2,list,i);
						}
						p.min_intensity=0.15;


						for(val=0.1; val<0.85; val+=0.05){
							//IJ.log("minintY" + val);
							p.min_intensityY=val;
							out.print(String.format(
									"Min intensity X  %7.2e Min intensity Y %7.2e Min vesicle size %d Max vesicle size %d Overlap threshold %7.2e ,", 
									p.min_intensity,p.min_intensityY,p.minves_size,p.maxves_size,p.colocthreshold));
							computeRegions();

							regionslist[0]=removeExternalObjects(regionslist[0]);
							regionslist[1]=removeExternalObjects(regionslist[1]);
							setIntensitiesandCenters(regionslist[0],imagea);
							setIntensitiesandCenters(regionslist[1],imageb);
							setRegionsLabels(regionslist[0], regions[0]);
							setRegionsLabels(regionslist[1], regions[1]);
							na=regionslist[0].size();
							nb=regionslist[1].size();

							calcColoc(out,out2,list,i);
						}


						p.min_intensityY=0.15;


						for(ival=2; ival<30; ival+=3){
							p.minves_size=ival;
							out.print(String.format(
									"Min intensity X  %7.2e Min intensity Y %7.2e Min vesicle size %d Max vesicle size %d Overlap threshold %7.2e ,", 
									p.min_intensity,p.min_intensityY,p.minves_size,p.maxves_size,p.colocthreshold));
							computeRegions();
							regionslist[0]=removeExternalObjects(regionslist[0]);
							regionslist[1]=removeExternalObjects(regionslist[1]);
							setIntensitiesandCenters(regionslist[0],imagea);
							setIntensitiesandCenters(regionslist[1],imageb);
							setRegionsLabels(regionslist[0], regions[0]);
							setRegionsLabels(regionslist[1], regions[1]);
							na=regionslist[0].size();
							nb=regionslist[1].size();
							calcColoc(out,out2,list,i);
						}

						p.minves_size=5;

						for(ival=100; ival<1600; ival+=100){
							p.maxves_size=ival;
							out.print(String.format(
									"Min intensity X  %7.2e Min intensity Y %7.2e Min vesicle size %d Max vesicle size %d Overlap threshold %7.2e ,", 
									p.min_intensity,p.min_intensityY,p.minves_size,p.maxves_size,p.colocthreshold));
							computeRegions();
							regionslist[0]=removeExternalObjects(regionslist[0]);
							regionslist[1]=removeExternalObjects(regionslist[1]);
							setIntensitiesandCenters(regionslist[0],imagea);
							setIntensitiesandCenters(regionslist[1],imageb);
							setRegionsLabels(regionslist[0], regions[0]);
							setRegionsLabels(regionslist[1], regions[1]);
							na=regionslist[0].size();
							nb=regionslist[1].size();
							calcColoc(out,out2,list,i);
						}


						p.maxves_size=500;

						for(val=0.1; val<1; val+=0.1){
							p.colocthreshold=val;
							out.print(String.format(
									"Min intensity X  %7.2e Min intensity Y %7.2e Min vesicle size %d Max vesicle size %d Overlap threshold %7.2e ,", 
									p.min_intensity,p.min_intensityY,p.minves_size,p.maxves_size,p.colocthreshold));
							computeRegions();
							regionslist[0]=removeExternalObjects(regionslist[0]);
							regionslist[1]=removeExternalObjects(regionslist[1]);
							setIntensitiesandCenters(regionslist[0],imagea);
							setIntensitiesandCenters(regionslist[1],imageb);
							setRegionsLabels(regionslist[0], regions[0]);
							setRegionsLabels(regionslist[1], regions[1]);
							na=regionslist[0].size();
							nb=regionslist[1].size();
							calcColoc(out,out2,list,i);
						}

						p.colocthreshold=0.5;

						out.println();
						out.flush();

					}


					//					if(p.nz>1)
					//						IJ.log(
					//								"Objects in X :" + na + " Mean volume in X " + Tools.round(meana , 4)+
					//								" Objects in Y :" + nb + " Mean volume in Y " + Tools.round(meanb , 4) +
					//								" ColocXY : " + colocAB +
					//								" ColocYX : " + colocBA +
					//								" IntX : " + colocA +
					//								" IntY : " + colocB
					//								);
					//					else
					//						IJ.log(
					//								"Objects in X :" + na + " Mean area in X " + Tools.round(meana , 4)+
					//								" Objects in Y :" + nb + " Mean area in Y " + Tools.round(meanb , 4) +
					//								" ColocXY : " + colocAB +
					//								" ColocYX : " + colocBA +
					//								" IntX : " + colocA + 
					//								" IntY : " + colocB
					//								);
					i=i+1;
					//		fstream.newLine();
					IJ.log(" ");

				}

			}
			out.println();
			out.println();
			out.print("Min intensity X " + p.min_intensity +";" +
					"Min intensity Y" + p.min_intensityY+";" +
					"Min vesicle size" + p.minves_size+";" +
					"Max vesicle size" + p.maxves_size+";" +
					"Overlap threshold" + p.colocthreshold
					);
			out.flush();
			out2.close();
			out3.close();
			out.close();
			//out.close();
			p.dispvesicles=temp;
		}catch (Exception e){//Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}

		doingbatch=false;
	}


	public static void coloc(){

		//display colocalization result image
		computeOverallMask();
		regionslist[0]=removeExternalObjects(regionslist[0]);
		regionslist[1]=removeExternalObjects(regionslist[1]);
		setIntensitiesandCenters(regionslist[0],imagea);
		setIntensitiesandCenters(regionslist[1],imageb);
		setRegionsLabels(regionslist[0], regions[0]);
		setRegionsLabels(regionslist[1], regions[1]);
		na=regionslist[0].size();
		nb=regionslist[1].size();

		MasksDisplay md= new MasksDisplay(p.ni,p.nj,p.nz,p.nlevels,p.cl,p);

		if (Analysis.p.cAB){
			//IJ.log("coloc AB");
			IJ.log("Colocating  objects in X  : " + Tools.round(colocsegAB(0),3) 
					+ " (" + positiveA + " vesicles over " + na + " )" );}
		if (Analysis.p.cBA){
			IJ.log("Colocating  objects in Y  : " + Tools.round(colocsegBA(null,0),3) 
					+ " (" + positiveB + " vesicles over " + nb + " )" );	
		}

		if (Analysis.p.cint){
			//IJ.log("coloc A");
			IJ.log("Mean intensity of channel Y in objects X: " + Tools.round(colocsegA(null),3));

		}

		if (Analysis.p.cintY){
			//IJ.log("coloc A");
			IJ.log("Mean intensity of channel X in objects Y: " + Tools.round(colocsegB(null),3));

		}

		//if (Analysis.p.ccorr){}
		//add cellmasks
		md.displaycoloc(regionslist[0],regionslist[1]);
		md.displaycolocpositiveA(regionslist[0]);
		md.displaycolocpositiveB(regionslist[1]);

		//		if(p.usecellmaskX){cellmask=cellMaskABinary[z][i][j]>254;} 
		//		if(p.usecellmaskY){cellmask=cellMaskBBinary[z][i][j]>254;}
		//		if(p.usecellmaskY && p.usecellmaskX){cellmask=
		//				cellMaskBBinary[z][i][j]>254  && 
		//				cellMaskABinary[z][i][j]>254
		//				;}
	}


	public static double colocsegA(PrintWriter out){

//		maxb=0;
//		minb=Double.POSITIVE_INFINITY;
//		//IJ.log("imgb test " + imgb.getTitle());
//		ImageProcessor imp;
//
//		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
//		imageb= new double [p.nz][p.ni][p.nj];
//
//		for (int z=0; z<p.nz; z++){
//			imgB.setSlice(z+1);
//			imp=imgB.getProcessor();
//			for (int i=0; i<p.ni; i++){  
//				for (int j=0;j< p.nj; j++){  
//					imageb[z][i][j]=imp.getPixel(i,j);	
//					//IJ.log("deb"+ imageb[z][i][j]);
//					if(imageb[z][i][j]>maxb)maxb=imageb[z][i][j];
//					if(imageb[z][i][j]<minb)minb=imageb[z][i][j];
//				}	
//			}
//		}

		double sum=0;
		int objects=0;
		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;
			sum+=regionsum(r,imageb,out);
		}

		return (sum/objects);
		//return (((sum/objects) - minb)/(maxb-minb));


	}


	public static void setimagea(){
		maxa=0;
		mina=Double.POSITIVE_INFINITY;
		//IJ.log("imgb test " + imgb.getTitle());
		ImageProcessor imp;

		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
		imagea= new double [p.nz][p.ni][p.nj];

		for (int z=0; z<p.nz; z++){
			imgA.setSlice(z+1);
			imp=imgA.getProcessor();
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					imagea[z][i][j]=imp.getPixel(i,j);	
					//IJ.log("deb"+ imageb[z][i][j]);
					if(imagea[z][i][j]>maxa)maxa=imagea[z][i][j];
					if(imagea[z][i][j]<mina)mina=imagea[z][i][j];
				}	
			}
		}

		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j<p.nj; j++){  
					imagea[z][i][j]= (imagea[z][i][j] -mina)/(maxa-mina);		
				}	
			}
		}


	}

	public static void setimageb(){
		maxb=0;
		minb=Double.POSITIVE_INFINITY;
		//IJ.log("imgb test " + imgb.getTitle());
		ImageProcessor imp;

		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
		imageb= new double [p.nz][p.ni][p.nj];

		for (int z=0; z<p.nz; z++){
			imgB.setSlice(z+1);
			imp=imgB.getProcessor();
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					imageb[z][i][j]=imp.getPixel(i,j);	
					//IJ.log("deb"+ imageb[z][i][j]);
					if(imageb[z][i][j]>maxb)maxb=imageb[z][i][j];
					if(imageb[z][i][j]<minb)minb=imageb[z][i][j];
				}	
			}
		}


		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j<p.nj; j++){  
					imageb[z][i][j]= (imageb[z][i][j] -minb)/(maxb-minb);		
				}	
			}
		}

	}


	public static double colocsegB(PrintWriter out){

		//		maxa=0;
		//		mina=Double.POSITIVE_INFINITY;
		//		//IJ.log("imgb test " + imgb.getTitle());
		//		ImageProcessor imp;
		//
		//		//IJ.log("nz " + p.nz + "ni " + p.ni+ "nj " + p.nj);
		//		imagea= new double [p.nz][p.ni][p.nj];
		//
		//		for (int z=0; z<p.nz; z++){
		//			imgA.setSlice(z+1);
		//			imp=imgA.getProcessor();
		//			for (int i=0; i<p.ni; i++){  
		//				for (int j=0;j< p.nj; j++){  
		//					imagea[z][i][j]=imp.getPixel(i,j);	
		//					//IJ.log("deb"+ imageb[z][i][j]);
		//					if(imagea[z][i][j]>maxa)maxa=imagea[z][i][j];
		//					if(imagea[z][i][j]<mina)mina=imagea[z][i][j];
		//				}	
		//			}
		//		}

		double sum=0;
		int objects=0;
		for (Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;
			sum+=regionsum(r,imagea,out);
		}

		return (sum/objects);
		//		return (((sum/objects) - mina)/(maxa-mina));


	}

	public static double colocsegAB(int imgnumber){

		double totalsignal=0;
		double colocsignal=0;
		
		
		int objectscoloc=0;
		//int objects=0;
		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();

			if (regioncoloc(r,regionslist[1], regions[1],maskA, imgnumber))objectscoloc++;

			//IJ.log(r.toString() + "ncoloc"+ objectscoloc);
		totalsignal+=r.rsize*r.intensity;
		colocsignal+=r.rsize*r.intensity*r.overlap;
		}

		
		positiveA=objectscoloc;
		//return (((double)objectscoloc)/objects);
		return (colocsignal/totalsignal);
	}
	
	
	public static double colocsegABsize(int imgnumber){

		double totalsize=0;
		double colocsize=0;
		
		
		int objectscoloc=0;
		//int objects=0;
		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();
			//objects++;
			if (regioncoloc(r,regionslist[1], regions[1],maskA, imgnumber))objectscoloc++;

		totalsize+=r.rsize;
		colocsize+=r.rsize*r.overlap;
		}

		
		positiveA=objectscoloc;
		return (colocsize/totalsize);
	}
	
	public static double colocsegABnumber(){

			
		int objectscoloc=0;
		int objects=0;
		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;
			if (r.colocpositive)objectscoloc++;
			//IJ.log(r.toString() + "ncoloc"+ objectscoloc);
		}

		positiveA=objectscoloc;
		return (((double)objectscoloc)/objects);
	}
	
	public static double colocsegBAnumber(){

		
		
		int objectscoloc=0;
		int objects=0;
		for (Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
			Region r = it.next();
			//IJ.log("obj" + r.value);
			objects++;
			if (r.colocpositive)objectscoloc++;
			//if(p.livedisplay)IJ.log(r.toString() + "ncoloc"+ objectscoloc);

		}
		positiveB=objectscoloc;
		return ( ((double)objectscoloc)/objects);


	}


	public static double colocsegBA(PrintWriter out, int imgnumber){

		double totalsignal=0;
		double colocsignal=0;
		
		int objectscoloc=0;
		//int objects=0;
		for (Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
			Region r = it.next();
			//IJ.log("obj" + r.value);

			if (regioncoloc(r,regionslist[0], regions[0],maskB, imgnumber))objectscoloc++;

			//if(p.livedisplay)IJ.log(r.toString() + "ncoloc"+ objectscoloc);
			totalsignal+=r.rsize*r.intensity;
			colocsignal+=r.rsize*r.intensity*r.overlap;
		}
		positiveB=objectscoloc;
		//return ( ((double)objectscoloc)/objects);
		return (colocsignal/totalsignal);

	}

	
	public static double colocsegBAsize(PrintWriter out, int imgnumber){

		double totalsize=0;
		double colocsize=0;
		
		int objectscoloc=0;
		//int objects=0;
		for (Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
			Region r = it.next();

			if (regioncoloc(r,regionslist[0], regions[0],maskB, imgnumber))objectscoloc++;

			totalsize+=r.rsize;
			colocsize+=r.rsize*r.overlap;
		}
		positiveB=objectscoloc;
		return (colocsize/totalsize);

	}



	public static boolean regioncoloc(Region r,ArrayList<Region> regionlist, short [] [] [] regions,byte [][][] mask, int imgnumber){
		boolean positive=false;
		int count=0;
		int countcoloc=0;
		int previousvalcoloc=0;
		int valcoloc;
		boolean oneColoc=true;
		double intColoc=0;
		double sizeColoc=0;
		int osxy=Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			valcoloc=regions[p.pz][p.px][p.py];
			//IJ.log("valcoloc " + valcoloc);
			if(valcoloc > 0){
				countcoloc++;
				if(previousvalcoloc!=0 && valcoloc!=previousvalcoloc)oneColoc=false;
				intColoc+=regionlist.get(valcoloc-1).intensity;
				sizeColoc+=regionlist.get(valcoloc-1).points;
				previousvalcoloc=valcoloc;
			}
			count++;
		}

		positive=((double)countcoloc)/count > p.colocthreshold;
		r.colocpositive=positive;
		r.overlap=(float) Tools.round(((double)countcoloc)/count,3) ;
		r.over_size=(float) Tools.round(((double)sizeColoc)/countcoloc,3);
		if(p.nz==1)
			r.over_size=(float) Tools.round( r.over_size/(osxy*osxy),3);
		else
			r.over_size=(float) Tools.round( r.over_size/(osxy*osxy*osxy),3);
		
		r.over_int=(float) Tools.round(((double)intColoc)/countcoloc,3);
		r.singlec=oneColoc;

		//		if(out != null){
		//			//regionIntensityAndCenter(r,mask);
		//			regionCenter(r);
		//			out.print(imgnumber 
		//					+"," + r.value 
		//					+"," + Tools.round(((double)countcoloc)/count,3) 
		//					+","+ count 
		//					+","+ Tools.round(r.intensity,3)
		//					+","+ Tools.round(((double)sizeColoc)/countcoloc,3)
		//					+","+ Tools.round(((double)intColoc)/countcoloc,3)
		//					+","+ oneColoc
		//					+","+ Tools.round(r.cx,2)
		//					+","+ Tools.round(r.cy,2)
		//					+","+ Tools.round(r.cz,2)
		//					);
		//			out.println();
		//		}
		return (positive);
	}


	public static void printobjectsA(PrintWriter out, int imgnumber){
		
		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();
			printobject(r,out, imgnumber);
		}
	}

	public static void printobjectsB(PrintWriter out, int imgnumber){

		for (Iterator<Region> it = regionslist[1].iterator(); it.hasNext();) {
			Region r = it.next();
			printobject(r,out, imgnumber);
		}
	}


	public static void printobjects(PrintWriter out, int imgnumber){

		for (Iterator<Region> it = regionslist[0].iterator(); it.hasNext();) {
			Region r = it.next();
			printobject(r,out, imgnumber);
		}
	}

	public static  void printobject(Region r, PrintWriter out, int imgnumber){

		//		int count=0;

		//		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
		//			Pix p = it.next();
		////			count++;
		//		}
		//IJ.log("print object " + r.value);
		if(out != null && p.nchannels==1){
			//regionIntensityAndCenter(r,mask);
			regionCenter(r);

			if(p.nz>1){
/*				out.print(imgnumber 
						+";" + r.value  
						+";"+ r.rsize //size
						+";"+ Tools.round(r.perimeter,3) //perimeter
						+";"+ r.length // no length in 3D 
						+";"+ Tools.round(r.intensity,3)					
						+";"+ Tools.round(r.cx,2)
						+";"+ Tools.round(r.cy,2)
						+";"+ Tools.round(r.cz,2)
						);
				out.println();			*/
				
				out.print(imgnumber 
						+"," + Tools.round(r.cx,2)  
						+","+ Tools.round(r.cy,2)
						+","+ Tools.round(r.cz,2)
						+","+ Tools.round(r.rsize,3) //perimeter
						+","+ Tools.round(r.intensity,3)
						+","+ r.length // no length in 3D 					
						+","+ Tools.round(0.0,2)
						+","+ Tools.round(0.0,2)
						+","+ Tools.round(0.0,2)
						);
				out.println();
				
				
			}
			else
			{
				out.print(imgnumber 
						+";" + r.value  
						+";"+ r.rsize //size
						+";"+ Tools.round(r.perimeter,3) //perimeter
						+";"+ r.length //length
						+";"+ Tools.round(r.intensity,3)					
						+";"+ Tools.round(r.cx,2)
						+";"+ Tools.round(r.cy,2)
						+";"+ Tools.round(r.cz,2)
						);
				out.println();			
			}

		}

		//		double size=count;
		//		if(p.subpixel){
		//			size= count/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
		//		}
		if(out != null && p.nchannels==2){
			//regionIntensityAndCenter(r,mask);
			regionCenter(r);
			out.print(imgnumber 
					+";" + r.value  
					+";"+ Tools.round(r.rsize,3) //size
					+";"+ Tools.round(r.perimeter,3) //perimeter
					+";"+ r.length //length
					+";"+ Tools.round(r.intensity,3)					
					+";"+ Tools.round(r.overlap,3)
					+";"+ Tools.round(r.over_size,3)
					+";"+ Tools.round(r.over_int,3)
					+";"+ r.singlec
					+";"+ Tools.round(r.coloc_o_int,3)
					+";"+ Tools.round(r.cx,2)
					+";"+ Tools.round(r.cy,2)
					+";"+ Tools.round(r.cz,2)
					);
			out.println();
		}


	}


	public static void setIntensitiesandCenters(ArrayList<Region> regionlist, double [][][] image){
		//IJ.log("starting setint" );
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			//IJ.log("rvalue int comp" + r.value);
			regionIntensityAndCenter(r,image);
		}
	}

	public static void setPerimeter(ArrayList<Region> regionlist, int [][][] regionsA){
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			if(p.nz==1)
				regionPerimeter(r,regionsA);
			else
				regionPerimeter3D(r,regionsA);

		}
	}


	public static void regionPerimeter(Region r, int [] [] [] regionsA){
		//2 Dimensions only
		double pr=0;
		//int rvalue= r.value;

		//IJ.log("region: " + r.value);
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			int edges=0;
			Pix v = it.next();
			//count number of free edges
			if(v.px!=0 && v.px!=p.ni-1 && v.py!=0 && v.py!=p.ni-1){//not on edges of image
				if(regionsA[v.pz][v.px-1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px+1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px][v.py-1]==0)edges++;
				if(regionsA[v.pz][v.px][v.py+1]==0)edges++;//!=rvalue
			}
			if(edges==1)pr+=1;
			if(edges==2)pr+=Math.sqrt(2);
			if(edges==3)pr+=2;
			//IJ.log("coord " + v.px + ", " + v.py +", "+ v.pz +"edges " + edges);


		}
		//return (sum/count);
		r.perimeter=pr;
		//IJ.log("perimeter " +pr);
		if(Analysis.p.subpixel){r.perimeter=pr/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);}
		//IJ.log("perimeter " +r.perimeter);

	}

	public static void regionPerimeter3D(Region r, int [] [] [] regionsA){
		//2 Dimensions only
		double pr=0;
		//int rvalue= r.value;

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			int edges=0;
			Pix v = it.next();
			//count number of free edges
			if(v.px!=0 && v.px!=p.ni-1 && v.py!=0 && v.py!=p.ni-1 && v.pz!=0 && v.pz!=p.nz-1){//not on edges of image
				if(regionsA[v.pz][v.px-1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px+1][v.py]==0)edges++;
				if(regionsA[v.pz][v.px][v.py-1]==0)edges++;
				if(regionsA[v.pz][v.px][v.py+1]==0)edges++;
				if(regionsA[v.pz+1][v.px][v.py]==0)edges++;
				if(regionsA[v.pz-1][v.px][v.py]==0)edges++;
			}
			if(edges==1)pr+=1;
			if(edges==2)pr+=Math.sqrt(2);
			if(edges==3)pr+=Math.sqrt(2);
			if(edges==4)pr+=Math.sqrt(2);
			if(edges==5)pr+=2*Math.sqrt(2);
			//IJ.log("coord " + v.px + ", " + v.py +", "+ v.pz +"edges " + edges);


		}
		//return (sum/count);
		r.perimeter=pr;
		//IJ.log("perimeter " +pr);
		if(Analysis.p.subpixel){r.perimeter=pr/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);}
		//IJ.log("perimeter " +r.perimeter);

	}



	public static void setlength(ArrayList<Region> regionlist, int [][][] regionsA){
		//2D only yet
		ImagePlus skeleton= new ImagePlus();
		//compute skeletonization
		int osxy=1;
		//int osz=1;
		if(p.subpixel && p.refinement){
			osxy=p.oversampling2ndstep*p.interpolation;
//			if(p.nz>1){
//				osz=p.oversampling2ndstep*p.interpolation;
//			}
		}
		int di,dj;
		di=p.ni *osxy;
		dj=p.nj *osxy;
		byte[] mask_bytes = new byte[di*dj];
		for (int i=0; i<di; i++) {
			for (int j=0; j<dj; j++) {  
				if(regionsA[0][i][j]>0)
					mask_bytes[j * di + i]= (byte) 0;
				else
					mask_bytes[j * di + i]=(byte) 255;
			}
		}
		ByteProcessor bp = new ByteProcessor(di, dj);
		bp.setPixels(mask_bytes);
		skeleton.setProcessor("Skeleton",bp);
		//skeleton.show();


		//do voronoi in 2D on Z projection
		IJ.run(skeleton, "Skeletonize", "");
		//skeleton.show();
		//		if (Analysis.p.save_images){
		//		String savepath = Analysis.p.wd + Analysis.currentImage.substring(0,Analysis.currentImage.length()-4) + "_skel_c1" +".zip";
		//		IJ.saveAs(skeleton, "ZIP", savepath);
		//		}
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			regionlength(r, skeleton);
		}


	}


	public static void regionlength(Region r, ImagePlus skel){
		//2 Dimensions only
		int length=0;
		//IJ.log("object clength "+ r.value);
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix v = it.next();
			//count number of pixels in skeleton
			if(skel.getProcessor().getPixel(v.px, v.py)==0)length++;
			//if(skel.getProcessor().getPixel(v.px, v.py)==0)IJ.log("coord " + v.px + ", " + v.py);

		}
		//return (sum/count);
		r.length=length;
		if(Analysis.p.subpixel){r.length= ((double)length)/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);}



	}

	public static void SetRegionsObjsVoronoi(ArrayList<Region> regionlist, ArrayList<Region> regionsvoronoi,float [][][] ri){
		int x,y,z;
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			x=r.pixels.get(0).px;
			y=r.pixels.get(0).py;
			z=r.pixels.get(0).pz;
			//IJ.log("region" + r.value+ "x "+x +  " y "+y + " z " +z + "label " + ri[z][x][y]);
			r.rvoronoi=regionsvoronoi.get((int) ri[z][x][y]);

		}
	}



	public static void setregionsThresholds(ArrayList<Region> regionlist, float [][][] ri,float [][][] ro){
		int x,y,z;
		for (Iterator<Region> it = regionlist.iterator(); it.hasNext();) {
			Region r = it.next();
			x=r.pixels.get(0).px;
			y=r.pixels.get(0).py;
			z=r.pixels.get(0).pz;
			r.beta_in=ri[z][x][y]/255;
			r.beta_out=ro[z][x][y]/255;
		}
	}



	public static double regionsum(Region r, double [] [] [] image, PrintWriter out){

		int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
		int fz2;
		if(Analysis.p.nz>1)fz2=factor2; else fz2=1;
		
		int count=0;
		double sum=0;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			sum+=image[p.pz/fz2][p.px/factor2][p.py/factor2];
			count++;
		}

		//		if(out!=null){
		//			out.print("");
		//			out.println();
		//		}
		//IJ.log("region" + r.value + "mean is " + (sum/count) );
		r.coloc_o_int=(sum/count);
		return (sum/count);

	}


	public static void regionCenter(Region r){

		int count=0;
		double sumx=0;
		double sumy=0;
		double sumz=0;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			sumx+=p.px;
			sumy+=p.py;
			sumz+=p.pz;
			count++;
		}

		//		if(out!=null){
		//			out.print("");
		//			out.println();
		//		}
		//print IJ.log("region" + r.value + "mean is " + (sum/count) );
		//return (sum/count);
		r.cx= (float) (sumx/count);
		r.cy= (float) (sumy/count);
		r.cz= (float) (sumz/count);
		if(Analysis.p.subpixel){

			r.cx= r.cx/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cy= r.cy/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cz= r.cz/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
		}

	}



	public static void regionIntensityAndCenter(Region r, double [] [] [] image){

		int count=0;
		double sum=0;
		double sumx=0;
		double sumy=0;
		double sumz=0;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {
			Pix p = it.next();
			if(!Analysis.p.refinement){
				sum+= image[p.pz][p.px][p.py] ;
			}
			//sum+= mask[p.pz][p.px][p.py] & 0xFF; //for conversion, corrects sign problem

			//if(p.px==277 && p.py==202 && p.pz==7){IJ.log("test value rint:" + maskA[p.pz][p.px][p.py]);}
			//if(r.value==6)IJ.log("value byte" + mask[p.pz][p.px][p.py] + " x"+ p.px +"y" + p.py + "z"+ p.pz);
			sumx+=p.px;
			sumy+=p.py;
			sumz+=p.pz;
			count++;
		}

		//return (sum/count);
		//r.intensity=(sum/(count*255));
		if(!Analysis.p.refinement){
			r.intensity=(sum/(count));
		}//done in refinement
		//IJ.log("inten " + r.intensity);

		r.cx= (float) (sumx/count);
		r.cy= (float) (sumy/count);
		r.cz= (float) (sumz/count);

		if(Analysis.p.subpixel){
			r.cx= r.cx/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cy= r.cy/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
			r.cz= r.cz/(Analysis.p.oversampling2ndstep*Analysis.p.interpolation);
		}

	}

	/**
	 * 
	 *  Allocate a byte maskA based on the double mask
	 *  
	 */
	
	public static void setMaskaTworegions( double [] [] [] mask)
	{
		maskA=new byte [p.nz][p.ni][p.nj];
		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					maskA[z][i][j] =  (byte) ( (int)(255*mask[z][i][j]));
				}
			}
		}
	}

	public static void setMaskaTworegions( double [] [] [] mask, ByteProcessor bp){

		maskA=new byte [p.nz][p.ni][p.nj];
		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					maskA[z][i][j] =  (byte) ( (int)(255*mask[z][i][j]));
					if ((byte) (bp.getPixel(i,j) & 0xFF)==0)
						maskA[z][i][j]=0;
				}
			}
		}


	}


	public static void setMaskbTworegions( double [] [] [] mask){

		maskB=new byte [p.nz][p.ni][p.nj];
		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					maskB[z][i][j] =  (byte) ( (int)(255*mask[z][i][j]));	
				}
			}
		}


	}



	public static void setmaska( int [] [] [] mask){

		maskA=new byte [p.nz][p.ni][p.nj];
		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					maskA[z][i][j] = (byte) ( (int)(mask[z][i][j])) ;

					//((double)mask[z][i][j]) / 255;		
				}
			}
		}


	}

	public static void setmaskb( int [] [] [] mask){

		maskB=new byte [p.nz][p.ni][p.nj];
		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					maskB[z][i][j] = (byte) ( (int)(mask[z][i][j])) ;
					//maskb[z][i][j] = ((double)mask[z][i][j]) / 255;		
				}
			}
		}

	}

	//	public static void testpatch(ArrayList<Region> regionslist, double [][][] image){
	//		IJ.log("testpacth");
	//		double totalsize=0;
	//		int objects=0;
	//		AnalysePatch ap;
	//		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
	//			Region r = it.next();
	//			if (r.value==25){
	//				ap=new AnalysePatch( image,  r, p, 1, 0);
	//				ap.run();
	//			}
	//		}
	//
	//
	//
	//	}


	public static double meansurface(ArrayList<Region> regionslist){

		double totalsize=0;
		int objects=0;
		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;	       			
			totalsize+= r.perimeter;
		}

		//if(Analysis.p.subpixel){return (totalsize/objects)/(Math.pow(Analysis.p.oversampling2ndstep*Analysis.p.interpolation, 2));}
		return(totalsize/objects);

	}

	public static double meanlength(ArrayList<Region> regionslist){

		double totalsize=0;
		int objects=0;
		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;	       			
			totalsize+= r.length;
		}
		
		//if(Analysis.p.subpixel){return (totalsize/objects)/(Math.pow(Analysis.p.oversampling2ndstep*Analysis.p.interpolation, 2));}
		return(totalsize/objects);

	}
	
	
	
	public static double meansize(ArrayList<Region> regionslist){

		double totalsize=0;
		int objects=0;
		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();
			objects++;	       			
			totalsize+= r.points;
		}

		if(Analysis.p.subpixel){return (totalsize/objects)/(Math.pow(Analysis.p.oversampling2ndstep*Analysis.p.interpolation, 2));}
		else return(totalsize/objects);

	}

	public static double totalsize(ArrayList<Region> regionslist){

		double totalsize=0;

		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();

			totalsize+= r.points;
		}

		return(totalsize);

	}

	public static void coloccorr(){


	}



	public static void looptest_settings(){
		firstbcoloc=true;

		p.minves_size=1;
		p.maxves_size=500;
		p.min_intensity=0.15;
		p.colocthreshold=0.5;

		for (int i=1;i<9;i=i+1){

			p.maxves_size=500+200*(i-1);
			bcoloc(); 
			firstbcoloc=false;
		}
		firstbcoloc=true;
	}


	public static  ArrayList<Region> removeExternalObjects(ArrayList<Region> regionslist){
		ArrayList<Region> newregionlist = new ArrayList<Region>();


		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();
			if(isInside(r))newregionlist.add(r);
		}
		regionslist=newregionlist;
		//IJ.log("new size" + newregionlist.size());
		return newregionlist;
	}


	public static boolean isInside(Region r){

		int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
		int fz2;
		if(Analysis.p.nz>1)fz2=factor2; else fz2=1;
		double size=0;
		int inside=0;
		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {	
			Pix px = it.next();
			if(overallCellMaskBinary[px.pz/fz2][px.px/factor2][px.py/factor2])inside++;
			size++;
		}
		return ((inside/size)>0.1);
	}

	public static void computeOverallMask(){

		boolean mask [][][]= new boolean [p.nz][p.ni][p.nj];

		for (int z=0; z<p.nz; z++){
			for (int i=0; i<p.ni; i++){  
				for (int j=0;j< p.nj; j++){  
					if(p.usecellmaskX && p.usecellmaskY){
						mask[z][i][j]=cellMaskABinary[z][i][j]//>254  
								&&
								cellMaskBBinary[z][i][j];//>254;
					}
					else if (p.usecellmaskX){mask[z][i][j]=cellMaskABinary[z][i][j];}//>254;}
					else if (p.usecellmaskY){mask[z][i][j]=cellMaskBBinary[z][i][j];}//>254;}
					else {mask[z][i][j]=true;}

				}
			}
		}
		overallCellMaskBinary=mask;


	}


	public static void computeRegions(){
		IJ.log("deprecated");
		if(p.usePSF==true || p.nz>1 ||p.nlevels==1 ){
			if(p.findregionthresh)compute_connected_regions_a((int) 255*p.thresh,solverX.Ri[0]);
			else compute_connected_regions_a((int) 255*p.thresh,null);
		}
		else{
			if(p.nlevels==2)compute_connected_regions_a(0.5,null);
			else compute_connected_regions_a(1.5,null);
		}


		if(p.usePSF==true || p.nz>1 ||p.nlevels==1 ){
			if(p.findregionthresh)compute_connected_regions_b((int) 255*p.thresh,solverY.Ri[0]);
			else compute_connected_regions_b((int) 255*p.thresh,null);
		}
		else{
			if(p.nlevels==2)compute_connected_regions_b(0.5,null);
			else compute_connected_regions_b(1.5,null);
		}
	}



	public static void setRegionsLabels( ArrayList<Region> regionslist, short [] [] [] regions)
	{
		int factor2 =Analysis.p.oversampling2ndstep*Analysis.p.interpolation;
		int fz2;
		if(Analysis.p.nz>1)fz2=factor2; else fz2=1;
		int index=1;

		for (int z=0; z<p.nz*fz2; z++)
		{
			for (int i=0; i<p.ni*factor2; i++)
			{  
				for (int j=0;j< p.nj*factor2; j++)
				{  
					regions[z][i][j]=0;
				}
			}
		}


		for (Iterator<Region> it = regionslist.iterator(); it.hasNext();) {
			Region r = it.next();
			//r.value=index; keep old index in csv file  : do not  (because displaying happens before, with the previous values)
			setRegionLabel(r,regions, index);
			//IJ.log(" "+index);
			index++;
		}
		

	}

	public static void setRegionLabel(Region r, short [] [] [] regions, int label){

		for (Iterator<Pix> it = r.pixels.iterator(); it.hasNext();) {	
			Pix px = it.next();
			regions[px.pz][px.px][px.py]= (short) label;
		}

	}




	public static void calcColoc(PrintWriter out, PrintWriter out2, String[] list, int i){

		//		out2.print("Min intensity X " + p.min_intensity +";" +
		//				"Min intensity Y" + p.min_intensityY+";" +
		//				"Min vesicle size" + p.minves_size+";" +
		//				"Max vesicle size" + p.maxves_size+";" +
		//				"Overlap threshold" + p.colocthreshold
		//				);
		//		out2.println();
		//		out2.print("Cell"+ list[i]);
		//		out2.println();
		//		out2.print("Region in X"+ ";" + "Overlap with Y" + ";" + "Size");
		//		out2.println();
		double colocAB=Tools.round(colocsegAB(0),4);
		//		out2.print("Region in Y"+ ";" + "Overlap with X" + ";" + "Size");
		//		out2.println();

		double colocBA=Tools.round(colocsegBA(null,0),4);
		double colocA=Tools.round(colocsegA(null),4);
		double colocB=Tools.round(colocsegB(null),4);
		out.print(
				na + ";" +
						Tools.round(meana , 4)  +";"
						+ nb +";" + 
						Tools.round(meanb , 4) +";" +
						colocAB +";" + 
						colocBA + ";"+
						colocA+ ";"+
						colocB+ ";");
		//out.println();
		//out.flush();
		//out2.flush();

	}
}





//imgt.setPosition(1, 1, 1);
//imp1=imgt.getChannelProcessor();
//imga=new ImagePlus(imgt.getTitle(), imp1);
//imga.show();
//imgt.setPosition(2, 1, 1);
//imp2=imgt.getChannelProcessor();
//imgb=new ImagePlus("B", imp2);
//imgb.show();
//Analysis.segmenta();

