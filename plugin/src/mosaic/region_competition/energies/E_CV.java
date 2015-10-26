package mosaic.region_competition.energies;


import java.util.HashMap;

import mosaic.core.image.Point;
import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.LabelStatistics;
import mosaic.region_competition.energies.Energy.ExternalEnergy;


public class E_CV extends ExternalEnergy {

    public E_CV() {
        super(null, null);
    }

    /**
     * Here we have the possibility to either put the current pixel
     * value to the BG, calculate the BG-mean and then calculate the
     * squared distance of the pixel to both means, BG and the mean
     * of the region (where the pixel currently still belongs to).
     * The second option is to remove the pixel from the region and
     * calculate the new mean of this region. Then compare the squared
     * distance to both means. This option needs a region to be larger
     * than 1 pixel/voxel.
     */
    @Override
    public EnergyResult CalculateEnergyDifference(Point contourPoint, ContourParticle contourParticle, int toLabel, HashMap<Integer, LabelStatistics> labelMap) {
        final int fromLabel = contourParticle.label;
        final float aValue = contourParticle.intensity;
        final LabelStatistics to = labelMap.get(toLabel);
        final LabelStatistics from = labelMap.get(fromLabel);
        final double vNewToMean = (to.mean * to.count + aValue) / (to.count + 1);
        final double energy = (aValue - vNewToMean) * (aValue - vNewToMean) - (aValue - from.mean) * (aValue - from.mean);
        return new EnergyResult(energy, false);
    }
}
