package mosaic.core.binarize;


import mosaic.core.imageUtils.Point;
import mosaic.core.imageUtils.images.IntensityImage;


/**
 * Basically is a binarized image view (with view we mean that the image is
 * not computed) of an image, based on the definition of intervals of an
 * intensityImage image
 * 1 = in the interval
 * 0 = outside the interval
 *
 * @author Stephan Semmler, refactored Pietro Incardona
 */

public class BinarizedIntervalIntesityImage extends IntervalsListDouble implements BinarizedImage {

    private final IntensityImage image;

    public BinarizedIntervalIntesityImage(IntensityImage image) {
        this.image = image;
    }

    @Override
    public boolean EvaluateAtIndex(Point p) {
        if (!image.isInBound(p)) {
            return false;
        }
        final float value = image.get(p);
        return Evaluate(value);
    }

    @Override
    public boolean EvaluateAtIndex(Integer p) {
        if (!image.isInBound(p)) {
            return false;
        }
        final float value = image.get(p);
        return Evaluate(value);
    }

}
