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
		
		test_compare_new();
		
	}
	
	// write each test as a void method so that don't have to write a lot in main
	
	public static void test_compare_new() {
		// set parameters
		int trackID = 59;
		int frame = 741;
		int increment = 1;
		int derivMethod = 1;
		
		// deal with derivMethod
		int first = frame;
		int second = frame;
		int dt = increment;
		switch(derivMethod) {
		case 1: //forward
			second = frame+increment;
			System.out.println("Derive method: forward");
			break;
		case 2: //backward
			first = frame-increment;
			System.out.println("Derive method: backward");
			break;
		case 3: //central
			first = frame-increment;
			second = frame+increment;
			dt = increment*2;
			System.out.println("Derive method: central");
		}
		
		// load prejav
		// note: for some reason need to do this outside the getIms methods
		String prejavPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(prejavPath);
		Track tr = ex.getTrack(trackID);
		
		// get point size Ims and ddtIm from mmf
		ImageStack mmf = test_getImsMmf(tr,first,second);
		
		// get point size Ims and ddtIm from prejav
		ImageStack prejav = test_getImsPrejav(tr,first,second);
		
		// visualization
		ImagePlus mmfPlus = new ImagePlus("from MMF", test_zoom(mmf));
		mmfPlus.show();
		ImagePlus prejavPlus = new ImagePlus("from prejav", test_zoom(prejav));
		prejavPlus.show();
	}
	
	public static ImageStack test_getImsMmf(Track tr, int first, int second) {
		// load mmf
		String mmfPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(mmfPath);
		ImageStack mmf = mr.getMmfStack();
		
		// grab frames
		ImageProcessor frameIm1 = mmf.getProcessor(first);
		ImageProcessor frameIm2 = mmf.getProcessor(second);
		int width = frameIm1.getWidth();
		int height = frameIm1.getHeight();
		ImageStack frames = new ImageStack(width,height);
		frames.addSlice(frameIm1);
		frames.addSlice(frameIm2);
		
		// generate whole frame ddtIm
		int dt = second-first;
		ImageProcessor ddtIm = test_calcTimeDeriv(frames,dt);
		
		/**
		// load prejav
		String prejavPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(prejavPath);
		Track tr = ex.getTrack(trackID);
		*/
		
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		
		// crop to get point size ddtIm
		frameIm1.setRoi(newRect);
		ImageProcessor ret1 = frameIm1.crop();
		frameIm2.setRoi(newRect);
		ImageProcessor ret2 = frameIm2.crop();
		ddtIm.setRoi(newRect);
		ImageProcessor ret3 = ddtIm.crop();
		
		// prepare return stack
		ImageStack ret = new ImageStack(newRect.width, newRect.height);
		ret.addSlice(ret1.convertToColorProcessor());
		ret.addSlice(ret2.convertToColorProcessor());
		ret.addSlice(ret3);
		return ret;
	}
	
	public static ImageStack test_getImsPrejav(Track tr, int first, int second) {
		/**
		// load prejav
		String prejavPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(prejavPath);
		Track tr = ex.getTrack(trackID);
		*/
		
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		
		// pad point images
		ImageProcessor padded1 = test_padImage(pt1,newRect);
		ImageProcessor padded2 = test_padImage(pt2,newRect);
		ImageStack points = new ImageStack(newRect.width,newRect.height);
		points.addSlice(padded1.convertToColorProcessor());
		points.addSlice(padded2.convertToColorProcessor());
		
		// generate point size ddtIm
		int dt = second-first;
		points.addSlice(test_calcTimeDeriv(points,dt));
		
		// prepare return stack
		return points;
	}
	
	///////////////
	// Old tests
	///////////////
	
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
	public static ImageProcessor test_calcTimeDeriv(ImageStack theseIm, int dt) {
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
		
		//ddtIm.autoThreshold();
		// TODO RGB threshold to zero and gray scale
		return ddtIm;
	}

	public static ImageStack test_zoom(ImageStack these) {
		// get original size images
		ImageProcessor im1 = these.getProcessor(1);
		ImageProcessor im2 = these.getProcessor(2);
		ImageProcessor im3 = these.getProcessor(3);
		int width = im1.getWidth();
		int height = im1.getHeight();
		
		// decide if need to zoom in
		int zoom = 1;
		if(width<300) {
			zoom = 300/width;
		}
		
		ImageStack ret = new ImageStack(width*zoom,height*zoom);
		ret.addSlice(im1.resize(width*zoom));
		ret.addSlice(im2.resize(width*zoom));
		ret.addSlice(im3.resize(width*zoom));
		
		return ret;
	}
	
	///////////////
	// Old tests
	///////////////
	
	/**
	public static ImageStack test_loadTestMMF() {
		// load entire MMF
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(path);
		ImageStack mmf = mr.getMmfStack();
		
		// visualization
		ImagePlus mmfViewer = new ImagePlus("entire MMF", mmf);
		mmfViewer.show();
		
		return mmf;
		
	}
	*/
	
	/**
	 * Grabs track #59 from sampleShortExp_copy.prejav
	 * <p>
	 * (Not a stand alone test; written for convenience)
	 */
	/**
	public static Track test_loadTestTrack() {
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(path);
		Track tr = ex.getTrack(59);
		// trackID=59: got significant disturbance at frame #739-755
		// good for test purposes
		
		
		// check extraction parameters of this experiment
		ExtractionParameters ep = ex.getEP();
		System.out.println("Frame range: " + ep.startFrame + " to " + ep.endFrame);
		// 500 frames?!!! This must not have been updated
		
		
		// check frame range directly
		System.out.println("Start frame: " + ex.getStartFrame());
		System.out.println("Number of frames: " + ex.getNumFrames());
		
		return tr;
		
	}
	*/
	
	/**
	public static void test_viewExperiment() {
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment_Viewer ev = new Experiment_Viewer();
		ev.run(path);
	}
	*/

	/**
	 * Grabs the correct frames at/near time t from a track depending on chosen deriv method
	 * @param frame
	 * @param increment
	 * @param derivMethod
	 * @return
	 */
	/**
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
	*/
	
	/**
	 * Grabs the correct points at/near time t from a track depending on chosen deriv method
	 * @param trackID track we're working on (not in there for now)
	 * @param frame frame we're working on
	 * @param increment time step between frames
	 * @param derivMethod forward(1)/backward(2)/central(3)
	 * @return a ImageStack of 2 points
	 */
	/**
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
		
		
		// visualization (zoom in to 300 px)
		int zoomFactor = (int)300/newRect.width;
		ImageStack showThese = new ImageStack(newRect.width*zoomFactor, newRect.height*zoomFactor);
		showThese.addSlice(im1.resize(newRect.width*zoomFactor));
		showThese.addSlice(im2.resize(newRect.width*zoomFactor));
		ImagePlus theseImP = new ImagePlus("grabbed points", showThese);
		theseImP.show();
		
		return theseIm;
	}
	*/
	
	/**
	public static void test_compare() {
		// set param
		int trackID = 59;
		int frame = 741;
		int increment = 1;
		int derivMethod = 1;
		int dt = increment;
		
		// deal with derivMethod
		if(derivMethod==3) {
			dt = increment*2;
		}
		
		ImageStack frames = test_grabFrames(frame,increment,derivMethod);
		ImageProcessor ddtFrames = test_calcTimeDeriv(frames,dt);
		
		ImageStack points = test_grabPoints(frame,increment,derivMethod);
		ImageProcessor ddtPoints = test_calcTimeDeriv(points,dt);
		
		// crop frames and ddtFrames wrt newRect
		// get newRect
		Track tr = test_loadTestTrack();
		int first = frame;
		int second = frame+increment; // for derivMethod=1 only
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		System.out.println("Roi for MMF, offset: (" + newRect.x + "," + newRect.y + "), dimension: " + newRect.width + "x" + newRect.height);
		ImageStack framePoints = new ImageStack(newRect.width,newRect.height);
		ImageProcessor tmp1 = frames.getProcessor(first);
		tmp1.setRoi(newRect);
		tmp1.crop();
		framePoints.addSlice(tmp1);
		ImageProcessor tmp2 = frames.getProcessor(first+increment);
		tmp2.setRoi(newRect);
		tmp2.crop();
		framePoints.addSlice(tmp2);
		ImagePlus viewer = new ImagePlus("cropped frame points",framePoints);
		viewer.show();
	}
	*/

}
