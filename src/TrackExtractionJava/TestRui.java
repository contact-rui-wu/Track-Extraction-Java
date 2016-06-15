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
		
		//test_compare();
		
		test_workOnTrack();
		
	}
	
	// write each test as a void method so that don't have to write a lot in main
	
	/**
	 * Test: compares ddt image calculated from the whole frame image (.mmf) and from the saved point-size image (.prejav)
	 * <p>
	 * Conclusion: .prejav image loses edge pixel information; always use .mmf image in the future
	 */
	public static void test_compare() {
		// set parameters
		int trackID = 59;
		int frame = 741;
		int increment = 1;
		int derivMethod = 3;
		
		// deal with derivMethod
		int first = frame;
		int second = frame;
		//int dt = increment;
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
			//dt = increment*2;
			System.out.println("Derive method: central");
		}
		
		// load prejav
		// note: for some reason need to do this outside the getIms methods
		String prejavPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(prejavPath);
		Track tr = ex.getTrack(trackID);
		
		// get point size Ims and ddtIm from mmf
		System.out.println("Calculating derivative image from .mmf");
		ImageStack mmf = test_getImsMmf(tr,first,second);
		// visualization
		//ImagePlus mmfPlus = new ImagePlus("from MMF", test_zoom(mmf,300));
		ImagePlus mmfPlus = test_zoom(new ImagePlus("from MMF",mmf), 15);
		mmfPlus.getStack().getProcessor(3).autoThreshold(); //easier to see
		mmfPlus.show();
		
		/**
		// obsolete: get point size Ims and ddtIm from prejav
		System.out.println("Calculating derivative image from .prejav");
		ImageStack prejav = test_getImsPrejav(tr,first,second);
		// visualization
		ImagePlus prejavPlus = new ImagePlus("from prejav", test_zoom(prejav));
		prejavPlus.show();
		*/
	}
	
	public static void test_workOnTrack() {
		// set parameters
		int trackID = 59;
		int increment = 1;
		int derivMethod = 1;
		
		// load mmf
		String mmfPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(mmfPath);
		ImageStack mmf = mr.getMmfStack();
		
		// load prejav
		String prejavPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.prejav";
		Experiment ex = new Experiment(prejavPath);
		Track tr = ex.getTrack(trackID);
		int start = tr.getStart().getFrameNum();
		//int end = tr.getEnd().getFrameNum();
		int end = 1000;
		int dt = increment;
		
		// prepare result stack
		ImageStack ret = new ImageStack(30,30);
		
		// deal with derivMethod
		switch(derivMethod) {
		case 1: //forward
			System.out.println("Derive method: forward");
			for(int i=start;i<end;i++) { //TODO deal with first/last/missing points, or maybe write the loop as a seperate method
				if(i%100==0) {
					System.out.println("Working on " + i + "th frame....");
				}
				// 1) grab frames
				ImageStack frames = test_getFrames(mmf,i,i+dt);
				// 2) calculate whole frame ddtIm
				ImageProcessor ddtFrame = test_calcTimeDeriv(frames,dt);
				// 3) grab points and crop accordingly
				Rectangle newRect = test_getNewRect(tr,i,i+dt,2);
				ddtFrame.setRoi(newRect);
				ImageProcessor ddtPoint = ddtFrame.crop();
				ddtPoint.autoThreshold(); //TODO autoThreshold() gives a different threshold value for each frame; need to do it after building the ddt stack with a global threshold value
				// 4) padAndCenter
				//    for now, set fixed size 30x30 (definitely larger than point)
				int centerX = ddtPoint.getWidth()/2;
				int centerY = ddtPoint.getHeight()/2;
				ImageProcessor newIm = CVUtils.padAndCenter(new ImagePlus("",ddtPoint), 30, 30, centerX, centerY);
				ret.addSlice(newIm);
			}
			break;
		case 2: //backward
			System.out.println("Derive method: backward");
			break;
		case 3: //central
			System.out.println("Derive method: central");
			dt = increment*2;
			break;
		}
		
		System.out.println("Done!");
		
		// visualization
		ImagePlus retPlus = test_zoom(new ImagePlus("",ret),10);
		retPlus.show();
		
		// compare with raw movie
		// TODO current method doesn't use getCombinedBounds(), so not really comparable to ddtIm movie
		tr.playBlankMovie();
	}
	
	/**
	 * Obtains frame size images from .mmf, calculate ddt image and crop according to points selected
	 * @param tr
	 * @param first
	 * @param second
	 * @return point size stack of first image, second image and ddt image
	 */
	public static ImageStack test_getImsMmf(Track tr, int first, int second) {
		// load mmf
		String mmfPath = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(mmfPath);
		ImageStack mmf = mr.getMmfStack();
		
		/**
		// below: replaced with test_getFrames()
		// grab frames
		// 1) using my own code
		ImageProcessor frameIm1 = mmf.getProcessor(first);
		ImageProcessor frameIm2 = mmf.getProcessor(second);
		// 2) using FrameLoader
		Communicator comm = new Communicator();
		FrameLoader fl = new FrameLoader(comm, mmf);
		int fl1 = fl.getFrame(first);
		if(fl1==0) {
			System.out.println("Loading first frame: success");
		} else {
			System.out.println("Error loading first frame");
		}
		ImageProcessor frameIm1 = fl.returnIm;
		int fl2 = fl.getFrame(second);
		if(fl2==0) {
			System.out.println("Loading second frame: success");
		} else {
			System.out.println("Error loading second frame");
		}
		ImageProcessor frameIm2 = fl.returnIm;
				
		// prepare stack for test_calcTimeDeriv()
		int width = frameIm1.getWidth();
		int height = frameIm1.getHeight();
		ImageStack frames = new ImageStack(width,height);
		frames.addSlice(frameIm1);
		frames.addSlice(frameIm2);
		*/
		
		ImageStack frames = test_getFrames(mmf,first,second);
		
		// generate whole frame ddtIm
		int dt = second-first;
		ImageProcessor ddtIm = test_calcTimeDeriv(frames,dt);
		
		/**
		// below: replaced with getNewRect()
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		System.out.println("Offset: (" + newRect.x + "," + newRect.y + ")");
		System.out.println("Dimension: " + newRect.width + "x" + newRect.height);
		System.out.println("Growing rectangle...");
		// expand newRect a little bit to include more edge info
		newRect.grow(3,3);
		System.out.println("New offset: (" + newRect.x + "," + newRect.y + ")");
		System.out.println("New dimension: " + newRect.width + "x" + newRect.height);
		*/
		
		Rectangle newRect = test_getNewRect(tr,first,second,2);
		
		// crop to get point size ddtIm
		/**
		frameIm1.setRoi(newRect);
		ImageProcessor ret1 = frameIm1.crop();
		frameIm2.setRoi(newRect);
		ImageProcessor ret2 = frameIm2.crop();
		*/
		ImageProcessor tmp1 = frames.getProcessor(1);
		tmp1.setRoi(newRect);
		ImageProcessor ret1 = tmp1.crop();
		ImageProcessor tmp2 = frames.getProcessor(2);
		tmp2.setRoi(newRect);
		ImageProcessor ret2 = tmp2.crop();
		ddtIm.setRoi(newRect);
		ImageProcessor ret3 = ddtIm.crop();
		
		/**
		// test: visualization
		ImagePlus plus1 = new ImagePlus("first point",ret1);
		plus1.show();
		ImagePlus plus2 = new ImagePlus("second point",ret2);
		plus2.show();
		ImagePlus plus3 = new ImagePlus("ddt image",ret3);
		plus3.show();
		// ok now
		*/
		
		// prepare return stack
		ImageStack ret = new ImageStack(newRect.width, newRect.height);
		ret.addSlice(ret1.convertToColorProcessor());
		//ret.addSlice(ret1);
		ret.addSlice(ret2.convertToColorProcessor());
		//ret.addSlice(ret2);
		ret.addSlice(ret3);
		return ret;
	}
	
	/**
	 * Obtain point size images from .prejav and calculate ddt image
	 * <p>
	 * (Obsolete; see {@link test_compare})
	 * @param tr
	 * @param first
	 * @param second
	 * @return point size stack of first image, second image and ddt image
	 */
	public static ImageStack test_getImsPrejav(Track tr, int first, int second) {
		
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
		
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);
		
		// pad point images
		ImageProcessor padded1 = test_padImage(pt1,newRect);
		ImageProcessor padded2 = test_padImage(pt2,newRect);
		ImageStack points = new ImageStack(newRect.width,newRect.height);
		points.addSlice(padded1);
		points.addSlice(padded2);
		
		// generate point size ddtIm
		int dt = second-first;
		ImageProcessor ret3 = test_calcTimeDeriv(points,dt);
		
		// prepare return stack
		ImageStack ret = new ImageStack(newRect.width, newRect.height);
		ret.addSlice(padded1.convertToColorProcessor());
		ret.addSlice(padded2.convertToColorProcessor());
		ret.addSlice(ret3);
		return ret;
	}
	
	/**
	 * Pads a point to a larger roi by adding black pixels
	 * <p>
	 * (Obsolete; only used in {@link test_getImsPrejav} which is itself obsolete)
	 * @param pt
	 * @param newRect
	 * @return padded image
	 */
	public static ImageProcessor test_padImage(TrackPoint pt, Rectangle newRect) {
		// following CVUtils.padAndCenter()
		ImagePlus im = new ImagePlus("original", pt.getRawIm());
		//System.out.println("pointIm (before padding) bit depth: " + im.getBitDepth());
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
		//System.out.println("pointIm (after padding) bit depth: " + retIm.getBitDepth());
		return retIm;
	}
	
	public static ImageStack test_getFrames(ImageStack mmf, int first, int second) {
		///**
		Communicator comm = new Communicator();
		FrameLoader fl = new FrameLoader(comm, mmf);
		//int width = mmf.getWidth();
		//int height = mmf.getHeight();
		
		int fl1 = fl.getFrame(first);
		ImageProcessor frameIm1 = fl.returnIm;
		int fl2 = fl.getFrame(second);
		ImageProcessor frameIm2 = fl.returnIm;
		int width = frameIm1.getWidth();
		int height = frameIm1.getHeight();
		//*/
		
		/**
		ImageProcessor frameIm1 = mmf.getProcessor(first);
		ImageProcessor frameIm2 = mmf.getProcessor(second);
		int width = frameIm1.getWidth();
		int height = frameIm1.getHeight();
		*/
		
		/**
		//test: visualization
		ImagePlus plus1 = new ImagePlus("first frame", frameIm1);
		plus1.show();
		ImagePlus plus2 = new ImagePlus("second frame", frameIm2);
		plus2.show();
		*/
		
		ImageStack ret = new ImageStack(width,height);
		ret.addSlice(frameIm1);
		ret.addSlice(frameIm2);
		
		return ret;
	}

	/**
	 * Given 2 gray scale images of the same dimension, compute the time derivative (ddt) image between them
	 * <p>
	 * This method always return the ddt image in RGB mode; any tweaking of visualization is done outside this method.
	 * @param theseIm 2-image stack
	 * @param dt time difference between the two images
	 * @return ddt image where positive diff is represented in red, negative in blue
	 */
	public static ImageProcessor test_calcTimeDeriv(ImageStack theseIm, int dt) {
		// assume 8-bit gray scale
		// Q: do I still need to deal with threshold?	
		
		// load images
		ImageProcessor im1 = theseIm.getProcessor(1);
		ImageProcessor im2 = theseIm.getProcessor(2);
		
		// prepare images for calculation
		/**
		// problem: ImageProcessor true types don't match
		// use getBitDepth() and check processor type
		System.out.println("First image bit depth: " + im1.getBitDepth());
		System.out.println("Second image bit depth: " + im2.getBitDepth());
		// temp fix: hard set these points to be ByteProcessors
		im1 = im1.convertToByteProcessor();
		im2 = im2.convertToByteProcessor();
		// problem solved: just be more careful about true types outside this method
		*/
		int width = theseIm.getWidth();
		int height = theseIm.getHeight();
	
		// prepare empty result image
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
		
		return ddtIm;
	}
	
	public static Rectangle test_getNewRect(Track tr, int first, int second, int edge) {
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
			
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);

		// expand newRect a little bit to include more edge info
		newRect.grow(edge,edge);
		
		return newRect;
	}

	public static ImagePlus test_zoom(ImagePlus original, double zoomFactor) {
		// get original dimension
		int width = original.getWidth();
		int height = original.getHeight();
		int newWidth = (int)(width*zoomFactor);
		int newHeight = (int)(height*zoomFactor);
		
		// prepare return image(s)
		ImagePlus ret = new ImagePlus();
		
		// determine whether it's one image or stack
		int size = original.getImageStackSize();
		if(size==1) { //single image
			ImageProcessor im = original.getProcessor();
			ret.setProcessor(im.resize(newWidth));
		} else {
			ImageStack ims = original.getStack();
			ImageStack retStack = new ImageStack(newWidth,newHeight);
			for(int i=1;i<=size;i++) {
				retStack.addSlice(ims.getProcessor(i).resize(newWidth));
			}
			ret.setStack(retStack);
		}
		
		return ret;
	}

}
