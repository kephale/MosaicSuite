package mosaic.plugins;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;

import Skeletonize3D_.Skeletonize3D_;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.Plot;
import ij.gui.PlotWindow;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import mosaic.bregman.segmentation.SegmentationParameters;
import mosaic.bregman.segmentation.SquasshSegmentation;
import mosaic.filamentSegmentation.GaussPsf;
import mosaic.plugins.utils.PlugInFloatBase;
import mosaic.utils.ConvertArray;
import mosaic.utils.ImgUtils;
import mosaic.utils.math.CubicSmoothingSpline;
import mosaic.utils.math.CubicSmoothingSpline.FittingStrategy;
import mosaic.utils.math.CubicSmoothingSpline.Spline;
import mosaic.utils.math.GraphUtils;
import mosaic.utils.math.GraphUtils.IntVertex;
import mosaic.utils.math.Matlab;
import mosaic.utils.math.Matrix;
import mosaic.utils.math.Matrix.MFunc;


/**
 * Implementation of filament segmentation plugin.
 * @author Krzysztof Gonciarz <gonciarz@mpi-cbg.de>
 */
public class FilamentSquassh extends PlugInFloatBase { // NO_UCD
    private static final Logger logger = Logger.getLogger(FilamentSquassh.class);
    
    // Segmentation parameters
    final private static double MaximumSplineError = 0.5;
    final private static double PSF = 2;
    final private static double LengthOfRefineLine = 3 * PSF;
    final private static int NumOfStepsBetweenFilamentDataPoints = 1; // >= 1
    
    final private static boolean Debug = true;
    
    // Synchronized map used to collect segmentation data from all plugin threads
    //
    // Map <
    //     frameNumber, 
    //     Map <
    //          channelNumber,
    //          listOfCubicSplines
    //         >
    //     >   
    // TreeMap is intentionally used to have always sorted data so output for example to
    // results table is nice and clean.
    private final Map<Integer, Map<Integer, List<CSS>>> iFilamentsData = new TreeMap<Integer, Map<Integer, List<CSS>>>();
    private synchronized void addNewFinding(Integer aFrameNumber, Integer aChannelNumber, List<CSS> aCubicSpline) {
        if (iFilamentsData.get(aFrameNumber) == null) {
            iFilamentsData.put(aFrameNumber, new TreeMap<Integer, List<CSS>>());
        }
        iFilamentsData.get(aFrameNumber).put(aChannelNumber, aCubicSpline);
    }

    /**
     * Segmentation procedure for plugin
     * @param aOutputImg - <not used>
     * @param aOrigImg - input image (to be segmented)
     * @param aChannelNumber - channel number used for drawing output image.
     */
    @Override
    protected void processImg(FloatProcessor aOutputImg, FloatProcessor aOrigImg, int aChannelNumber) {
        // Get dimensions of input image
        final int originalWidth = aOrigImg.getWidth();
        final int originalHeight = aOrigImg.getHeight();

        // Convert to array
        final double[][] img = new double[originalHeight][originalWidth];
        ImgUtils.ImgToYX2Darray(aOrigImg, img, 1.0f);

        // --------------- SEGMENTATION --------------------------------------------
        List<CSS> filaments = perfromSegmentation(new ImagePlus("", aOrigImg), /* segment */ true);

        // Save results and update output image
        addNewFinding(aOrigImg.getSliceNumber(), aChannelNumber, filaments);
    }
    
    @Override
    protected boolean showDialog() {
        // TODO: Create dialog.
        return true;
    }

    @Override
    protected boolean setup(String aArgs) {
        setResultDestination(ResultOutput.NONE);
        iProcessedImg = iInputImg.duplicate();
        iProcessedImg.setTitle("filaments_" + iInputImg.getTitle());
        
        return true;
    }
    
    @Override
    protected void postprocessBeforeShow() {
        // Show all segmentation results
        drawFilaments(iFilamentsData);
        showPlot(iFilamentsData);
    }

