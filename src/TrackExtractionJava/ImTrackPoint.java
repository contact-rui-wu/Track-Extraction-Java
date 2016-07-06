package TrackExtractionJava;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
//import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.util.Vector;


public class ImTrackPoint extends TrackPoint{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	transient protected ImageProcessor im;
	protected ImageProcessor imDeriv; //TODO will replace with secondaryIms
	protected byte[] serializableIm;
	protected int imOriginX;
	protected int imOriginY;
	
	// for now: stores ddt images
	protected enum SecondaryTypes {
		DDT
	}
	SecondaryTypes secondaryTypes;
	protected Vector<ImageProcessor> secondaryIms;
	protected Vector<Rectangle> secondaryRects;
	
	protected int trackWindowWidth;
	protected int trackWindowHeight;
	
	/**
	 * Identitfies the point as an IMTRACKPOINT
	 */
	final int pointType = 1;
	
	
	public ImTrackPoint() {
	}

	public ImTrackPoint(double x, double y, Rectangle rect, double area, int frame,
			int thresh) {
		super(x, y, rect, area, frame, thresh);
	}

	
	public ImTrackPoint(TrackPoint point, ImagePlus frameIm){
		super(point);
		findAndStoreIm(frameIm);
	}
	
	public void setImage (ImageProcessor im, int dispWidth, int dispHeight){
		this.im = im;
		trackWindowWidth = dispWidth;
		trackWindowHeight = dispHeight;
	}
	
	/**
	 * Manually set ddtIm and ddtRect to any im and rect pair given
	 */
	public void setDdtImage(ImageProcessor ddtIm, Rectangle ddtRect) {
		if (secondaryIms==null || secondaryRects==null) {
			secondaryIms = new Vector<ImageProcessor>(1,1);
			secondaryRects = new Vector<Rectangle>(1,1);
		}
		if (secondaryIms.isEmpty() || secondaryRects.isEmpty()) {
			secondaryIms.add(0,ddtIm);
			secondaryRects.add(0,ddtRect);
		} else {
			secondaryIms.set(0,ddtIm);
			secondaryRects.set(0,ddtRect);
		}
	}
	
	/**
	 * Default: find and store ddt point image using this track point's own rect (padded)
	 * <p>
	 */
	public void setDdtImage(ImagePlus ddtFrameIm, int pixelPad) {
		if (secondaryIms==null || secondaryRects==null) {
			secondaryIms = new Vector<ImageProcessor>(1,1);
			secondaryRects = new Vector<Rectangle>(1,1);
		}
		findAndStore2ndIm(0, ddtFrameIm, pixelPad);
	}
		
	/**
	 * This method is obsolete; ddt calculation is now done elsewhere. Retained only to avoid dependency problems.
	 */
	public void calcImDeriv(ImTrackPoint prevITP, ImTrackPoint nextITP, int derivMethod){
		
		Rectangle newRect;
		
		switch(derivMethod){
			case ExtractionParameters.DERIV_FORWARD:
				newRect = getCombinedBounds(nextITP, this);
				
				imDeriv = new  ColorProcessor(newRect.width, newRect.height);
				for (int i=0; i<newRect.width; i++){
					for (int j=0; j<newRect.height; j++){
						int val1 = getPixVal(nextITP, i+newRect.x, j+newRect.y);
						int val2 = getPixVal(this, i+newRect.x, j+newRect.y);
						int val = val1-val2;
						if (val>0){
							imDeriv.setColor(new Color(0, val, 0));
							imDeriv.drawPixel(i, j);
						} else {
							imDeriv.setColor(new Color(-val, 0, 0));
							imDeriv.drawPixel(i, j);
						}
					}
				}
				break;
			case ExtractionParameters.DERIV_BACKWARD:
				newRect = getCombinedBounds(this, prevITP);
				
				imDeriv = new ColorProcessor(newRect.width, newRect.height);
				for (int i=0; i<newRect.width; i++){
					for (int j=0; j<newRect.height; j++){
						int val = getPixVal(this, i+newRect.x, j+newRect.y)-getPixVal(prevITP, i+newRect.x, j+newRect.y);
						if (val>0){
							imDeriv.setColor(new Color(0, val, 0));
							imDeriv.drawPixel(i, j);
						} else {
							imDeriv.setColor(new Color(-val, 0, 0));
							imDeriv.drawPixel(i, j);
						}
					}
				}
				break;
			case ExtractionParameters.DERIV_SYMMETRIC:
				newRect = getCombinedBounds(nextITP, prevITP);
				
				imDeriv = new ColorProcessor(newRect.width, newRect.height);
				for (int i=0; i<newRect.width; i++){
					for (int j=0; j<newRect.height; j++){
						int val = (getPixVal(nextITP, i+newRect.x, j+newRect.y)-getPixVal(prevITP, i+newRect.x, j+newRect.y))/2;
						if (val>0){
							imDeriv.setColor(new Color(0, val, 0));
							imDeriv.drawPixel(i, j);
						} else {
							imDeriv.setColor(new Color(-val, 0, 0));
							imDeriv.drawPixel(i, j);
						}
					}
				}
				break;
			default:
				break;
		}
		
		
	}
	//*/
	
