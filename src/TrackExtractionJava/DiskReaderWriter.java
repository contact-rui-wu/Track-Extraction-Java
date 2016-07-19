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
	int dataVersionTag;
	
	//////////////////
	// Constructors //
	//////////////////
	
	public DiskReaderWriter() {
		// default constructor
	}
	
	public DiskReaderWriter(DataInputStream dis, PrintWriter pw) {
		this.dis = dis;
		this.pw = pw;
	}
	
	public DiskReaderWriter(DataOutputStream dos, PrintWriter pw) {
		this.dos = dos;
		this.pw = pw;
	}
	
	/////////////////////////////////////////////////////////////////////
	// Experiment-level to/fromDisk methods: handles all versions here //
	/////////////////////////////////////////////////////////////////////
	
	public void experimentToDisk(Experiment ex) {
		// handle all exceptions here
		try {
			// write data version tag
			// experiment size
			// # of tracks
			// loop of trackToDisk()
			dos.flush();
		} catch (Exception e) {
			// handle exceptions
		}
	}
	
	public Experiment experimentFromDisk() {
		try {
			Experiment ex = new Experiment();
			// read and try to match version tag
			// - if fails, go back to beginning and assume version 0
			// read experiment size
			// read # of tracks
			// loop of trackFromDisk()
			return ex;
		} catch (Exception e) {
			// handle exceptions
			return null;
		}
	}
	
	//////////////////////////////////////////////
	// Sub-level to/fromDisk methods: version 1 //
	//////////////////////////////////////////////
	
	////////// track level //////////
	
	// Note: need to write sizeOnDisk at track level so that can skip tracks at experiment level
	
	public void trackToDisk1(Track tr) {
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
	
	////////// track point level //////////
	
	// Note: no need for sizeOnDisk at this level since we almost never look at single points
	// TODO handle different track point types
	
	public void trackPointToDisk1(TrackPoint tp) {
		// write track point type
		// write features
	}
	
	public TrackPoint trackPointFromDisk1() {
		TrackPoint tp = new TrackPoint();
		// read track point type
		// read features
		return tp;
	}
	
	////////// feature level //////////
	
	// Note: simple features like rect can be handled directly in the track point level
	// TODO handle non-image features, e.g. backbone
	
	public void imageToDisk1(ImageProcessor ip, Rectangle rect) throws IOException {
		// write image processor type
		dos.writeByte(ip.getBitDepth());
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
	
	//////////////////////////////////////////////
	// Sub-level to/fromDisk methods: version 0 //
	//////////////////////////////////////////////
	
	// Note: when all get() methods are ready, copy the object-specific methods here and modify appropriately 
	
	////////// track level //////////
	
	public int trackToDisk0(Track tr, DataOutputStream dos, PrintWriter pw) {
		return tr.toDisk(dos, pw);
	}
	
	public static Track trackFromDisk0(DataInputStream dis, int pointType, Experiment ex, PrintWriter pw) {
		return Track.fromDisk(dis, pointType, ex, pw);
	}
	
	////////// track point level //////////
	
	// TrackPoint
	
	public int tpToDisk0(TrackPoint tp, DataOutputStream dos, PrintWriter pw) {
		return tp.toDisk(dos, pw);
	}
	
	public static TrackPoint tpFromDisk0(DataInputStream dis, Track tr, PrintWriter pw) {
		return TrackPoint.fromDisk(dis, tr, pw);
	}
	
	// ImTrackPoint
	
	public int itpToDisk0(ImTrackPoint itp, DataOutputStream dos, PrintWriter pw) {
		return itp.toDisk(dos, pw);
	}
	
	public static ImTrackPoint itpFromDisk0(DataInputStream dis, Track tr, PrintWriter pw) {
		return ImTrackPoint.fromDisk(dis, tr, pw);
	}
	
	// MaggotTrackPoint
	
	public int mtpToDisk0(MaggotTrackPoint mtp, DataOutputStream dos, PrintWriter pw) {
		return mtp.toDisk(dos, pw);
	}
	
	public static MaggotTrackPoint mtpFromDisk0(DataInputStream dis, Track tr, PrintWriter pw) {
		return MaggotTrackPoint.fromDisk(dis, tr, pw);
	}
	
	// BackboneTrackPoint
	
	public int btpToDisk0(BackboneTrackPoint btp, DataOutputStream dos, PrintWriter pw) {
		return btp.toDisk(dos, pw);
	}
	
	public static BackboneTrackPoint btpFromDisk0(DataInputStream dis, Track tr, PrintWriter pw) {
		return BackboneTrackPoint.fromDisk(dis, tr, pw);
	}
	
}
