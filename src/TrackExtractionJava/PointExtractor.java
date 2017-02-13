package TrackExtractionJava;

import ij.*;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.util.Vector;


/**
 * Extracts TrakPoints from an image
 * @author Natalie
 *
 */
public class PointExtractor {

	/**
	 * The stack of images to extract points from 
	 */
//	ImageStack imageStack;
	/**
	 * Parameters used for extracting points
	 */
	ExtractionParameters ep;
	
	/**
	 * Message handler
	 */
	Communicator comm;
	/**
	 * Image Process calculator
	 * <p>
	 * See IJ_Props.txt for calculator function strings 
	 */
//	ImageCalculator IC;
	
	/**
	 * The points which were extracted in the last call to extractFrame() 
	 * <p>
	 * Only publicly accessible via getPoints()
	 */
	private Vector<TrackPoint> extractedPoints;
	/**
	 * Index of the last frame that was run through the point extractor
	 */
	int lastFrameExtracted;
	/**
	 * How many frames the extractor moves forward when nextFrame() is called
	 */
	int increment;
	/**
	 * First frame to be extracted
	 */
	int startFrameNum;
	/**
	 * Index of the last frame in the stack 
	 */
	int endFrameNum;
	/**
	 * The frame being processed
	 */
	int currentFrameNum;
	
	/**
	 * Whether or not the frame being processed is the first one
	 */
	boolean isFirstRun;
	
	
	/**
	 * Object that retrieves frame from imagestack
	 */
	FrameLoader fl;
	/**
	 * Normalization type (See class below)
	 */
	_frame_normalization_methodT fnm;
	/**
	 * Normalization Factor
	 */
	double normFactor;
	
	

	/**
	 * The current region of analysis
	 */
	Rectangle analysisRect;
	/**
	 * The image being processed
	 */
	ImagePlus currentIm;
	/**
	 * Thresholded current image
	 */
	ImagePlus threshIm;
	/**
	 * Previous frame image used for ddt calculation
	 */
	ImagePlus prevIm;
	/**
	 * Next frame image used for ddt calculation
	 */
	ImagePlus nextIm;
	
	/**
	 * The resultsTable made by finding particles in the currentIm
	 * <p>
	 * Includes points out of the size range 
	 */
//	private ResultsTable pointTable;
	
	/**
	 * background image
	 */
//	ImagePlus backgroundIm;
	

	/**
	 * Foreground image
	 */
//	ImagePlus foregroundIm;
	

	/**
	 * Threshold comparison Image, used for non-global thresholding
	 */
//	ImagePlus threshCompIm;

	/**
	 * Index of the last frame where the background image is valid (and doesn't need to be reloaded)
	 */
	int backValidUntil;
	
	
	////////////////////////////
	// Point Extracting methods 
	////////////////////////////
	
	/**
	 * Constructs a Point Extractor for a given stack
	 * @param stack
	 * @param comm
	 * @param ep
	 */
	public PointExtractor(ImageStack stack, Communicator comm, ExtractionParameters ep){
		init(ep.startFrame, stack, comm, ep);
	}
	
	/**
	 * Machinery for constructing a PointExtractor
	 * @param startFrame
	 * @param stack
	 * @param comm
	 * @param ep
	 */
	private void init(int startFrame, ImageStack stack, Communicator comm, ExtractionParameters ep){
		startFrame = startFrame < 1 ? 1 : startFrame; //imagej stacks start at 1
		this.startFrameNum = startFrame;
//		imageStack = stack;
		this.comm = comm;
		this.ep = ep;
		endFrameNum = stack.getSize(); //imagej stacks end at num frames
		if (ep.endFrame > endFrameNum) {
			ep.endFrame = endFrameNum;
		}
//		endFrameNum = imageStack.getSize()-1;//.getNFrames()-1;// 
		increment = ep.increment;
		lastFrameExtracted=-1;
		isFirstRun=true;
		fl = new FrameLoader(comm, stack);
//		IC = new ImageCalculator();
		fnm = fl.fnm;
	}
	
