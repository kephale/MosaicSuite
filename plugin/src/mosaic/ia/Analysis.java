package mosaic.ia;


import java.awt.Color;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import javax.vecmath.Point3d;

import fr.inria.optimization.cmaes.CMAEvolutionStrategy;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Plot;
import mosaic.ia.gui.DistributionsPlot;
import mosaic.ia.gui.PlotHistogram;
import mosaic.ia.utils.DistanceCalculations;
import mosaic.ia.utils.DistanceCalculationsCoords;
import mosaic.ia.utils.DistanceCalculationsImage;
import mosaic.utils.math.StatisticsUtils;
import mosaic.utils.math.StatisticsUtils.MinMaxMean;
import weka.estimators.KernelEstimator;


public class Analysis {
    private final int dgrid_size = 1000;
    private boolean showAllRerunResults = false;
    private int potentialType;

    private double[] iDistances; // Nearest neighbour;
    private double[] q_D_grid, dgrid;
    private double[] nnObserved;
    private double[][] best;
    private int bestFitnessindex = 0;
    private double minDistance, maxDistance, meanDistance;


    public boolean calcDist(double gridSize, double kernelWeightq, double kernelWeightp, ImagePlus genMask, ImagePlus iImageX, ImagePlus iImageY) {
        DistanceCalculations dci;
        dci = new DistanceCalculationsImage(iImageX, iImageY, genMask, gridSize, kernelWeightq, dgrid_size);
        return calcDistributions(dci, kernelWeightp);
    }
    
    public boolean calcDist(double gridSize, double kernelWeightq, double kernelWeightp, ImagePlus genMask, Point3d[] particleXSetCoordUnfiltered, Point3d[] particleYSetCoordUnfiltered, double x1,double x2,double y1,double y2,double z1,double z2) {
        DistanceCalculations dci;
        dci = new DistanceCalculationsCoords(particleXSetCoordUnfiltered, particleYSetCoordUnfiltered, genMask, x1, y1, z1, x2, y2, z2, gridSize, kernelWeightq, dgrid_size);
        return calcDistributions(dci, kernelWeightp);
    }
    
    public boolean calcDistributions(DistanceCalculations dci, double kernelWeightp) {
        iDistances = dci.getDistancesOfX();
        dgrid = dci.getDistancesDistribution();
        q_D_grid = StatisticsUtils.normalizePdf(dci.getProbabilityDistribution(), false);
        
        KernelEstimator kernelEstimatorNN = createkernelDensityEstimator(iDistances, kernelWeightp);
        nnObserved = new double[dgrid.length];
        for (int i = 0; i < dgrid.length; i++) {
            nnObserved[i] = kernelEstimatorNN.getProbability(dgrid[i]);
        }
        StatisticsUtils.normalizePdf(nnObserved, false);
        
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(iDistances);
        minDistance = mmm.min;
        maxDistance = mmm.max;
        meanDistance = mmm.mean;
        System.out.println("Min/Max/Mean distance of X to Y: " + mmm.min + " / " + mmm.max + " / " + mmm.mean);
        
        new DistributionsPlot(dgrid, q_D_grid, nnObserved).show();
        PlotHistogram.plot("ObservedDistances", iDistances, getOptimBins(iDistances, 8, iDistances.length / 8));
        IJ.showMessage("Suggested Kernel wt(p): " + calcWekaWeights(iDistances));
    
        return true;
    }

    public static class Result {
        public final double iStrength;
        public final double iThresholdScale;
        public final double iResidual;
        
        public Result (double aStrength, double aThresholdScale, double aResidual) {
            iStrength = aStrength;
            iThresholdScale = aThresholdScale;
            iResidual = aResidual;
        }
        
        @Override
        public String toString() { 
            return "[strength="+ iStrength + ", threshold/scale="+iThresholdScale+", residual="+iResidual+"]";
        }
    }
    
