package mosaic.region_competition.wizard.score_function;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import ij.IJ;
import ij.ImagePlus;
import ij.io.Opener;
import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;
import mosaic.core.imageUtils.images.LabelImage;
import mosaic.core.imageUtils.iterators.RegionIterator;
import mosaic.plugins.Region_Competition;
import mosaic.plugins.Region_Competition.EnergyFunctionalType;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.Settings;
import mosaic.region_competition.energies.CurvatureBasedFlow;


public class ScoreFunctionRCsmo extends ScoreFunctionBase {

    private final String[] file;
    private double smooth[];
    private int Area[];

    private final IntensityImage i[];
    private final LabelImage l[];
    private final Settings s;

    @Override
    public Settings createSettings(Settings s, double pop[]) {
        final Settings st = new Settings(s);

        st.m_EnergyContourLengthCoeff = (float) pop[1];
        st.m_CurvatureMaskRadius = (int) pop[0];

        return st;
    }

    public ScoreFunctionRCsmo(IntensityImage i_[], LabelImage l_[], Settings s_) {
        i = i_;
        l = l_;

        s = s_;
        file = new String[l.length];
    }

    public LabelImage getLabel(int im) {
        return l[im];
    }

    public void setSmooth(double a[]) {
        smooth = a;
    }

    public void setArea(int[] sizeA) {
        Area = sizeA;
    }

    static double SmoothNorm(LabelImage l) {
        // Scan for particles

        final int off[] = l.getDimensions().clone();
        Arrays.fill(off, 0);

        int np = 0;
        double eCurv2_p = 0.0;
        double eCurv2_n = 0.0;

        double eCurv4_p = 0.0;
        double eCurv4_n = 0.0;

        double eCurv8_p = 0.0;
        double eCurv8_n = 0.0;

        double eCurv16_p = 0.0;
        double eCurv16_n = 0.0;

        double eCurv_tot_p = 0.0;
        double eCurv_tot_n = 0.0;

        final RegionIterator img = new RegionIterator(l.getDimensions(), l.getDimensions(), off);

        while (img.hasNext()) {
            final int i = img.next();
            final Point p = img.getPoint();
            if (l.getDataLabel()[i] < 0) {
                np++;
                final int id = Math.abs(l.getDataLabel()[i]);

                final CurvatureBasedFlow f2 = new CurvatureBasedFlow(2, l, null);
                final CurvatureBasedFlow f4 = new CurvatureBasedFlow(4, l, null);
                final CurvatureBasedFlow f8 = new CurvatureBasedFlow(8, l, null);
                final CurvatureBasedFlow f16 = new CurvatureBasedFlow(16, l, null);

                final double eCurv2 = f2.generateData(p, 0, id);
                final double eCurv4 = f4.generateData(p, 0, id);
                final double eCurv8 = f8.generateData(p, 0, id);
                final double eCurv16 = f16.generateData(p, 0, id);

                final double c2 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI*2) */;
                final double c4 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;
                final double c8 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;
                final double c16 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;

                if (eCurv2 > 0.0) {
                    eCurv2_p += c2 * eCurv2;
                }
                else {
                    eCurv2_n += c2 * eCurv2;
                }

                if (eCurv4 > 0.0) {
                    eCurv4_p += c4 * eCurv4;
                }
                else {
                    eCurv4_n += c4 * eCurv4;
                }

                if (eCurv8 > 0.0) {
                    eCurv8_p += c8 * eCurv8;
                }
                else {
                    eCurv8_n += c8 * eCurv8;
                }

                if (eCurv16 > 0.0) {
                    eCurv16_p += c16 * eCurv16;
                }
                else {
                    eCurv16_n += c16 * eCurv16;
                }

                // Get the module of the curvature flow
            }
        }

        // Sum 2 4 8 16

        eCurv_tot_p = eCurv2_p + eCurv4_p + eCurv8_p + eCurv16_p;
        eCurv_tot_n = eCurv2_n + eCurv4_n + eCurv8_n + eCurv16_n;

        // Smooth