	/**
	 * Calculates the index of the next frame
	 * @return Index of the frame following the last frame loaded
	 */
	public int nextFrameNum(){
		if (lastFrameExtracted == -1){
			return startFrameNum;
		} else {
			return lastFrameExtracted+increment;
		}
	}

	
	/**
	 * 
	 * @param frameNum Index of the stack slice to load
	 * @return
	 */
	public int extractFramePoints(int frameNum) {
		
		if(loadFrameNew(frameNum)>0){
			comm.message("Frame "+frameNum+" was NOT successfully loaded in Point Extractor", VerbLevel.verb_debug);
			return 1; 
		}
		comm.message("Frame "+frameNum+" successfully loaded in Point Extractor", VerbLevel.verb_debug);
		extractPoints(frameNum);
		lastFrameExtracted = frameNum;
		return 0;
	}
	
	/**
	 * Loads the frame into the currentIm, backSubIm, ForegroundIm, and threshIm images, and loads the proper backgroundIm; if doing ddt calculation, also loads prevIm and nextIm
	 * @param frameNum Index of the frame to be loaded
	 * @return status, 0 means all is good, otherwise error
	 */
	public int loadFrameNew(int frameNum) {
		
		if (ep.doDdt) { // with ddt calculation
			// set currentFrameNum, make sure it's not out of range
			currentFrameNum = frameNum;
			if (currentFrameNum>endFrameNum) {
				comm.message("Attempt to load frame "+currentFrameNum+" but the stack ends at frame "+endFrameNum,VerbLevel.verb_error);
				return 1;
			}
			// note: endFrameNum is set by extr params
			
			// load current/prev/nextIm
			if (fl.lastFrameLoaded==frameNum && currentIm!=null && nextIm!=null) { // if only need to reset pointers
				comm.message("Frame "+frameNum+" already in FrameLoader as old nextIm, resetting pointers for prevIm and currentIm only", VerbLevel.verb_debug);
				prevIm = (ImagePlus)currentIm.clone();
				currentIm = (ImagePlus)nextIm.clone();
				// load new nextIm
				if (fl.getFrame(frameNum+increment,fnm,normFactor)!=0) {
					comm.message("FrameLoader returned error when loading nextIm for frame "+frameNum,VerbLevel.verb_debug);
					nextIm = null;
				} else {
					nextIm = new ImagePlus(null,fl.returnIm);
				}
			} else { // if need to load all 3 new images
				// load currentIm
				if (fl.getFrame(frameNum,fnm,normFactor)!=0) {
					comm.message("FrameLoader returned error when loading currentIm for frame "+frameNum,VerbLevel.verb_error);
					return 2;
				} else {
					currentIm = new ImagePlus(null,fl.returnIm);
				}
				// load prevIm
				if (fl.getFrame(frameNum-increment,fnm,normFactor)!=0) {
					comm.message("FrameLoader returned error when loading prevIm for frame "+frameNum,VerbLevel.verb_debug);
					prevIm = null;
				} else {
					prevIm = new ImagePlus(null,fl.returnIm);
				}
				// load nextIm
				if (fl.getFrame(frameNum+increment,fnm,normFactor)!=0) {
					comm.message("FrameLoader returned error when loading nextIm for frame "+frameNum,VerbLevel.verb_debug);
					nextIm = null;
				} else {
					nextIm = new ImagePlus(null,fl.returnIm);
				}
			}
			
			// make sure currentIm exists and do threshold
			assert (currentIm.getImage()!=null); // see if this works
			comm.message("Thresholding image to zero...", VerbLevel.verb_debug);
			defaultThresh();
			comm.message("...finished thresholding image to zero", VerbLevel.verb_debug);
			
			// check if prev/nextIm exist
			if (prevIm==null && nextIm==null) {
				comm.message("Error in ddt calculation: neither prevIm nor nextIm is loaded for frame "+frameNum, VerbLevel.verb_error);
				return 3;
			} else if (prevIm==null && nextIm!=null) {
				comm.message("prevIm for frame "+frameNum+" not available, calculating ddt using forward method", VerbLevel.verb_verbose);
			} else if (prevIm!=null && nextIm==null) {
				comm.message("nextIm for frame "+frameNum+" not available, calculating ddt using backward method", VerbLevel.verb_verbose);				
			}
			
			return 0;
		} else { // no ddt calculation
			return loadFrame(frameNum);
		}
	}
	
