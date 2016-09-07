package mosaic.particleTracker;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.process.ByteProcessor;
import mosaic.core.detection.Particle;
import net.sf.javaml.utils.ArrayUtils;


/**
 *  This class is responsible for displaying MSS/MSD plots.
 *  @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
class TrajectoryAnalysisPlot extends ImageWindow implements ActionListener {

    private static final long serialVersionUID = 1L;

    // UI stuff
    private final Button iMssButton;
    private final Button iMsdButton;
    private final Checkbox iLogScale;

    private final TrajectoryAnalysis iTrajectoryAnalysis;

    // Dimensions of plot
    private static final int WIN_WIDTH = 800;
    private static final int WIN_HEIGHT = 600;

    /**
     * @param aTrajectory Trajectory to be analyzed and plotted.
     * @param aPixelDim Dimensions of pixel in meters (needed for proper trajectory analysis)
     * @param aTimeInterval Time interval between frames used to calculate trajectory analysis.
     */
    TrajectoryAnalysisPlot(Trajectory aTrajectory, double aPixelDim, double aTimeInterval) {
        this(aTrajectory.existing_particles, aPixelDim, aTimeInterval);
    }

    /**
     * @param aParticles Particles to be analyzed and plotted.
     * @param aPixelDim Dimensions of pixel in meters (needed for proper trajectory analysis)
     * @param aTimeInterval Time interval between frames used to calculate trajectory analysis.
     */
    private TrajectoryAnalysisPlot(final Particle[] aParticles, double aPixelDim, double aTimeInterval) {
        // A little bit nasty but working method of setting window size (and further plot size).
        // Other methods like setSize(...) do not work even if applied to both - ImageWindow and Plot
        super(new ImagePlus("Trajectory Analysis", new ByteProcessor(WIN_WIDTH,WIN_HEIGHT, new byte[WIN_WIDTH*WIN_HEIGHT], null)));

        // Create UI for this window
        iMssButton = new Button(" MSS ");
        iMssButton.addActionListener(this);
        iMsdButton = new Button(" MSD ");
        iMsdButton.addActionListener(this);

        iLogScale = new Checkbox("logarithmic scale", true);

        final Panel panel = new Panel(new GridLayout(3,1));
        panel.add(iMssButton);
        panel.add(iMsdButton);
        panel.add(iLogScale);

        add(panel);


        // Do all needed calculations

        // Calculate MSD / MSS
        iTrajectoryAnalysis = new TrajectoryAnalysis(aParticles);
        iTrajectoryAnalysis.setLengthOfAPixel(aPixelDim);
        iTrajectoryAnalysis.setTimeInterval(aTimeInterval);
        if (iTrajectoryAnalysis.calculateAll() != TrajectoryAnalysis.SUCCESS) {
            this.close();
            IJ.error("It is impossible to calculate MSS/MSD for this trajectory!");
            return;
        }
        // Show it all to user (by default it presents MSS/linear plot).
        plotMss();
        pack();

    }

    /**
     * This method updates plot according to given data. It presents data set of points
     * altogether with straight line with given slope an y-axis intercept.
     *
     * @param aX data set with x points to be presented
     * @param aY data set with y points to be presented
     * @param aSlope slope of linear regression
     * @param aY0 y-axis intercept of linear regression
     * @param aXlabel
     * @param aYlabel
     * @param aWindowLabel title of window
     */
    private void updatePlot(double[] aX, double[] aY, double aSlope, double aY0, Double aDiffusionCoefficient,
            String aXlabel, String aYlabel, String aWindowLabel) {

        // Generate data for slop line
        final double[] slopeLine = new double[aX.length];
        for (int i = 0; i < aX.length; ++i) {
            slopeLine[i] = aSlope * aX[i] + aY0;
        }

        // Calculate X/Y min/max for plot
        final double minX = aX[0];
        final double maxX = aX[aX.length-1];
        double minY = ArrayUtils.min(aY);
        double maxY = ArrayUtils.max(aY);
        minY = Math.min(minY, ArrayUtils.min(slopeLine));
        maxY = Math.max(maxY, ArrayUtils.max(slopeLine));

        // Create plot with slope line
        PlotWindow.noGridLines = false; // draw grid lines
        final Plot plot = new Plot("", aXlabel, aYlabel, aX, slopeLine);
        plot.setSize(WIN_WIDTH, WIN_HEIGHT);
        setTitle(aWindowLabel);
        plot.setLimits(minX, maxX, minY, maxY);
        plot.setLineWidth(2);

        // Plot data
        plot.setColor(Color.red);
        plot.addPoints(aX,aY,PlotWindow.X);

        // add label
        plot.setColor(Color.DARK_GRAY);
        plot.changeFont(new Font("Helvetica", Font.BOLD, 14));
        plot.addLabel(0.05, 0.10, String.format("slope = %4.8f", aSlope));
        plot.addLabel(0.05, 0.15, String.format("y0 intercept = %5.8f", aY0));
        if (aDiffusionCoefficient != null) {
            plot.addLabel(0.05, 0.20, String.format("diffusion coefficient D2 = %5.8e", aDiffusionCoefficient));
        }

        // color for slope line
        plot.setColor(Color.blue);
        setImage(plot.getImagePlus());
    }

    /**
     * Plots MSS (moment scaling spectrum) with log/linear plot.
     */
    private void plotMss() {
        if (iLogScale.getState()) {
            updatePlot(iTrajectoryAnalysis.toLogScale(iTrajectoryAnalysis.getMomentOrders()),
                    iTrajectoryAnalysis.toLogScale(iTrajectoryAnalysis.getGammasLogarithmic()),
                    iTrajectoryAnalysis.getMSSlogarithmic(),
                    iTrajectoryAnalysis.getMSSlogarithmicY0(),
                    null,
                    "log(moment order \u03BD)", "log(scaling coefficient \u213D)", "MSS (log)");
        }
        else {
            updatePlot(iTrajectoryAnalysis.toDouble(iTrajectoryAnalysis.getMomentOrders()),
                    iTrajectoryAnalysis.getGammasLogarithmic(),
                    iTrajectoryAnalysis.getMSSlinear(),
                    iTrajectoryAnalysis.getMSSlinearY0(),
                    null,
                    "moment order \u03BD", "scaling coefficient \u213D", "MSS");

        }
    }

    /**
     * Plots MSD (mean square displacement -> mean moment order = 2) with log/linear plot
     */
    private void plotMsd() {
        final int order = 1; // special case for order=2 -> MSD (array starts with 0 for moment=1)
        int size = iTrajectoryAnalysis.getFrameShifts().length;
        double[] timeSteps = new double[size];
        for (int i = 0; i < size; ++i) {
            timeSteps[i] = iTrajectoryAnalysis.getFrameShifts()[i] * iTrajectoryAnalysis.getTimeInterval();
        }
        if (iLogScale.getState()) {
            updatePlot(iTrajectoryAnalysis.toLogScale(timeSteps),
                    iTrajectoryAnalysis.toLogScale(iTrajectoryAnalysis.getMSDforMomentIdx(order)),
                    iTrajectoryAnalysis.getGammasLogarithmic()[order],
                    iTrajectoryAnalysis.getGammasLogarithmicY0()[order],
                    iTrajectoryAnalysis.getDiffusionCoefficients()[order],
                    "log(\u03B4t)", "log(\u03BC(\u03B4t))", "MSD (log)");
        }
        else {
            updatePlot(timeSteps,
                    iTrajectoryAnalysis.getMSDforMomentIdx(order),
                    iTrajectoryAnalysis.getGammasLinear()[order],
                    iTrajectoryAnalysis.getGammasLinearY0()[order],
                    null,
                    "\u03B4t", "\u03BC(\u03B4t)", "MSD");

        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        final Object o = e.getSource();

        if (o == iMssButton) {
            plotMss();
        }
        else if (o == iMsdButton) {
            plotMsd();
        } 
    }
}