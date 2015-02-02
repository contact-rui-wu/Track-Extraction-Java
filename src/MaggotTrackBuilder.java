import ij.ImageStack;


public class MaggotTrackBuilder extends TrackBuilder {

	public MaggotTrackBuilder(ImageStack IS, ExtractionParameters ep) {
		super(IS, ep);
	}
	
	/**
	 * Builds the tracks with MaggotTrackPoints 
	 */
	public void run(){
		
		ep.trackPointType=2;//Make sure the track is loaded as MaggotTrackPoints
		buildTracks();
		orientMaggots();
	}

	/**
	 * Orients all the tracks so that all maggots have their head in the direction of motion
	 */
	protected void orientMaggots(){
		for (int i=0; i<finishedTracks.size(); i++){
			orientMaggotTrack(finishedTracks.get(i));  
		}
	} 

	/**
	 * Orients the maggots in a track in the direction of their motion
	 * @param track Track to be oriented
	 */
	protected void orientMaggotTrack(Track track){
//		if (ep.trackPointType!=2){
//			//Load points as MaggotTrack points
//			
//		}
		
		if (track.points!=null && track.points.size()>0 ){//&& track.points.get(0).pointType>=2
			
			MaggotTrackPoint pt;
			MaggotTrackPoint prevPt = (MaggotTrackPoint)track.points.get(0);
			int lastEndFrameAnalyzed=-1;
//			int lastValidFrame = (prevPt.midline!= null && prevPt.midline.getNCoordinates()!=0) ? 0 : -1;
			
			int AMDSegStart = (prevPt.midline!= null && prevPt.midline.getNCoordinates()!=0) ? 0 : -1;
			int AMDSegEnd = -1;
			
			for (int i=1; i<track.points.size(); i++){
			
				pt = (MaggotTrackPoint)track.points.get(i);
				
				//WHEN THE SPINE IS VALID
				if (pt.midline!= null && pt.midline.getNCoordinates()!=0) {//WHEN THE SPINE IS VALID 
					//If a midline exists, align it with the last valid point
					
					int orStat = pt.chooseOrientation(prevPt);
					if (orStat<0){
						comm.message("Orientation Failed, frame "+(i+ep.startFrame), VerbLevel.verb_error);
					}
					prevPt = pt;
					
					//Set the new start frame when the last segment has just been analyzed
					//	->Because of initialization, this will happen on the first valid spine in the track
					if (AMDSegEnd==lastEndFrameAnalyzed){//if (AMDSegStart==AMDSegEnd){
						AMDSegStart=i;
					}
					//Always set this as the last frame of the ending segment
					AMDSegEnd=i;
					
					
				} else  { 
					//When the midline doesn't exist, analyze the previous segment of midlines
					//But only if that segment has at least 2 points
					
					comm.message("Midline invalid, Track "+track.trackID+" frame "+(i+ep.startFrame), VerbLevel.verb_error);
					
					//Analyze the direction of motion for the segment leading up to this frame, starting with lastEndFrameAnalyzed (or 0)
					if ( (AMDSegEnd-AMDSegStart)>1 ){ //TODO && (AMDSegEnd-AMDSegStart)<AMDSegEnd (?)
						analyzeMaggotDirection(track, AMDSegStart, AMDSegEnd);
					}
					
					//Regardless, acknowledge the empty spine so that the next valid segment is analyzed correctly 
					lastEndFrameAnalyzed=AMDSegEnd;
					
				}
			}
			
			//Catch the case when there were no gaps, so the direction was never analyzed
			if (lastEndFrameAnalyzed==-1){
				//Analyze the direction of motion for the whole track
				analyzeMaggotDirection(track, AMDSegStart, AMDSegEnd);
			}
			
			
		} else {
			comm.message("Track was not oriented", VerbLevel.verb_error);
		}
		
		
		
	}
	
	/**
	 * Checks if the segment of MaggotTrackPoints is oriented in the direction of motion
	 * @param track Track to be oriented
	 * @param startInd Starting INDEX (not frame) to be oriented
	 * @param endInd Ending  INDEX (not frame) to be oriented
	 */
	protected void analyzeMaggotDirection(Track track, int startInd, int endInd){
		
		if (track.points.isEmpty() || startInd<0 || endInd<0 || startInd>=endInd){
			track.tb.comm.message("Direction Analyisis Error: Track has "+track.points.size()+" points, startInd="+startInd+", endInd="+endInd, VerbLevel.verb_message);
			return;
		}
		
		comm.message("Analyzing midline direction: Track "+track.trackID+" "+(startInd+ep.startFrame)+"-"+(endInd+ep.startFrame), VerbLevel.verb_error);
		
		double dpSum=0;
		MaggotTrackPoint pt;
		MaggotTrackPoint prevPt = (MaggotTrackPoint) track.points.get(startInd);
		for (int i=startInd+1; i<=endInd; i++){
			pt = (MaggotTrackPoint) track.points.get(startInd);
			dpSum += pt.MaggotDotProduct(prevPt);
			prevPt = pt;
		} 
		
		
		if (dpSum<0){
			flipSeg(track, startInd, endInd);
		}
		
		
	}
	
	/**
	 * Flips the orientation of every maggot head/tail/midline in the segment
	 * @param track
	 * @param startInd
	 * @param endInd
	 */
	protected void flipSeg(Track track, int startInd, int endInd) {
		for (int i=startInd; i<=endInd; i++){
			MaggotTrackPoint pt = (MaggotTrackPoint) track.points.get(i);
			pt.flipHT();
		}
	}
	
	
	
}