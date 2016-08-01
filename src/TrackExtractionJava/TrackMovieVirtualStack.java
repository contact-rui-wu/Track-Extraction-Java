package TrackExtractionJava;

import java.awt.image.ColorModel;
import java.util.Vector;

import ij.ImagePlus;
import ij.VirtualStack;
import ij.process.ImageProcessor;

public class TrackMovieVirtualStack extends VirtualStack {

	private Track tr;
	private MaggotDisplayParameters mdp;
	private Vector<BackboneTrackPoint> btps;
	private Vector<Force> Forces;
	
	
	


	//private int imWidth;
	//private int imHeight;
	private ImagePlus imp = null;
	private boolean showFitHistory = false;
	
	public TrackMovieVirtualStack(Track tr, MaggotDisplayParameters mdp, boolean showFitHistory, int width, int height, ColorModel cm, String path) {
		super(width, height, cm, path);
		this.tr = tr;
		this.mdp = mdp;
		this.showFitHistory = showFitHistory && tr.getBackboneHistoryLength() > 0;
		init();
	}
	
	public ImagePlus getImagePlus () {
		if (null == imp){
			imp = new ImagePlus("Track "+tr.getTrackID()+": frames "+tr.points.firstElement().frameNum+"-"+tr.points.lastElement().frameNum ,this);
		}
		if (showFitHistory) {
			int t = tr.getBackboneHistoryLength();
			int z = tr.getNumPoints();
			if (t > 0 && z > 0)  {
//				imp = HyperStackConverter.toHyperStack(imp, 1,z, t);
				imp.setDimensions(1, z, t);
				imp.setOpenAsHyperStack(true);
			}
		}
		return imp;
	}
	
	public MaggotDisplayParameters getMaggotDisplayParameters() {
		return mdp;
	}

	public void setMaggotDisplayParameters(MaggotDisplayParameters mdp) {
		this.mdp = mdp;
		updateImage();
	}
	public Vector<Force> getForces() {
		return Forces;
	}

	public void setForces(Vector<Force> forces) {
		Forces = forces;
		updateImage();
	}
	public void updateImage() {
		if (imp == null) { return;}
		imp.setProcessor(getProcessor(imp.getCurrentSlice()));
		imp.updateAndDraw(); //updateAndRepaintWindow is another option if this doesn't work

	}
	public boolean windowClosed() {
		return (imp == null || imp.getWindow() == null);
	}
	
	private void init () {
		btps = new Vector<BackboneTrackPoint>();
		for (TrackPoint tp : tr.points) {
			BackboneTrackPoint btp = (BackboneTrackPoint) tp;
			if (btp == null) { continue; }
			btps.add(btp);
		}
		Forces = new SimpleExtractionParameters().getFittingParameters().getForces(0);
	}
	
	
	public TrackMovieVirtualStack(Track tr, MaggotDisplayParameters mdp, boolean showFitHistory) {
		this (tr, mdp, showFitHistory, tr.getPoint(0).getIm(mdp).getWidth(), tr.getPoint(0).getIm(mdp).getHeight(), tr.getPoint(0).getIm(mdp).getColorModel(), tr.exp.getFileName());
		
	}
	public TrackMovieVirtualStack(Track tr, MaggotDisplayParameters mdp) {
		this(tr, mdp, false);
	}
	public TrackMovieVirtualStack(Track tr) {
		this(tr, new MaggotDisplayParameters());
	}
	
	
	/**
	 * Does nothing
	 */
	public void addSlice(String name){
		return;
	}
	
	/**
	 * Does nothing
	 */
	public void deleteLastSlice(){
		return;
	}
	
	/**
	 * Does nothing
	 */
	public void deleteSlice(int n){
		return;
	}
	
	public int getBitDepth(){
		return getProcessor(0).getBitDepth();
	}

	public String getDirectory(){
		return "";
	}
	
	/**
	 * Returns the file name (not including the directory)
	 */
	public String getFileName(){
		return "";
	}

	//Returns the ImageProcessor for the specified frame number
	//	Overrides the method in ImageStack
	//	Ensures that the frame is in the current mmfStack, and then gets the image through CommonBackgroundStack methods

	public ImageProcessor getProcessor (int frameNumber) {
		int history = -1;
		if (showFitHistory) {
			history = ((int) (frameNumber/tr.getNumPoints()));
			frameNumber =  frameNumber - tr.getNumPoints()*((int) (frameNumber/tr.getNumPoints()));
		}
		TrackPoint tp = tr.getPointCoerced(frameNumber-1);
		
		if (tp != null) {
			if (mdp.forces || showFitHistory) {
				BackboneTrackPoint btp = (BackboneTrackPoint) tp;
				if (null != btp) {
					btp.setTargetBackbones(Forces, btps, history);
				}
			}
			return tp.getIm(mdp);
		} else {
			return null;
		}
	}
	
	public int getSize() {
		if (showFitHistory && tr.getBackboneHistoryLength() > 0) {
			return tr.getNumPoints()*tr.getBackboneHistoryLength();
		} else {
			return tr.getNumPoints();
		}
	}
	
	
	public String getSliceLabel(int n){
		TrackPoint tp = tr.getPointCoerced(n-1);
		if (tp == null) {
			return "no point " + n;
		}
		
		String label = "Pt# " + n + "/" + tr.getNumPoints() + tp.infoSpill();
		return label;
	}
	
	

}