	// 20160608: moved to TrackPoint
	/*
	public static Rectangle getCombinedBounds(ImTrackPoint im1, ImTrackPoint im2){
		
		int x = (im1.rect.x<im2.rect.x)? im1.rect.x : im2.rect.x;
		int y = (im1.rect.y<im2.rect.y)? im1.rect.y : im2.rect.y;
		int w = ((im2.rect.x+im2.rect.width-im1.rect.x)>(im1.rect.x+im1.rect.width-im2.rect.x))? (im2.rect.x+im2.rect.width-im1.rect.x) : (im1.rect.x+im1.rect.width-im2.rect.x);
		int h = ((im2.rect.y+im2.rect.height-im1.rect.y)>(im1.rect.y+im1.rect.height-im2.rect.y))? (im2.rect.y+im2.rect.height-im1.rect.y) : (im1.rect.y+im1.rect.height-im2.rect.y);
		
		return new Rectangle(x, y, w, h);
	}
	*/
	
	public static int getPixVal(ImTrackPoint itp, int xx, int yy){
		
		int i = xx-itp.rect.x;
		int j = yy-itp.rect.y;
		if (i>=0 && j>=0 && i<itp.im.getWidth() && j<itp.im.getHeight()){
			return itp.im.get(i, j);//TODO add mask?
		} else {
			return 0;
		}
		
	}
	
	public ImageProcessor getRawIm(){
		return im;
	}
	
	public ImageProcessor getRaw2ndIm(int imInd) {
		return secondaryIms.get(imInd);
	}
	
	public ImageProcessor getIm(){
		//pad the image so be the same dimensions as the rest in the stack
		imOriginX = (int)x-(trackWindowWidth/2)-1;
		imOriginY = (int)y-(trackWindowHeight/2)-1;
		return CVUtils.padAndCenter(new ImagePlus("Point "+pointID, im), trackWindowWidth, trackWindowHeight, (int)x-rect.x, (int)y-rect.y);
	}
	
	public ImageProcessor getDdtIm() {
		return get2ndIm(0);
	}
	
	public ImageProcessor get2ndIm(int imInd) {
		// get raw secondary image
		ImageProcessor raw2ndIm = getRaw2ndIm(imInd);
		
		// pad to same size for visualization
		int centerX = raw2ndIm.getWidth()/2;
		int centerY = raw2ndIm.getHeight()/2;
		return CVUtils.padAndCenter(new ImagePlus("Point "+pointID+" (secondary)", raw2ndIm), trackWindowWidth, trackWindowHeight, centerX, centerY);
	}
	
	public void drawPoint(ColorProcessor backIm, Color c){
		
		for (int xc=0; xc<im.getWidth(); xc++){
			for (int yc=0; yc<im.getHeight(); yc++){
				int val = im.getPixel(xc, yc);
				if (backIm.getColor(xc+rect.x, yc+rect.y).getRed()<val){
					backIm.setColor(new Color(c.getRed()*val/255, c.getGreen()*val/255, c.getBlue()*val/255));
					backIm.drawPixel(xc+rect.x, yc+rect.y);
				}
			}
		}
	}
	
	public void findAndStoreIm(ImagePlus frameIm){
		Roi oldRoi = frameIm.getRoi();
		frameIm.setRoi(rect);
		im = frameIm.getProcessor().crop();//Does not affect frameIm's image
		frameIm.setRoi(oldRoi);
	}
			