    public boolean cmaOptimization(List<Result> aResultsOutput, int cmaReRunTimes) {
        final CMAMosaicObjectiveFunction fitfun = new CMAMosaicObjectiveFunction(dgrid, q_D_grid, iDistances, potentialType, nnObserved);
        if (potentialType == PotentialFunctions.NONPARAM) {
            PotentialFunctions.initializeNonParamWeights(minDistance, maxDistance);
            best = new double[cmaReRunTimes][PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
        }
        else {
            best = new double[cmaReRunTimes][2];
        }
        double[] allFitness = new double[cmaReRunTimes];
        double bestFitness = Double.MAX_VALUE;
        boolean diffFitness = false;
        for (int k = 0; k < cmaReRunTimes; k++) {
            final CMAEvolutionStrategy cma = new CMAEvolutionStrategy();
            cma.options.writeDisplayToFile = 0;
    
            cma.readProperties(); // read options, see file
            // CMAEvolutionStrategy.properties
            cma.options.stopFitness = 1e-12; // optional setting
            cma.options.stopTolFun = 1e-15;
            final Random rn = new Random(System.nanoTime());
            if (potentialType == PotentialFunctions.NONPARAM) {
                cma.setDimension(PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1);
                final double[] initialX = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                final double[] initialsigma = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                    initialX[i] = meanDistance * rn.nextDouble();
                    initialsigma[i] = initialX[i] / 3;
                }
                cma.setInitialX(initialX);
                cma.setInitialStandardDeviations(initialsigma);
            }
    
            else if (potentialType == PotentialFunctions.STEP) {
                cma.setDimension(2);
    
                final double[] initialX = new double[2];
                final double[] initialsigma = new double[2];
                initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5
    
                if (meanDistance != 0) {
                    initialX[1] = rn.nextDouble() * meanDistance;
                    initialsigma[1] = initialX[1] / 3;
                }
                else {
                    initialX[1] = 0;
                    initialsigma[1] = 1E-3;
                }
    
                initialsigma[0] = initialX[0] / 3;
    
                cma.setTypicalX(initialX);
                cma.setInitialStandardDeviations(initialsigma);
            }
            else {
                cma.setDimension(2);
                final double[] initialX = new double[2];
    
                final double[] initialsigma = new double[2];
                initialX[0] = rn.nextDouble() * 5; // epsilon. average strength of 5
    
                if (meanDistance != 0) {
                    initialX[1] = rn.nextDouble() * meanDistance;
                    initialsigma[1] = initialX[1] / 3;
                }
                else {
                    initialX[1] = 0;
                    initialsigma[1] = 1E-3;
                }
    
                initialsigma[0] = initialX[0] / 3;
    
                cma.setTypicalX(initialX);
                cma.setInitialStandardDeviations(initialsigma);
            }
    
            // initialize cma and get fitness array to fill in later
            final double[] fitness = cma.init();
    
            // iteration loop
            while (cma.stopConditions.getNumber() == 0) {
    
                // --- core iteration step ---
                final double[][] pop = cma.samplePopulation(); // get a new population
                // of solutions
                for (int i = 0; i < pop.length; ++i) { 
                    // for each candidate solution i
                    // a simple way to handle constraints that define a convex feasible domain
                    // (like box constraints, i.e. variable boundaries) via "blind re-sampling"
                    // assumes that the feasible domain is convex, the optimum is
                    while (!fitfun.isFeasible(pop[i])) {
                        // not located on (or very close to) the domain boundary,
                        pop[i] = cma.resampleSingle(i); // initialX is feasible
                    }
                    // and initialStandardDeviations are sufficiently small to
                    // prevent quasi-infinite looping here compute fitness/objective value
                    fitness[i] = fitfun.valueOf(pop[i]); // fitfun.valueOf() is to be minimized
                }
                cma.updateDistribution(fitness); // pass fitness array to update
                // search distribution
                // --- end core iteration step ---
    
                final int outmod = 150;
                if (cma.getCountIter() % (15 * outmod) == 1) {
                    cma.printlnAnnotation(); // might write file as well
                }
                if (cma.getCountIter() % outmod == 1) {
                    cma.println();
                }
            }
            // evaluate mean value as it is the best estimator for the optimum
            cma.setFitnessOfMeanX(fitfun.valueOf(cma.getMeanX())); // updates the best ever solution
    
            cma.println();
            cma.println("Terminated due to");
            for (final String s : cma.stopConditions.getMessages()) {
                cma.println("  " + s);
            }
            cma.println("best function value " + cma.getBestFunctionValue() + " at evaluation " + cma.getBestEvaluationNumber());
    
            allFitness[k] = cma.getBestFunctionValue();
            if (allFitness[k] < bestFitness) {
                if (k > 0 && bestFitness - allFitness[k] > allFitness[k] * .00001) {
                    diffFitness = true;
                }
                bestFitness = allFitness[k];
                bestFitnessindex = k;
    
            }
            best[k] = cma.getBestX();
            double strength = 0;
            double thresholdOrScale = 0;
            if (potentialType != PotentialFunctions.NONPARAM) {
                strength = best[k][0];
                thresholdOrScale = best[k][1];
            }
            double residual = allFitness[k];
            aResultsOutput.add(new Result(strength, thresholdOrScale, residual));
        }
    
        if (diffFitness) {
            IJ.showMessage("Warning: Optimization returned different results for reruns. The results may not be accurate. Displaying the parameters and the plots corr. to best fitness.");
        }
    
        if (!showAllRerunResults) { // show only best
            final double[] fitPotential = fitfun.getPotential(best[bestFitnessindex]);
            fitfun.l2Norm(best[bestFitnessindex]); // to calc pgrid for best params
            final Plot plot = new Plot("Estimated potential", "distance", "Potential value", fitfun.getD_grid(), fitPotential);
            MinMaxMean mmm = StatisticsUtils.getMinMaxMean(fitPotential);
            plot.setLimits(fitfun.getD_grid()[0] - 1, fitfun.getD_grid()[fitfun.getD_grid().length - 1], mmm.min, mmm.max);
            plot.setColor(Color.BLUE);
            plot.setLineWidth(2);
            final DecimalFormat format = new DecimalFormat("#.####E0");
    
            if (potentialType == PotentialFunctions.NONPARAM) {
                String estim = "";
                final double[] dp = new double[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1];
                double minW = Double.MAX_VALUE, maxW = Double.MIN_VALUE;
                for (int i = 0; i < PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1; i++) {
                    dp[i] = PotentialFunctions.dp[i];
                    estim = estim + "w[" + i + "]=" + best[bestFitnessindex][i] + " ";
                    if (best[bestFitnessindex][i] < minW) {
                        minW = best[bestFitnessindex][i];
                    }
                    if (best[bestFitnessindex][i] > maxW) {
                        maxW = best[bestFitnessindex][i];
                    }
                }
                System.out.println(estim);
                final Plot plotWeight = new Plot("Estimated Nonparam weights for best fitness:", "Support", "Weight", new double[1], new double[1]);
                plot.addLabel(.65, .3, "Residual: " + format.format(allFitness[bestFitnessindex]));
    
                plotWeight.setLimits(dp[0], dp[PotentialFunctions.NONPARAM_WEIGHT_SIZE - 1 - 1], minW, maxW);
                plotWeight.addPoints(dp, best[bestFitnessindex], Plot.CROSS);
                plotWeight.setColor(Color.RED);
    
                plotWeight.setLineWidth(2);
                plotWeight.show();
            }
            else if (potentialType == PotentialFunctions.STEP) {
                best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);// epsil
                best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
                System.out.println("Best parameters: Epsilon, Threshold:" + best[0] + " " + best[bestFitnessindex][1]);
    
                plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
                plot.addLabel(.65, .4, "Threshold: " + format.format(best[bestFitnessindex][1]));
                plot.addLabel(.65, .5, "Residual: " + format.format(allFitness[bestFitnessindex]));
            }
            else {
                best[bestFitnessindex][0] = Math.abs(best[bestFitnessindex][0]);
                best[bestFitnessindex][1] = Math.abs(best[bestFitnessindex][1]);
                System.out.println("Best parameters:  Epsilon, Sigma:" + best[bestFitnessindex][0] + " " + best[bestFitnessindex][1]);
                plot.addLabel(.65, .3, "Strength: " + format.format(best[bestFitnessindex][0]));
                plot.addLabel(.65, .4, "Scale: " + format.format(best[bestFitnessindex][1]));
                plot.addLabel(.65, .5, "Residual: " + format.format(allFitness[bestFitnessindex]));
            }
            System.out.println("N= " + iDistances.length);
            plot.show();
            
            double[] P_grid = fitfun.getPGrid();
            P_grid = StatisticsUtils.normalizePdf(P_grid, false);

            new DistributionsPlot(dgrid, P_grid, q_D_grid, nnObserved, potentialType, best, allFitness, bestFitnessindex).show();
        }
    
