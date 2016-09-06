package TrackExtractionJava;

import ij.process.FloatPolygon;

import java.util.Arrays;
import java.util.Vector;


/* expands/contracts backbone along lines from center of mass to each point
 * so that the new length of the backbone is targetLength
 * 
 */
public class SpineExpansionForce extends Force{

	static final String defaultName = "Spine-Expansion";
	private float targetLength;
	private boolean comExpansion = false;
	public SpineExpansionForce(float[] weights, float totalWeight, float targetLength) {
		this (weights, totalWeight, targetLength, true);
	}
	public SpineExpansionForce(float[] weights, float totalWeight, float targetLength, boolean comExpansion){
		super(weights, totalWeight, 2, "Spine-Length");
		this.targetLength = targetLength;
//		this.setTargetLength(targetLength);
		this.comExpansion = comExpansion;
	}
	
	
	private FloatPolygon expandAlongBackbone (BackboneTrackPoint btp) {
		double x[] = MathUtils.castFloatArray2Double(btp.bbOld.xpoints);
		double y[] = MathUtils.castFloatArray2Double(btp.bbOld.ypoints);
		double alpha = targetLength/MathUtils.curveLength(x, y);
		
		int mid = x.length/2;
		float[] targetX = new float[x.length];
		float[] targetY = new float[x.length];
		targetX[mid] = (float) x[mid];
		targetY[mid] = (float) y[mid];
		
		for (int j = mid -1; j >= 0; --j) {
			targetX[j] = targetX[j+1] + (float) (alpha*(x[j] - x[j+1]));
			targetY[j] = targetY[j+1] + (float) (alpha*(y[j] - y[j+1]));
		}
		for (int j = mid+1; j < x.length; ++j) {
			targetX[j] = targetX[j-1] + (float) (alpha*(x[j] - x[j-1]));
			targetY[j] = targetY[j-1] + (float) (alpha*(y[j] - y[j-1]));
		}

		return new FloatPolygon(targetX, targetY);
		
	}
	private FloatPolygon expandFromCenterOfMass (BackboneTrackPoint btp){
		double x[] = MathUtils.castFloatArray2Double(btp.bbOld.xpoints);
		double y[] = MathUtils.castFloatArray2Double(btp.bbOld.ypoints);
		
		double cx = MathUtils.mean(x);
		double cy = MathUtils.mean(y);
		
		double alpha = targetLength/MathUtils.curveLength(x, y);
		
		
		int numBBPts = btp.getNumBBPoints();

		float[] targetX = new float[numBBPts];
		Arrays.fill(targetX, 0);
		float[] targetY = new float[numBBPts];
		Arrays.fill(targetY, 0);
		
		for (int k = 0; k < x.length; ++k) {
			targetX[k] = (float) (alpha*(x[k] - cx) + cx);
			targetY[k] = (float) (alpha*(y[k] - cy) + cy);
		}
		
		
		return new FloatPolygon(targetX, targetY);
	}
	
	public FloatPolygon getTargetPoints(int btpInd, Vector<BackboneTrackPoint> allBTPs){
		
		
		
		BackboneTrackPoint btp = allBTPs.get(btpInd);
		if (comExpansion) {
			return expandFromCenterOfMass(btp);
		} else {
			return expandAlongBackbone(btp);
		}
		
		
	}


	public float getTargetLength() {
		return targetLength;
	}


	public void setTargetLength(float targetLength) {
		this.targetLength = targetLength;
	}
	
	
}
