package TrackExtractionJava;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
//import java.util.HashMap;

public class ImTrackPoint extends TrackPoint{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	//transient protected ImageProcessor im;
	protected ImageProcessor im; // transient seems unnecessary?
	protected ImageProcessor ddtIm;
	protected ImageProcessor imDeriv; // TODO Rui: get rid of old imDeriv dependencies
	protected byte[] serializableIm;
	protected int imOriginX;
	protected int imOriginY;
	
	private int trackWindowWidth;
	protected int getTrackWindowWidth() {
		return trackWindowWidth;
	}

	protected void setTrackWindowWidth(int trackWindowWidth) {
		this.trackWindowWidth = trackWindowWidth;
	}

	protected int getTrackWindowHeight() {
		return trackWindowHeight;
	}

	protected void setTrackWindowHeight(int trackWindowHeight) {
		this.trackWindowHeight = trackWindowHeight;
	}

	private int trackWindowHeight;
	
	/**
	 * Identitfies the point as an IMTRACKPOINT
	 */
	final static int pointType = 1;
	
	
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
	
	/**
	 * Set image to ImTrackPoint
	 * @param im true-size (not enlarged or padded) image to be set
	 * @param imType 0: rawIm; 1: ddtIm
	 */
	public void setImNew(ImageProcessor im, int imType) {
		switch (imType) {
		case 0:
			this.im = im;
			break;
		case 1:
			this.ddtIm = im;
		}
	}
	
	/**
	 * Get true-size (not enlarged or padded) image from ImTrackPoint
	 * @param imType 0: rawIm; 1: ddtIm
	 * @return ImageProcessor containing the corresponding image
	 */
	public ImageProcessor getImNew(int imType) {
//		try {
			switch (imType) {
			case 0:
				return im;
			case 1:
				return ddtIm;
			default:
				return null;
			}
//		} catch (NullPointerException e) {
//			return null;
//		}
	}
	
	/*
	public ImageProcessor getPadImNew(int imType) {
		int buffer = 0;
		return getPadImNew(imType,buffer);
	}
	*/
	
	public ImageProcessor getPadImNew(int imType) {
		return getPadImNew(imType,1);
	}
	
	/**
	 * Get scaled and padded image from ImTrackPoint
	 * @param imType 0: rawIm; 1: ddtIm
	 * @return ImageProcessor containing the corresponding padded image
	 */
	public ImageProcessor getPadImNew(int imType, double scaleFac) {
//		try {
			imOriginX = (int)x-(trackWindowWidth/2)-1;
			imOriginY = (int)y-(trackWindowHeight/2)-1;
			int newWidth;
			int newHeight;
			ImageProcessor srcIm;
			Color padColor;
			switch (imType) {
			case 0:
//				int newRawW = (int)(im.getWidth()*scaleFac);
//				int newRawH = (int)(im.getHeight()*scaleFac);
//				return CVUtils.padAndCenter(new ImagePlus(null, im.resize(newRawW,newRawH)), trackWindowWidth, trackWindowHeight, newRawW/2, newRawH/2);
				newWidth = (int)(im.getWidth()*scaleFac);
				newHeight = (int)(im.getHeight()*scaleFac);
				im.setInterpolationMethod(ImageProcessor.BILINEAR);
				srcIm = im.resize(newWidth);
				padColor = Color.black;
				break;
			case 1:
//				Rectangle ddtRect = (Rectangle)rect.clone();
//				ddtRect.grow(buffer,buffer);
//				int newDdtW = (int)(ddtIm.getWidth()*scaleFac);
//				int newDdtH = (int)(ddtIm.getHeight()*scaleFac);
//				return CVUtils.padAndCenter(new ImagePlus(null, ddtIm.resize(newDdtW,newDdtH)), trackWindowWidth, trackWindowHeight, newDdtW/2, newDdtH/2, new Color(127,127,127));
				newWidth = (int)(ddtIm.getWidth()*scaleFac);
				newHeight = (int)(ddtIm.getHeight()*scaleFac);
				ddtIm.setInterpolationMethod(ImageProcessor.BILINEAR);
				srcIm = ddtIm.resize(newWidth);
				padColor = new Color(127,127,127);
				break;
			default:
				return null;
			}
			
			return CVUtils.padAndCenter(srcIm, trackWindowWidth, trackWindowHeight, newWidth/2, newHeight/2, padColor);
//		} catch (NullPointerException e) {
//			return null;
//		}
	}
	
	public void setImage (ImageProcessor im, int dispWidth, int dispHeight){
		this.im = im;
		trackWindowWidth = dispWidth;
		trackWindowHeight = dispHeight;
	}
	
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
	
	public ImTrackPoint getPrev(){
		return (ImTrackPoint)prev;
	}
	
	public ImTrackPoint getNext(){
		return (ImTrackPoint)next;
	}
	
