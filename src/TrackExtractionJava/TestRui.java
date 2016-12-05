/**
 * Sandbox for the Bermanator
 */

package TrackExtractionJava;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Vector;

import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import edu.nyu.physics.gershowlab.mmf.mmf_Reader;

public class TestRui {

	public static void main(String[] args) {
		// put the testing methods here
		// uncomment when a test is ready to run
		
		test_getRawPaddedMovie();
		
		//test_frameVSPointDdtScheme();
		
		//test_frameSizeDdtMovie();
		
		//test_vectorSizeAndCapacity(4);
		
		//test_extraction(0); //0 - rect MMF; 1 - square MMF
		
		//test_loadPrejav();
		
		//test_loadJav();
		
		//test_viewSampleExp(2); //rect MMF, 0 - Natalie's sample (doesn't work anymore because no secondary fields); 1 - prejav; 2 - jav
		
	}
	
	// write each test as a void method so that don't have to write a lot in main
	
	/**
	 * For dft image registration tests in MATLAB
	 */
	public static void test_getRawPaddedMovie() {
		ImageJ ij = new ImageJ();
		// do extraction and look at the tracks
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = true;
		prParams.saveMagEx = false;
		prParams.doFitting = false;
		prParams.showFitEx = false;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true; // note: deprecated in master branch
		extrParams.startFrame = 1;
		extrParams.endFrame = 9000;
		extrParams.frameSizeDdt = false;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/pipeline/Java/sampleShortExp_copy.mmf";
		Experiment_Processor ep = new Experiment_Processor();
		ep.runningFromMain = true;
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.run(path);
		/*
		// full length track chosen: 0
		Track tr = ep.ex.getTrack(0);
		System.out.println("Chosen track length: "+tr.getNumPoints());
		int width = ep.extrParams.trackWindowWidth;
		int height = ep.extrParams.trackWindowHeight;
		// get raw movie
		ImageStack rawMovie = new ImageStack(width,height);
		for (int i=1;i<=tr.getNumPoints();i++) {
			ImageProcessor ip = tr.getFramePoint(i).getRawIm();
			Rectangle rect = tr.getFramePoint(i).rect;
			ImageProcessor ipad = CVUtils.padAndCenter(new ImagePlus("",ip), width, height, rect.width/2, rect.height/2);
			rawMovie.addSlice(ipad);
		}
		ImagePlus rawMoviePlus = new ImagePlus("raw movie", rawMovie);
		rawMoviePlus.show();
		// get ddt movie
		ImageStack ddtMovie = new ImageStack(width,height);
		for (int i=1;i<=tr.getNumPoints();i++) {
			TrackPoint tp = tr.getFramePoint(i);
			ImageProcessor ipad;
			if (tp.is2ndValid(0)) {
				ImageProcessor ip = tr.getFramePoint(i).get2ndIm(0);
				Rectangle rect = tr.getFramePoint(i).get2ndRect(0);
				ipad = CVUtils.padAndCenter(new ImagePlus("",ip), width, height, rect.width/2, rect.height/2);
			} else {
				ipad = new ColorProcessor(width,height);
				ipad.setColor(new Color(0,0,0));
				ipad.fill();
			}
			ddtMovie.addSlice(ipad);
		}
		ImagePlus ddtMoviePlus = new ImagePlus("ddt movie", ddtMovie);
		ddtMoviePlus.show();
		*/
	}
	
	public static void test_frameVSPointDdtScheme() {
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/ddt_calculation/sampleShortExp/";
		String mmfname = "sampleShortExp_copy.mmf";
		TicToc timer = new TicToc();
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = true;
		prParams.saveMagEx = false;
		prParams.doFitting = false;
		prParams.showFitEx = false;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		//prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		extrParams.frameSizeDdt = false;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		ep.runningFromMain = true;
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		// first run frame size scheme (default in extrParams)
		timer.tic();
		ep.run(dir+mmfname);
		System.out.println("Runtime: "+timer.tocSec()+" s");
	}
	
