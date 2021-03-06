package mosaic.bregman.segmentation;


import java.util.List;


public class Region implements Comparable<Region> {

    public Region(int aLabel, List<Pix> aPixels) {
        iLabel = aLabel;
        iPixels = aPixels;
    }

    // Object definition
    public int iLabel;
    public final List<Pix> iPixels;
    
    // Object properties
    public double intensity; // estimated intensity
    public double length; // length of skeleton
    public double perimeter; // perimeter for 2D, surface for 3D
    public float realSize; // real size in pixels (interpolation and/or oversampling taken into account)
    private float cx, cy, cz; // region center
    
    Region rvoronoi;

    /**
     * @return 2-element array of type Pix with {minPixCoord, maxPixCoord}
     */
    Pix[] getMinMaxCoordinates() {
        int xmin = Integer.MAX_VALUE;
        int ymin = Integer.MAX_VALUE;
        int zmin = Integer.MAX_VALUE;
        int xmax = Integer.MIN_VALUE;
        int ymax = Integer.MIN_VALUE;
        int zmax = Integer.MIN_VALUE;

        for (final Pix p : iPixels) {
            if (p.px < xmin) xmin = p.px;
            if (p.px > xmax) xmax = p.px;
            if (p.py < ymin) ymin = p.py;
            if (p.py > ymax) ymax = p.py;
            if (p.pz < zmin) zmin = p.pz;
            if (p.pz > zmax) zmax = p.pz;
        }
        
        Pix aMin = new Pix(zmin, xmin, ymin);
        Pix aMax = new Pix(zmax, xmax, ymax);
        
        return new Pix[] {aMin, aMax};
    }
    
    void calculateRegionCenter(int aScaleXY, int aScaleZ) {
        double sumx = 0;
        double sumy = 0;
        double sumz = 0;
        for (Pix p : iPixels) {
            sumx += p.px;
            sumy += p.py;
            sumz += p.pz;
        }
        int count = iPixels.size();

        cx = (float) (sumx / count);
        cy = (float) (sumy / count);
        cz = (float) (sumz / count);

        cx = cx / (aScaleXY);
        cy = cy / (aScaleXY);
        cz = cz / (aScaleZ);
    }
    
    @Override
    public int compareTo(Region otherRegion) {
        return (iLabel < otherRegion.iLabel) ? 1 : ((iLabel > otherRegion.iLabel) ? -1 : 0);
    }

    public double getcx() {
        return cx;
    }

    public double getcy() {
        return cy;
    }

    public double getcz() {
        return cz;
    }

    public double getintensity() {
        return intensity;
    }

    public double getrsize() {
        return realSize;
    }

    public double getperimeter() {
        return perimeter;
    }

    public double getlength() {
        return length;
    }
    
    @Override
    public String toString() { 
        return "{" + iLabel + ", " + iPixels.size() + "}";
    }
}