        if (np == 0) {
            return 0.0;
        }
        return (eCurv_tot_p - eCurv_tot_n) / np / 8.0;
    }

    public double Smooth(LabelImage l) {
        // Scan for particles

        final int off[] = l.getDimensions().clone();
        Arrays.fill(off, 0);

        double eCurv2_p = 0.0;
        double eCurv2_n = 0.0;

        double eCurv4_p = 0.0;
        double eCurv4_n = 0.0;

        double eCurv8_p = 0.0;
        double eCurv8_n = 0.0;

        double eCurv16_p = 0.0;
        double eCurv16_n = 0.0;

        double eCurv_tot_p = 0.0;
        double eCurv_tot_n = 0.0;

        final RegionIterator img = new RegionIterator(l.getDimensions(), l.getDimensions(), off);

        while (img.hasNext()) {
            final int i = img.next();
            final Point p = img.getPoint();
            if (l.getDataLabel()[i] < 0) {
                final int id = Math.abs(l.getDataLabel()[i]);

                final CurvatureBasedFlow f2 = new CurvatureBasedFlow(2, l, null);
                final CurvatureBasedFlow f4 = new CurvatureBasedFlow(4, l, null);
                final CurvatureBasedFlow f8 = new CurvatureBasedFlow(8, l, null);
                final CurvatureBasedFlow f16 = new CurvatureBasedFlow(16, l, null);

                final double eCurv2 = f2.generateData(p, 0, id);
                final double eCurv4 = f4.generateData(p, 0, id);
                final double eCurv8 = f8.generateData(p, 0, id);
                final double eCurv16 = f16.generateData(p, 0, id);

                final double c2 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI*2) */;
                final double c4 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;
                final double c8 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;
                final double c16 = 1.0/* frm(l.getLabelMap().get(id).count - 4*Math.PI) */;

                if (eCurv2 > 0.0) {
                    eCurv2_p += c2 * eCurv2;
                }
                else {
                    eCurv2_n += c2 * eCurv2;
                }

                if (eCurv4 > 0.0) {
                    eCurv4_p += c4 * eCurv4;
                }
                else {
                    eCurv4_n += c4 * eCurv4;
                }

                if (eCurv8 > 0.0) {
                    eCurv8_p += c8 * eCurv8;
                }
                else {
                    eCurv8_n += c8 * eCurv8;
                }

                if (eCurv16 > 0.0) {
                    eCurv16_p += c16 * eCurv16;
                }
                else {
                    eCurv16_n += c16 * eCurv16;
                }

                // Get the module of the curvature flow
            }
        }

        // Sum 2 4 8 16

        eCurv_tot_p = eCurv2_p + eCurv4_p + eCurv8_p + eCurv16_p;
        eCurv_tot_n = eCurv2_n + eCurv4_n + eCurv8_n + eCurv16_n;

        // Smooth

        return eCurv_tot_p - eCurv_tot_n;
    }

    @Override
    public double valueOf(double[] x) {
        double result = 0.0;

        s.m_EnergyContourLengthCoeff = (float) x[1];
        s.m_CurvatureMaskRadius = (float) x[0];
        if (s.m_GaussPSEnergyRadius > 2.0) {
            s.m_EnergyFunctional = EnergyFunctionalType.e_PS;
        }
        else {
            s.m_EnergyFunctional = EnergyFunctionalType.e_PC;
        }

        // write the settings
        Region_Competition.getConfigHandler().SaveToFile(IJ.getDirectory("temp") + "RC_smo" + x[0] + "_" + x[1], s);

        for (int im = 0; im < i.length; im++) {
            IJ.run(i[im].getImageIP(), "Region Competition", "config=" + IJ.getDirectory("temp") + "RC_smo" + x[0] + "_" + x[1] + "  " + "output=" + IJ.getDirectory("temp") + "RC_smo" + x[0] + "_" + x[1]
                    + "_" + im + "_" + ".tif" + " normalize=false");

            // Read Label Image

            final Opener o = new Opener();
            file[im] = new String(IJ.getDirectory("temp") + "RC_smo" + x[0] + "_" + x[1] + "_" + im + "_" + ".tif");
            final ImagePlus ip = o.openImage(file[im]);

            l[im].initWithImg(ip);
            HashMap<Integer, LabelStatistics> labelMap = new HashMap<Integer, LabelStatistics>();
            createStatistics(l[im], i[im], labelMap);

            // Scoring
            int count = 0;
            final Collection<LabelStatistics> li = labelMap.values();

            for (int i = 1; i < li.toArray().length; i++) {
                count += ((LabelStatistics) li.toArray()[i]).count;
            }

            l[im].initBoundary();
            l[im].initContour();

            if (Area != null) {
                result += (Smooth(l[im]) - smooth[im]) * (Smooth(l[im]) - smooth[im]) + 0.2 * (count - Area[im]) * (count - Area[im]);
            }
            else {
                result += (Smooth(l[im]) - smooth[im]) * (Smooth(l[im]) - smooth[im]);
            }
        }

        return result;
    }

    @Override
    public boolean isFeasible(double[] x) {
        int minSz = Integer.MAX_VALUE;
        for (final LabelImage lbt : l) {
            for (final int d : lbt.getDimensions()) {
                if (d < minSz) {
                    minSz = d;
                }
            }
        }

        if (x[0] <= 2.0 || x[1] <= 0.0 || x[0] > minSz / 2 || x[1] > 1.0) {
            return false;
        }

        return true;
    }

    @Override
    public void show() {

        for (int im = 0; im < l.length; im++) {
            l[im].show("init", 255);
        }

    }

    @Override
    public double[] getAMean(Settings s) {
        final double[] aMean = new double[2];

        aMean[0] = s.m_CurvatureMaskRadius;
        aMean[1] = s.m_EnergyContourLengthCoeff;

        return aMean;
    }

    @Override
    public TypeImage getTypeImage() {
        return TypeImage.FILENAME;
    }

    @Override
    public ImagePlus[] getImagesIP() {
        return null;
    }

    @Override
    public String[] getImagesString() {
        return file;
    }

}