	@SuppressWarnings("unused")
	public static void test_frameSizeDdtMovie() {
		ImageJ ij = new ImageJ();
		// load mmf and get ready for point extractor
		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleMMF_copy.mmf";
		mmf_Reader mr = new mmf_Reader();
		mr.loadStack(path);
		ImageStack mmf = mr.getMmfStack();
		Communicator comm = new Communicator();
		comm.setVerbosity(VerbLevel.verb_warning);
		ExtractionParameters ep = new ExtractionParameters();
		ep.subset=true;
		ep.startFrame=1;
		ep.endFrame=80;
		ep.derivMethod=2;
		
		PointExtractor pe = new PointExtractor(mmf,comm,ep);
		
		int width = mmf.getProcessor(ep.startFrame).getWidth();
		int height = mmf.getProcessor(ep.startFrame).getHeight();
		ImageStack ddtStack = new ImageStack(width,height);
		for (int i=ep.startFrame;i<=ep.endFrame;i++) {
			if (i%10==0) {
				System.out.println("Working on "+i+"th frame");
			}
			if (pe.loadFrameNew(i)!=0) {
				System.out.println("Failed to load frame "+i);
				break;
			} else {
				if (pe.ddtIm!=null) {
					//ImageProcessor newIP = pe.ddtIm.getProcessor();
					//newIP.autoThreshold();
					//ddtStack.addSlice(newIP);
					ddtStack.addSlice(pe.ddtIm.getProcessor());
				} else continue; // skip bad ddtIm
			}
		}
		
		System.out.println(pe.comm.toString());
		
		ImagePlus ddtPlus = new ImagePlus(null,ddtStack);
		//ddtPlus.show();
	}
	
	public static void test_vectorSizeAndCapacity(int scenario) {
		Vector<Integer> ints;
		Vector<Boolean> validity;
		switch (scenario) {
		case 1: //no space, nothing is stored
			ints = new Vector<Integer>(0,1);
			validity = new Vector<Boolean>(0,1);
			break;
		case 2: //3 spaces, nothing is stored
			ints = new Vector<Integer>(3,1);
			validity = new Vector<Boolean>(3,1);
			break;
		case 3: //3 spaces, middle is invalid
			ints = new Vector<Integer>(0,1);
			ints.setSize(3);
			ints.set(0, 10);
			ints.set(2, 20);
			validity = new Vector<Boolean>(0,1);
			validity.setSize(3);
			validity.set(0, true);
			validity.set(1, false);
			validity.set(2, true);
			// now I want to add another one
			ints.setSize(4);
			validity.setSize(4);
			validity.set(3, true);
			ints.set(3, 15);
			break;
		case 4: //what does the data structure look like when size<capacity?
			ints = new Vector<Integer>(3,1);
			ints.setSize(2);
			validity = new Vector<Boolean>(3,1);
			break;
		default:
			ints = new Vector<Integer>();
			validity = new Vector<Boolean>();
		}
		System.out.println("validity: capacity="+validity.capacity()+", size="+validity.size());
		System.out.println("ints: capacity="+ints.capacity()+", size="+ints.size());
		if (validity.size()==0) System.out.println("Vectors not initialized!");
		try {
			for (int i=0; i<validity.size(); i++) {
				if (validity.get(i)) {
					System.out.println(ints.get(i));
				} else {
					System.out.println("invalid");
				}
			}
		} catch (Exception e) {
			System.out.println("Failed to get ints values!");
		}
	}
	
	@SuppressWarnings("unused")
	public static void test_loadPrejav() {
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/";
		String fName = "sampleMMF_copy.prejav";
		Experiment ex = new Experiment(dir+fName);
		ExperimentFrame exFrame = new ExperimentFrame(ex);
		exFrame.run(null);
	}
	
