package mosaic.detection;

import ij.ImagePlus;
import ij.gui.ImageCanvas;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.util.Vector;

import javax.vecmath.Vector3f;

import mosaic.core.Particle;

/**
 * Defines an overlay Canvas for a given <code>ImagePlus</code> on which the detected particles from 
 * a <code>MyFrame</code> are displayed for preview
 */
public class PreviewCanvas extends ImageCanvas {

	private static final long serialVersionUID = 1L;
	private MyFrame preview_frame;
	int magnification = 1;
	private int preview_slice_calculated;
	private int radius;
	
	public Vector<Vector3f> shifts;
	public Vector<Vector3f> shiftPositions;
	public Vector<Particle> particlesShiftedToDisplay;

	/**
	 * Constructor.
	 * <br>Creates an instance of PreviewCanvas from a given <code>ImagePlus</code>
	 * <br>Displays the detected particles from the given <code>MyFrame</code>
	 * @param aimp - the given image plus on which the detected particles are displayed
	 * @param preview_f - the <code>MyFrame</code> with the detected particles to display
	 * @param mag - the magnification factor of the <code>ImagePlus</code> relative to the initial
	 */
	public PreviewCanvas(ImagePlus aimp, double mag) {
		super(aimp);
		this.preview_frame = null;
		this.magnification = (int)mag;
	}


	/**
	 * Overloaded Constructor.
	 * <br>Creates an instance of PreviewCanvas from a given <code>ImagePlus</code>
	 * <br>Displays the detected particles from the given <code>MyFrame</code>
	 * <br> sets the magnification factor to 1
	 * @param aimp
	 * @param preview_f
	 */
	private PreviewCanvas(ImagePlus aimp) {
		this(aimp, 1);
	}

	public void setPreviewFrame(MyFrame aPreviewFrame) {
		this.preview_frame = aPreviewFrame;
	}
	
	public void setPreviewParticleRadius(int radius) {
		this.radius = radius;
	}
	
	public void setPreviewSliceCalculated(int slice_calculated) {
		this.preview_slice_calculated = slice_calculated;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.Component#paint(java.awt.Graphics)
	 */
	public void paint(Graphics g) {            
		super.paint(g);
		int frameToDisplay = getFrameNumberFromSlice(this.imp.getCurrentSlice());
		Vector<Particle> particlesToDisplay = null;
		if(frameToDisplay == getFrameNumberFromSlice(preview_slice_calculated)) {
			// the preview display color is set to red
			g.setColor(Color.red);
			if(preview_frame != null){
				particlesToDisplay = preview_frame.getParticles();
				circleParticles(g, particlesToDisplay);
				if (shifts != null) {
					g.setColor(Color.PINK);
					paintShiftArrows(g, shifts, shiftPositions);
					circleParticles(g, particlesShiftedToDisplay);
				}
			}
		}
//		if(frames != null){
//			particlesToDisplay = frames[frameToDisplay-1].getParticles();
//			// the located particles display color is set to blue
//			g.setColor(Color.blue);
//			circleParticles(g, particlesToDisplay);
//		}

	}
	/**
	 * Inner class method
	 * <br> Invoked from the <code>paint</code> overwritten method
	 * <br> draws a dot and circles the detected particle directly of the given <code>Graphics</code>
	 * @param g
	 */
	private void circleParticles(Graphics g, Vector<Particle> particlesToDisplay) {
		if (particlesToDisplay == null || g == null) return;

		this.magnification = (int)Math.round(imp.getWindow().getCanvas().getMagnification());
		// go over all the detected particle 
		for (int i = 0; i< particlesToDisplay.size(); i++) {
			// draw a dot at the detected particle position (oval of height and width of 0)
			// the members x, y of the Particle object are opposite to the screen X and Y axis
			// The x-axis points top-down and the y-axis is oriented left-right in the image plane. 
			g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).y), 
					this.screenYD(particlesToDisplay.elementAt(i).x), 
					0, 0);
			// circle the  the detected particle position according to the set radius
			g.drawOval(this.screenXD(particlesToDisplay.elementAt(i).y-radius/1.0), 
					this.screenYD(particlesToDisplay.elementAt(i).x-radius/1.0), 
					2*radius*this.magnification-1, 2*radius*this.magnification-1); 
		}
	}

	/**
	 * @param sliceIndex: 1..#slices
	 * @return a frame index: 1..#frames
	 */
	private int getFrameNumberFromSlice(int sliceIndex) {
		return (sliceIndex-1) / imp.getNSlices() + 1;
	}
	
	/**
	 * Inner class method
	 * <br> Invoked from the <code>paint</code> overwritten method
	 * <br> draws an arrow from detected particle directly of the given <code>Graphics</code>
	 * @param g
	 */
	private void paintShiftArrows(Graphics g, Vector<Vector3f> shifts, Vector<Vector3f> shiftPositions ) {
		if (shifts == null || g == null) return;

		this.magnification = (int)Math.round(imp.getWindow().getCanvas().getMagnification());
		// go over all the detected particle 
		for (int i = 0; i< shifts.size(); i++) {
			// draw a dot at the detected particle position (oval of hieght and windth of 0)
			// the members x, y of the Particle object are opposite to the screen X and Y axis
			// The x-axis points top-down and the y-axis is oriented left-right in the image plane. 
			g.drawLine(this.screenXD(shiftPositions.get(i).y),this.screenYD(shiftPositions.get(i).x),this.screenXD(shiftPositions.get(i).y+shifts.get(i).y*20),this.screenYD(shiftPositions.get(i).x+shifts.get(i).x*20));
		}
	}

}