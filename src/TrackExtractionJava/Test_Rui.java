/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

// what to import?
// - ImageJ ImageProcessor stuff

public class Test_Rui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		//test_pointGraber();
		
		//test_ddt();
		
		//test_scribe();
	}
	
	// write each test as a void method so that don't have to write a lot in main

	/**
	 * Grabs the correct points from a track depending on chosen deriv method
	 * @param track track we're working on
	 * @param timeStamp frame we're working on
	 * @param derivMethod forward/backward/central
	 * @return 2 or 3 points depending on derivMethod
	 */
	public static void test_pointGraber() {
		// actually not void
	}
	
	/**
	 * Computes ddt for one point
	 * @param derivMethod forward/backward/central
	 * @param thisIm an ImageJ ImageProcessor object (I think); always needed
	 * @param lastIm needed for backward and central
	 * @param nextIm needed for forward and central
	 * @return ddt value matrix, doesn't have to be ImageProcessor (right?)
	 */
	public static void test_ddt(int derivMethod, ImageProcessor thisIm, ImageProcessor lastIm, ImageProcessor nextIm) {
		// actually not void
		// 3 methods: forward, backward, central
		// assume correct roi TODO think about how to update roi (check getCombinedBounds)
		// assume same threshold TODO later need to account for changed threshold
		// assume 8-bit gray scale
		
		// load frames
		// suppose we have width, height, *Im.frameNum, *Im.getPixVal(i,j)
		
		// default: assume 1 frame increment between frames TODO check FrameLoader
		int dt = 1;
		
		// forward
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				int PixDiff = nextIm.getPixVal(i,j)-thisIm.getPixVal(i,j);
				float ddt = PixDiff/dt;
			}
		}
		
		// backward
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				int PixDiff = thisIm.getPixVal(i,j)-lastIm.getPixVal(i,j);
				float ddt = PixDiff/dt;
			}
		}
		
		// central
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				int PixDiff = nextIm.getPixVal(i,j)-lastIm.getPixVal(i,j);
				float ddt = PixDiff/(2*dt);
			}
		}
		
	}
	
	/**
	 * Adds ddt value to the corresponding point
	 * @param thisPoint point to add to
	 * @param ddtMtx ddt value matrix computed by test_ddt
	 */
	public static void scribe() {
		
	}

}