    private void drawFilaments(final Map<Integer, Map<Integer, List<CSS>>> iFilamentsData) {
        Overlay overlay = new Overlay();
        iProcessedImg.setOverlay(overlay);

        for (int frame : iFilamentsData.keySet()) {
            Map<Integer, List<CSS>> ms  = iFilamentsData.get(frame);
            // for every image take all filaments
            for (Entry<Integer, List<CSS>> e : ms.entrySet()) {

                // and draw them one by one
                for (final CSS css : e.getValue()) {
                    FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css);
                    if (!css.debgugCss || css.debgugCss && Debug) {
                        Roi r = new PolygonRoi(coordinates.x.getArrayYXasFloats()[0], coordinates.y.getArrayYXasFloats()[0], Roi.POLYLINE);
                        r.setPosition(frame);
                        r.setStrokeColor(css.getColor());
                        r.setStrokeWidth(0.25);
                        overlay.add(r);
                    }

                    if (Debug) {
                        for (int i = 0 ; i < css.xt.length; i++) {
                            final double radius = 0.125;
                            Roi r = new ij.gui.EllipseRoi(css.xt[i] - radius, css.yt[i] - radius, css.xt[i] + radius, css.yt[i] + radius, 1);
                            r.setPosition(frame);
                            r.setStrokeColor(Color.BLUE);
                            if (!css.debgugCss) {
                                r.setFillColor(Color.BLUE);
                            }
                            overlay.add(r);
                        }
                        drawPerpendicularLines(css, iProcessedImg, frame);
                    }
                }
            }
        }
    }
    
    protected void showPlot(final Map<Integer, Map<Integer, List<CSS>>> aFilamentsData) {
            PlotWindow.noGridLines = false; // draw grid lines
            final Plot plot = new Plot("Filaments Plot", "X", "Y");
            plot.setLimits(0, iInputImg.getWidth() - 1, iInputImg.getHeight() - 1, 0);
            
            // Plot data
            plot.setColor(Color.blue);
            for (final Map<Integer, List<CSS>> ms : aFilamentsData.values()) {
                for (final List<CSS> ps : ms.values()) {
                    int count = 0;
                    for (final CSS css : ps) {
                        if (css.getColor() == Color.RED) continue; 
                        // Mix colors - it is good for spotting changes for single filament
                        switch(count) {
                            case 0: plot.setColor(Color.BLUE);break;
                            case 1: plot.setColor(Color.RED);break;
                            case 2: plot.setColor(Color.GREEN);break;
                            case 3: plot.setColor(Color.BLACK);break;
                            case 4: plot.setColor(Color.CYAN);break;
                            default:plot.setColor(Color.MAGENTA);break;
                        }
                        count = (++count % 5); // Keeps all values in 0-5 range
                        
                        // Put stuff on plot
                        FilamentXyCoordinates coordinates = generateXyCoordinatesForFilament(css);
                        plot.addPoints(coordinates.x.getData(), coordinates.y.getData(), PlotWindow.LINE);
                    }
                }
            }

            plot.show();
    }
    
    // ================================= Implementation ============
    
    public List<CSS> perfromSegmentation(ImagePlus aInputImg, boolean aSegmentFirst) {
        // Input is to be segmented or ready mask?
        ImagePlus segmentedImg = aSegmentFirst ? runSquassh(aInputImg) : aInputImg.duplicate();

        // Remove holes in regions and put every region into seperate matrix
        ImagePlus binarizedImg = ImgUtils.binarizeImage(segmentedImg);
        ImagePlus regionsImg = ImgUtils.removeHoles(binarizedImg);
        final Matrix imgMatrix = ImgUtils.imageToMatrix(regionsImg);
        Matrix[] regionsMatrices = createSeperateMatrixForEachRegion(imgMatrix);
    
        List<CSS> css = new ArrayList<CSS>();
        int height = aInputImg.getHeight();
        aInputImg.getProcessor().setInterpolate(true);
        regionsImg.getProcessor().setInterpolate(true);
        ImageProcessor.setUseBicubic(true);
        
        // Process each connected component
        for (int regionIdx = 0; regionIdx < regionsMatrices.length; regionIdx++) {
            logger.debug("Processing region wiht index: " + regionIdx);
            Matrix currentRegion = Matlab.logical(regionsMatrices[regionIdx], 0);
            
            processOneRegion(currentRegion, height, aInputImg, regionsImg, css);
        }
    
        return css;
    }
    
    void processOneRegion(Matrix currentRegion, int height, ImagePlus aInputImg, ImagePlus regionsImg, List<CSS> css) {
        Matrix skeleton = skeletonize(currentRegion);
        PathResult longestPath = runLongestShortestPaths(skeleton);

        // Generate cubic smoothing splines for longest path
        List<Integer> allPoints = generatePoints(longestPath);
        if (allPoints.size() == 0) return;
        
        // Generate coordinates for creating parametric splines x(t) and y(t)
        final double[] xt = new double[allPoints.size()];
        final double[] yt = new double[allPoints.size()];
        final double[] t = new double[allPoints.size()];
        generateParametricCoordinates(height, allPoints, xt, yt, t);

        // Calculate first "rough" spline which fits longest shortest path in skeleton
        double aMaxSinglePointError = 3 * MaximumSplineError;
        CSS cssResult = new CSS(
            new CubicSmoothingSpline(t, xt, FittingStrategy.MaxSinglePointValue, aMaxSinglePointError),
            new CubicSmoothingSpline(t, yt, FittingStrategy.MaxSinglePointValue, aMaxSinglePointError),
            xt,
            yt,
            t
        );
        cssResult.debgugCss = true;
        css.add(cssResult);

        // Refine points on spline along refine lines
        CSS refined = refine(cssResult, aInputImg, regionsImg);
        refined.setColor(Color.GREEN);
        css.add(refined);

        // Take care of "tips" of filament. Make spline longer in each direction until brightness is above of average value with given PSF
        if (refined.cssX.getNumberOfKNots() > 1 && refined.cssY.getNumberOfKNots() > 2) {
            double boundaryValue = calcAverageIntensityOfSpline(refined, aInputImg) * valueOfGaussAtStepPoint(PSF);
            refined.setMinT(findT(refined, refined.cssX.getKnot(0), -0.01, boundaryValue, aInputImg, regionsImg));
            refined.setMaxT(findT(refined, refined.cssX.getKnot(refined.cssX.getNumberOfKNots() - 1), 0.01, boundaryValue, aInputImg, regionsImg));
        }
    }
    
    double findT(CSS aSpline, double aInitValue, double aStep, double aBoundaryValue, ImagePlus aInputImg, ImagePlus aRegionsImg) {
        double tmin = aInitValue;
        double minVal = tmin;
        while(true) {
            double xv = aSpline.cssX.getValue(tmin);
            double yv = aSpline.cssY.getValue(tmin);
            // Take care about proper cubic interpolation (-0.5 to get value from middle of pixel)
            double pixelValue = aInputImg.getProcessor().getInterpolatedValue(xv - 0.5, yv - 0.5);
            // Check boundary value and region boundary (as 255/2 for binary image).
            if (pixelValue >= aBoundaryValue && aRegionsImg.getProcessor().getInterpolatedValue(xv - 0.5, yv - 0.5) >= 255.0/2) minVal = tmin;
            else break;
            tmin += aStep;
        }
        return minVal;
    }
    
    private double calcAverageIntensityOfSpline(CSS aSpline, ImagePlus aImage) {
        double sum = 0;
        for (int ti = 0; ti < aSpline.cssX.getNumberOfKNots(); ti++) {
            double tv = aSpline.cssX.getKnot(ti);
            double xv = aSpline.cssX.getValue(tv);
            double yv = aSpline.cssY.getValue(tv);
            double pixelValue = aImage.getProcessor().getInterpolatedValue(xv - 0.5, yv - 0.5);
            sum += pixelValue;
        }
        
        return sum / aSpline.cssX.getNumberOfKNots();
    }

    private void generateParametricCoordinates(int height, List<Integer> allPoints, final double[] xt, final double[] yt, final double[] t) {
        for (int i = 0; i < allPoints.size(); ++i) {
            Integer point = allPoints.get(i);
            // Index of a point is a absolute index of image - find coordinates
            final int x = point / height;
            final int y = point % height;
            
            // Adjust to point initially in a middle of pixel (+ 0.5)
            xt[i] = x + 0.5;
            yt[i] = y + 0.5;
            
            // Generate parametric value in range 0-100. It is much easier to fit spline into such range than for example 0-1. 
            // In range 0-100 reaction on smoothing parameter is more linear in its range (0-1).
            t[i] = (100 * ((double) i) / (allPoints.size() - 1));
        }
    }

    private List<Integer> generatePoints(PathResult longestPath) {
        List<Integer> pts = new ArrayList<Integer>();
        GraphPath<IntVertex, DefaultEdge> path = longestPath.path;
        if (path != null) {
            UndirectedGraph<IntVertex, DefaultEdge> graph = longestPath.graph;
            List<DefaultEdge> edgeList = path.getEdgeList();
            for (int idx = 0; idx < edgeList.size(); idx += NumOfStepsBetweenFilamentDataPoints) {
                pts.add(graph.getEdgeSource(edgeList.get(idx)).getLabel());
            }
            // Make sure that very last point is also there
            pts.add(path.getEndVertex().getLabel());
        }
        return pts;
    }

    static public class PathResult {
        PathResult(UndirectedGraph<IntVertex, DefaultEdge> aGraph, GraphPath<IntVertex, DefaultEdge> aPath) { graph = aGraph; path = aPath; }

        UndirectedGraph<IntVertex, DefaultEdge> graph;
        GraphPath<IntVertex, DefaultEdge> path;
    }

    private PathResult runLongestShortestPaths(Matrix aImageMatrix) {
        Graph<IntVertex, DefaultEdge> g = GraphUtils.matrixToGraph(aImageMatrix, true);
        UndirectedGraph<IntVertex, DefaultEdge> gMst = GraphUtils.minimumSpanningTree(g);
        WeightedGraph<IntVertex, DefaultWeightedEdge> gs = GraphUtils.simplifySimipleUndirectedGraph(gMst);
        GraphPath<IntVertex, DefaultWeightedEdge> path1 = GraphUtils.findLongestShortestPath(gs);
        GraphPath<IntVertex, DefaultEdge> path = null;
        if (path1 != null) {
            DijkstraShortestPath<IntVertex, DefaultEdge> dijkstraShortestPath = new DijkstraShortestPath<IntVertex, DefaultEdge>(gMst, path1.getStartVertex(), path1.getEndVertex());
            path = dijkstraShortestPath.getPath();
        }

        return new PathResult(gMst, path);
    }

    
    class RefineCoords {
        RefineCoords(double aX1, double aY1, double aX2, double aY2) { x1 = aX1; y1 = aY1; x2 = aX2; y2 = aY2; }
        double x1, y1, x2, y2;
    }
    
    RefineCoords[] generateCoordinatesForRefineLines(CSS c) {
        RefineCoords[] coords = new RefineCoords[c.t.length];
        for (int num = 0; num < c.t.length; num++) {
            double t = c.t[num];
            double x = c.xt[num];
            double y = c.yt[num];
            Spline sx = c.cssX.getSplineForValue(t);
            Spline sy = c.cssY.getSplineForValue(t);
            double dxv = sx.equation.getDerivative(1).getValue(t - sx.shift);
            double dyv = sy.equation.getDerivative(1).getValue(t - sy.shift);

            double alpha;
            if (dxv == 0) alpha = Math.PI/2; // vertical
            else if (dyv == 0) alpha = 0; // horizontal
            else alpha = Math.atan(dyv/dxv);
            
            coords[num] = new RefineCoords(
                    x - LengthOfRefineLine * Math.cos(alpha - Math.PI/2),
                    y - LengthOfRefineLine * Math.sin(alpha - Math.PI/2),
                    x + LengthOfRefineLine * Math.cos(alpha - Math.PI/2),
                    y + LengthOfRefineLine * Math.sin(alpha - Math.PI/2)
            );
        }
        return coords;
    }
    
    private CSS refine(CSS c, ImagePlus xyz, ImagePlus binary) {
        double[] newXt = c.xt.clone();
        double[] newYt = c.yt.clone();
        double[] newT = c.t; // This is not changed so it is not copied;
        
        RefineCoords[] rcs = generateCoordinatesForRefineLines(c);
        int inter=(int)LengthOfRefineLine * 20 + 1;
        
        for (int num = 0; num < c.t.length; num += 1) {
            double x = c.xt[num];
            double y = c.yt[num];
            RefineCoords rc = rcs[num];
            double dx1 = (rc.x1 - x)/(inter - 1);
            double dy1 = (rc.y1 - y)/(inter - 1);
            double dx2 = (rc.x2 - x)/(inter - 1);
            double dy2 = (rc.y2 - y)/(inter - 1);

            double sumx = 0, sumy = 0, w = 0;
            
            double px1 = x, py1 = y;
            double px2 = x + dx2, py2 = y + dy2; // skip (x, y) - it is already calculated for px1/py1 pair
            double shift = 0.5; // shift for bicubic interpolation making it symmetric in respect to middle of pixel.
            for (int i = 0; i < inter; ++i) {
                    double pixelValue = xyz.getProcessor().getInterpolatedValue((float)px1 - shift, (float)py1 - shift);
                    if (binary.getProcessor().getPixelValue((int)(px1), (int)(py1)) == 0) break;
                    w += pixelValue;
                    sumx += px1 * pixelValue;
                    sumy += py1 * pixelValue;
                    px1 += dx1;
                    py1 += dy1;
            }
            for (int i = 0; i < inter; ++i) {
                    double pixelValue = xyz.getProcessor().getInterpolatedValue((float)px2 - shift, (float)py2 - shift);
                    if (binary.getProcessor().getPixelValue((int)(px2), (int)(py2)) == 0) break;
                    w += pixelValue;
                    sumx += px2 * pixelValue;
                    sumy += py2 * pixelValue;
                    px2 += dx2;
                    py2 += dy2;
            }
            
            if (w == 0) continue;
            newXt[num] = (sumx/w);
            newYt[num] = (sumy/w);
        }
        
        return new CSS(
                new CubicSmoothingSpline(newT, newXt, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError),
                new CubicSmoothingSpline(newT, newYt, FittingStrategy.MaxSinglePointValue, MaximumSplineError * 2, MaximumSplineError),
                newXt, 
                newYt, 
                newT);
    }

    private void drawPerpendicularLines(CSS css, ImagePlus xyz, int aFrame) {
        Overlay overlay = xyz.getOverlay();
        if (css.getColor() != Color.RED) return;
        RefineCoords[] rcs = generateCoordinatesForRefineLines(css);
        for (int num = 0; num < css.t.length; num += 1) {
            RefineCoords rc = rcs[num];
            Roi r = new Line(rc.x1, rc.y1, rc.x2, rc.y2);
            r.setPosition(aFrame);
            r.setStrokeColor(Color.WHITE);
            overlay.add(r);
        }
        xyz.draw();
    }

    static class CSS {
        final CubicSmoothingSpline cssX;
        final CubicSmoothingSpline cssY;
        final double[] xt;
        final double[] yt;
        final double[] t;
        private double tMin;
        private double tMax;
        private Color color = Color.RED;
        boolean debgugCss = false;
        
        CSS(CubicSmoothingSpline aCssX, CubicSmoothingSpline aCssY, double[] aXt, double[] aYt, double[] aT) {
            xt = aXt;
            yt = aYt;
            t = aT;
            cssX = aCssX;
            cssY = aCssY;
            tMin = t[0];
            tMax = t[t.length - 1];
        }
        
        void setColor(Color aColor) { color = aColor; }
        Color getColor() { return color; }
        void setMinT(double aMin) { tMin = aMin; }
        double getMinT() { return tMin; }
        void setMaxT(double aMax) { tMax = aMax; }
        double getMaxT() { return tMax; }
    }

    static public class FilamentXyCoordinates {
        public Matrix x;
        public Matrix y;

        protected FilamentXyCoordinates(Matrix aXvalues, Matrix aYvalues) {
            x = aXvalues;
            y = aYvalues;
        }
    }

    static public FilamentXyCoordinates generateXyCoordinatesForFilament(final CSS css) {
        // Generate x,y coordinates for current filament
        final Matrix t = Matlab.linspace(css.getMinT(), css.getMaxT(), css.cssX.getNumberOfKNots() * 2);
        Matrix x = t.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.cssX.getValue(t.get(aRow, aCol));
            }
        });
        Matrix y = t.copy().process(new MFunc() {
            @Override
            public double f(double aElement, int aRow, int aCol) {
                return css.cssY.getValue(t.get(aRow, aCol));
            }
        });

        return new FilamentXyCoordinates(x, y);
    }

    private Matrix skeletonize(Matrix aImg) {
        // Create ByteProcessor image
        byte[] maskBytes = new byte[aImg.size()];
        for (int x = 0; x < aImg.getData().length; x++) {
            maskBytes[x] = aImg.getData()[x] != 0 ? (byte) 255 : (byte) 0;
        }
        final ByteProcessor bp = new ByteProcessor(aImg.numCols(), aImg.numRows(), maskBytes);

        // And skeletonize
        final ImagePlus skeleton = new ImagePlus("Skeletonized", bp);
        Skeletonize3D_ skel = new Skeletonize3D_();
        skel.setup("", skeleton);
        skel.run(skeleton.getProcessor());
        
        // Return as a matrix
        return  ImgUtils.imageToMatrix(skeleton);
    }

    private Matrix[] createSeperateMatrixForEachRegion(final Matrix aImageMatrix) {
        Matrix logical = Matlab.logical(aImageMatrix, 0);
        Map<Integer, List<Integer>> cc = Matlab.bwconncomp(logical, true);
        logger.debug("Number of found connected components " + cc.size());

        // Create separate matrix for each connected component
        Matrix[] matrices = new Matrix[cc.size()];
        int idx = 0;
        for (List<Integer> p : cc.values()) {
            Matrix m = aImageMatrix.newSameSize();
            for (int i : p) {
                m.set(i, aImageMatrix.get(i));
            }
            matrices[idx++] = m;
        }
        return matrices;
    }

    private ImagePlus runSquassh(ImagePlus aImage) {
        double[][][] img3 = ImgUtils.ImgToZXYarray(aImage);
        SegmentationParameters sp = new SegmentationParameters(2, 1, 0.006125, 0.006125, true, SegmentationParameters.IntensityMode.AUTOMATIC, SegmentationParameters.NoiseModel.GAUSS, PSF, 1, 0, 5);
        ImageStatistics statistics = aImage.getStatistics();
        SquasshSegmentation ss = new SquasshSegmentation(img3, sp, statistics.histMin, statistics.histMax);
        ss.run();
        ImagePlus ip1 = ImgUtils.ZXYarrayToImg(ConvertArray.toDouble(ss.iLabeledRegions), "Output");
        new ImageConverter(ip1).convertToGray8();
        return ip1;
    }

    private double valueOfGaussAtStepPoint(double aSigma) {
        // Make reasonable large number of points in respect to sigma to keep quality
        final int NumOfPsfPoints = (int)aSigma * 20;
        
        // Make sure that we have odd number of points (this is required by later code).
        int len = (NumOfPsfPoints / 2) * 2 + 1;
        
        //  Generate gauss with provided sigma
        double[] gauss = GaussPsf.generateKernel(len, 1, aSigma)[0];
        
        // Generate step function
        double[] step = new double[len  * 2];
        for (int i = len; i < len * 2; i++) step[i] = 1;

        // Convolve exactly at step point (for n = len)
        double value = 0;
        int n = len;
        for (int m = -len/2; m <= len/2; m++) {
            value += step[n - m] * gauss[m + len/2 /* moves -n/2..n/2 to 0..n */];
        }
        
        return value;
    }
}
