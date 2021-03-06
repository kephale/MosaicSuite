package mosaic.core.imageUtils.masks;

import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.iterators.SpaceIterator;

public abstract class EllipseBase implements Mask {
    private final float iMiddlePoint[];
    private final float iRadius[];
    
    protected float iScaling[];
    protected final int[] iDimensions;
    protected final boolean iMask[];
    protected final SpaceIterator iIterator;
    
    protected int iNumOfFgPoints = 0;
    
    EllipseBase(float aRadius, int aSizeOfRegion, float[] aScaling) {
        int numOfDims = aScaling.length;
        
        iMiddlePoint = new float[numOfDims];
        iRadius = new float[numOfDims];
        iDimensions = new int[numOfDims];
        for (int i = 0; i < numOfDims; ++i) {
            iRadius[i] = aRadius;
            iMiddlePoint[i] = aSizeOfRegion / 2.0f;
            iDimensions[i] = aSizeOfRegion;
        }
        iScaling = aScaling;

        iIterator = new SpaceIterator(iDimensions);
        
        iMask = new boolean[iIterator.getSize()];
    }
    
    @Override
    public boolean isInMask(int aIndex) {
        return iMask[aIndex];
    }

    @Override
    public int[] getDimensions() {
        return iDimensions;
    }

    @Override
    public int getNumOfFgPoints() {
        return iNumOfFgPoints;
    }
    
    /**
     * Calculates equation of a circle divided by squared radius. It takes care about scaling.
     * @param aPoint - input point in region containing circle
     * @return value of ((x - x0)^2 + (y - y0)^2 + ...) / r^2   
     *         if value == 1 we are on boundary of circle
     */
    protected float hyperEllipse(Point aPoint) {
        int[] iCoords = aPoint.iCoords;
        float result = 0.0f;
        for (int vD = 0; vD < iRadius.length; vD++) {
            // add 0.5 to each coordinate to place it in the middle of pixel
            float dist = (iCoords[vD] + 0.5f - iMiddlePoint[vD]) * iScaling[vD];
            result += dist * dist / (iRadius[vD] * iRadius[vD]);
        }
        return result;
    }
}
