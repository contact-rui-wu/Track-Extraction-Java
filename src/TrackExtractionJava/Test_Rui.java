/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.ImageStack;
import ij.ImagePlus;

public class Test_Rui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		/**
		test_pointGraber();
		*/
		
		/**
		 * prepare images for the test
		test_ddt();
		 */
		
		/**
		test_scribe();
		*/
	}
	
	// write each test as a void method so that don't have to write a lot in main

	/**
	 * Grabs the correct points at/near time t from a track depending on chosen deriv method
	 * @param track track we're working on
	 * @param t frame we're working on
	 * @param derivMethod forward(1)/backward(2)/central(3)
	 * @return 2 or 3 points depending on derivMethod
	 */
	public static void test_grabPoints() {
		// placeholder
		ImageStack track;
		int t;
		int derivMethod=1; // change manually: 1 for forward, 2 for backward, 3 for central
		
		// prepare empty result image stack
		ImageStack theseIm = new ImageStack();
		
		// for test purpose: visualization

	}
	
	/**
	 * Given 2 points, compute ddt between them
	 * @param point1 earlier point
	 * @param point2 later point
	 * @param dt time increment between them
	 * @return ddt image, 8 bit gray scale
	 */
	public static void test_ddt() {
		// placeholder
		ByteProcessor point1;
		ByteProcessor point2;
		int dt=1; // change manually: 1 for forward/backward, 2 for central
		
		// assume 8-bit gray scale
		// Q: do I still need to deal with threshold?
		
		// TODO get correct roi
		// check getCombinedBounds
		// for now: assume they overlap, use point1 dimension
		int width = point1.getWidth();
		int height = point1.getHeight();
		
		// prepare empty result image
		FloatProcessor ddtIm = new FloatProcessor(width, height);
		
		// fill ddtIm
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				int pixDiff = point2.getPixel(i,j)-point1.getPixel(i,j);
				float ddt = ((pixDiff+255)/2)/dt;
				ddtIm.setf(i,j,ddt);
			}
		}
		
		/**
		 * scratch: ddt actually doesn't need 3 images, so it dosen't need to know which method either
		// fill ddtIm
		switch(derivMethod) {
			case 1 : // forward
				for(int i=0; i<width; i++) {
					for(int j=0; j<height; j++) {
						int pixDiff = nextIm.getPixel(i,j)-thisIm.getPixel(i,j);
						float ddt = pixDiff/dt;
						// deal with -ve values (Natalie used color)
						ddtIm.setf(i,j,ddt);
					}
				}
			case 2 : // backward
				for(int i=0; i<width; i++) {
					for(int j=0; j<height; j++) {
						int PixDiff = thisIm.getPixel(i,j)-prevIm.getPixel(i,j);
						float ddt = PixDiff/dt;
						ddtIm.setf(i,j,ddt);
					}
				}
			case 3 : // central
				for(int i=0; i<width; i++) {
					for(int j=0; j<height; j++) {
						int PixDiff = nextIm.getPixel(i,j)-prevIm.getPixel(i,j);
						float ddt = PixDiff/(2*dt);
						ddtIm.setf(i,j,ddt);
					}
				}
		}
		*/
		
		// for test purpose: visualization
		ImagePlus ddtImPlus = new ImagePlus("ddt image", ddtIm);
		ddtImPlus.show();
		
	}
	
	/**
	 * Adds ddt value to the corresponding point
	 * @param thisPoint point to add to
	 * @param ddtIm ddt value matrix computed by test_ddt
	 */
	public static void test_scribe() {
		
	}

}