	/**
	 * Loads the frame into the currentIm, backSubIm, ForegroundIm, and threshIm images, and loads the proper backgroundIm 
	 * @param frameNum Index of the frame to be loaded
	 * @return status, 0 means all is good, otherwise error
	 */
	public int loadFrame(int frameNum){
		
		if (frameNum==currentFrameNum && (analysisRect==null ||(fl.returnIm.getWidth()==analysisRect.getWidth() && fl.returnIm.getHeight()==analysisRect.getHeight()))){
			//if (comm!=null) comm.message("Frame already loaded in Frameloader", VerbLevel.verb_verbose);
			return 0;
		}
		
		//Set the current frame
		currentFrameNum = frameNum;
		if (currentFrameNum>endFrameNum) {
			comm.message("Attempt to load frame "+currentFrameNum+" but the stack ends at frame "+endFrameNum, VerbLevel.verb_error);
			return 1;
		}
		
		//Calculate the background image
//		calculateBackground();
		
		//Set the current image and analysis region
		if (fl.getFrame(frameNum, fnm, normFactor)!=0) {
			comm.message("Frame Loader returned error", VerbLevel.verb_warning);
			return 2;
		} else {
			if (comm!=null) comm.message("No error from frame loader when loading frame "+frameNum+" in pe.loadFrame", VerbLevel.verb_debug);
			//currentIm = null; // trying to make sure GC gets the old im; didn't work this way
			currentIm = new ImagePlus("Frame "+frameNum,fl.returnIm);
		}
		assert (currentIm!=null);
//		analysisRect = fl.ar;
				
		if (comm!=null) comm.message("Thresholding image to zero...", VerbLevel.verb_debug);
		defaultThresh();
		if (comm!=null) comm.message("...finished thresholding image to zero", VerbLevel.verb_debug);
		
		return 0;
	}
	
	

	/**
	 * Thresholds backSubIm according to the method set in the extraction parameters, and stores the results in threshIm  
	 */
	void defaultThresh() {
		
		threshIm = new ImagePlus("Thresh im Frame "+currentFrameNum, currentIm.getProcessor().getBufferedImage());
		threshIm.getProcessor().setThreshold((double) ep.globalThreshValue, (double) 255, ImageProcessor.NO_LUT_UPDATE);
		threshIm.getProcessor().threshold((int) ep.globalThreshValue);

	}
	
	void rethresh(int thresh){
		if (comm!=null) comm.message("Rethresholding to "+thresh, VerbLevel.verb_debug);
		threshIm = new ImagePlus("Thresh im Frame "+currentFrameNum, currentIm.getProcessor().getBufferedImage());
		threshIm.getProcessor().setThreshold((double) thresh, (double) 255, ImageProcessor.NO_LUT_UPDATE);
		threshIm.getProcessor().threshold(thresh);
	}
	
	public void extractPoints(int frameNum) {
		extractPoints(frameNum, (int)ep.globalThreshValue);
	}
	
	/**
	 * Extracts points from the specified frame, storing them in extractedPoints
	 * @param frameNum
	 */
	public void extractPoints(int frameNum, int thresh) {
		
//		if (currentFrameNum!= frameNum){
			loadFrameNew(frameNum);
//		}
		if (thresh!=ep.globalThreshValue){
			if (comm!=null) comm.message("Rethresholding to "+thresh, VerbLevel.verb_message);
			rethresh(thresh);
		}
		
		if (comm!=null) comm.message("extract points called", VerbLevel.verb_debug);
	    
	    boolean showResults =  ep.showSampleData>=2 && ep.sampleInd==frameNum;
	    if (showResults) {
	    	threshIm.show();
	    }

	    extractedPoints = findPtsInIm(currentFrameNum, currentIm, threshIm, thresh, fl.getStackDims(), analysisRect, ep, showResults, comm);
	    
	    String s = "Frame "+currentFrameNum+": Extracted "+extractedPoints.size()+" new points";
	    if (comm!=null) comm.message(s, VerbLevel.verb_message);
	    
	    // set ddtIm for all points extracted in this frame
	    if (ep.doDdt) {
	    	setDdtIm4Pts();
	    }
	    
	}
	