	/**
	 * Set secondary image using track point's own rectangle without padding
	 */
	// unnecessary; can just set ExtractionParameter.ddtPixelPad=0
	/*
	public void findAndStore2ndIm(int imInd, ImagePlus secondaryFrameIm) {
		findAndStore2ndIm(imInd, secondaryFrameIm, rect);
	}
	*/
	
	/**
	 * Set secondary image using the point's own rectangle padded by pixelPad on all 4 sides
	 */
	public void findAndStore2ndIm(int imInd, ImagePlus secondaryFrameIm, int pixelPad) {
		Rectangle newRect = (Rectangle)rect.clone();
		newRect.grow(pixelPad,pixelPad);
		findAndStore2ndIm(imInd, secondaryFrameIm, newRect);
	}
	
	/**
	 * Crop secondary point image from the source frame image using the 
	 * given rectangle, stores the image in secondaryIms and the rectangle
	 * in secondaryRects
	 */
	public void findAndStore2ndIm(int imInd, ImagePlus secondaryFrameIm, Rectangle rect) {
		Roi oldRoi = secondaryFrameIm.getRoi();
		secondaryFrameIm.setRoi(rect);
		//ImageProcessor tmp = secondaryFrameIm.getProcessor();
		//tmp.setRoi(rect);
		secondaryIms.add(imInd, secondaryFrameIm.getProcessor().crop());
		secondaryRects.add(imInd, (Rectangle)rect.clone());
		secondaryFrameIm.setRoi(oldRoi);
	}
	
	protected void strip(){
		super.strip();
		serializableIm = null;
	}
	
	/**
	 * Generates Serializable forms of any non-serializable ImageJ objects 
	 * <p>
	 * For ImTrackPoints, the image is converted to a byte array
	 */
	protected void preSerialize(){
		FileSaver fs = new FileSaver(new ImagePlus("ImTrackPoint "+pointID, im));
		serializableIm = fs.serialize();
	}
	
	/**
	 * Recreates any non-serializable ImageJ objects 
	 * <p>
	 * For ImTrackPoints, the byte array is converted back into an ImageProcessor
	 */
	protected void postDeserialize(){
		Opener op = new Opener();
		ImagePlus im2 = op.deserialize(serializableIm);
		im = im2.getProcessor();		
	}
	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		//Write all TrackPoint data
		super.toDisk(dos, pw);
		
		//Image offest, width, and height already written in TrackPoint
		
		//Write image
		try {
			dos.writeByte(im.getWidth());
			dos.writeByte(im.getHeight());
			for (int j=0; j<im.getWidth(); j++){
				for (int k=0; k<im.getHeight(); k++){
					dos.writeByte(im.getPixel(j,k));
				}
			}
			
//			if (imDeriv==null){
//				dos.writeByte(0);
//				dos.writeByte(0);
//			} else {
//				dos.writeByte(imDeriv.getWidth());
//				dos.writeByte(imDeriv.getHeight());
//				for (int j=0; j<imDeriv.getWidth(); j++){
//					for (int k=0; k<imDeriv.getHeight(); k++){
//						dos.writeByte(((ColorProcessor)imDeriv).getColor(j, k).getRed()-128);
//						dos.writeByte(((ColorProcessor)imDeriv).getColor(j, k).getGreen()-128);
//						//Blue is empty
//					}
//				}
//			}
			
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing ImTrackPoint image for point "+pointID+"; aborting save");
			return 1;
		}
		
		///*
		// write secondary images
		try {
			// write number of secondary images stored
			if (secondaryIms==null || secondaryRects==null || secondaryIms.size()!=secondaryRects.size()) {
				dos.writeByte(0);
			} else {
				dos.writeByte(secondaryIms.size());
				for (int i=0; i<secondaryIms.size(); i++) {
					ColorProcessor tmpIm = (ColorProcessor)secondaryIms.get(i);
					Rectangle tmpRect = secondaryRects.get(i);
					// write each secondary rectangle
					dos.writeInt(tmpRect.x);
					dos.writeInt(tmpRect.y);
					dos.writeInt(tmpRect.width);
					dos.writeInt(tmpRect.height);
					// write each secondary image
					for (int j=0; j<tmpIm.getWidth(); j++) {
						for (int k=0; k<tmpIm.getHeight(); k++) {
							dos.writeByte(tmpIm.getColor(j,k).getRed()-128);
							dos.writeByte(tmpIm.getColor(j,k).getBlue()-128);
							// for now, green is empty
						}
					}
				}
			}
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing ImTrackPoint secondary images for point "+pointID+"; aborting save");
			return 1;
		}
		//*/
		
