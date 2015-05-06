package TrackExtractionJava;

import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Plot;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.Color;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.Collections;
import java.util.ListIterator;
import java.util.Vector;


public class Track implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Used to generate unique IDs for the TrackPoints
	 * <p> 
	 * Incremented each time a new track is made
	 */
	static int nextIDNum=0;
	/**
	 * Constituent TrackPoints
	 */
	protected Vector<TrackPoint> points;
	/**
	 * Unique identifier for the track 
	 */
	private int trackID;
	
	/**
	 * Maximum ROI height, for playing movies
	 */
	private int maxHeight;
	
	/**
	 * Maximum image height, for playing movies
	 */
	private int maxWidth;
	
	
	Vector<Boolean> isCollision;
	
	
	private transient TrackMatch match;
	
	/**
	 * Access to the TrackBuilder
	 */
	transient TrackBuilder tb;
	/**
	 * Access to the experiment
	 */
	Experiment exp;
	
	transient Communicator comm;
	
	
	public Track(){
		
	}
	
	public Track(TrackBuilder tb){
		maxHeight=0;
		maxWidth=0;
		
		points = new Vector<TrackPoint>();
		isCollision = new Vector<Boolean>();
		
		trackID = nextIDNum;
		nextIDNum++;
		this.tb = tb;

	}
	
	
	public Track(Vector<BackboneTrackPoint> pts, int ID){
		maxHeight=0;
		maxWidth=0;
		
		points = new Vector<TrackPoint>();
		points.addAll(pts);
		isCollision = new Vector<Boolean>();
		
		trackID = ID;

	}
	
	public Track(Vector<BackboneTrackPoint> pts, Track tr){
		
		nextIDNum = tr.getNextIDNum();
		maxHeight = tr.maxHeight;
		maxWidth = tr.maxWidth;
		exp = tr.exp;
		isCollision = tr.isCollision;
		
		points = new Vector<TrackPoint>();
		points.addAll(pts);
		for (int i=0; i<points.size(); i++){
			points.get(i).track = this;
		}
		
		
		trackID = nextIDNum;
		nextIDNum++;
		
		
	}
	
	public Track(TrackPoint firstPt, TrackBuilder tb){
		maxHeight=0;
		maxWidth=0;
		
		points = new Vector<TrackPoint>();
		isCollision = new Vector<Boolean>(); 
		
		extendTrack(firstPt);
		
		trackID = nextIDNum;
		nextIDNum++;
		this.tb = tb;
		
	}
	
	/**
	 * Adds the given point to the end of the Track
	 * @param pt
	 */
	public void extendTrack(TrackPoint pt){
		points.add(pt);
		pt.track=this;
//		pt.setTrack(this);
		isCollision.addElement(false);
		
		if(pt.rect.height>maxHeight){
			maxHeight = pt.rect.height; 
		}
		
		if(pt.rect.width>maxWidth){
			maxWidth = pt.rect.width; 
		}
		
	}
	
	/**
	 * Finds the nearest point in a list to the last point in the track 
	 * @param list List of points to search over
	 * @return Nearest point in the list to the last point in the track
	 */
	public TrackPoint nearestPointinList2End(Vector<TrackPoint> list){
		return points.lastElement().nearestInList2Pt(list);
	}
	
	/**
	 * Finds nearest points in a list to the last point in the track, up to NPTS points
	 * @param list List of points to search over
	 * @param nPts Max number of nearest points to find
	 * @return Nearest point in the list to the last point in the track, up to NPTS
	 */
	public Vector<TrackPoint> nearestNPts2End(Vector<TrackPoint> list, int nPts){
		return points.lastElement().nearestNPts2Pt(list, nPts);
	}
	
	/**
	 * Distance from last point in track to query point
	 * @param pt Query point
	 * @return Distance from last point in track to query point
	 */
	public double distFromEnd(TrackPoint pt){
		if ( (pt!=null) && (!points.isEmpty()) ){
			return pt.dist(points.lastElement());
		}
		return -1;
	}
	
	///////////////////////////
	// Area methods
	///////////////////////////	
	/**
	 * Calculates the mean area of the contours in the track
	 * @return Mean area of the contours in the track
	 */
	public double meanArea(){
		double sum = 0;
		ListIterator<TrackPoint> ptIter = points.listIterator();
		while (ptIter.hasNext()){
			sum += ptIter.next().area;
		}
		return ((double)sum)/points.size();
		
	}
	
	/**
	 * Calculates the median area of the contours in the track
	 * @return Median area of the contours in the track
	 */
	public double medianArea(){
		if (points.isEmpty()) {
	        return 0;
	    }
		Vector<Double> areas = new Vector<Double>();
		ListIterator<TrackPoint> ptIter = points.listIterator();
		while (ptIter.hasNext()){
			areas.add((Double)ptIter.next().area);
		}
		Collections.sort(areas);
		return areas.get(areas.size()/2);
		
	}
	
	
	
	///////////////////////////
	// Accessors
	///////////////////////////	
	
	public int getTrackID(){
		return trackID;
	}
	
	public int getNumPoints(){
		return points.size();
	}
	
	public Vector<TrackPoint> getPoints(){
		return points;
	}
	
	public TrackPoint getPoint(int index){
		if (index<0|| index>points.size()){
			return null;
		} else {
			return points.get(index);
		}
	}

	public TrackPoint getFramePoint(int frame){
		if (frame<getStart().frameNum || frame>getEnd().frameNum){
			return null;
		} else {
			return points.get(frame-getStart().frameNum);
		}
	}
	
	public TrackPoint getStart(){
		return points.firstElement();
	}
	public TrackPoint getEnd(){
		return points.lastElement();
	}
	
	public void playMovie() {
		comm = new Communicator();
		playMovie(trackID, null);
		
		if (!comm.outString.equals("")) new TextWindow("PlayMovie Error", comm.outString, 500, 500); 
	}
	
	public void playMovie(MaggotDisplayParameters mdp) {
		playMovie(trackID, mdp);
	}
	
	public void playMovie(int labelInd, MaggotDisplayParameters mdp){
		
		if (tb!=null){
			tb.comm.message("This track has "+points.size()+"points", VerbLevel.verb_message);
		}
		ListIterator<TrackPoint> tpIt = points.listIterator();
		if (tpIt.hasNext()) {
		
			
			TrackPoint point = tpIt.next();
			point.setTrack(this);
			
			//Get the first image
			ImageProcessor firstIm;
			if (mdp!=null) {
				firstIm = point.getIm(mdp);
			} else{
				firstIm = point.getIm();
			}
			
			ImageStack trackStack = new ImageStack(firstIm.getWidth(), firstIm.getHeight());
			
			trackStack.addSlice(firstIm);
			
			//Add the rest of the images to the movie
			while(tpIt.hasNext()){
				point = tpIt.next();
				point.setTrack(this);
				
				//Get the next image
				ImageProcessor img;
				if (mdp!=null) {
					img = point.getIm(mdp);
				} else{
					img = point.getIm();
				}
				trackStack.addSlice(img);
			}
				
			//Show the stack
			ImagePlus trackPlus = new ImagePlus("Track "+trackID+": frames "+points.firstElement().frameNum+"-"+points.lastElement().frameNum ,trackStack);
			
			trackPlus.show();
			
		}
	}
	

	
	public String infoString(){
		String info = "";
		
		info += "Track "+trackID+": "+points.size()+" points,";
		if (points.size()!=0){
			info += " frames "+points.firstElement().frameNum+"-"+points.lastElement().frameNum;
		}
		
		for (int i=0; i<points.size(); i++){
			info += "\n Point "+i+": "+points.get(i).infoSpill();
		}
		
		return info;
		
	}
	
	public void setMatch(TrackMatch tm){
		match = tm;
	}
	
	public TrackMatch getMatch(){
		return match;
	}
	
	public int getNextIDNum(){
		return nextIDNum;
	}
	
	public boolean isCollisionTrack(){
		return false;
	}

	
	
	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		if (pw!=null) pw.println("Writing track "+trackID+"...");
		
		//Write the size in bytes in this track to disk
		try {
			if (pw!=null) pw.println("Getting track size");
			int nBytes = sizeOnDisk(pw);
			if (pw!=null) pw.println("Size on disk: "+nBytes+" bytes");
			if (nBytes>=0){
				if (pw!=null) pw.println("Writing Track size");
				dos.writeInt(nBytes);
			} else {
				if (pw!=null) pw.println("...Error getting size of track "+trackID+"; aborting save");
				return 3;
			}
		} catch (Exception e) {
			StringWriter sw = new StringWriter();
			PrintWriter pw2 = new PrintWriter(sw);
			e.printStackTrace(pw2);
			if (pw!=null) pw.println(sw.toString());
			if (pw!=null) pw.println("...Error writing size of track "+trackID+"; aborting save");
			return 3;
		}
		
		//Write the # of points in this track to disk
		try {
			if (pw!=null) pw.println("Writing #pts ("+points.size()+")");
			if (points.size()>=0){
				dos.writeInt(points.size());
			} else {
				if (pw!=null) pw.println("...Error getting # of points in track "+trackID+"; aborting save");
				return 2;
			}
		} catch (Exception e) {
			if (pw!=null) pw.println("...Error writing # of points in track "+trackID+"; aborting save");
			return 2;
		}
		
		//Write the points to disk
		try {
			if (pw!=null) pw.println("Writing points...");
			for (int i=0; i<points.size(); i++){
				if (points.get(i).toDisk(dos,pw)!=0){
					if (pw!=null) pw.println("...Error writing TrackPoint "+points.get(i).pointID+"; aborting save");
					return 1;
				}
				if (i==(points.size()-1)){
					pw.println("Last point in track "+trackID+" written");
				}
			}
			if (pw!=null) pw.println("...done writing points");
		} catch (Exception e) {
			if (pw!=null) pw.println("...Error writing points; aborting save");
			return 1;
		}
		
		if (pw!=null) pw.println("...Track Saved!");
		return 0;
	}

	
	private int sizeOnDisk(PrintWriter pw){
		
		//Add the size of the "# of points" field (32-bit integer)
		int size = Integer.SIZE/Byte.SIZE;
		
		//Add the size of each point
		for (int i=0; i<points.size(); i++){
			size += points.get(i).sizeOnDisk();
		}
		
		return size;
	}
	
	public static Track fromDisk(DataInputStream dis, int pointType, Experiment experiment, PrintWriter pw){
		
		Track tr = new Track();
		
		//load data
		if (tr.loadFromDisk(dis, pointType, experiment, pw)==0){
			return tr;
		} else {
			return null;
		}
	}
	
	private int loadFromDisk(DataInputStream dis, int pointType, Experiment experiment, PrintWriter pw){
		
		points = new Vector<TrackPoint>();
		exp = experiment;
		trackID=nextIDNum++;
		
		//advance past size on disk
		try {
			int size = dis.readInt();
			if (pw!=null) pw.println(size+" bytes to load...");
		} catch (Exception e) {
			if (pw!=null) pw.println("ERROR: Unable to advance past field 'size on disk'");
			return 4;
		}
		
		//Read nPts and then the points
		try {
			int nPts = dis.readInt();
			if (pw!=null) pw.println(nPts+" Points to load...");
			
			switch (pointType){
				case 0: 
					TrackPoint tp;
					for (int i=0; i<nPts; i++){
						//Load TrackPoints
						tp = TrackPoint.fromDisk(dis, this, pw);
						if (tp!=null) {
							points.addElement(tp);
						} else {
							if (pw!=null) pw.println("ERROR: null Trackpoint ("+i+"/"+(nPts-1)+")");
							return 3;
						}
					}
					break;
				case 1:
					ImTrackPoint itp;
					for (int i=0; i<nPts; i++){
						//Load ImTrackPoints
						itp = ImTrackPoint.fromDisk(dis, this, pw);
						if (itp!=null) {
							points.addElement(itp);
						} else {
							if (pw!=null) pw.println("ERROR: null ImTrackpoint ("+i+"/"+(nPts-1)+")");
							return 3;
						}
					}
					break;
				case 2:
					MaggotTrackPoint mtp;
					for (int i=0; i<nPts; i++){
						//Load MaggotTrackPoints
						mtp = MaggotTrackPoint.fromDisk(dis, this, pw);
						if (mtp!=null) {
							points.addElement(mtp);
						} else {
							if (pw!=null) pw.println("ERROR: null MaggotTrackpoint ("+i+"/"+(nPts-1)+")");
							return 3;
						}
					}
					break;
				case 3:
					BackboneTrackPoint btp;
					for (int i=0; i<nPts; i++){
						//Load BackboneTrackPoints
						btp = BackboneTrackPoint.fromDisk(dis, this, pw);
						if (btp!=null) {
							points.add(btp);
						} else {
							if (pw!=null) pw.println("ERROR: null BackboneTrackpoint ("+i+"/"+(nPts-1)+")");
							return 3;
						}
					}
					break;
				default:
					//Invalid point type
					if (pw!=null) pw.println("ERROR: Invalid Point type ("+pointType+")");
					return 2;
			}
			
			if (pw!=null) pw.println("...done loading points");
			
		} catch (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			if (pw!=null) pw.println("ERROR: Unable to load points\n"+sw.toString());
			return 1;
		}
		
		return 0;
	}
	
	/**
	 * Pre-Serializes all TrackPoints
	 */
	public void preSerialize(){
		ListIterator<TrackPoint> tpIt = points.listIterator();
		while (tpIt.hasNext()) {
			tpIt.next().preSerialize();
		}
	}
	
	/**
	 * Post-Deserializes all TrackPoints
	 */
	public void postDeserialize(){
		ListIterator<TrackPoint> tpIt = points.listIterator();
		while (tpIt.hasNext()) {
			tpIt.next().postDeserialize();
		}
	}
	
	
	
	
	
	
	
	
	protected static String makeDescription(String ID, Vector<TrackPoint> pointList, String addInfo){
		
		String lb = "\n";//System.lineSeparator();
		
		String d = "";
		d += "Track "+ID+lb+lb;
		
		if (pointList!=null){
			d += "Frames: ";
			d += (pointList.size()>0) ? pointList.firstElement().frameNum+"-"+pointList.lastElement().frameNum+lb+lb : "X-X"+lb+lb;
		}
		
		if(!addInfo.equals("")) d += addInfo+lb+lb;
		
		if (pointList!=null){
			d += "Points("+pointList.size()+"):"+lb;
			for (int i=0; i<pointList.size(); i++){
				TrackPoint pt = pointList.get(i);
				d += (i+1)+": "+pt.getTPDescription()+lb;
			}
		}else {
			d += "Frames: "+lb+lb;
			d += "Points(X):"+lb;
			d += "(point list)";
		}
		return d;
	}
	
	public String description(){
		return makeDescription(""+trackID, points, "");
	}
	
	public static String emptyDescription(){
		return makeDescription("X", null, "");
	}
	
	
	public void showEnergyPlot(){
		
		Plot plot = new Plot("Example plot", "Frame", "Energy");
		
		if (exp!=null && exp.getForces()!=null){
			
			//Get x coords
			float[] frames = new float[points.size()];
			int startframe = points.firstElement().frameNum;
			for (int i=0; i<frames.length; i++){
				frames[i] = startframe+i;
			}
			
			Vector<float[]> energies = new Vector<float[]>();
			for (int i=0; i<exp.getForces().size(); i++){
				
				float[] energy = new float[points.size()];
				for (int j=0; j<frames.length; j++){
					energy[j] = exp.getForces().get(i).getEnergy(j, points);;
				}
					
				energies.add(energy);
			}
			
			Color[] colors = {Color.WHITE, Color.MAGENTA,Color.GREEN, Color.CYAN, Color.RED};
			for (int i=0; i<exp.getForces().size(); i++){
				if (i<MaggotDisplayParameters.DEFAULTshowForce.length && MaggotDisplayParameters.DEFAULTshowForce[i])
				plot.setColor(colors[i]);
				plot.addPoints(frames, energies.get(i), Plot.LINE); 
				plot.draw();
			}
			
		}
		
		plot.show();
	}
	

	
		
}