	@SuppressWarnings("unused")
	public static void test_loadJav() {
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/";
		String fName = "sampleMMF_copy.jav";
		Experiment ex = new Experiment(dir+fName);
		//ex.getEP().trackWindowWidth = 35;
		//ex.getEP().trackWindowHeight = 35;
		// TODO make ExperimentFrame display movies in any window size we want
		// alternatively, save ExtractionParameter with the experiment and always draw from that
		ExperimentFrame exFrame = new ExperimentFrame(ex);
		exFrame.run(null);
	}
	
	@SuppressWarnings("unused")
	public static void test_viewSampleExp(int whose) {
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/";
		String fnameN = "Berlin@Berlin_2NDs_B_Square_SW_96-160_201411201541.prejav";
		String fnameR1 = "sampleShortExp_copy.prejav";
		String fnameR2 = "sampleShortExp_copy.jav";
		Experiment_Viewer ev = new Experiment_Viewer();
		switch(whose) {
		case 0: //Natalie's
			ev.run(dir+fnameN);
			break;
		case 1: //mine (w/o fitting)
			ev.run(dir+fnameR1);
			break;
		case 2: //mine (with fitting)
			ev.run(dir+fnameR2);
			break;
		case 3: //manually choose file
			ev.run("");
			break;
		}
	}
	
	@SuppressWarnings("unused")
	public static void test_extraction(int whose) {
		ImageJ ij = new ImageJ();
		TicToc timer = new TicToc();
		
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = false;
		prParams.saveMagEx = false;
		prParams.doFitting = true;
		prParams.showFitEx = true;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		//prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		
		String path;
		switch (whose) {
		case 0:
			path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
			break;
		case 1:
			path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleMMF_copy.mmf";
			break;
		default:
			path = "";
			break;
		}
		Experiment_Processor ep = new Experiment_Processor();
		
		ep.runningFromMain = true;
		
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		
		timer.tic();
		ep.run(path);
		System.out.println("Runtime: "+timer.toc()/1000+" seconds");
	}
	
	/**
	 * Grabs any two frame from an image stack
	 * @param mmf Image stack to grab from
	 * @param first First frame number to grab
	 * @param second Second frame number to grab
	 * @return A 2-frame image stack
	 */
	@SuppressWarnings("unused")
	public static ImageStack draft_fetchFrames(ImageStack mmf, int frame, int increment, int derivMethod) {
		// handle derivMethod
		int first = frame;
		int second = frame;
		switch(derivMethod) {
		case 1:
			second = frame+increment;
			break;
		case 2:
			first = frame-increment;
			break;
		case 3:
			first = frame-increment;
			second = frame+increment;
			break;
		}
		
		///**
		// using FrameLoader
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
		
		// handle different thresholds
		
		/**
		// using ImageJ methods
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
	 * Calculates the time derivative (ddt) image between two gray scale images of the same dimension
	 * <p>
	 * This method always return the ddt image in RGB mode; any tweaking of visualization is done outside this method.
	 * @param theseIm 2-image stack
	 * @param dt time difference between the two images
	 * @return ddt image where positive diff is represented in red, negative in blue
	 */
	public static ImageProcessor draft_calcDdtIm(ImageStack theseIm, int dt) {
		// assume 8-bit gray scale, same threshold info
		
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
	
	public static Rectangle draft_calcNewRoi(Track tr, int frame, int increment, int derivMethod, int edge) {
		// handle derivMethod
		int first = frame;
		int second = frame;
		switch(derivMethod) {
		case 1:
			second = frame+increment;
			break;
		case 2:
			first = frame-increment;
			break;
		case 3:
			first = frame-increment;
			second = frame+increment;
			break;
		}
		
		// grab points
		TrackPoint pt1 = tr.getFramePoint(first);
		TrackPoint pt2 = tr.getFramePoint(second);
			
		// get new roi from points
		Rectangle newRect = pt1.getCombinedBounds(pt1, pt2);

		// expand newRect a little bit to include more edge info
		newRect.grow(edge,edge);
		
		return newRect;
	}

}
