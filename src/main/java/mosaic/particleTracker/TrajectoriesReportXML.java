package mosaic.particleTracker;

import java.io.File;
import java.util.Iterator;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import mosaic.core.detection.Particle;
import mosaic.plugins.ParticleTracker3DModular_;
import mosaic.plugins.ParticleTracker3DModular_.CalibrationData;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class creates XML report with information:
 * <li> configuration of plugin (kernel radius, link range...)
 * <li> source frame information (resolution, number of frames...)
 * <li> detected trajectories (all trajectory analysis and data)
 *
 */
public class TrajectoriesReportXML {
    private Document iReport;
    private final ParticleTracker3DModular_ iTracker;

    public TrajectoriesReportXML (String aFileName, ParticleTracker3DModular_ aTracker) {
        iTracker = aTracker;

        try {
            // Create new xml document
            iReport = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

            // Fill it with data
            generateReport();

            // Finalize and save results
            final Transformer transformer = TransformerFactory.newInstance().newTransformer();
            final DOMSource source = new DOMSource(iReport);
            final StreamResult result = new StreamResult(new File(aFileName));
            transformer.transform(source, result);

        } catch (final TransformerException e) {
            e.printStackTrace();
        } catch (final ParserConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void generateReport() {

        final Element rootElement = iReport.createElement("ParticleTracker");
        iReport.appendChild(rootElement);

        // NOTE: -------------------------------------------------------------------------
        // To add css support just uncomment following lines. Of course first css must be
        // created to nicely present xml content.
        // -------------------------------------------------------------------------------
        // Node css = iReport.createProcessingInstruction
        //         ("xml-stylesheet", "type=\"text/css\" href=\"report.css\"");
        // iReport.insertBefore(css, rootElement);

        generateConfiguration(rootElement);
        generateFramesInfo(rootElement);
        generateTrajectoriesInfo(rootElement);
    }

    private void generateConfiguration(Element aParent) {
        final Element conf = addElement(aParent, "Configuration");

        addElementWithAttr(conf, "KernelRadius", "value", iTracker.getRadius());
        addElementWithAttr(conf, "CutoffRadius", "value", iTracker.getCutoffRadius());
        addElementWithAttr(conf, "Threshold", "mode", iTracker.getThresholdMode(), "value", iTracker.getThresholdValue());
        addElementWithAttr(conf, "Displacement", "value", iTracker.displacement);
        addElementWithAttr(conf, "Linkrange", "value", iTracker.iLinkRange);
    }

    private void generateFramesInfo(Element aParent) {
        final Element conf = addElement(aParent, "FramesInfo");

        addElementWithAttr(conf, "Width", "value", iTracker.getWidth());
        addElementWithAttr(conf, "Height", "value", iTracker.getHeight());
        addElementWithAttr(conf, "NumberOfSlices", "value", iTracker.getNumberOfSlices());
        addElementWithAttr(conf, "NumberOfFrames", "value", iTracker.getNumberOfFrames());
        addElementWithAttr(conf, "GlobalMinimum", "value", iTracker.getGlobalMinimum());
        addElementWithAttr(conf, "GlobalMaximum", "value", iTracker.getGlobalMaximum());
    }

    private void generateTrajectoriesInfo(Element aParent) {
        final Element traj = addElement(aParent, "Trajectories");

        final Iterator<Trajectory> iter = iTracker.iTrajectories.iterator();
        while (iter.hasNext()) {
            addTrajectory(traj, iter.next());
        }
    }

    private void addTrajectory(Element aParent, Trajectory aTrajectory) {
        final Element traj = addElementWithAttr(aParent, "Trajectory", "ID", aTrajectory.iSerialNumber);

        generateTrajectoryAnalysis(traj, aTrajectory);
        generateTrajectoryData(traj, aTrajectory);
    }

    private void generateTrajectoryData(Element aParent, Trajectory aTrajectory) {
        final Element trajData = addElement(aParent, "TrajectoryData");

        for (final Particle p : aTrajectory.iParticles) {
            final Element frame = addElementWithAttr(trajData, "Frame", "number", p.getFrame());

            final Element coordinates = addElement(frame, "Coordinates");
            coordinates.setAttribute("x", "" + p.getx());
            coordinates.setAttribute("y", "" + p.gety());
            coordinates.setAttribute("z", "" + p.getz());

            final Element intensity = addElement(frame, "IntensityMoments");
            intensity.setAttribute("m0", "" + p.m0);
            intensity.setAttribute("m1", "" + p.m1);
            intensity.setAttribute("m2", "" + p.m2);
            intensity.setAttribute("m3", "" + p.m3);
            intensity.setAttribute("m4", "" + p.m4);

            addElementWithAttr(frame, "NonParticleDiscriminationScore", "value", p.nonParticleDiscriminationScore);
        }
    }

    private void generateTrajectoryAnalysis(Element aParent, Trajectory aTrajectory) {
        CalibrationData calData = iTracker.getImageCalibrationData();
        if (calData.errorMsg == null) {
            final Element trajAnalysis = addElement(aParent, "TrajectoryAnalysis");
            final TrajectoryAnalysis ta = new TrajectoryAnalysis(aTrajectory);
            ta.setLengthOfAPixel(calData.pixelDimension);
            ta.setTimeInterval(calData.timeInterval);
            if (ta.calculateAll() == TrajectoryAnalysis.SUCCESS) {
                addElementWithAttr(trajAnalysis, "MSS", "slope", "" + ta.getMSSlinear(), "yAxisIntercept", "" + ta.getMSSlinearY0());
                addElementWithAttr(trajAnalysis, "MSD", "slope", "" + ta.getGammasLogarithmic()[1], "yAxisIntercept", "" + ta.getGammasLogarithmicY0()[1]);
                addElementWithAttr(trajAnalysis, "DiffusionCoefficient", "D2", "" + ta.getDiffusionCoefficients()[1]);
            }
            else {
                addElementWithAttr(trajAnalysis, "MSS", "slope", "", "yAxisIntercept", "");
                addElementWithAttr(trajAnalysis, "MSD", "slope", "", "yAxisIntercept", "");
                addElementWithAttr(trajAnalysis, "DiffusionCoefficient", "D2", "");
            }
        }
    }

    private Element addElement(Element aParent, String aName) {
        final Element el = iReport.createElement(aName);
        aParent.appendChild(el);
        return el;
    }

    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, int aValue) {
        return addElementWithAttr(aParent, aName, aAttribute, "" + aValue);
    }

    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, double aValue) {
        return addElementWithAttr(aParent, aName, aAttribute, "" + aValue);
    }

    private Element addElementWithAttr(Element aParent, String aName, String aAttribute, String aValue) {
        final Element el = iReport.createElement(aName);
        el.setAttribute(aAttribute, aValue);
        aParent.appendChild(el);

        return el;
    }

    private Element addElementWithAttr(Element aParent, String aName, String aAttribute1, String aValue1, String aAttribute2, String aValue2) {
        final Element el = iReport.createElement(aName);
        el.setAttribute(aAttribute1, aValue1);
        el.setAttribute(aAttribute2, aValue2);
        aParent.appendChild(el);

        return el;
    }
}
