package TrackExtractionJava;

import ij.ImagePlus;
import ij.gui.Roi;
import ij.io.FileSaver;
import ij.io.Opener;
import ij.process.ByteProcessor;
import ij.process.ShortProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
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
	/**
	 * @deprecated Will be replaced by the secondary fields
	 */
	@Deprecated
	protected ImageProcessor imDeriv;
	protected byte[] serializableIm;
	protected int imOriginX;
	protected int imOriginY;
	protected int trackWindowWidth;
	protected int trackWindowHeight;
	
	/**
	 * Identitfies the point as an IMTRACKPOINT
	 */
	final int pointType = 1;
	
	////////// Below: secondary image fields //////////
	
	protected Vector<ImagePlus> secondaryIms;
	protected Vector<Rectangle> secondaryRects;
	protected Vector<Boolean> secondaryValidity; //is this necessary?
	
	@Override
	public ImagePlus view2ndIm(int secondaryType) {
		return view2ndIm(secondaryType,null);
	}
	
	@Override
	public ImagePlus view2ndIm(int secondaryType, ExtractionParameters ep) {
		ImagePlus ip = get2ndIm(secondaryType);
		if (ep==null) {
			ep = new ExtractionParameters(); //use default
		}
		ImageProcessor newIm = CVUtils.padAndCenter(ip, ep.trackWindowWidth, ep.trackWindowHeight, ip.getWidth()/2, ip.getHeight()/2);
		return new ImagePlus(ip.getTitle(), newIm.resize(ep.trackWindowWidth*ep.trackZoomFac));
	}
	
	@Override
	public ImagePlus get2ndIm(int secondaryType) {
		ImagePlus retIm;
		if (secondaryValidity.get(secondaryType)) {
			retIm = secondaryIms.get(secondaryType);
		} else {
			System.out.println("Failed to get secondary image");
			retIm = null;
		}
		return retIm;
	}
	
	@Override
	public Rectangle get2ndRect(int secondaryType) {
		Rectangle retRect;
		if (secondaryValidity.get(secondaryType)) {
			retRect = secondaryRects.get(secondaryType);
		} else {
			System.out.println("Failed to get secondary rectangle");
			retRect = null;
		}
		return retRect;
	}
	
	@Override
	public void set2ndImAndRect(ImageProcessor im, Rectangle rect, int secondaryType) {
		try {
			if (secondaryIms==null || secondaryRects==null || secondaryValidity==null) {
				secondaryIms = new Vector<ImagePlus>();
				secondaryIms.setSize(secondaryType+1);
				secondaryRects = new Vector<Rectangle>();
				secondaryRects.setSize(secondaryType+1);
				secondaryValidity = new Vector<Boolean>();
				secondaryValidity.setSize(secondaryType+1);
			}
			secondaryIms.setElementAt(new ImagePlus(null, im), secondaryType);
			secondaryRects.setElementAt(rect, secondaryType);
			secondaryValidity.setElementAt(true, secondaryType);
		} catch (Exception e) {
			System.out.println("Failed to set secondary image");
			secondaryValidity.setElementAt(false, secondaryType);
		}
		
	}
	
	/**
	 * Given a larger ddtIm (can be ddtFrameIm or ddtCollisionPointIm), crop the correct ddtPointIm using a given rectangle
	 * @param ddtFrameIm
	 * @param rect Make sure you pass in a clone!
	 */
	@Override
	public void findAndStoreDdtIm(ImagePlus ddtFrameIm, Rectangle rect) {
		Roi oldRoi = ddtFrameIm.getRoi();
		ddtFrameIm.setRoi(rect);
		set2ndImAndRect(ddtFrameIm.getProcessor().crop(), rect, 0);
		ddtFrameIm.setRoi(oldRoi);
	}
	
	////////// Above: secondary image fields //////////
	
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
	 * @deprecated
	 * This method is obsolete; ddt calculation is now done elsewhere. Retained only to avoid dependency problems.
	 */
	@Deprecated
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
	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		//Write all TrackPoint data
		super.toDisk(dos, pw);
		
		//Image offset, width, and height already written in TrackPoint
		
		//Write image
		try {
			dos.writeByte(im.getWidth());
			dos.writeByte(im.getHeight());
			for (int j=0; j<im.getWidth(); j++){
				for (int k=0; k<im.getHeight(); k++){
					dos.writeByte(im.getPixel(j,k));
				}
			}			
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing ImTrackPoint image for point "+pointID+"; aborting save");
			return 1;
		}
		// write secondary images
		try {
			// write number of secondary images
			if (secondaryIms.size()!=secondaryRects.size() || secondaryRects.size()!=secondaryValidity.size() || secondaryIms.size()!=secondaryValidity.size()) {
				//TODO isn't there better way to check equality of 3 integers?
				throw new Exception();
			} else {
				dos.writeByte(secondaryIms.size());
				for (int i=0; i<secondaryIms.size(); i++) {
					// check and write validity
					dos.writeByte(secondaryValidity.get(i) ? 1:0);
					if (secondaryValidity.get(i)) {
						// get the im and rect out
						ImageProcessor secIm = secondaryIms.get(i).getProcessor();
						Rectangle secRect = secondaryRects.get(i);
						// write rectangle
						dos.writeInt(secRect.x);
						dos.writeInt(secRect.y);
						dos.writeInt(secRect.width);
						dos.writeInt(secRect.height);
						// write image
						// check and write processor type
						dos.writeByte(secIm.getBitDepth());
						switch (secIm.getBitDepth()) {
						case 8: //byte
							for (int j=0; j<secIm.getWidth(); j++) {
								for (int k=0; k<secIm.getHeight(); k++) {
									dos.writeByte(secIm.getPixel(j,k));
								}
							}
							break;
						case 16: //short
							for (int j=0; j<secIm.getWidth(); j++) {
								for (int k=0; k<secIm.getHeight(); k++) {
									dos.writeShort(secIm.getPixel(j,k));
								}
							}
							break;
						case 24: //color
							ColorProcessor colorIm = (ColorProcessor)secondaryIms.get(i).getProcessor();
							for (int j=0; j<colorIm.getWidth(); j++) {
								for (int k=0; k<colorIm.getHeight(); k++) {
									Color color = colorIm.getColor(j,k);
									if (color.getRed()==0 && color.getGreen()==0 && color.getBlue()==0) {
										// zero ddt
										dos.writeShort(0);
									} else if (color.getRed()>0 && color.getGreen()==0 && color.getBlue()==0) {
										// +ve ddt
										dos.writeShort(color.getRed());
									} else if (color.getRed()==0 && color.getGreen()==0 && color.getBlue()>0) {
										// -ve ddt
										dos.writeShort(0-color.getBlue());
									} else {
										// invalid ddt value
										throw new Exception();
									}
								}
							}
							break;
						case 32: //float
							for (int j=0; j<secIm.getWidth(); j++) {
								for (int k=0; k<secIm.getHeight(); k++) {
									dos.writeFloat(secIm.getPixel(j,k));
								}
							}
							break;
						}
					}
				}
			}
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing ImTrackPoint secondary images for point "+pointID+"; aborting save");
			return 1;
		}
		
		return 0;
	}
	
	/**
	 * TODO shouldn't this be integrated into {@link toDisk}?
	 */
	public int sizeOnDisk(){
		
		int size = super.sizeOnDisk();
		// image
		size += (2+im.getWidth()*im.getHeight());//Size of byte=1
		// secondary images
		if (secondaryIms.size()==secondaryRects.size() && secondaryRects.size()==secondaryValidity.size()) {
			size += Byte.SIZE;
			for (int i=0; i<secondaryIms.size(); i++) {
				size += Byte.SIZE;
				if (secondaryValidity.get(i)) {
					size += Integer.SIZE*4+Byte.SIZE;
					switch (secondaryIms.get(i).getBitDepth()) {
					case 8:
						size += Byte.SIZE*secondaryIms.get(i).getWidth()*secondaryIms.get(i).getHeight();
						break;
					case 16: case 24:
						size += Short.SIZE*secondaryIms.get(i).getWidth()*secondaryIms.get(i).getHeight();
						break;
					case 32:
						size += Float.SIZE*secondaryIms.get(i).getWidth()*secondaryIms.get(i).getHeight();
						break;
					}
				}
			}
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
		} catch (Exception e) {
			e.printStackTrace(pw);
			if (pw!=null) pw.println("Error loading ImTrackPoint image");
			return 3;
		}
		
		// read new data: secondary images
		// Q: do I need ExtractionParameters here?
		try {
			int secSize = dis.readByte();
			secondaryIms = new Vector<ImagePlus>();
			secondaryIms.setSize(secSize);
			secondaryRects = new Vector<Rectangle>();
			secondaryRects.setSize(secSize);
			secondaryValidity = new Vector<Boolean>();
			secondaryValidity.setSize(secSize);
			for (int i=0; i<secSize; i++) {
				// read and check validity
				int secValid = dis.readByte();
				if (secValid==0) {
					secondaryValidity.set(i, false);
				} else if (secValid==1) {
					secondaryValidity.set(i, true);
					secondaryRects.set(i, new Rectangle(dis.readInt(), dis.readInt(), dis.readInt(), dis.readInt()));
					// read and check image processor type
					int ipType = dis.readByte();
					switch (ipType) {
					case 8:
						ByteProcessor bip = new ByteProcessor(secondaryRects.get(i).width,secondaryRects.get(i).height);
						for (int j=0; j<secondaryRects.get(i).width; j++) {
							for (int k=0; k<secondaryRects.get(i).height; k++) {
								bip.set(j,k,dis.readByte());
							}
						}
						secondaryIms.set(i,new ImagePlus(null,bip));
						break;
					case 16:
						ShortProcessor sip = new ShortProcessor(secondaryRects.get(i).width,secondaryRects.get(i).height);
						for (int j=0; j<secondaryRects.get(i).width; j++) {
							for (int k=0; k<secondaryRects.get(i).height; k++) {
								sip.set(j,k,dis.readShort());
							}
						}
						secondaryIms.set(i,new ImagePlus(null,sip));
						break;
					case 24:
						ColorProcessor cip = new ColorProcessor(secondaryRects.get(i).width,secondaryRects.get(i).height);
						for (int j=0; j<secondaryRects.get(i).width; j++) {
							for (int k=0; k<secondaryRects.get(i).height; k++) {
								int px = dis.readByte();
								if (px>=0) {
									cip.setColor(new Color(px,0,0));
								} else if (px<0) {
									cip.setColor(new Color(0,0,-px));
								}
								cip.drawPixel(j,k);
							}
						}
						secondaryIms.set(i,new ImagePlus(null,cip));
						break;
					case 32:
						FloatProcessor fip = new FloatProcessor(secondaryRects.get(i).width,secondaryRects.get(i).height);
						for (int j=0; j<secondaryRects.get(i).width; j++) {
							for (int k=0; k<secondaryRects.get(i).height; k++) {
								fip.setf(j,k,dis.readFloat());
							}
						}
						secondaryIms.set(i,new ImagePlus(null,fip));
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace(pw);
			if (pw!=null) pw.println("Error loading ImTrackPoint secondary images");
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
