package mosaic.core.cluster;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import mosaic.core.cluster.LSFBatch;
import mosaic.core.cluster.JobStatus.jobS;
import mosaic.core.cluster.LSFBatch.LSFJob;
import mosaic.core.utils.ShellCommand;

import mosaic.core.GUI.ProgressBarWin;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ProgressBar;
import ij.io.Opener;


public class ClusterSession
{
	int nImages;
	ClusterProfile cp;
	
	ClusterSession(ClusterProfile cp_)
	{
		cp = cp_;
	}
	
	private String readFileAsString(String filePath) throws IOException {
        StringBuffer fileData = new StringBuffer();
        BufferedReader reader = new BufferedReader(
                new FileReader(filePath));
        char[] buf = new char[1024];
        int numRead=0;
        while((numRead=reader.read(buf)) != -1){
            String readData = String.valueOf(buf, 0, numRead);
            fileData.append(readData);
        }
        reader.close();
        return fileData.toString();
    }
	
	/**
	 * Cleanup all the data you created
	 */
	
	private void CleanUp()
	{
		
	}

	
	/**
	 * 
	 * Create a JobArray on Cluster
	 * 
	 * @param img Image to process
	 * @param ss Secure Shell session
	 * 
	 */
	
	private void createJobArrayFromImage(ImagePlus img, SecureShellSession ss, ProgressBarWin wp)
	{
		if (img == null)
		{nImages = 0; return;}
		
		int nImages = img.getNFrames();
		String tmp_dir = IJ.getDirectory("temp");
	
		wp.SetStatusMessage("Preparing data...");
	
		ImageStack stk = img.getStack();
	
		int stack_size = stk.getSize() / nImages;
	
		for (int i = 0 ; i < nImages ; i++)
		{
			ImageStack tmp_stk = new ImageStack(img.getWidth(),img.getHeight());
			for (int j = 0 ; j < stack_size ; j++)
			{
				tmp_stk.addSlice("st"+j,stk.getProcessor(i*stack_size+j+1));
			}
		
			ImagePlus ip = new ImagePlus("tmp",tmp_stk);
			IJ.saveAs(ip,"Tiff", tmp_dir + "tmp_" + (i+1));
		
			wp.SetProgress(100*i/nImages);
		}
	
		// Create an SSH connection with the cluster
		// Get the batch system of the cluster and set the class
		//  to process the batch system output
	
		BatchInterface bc = cp.getBatchSystem();
	
		ss.setShellProcessOutput(bc);

		// transfert the images

		File [] fl = new File[nImages];
	
		for (int i = 0 ; i < nImages ; i++)
		{
			fl[i] = new File(tmp_dir + "tmp_" + (i+1) + ".tif");
		}
	
		wp.SetProgress(0);
		wp.SetStatusMessage("Uploading...");
	
		if (ss.upload(cp.getPassword(),fl,wp) == false)
		{
			CleanUp();
		}
	
		// Download a working version of Fiji
		// and copy the plugins
	
		if (ss.checkDirectory(cp.getRunningDir()+"Fiji.app") == false)
		{
			wp.SetStatusMessage("Installing Fiji on cluster... ");
		
			String CommandL[] = {"cd " + cp.getRunningDir(),
				"wget mosaic.mpi-cbg.de/Downloads/fiji-linux64.tar.gz",
				"tar -xf fiji-linux64.tar.gz",
				"cd Fiji.app",
				"cd plugins",
				"mkdir Mosaic_ToolSuite",
				"cd Mosaic_ToolSuite",
				"wget mosaic.mpi-cbg.de/Downloads/Mosaic_ToolSuite.jar"};
	
	
			ss.runCommands(cp.getPassword(), CommandL);
		}
	
		wp.SetStatusMessage("Interfacing with batch system...");
	
		// create the macro script

		String macro = new String("job_id = getArgument();\n"
			   + "if(job_id == \"\" )\n"
			   + "   exit(\"No job id\");\n"
			   + "\n"
			   + "run(\"Squassh\",\"config=" + ss.getTransfertDir() + "spb_settings.dat" + " output=" + ss.getTransfertDir() + "tmp_" + "\"" + " + job_id + " + "\"_seg.tif" + " filepath=" + ss.getTransfertDir() + "tmp_" + "\"" + "+ job_id" + " + \".tif\" );\n");
			   
		// Create the batch script if required and upload it
	
		String run_s = cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id() + ".ijm";
		String scr = bc.getScript(run_s,cp.getRunningDir() + ss.getSession_id() + "/" + ss.getSession_id(),ss.getSession_id(),nImages);
		if (scr != null)
		{
			PrintWriter out;
			try 
			{
				// Running script
			
				out = new PrintWriter(tmp_dir + ss.getSession_id());
				out.println(scr);
				out.close();
			
				// ImageJ plugins macro
			
				out = new PrintWriter(tmp_dir + ss.getSession_id() + ".ijm");
				out.println(macro);
				out.close();
				
				File fll[] = new File[3];
				fll[0] = new File(tmp_dir + ss.getSession_id());
				fll[1] = new File(tmp_dir + "spb_settings.dat");
				fll[2] = new File(tmp_dir + ss.getSession_id() + ".ijm");
				ss.upload(cp.getPassword(),fll,null);
			} 
			catch (FileNotFoundException e) 
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	
		// run the command
	
		wp.SetStatusMessage("Running...");
	
		String [] commands = new String[2];
		commands[0] = new String("cd " + ss.getTransfertDir());
		commands[1] = bc.runCommand(ss.getTransfertDir());
	
		ss.runCommands(cp.getPassword(), commands);
	
		// Wait that the command get processed
		// Yes of course horrible but it work
	
		int n_attempt = 0;
		while (bc.getJobID() == 0 && n_attempt < 100000000) 
		{try {Thread.sleep(100);}
		catch (InterruptedException e) 
		{e.printStackTrace();} 
		n_attempt++;}
	
		// Check if we failed to launch the job
	
		if (bc.getJobID() == 0)
		{IJ.error("Failed to run the Job on the cluster");CleanUp();return;}
		
		// create JobID file
		
		try 
		{
			PrintWriter out;
		
			// Create jobID file
		
			out = new PrintWriter(tmp_dir + "JobID");
			out.println(new String(bc.getJobID() + " " + nImages));
			out.close();
		
			File fll[] = new File[1];
			fll[0] = new File(tmp_dir + "JobID");
			
			ss.upload(cp.getPassword(),fll,null);
		} 
		catch (FileNotFoundException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Get the Jobs directory in the temporal folder
	 * 
	 * @return Get all the directory string
	 * 
	 */
	
	String[] getJobDirectories()
	{
		final String tmp_dir = IJ.getDirectory("temp");
		
		// List all job directory
		
		File file = new File(tmp_dir);
		String[] directories = file.list(new FilenameFilter() 
		{
		  @Override
		  public boolean accept(File dir, String name) 
		  {
				Pattern jobID = Pattern.compile(tmp_dir + "Job[0-9]+");
				
				Matcher matcher = jobID.matcher(dir + File.separator + name);
			  
				File f = new File(dir, name);
				if (f.isDirectory() == true && matcher.find())
				{
					return true;
				}
				return false;
		  }
		});
		
		for (int i = 0 ; i < directories.length ; i++)
		{
			directories[i] = tmp_dir + directories[i];
		}
			
		return directories;
	}
	
	/**
	 * 
	 * Process the stack
	 * 
	 * @param output List of output patterns
	 */
	
	void stackVisualize(String output[])
	{
		String directories[] = getJobDirectories();
		
		// Visualize all job directory
		
		for (int j = 0 ; j < directories.length ; j++)
		{
			if (directories[j].endsWith(".tiff") || directories[j].endsWith(".tif") )
			{
				File [] fl = new File(directories[j]).listFiles();
				int nf = fl.length;
				Opener op = new Opener();
				
				ImagePlus ip = op.openImage(fl[0].getAbsolutePath());
				
				IJ.run("Image Sequence...", "open=" + directories[j] + " starting=1 increment=1 scale=100 file=[] or=[] sort");
				IJ.run("Stack to Hyperstack...", "order=xyczt(default) channels=" + ip.getNChannels() + " slices=" + ip.getNSlices() + " frames=" + nf + " display=Composite");
			}
		}
	}

	/**
	 * 
	 * Reorganize the downloaded data
	 * 
	 * @param output List of output patterns
	 */
	
	void reorganize(String output[])
	{		
		String directories[] = getJobDirectories();
		
		// reorganize
		
		try 
		{
			for (int i = 0 ; i < directories.length ; i++)
			{
				for (int j = 0 ; j < output.length ; j++)
				{
					String tmp = new String(output[j]);
		
					Process tProcess;
					tProcess = Runtime.getRuntime().exec("mkdir " + directories[i] + "/" + tmp.replace("*","_"));
					tProcess.waitFor();
				}

				for (int j = 0 ; j < output.length ; j++)
				{
					String tmp = new String(output[j]);
		
					int nf = new File(directories[i]).listFiles().length/output.length;
					
					Process tProcess;
					for (int k = 0 ; k < nf ; k++)
					{
						tProcess = Runtime.getRuntime().exec("mv " + directories[i] + "/" + tmp.replace("*","tmp_" + (k+1)) + "   " + directories[i] + "/" + tmp);
						tProcess.waitFor();
					}
				}
			}
		
		} 
		catch (IOException e) 
		{
		// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
		// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	/**
	 * 
	 * Get data
	 * 
	 * @param output Output format
	 * @param ss SecureShellSession
	 * @param wp Progress bar window
	 * @param bc Batch interface
	 */
	
	void getData(String output[], SecureShellSession ss, ProgressBarWin wp, BatchInterface bc)
	{
		String tmp_dir = IJ.getDirectory("temp");
		File [] fldw = new File[bc.getNJobs() * output.length];
	
		for (int i = 0 ; i < bc.getNJobs() ; i++)
		{
			for (int j = 0 ; j < output.length ; j++)
			{
				String tmp = new String(output[j]);
				fldw[i*output.length + j] = new File(bc.getDir() + File.separator + tmp.replace("*","tmp_" + (i+1)) );
			}
		}
	
		try {
			ShellCommand.exeCmdNoPrint("mkdir " + tmp_dir + File.separator + "Job" + bc.getJobID());
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		wp.SetStatusMessage("Downloading...");
		ss.download(cp.getPassword(), fldw, new File(tmp_dir + File.separator + "Job" + bc.getJobID()) , wp);
	}
	
	/**
	 * 
	 * Run a selected command on the cluster, ensure that the cluster has a Fiji installed if not
	 * it provide an automate installation
	 * 
	 * @param img Image the frames are parallelized
	 * @param options options String to pass to the plugins
	 * @param output output that the plugins generate with "*" as wild card example: "dir1/dir*_out/*.tif"
	 *        on a file "tmp_1" will be expanded in "dir1/dirtmp1_1_out/tmp_1.tif"
	 * @param ExtTime extimated running time (to select the queue on the cluster)
	 */
	
	public void runPluginsOnFrames(ImagePlus img, String options, String output[], double ExtTime)
	{
		String tmp_dir = IJ.getDirectory("temp");
		SecureShellSession ss = new SecureShellSession(cp);
		ProgressBarWin wp = new ProgressBarWin();
		wp.setVisible(true);
		
		// Create job array
		
		createJobArrayFromImage(img,ss,wp);
		BatchInterface bc = cp.getBatchSystem();
		
		//
		
		BatchInterface bcl[] = bc.getAllJobs(ss);
		ClusterStatusStack css[] = new ClusterStatusStack[bcl.length];
		ImageStack st[] = new ImageStack[bcl.length];
		ImagePlus ip[] = new ImagePlus[bcl.length];
		
		// get the status wait completition;

		for (int j = 0 ; j < bcl.length ; j++)
		{
			bcl[j].createJobStatus();
			css[j] = new ClusterStatusStack();
			st[j] = css[j].CreateStack(bcl[j].getJobStatus());
			ip[j] = new ImagePlus("Cluster status",st[j]);
			ip[j].show();
			bcl[j].setJobStatus(bcl[j].getJobStatus());
		}
		
		int n_bc = 0;
		while (n_bc < bcl.length)
		{
			n_bc = 0;
			for (int j = 0 ; j < bcl.length ; j++)
			{
				String commands[] = new String[1];
				commands[0] = bcl[j].statusJobCommand();
				bcl[j].reset();
				ss.setShellProcessOutput(bcl[j]);
				ss.runCommands(cp.getPassword(), commands);
			
				// Wait the command get Processed

				bcl[j].waitParsing();
			
				if (JobStatus.allComplete(bcl[j].getJobsStatus()) == true)
				{
					css[j].UpdateStack(st[j], bcl[j].getJobStatus());
					getData(output,ss,wp,bcl[j]);
					bcl[j].clean(ss);
					bcl[j] = null;
					break;
				}
				css[j].UpdateStack(st[j], bcl[j].getJobStatus());
			
			
				// wait 10 second to send get again the status
			
				try 
				{
					Thread.sleep(10000);
				} 
				catch (InterruptedException e) 
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		reorganize(output);
		stackVisualize(output);
	}
	
	public void runPluginsOnImages(ImagePlus img[], String options, double ExtTime)
	{
		// Save the image
		
		// transfert the images
		
		// Download a working version of Fiji
		// and copy the plugins
		
		// create the macro script
		
		// run the command
		
		// get the data
		
		// show the data
	}
}