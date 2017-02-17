package TrackExtractionJava;

//import java.awt.image.ColorModel;
//import java.util.Vector;

import ij.ImagePlus;
import ij.process.ImageProcessor;
//import ij.VirtualStack;

/**
 * (Rui: modified from TrackMovieVirtualStack)
 */
public class TrackDdtVirtualStack extends TrackMovieVirtualStack {

	private ImagePlus ddtimp = null;
	
	/*
	 * WIP: Constructors
	 */
	public TrackDdtVirtualStack(Track tr) {
		super(tr);
	}
	
	public ImagePlus getImagePlus () {
		if (null == ddtimp){
			ddtimp = new ImagePlus("Track "+tr.getTrackID()+": frames "+tr.points.firstElement().frameNum+"-"+tr.points.lastElement().frameNum,this);
		}
		return ddtimp;
	}
	
	public void updateImage() {
		if (ddtimp == null) { return;}
		ddtimp.setProcessor(getProcessor(ddtimp.getCurrentSlice()));
		ddtimp.updateAndDraw(); //updateAndRepaintWindow is another option if this doesn't work
	}
	
	public boolean windowClosed() {
		return (ddtimp == null || ddtimp.getWindow() == null);
	}
	
	/*
	private void init () {
		btps = new Vector<BackboneTrackPoint>();
		for (TrackPoint tp : tr.points) {
			if (!(tp instanceof BackboneTrackPoint)) {continue;}
			BackboneTrackPoint btp = (BackboneTrackPoint) tp;
			btps.add(btp);
		}
//		Forces = new SimpleExtractionParameters().getFittingParameters().getForces(0);
	}
	*/
	
	/*
	public TrackDdtVirtualStack(Track tr) {
		this (tr, tr.getPoint(0).getPadImNew(1).getWidth(), tr.getPoint(0).getPadImNew(1).getHeight(), tr.getPoint(0).getPadImNew(1).getColorModel(), tr.exp.getFileName());
	}
	
	public TrackDdtVirtualStack(Track tr, MaggotDisplayParameters mdp) {
		this(tr, mdp, false);
	}
	
	public TrackDdtVirtualStack(Track tr) {
		this(tr, new MaggotDisplayParameters());
	}
	*/
	
	//Returns the ImageProcessor for the specified frame number
	//	Overrides the method in ImageStack
	//	Ensures that the frame is in the current mmfStack, and then gets the image through CommonBackgroundStack methods

	public ImageProcessor getProcessor (int frameNumber) {
		
		int history = -1;
		if (super.showFitHistory) {
			history = ((int) (frameNumber/tr.getNumPoints()));
			frameNumber =  frameNumber - tr.getNumPoints()*((int) (frameNumber/tr.getNumPoints()));
		}
		int zoomFac = super.getMaggotDisplayParameters().getExpandFac();
		
		TrackPoint tp = tr.getPointCoerced(frameNumber-1);
		
		if (tp != null) {
			/*
			if (tp instanceof BackboneTrackPoint && (mdp.forces || showFitHistory)) {
				BackboneTrackPoint btp = (BackboneTrackPoint) tp;
				if (null != btp) {
					btp.setTargetBackbones(Forces, btps, history);
				}
			}
			return tp.getIm(mdp);
			*/
			return tp.getPadImNew(1,1,zoomFac); // note: if TrackPoint, will return null too
		} else {
			return null;
		}
	}

}