	protected static Vector<TrackPoint> findPtsInIm(int frameNum, ImagePlus currentIm, ImagePlus threshIm, int thresh, int[] frameSize, Rectangle analysisRect, ExtractionParameters ep, boolean showResults,  Communicator comm){
		
//		boolean excl = ep.excludeEdges;
//		ep.excludeEdges = false;
		if (comm!=null && analysisRect!=null) comm.message("Analysis Rect: ("+analysisRect.x+","+analysisRect.y+"), "+analysisRect.width+"x"+analysisRect.height, VerbLevel.verb_message);
		ResultsTable pointTable = CVUtils.findPoints(threshIm, thresh, analysisRect, ep, (int)ep.minArea, (int)ep.maxArea, showResults);
//		ep.excludeEdges = excl;
		
//		if (showResults) {
		if (comm!=null) comm.message("Frame "+frameNum+": "+pointTable.getCounter()+" points in ResultsTable", VerbLevel.verb_verbose);
//	    }

			//ResultsTable rt, int frameNum, Rectangle analysisRect, int[] frameSize, ImagePlus currentIm, ImagePlus threshIm, ExtractionParameters ep, int thresh, Communicator comm
		Vector<TrackPoint> pts = rt2TrackPoints(pointTable, frameNum, analysisRect, frameSize, currentIm, threshIm, ep, thresh, comm);
		return pts;
	}
	

	/**
	 * Adds a row from the results table to the list of TrackPoints, if the point is the proper size according to the extraction parameters
	 * @param rt Results Table containing point info 
	 * @param frameNum Frame number
	 * @return List of Trackpoints within the 
	 */
	
	/**
	 * This is ridiculous. Get rid of some of these isoforms
	 */
//	public Vector<TrackPoint> rt2TrackPoints (ResultsTable rt, int frameNum, int thresh) {
//		return rt2TrackPoints (rt, frameNum, analysisRect, ep, thresh, comm);
//	}
	
	
	public Vector<TrackPoint> rt2TrackPoints (ResultsTable rt, int frameNum, Rectangle analysisRect, ExtractionParameters ep, int thresh, Communicator comm) {
		return rt2TrackPoints (rt, frameNum, analysisRect, fl.getStackDims(), currentIm, threshIm, ep, thresh, comm);
	}
	
