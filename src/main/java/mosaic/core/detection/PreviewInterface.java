package mosaic.core.detection;


import java.awt.event.ActionEvent;


public interface PreviewInterface {

    public void preview(ActionEvent e, int zDepth);

    public void saveDetected(ActionEvent e);
}
