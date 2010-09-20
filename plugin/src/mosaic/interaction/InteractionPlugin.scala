package mosaic.interaction

import scalala.tensor.Matrix
import mosaic.core.CellOutline
import ij.plugin.Macro_Runner
import mosaic.core.Particle
import mosaic.core.optimization._
import mosaic.core.ImagePreparation
import mosaic.calibration.KernelDensityEstimator
import mosaic.calibration.NearestNeighbour

import ij.IJ
import ij.plugin.PlugIn
import ij.gui.GenericDialog
import scalala.Scalala._
import scalala.tensor.dense._
import scalala.tensor.Vector
import scalanlp.optimize.StochasticGradientDescent
import cma.fitness.AbstractObjectiveFunction

class InteractionPlugin extends PlugIn with ImagePreparation {
	// Image/domain dimension, normally 2 or 3
	val dim = 3 
	// 
	val nn = new NearestNeighbour(dim)

	@Override
	def run(arg: String) {
		println("Run Interaction Plugin ")
		
		
		gd = new GenericDialog("Input selection...", IJ.getInstance());
		gd.addChoice("Input soutce:", Array("Image","Matlab"), "Image")
		gd.showDialog();
		val (domainSize,isInDomain,refGroup, testGroup) = gd.getNextChoiceIndex match {
				case 0 => generateModelInputFromImages
				case 1 => readMatlabData
			}
//		no images below here
		
		initNearestNeighbour(refGroup)
		val (qOfD, d) = calculateQofD(domainSize, isInDomain)
		val dd = findD(testGroup, isInDomain)
		val shape = selectPotential()

//		nll optimization CMA
		val nbrPara = 1
		val fitfun = new LikelihoodOptimizer(new DenseVector(qOfD), new DenseVector(d),new DenseVector(dd), shape);
		potentialParamEst(fitfun,nbrPara)
		
//		hypothesis testing
//		Monte Carlo sample Tk with size K from the null distribution of T obtained by sampling N distances di from q(d)
//		additional Monte Carlo sample Uk to rank U
	}
	
	/** 
	 * Shows a dialog to the user, where he can choose one of the available potentials.
	 * The potential has to be defined in object PotentialFunctions
	 * @return by user selected potential
	 */
	def selectPotential(): ((Vector,Double,Double) => Vector) = {
		gd = new GenericDialog("Potential selection...", IJ.getInstance());
		gd.addChoice("Potential shape", PotentialFunctions.functions, PotentialFunctions.functions(0))
		gd.showDialog();
		PotentialFunctions.potentialShape(gd.getNextChoiceIndex)
	}
		
	/**
	 * qOfD with NN and Kernel estimation
	 * @param domainSize size of domain, in which state density q should be sampled
	 * @return state density as tuple (q,d) with values of state density q and distances d, at which q is specified
	 */
	def calculateQofD(domainSize: Array[Int], isInDomain: (Array[Double] => Boolean)):(Array[Double],Array[Double])= {
	  
	  val nbrQueryPoints = 50
	  val scale = new DenseVector(domainSize map(_.toDouble))
	   
	  // independent randomly placed query objects
//	  val queryPoints = List.fill(nbrQueryPoints)((rand(dim) :*scale).toArray)
	  
	  // regularly placed query objects
	  val queryPoints = nn.getSampling(List((domainSize(0), nbrQueryPoints),(domainSize(1), nbrQueryPoints),(domainSize(2), Math.floor(nbrQueryPoints/10).toInt))) //TODO less queries in z direction.
	  // only take samples in the cell/domain
	  println("Number of query points: " + queryPoints.size)
	  val validQueryPoints = queryPoints.filter(isInDomain(_))
	  println("Number of valid query points: " + validQueryPoints.size)
	   
	  val dist = getDistances(validQueryPoints.toArray)
      
      // estimate q(d)
	  val est = new KernelDensityEstimator()
	  est.addValues(dist)
	  
	  val maxDist = dist.reduceLeft(Math.max(_,_))
	  val minDist = Math.min(0,dist.reduceLeft(Math.min(_,_))) //TODO correct? with 0?
	  
	  val x = linspace(minDist, maxDist, 100)
	  val xArray = x.toArray
	  val prob = est.getProbabilities(xArray)
	  // TODO check prob. with integration equals 1.
	  plot(x, (new DenseVector(prob)))
	  title("q(D)"); xlabel("d"); ylabel("q(d)")

	  (prob, xArray)
	}
	
	//	D with NN
	def findD(queryPoints: Array[Array[Double]], isInDomain: (Array[Double] => Boolean)):Array[Double]= {
	  getDistances(queryPoints.filter(isInDomain(_)))
	}
	
	/**
	 * Calculate distances of queryPoints (X) to nearest neighbor of reference group (Y)
	 * @param queryPoints for which we measure the distance to the nearest neighbor in the reference group
	 * @return distance of each query point to it's nearest neighbor
	 */
	private def getDistances(queryPoints: Array[Array[Double]]):Array[Double]= {
			// find NN
				val time = (new java.util.Date()).getTime()
//			val dist = nn.getDistances(queryPoints) 
//				println("Search nearest neighbour in KDtree "+((new java.util.Date()).getTime() - time)*0.001)
			val dist = nn.bruteForceNN(queryPoints) 
			    println("brute force nearest neighbour search "+((new java.util.Date()).getTime() - time)*0.001)
			dist
	}
	
