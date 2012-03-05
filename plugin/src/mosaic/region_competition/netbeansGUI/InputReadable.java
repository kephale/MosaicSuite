package mosaic.region_competition.netbeansGUI;

import mosaic.region_competition.EnergyFunctionalType;


/**
 * Input interface <br>
 * Implement this to read from user input
 */
public interface InputReadable
{
	
	/**
	 * Reads the input values into the correspondent data structures
	 * @return true on success, false on error
	 */
	public boolean processInput();
	
	// Input
	
	public LabelImageInitType getLabelImageInitType();
	public String getLabelImageFilename();
	public int getNumIterations();
	
	/**
	 * @return The filepath as String or empty String if no file was chosen.
	 */
	public String getInputImageFilename();
	
	
	// UI
	
	public boolean useStack();
	public boolean showStatistics();

	
	// Debugging
	
	public boolean useOldRegionIterator();
	public int getKBest();

	public boolean useRegularization();
	
	/**
	 * enum to determine type of initialization
	 */
	public enum LabelImageInitType
	{
		Rectangle, Ellipses, UserDefinedROI, File, Bubbles
	}
	
}

