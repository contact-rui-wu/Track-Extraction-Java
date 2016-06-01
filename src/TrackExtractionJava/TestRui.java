/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.ImageStack;
import ij.ImagePlus;

public class TestRui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		//test_grabPoints();
		
		test_calcTimeDeriv();
		
		//test_scribe();

	}
	
	// write each test as a void method so that don't have to write a lot in main

	/**
	 * Grabs the correct points at/near time t from a track depending on chosen deriv method
	 * @param track track we're working on
	 * @param t frame we're working on
	 * @param increment time step between frames
	 * @param derivMethod forward(1)/backward(2)/central(3)
	 * @return 2 or 3 points depending on derivMethod
	 */
	public static void test_grabPoints() {
		// placeholder
		ImageStack track;
		int t;
		int increment;
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
	public static void test_calcTimeDeriv() {
		// assume 8-bit gray scale
		// Q: do I still need to deal with threshold?	
		
		int dt=1; // change manually: 1 for forward/backward, 2 for central
		
		// 1) simple, generated binary points
		/**
		ByteProcessor point1 = new ByteProcessor(100,100);
		ByteProcessor point2 = new ByteProcessor(100,100);
		int width = point1.getWidth();
		int height = point1.getHeight();
		int xcenter = width/2;
		int ycenter = height/2;
		// fill point1: center
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(Math.abs(i-xcenter)<20 && Math.abs(j-ycenter)<20) {
					point1.set(i,j,255);
				} else {
					point1.set(i,j,0);
				}
			}
		}
		// fill point2: move 10px to the right
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(Math.abs(i-xcenter-10)<20 && Math.abs(j-ycenter)<20) {
					point2.set(i,j,255);
				} else {
					point2.set(i,j,0);
				}
			}
		}		
		*/
		
		// 2) simple, generated gradient points
		///**
		ByteProcessor point1 = new ByteProcessor(100,100);
		ByteProcessor point2 = new ByteProcessor(100,100);
		int width = point1.getWidth();
		int height = point1.getHeight();
		int xcenter = width/2;
		int ycenter = height/2;
		// fill point1: center
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(Math.abs(i-xcenter)<20 && Math.abs(j-ycenter)<20) {
					point1.set(i,j,255-(i*2+50));
				} else {
					point1.set(i,j,0);
				}
			}
		}
		// fill point2: move 10px to the right
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				if(Math.abs(i-xcenter-10)<20 && Math.abs(j-ycenter)<20) {
					point2.set(i,j,255);
				} else {
					point2.set(i,j,0);
				}
			}
		}		
		//*/
		
		// 3) external test images
		///**
		// TODO see if can use FrameLoader or PointExtractor
		//*/
		
		// TODO get correct roi
		// check getCombinedBounds
		// for now: assume they overlap, use point1 dimension

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
		
		// for test purpose: visualization
		ImagePlus point1Plus = new ImagePlus("point1", point1);
		point1Plus.show();
		ImagePlus point2Plus = new ImagePlus("point2", point2);
		point2Plus.show();
		ImagePlus ddtImPlus = new ImagePlus("ddt image", ddtIm);
		ddtImPlus.show();
		
	}
	
	/**
	 * Adds ddt value to the corresponding point
	 * @param t frame we're working on
	 * @param ddtIm ddt value matrix computed by test_ddt
	 */
	public static void test_scribe() {
		// TODO think of better name
	}

}
