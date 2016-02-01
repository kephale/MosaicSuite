package mosaic.bregman.segmentation;


import java.util.ArrayList;


public class Region implements Comparable<Region> {

    Region(int value, int points) {
        this.value = value;
        this.points = points;
    }

    public ArrayList<Pix> pixels = new ArrayList<Pix>();
    public double intensity;
    public double length;
    public float over_int;
    public float over_size;
    public float overlap;
    public final int points;
    public float rsize;
    public double coloc_o_int;
    public boolean colocpositive = false;
    public double perimeter;
    public boolean singlec;
    
    float cx, cy, cz;
    int value;
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

        for (final Pix p : pixels) {
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
    
    @Override
    public int compareTo(Region otherRegion) {
        return (value < otherRegion.value) ? 1 : ((value > otherRegion.value) ? -1 : 0);
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
        return rsize;
    }

    public double getperimeter() {
        return perimeter;
    }

    public double getlength() {
        return length;
    }

    public double getoverlap_with_ch() {
        return overlap;
    }

    public double getcoloc_object_size() {
        return over_size;
    }

    public double getcoloc_object_intensity() {
        return over_int;
    }

    public boolean getsingle_coloc() {
        return singlec;
    }

    public double getcoloc_image_intensity() {
        return coloc_o_int;
    }
}