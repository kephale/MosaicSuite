package mosaic.core.particleLinking;

import ij.IJ;

import java.io.IOException;
import java.util.Vector;

import mosaic.core.detection.MyFrame;
import mosaic.core.detection.Particle;
import mosaic.core.utils.MosaicUtils;

public class ParticleLinker {
	/**
	 * Second phase of the algorithm - 
	 * <br>Identifies points corresponding to the 
	 * same physical particle in subsequent frames and links the positions into trajectories
	 * <br>The length of the particles next array will be reset here according to the current linkrange
	 * <br>Adapted from Ingo Oppermann implementation
	 */
	public void linkParticles(MyFrame[] frames, int frames_number, int linkrange, float displacement) {

		int m, i, j, k, nop, nop_next, n;
		int ok, prev, prev_s, x = 0, y = 0, curr_linkrange;
		boolean[][] g;
		int[] xv,yv;
		boolean[] okv;
		double min, z;
		float max_cost;
		float[][] cost;
		Vector<Particle> p1, p2;

		// set the length of the particles next array according to the linkrange
		// it is done now since link range can be modified after first run
		for (int fr = 0; fr<frames.length; fr++) {
			for (int pr = 0; pr<frames[fr].getParticles().size(); pr++) {
				frames[fr].getParticles().elementAt(pr).next = new int[linkrange];
			}
		}
		curr_linkrange = linkrange;

		/* If the linkrange is too big, set it the right value */
		if(frames_number < (curr_linkrange + 1))
			curr_linkrange = frames_number - 1;

//		max_cost = displacement * displacement;

		for(m = 0; m < frames_number - curr_linkrange; m++) {
			IJ.showStatus("Linking Frame " + (m+1));
			nop = frames[m].getParticles().size();
			for(i = 0; i < nop; i++) {
				frames[m].getParticles().elementAt(i).special = false;
				for(n = 0; n < linkrange; n++)
					frames[m].getParticles().elementAt(i).next[n] = -1;
			}

			for(n = 0; n < curr_linkrange; n++) {
				max_cost = (float)(n + 1) * displacement * (float)(n + 1) * displacement;

				nop_next = frames[m + (n + 1)].getParticles().size();

				/* Set up the cost matrix */
				cost = new float[nop+1][nop_next+1];
				
				/* Set up the relation matrix */
				g = new boolean[nop+1][nop_next+1];
				xv = new int[nop_next+1];
				yv = new int[nop+1];
				
				okv = new boolean[nop_next+1];
				for (i = 0; i< okv.length; i++) okv[i] = true;
				
				/* Set g to zero - not necessary */
				//for (i = 0; i< g.length; i++) g[i] = false;

				p1 = frames[m].getParticles();
				p2 = frames[m + (n + 1)].getParticles();
				//    			p1 = frames[m].particles;
				//    			p2 = frames[m + (n + 1)].particles;


				/* Fill in the costs */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						cost[i][j] = 
							(p1.elementAt(i).x - p2.elementAt(j).x)*(p1.elementAt(i).x - p2.elementAt(j).x) + 
							(p1.elementAt(i).y - p2.elementAt(j).y)*(p1.elementAt(i).y - p2.elementAt(j).y) + 
							(p1.elementAt(i).z - p2.elementAt(j).z)*(p1.elementAt(i).z - p2.elementAt(j).z) + 
							(p1.elementAt(i).m0 - p2.elementAt(j).m0)*(p1.elementAt(i).m0 - p2.elementAt(j).m0) + 
							(p1.elementAt(i).m2 - p2.elementAt(j).m2)*(p1.elementAt(i).m2 - p2.elementAt(j).m2);
					}
				}

				for(i = 0; i < nop + 1; i++)
					cost[i][nop_next] = max_cost;
				for(j = 0; j < nop_next + 1; j++)
					cost[nop][j] = max_cost;
				cost[nop][nop_next] = 0.0f;

				/* Initialize the relation matrix */
				/* Initialize the relation matrix */
				for(i = 0; i < nop; i++) { // Loop over the x-axis
					IJ.showStatus("Linking Frame " + (m+1) + ": Initializing Relation matrix");
					IJ.showProgress(i,nop);
					min = max_cost;
					prev = -1;
					for(j = 0; j < nop_next; j++) { // Loop over the y-axis without the dummy

						/* Let's see if we can use this coordinate */						
						if(okv[j] && min > cost[i][j]) {
							min = cost[i][j];
							if(prev >= 0) {
								okv[prev] = true;
								g[i][prev] = false;
							}

							okv[j] = false;
							g[i][j] = true;
							
							prev = j;
						}
					}

					/* Check if we have a dummy particle */
					if(min == max_cost) {
						if(prev >= 0) {
							okv[prev] = true;
							g[i][prev] = false;
						}
						g[i][nop_next] = true;
						okv[nop_next] = false;
					}
				}
				
				/* Look for columns that are zero */
				for(j = 0; j < nop_next; j++) {
					ok = 1;
					for(i = 0; i < nop + 1; i++) {
						if(g[i][j])
							ok = 0;
					}
					if(ok == 1)
						g[nop][j] = true;
				}

				/* Build xv and yv, a speedup for g */
				for(i = 0; i < nop + 1; i++) {
				//	for(j = 0; j < nop_next+1; j++) {
					for(j = 0; j < nop_next + 1; j++) {
						if(g[i][j]) {
							xv[j] = i;
							yv[i] = j;
						}
					}
					/*if(g[i][nop_next]) {
						xv[nop_next] = i;
						yv[i] = nop_next;
					}*/
				}
				xv[nop_next] = nop;
				yv[nop] = nop_next;
				
			/* The relation matrix is initilized */
			
				
				/* Now the relation matrix needs to be optimized */
				IJ.showStatus("Linking Frame " + (m+1) + ": Optimizing Relation matrix");
				min = -1.0;
				while(min < 0.0) {
					min = 0.0;
					int prev_i = 0, prev_j = 0, prev_x = 0, prev_y = 0;
					for(i = 0; i < nop + 1; i++) {
						for(j = 0; j < nop_next + 1; j++) {
							if(i == nop && j == nop_next)
								continue;

							if(g[i][j] == false && 
									cost[i][j] <= max_cost) {
								/* Calculate the reduced cost */

								// Look along the x-axis, including
								// the dummy particles
								x = xv[j];

								// Look along the y-axis, including
								// the dummy particles
								y = yv[i];
								
								
								/* z is the reduced cost */
								z = cost[i][j] + 
								cost[x][y] - 
								cost[i][y] - 
								cost[x][j];
								
								if(z > -1.0e-10)
									z = 0.0;
								if(z < min) {
									min = z;

									prev_i = i;
									prev_j = j;
									prev_x = x;
									prev_y = y;
								}
							}
						}
					}

					if(min < 0.0) {
						g[prev_i][prev_j] = true;
						xv[prev_j] = prev_i;
						yv[prev_i] = prev_j;
						g[prev_x][prev_y] = true;
						xv[prev_y] = prev_x;
						yv[prev_x] = prev_y;
						g[prev_i][prev_y] = false;
						g[prev_x][prev_j] = false;
						
						// ensure the dummies still map to each other
						xv[nop_next] = nop;
						yv[nop] = nop_next;
					}
				}

				/* After optimization, the particles needs to be linked */
				for(i = 0; i < nop; i++) {
					for(j = 0; j < nop_next; j++) {
						if(g[i][j] == true)
							p1.elementAt(i).next[n] = j;
					}
				}
			}

			if(m == (frames_number - curr_linkrange - 1) && curr_linkrange > 1)
				curr_linkrange--;
		}

		/* At the last frame all trajectories end */
		for(i = 0; i < frames[frames_number - 1].getParticles().size(); i++) {
			frames[frames_number - 1].getParticles().elementAt(i).special = false;
			for(n = 0; n < linkrange; n++)
				frames[frames_number - 1].getParticles().elementAt(i).next[n] = -1;
		}
	}	
}