	/**
	 * Initializes KDTree with reference group (Y), to allow fast nearest neighbor search
	 */
	private def initNearestNeighbour(refPoints : Array[Array[Double]]) {
			val time = (new java.util.Date()).getTime()
		// generate KDTree
		nn.setReferenceGroup(refPoints)
			println("Generation KDtree "+((new java.util.Date()).getTime() - time)*0.001)
	}
	
	/**
	 * Estimates parameter of potential
	 * @param fitfun function to optimize, which has potential parameter as parameter 
	 */
	def potentialParamEst(fitfun: LikelihoodOptimizer, nbrParameter:Int){
		
		// CMA Optimization
		val sol = CMAOptimization.optimize(fitfun, nbrParameter)
		val solOutput = "CMA Optimization: " + PotentialFunctions.parametersToString(sol._2) + " min. value: " + sol._1
		println(solOutput)
		
		// Stochastic Steepest Descent
		val alpha = 0.0001
		val maxIter = 1000
		val batchSize = 10
		val initGuess = rand(nbrParameter)
		val stochasticSteepestDescent = new StochasticGradientDescent[Int, Vector](alpha, maxIter, batchSize)
		val minGuess = stochasticSteepestDescent.minimize(fitfun,initGuess)
		val solSteepestDescent = (fitfun.valueAt(minGuess), minGuess)
		val solSteepestDescentOutput = "Stochastic Steepest Descent:  " + PotentialFunctions.parametersToString(solSteepestDescent._2.toArray) + " min. value: " + solSteepestDescent._1
		println(solSteepestDescentOutput)
		
		plotPotential(fitfun.potentialShape, sol._2)
		plotPotential(fitfun.potentialShape, solSteepestDescent._2.toArray)
		
		IJ.showMessage("Interaction Plugin: parameter estimation", solOutput + '\n' + solSteepestDescentOutput);
		
	}
	
	/** Plots potential with specified shape and parameters
	 * @param shape :		potential shape
	 * @param parameters :	potential parameters
	 */
	def plotPotential(shape: (Vector,Double,Double) => Vector, parameters: Array[Double]) ={
		
		val para = PotentialFunctions.defaultParameters(parameters)
		val x = linspace(-5,100)
		val y = shape(x,para(1),para(2)) * para(0)
		
		val fig = figure()
		subplot(fig.rows+1,fig.cols,fig.rows * fig.cols +1)
		
		plot(x,y)
		title("phi(d) with " + PotentialFunctions.parametersToString(parameters) ); xlabel("d");	ylabel("phi(d)")	
	}
	
	/**
	 * Shows an ImageJ message with info about this plugin
	 */
	private def showAbout() {
		IJ.showMessage("Interaction estimation based on statistical object-based co-localization framework",
				"TODO, shift the blame on the developper." //TODO showAbout   
		);
	}
	
	private def readMatlabData():(Array[Int],(Array[Double] => Boolean),Array[Array[Double]],Array[Array[Double]]) = {
		import mosaic.calibration.ReadMat
		val path = "/Users/marksutt/Documents/MA/data/"
		val matFileName = "TestPlugin/EndVir.mat"
		val delx:Double = 160; val delz:Double = 400; val voxDepthFactor = delz/delx ; val res:Double = 80
		val refGroup = ReadMat.getMatrix(path + matFileName,"Endosomes3D")
		val queryGroup = ReadMat.getMatrix(path + matFileName,"Viruses3D")
		def scaleCoord(coord: Matrix, voxDepthFactor:Double, pixelSize:Double = 1) {
			// the upper left pixel in the first slice is (0.5,0.5,0.0) in the output of
			// the 3D tracker. Shift z component!
			val voxelDepth = voxDepthFactor * pixelSize
			coord.getCol(0) *= pixelSize; coord.getCol(1) *= pixelSize;  
			coord.getCol(2) *= voxelDepth; coord.getCol(2) += voxelDepth/2;
		}
		scaleCoord(refGroup, voxDepthFactor)
		scaleCoord(queryGroup, voxDepthFactor)
		val arr:Array[Array[Double]] = (for (i <- (0 until refGroup.rows)) yield refGroup.getRow(i).toArray).toArray
		val arrQuery:Array[Array[Double]] = (for (i <- (0 until queryGroup.rows)) yield queryGroup.getRow(i).toArray).toArray
		
		val maskOpenMacro = "run(\"Image Sequence...\", \"open=" +path + "/3Ddata/Endosomes/Mask_2/3110.tif number=14 starting=0 increment=1 scale=100 file=[] or=[] sort\");"
		IJ.runMacro(maskOpenMacro)
		val maskLoaded = IJ.getImage()
		val outline = (new CellOutline())
		outline.setMask(maskLoaded)
		//val isInDomain = (x:Array[Double]) => outline.inCell(x.map((d:Double) => Math.floor(d/delx + 0.5)))
		val isInDomain = (x:Array[Double]) => {x(2) = x(2) / voxDepthFactor; outline.inCell(x)}
		val domainSize = Array[Int](maskLoaded.getWidth, maskLoaded.getHeight, maskLoaded.getNSlices)
		(domainSize, isInDomain, arr, arrQuery)
	}
}