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
	
	public TrackMovieVirtualStack(Track tr, MaggotDisplayParameters mdp, int width, int height, ColorModel cm, String path) {
		super(width, height, cm, path);
		this.tr = tr;
		this.mdp = mdp;
		init();
	}
	
	public ImagePlus getImagePlus () {
		if (null == imp){
			imp = new ImagePlus("Track "+tr.getTrackID()+": frames "+tr.points.firstElement().frameNum+"-"+tr.points.lastElement().frameNum ,this);
		}
		return imp;
	}
	
	public MaggotDisplayParameters getMaggotDisplayParameters() {
		return mdp;
	}

	public void setMaggotDisplayParameters(MaggotDisplayParameters mdp) {
		this.mdp = mdp;
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
		Forces = new SimpleExtractionParameters().getStraightFittingParameters().getForces(0);
	}
	
	
	public TrackMovieVirtualStack(Track tr, MaggotDisplayParameters mdp) {
		this (tr, mdp, tr.getPoint(0).getIm(mdp).getWidth(), tr.getPoint(0).getIm(mdp).getHeight(), tr.getPoint(0).getIm(mdp).getColorModel(), tr.exp.getFileName());
		
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
		TrackPoint tp = tr.getPointCoerced(frameNumber-1);
		if (tp != null) {
			if (mdp.forces) {
				BackboneTrackPoint btp = (BackboneTrackPoint) tp;
				if (null != btp) {
					btp.setTargetBackbones(Forces, btps);
				}
			}
			return tp.getIm(mdp);
		} else {
			return null;
		}
	}
	
	public int getSize() {
		return tr.getNumPoints();
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