	public static Vector<TrackPoint> rt2TrackPoints (ResultsTable rt, int frameNum, Rectangle analysisRect, int[] frameSize, ImagePlus currentIm, ImagePlus threshIm, ExtractionParameters ep, int thresh, Communicator comm) {
		
		int arX=0;
		int arY=0;
		
		if (analysisRect!=null){
			arX=analysisRect.x;
			arY=analysisRect.y;
		}
		
		
		Vector<TrackPoint> tp = new Vector<TrackPoint>();
		
		for (int row=0; row<rt.getCounter(); row++) {
			
			if (comm!=null) comm.message("Gathering info for Point "+row+" from ResultsTable", VerbLevel.verb_debug);
			double area = rt.getValueAsDouble(ResultsTable.AREA, row);
			if (comm!=null) comm.message("Point "+row+": area="+area, VerbLevel.verb_debug);
			double x = rt.getValueAsDouble(ResultsTable.X_CENTROID, row)+arX;
			double y = rt.getValueAsDouble(ResultsTable.Y_CENTROID, row)+arY;
			double width = rt.getValueAsDouble(ResultsTable.ROI_WIDTH, row);
			double height = rt.getValueAsDouble(ResultsTable.ROI_HEIGHT, row);
			double boundX = rt.getValueAsDouble(ResultsTable.ROI_X, row)+arX;
			double boundY = rt.getValueAsDouble(ResultsTable.ROI_Y, row)+arY;
			
			boolean inB = inBounds(new Rectangle((int)boundX,(int)boundY,(int)width,(int)height), ep.boundarySize, frameSize[0], frameSize[1]);
			
			if (!ep.clipBoundaries || inB){
				
				Rectangle rect = new Rectangle((int)boundX-ep.roiPadding, (int)boundY-ep.roiPadding, (int)width+2*ep.roiPadding, (int)height+2*ep.roiPadding);
				Rectangle crRect = new Rectangle((int)boundX-arX-ep.roiPadding, (int)boundY-arY-ep.roiPadding, (int)width+2*ep.roiPadding, (int)height+2*ep.roiPadding);
	//			Rectangle rect = new Rectangle((int)boundX, (int)boundY, (int)width, (int)height);
				//Rectangle rect = new Rectangle((int)x-ep.roiPadding, (int)y-ep.roiPadding, (int)2*ep.roiPadding, (int)2*ep.roiPadding);
//				if (crRect.x<0 || crRect.y<0){
//					new TextWindow("Cropping info crRect", "crRect: (x="+crRect.x+", y="+crRect.y+", w="+crRect.width+", h="+crRect.height+")", 500, 500);
//				}
					
				
				if (comm!=null) comm.message("Converting Point "+row+" "+"("+(int)x+","+(int)y+")"+"to TrackPoint", VerbLevel.verb_debug);
				if (ep.properPointSize(area)) {
					
					switch (ep.trackPointType){
						case 1: //ImTrackPoint
							ImTrackPoint iTPt = new ImTrackPoint(x,y,rect,area,frameNum,thresh);
//							if (currentFrameNum!=frameNum){
//								loadFrame(frameNum);
//							}
							Roi oldRoi = currentIm.getRoi();
							currentIm.setRoi(crRect);
							ImageProcessor im = currentIm.getProcessor().crop(); //does not affect currentIm
							currentIm.setRoi(oldRoi);
							iTPt.setImage(im, ep.trackWindowWidth, ep.trackWindowHeight);
							tp.add(iTPt);
							break;
						case 2: //MaggotTrackPoint
							MaggotTrackPoint mtPt = new MaggotTrackPoint(x,y,rect,area,frameNum,thresh);
							mtPt.setCommunicator(comm);
//							if (currentFrameNum!=frameNum){
//								loadFrame(frameNum);
//							}
							mtPt.setStart((int)rt.getValue("XStart", row)+arX, (int)rt.getValue("YStart", row)+arY);
							Roi roi = currentIm.getRoi();
							currentIm.setRoi(crRect);
							ImageProcessor im2 = currentIm.getProcessor().crop(); //does not affect currentIm
							currentIm.setRoi(roi);
							//Set the image mask
							mtPt.setMask(getMask(rt, row, threshIm, ep));
							
//							String debugInfo = "Rect: (x="+mtPt.x+", y="+mtPt.y+")\n";
//							debugInfo+="Bitdepth="+im2.getBitDepth()+"\n";
//							debugInfo+="Type="+im2.getBufferedImage().getType()+"(proper="+BufferedImage.TYPE_BYTE_GRAY+")\n";
//							new TextWindow("New point info", debugInfo, 500, 500);
							
							/*
							new ImagePlus("Current image", currentIm.getProcessor()).show();
							new ImagePlus("New point image", im2).show();
							*/
							
							mtPt.setImage(im2, ep.trackWindowWidth, ep.trackWindowHeight);
							mtPt.extractFeatures();
							tp.add(mtPt);
							break;
						default:
							TrackPoint newPt = new TrackPoint(x,y,rect,area,frameNum,thresh); 
							tp.add(newPt);
							if (comm!=null) comm.message("Point "+row+" has pointID "+newPt.pointID, VerbLevel.verb_debug);
					}
					
				} else{
					if (comm!=null) comm.message("Point was not proper size: not made into a point", VerbLevel.verb_debug);
				}
			} else {
				if (comm!=null) comm.message("Point "+row+" from ResultsTable in frame "+frameNum+" was clipped", VerbLevel.verb_verbose);
			}
			
		}
		
		return tp;
		
	}

	
//	private boolean inBounds(int boundX, int boundY, int width, int height){
//		return inBounds(boundX, boundY, width, height, ep.boundarySize, fl.getStackDims()[0], fl.getStackDims()[1]);
//	}
	
