package mosaic.particleTracker;


import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;

import mosaic.core.GUI.HelpGUI;


public class ParticleTrackerHelp extends HelpGUI {

    private final JFrame frame;
    private final JPanel panel;

    public ParticleTrackerHelp(int x, int y) {
        frame = new JFrame("Particle Tracker Help");
        frame.setSize(555, 780);
        frame.setLocation(x + 500, y - 50);

        panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        panel.setPreferredSize(new Dimension(575, 720));

        final JPanel pref = new JPanel(new GridBagLayout());

        setPanel(pref);
        setHelpTitle("Particle Tracker");
        createTutorial("http://mosaic.mpi-cbg.de/ParticleTracker/tutorial.html");
        createArticle("http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=1&zoom=100");
        createSection("Detection", null);
        String desc = new String("Approximate radius of the particles in the images in units of pixels." + "The value should be slightly larger than the visible particle radius,"
                + "but smaller than the smallest inter-particle separation. ");
        createField("Radius (w)", desc, "http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=2&zoom=150,0,-310");
        desc = new String("The score cut-off for the non-particle discrimination");
        createField("Cutoff (T_s)", desc, "http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=3&zoom=150,0,-270");
        desc = new String("The percentile (r) that determines which bright pixels are " + "accepted as Particles. All local maxima in the upper rth percentile of the "
                + "image intensity distribution are considered candidate Particles.");
        createField("Percentile", desc, "http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=2&zoom=150,0,-370");
        createSection("Linking", null);
        desc = new String("The maximum number of pixels a particle is allowed to move between two" + "succeeding frames");
        createField("Displacement (L)", desc, "http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=4&zoom=150,0,-350");
        desc = new String("How many future frame are considered for the linking stage");
        createField("Link Range (R)", desc, "http://mosaic.mpi-cbg.de/docs/Sbalzarini2005a.pdf#page=4&zoom=150,0,-350");

        panel.add(pref);
        frame.add(panel);

        frame.setVisible(true);
    }
}
