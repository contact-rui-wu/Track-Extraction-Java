import ij.IJ;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ListIterator;
import java.util.Vector;


public class Experiment implements Serializable{

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * Name of the original file? or the .ser file? 
	 */
	public String fname;
	/**
	 * The extraction parameters used to extract this experiment
	 */
	public ExtractionParameters ep;
	/**
	 * List of IDs that contain CollisionTracks
	 */
	Vector<Integer> collisionTrackIDs;
	/**
	 * List of tracks contained within the experiment
	 */
	public Vector<Track> tracks;
	/**
	 * 
	 */
	Vector<Force> Forces;
	
	public Experiment(){
		
	}
	
	public Experiment(String filename){
		try {
			fname = filename;
			ep = new ExtractionParameters();
//			Forces = ;
			
			//TODO use experiment_Processor functions
			DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(filename))));
			
			loadFromDisk(dis, new PrintWriter(System.out));
			System.out.println("Experiment loaded; "+tracks.size()+" tracks"); 
		} catch (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			System.out.println(sw.toString());
		}
	}
	
	/**
	 * Constructs an Experiment object
	 * @param fname
	 * @param ep
	 * @param collisionTrackIDs
	 * @param tracks
	 */
	public Experiment(String fname, ExtractionParameters ep, Vector<Integer> collisionTrackIDs, Vector<Track> tracks) {
		init(fname, ep, collisionTrackIDs, tracks);
	}
	
	public Experiment(TrackBuilder tb){
		init("", tb.ep, tb.finishedColIDs, tb.finishedTracks);
	}
	
	
	public void init(String fname, ExtractionParameters ep, Vector<Integer> collisionTrackIDs, Vector<Track> tracks) {
		this.fname = fname;
		this.ep = ep;
		this.collisionTrackIDs = collisionTrackIDs;
		this.tracks = tracks;
	}
	
	@SuppressWarnings("unchecked")
	public Experiment(Experiment exOld){
		init(exOld.fname, exOld.ep, exOld.collisionTrackIDs, (Vector<Track>)exOld.tracks.clone());
		Forces = exOld.Forces;
	}

	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		
		if (tracks.size()==0){
			if (pw!=null) pw.println("No tracks in experiment; save aborted"); 
			return 4;
		}
		
		if (pw!=null) pw.println("Saving experiment to disk...");
		
		
		//Write the Experiment Type
		try {
			int code = getTypeCode();
			if (code>=0){
				if (pw!=null) pw.println("Writing type code ("+code+")");
				dos.writeInt(code);
			} else {
				if (pw!=null) pw.println("Invalid experiment code; save aborted");
				return 3;
			}
		} catch (Exception e) {
			if (pw!=null) pw.println("...Error writing experiment type code; save aborted");
			return 3;
		}
		
		//Write the # of tracks
		try {
			if (pw!=null) pw.println("Writing # of tracks ("+tracks.size()+")");
			dos.writeInt(tracks.size());
		} catch (Exception e) {
			if (pw!=null) pw.println("...Error writing # of tracks; save aborted");
			return 2;
		}
		
		//Write each track
		try {
			if (pw!=null) pw.println("Writing Tracks");
			
			for (int j=0; j<tracks.size(); j++){
				Track tr = tracks.get(j);
				if (pw!=null) pw.println("Writing track number "+j+"("+tr.trackID+")");
				if(tr.toDisk(dos,pw)!=0) {
					if (pw!=null) pw.println("...Error writing track "+tr.trackID+"; save aborted");
					return 1; 
				}
			}
			
		} catch (Exception e) {
			if (pw!=null) pw.println("\n...Error writing tracks; save aborted");
			return 1;
		}
		
		try{
			dos.writeInt(0);
		} catch (Exception e){
			if (pw!=null) pw.println("\n...Error writing end of file; save aborted");
			return 1;
		}
		
		if (pw!=null) pw.println("\n...Experiment Saved!");
		return 0;
	}
	
	public int getTypeCode(){
		int trackType = -1;
		
		for (int i=0; (trackType<0 && i<tracks.size()); i++){
			if(tracks.get(i).points.size()>0){
				
				if (tracks.get(i).points.firstElement() instanceof BackboneTrackPoint){
					return 3;
				} else if (tracks.get(i).points.firstElement() instanceof MaggotTrackPoint){
					return 2;
				} else if (tracks.get(i).points.firstElement() instanceof ImTrackPoint){
					return 1;
				} else if (tracks.get(i).points.firstElement() instanceof TrackPoint){
					return 0;
				}
			}
		}
		
		return trackType;
	}
	
	public static Experiment fromDisk(DataInputStream dis, String filename, ExtractionParameters exParam, FittingParameters fp, PrintWriter pw){
		
		
		try {
			Experiment ex = new Experiment();
			if (pw!=null) pw.println("Setting parameters");
			ex.fname = filename;
			ex.ep = exParam;
			ex.Forces = fp.getForces(0);
			if (pw!=null) pw.println("Loading from Disk... ");
			ex.loadFromDisk(dis, pw);
			if (pw!=null) pw.println("...load from disk complete");
			return ex;
		} catch (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			if (pw!=null) pw.println("...error loading experiment from disk:\n"+sw.toString());
			return null;
		}
		
	}
	
	private void loadFromDisk(DataInputStream dis, PrintWriter pw){
		System.out.println("Loading from disk...");
		int progress = -2;
		try{

			System.out.println("Loading from disk...");
			//Read the Experiment Type
			int tpType = dis.readInt();
			if (pw!=null) pw.println("==> trackpoint type "+tpType);
			progress++;//=-1
			
			//Read the # of tracks
			int numTracks = dis.readInt();
			if (pw!=null) pw.println("==> "+numTracks+" tracks");
			tracks = new Vector<Track>();
			progress++;//=0
			
			//Read each track
			progress = 0;
			Track nextTrack;
			for (int i=0; i<numTracks; i++){

				if (pw!=null) pw.println("==> Track "+i+"/"+(numTracks-1));
				nextTrack = Track.fromDisk(dis, tpType, this, pw);
				if (nextTrack==null) {
					if (pw!=null) pw.println("(null)");
					return;
				}
				tracks.add(nextTrack);
				
				progress++;//= # of tracks loaded
				
				//TODO ask for garbage collection
			}

			System.out.println("...done loading!");
			
		} catch (Exception e){
			if (pw!=null) pw.println("Error: progress code "+progress);
			System.out.println("...Error loading");
			return;
		}
		
			
	}
	
	
	/**
	 * Saves this Experiment in the specified dir+filename
	 * @param dir The directory in which to save the file(; if empty, saves in current directory?)
	 * @param filename The file name including the extension
	 */
	public void serialize(String dir, String filename){
		
		//TODO CHECK THE DIR/FILENAME
		fname = dir+File.separator+filename;
		File f = new File(fname);
		
		//Pre-serialize the tracks 
		IJ.showStatus("PreSerializing...");
		ListIterator<Track> trIt = tracks.listIterator();
		while (trIt.hasNext()){
			trIt.next().preSerialize();
		}
		
		//Serialize the Experiment
		IJ.showStatus("Writing objects to file");
		try {
			
			FileOutputStream fo = new FileOutputStream(f);
			ObjectOutputStream oo = new ObjectOutputStream(fo);
			
			oo.writeObject(this);
			
			oo.close();
			fo.close();
			IJ.showStatus("Done writing objects to file");
			
		} catch (Exception e){
			IJ.showStatus("Error writing objects to file");
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			IJ.showMessage("Error saving experiment:\n"+sw.toString());
			return;
		}
		
		
		
	}
	
	
	/**
	 * Opens a serialized Experiment 
	 * @param fname
	 * @return
	 * @throws Exception 
	 */
	public static Experiment deserialize(String fname) throws Exception{
		
		Experiment ex; 
		
		//TODO Check the extension
		File f = new File(fname);
			
		//Deserialize the experiment
		FileInputStream fi = new FileInputStream(f);
		ObjectInputStream oi = new ObjectInputStream(fi);
		ex = (Experiment) oi.readObject();
		oi.close();
		fi.close();
		
		//PostDeserialize the tracks
		ListIterator<Track> trIt = ex.tracks.listIterator();
		while (trIt.hasNext()){
			trIt.next().postDeserialize();
		}
		
		return ex;
	}
	
	public String getFileName(){
		return fname;
	}
	
	
	
	public int getTrack(int trackNum){
		
		ListIterator<Track> trIt = tracks.listIterator();
		while(trIt.hasNext()){
			if(trIt.next().trackID==trackNum) return trIt.previousIndex();
		}
		return -1;
	}
	
	
	
	public void setForces(Vector<Force> Forces){
		this.Forces = Forces;
	}
	
	
	public void replaceTrack(Track newTrack, int ind){
		tracks.setElementAt(newTrack, ind);
	}
	
}
