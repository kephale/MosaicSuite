package mosaic.region_competition.energies;

import mosaic.region_competition.ContourParticle;
import mosaic.region_competition.IntensityImage;
import mosaic.region_competition.LabelImage;
import mosaic.region_competition.Point;


public abstract class Energy
{
	public abstract Object atStart();
	
	/**
	 * @return EnergyResult, entries (energy or merge) are null if not calculated by this energy
	 */
	public abstract EnergyResult CalculateEnergyDifference(Point contourPoint, 
			ContourParticle contourParticle, int toLabel);

	public static class EnergyResult
	{
		public EnergyResult(Double energy, Boolean merge)
		{
			this.energyDifference = energy;
			this.merge = merge;
		}
		public Double energyDifference;
		public Boolean merge;
	}

	/**
	 * Responsible for regularization
	 * Independent of image I
	 */
	public static abstract class InternalEnergy extends Energy
	{
		protected LabelImage labelImage;
		public InternalEnergy(LabelImage labelImage)
		{
			this.labelImage = labelImage;
		}
	}

	/**
	 * Responsible for data fidelity
	 */
	public static abstract class ExternalEnergy extends Energy
	{
		protected IntensityImage intensityImage;
		protected LabelImage labelImage;
		public ExternalEnergy(LabelImage labelImage, IntensityImage intensityImage)
		{
			this.labelImage = labelImage;
			this.intensityImage = intensityImage;
		}
	}
}
