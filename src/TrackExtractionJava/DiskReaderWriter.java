package TrackExtractionJava;

import java.awt.Rectangle;
import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ShortProcessor;

public class DiskReaderWriter {
	
	DataInputStream dis;
	DataOutputStream dos;
	PrintWriter pw;
	
	////////// Constructors //////////
	
	public DiskReaderWriter(DataInputStream dis, PrintWriter pw) {
		this.dis = dis;
		this.pw = pw;
	}
	
	public DiskReaderWriter(DataOutputStream dos, PrintWriter pw) {
		this.dos = dos;
		this.pw = pw;
	}
	
	////////// Experiment-level to/fromDisk methods //////////
	
	public void experimentToDisk(Experiment ex) {
		// handle all exceptions here
		try {
			// version tag: if got tag, then has secondary images
			// experiment size
			// # of tracks
			// loop of trackToDisk()
		} catch (Exception e) {
			// handle exceptions
		}
	}
	
	public Experiment experimentFromDisk() {
		try {
			Experiment ex = new Experiment();
			// read and try to match version tag
			// - if fails, go back to beginning
			// read experiment size
			// read # of tracks
			// loop of trackFromDisk()
			return ex;
		} catch (Exception e) {
			// handle exceptions
			return null;
		}
	}
	
	////////// Sub-level to/fromDisk methods: version 1 //////////
	
	public void trackToDisk1(Track tr) {
		// do not use try block
		// track size
		// # of points
		// loop of trackPointToDisk()
	}
	
	public Track trackFromDisk1() {
		Track tr = new Track();
		// read track size
		// read # of points
		// loop of trackPointFromDisk()
		return tr;
	}
	
	public void trackPointToDisk1(TrackPoint tp) {
		// do not use try block
		// get track point type
		// (just add a validity boolean to all possible features of a track point?)
	}
	
	public TrackPoint trackPointFromDisk1() {
		TrackPoint tp = new TrackPoint();
		// read track point size
		// read track point type
		return tp;
	}
		
	public void imageToDisk1(ImageProcessor ip, Rectangle rect) throws IOException {
		// write image processor type
		dos.writeByte(ip.getBitDepth()); //TODO absent in old version
		switch (ip.getBitDepth()) {
		case 8: //byte processor
			for (int j=0; j<rect.width; j++) {
				for (int k=0; k<rect.height; k++) {
					dos.writeByte(ip.getPixel(j, k));
				}
			}
			break;
		case 16: //short processor
			for (int j=0; j<rect.width; j++) {
				for (int k=0; k<rect.height; k++) {
					dos.writeShort(ip.getPixel(j, k));
				}
			}
			break;
		case 24: //color processor
			ColorProcessor colorIm = ip.convertToColorProcessor();
			for (int j=0; j<rect.width; j++) {
				for (int k=0; k<rect.height; k++) {
					Color color = colorIm.getColor(j,k);
					dos.writeByte(color.getRed());
					dos.writeByte(color.getGreen());
					dos.writeByte(color.getBlue());
				}
			}
			break;
		case 32: //float processor
			for (int j=0; j<rect.width; j++) {
				for (int k=0; k<rect.height; k++) {
					dos.writeFloat(ip.getPixel(j, k));
				}
			}
			break;
		}	
	}
	
	public ImageProcessor imageFromDisk1(int width, int height) throws IOException {
		int ipType = dis.readByte();
		ImageProcessor ip;
		switch (ipType) {
		case 8: //byte processor
			ip = new ByteProcessor(width,height);
			for (int j=0; j<width; j++) {
				for (int k=0; k<height; k++) {
					ip.set(j,k,dis.readByte());
				}
			}
			break;
		case 16: //short processor
			ip = new ShortProcessor(width,height);
			for (int j=0; j<width; j++) {
				for (int k=0; k<height; k++) {
					ip.set(j,k,dis.readShort());
				}
			}
			break;
		case 24: //color processor
			ip = new ColorProcessor(width,height);
			for (int j=0; j<width; j++) {
				for (int k=0; k<height; k++) {
					ip.setColor(new Color(dis.readByte(),dis.readByte(),dis.readByte()));
					ip.drawPixel(j, k);
				}
			}
			break;
		case 32: //float processor
			ip = new FloatProcessor(width,height);
			for (int j=0; j<width; j++) {
				for (int k=0; k<height; k++) {
					ip.setf(j,k,dis.readFloat());
				}
			}
			break;
		default:
			ip = null; //TODO need to pass this upward
		}
		return ip;
	}
	
	////////// Sub-level to/fromDisk methods: version 0 //////////
	
	// Note: don't try to optimize, copy the exact codes
	
	public int trackToDisk0() {
		
	}
	
	private int trackSizeOnDisk() {
		
	}
	
	public static Track trackFromDisk0() {
		
	}
	
	private int loadTrackFromDisk() {
		
	}
	
	// TODO copy track point to/from/sizeOnDisk() methods to here
	
}