		return 0;
	}
	
	public int sizeOnDisk(){
		
		int size = super.sizeOnDisk();
		// image
		size += (2+im.getWidth()*im.getHeight());//Size of byte=1
		///*
		// secondary images
		size += 1; //stores number of secondary images
		if (secondaryIms != null && secondaryRects != null) {
			if (secondaryIms.size()==secondaryRects.size()) {
				for (int i=0; i<secondaryIms.size(); i++) {
					size += (4+secondaryRects.get(i).width+secondaryRects.get(i).height);
				} //size of each secondary image
			}

		}
		//*/
		return size;
	}

	public static ImTrackPoint fromDisk(DataInputStream dis, Track t, PrintWriter pw){
		
		ImTrackPoint itp = new ImTrackPoint();
		
		if (itp.loadFromDisk(dis,t,pw)==0){
			return itp;
		} else {
			return null;
		}
	}
	
	protected int loadFromDisk(DataInputStream dis, Track t, PrintWriter pw){
		
		//Load all superclass info
		if (super.loadFromDisk(dis, t,pw)!=0){
			return 1;
		}
		
		//read new data: image
		try {
			ExtractionParameters ep;
			if (t.exp==null){
				ep=new ExtractionParameters();
			} else{
				ep =t.exp.getEP();
			}
				
			trackWindowWidth = ep.trackWindowWidth;
			trackWindowHeight = ep.trackWindowHeight;
			
			//Get image data
			int w = dis.readByte();
			int h = dis.readByte();
			byte[] pix = new byte[w*h];
			for (int x=0; x<w; x++){
				for(int y=0; y<h; y++){
					pix[y*w+x] = dis.readByte();
				}
			}
			ImagePlus imp = new ImagePlus("new",new ByteProcessor(w, h, pix));

			im = imp.getProcessor();
			
			//Get imderiv data	
//			w = dis.readByte();
//			h = dis.readByte();
//			imDeriv = new ColorProcessor(w, h);
//			for (int x=0; x<w; x++){
//				for(int y=0; y<h; y++){
//					int [] colors = new int[2];
//					for (int cc=0; cc<2; cc++){
//						colors[cc] = (int)dis.readByte()+128;
//					}
//					imDeriv.setColor(new Color(colors[0], colors[1], 0));
//					imDeriv.drawPixel(x, y);
//				}
//			}
			
			///*
			// get secondary images data
			// get number of images
			int numOf2nds = dis.readByte();
			if (numOf2nds!=0) {
				for (int i=0; i<numOf2nds; i++) {
					// load rectangle
					int secX = dis.readByte();
					int secY = dis.readByte();
					int secW = dis.readByte();
					int secH = dis.readByte();
					Rectangle rect = new Rectangle(secX,secY,secW,secH);
					secondaryRects.set(i, rect);
					// load image
					ColorProcessor secIm = new ColorProcessor(secW,secH);
					for (int j=0; j<secW; j++) {
						for (int k=0; k<secH; k++) {
							int[] colors = new int[2];
							colors[0]=(int)dis.readByte()+128; //red
							colors[1]=(int)dis.readByte()+128; //blue
							secIm.setColor(new Color(colors[0],0,colors[1]));
							secIm.drawPixel(j,k);
						}
					}
					secondaryIms.set(i, secIm);
				}
			}
			//*/
			
		} catch (Exception e) {
			e.printStackTrace(pw);
			if (pw!=null) pw.println("Error loading ImTrackPoint Info");
			return 3;
		}
		
		return 0;
	}
	
	public String getCSVfieldVal(int ind){
		
		if (ind<=CSVPrefs.maxInd(super.getTypeName())){
			return super.getCSVfieldVal(ind);
		}
		
		
		switch (ind-CSVPrefs.maxInd(super.getTypeName())) {
		case 1:
			return imOriginX+"";
		case 2:
			return imOriginY+"";
		default: 
			return "";
		
		}
        
	}
	
	
	
	public String getTypeName(){
		return "ImTrackPoint";
	}
	
	
}
