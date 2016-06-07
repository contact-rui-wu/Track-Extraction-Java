/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ColorProcessor;
import ij.ImageStack;
import ij.ImagePlus;

import edu.nyu.physics.gershowlab.mmf.mmf_Reader;

public class TestRui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		//test_grabPoints();
		// works!
		
		//test_calcTimeDeriv();
		// works apart from the fact that the images grabbed are not raw
		// if use ImTrackPoint.getRawIm, it's not automatically enlarged (why?)
		// if use ImTrackPoint.getIm, returns 300x300 image but with the backbone points
		
		//test_MMF();
		
		//test_prejav();
		
		//test_NB();
	}
	
	// write each test as a void method so that don't have to write a lot in main
	
	/////////////////////////////////////////
	// Opening and playing with .MMF files
	/////////////////////////////////////////
	
	public static void test_MMF() {
		//Experiment_Processor ep = new Experiment_Processor();
		//ep.runningFromMain = true;
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleMMF_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		//String path = "";
		mr.loadStack(path);
		ImagePlus mmfStack = new ImagePlus(path, mr.getMmfStack());
		mmfStack.show();
		// Worked!
	}
	
	////////////////////////////////////////////
	// Opening and playing with .prejav files
	////////////////////////////////////////////
	
	public static void test_prejav() {
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		//Experiment_Viewer exv = new Experiment_Viewer();
		//exv.run(path);
		// above: opens a panel, can look at numbers in each track, can play track movie - success
		Experiment ex = new Experiment(path);
		//int x = ex.getNumFrames();
		//System.out.println(x);
		// above: loads tracked experiment and get # of frames - success
		Track tr = ex.getTrack(59);
		tr.playMovie();
		// trackID=59: got significant disturbance at frame #739-755
		// good for testing
	}
	
	/////////////////////////////////
	// Calculating time derivative
	/////////////////////////////////
	
	/**
	 * Grabs the correct points at/near time t from a track depending on chosen deriv method
	 * @param track track we're working on
	 * @param t frame we're working on
	 * @param increment time step between frames
	 * @param derivMethod forward(1)/backward(2)/central(3)
	 * @return 2 or 3 points depending on derivMethod
	 */
	public static ImageStack test_grabPoints() {
		// load sample track
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(path);
		Track tr = ex.getTrack(59);
		
		// set parameters
		int frame = 741; // change manually TODO write as loop index
		int increment = 1;
		int derivMethod = 3; // change manually: 1 for forward, 2 for backward, 3 for central
		
		// prepare empty result image stack
		ImagePlus im1 = new ImagePlus();
		ImagePlus im2 = new ImagePlus();
		int first = frame;
		int second = frame;
		
		switch(derivMethod) {
		case 1: // forward
			System.out.println("derivMethod: forward");
			// grab t then t+1
			first = frame;
			second = frame+increment;
			break;
		case 2: // backward
			System.out.println("derivMethod: backward");
			// grab t-1 then t
			first = frame-increment;
			second = frame;
			break;
		case 3: // central
			System.out.println("derivMethod: central");
			// grab t-1 then t+1
			first = frame-increment;
			second = frame+increment;
			break;
		default:
			System.out.println("Invalid derivMethod");
			break;
		}
		
		im1.setProcessor(tr.getFramePoint(first).getIm());
		im2.setProcessor(tr.getFramePoint(second).getIm());
		// note: if use ImTrackPoint.getIm, will get backbone points as well
		// see if getRawIm returns same dimension
		// nope (but does get rid of backbone points)
		//im1.show();
		//im2.show();
		
		// prepare return stack
		ImageStack theseIm = new ImageStack(im1.getWidth(),im1.getHeight());
		theseIm.addSlice(im1.getProcessor());
		theseIm.addSlice(im2.getProcessor());
		
		// for test purpose: visualization
		ImagePlus theseImP = new ImagePlus("grabbed points", theseIm);
		theseImP.show();
		
		return theseIm;
	}
	
	/**
	 * Given 2 points, compute ddt image between them
	 * <p>
	 * (Different from ImTrackPoint.calcImDeriv which returns red/blue color ddt image)
	 * @param point1 earlier point
	 * @param point2 later point
	 * @param dt time increment between them
	 * @return ddt image, 8 bit gray scale
	 */
	public static void test_calcTimeDeriv() { //using test_grabPoints
		// assume 8-bit gray scale
		// Q: do I still need to deal with threshold?	
		
		int dt=1; // default time step=1
		
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
		*/
		
		// 3) external test images
		///**
		ImageStack theseIm = test_grabPoints();
		ImageProcessor point1 = theseIm.getProcessor(1);
		ImageProcessor point2 = theseIm.getProcessor(2);
		int width = theseIm.getWidth();
		int height = theseIm.getHeight();
		//*/
		
		// TODO get correct roi
		// check getCombinedBounds
		// for now: use padded dimension

		// TODO change to RGB mode
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
		//ImagePlus point1Plus = new ImagePlus("point1", point1);
		//point1Plus.show();
		//ImagePlus point2Plus = new ImagePlus("point2", point2);
		//point2Plus.show();
		ImagePlus ddtImPlus = new ImagePlus("ddt image", ddtIm);
		ddtImPlus.show();
		
	}
	
	public static void test_NB() {
		// load experiment and track
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(path);
		//Track tr = ex.getTrack(trID);
		// set parameters
		int trID = 59;
		int frame = 741; // change manually TODO write as loop index
		int increment = 1;
		int derivMethod = 1; // change manually: 1 for forward, 2 for backward, 3 for central
		// load points
		// this point
		TrackPoint thisTP = ex.getTrack(trID).getFramePoint(frame);
		ImagePlus thisIm = new ImagePlus(null, ex.getTrack(trID).getFramePoint(frame).getRawIm());
		ImTrackPoint thisITP = new ImTrackPoint(thisTP, thisIm);
		// previous point
		TrackPoint prevTP = ex.getTrack(trID).getFramePoint(frame-increment);
		ImagePlus prevIm = new ImagePlus(null, ex.getTrack(trID).getFramePoint(frame-increment).getRawIm());
		ImTrackPoint prevITP = new ImTrackPoint(prevTP, prevIm);
		// next point
		TrackPoint nextTP = ex.getTrack(trID).getFramePoint(frame+increment);
		ImagePlus nextIm = new ImagePlus(null, ex.getTrack(trID).getFramePoint(frame+increment).getRawIm());
		ImTrackPoint nextITP = new ImTrackPoint(nextTP, nextIm);
		// problem: convert TP to ITP (calcImDeriv only recognizes ITP)
		// set derivative
		thisITP.calcImDeriv(prevITP, nextITP, derivMethod);
		ImagePlus thisPlus = new ImagePlus("imDeriv", thisITP.imDeriv);
		//thisPlus.show();
		int centerX = (int)thisITP.x-thisITP.rect.x;
		int centerY = (int)thisITP.y-thisITP.rect.y;
		ImageProcessor display = CVUtils.padAndCenter(thisPlus, 300, 300, centerX, centerY);
		ImagePlus displayPlus = new ImagePlus("imDerivDisplay", display);
		displayPlus.show();
	}

}
