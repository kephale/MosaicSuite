package mosaic.regions.GUI;


import mosaic.regions.Settings;
import mosaic.regions.RegionsUtils.RegularizationType;


abstract class RegularizationGUI extends SettingsBaseGUI {

    protected RegularizationGUI(Settings aSettings) {
        super(aSettings);
    }

    private static RegularizationGUI factory(Settings aSettings, RegularizationType type) {
        RegularizationGUI result = null;

        switch (type) {
            case Sphere_Regularization: {
                result = new CurvatureFlowGUI(aSettings);
                break;
            }
            case Approximative:
            case None:
            default: {
                result = new DefaultRegularizationGUI();
                break;
            }
        }
        return result;
    }

    public static RegularizationGUI factory(Settings aSettings, String regularization) {
        final RegularizationType type = RegularizationType.getEnum(regularization);
        return factory(aSettings, type);
    }

}

class CurvatureFlowGUI extends RegularizationGUI {

    public CurvatureFlowGUI(Settings aSettings) {
        super(aSettings);
    }

    @Override
    public void createDialog() {
        gd.setTitle("Curvature Based Gradient Flow Options");
        gd.addNumericField("R_k", iSettings.energyCurvatureMaskRadius, 0);
    }

    @Override
    public void process() {
        iSettings.energyCurvatureMaskRadius = (int) gd.getNextNumber();
    }
}

class DefaultRegularizationGUI extends RegularizationGUI {

    protected DefaultRegularizationGUI() {
        super(null);
    }

    @Override
    public void createDialog() {
        gd = getNoGUI();
    }

    @Override
    public void process() {
    }

}