        return true;
    }
 
    public boolean hypothesisTesting(int monteCarloRunsForTest, double alpha) {
        if (best == null) {
            IJ.showMessage("Error: Run estimation first");
            return false;
        }
        else if (potentialType == PotentialFunctions.NONPARAM) {
            IJ.showMessage("Hypothesis test is not applicable for Non Parametric potential \n since it does not have 'strength' parameter");
            return false;
        }
    
        System.out.println("Running test with " + monteCarloRunsForTest + " and " + alpha);
        final HypothesisTesting ht = new HypothesisTesting(StatisticsUtils.calculateCdf(q_D_grid, true), dgrid, iDistances, best[bestFitnessindex], potentialType, monteCarloRunsForTest, alpha);
        ht.rankTest();
        return true;
    }

    public static KernelEstimator createkernelDensityEstimator(double[] distances, double weight) {
        final KernelEstimator kernel = new KernelEstimator(0.01);
        // weight is important, since bandwidth is calculated with it:
        // http://stackoverflow.com/questions/3511012/how-ist-the-bandwith-calculated-in-weka-kernelestimator-class
        System.out.println("Weight:" + weight);
        for (double value : distances) {
            kernel.addValue(value, weight); 
        }

        return kernel;
    }
    
    /**
     * Return the optimal bin number for a histogram of the data given in array, sing the
     * Freedman and Diaconis rule (bin_space = 2*IQR/n^(1/3)).
     * Inspired from Fiji TMUtils.java
     */
    public static int getOptimBins(double[] values, int minBinNumber, int maxBinNumber) {
        final int size = values.length;
        final double q1 = StatisticsUtils.getPercentile(values, 0.25);
        final double q3 = StatisticsUtils.getPercentile(values, 0.75);
        final double interQRange = q3 - q1;
        final double binWidth = 2 * interQRange * Math.pow(size, -0.33);
        MinMaxMean range = StatisticsUtils.getMinMaxMean(values);

        int noBins = (int) ((range.max - range.min) / binWidth + 1);
        if (noBins > maxBinNumber) {
            noBins = maxBinNumber;
        }
        else if (noBins < minBinNumber) {
            noBins = minBinNumber;
        }
        return noBins;
    }
    
    private static double calcSilvermanBandwidth(double[] distances) {
        final double q1 = StatisticsUtils.getPercentile(distances, 0.25);
        final double q3 = StatisticsUtils.getPercentile(distances, 0.75);
        final double silBandwidth = 0.9 * Math.min(StatisticsUtils.calcStandardDev(distances), (q3 - q1) / 1.34) * Math.pow(distances.length, -.2);
        System.out.println("Silverman's bandwidth: " + silBandwidth);

        return silBandwidth;
    }
    
    public static double calcWekaWeights(double[] distances) {
        MinMaxMean mmm = StatisticsUtils.getMinMaxMean(distances);
        final double range = mmm.max - mmm.min;
        final double bw = calcSilvermanBandwidth(distances);
        return ((1.0d / distances.length) * (range / bw) * (range / bw));
    }
    
    public double getMinD() {
        return minDistance;
    }

    public double getMaxD() {
        return maxDistance;
    }

    public double[] getDistances() {
        return iDistances;
    }
    
    public void setPotentialType(int potentialType) {
        this.potentialType = potentialType;
    }
}
