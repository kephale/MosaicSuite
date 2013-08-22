package mosaic.bregman;


import ij.IJ;

import java.util.Date;


public class ASplitBregmanSolverTwoRegions extends ASplitBregmanSolver {

	int l=0; // use mask etc of level 0 

	public ASplitBregmanSolverTwoRegions(Parameters params, 
			double [] [] [] image, double [] [] [] [] speedData, double [] [] [] [] mask,
			MasksDisplay md, int channel, AnalysePatch ap){
		super(params,image,speedData,mask,md, channel, ap);
		

	}


	@Override
	protected void step() throws InterruptedException {
		long lStartTime = new Date().getTime(); //start time
		//energy=0;

		Tools.subtab(temp1[l], temp1[l], b2xk[l]);  
		Tools.subtab(temp2[l], temp2[l], b2yk[l]);


		//temp3=divwb
		Tools.mydivergence(temp3[l], temp1[l], temp2[l]);//, temp3[l]);


		//RHS = -divwb+w2k-b2k+w3k-b3k;
		//temp1=RHS
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					temp1[l][z][i][j]=-temp3[l][z][i][j]+w1k[l][z][i][j]-b1k[l][z][i][j]+w3k[l][z][i][j]-b3k[l][z][i][j];
				}	
			}
		}

		//temp1=uk

		dct2d.forward(temp1[l][0], true);
		for (int i=0; i<ni; i++) {  
			for (int j=0; j<nj; j++) {  
				if(eigenLaplacian[i][j] !=0) temp1[l][0][i][j]=temp1[l][0][i][j]/eigenLaplacian[i][j];
			}	
		}
		dct2d.inverse(temp1[l][0], true);


		//%-- w1k subproblem
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w1k[l][z][i][j]=-(p.ldata/p.lreg)*p.gamma*speedData[l][z][i][j] +b1k[l][z][i][j] + temp1[l][z][i][j];
				}
			}
		}


		//%-- w3k subproblem		
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					w3k[l][z][i][j]=Math.max(Math.min(temp1[l][z][i][j]+ b3k[l][z][i][j],1),0);
				}	
			}
		}



		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					b1k[l][z][i][j]=b1k[l][z][i][j] +temp1[l][z][i][j]-w1k[l][z][i][j];
					b3k[l][z][i][j]=b3k[l][z][i][j] +temp1[l][z][i][j]-w3k[l][z][i][j];	

				}	
			}
		}

		//%-- w2k sub-problem
		Tools.fgradx2D(temp3[l], temp1[l]);
		Tools.fgrady2D(temp4[l], temp1[l]);
		
		Tools.addtab(temp1[l], temp3[l], b2xk[l]);
		Tools.addtab(temp2[l], temp4[l], b2yk[l]);
		//temp1=w2xk temp2=w2yk
		Tools.shrink2D(temp1[l], temp2[l], temp1[l], temp2[l], p.gamma);
		//do shrink3D
		
		
		for (int z=0; z<nz; z++){
			for (int i=0; i<ni; i++) {  
				for (int j=0; j<nj; j++) {  
					b2xk[l][z][i][j]=b2xk[l][z][i][j] +temp3[l][z][i][j]-temp1[l][z][i][j];
					b2yk[l][z][i][j]=b2yk[l][z][i][j] +temp4[l][z][i][j]-temp2[l][z][i][j];
					//mask[l][z][i][j]=w3k[l][z][i][j];
				}	
			}
		}
		
		
//		normtab[l]=0;
//		for (int z=0; z<nz; z++){
//			for (int i=0; i<ni; i++) {  
//				for (int j=0; j<nj; j++) {  
//					//					l2normtab[l]+=Math.sqrt(Math.pow(w3k[l][z][i][j]-w3kp[l][z][i][j],2));
//					normtab[l]+=Math.abs(w3k[l][z][i][j]-w3kp[l][z][i][j]);
//				}	
//			}
//		}

		//Tools.copytab(w3kp[l], w3k[l]);

		energytab[l]=Tools.computeEnergy(speedData[l], w3k[l], temp3[l], temp4[l], p.ldata, p.lreg);


		//doneSignal2.await();

		energy+=energytab[l];

		if(p.livedisplay) md.display2regions(w3k[l][0], "Mask", channel);


		long lEndTime = new Date().getTime(); //end time

		long difference = lEndTime - lStartTime; //check different
		totaltime +=difference;
		//IJ.log("Elapsed milliseconds: " + difference);

	}


	

}