	public static Rectangle getCombinedBounds(ImTrackPoint im1, ImTrackPoint im2){
		
		int x = (im1.rect.x<im2.rect.x)? im1.rect.x : im2.rect.x;
		int y = (im1.rect.y<im2.rect.y)? im1.rect.y : im2.rect.y;
		int w = ((im2.rect.x+im2.rect.width-im1.rect.x)>(im1.rect.x+im1.rect.width-im2.rect.x))? (im2.rect.x+im2.rect.width-im1.rect.x) : (im1.rect.x+im1.rect.width-im2.rect.x);
		int h = ((im2.rect.y+im2.rect.height-im1.rect.y)>(im1.rect.y+im1.rect.height-im2.rect.y))? (im2.rect.y+im2.rect.height-im1.rect.y) : (im1.rect.y+im1.rect.height-im2.rect.y);
		
		return new Rectangle(x, y, w, h);
	}
	
	/**
	 * Gets the pixel at the absolute (frame) coordinate xx, yy,  or 0 if the coordinate is not in the trackpoint frame
	 * @param itp
	 * @param xx
	 * @param yy
	 * @return
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
	
	/**
	 * ATTENTION: old method before ddt calculation is implemented; raw here is vs. enlarged and padded, not vs. ddt
	 */
	public ImageProcessor getRawIm(){
		return im;
	}
	
	public ImageProcessor getIm(){
		//pad the image so be the same dimensions as the rest in the stack
		imOriginX = (int)x-(trackWindowWidth/2)-1;
		imOriginY = (int)y-(trackWindowHeight/2)-1;
		return CVUtils.padAndCenter(new ImagePlus("Point "+pointID, im), trackWindowWidth, trackWindowHeight, (int)x-rect.x, (int)y-rect.y);
		 
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
	
	// TODO Rui: write to/loadFrom/sizeOnDiskNew methods for ImTrackPoint
	
	/**
	 * not used; ddt condition is replaced by ddtIm==/!=null in {@link #toDisk(DataOutputStream,PrintWriter) toDisk}
	 */
	public int toDisk(DataOutputStream dos, PrintWriter pw, boolean ddt) {

		if (!ddt) {
			return toDisk(dos, pw);
		} else {
			super.toDisk(dos, pw);
			// write images
			try {
				// write raw image
				dos.writeByte(im.getWidth());
				dos.writeByte(im.getHeight());
				for (int i = 0; i < im.getWidth(); i++) {
					for (int j = 0; j < im.getHeight(); j++) {
						dos.writeByte(im.getPixel(i, j));
					}
				}
				// write ddt image
				if (ddtIm == null) {
					dos.writeByte(0);
					dos.writeByte(0);
				} else {
					dos.writeByte(ddtIm.getWidth());
					dos.writeByte(ddtIm.getHeight());
					for (int p = 0; p < ddtIm.getWidth(); p++) {
						for (int q = 0; q < ddtIm.getHeight(); q++) {
							dos.writeByte(ddtIm.getPixel(p, q));
						}
					}
				}
			} catch (Exception e) {
				if (pw != null)
					pw.println("Error writing ImTrackPoint image for point " + pointID + "; aborting save");
				return 1;
			}

			return 0;
		}
	}
	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		//Write all TrackPoint data
		super.toDisk(dos, pw);
		
		//Image offset, width, and height already written in TrackPoint
		
		//Write images
		try {
			// write raw image
			dos.writeByte(im.getWidth());
			dos.writeByte(im.getHeight());
			for (int j=0; j<im.getWidth(); j++){
				for (int k=0; k<im.getHeight(); k++){
					dos.writeByte(im.getPixel(j,k));
				}
			}
			// write ddt image if exists
			if (ddtIm==null) {
				dos.writeByte(0);
				dos.writeByte(0);
			} else {
				dos.writeByte(ddtIm.getWidth());
				dos.writeByte(ddtIm.getHeight());
				for (int p = 0; p < ddtIm.getWidth(); p++) {
					for (int q = 0; q < ddtIm.getHeight(); q++) {
						dos.writeByte(ddtIm.getPixel(p, q));
					}
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
		
		return 0;
	}
	
	public int sizeOnDisk(){
		
		int size = super.sizeOnDisk();
		size += 2;//Im width, height
		size += im.getWidth()*im.getHeight();//pixels
		size += 2; // ddtIm width, height (both=0 if ddtIm=null)
		if (ddtIm!=null) {
			size += ddtIm.getWidth()*ddtIm.getHeight();
		}
		
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
			
			//Get rawIm data
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
			
			// get ddtIm data
			int ddtW = dis.readByte();
			int ddtH = dis.readByte();
			if (ddtW!=0 & ddtH!=0) {
				byte[] ddtPix = new byte[ddtW*ddtH];
				for (int i=0;i<ddtW;i++) {
					for (int j=0;j<ddtH;j++) {
						ddtPix[j*ddtW+i] = dis.readByte();
					}
				}
				ddtIm = new ByteProcessor(ddtW,ddtH,ddtPix);
			}
			
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
	
	

	public int getPointType(){
		return ImTrackPoint.pointType;
	}
	
	public String getTypeName(){
		return "ImTrackPoint";
	}
	
	
}