	public static boolean inBounds(Rectangle boxBounds, int boundarySize, int stackW, int stackH){
//		int bs = ep.boundarySize;
//		int sw = fl.getStackDims()[0];//2592;//fl.imageStack.getWidth();
//		int sh = fl.getStackDims()[1];//1944;//fl.imageStack.getHeight();
		return boxBounds.x>boundarySize && boxBounds.y>boundarySize && 
				(boxBounds.x+boxBounds.width)<(stackW-boundarySize) && 
				(boxBounds.y+boxBounds.height)<(stackH-boundarySize);
	}
	
	private static ImageProcessor getMask(ResultsTable rt, int row, ImagePlus threshIm, ExtractionParameters ep){
		
		//Get info from table
		double width = rt.getValueAsDouble(ResultsTable.ROI_WIDTH, row);
		double height = rt.getValueAsDouble(ResultsTable.ROI_HEIGHT, row);
		double boundX = rt.getValueAsDouble(ResultsTable.ROI_X, row);
		double boundY = rt.getValueAsDouble(ResultsTable.ROI_Y, row);
		Rectangle rect = new Rectangle((int)boundX-ep.roiPadding, (int)boundY-ep.roiPadding, (int)width+2*ep.roiPadding, (int)height+2*ep.roiPadding);
		int startX = (int)(rt.getValue("XStart", row)-(boundX-ep.roiPadding));
		int startY = (int)(rt.getValue("YStart", row)-(boundY-ep.roiPadding));
		
		//Get threshIm clip
		Roi roi = threshIm.getRoi();
		threshIm.setRoi(rect);
		ImageProcessor im2 = threshIm.getProcessor().crop(); //does not affect currentIm
		threshIm.setRoi(roi);
		
		//Get masked im IP from im2
		//-->Snippet from http://rsb.info.nih.gov/ij/developer/source/ createMask(ImagePlus imp)
		ImagePlus imp =new ImagePlus("ThreshIm for particle "+row,im2); 
		IJ.doWand(imp, startX, startY, 0, null);//Particle is now in ROI
		ImageProcessor ip = imp.getProcessor();
        ip.setRoi(imp.getRoi());
        ip.setValue(255);
        ip.fill(ip.getMask());

		return ip;
	}
	
	

	
	/**
	 * Returns the extracted points
	 * @return Extracted points
	 */
	public Vector<TrackPoint> getPoints(){
		return extractedPoints;
	}
	
	
	public void setAnalysisRect(Rectangle r){
		if (comm!=null){
			if (analysisRect!=null){
				comm.message("Analysis Rect being reset; prev:("+analysisRect.x+","+analysisRect.y+") w="+analysisRect.width+", h="+analysisRect.height, VerbLevel.verb_debug);
			} else {
				comm.message("Analysis Rect being reset; prev: null", VerbLevel.verb_debug);
			}
		}
		analysisRect = r;
		fl.setAnalysisRect(r);
		if (comm!=null){
			if (analysisRect==null){
				comm.message("Analysis Rect reset; new:null", VerbLevel.verb_debug);
			} else {
				comm.message("Analysis Rect reset; new:("+analysisRect.x+","+analysisRect.y+") w="+analysisRect.width+", h="+analysisRect.height, VerbLevel.verb_debug);
			}
		}
	}
	public Rectangle getAnalysisRect(){
		return analysisRect;
	}
	
	/**
	 * Calculates time derivative between 2 given images
	 * @return ddt image with same dimension as the given images
	 */
	/*
	public ImageProcessor calcDdtIm(ImageProcessor im1, ImageProcessor im2, Integer dt) {
		assert(im1.getWidth()==im2.getWidth() && im1.getHeight()==im2.getHeight());
		Rectangle ddtRect = new Rectangle(0,0,im1.getWidth(),im1.getHeight());
		return calcDdtIm(im1,im2,ddtRect,dt);
	}
	*/
	// note: this is never used
		
