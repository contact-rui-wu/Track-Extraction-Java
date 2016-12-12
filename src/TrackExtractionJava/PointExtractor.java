package TrackExtractionJava;

import ij.*;
import ij.gui.Roi;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
//import ij.process.ColorProcessor;
import ij.process.ByteProcessor;

import java.awt.Rectangle;
import java.awt.Color;
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
	//boolean isFirstRun;
	
	
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
	 * Thresholded image
	 */
	ImagePlus threshIm;
	
	// Handling ddt calculation
	ImagePlus prevIm;
	ImagePlus nextIm;
	ImagePlus ddtIm; //always corresponds to currentIm
	
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
		this.startFrameNum = startFrame;
//		imageStack = stack;
		this.comm = comm;
		this.ep = ep;
		endFrameNum = stack.getSize()-1;
//		endFrameNum = imageStack.getSize()-1;//.getNFrames()-1;// 
		increment = ep.increment;
		lastFrameExtracted=-1;
		//isFirstRun=true;
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
		//if(loadFrame(frameNum)>0){
		//	comm.message("Frame "+frameNum+" was NOT successfully loaded in Point Extractor", VerbLevel.verb_debug);
		//	return 1; 
		//}
		comm.message("Frame "+frameNum+" successfully loaded in Point Extractor", VerbLevel.verb_debug);		
		extractPoints(frameNum);
		lastFrameExtracted = frameNum;
		return 0;
	}
	
	/**
	 * Load currentIm, prevIm and/or nextIm depending on derivMethod; calculate and set ddtIm
	 * <p>
	 */
	public int loadFrameNew(int frameNum) {
		// address out-of-range error
		// Q: Natalie only did current>end, is it because we can have frameNum = -1?
		/*
		if (frameNum<startFrameNum || frameNum>endFrameNum) {
			System.out.println("Out of processing range!");
			return 1; //out-of-range error
		}
		*/
		
		currentFrameNum = frameNum;
		if (currentFrameNum>endFrameNum) {
			comm.message("Attempt to load frame "+currentFrameNum+" but the stack ends at frame "+endFrameNum, VerbLevel.verb_error);
			return 1;
		}
		
		int prevFrameNum = frameNum-increment;
		int nextFrameNum = frameNum+increment;
		
		switch(ep.derivMethod) {
		case 1: //forward
			if (frameNum==startFrameNum) { //beginning of extraction
				// load currentIm
				if (fl.getFrame(frameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2; //frame loader returned error
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+frameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					currentIm = new ImagePlus("Frame "+frameNum,fl.returnIm);
				}
				// load nextIm
				if (fl.getFrame(nextFrameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+nextFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					nextIm = new ImagePlus("Frame "+nextFrameNum,fl.returnIm);
				}
			} else { //middle of extraction
				// move old nextIm up
				currentIm = nextIm;
				// load new nextIm
				if (fl.getFrame(nextFrameNum, fnm, normFactor)!=0) { //error or reached end of stack
					nextIm = null;
					if (frameNum==endFrameNum) {
						if (comm!=null) comm.message("Reached end of stack", VerbLevel.verb_warning);
					} else {
						if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
						return 2;
					}
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+nextFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					nextIm = new ImagePlus("Frame "+nextFrameNum,fl.returnIm);
				}
			}
			break;
		case 2: //backward
			if (frameNum==startFrameNum) { //beginning of extraction
				//load currentIm
				if (fl.getFrame(frameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+frameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					currentIm = new ImagePlus("Frame "+frameNum,fl.returnIm);
				}
				//try to load prevIm
				if (fl.getFrame(prevFrameNum, fnm, normFactor)!=0) { //error or at beginning of stack
					if (comm!=null) comm.message("At beginning of stack", VerbLevel.verb_warning);
					assert prevIm == null;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+prevFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					prevIm = new ImagePlus("Frame "+prevFrameNum,fl.returnIm);
				}
			} else { //middle of extraction
				// move old currentIm up
				prevIm = currentIm;
				// load new currentIm
				if (fl.getFrame(frameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+frameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					currentIm = new ImagePlus("Frame "+frameNum,fl.returnIm);
				}
			}
			break;
		case 3: //central
			if (frameNum==startFrameNum) { //beginning of extraction
				//load currentIm, nextIm, then move on
				if (fl.getFrame(frameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2; //frame loader returned error
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+frameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					currentIm = new ImagePlus("Frame "+frameNum,fl.returnIm);
				}
				// load nextIm
				if (fl.getFrame(nextFrameNum, fnm, normFactor)!=0) {
					if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
					return 2;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+nextFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					nextIm = new ImagePlus("Frame "+nextFrameNum,fl.returnIm);
				}
				//try to load prevIm
				if (fl.getFrame(prevFrameNum, fnm, normFactor)!=0) { //error or at beginning of stack
					if (comm!=null) comm.message("At beginning of stack", VerbLevel.verb_warning);
					assert prevIm == null;
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+prevFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					prevIm = new ImagePlus("Frame "+prevFrameNum,fl.returnIm);
				}
			} else {
				// move old currentIm and nextIm up
				prevIm = currentIm;
				currentIm = nextIm;
				// load new nextIm
				if (fl.getFrame(nextFrameNum, fnm, normFactor)!=0) { //error or reached end of stack
					nextIm = null;
					if (frameNum==endFrameNum) {
						if (comm!=null) comm.message("Reached end of stack", VerbLevel.verb_warning);
					} else {
						if (comm!=null) comm.message("Frame Loader returned error", VerbLevel.verb_warning);
						return 2;
					}
				} else {
					if (comm!=null) comm.message("No error from frame loader when loading frame "+nextFrameNum+" in pe.loadFrame", VerbLevel.verb_debug);
					nextIm = new ImagePlus("Frame "+nextFrameNum,fl.returnIm);
				}
			}
			break;
		}
		
		if (ep.frameSizeDdt) {
			ddtIm = new ImagePlus();
			calcAndSetDdtFrameIm(ep.derivMethod);
			if (ddtIm==null) {
				if (comm!=null) comm.message("No ddtIm for frame "+frameNum, VerbLevel.verb_warning);
			}
		} else {
			ddtIm = null;
		}
		
		if (comm!=null) comm.message("Thresholding image to zero...", VerbLevel.verb_debug);
		defaultThresh();
		if (comm!=null) comm.message("...finished thresholding image to zero", VerbLevel.verb_debug);
		
		return 0;
	}
	
	/**
	 * Loads the frame into the currentIm, backSubIm, ForegroundIm, and threshIm images, and loads the proper backgroundIm
	 * @param frameNum Index of the frame to be loaded
	 * @return status, 0 means all is good, otherwise error
	 */
	public int loadFrame(int frameNum){
		
		if (frameNum==currentFrameNum && (analysisRect==null ||(fl.returnIm.getWidth()==analysisRect.getWidth() && fl.returnIm.getHeight()==analysisRect.getHeight()))){
			if (comm!=null) comm.message("Frame already loaded in Frameloader", VerbLevel.verb_message);
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
		threshIm.getProcessor().threshold((int) ep.globalThreshValue);

	}
	
	void rethresh(int thresh){
		if (comm!=null) comm.message("Rethresholding to "+thresh, VerbLevel.verb_debug);
		threshIm = new ImagePlus("Thresh im Frame "+currentFrameNum, currentIm.getProcessor().getBufferedImage());
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

	    // handle local ddt calculation here
	    if (ep.frameSizeDdt) {
	    	extractedPoints = findPtsInIm(currentFrameNum, currentIm, threshIm, ddtIm, thresh, fl.getStackDims(), analysisRect, ep, showResults, comm);
	    } else {
	    	extractedPoints = findPtsInIm(currentFrameNum, currentIm, threshIm, null, thresh, fl.getStackDims(), analysisRect, ep, showResults, comm);
	    	addDdtImToPoints();
	    }
	    
	    String s = "Frame "+currentFrameNum+": Extracted "+extractedPoints.size()+" new points";
	    if (comm!=null) comm.message(s, VerbLevel.verb_message);
	    
	}
	
	protected static Vector<TrackPoint> findPtsInIm(int frameNum, ImagePlus currentIm, ImagePlus threshIm, int thresh, int[] frameSize, Rectangle analysisRect, ExtractionParameters ep, boolean showResults,  Communicator comm){
		return findPtsInIm(frameNum, currentIm, threshIm, null, thresh, frameSize, analysisRect, ep, showResults, comm);
	}
	
	protected static Vector<TrackPoint> findPtsInIm(int frameNum, ImagePlus currentIm, ImagePlus threshIm, ImagePlus ddtIm, int thresh, int[] frameSize, Rectangle analysisRect, ExtractionParameters ep, boolean showResults,  Communicator comm){
		
//		boolean excl = ep.excludeEdges;
//		ep.excludeEdges = false;
		if (comm!=null && analysisRect!=null) comm.message("Analysis Rect: ("+analysisRect.x+","+analysisRect.y+"), "+analysisRect.width+"x"+analysisRect.height, VerbLevel.verb_message);
		ResultsTable pointTable = CVUtils.findPoints(threshIm, thresh, analysisRect, ep, (int)ep.minArea, (int)ep.maxArea, showResults);
//		ep.excludeEdges = excl;
		
//		if (showResults) {
		if (comm!=null) comm.message("Frame "+frameNum+": "+pointTable.getCounter()+" points in ResultsTable", VerbLevel.verb_message);
//	    }

			//ResultsTable rt, int frameNum, Rectangle analysisRect, int[] frameSize, ImagePlus currentIm, ImagePlus threshIm, ExtractionParameters ep, int thresh, Communicator comm
		Vector<TrackPoint> pts = rt2TrackPoints(pointTable, frameNum, analysisRect, frameSize, currentIm, threshIm, ddtIm, ep, thresh, comm);
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
		return rt2TrackPoints (rt, frameNum, analysisRect, fl.getStackDims(), currentIm, threshIm, ddtIm, ep, thresh, comm);
	}
	
	/**
	 * Proceeds without ddt frame image; handles dependency problem in {@link DistanceMapSplitting}
	 */
	/*
	public static Vector<TrackPoint> rt2TrackPoints (ResultsTable rt, int frameNum, Rectangle analysisRect, int[] frameSize, ImagePlus currentIm, ImagePlus threshIm, ExtractionParameters ep, int thresh, Communicator comm) {
		return rt2TrackPoints (rt, frameNum, analysisRect, frameSize, currentIm, threshIm, null, ep, thresh, comm);
	}
	*/
	
	public static Vector<TrackPoint> rt2TrackPoints (ResultsTable rt, int frameNum, Rectangle analysisRect, int[] frameSize, ImagePlus currentIm, ImagePlus threshIm, ImagePlus ddtIm, ExtractionParameters ep, int thresh, Communicator comm) {
		
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
					Rectangle ddtRect = (Rectangle)rect.clone();
					ddtRect.grow(ep.derivPixelPad,ep.derivPixelPad);
					switch (ep.trackPointType){
						case 1: //ImTrackPoint
							ImTrackPoint iTPt = new ImTrackPoint(x,y,rect,area,frameNum,thresh);
//							if (currentFrameNum!=frameNum){
//								loadFrame(frameNum);
//							}
							// add image
							Roi oldRoi = currentIm.getRoi();
							currentIm.setRoi(crRect);
							ImageProcessor im = currentIm.getProcessor().crop(); //does not affect currentIm
							currentIm.setRoi(oldRoi);
							iTPt.setImage(im, ep.trackWindowWidth, ep.trackWindowHeight);
							// add ddt point image
							// NOTE: deprecated since frame size ddt calculation is too slow
							if(ep.frameSizeDdt) {
							// note: any error will be caught in ImTrackPoint.set2ndImAndRect() and validity will be set to false
								//Rectangle ddtRect = (Rectangle)iTPt.rect.clone();
								//ddtRect.grow(ep.derivPixelPad,ep.derivPixelPad);
								iTPt.findAndStoreDdtIm(ddtIm,ddtRect);
							}
							tp.add(iTPt);
							break;
						case 2: //MaggotTrackPoint
							MaggotTrackPoint mTPt = new MaggotTrackPoint(x,y,rect,area,frameNum,thresh);
							mTPt.setCommunicator(comm);
//							if (currentFrameNum!=frameNum){
//								loadFrame(frameNum);
//							}
							mTPt.setStart((int)rt.getValue("XStart", row)+arX, (int)rt.getValue("YStart", row)+arY);
							Roi roi = currentIm.getRoi();
							currentIm.setRoi(crRect);
							ImageProcessor im2 = currentIm.getProcessor().crop(); //does not affect currentIm
							currentIm.setRoi(roi);
							//Set the image mask
							mTPt.setMask(getMask(rt, row, threshIm, ep));
							
//							String debugInfo = "Rect: (x="+mtPt.x+", y="+mtPt.y+")\n";
//							debugInfo+="Bitdepth="+im2.getBitDepth()+"\n";
//							debugInfo+="Type="+im2.getBufferedImage().getType()+"(proper="+BufferedImage.TYPE_BYTE_GRAY+")\n";
//							new TextWindow("New point info", debugInfo, 500, 500);
							
							/*
							new ImagePlus("Current image", currentIm.getProcessor()).show();
							new ImagePlus("New point image", im2).show();
							*/
							
							mTPt.setImage(im2, ep.trackWindowWidth, ep.trackWindowHeight);
							// add ddt point image
							// NOTE: deprecated since frame size ddt calculation is too slow
							if (ep.frameSizeDdt) {
								mTPt.findAndStoreDdtIm(ddtIm,ddtRect);
							}
							
							mTPt.extractFeatures();
							tp.add(mTPt);
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
				if (comm!=null) comm.message("Point "+row+" from ResultsTable in frame "+frameNum+" was clipped", VerbLevel.verb_message);
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
	
	///////////////////////
	// ddt frame methods
	///////////////////////
	
	// ignoring analysisRect for now TODO deal with analysisRect
	
	/**
	 * Calculates ddtIm of current frame using specified derivation method
	 */
	public void calcAndSetDdtFrameIm(int derivMethod) {
		switch(derivMethod) {
		case 1: //forward
			// handle last frame
			if(nextIm == null) {
				ddtIm = null;
				System.out.println("Cannot calculate ddt frame image: nextIm doesn't exist");
			} else {
				calcAndSetDdtFrameIm(currentIm,nextIm,increment);
			}
			break;
		case 2: //backward
			// handle first frame
			if(prevIm == null) {
				ddtIm = null;
				System.out.println("Cannot calculate ddt frame image: prevIm doesn't exist");
			} else {
				calcAndSetDdtFrameIm(prevIm,currentIm,increment);
			}
			break;
		case 3: //central
			if(prevIm == null) {
				ddtIm = null;
				System.out.println("Cannot calculate ddt frame image: prevIm doesn't exist");
			} else if(nextIm == null) {
				ddtIm = null;
				System.out.println("Cannot calculate ddt frame image: nextIm doesn't exist");
			} else {
				calcAndSetDdtFrameIm(prevIm,nextIm,increment*2);
			}
			break;
		}
	}
	
	/**
	 * Calculates ddtIm of current frame given two images and dt
	 */
	public void calcAndSetDdtFrameIm(ImagePlus imp1, ImagePlus imp2, int dt) {
		
		ImageProcessor im1 = imp1.getProcessor();
		ImageProcessor im2 = imp2.getProcessor();
		
		ddtIm.setProcessor(new ByteProcessor(im1.getWidth(), im1.getHeight()));
		
		for(int i=0; i<im1.getWidth(); i++) {
			for(int j=0; j<im1.getHeight(); j++) {
				int pixDiff = im2.getPixel(i,j)-im1.getPixel(i,j);
				int ddt = (pixDiff/dt+256)/2;
				/*
				if (pixDiff>0) {
					ddtIm.getProcessor().setColor(new Color(ddt,0,0));
				} else {
					ddtIm.getProcessor().setColor(new Color(0,0,-ddt));
				}
				ddtIm.getProcessor().drawPixel(i,j);
				*/
				ddtIm.getProcessor().set(i,j,ddt);
			}
		}
	}
	
	public void addDdtImToPoints() {
		for (int i=0; i<extractedPoints.size(); i++) {
			TrackPoint pt = extractedPoints.get(i);
			Rectangle ddtRect = (Rectangle)pt.rect.clone();
			ddtRect.grow(ep.derivPixelPad, ep.derivPixelPad);
			pt.ensure2ndSize(0);
			try {
				ImageProcessor ddtPointIm = calcDdtPointIm(ddtRect,ep.derivMethod);
				pt.set2ndImAndRect(ddtPointIm,ddtRect,0);
			} catch (Exception e) {
				pt.secondaryValidity.setElementAt(false, 0);
			}
		}
	}
	
	public ImageProcessor calcDdtPointIm(Rectangle rect, int derivMethod) {
		//ImageProcessor ddtPointIm = new ColorProcessor(rect.width, rect.height);
		ImageProcessor ddtPointIm = new ByteProcessor(rect.width, rect.height);
		ImageProcessor im1;
		ImageProcessor im2;
		int dt = increment;
		switch (derivMethod) {
		case 1: //forward
			im1 = cropFrameIm(currentIm.getProcessor(), rect);
			im2 = cropFrameIm(nextIm.getProcessor(), rect);
			break;
		case 2: //backward
			im1 = cropFrameIm(prevIm.getProcessor(), rect);
			im2 = cropFrameIm(currentIm.getProcessor(), rect);
			break;
		case 3: //central
			dt = increment*2;
			im1 = cropFrameIm(prevIm.getProcessor(), rect);
			im2 = cropFrameIm(nextIm.getProcessor(), rect);
			break;
		default:
			return null;
		}
		for(int i=0; i<im1.getWidth(); i++) {
			for(int j=0; j<im1.getHeight(); j++) {
				int pixDiff = im2.getPixel(i,j)-im1.getPixel(i,j);
				int ddt = (pixDiff/dt+256)/2; // note: java default gray is 128
				/*
				if (pixDiff>0) {
					ddtPointIm.setColor(new Color(ddt,0,0));
				} else {
					ddtPointIm.setColor(new Color(0,0,-ddt));
				}
				ddtPointIm.drawPixel(i,j);
				*/
				ddtPointIm.set(i,j,ddt);
			}
		}
		return ddtPointIm;
	}
	
	public ImageProcessor cropFrameIm(ImageProcessor frameIm, Rectangle cropRect) {
		Rectangle oldRoi = frameIm.getRoi();
		frameIm.setRoi(cropRect);
		ImageProcessor croppedIm = frameIm.crop();
		frameIm.setRoi(oldRoi);
		return croppedIm;
	}
	
}





