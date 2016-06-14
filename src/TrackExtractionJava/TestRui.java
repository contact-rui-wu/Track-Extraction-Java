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
		
		test_compare();
		
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
		ImagePlus mmfPlus = new ImagePlus("from MMF", test_zoom(mmf));
		mmfPlus.getStack().getProcessor(3).autoThreshold(); //easier to see
		mmfPlus.show();
		
		/**
		// get point size Ims and ddtIm from prejav
		System.out.println("Calculating derivative image from .prejav");
		ImageStack prejav = test_getImsPrejav(tr,first,second);
		// visualization
		ImagePlus prejavPlus = new ImagePlus("from prejav", test_zoom(prejav));
		prejavPlus.show();
		*/
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
		
		// grab frames and prepare stack for test_calcTimeDeriv()
		// 1) using my own code
		ImageProcessor frameIm1 = mmf.getProcessor(first);
		ImageProcessor frameIm2 = mmf.getProcessor(second);
		int width = frameIm1.getWidth();
		int height = frameIm1.getHeight();
		ImageStack frames = new ImageStack(width,height);
		frames.addSlice(frameIm1);
		frames.addSlice(frameIm2);
		// 2) TODO using FrameLoader
		
		// generate whole frame ddtIm
		int dt = second-first;
		ImageProcessor ddtIm = test_calcTimeDeriv(frames,dt);
		
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

}