	/**
	 * Calculates time derivative between 2 given images within a specified rectangle
	 * @return ddt image with dimension specified by ddtRect
	 */
	public ImageProcessor calcDdtIm(ImageProcessor im1, ImageProcessor im2, Rectangle ddtRect, Integer dt) {
		// crop images
		im1 = CVUtils.cropByRect(im1, ddtRect);
		im2 = CVUtils.cropByRect(im2, ddtRect);
		
		// prepare empty ddtIm
		ImageProcessor ddtIm = new ByteProcessor(ddtRect.width,ddtRect.height);
		
		// subtract each pixel
		for (int i=0; i<ddtRect.width; i++) {
			for (int j=0; j<ddtRect.height; j++) {
				int pixDiff = im2.getPixel(i,j)-im1.getPixel(i,j);
//				int ddt = pixDiff/dt+128;
				int ddt = (pixDiff/dt+255)/2;
				ddtIm.putPixel(i,j,ddt);
			}
		}
		
		// deal with edge artifact
		// identify rawRect corners
		/*
		int w = ddtRect.width;
		int h = ddtRect.height;
		ddtIm.putPixel(3, 3, 110); // top left
		ddtIm.putPixel(w-4, h-4, 110); // bottom right
		ddtIm.putPixel(3, h-4, 110); // bottom left
		ddtIm.putPixel(w-4, 3, 110); // top right
		*/
		// horizontal
		/*
		for (int x=3;x<ddtIm.getWidth()-6;x++) {
			// see if can locate edge at all
			ddtIm.putPixel(x, 0, 100);
			ddtIm.putPixel(x, ddtIm.getHeight()-1, 100);
			
			// top
			if (ddtIm.getPixel(x,3)!=127 && ddtIm.getPixel(x,4)==127) {
				ddtIm.putPixel(x,3,127);
			}
			// bottom
			if (ddtIm.getPixel(x,ddtIm.getHeight()-6)!=127 && ddtIm.getPixel(x,ddtIm.getHeight()-7)==127) {
				ddtIm.putPixel(x,ddtIm.getHeight()-6,127);
			}
			
		}
		*/
		// vertical
		/*
		for (int y=0;y<ddtIm.getHeight();y++) {
			// left
			if (ddtIm.getPixel(0,y)!=128 && ddtIm.getPixel(1,y)==128) {
				ddtIm.set(0,y,128);
			}
			// right
			if (ddtIm.getPixel(ddtIm.getWidth()-1,y)!=128 && ddtIm.getPixel(ddtIm.getWidth()-2,y)==128) {
				ddtIm.set(ddtIm.getWidth()-1,y,128);
			}
		}
		*/
		
		return ddtIm;
	}
	
	/**
	 * Calculate and set point-sized ddtIm for all points extracted in the current frame
	 */
	public void setDdtIm4Pts() {
		for (int i=0; i<extractedPoints.size(); i++) {
			TrackPoint pt = extractedPoints.get(i);
			Rectangle ddtRect = (Rectangle)pt.rect.clone();
			int buffer = ep.ddtBuffer;
			ddtRect.grow(buffer,buffer);
			try {
				ImageProcessor ddtPtIm = new ByteProcessor(ddtRect.width,ddtRect.height);
				if (prevIm==null && nextIm!=null) { // first frame in stack, forward method
					ddtPtIm = calcDdtIm(currentIm.getProcessor(),nextIm.getProcessor(),ddtRect,increment);
				} else if (prevIm!=null && nextIm==null) { // last frame in stack, backward method
					ddtPtIm = calcDdtIm(prevIm.getProcessor(),currentIm.getProcessor(),ddtRect,increment);
				} else if (prevIm!=null && nextIm!=null) { // frame in the middle, central method
					ddtPtIm = calcDdtIm(prevIm.getProcessor(),nextIm.getProcessor(),ddtRect,increment*2);
				}
				pt.setImNew(ddtPtIm, 1);
			} catch (Exception e) {
//				System.out.println("Track "+pt.track.getTrackID()+" has no valid ddtIm at frame "+pt.getFrameNum());
				comm.message("Track "+pt.track.getTrackID()+" has no valid ddtIm at frame "+pt.getFrameNum()+", drawing blank image", VerbLevel.verb_message);
//				pt.setImNew(null, 1);
				ByteProcessor placeholder = new ByteProcessor(ddtRect.width,ddtRect.height);
				placeholder.setColor(new Color(127,127,127));
				placeholder.fill();
				pt.setImNew(placeholder,1);
			}
		}
	}
	
}





