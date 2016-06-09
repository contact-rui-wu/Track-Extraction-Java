/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.Graphics;

import ij.process.ImageProcessor;
import ij.process.ByteProcessor;
//import ij.process.FloatProcessor;
import ij.process.ColorProcessor;
import ij.ImageStack;
import ij.ImagePlus;
//import ij.ImageJ;

import edu.nyu.physics.gershowlab.mmf.mmf_Reader;
import edu.nyu.physics.gershowlab.mmf.MmfVirtualStack;

public class TestRui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		ImageStack mmf = test_loadTestMMF();
		
		//test_wholeProcedure(1);
		// now works with padding and true raw image!
		
		//test_NB();
		// problem: calcImDeriv needs ImTrackPoint
		// possible fix: change its params to accept TrackPoint
		// but needs to check actual point type
		// need to ask Natalie about converting between point types
	}
	
	// write each test as a void method so that don't have to write a lot in main
	
	public static ImageStack test_loadTestMMF() {
		// loda full size MMF
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleMMF_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(path);
		MmfVirtualStack mmf = mr.getMmfStack();
		
		// crop
		int width = mmf.getProcessor(1).getWidth();
		int height = mmf.getProcessor(1).getHeight();
		System.out.println("Dimension: " + width + "x" + height);
		System.out.println("Number of frames: " + mmf.getSize());
		//mmf = mmf.crop(1,1,1,width,height,2000); //doesn't work
		//mmf.deleteLastSlice(); //doesn't work
		int start = 1;
		int stop = 300;
		ImageStack retStack = new ImageStack(width,height);
		for(int i=start; i<stop+1; i++) {
			ImageProcessor ip = mmf.getProcessor(i);
			retStack.addSlice(ip);
		}
		// problem: brute force takes lot of memory, will crash for >300 frames
		// TODO is there something in mmf_Reader that I can use?
		
		// visualization
		ImagePlus retPlus = new ImagePlus("crop test", retStack);
		retPlus.show();
		
		return retStack;
	}
	
	/**
	 * Grabs track #59 from sampleShortExp_copy.prejav
	 * <p>
	 * (Not a stand alone test; written for convenience)
	 */
	public static Track test_loadTestTrack() {
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(path);
		Track tr = ex.getTrack(59);
		return tr;
		// trackID=59: got significant disturbance at frame #739-755
		// good for test purposes
	}

	/**
	 * Grabs the correct frames at/near time t from a track depending on chosen deriv method
	 * @param frame
	 * @param increment
	 * @param derivMethod
	 * @return
	 */
	public static ImageStack test_grabFrames(int frame, int increment, int derivMethod) {
		// load mmf
		ImageStack mmf = test_loadTestMMF();
		
		// deal with derivMethod
		int first = frame;
		int second = frame;
		
		switch(derivMethod) {
		case 1: // forward
			System.out.println("derivMethod: forward");
			// grab t then t+1
			second = frame+increment;
			break;
		case 2: // backward
			System.out.println("derivMethod: backward");
			// grab t-1 then t
			first = frame-increment;
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
		
		// grab images
		ImageProcessor im1 = mmf.getProcessor(first);
		ImageProcessor im2 = mmf.getProcessor(second);
		int width = im1.getWidth();
		int height = im1.getHeight();
		
		// prepare return stack
		ImageStack theseFrames = new ImageStack(width, height);
		theseFrames.addSlice(im1);
		theseFrames.addSlice(im2);
		
		// visualization
		ImagePlus showThese = new ImagePlus("grabbed frames", theseFrames);
		showThese.show();
		
		return theseFrames;
	}
	
	/**
	 * Grabs the correct points at/near time t from a track depending on chosen deriv method
	 * @param trackID track we're working on (not in there for now)
	 * @param frame frame we're working on
	 * @param increment time step between frames
	 * @param derivMethod forward(1)/backward(2)/central(3)
	 * @return a ImageStack of 2 points
	 */
	//@SuppressWarnings("static-access") //to make getCombinedBounds happy
	public static ImageStack test_grabPoints(int frame, int increment, int derivMethod) {
		// load sample track
		Track tr = test_loadTestTrack();
		
		// set parameters
		// obsolete: maggot images are not accurate
		//MaggotDisplayParameters mdp = new MaggotDisplayParameters();
		//mdp.setAllFalse(); //do now draw backbone labels
		
		int first = frame;
		int second = frame;
		
		switch(derivMethod) {
		case 1: // forward
			System.out.println("derivMethod: forward");
			// grab t then t+1
			//first = frame;
			second = frame+increment;
			break;
		case 2: // backward
			System.out.println("derivMethod: backward");
			// grab t-1 then t
			first = frame-increment;
			//second = frame;
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
		
		/**
		// get MaggotTrackPoint image - works
		
		// grab points
		ImageProcessor im1 = tr.getFramePoint(first).getIm(mdp);
		ImageProcessor im2 = tr.getFramePoint(second).getIm(mdp);
		// note: this is MaggotTrackPoint.getIm()
		// it's already padded and centered, which is not what we want
		
		// grab images and put into return stack
		ImageStack theseIm = new ImageStack(im1.getWidth(),im1.getHeight());
		theseIm.addSlice(im1);
		theseIm.addSlice(im2);
		*/
		
		///**
		// get ImTrackPoint image (true raw image) - works
		
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		
		// pad with true global coordinates
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		System.out.println("offset: (" + newRect.x + "," + newRect.y + "), dimension: " + newRect.width + "x" + newRect.height);
		// (moved getCombinedBounds from ImTrackPoint to TrackPoint to make it happy)
		ImageProcessor im1 = test_padImage(pt1, newRect);
		ImageProcessor im2 = test_padImage(pt2, newRect);
		
		// put padded images into return stack
		ImageStack theseIm = new ImageStack(newRect.width, newRect.height);
		theseIm.addSlice(im1);
		theseIm.addSlice(im2);
		//*/
		
		// visualization (zoom in to 300 px)
		int zoomFactor = (int)300/newRect.width;
		ImageStack showThese = new ImageStack(newRect.width*zoomFactor, newRect.height*zoomFactor);
		showThese.addSlice(im1.resize(newRect.width*zoomFactor));
		showThese.addSlice(im2.resize(newRect.width*zoomFactor));
		ImagePlus theseImP = new ImagePlus("grabbed points", showThese);
		theseImP.show();
		
		return theseIm;
	}
	
	/**
	 * Pads a point to a new roi
	 * <p>
	 * (Not a standalone test; written for test_grabPoints())
	 * @param pt
	 * @param newRect
	 * @return padded image
	 */
	public static ImageProcessor test_padImage(TrackPoint pt, Rectangle newRect) {
		// following CVUtils.padAndCenter()
		ImagePlus im = new ImagePlus("original", pt.getRawIm());
		//im.show();
		int type = im.getBufferedImage().getType();
		BufferedImage newIm = new BufferedImage(newRect.width, newRect.height,type);
		Graphics g = newIm.getGraphics();
		g.setColor(Color.black);
		g.fillRect(0, 0, newRect.width, newRect.height);
		int offsetX = pt.rect.x - newRect.x;
		int offsetY = pt.rect.y - newRect.y;
		g.drawImage(im.getBufferedImage(), offsetX, offsetY, null);
		
		ImageProcessor retIm = new ByteProcessor(newIm);
		//retIm.show();
		return retIm;
	}
	
	/**
	 * Given 2 images (same dimension), compute ddt image between them
	 * @param theseIm 2-image stack
	 * @param dt time increment setting
	 * @return ddt image, color
	 */
	public static ImageProcessor test_calcTimeDeriv(ImageStack theseIm, int dt) { //using test_grabPoints
		// assume 8-bit gray scale
		// Q: do I still need to deal with threshold?	
		
		// load images
		ImageProcessor im1 = theseIm.getProcessor(1);
		ImageProcessor im2 = theseIm.getProcessor(2);
		
		// prepare images for calculation
		// temp fix: hard set these points to be ByteProcessors
		// problem is with ImageJ's ImageStack, not with our code
		// TODO use getBitDepth() and check processor type
		im1 = im1.convertToByteProcessor();
		im2 = im2.convertToByteProcessor();
		int width = theseIm.getWidth();
		int height = theseIm.getHeight();

		// prepare empty result image
		//FloatProcessor ddtIm = new FloatProcessor(width, height);
		ColorProcessor ddtIm = new ColorProcessor(width, height);
		// fill ddtIm
		for(int i=0; i<width; i++) {
			for(int j=0; j<height; j++) {
				int pixDiff = im2.getPixel(i,j)-im1.getPixel(i,j);
				int ddt = pixDiff/dt;
				//float ddt = ((pixDiff+255)/2)/dt;
				//ddtIm.setf(i,j,ddt);
				if (pixDiff>0) {
					ddtIm.setColor(new Color(ddt,0,0));//move into: red
				} else {
					ddtIm.setColor(new Color(0,0,-ddt)); //move out of: blue
				}
				ddtIm.drawPixel(i,j);
			}
		}
		
		// visualization:
		// RGB mode
		ddtIm.autoThreshold(); //if just want to see color binary
		ImagePlus ddtImPlus = new ImagePlus();
		if(width<300 || height<300) {
			int zoomFactor = (int)300/width;
			ddtImPlus.setProcessor(ddtIm.resize(width*zoomFactor));
		} else {
			ddtImPlus.setProcessor(ddtIm);
		}
		ddtImPlus.show();
		// TODO gray scale mode
		
		return ddtIm;
	}
	
	public static void test_wholeProcedure(int fop) {
		// set param
		int frame = 741;
		int increment = 1;
		int derivMethod = 1;
		int dt = increment;
		ImageStack theseIm = new ImageStack();
		
		// grab images
		if(fop==1) {
			theseIm = test_grabFrames(frame,increment,derivMethod);
		} else if(fop==2) {
			theseIm = test_grabPoints(frame,increment,derivMethod);
		} else {
			System.out.println("Choose 1 (frames) or 2 (points)");
			return;
		}
		
		// calculate and show
		if(derivMethod==3) {
			dt = increment*2;
		}
		ImageProcessor ddtIm = test_calcTimeDeriv(theseIm, dt);
		
		// for frames, crop for trackID 59 and show
		if(fop==1) {
			Track tr = test_loadTestTrack();
			TrackPoint pt1 = tr.getFramePoint(frame);
			TrackPoint pt2 = tr.getFramePoint(frame+increment);
			// TODO repeated code in grabPoints() I think
			Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
			System.out.println("offset: (" + newRect.x + "," + newRect.y + "), dimension: " + newRect.width + "x" + newRect.height);
			ddtIm.setRoi(newRect);
			ImageProcessor newIm = ddtIm.crop();
			int zoomFactor = (int)300/newRect.width;
			ImagePlus ddtImCropped = new ImagePlus("cropped", newIm.resize(newRect.width*zoomFactor));
			ddtImCropped.show();
			// problem: cropped image always black
			// STUPID: they're not the same experiment....
		}
	}
	
	public static void test_NB() {
		
		// load experiment and track
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(path);
		//Track tr = ex.getTrack(trID);
		
		// set parameters
		int trID = 59;
		int frame = 741; // change manually
		int increment = 1;
		int derivMethod = 1; // change manually: 1 for forward, 2 for backward, 3 for central
		MaggotDisplayParameters mdp = new MaggotDisplayParameters();
		mdp.setAllFalse();
		
		/**
		// check point type
		TrackPoint test = ex.getTrack(trID).getFramePoint(frame);
		System.out.println(test.getTypeName());
		System.out.println(test.pointType);
		// ok it's a MaggotTrackPoint
		// need to turn off backbone display in MaggotDisplayParameters
		ImagePlus testImPlus = new ImagePlus(null, test.getIm(mdp));
		testImPlus.show();
		// success
		 */
		
		///**
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
		//*/
	}

}
