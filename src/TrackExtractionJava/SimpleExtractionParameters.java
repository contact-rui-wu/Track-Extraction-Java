package TrackExtractionJava;

public class SimpleExtractionParameters {
	//gives access to the most commonly used extraction and fitting parameters
	
	private int endFrame;
	private int startFrame;
	private int globalThreshValue;
	private double frameRate;
	private double typicalLarvaLengthInPixels = 20;
	private double timeLengthScaleFactor = 0.5;
	private double timeSmoothScaleFactor = 1;
	private float imageWeight;
	private float spineLengthWeight;
	private float spineSmoothWeight;
	
	
	public SimpleExtractionParameters() {
		// TODO Auto-generated constructor stub
	}
	
	public ExtractionParameters getExtractionParameters () {
		ExtractionParameters ep = new ExtractionParameters();
		ep.endFrame = endFrame;
		ep.startFrame = startFrame;
		ep.globalThreshValue = globalThreshValue;
		ep.minArea = typicalLarvaLengthInPixels;
		ep.maxArea = typicalLarvaLengthInPixels*typicalLarvaLengthInPixels;
		ep.maxMatchDist = typicalLarvaLengthInPixels/2 + typicalLarvaLengthInPixels/frameRate;
		return ep;
	}
	
	public FittingParameters getStraightFittingParameters() {
		FittingParameters fp = new FittingParameters();
		fp.imageWeight = imageWeight;
		fp.spineExpansionWeight = -1;
		fp.spineLengthWeight = spineLengthWeight;
		fp.spineSmoothWeight = spineSmoothWeight;
		fp.timeLengthWeight = new float[] {(float) (timeLengthScaleFactor*frameRate/15)};
		fp.timeSmoothWeight = new float[] {(float) (timeSmoothScaleFactor*frameRate/15)};
		
		
		return fp;
	}

}
