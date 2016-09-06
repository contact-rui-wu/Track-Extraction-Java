package TrackExtractionJava;

import ij.gui.PolygonRoi;
import ij.process.FloatPolygon;
import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Vector;


/**
 * A TrackPoint containing an image, contour, midline, and backbone
 * @author Natalie
 *
 */
public class BackboneTrackPoint extends MaggotTrackPoint{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * Identifies the point as a BACKBONETRACKPOINT
	 */
	final static int pointType = 3;
	 
	/**
	 * minimum total movement to call an update on the clustering
	 * note: uses distance, not squared distance
	 */
	private static final double minShiftToUpdateClusters = 0.1;
	
	/**
	 * The number of pixels in the image considered as part of the maggot
	 */
	private transient int numPix;
	/**
	 * A list of X Coordinates of points that are considered as part of the maggot 
	 * <p>
	 * Coordinates are *Absolute* coordinates, not coords relative to image frame
	 * <p>
	 * Contains numPix valid elements
	 */
	private transient float[] MagPixX;
	/**
	 * A list of Y Coordinates of points that are considered as part of the maggot  
	 * <p>
	 * Coordinates are *Absolute* coordinates, not coords relative to image frame
	 * <p>
	 * Contains numPix valid elements  
	 */
	private transient float[] MagPixY;
	/**
	 * A list of Intensity values (0;255) corresponding to the MagPix points 
	 * <p>
	 * Contains numPix valid elements
	 */
	private transient int[] MagPixI;
	
	private transient double[][] MagPixWold;
	private transient double[][] MagPixWnew;
	
	/**
	 * A list of cluster indices (0;numBBpoints-1) corresponding to the nearest bbOld point to each MagPix point 
	 * <p>
	 * Contains numPix valid elements
	 */
	private transient int[] clusterInds;
	
	private transient double cumulativeShift = 0;
	
	/**
	 * The pixel clustering method used by the backbone fitter
	 */
	private int clusterMethod=0;
	
	/**
	 * The number of points in the backbone
	 */
	private int numBBPts;
	
	private transient double gmmClusterVariance = -1;
	
	/**
	 * The final backbone of the maggot
	 * <p>
	 * NOTE: during backbone fitting, this is not set; instead use bbOld/bbNew
	 */
	PolygonRoi backbone;
	
	/**
	 * The BackboneFitter's initial guess for the backbone. Absolute coordinates.
	 * <p>
	 * (Used for plotting)
	 */
	private transient FloatPolygon bbInit;
	/**
	 * Temporary backbone used to fit the final backbone. Absolute coordinates.
	 * <p>
	 * In the backbone-fitting algorithm, bbOld stores the previous iteration's backbone, and 
	 * is used for all calculations of new backbones  
	 */
	protected transient FloatPolygon bbOld = null;
	/**
	 * Temporary backbone used to fit the final backbone. Absolute coordinates.
	 * <p>
	 * In the backbone fitting algorithm, bbNew stores the current iteration's backbone
	 */
	protected transient FloatPolygon bbNew  = null;
	
	/**
	 * The target backbones used to generate the last backbone. Absolute coordinates. Used in track.showFitting.
	 * Not saved to disk. 
	 */
	protected transient Vector<FloatPolygon> targetBackbones = null;
	
	/**
	 * The backbone fitter used to fit this btp. Present primarily for access to bf.communicator
	 * Not saved to disk.
	 */
	transient BackboneFitter bf;
	
	/**
	 * Flag indicating whether or not the midline (i.e. the one generated during extraction) was  
	 * used as the initial backbone guess. True means the midline was replaced before calculating a 
	 * backbone.
	 */
	protected boolean artificialMid;

	/**
	 * Flag indicating whether or not this backbone is considered a potentially incorrect representation 
	 * of the larva's posture. 
	 * <p>
	 * Calculated in the backbone fitter after all fitting has occurred 
	 */
	public boolean suspicious = false; 
	
	
	/**
	 * Flag indicating whether or not this btp is used in calculations for neighboring btp's backbone
	 */
	protected boolean hidden = false;
	
	/**
	 * Flag indicating whether or not this backbone is to be updated in the current iteration of the backbone fitter
	 */
	protected boolean frozen = false;
	
	/**
	 * Relatively unused currently; put in place to modulate the step size by which bbOld is adjusted to 
	 * create bbNew. 
	 * <p>
	 * See BackboneFitter.bbRelaxationStep 
	 */
	protected double scaleFactor = 1;
	
	
	
	private Vector<FloatPolygon> bbHistory = null;
	private Vector<String> historyLabel = null;
	
	private boolean inHistory = false; //to say whether we are displaying images back in time
	private String historyMessage;
	
	//protected FloatPolygon bbFromMidline = null;
	
	
	public BackboneTrackPoint(){
		
	}
	
	/**
	 * Constructs a BackboneTrackPoint
	 * @param x
	 * @param y
	 * @param rect
	 * @param area
	 * @param frame
	 * @param thresh
	 */
	public BackboneTrackPoint(double x, double y, Rectangle rect, double area,
			int frame, int thresh, int numBBPts) {
		super(x, y, rect, area, frame, thresh);
		this.numBBPts = numBBPts; 
	}
	
	

	/**
	 * Creates a new BackboneTrackPoint by copying the info in a MaggotTrackPoint
	 * @param mtp The MagggotTrackPoint to be copied
	 * @param numBBPts The number of points in the new Backbone
	 * @return The new BackboneTrackPoint
	 */
	public static BackboneTrackPoint convertMTPtoBTP(MaggotTrackPoint mtp, int numBBPts){
		
		//Copy all old info
		BackboneTrackPoint btp = new BackboneTrackPoint(mtp.x, mtp.y, mtp.rect, mtp.area, mtp.frameNum, mtp.thresh, numBBPts);
		mtp.copyInfoIntoBTP(btp);

		//Make new stuff
//		btp.numBBPts = numBBPts; 
		
		return btp;
	}
	
	/*
	private void storeMidlineAsBB() {
		FloatPolygon newMid = midline.getFloatPolygon();
		float[] xmid = new float[newMid.npoints];
		float[] ymid = new float[newMid.npoints];
		
		//Gather absolute coordinates for the backbones
		for(int i=0; i<newMid.npoints; i++){
			xmid[i] = newMid.xpoints[i]+rect.x;
			ymid[i] = newMid.ypoints[i]+rect.y;
		}
		FloatPolygon initBB = new FloatPolygon(xmid, ymid);//midline.getFloatPolygon();
		bbFromMidline = MaggotTrackPoint.getInterpolatedSegment(new PolygonRoi(initBB, Roi.POLYLINE), numBBPts).getFloatPolygon();
		
	}*/
	
	/**
	 * Resets the backbone info, marking the point with the artificialMid flag
	 * See also: setBackboneInfo 
	 * @param clusterMethod
	 * @param newMidline
	 * @param prevOrigin
	 */
	protected void fillInBackboneInfo(int clusterMethod, PolygonRoi newMidline, float[] prevOrigin){
		artificialMid = true;
		setBackboneInfo(clusterMethod, newMidline, prevOrigin);
	}
	
	/**
	 * Sets initial backbone guess, complete with pixel/cluster info. All info stored as absolute coordinates
	 * @param clusterMethod
	 * @param newMidline
	 * @param prevOrigin
	 */
	protected void setBackboneInfo(int clusterMethod, PolygonRoi newMidline, float[] prevOrigin){
		
		if(newMidline!=null){
			
			//Correct the origin of the midline
			FloatPolygon newMid = newMidline.getFloatPolygon();
			float[] xmid = new float[newMid.npoints];
			float[] ymid = new float[newMid.npoints];
			
			//Gather absolute coordinates for the backbones
			for(int i=0; i<newMid.npoints; i++){
				xmid[i] = newMid.xpoints[i]+prevOrigin[0];
				ymid[i] = newMid.ypoints[i]+prevOrigin[1];
			}
			FloatPolygon initBB = new FloatPolygon(xmid, ymid);//midline.getFloatPolygon();
			
			
			if (this.clusterMethod!=clusterMethod){
				this.clusterMethod=clusterMethod;
			}
			setInitialBB(new PolygonRoi(initBB, PolygonRoi.POLYLINE), numBBPts);
			setMagPix();
			setInitialClusterInfo(); 
			
		}
	}
	
	
	/**
	 * Sets bbOld to the initial backbone guess
	 * @param initBB The initial guess, does not need to contain the correct number of points
	 * @param numPts The correct number of backbone points
	 */
	private void setInitialBB(PolygonRoi initBB, int numPts){
		
		if(bf!=null){
			bf.comm.message("Setting initial Backbone", VerbLevel.verb_debug);
		}
		numBBPts = numPts;
		backbone = initBB;
		if (initBB.getNCoordinates()!=numBBPts){
			if(bf!=null){
				bf.comm.message("initBB has "+initBB.getNCoordinates()+" coords; Interpolating segment", VerbLevel.verb_debug);
			}
			initBB = MaggotTrackPoint.getInterpolatedSegment(initBB, numPts, true);
		}
		if(bf!=null){
			if (initBB!=null){
				bf.comm.message("initBB sucessful", VerbLevel.verb_debug);
			} else{
				bf.comm.message("initBB interpolation failed", VerbLevel.verb_debug);
			}
			
		}
		if (bbInit==null){
			bbInit = initBB.getFloatPolygon();
		}
		bbOld = initBB.getFloatPolygon();
		
	}
	
	/**
	 * Complies lists of locations and intensity values for the points considered to be part of the maggot
	 */
	private int setMagPix(){
		if (im==null){
			return -1;
		}
		
		if(comm!=null){
			comm.message("Setting MagPix", VerbLevel.verb_debug);
		}
		if(bf!=null){
			bf.comm.message("Setting MagPix", VerbLevel.verb_debug);
		}
		
		
		ImageProcessor maskIm = getMask();
		
		MagPixX = new float[maskIm.getWidth()*maskIm.getHeight()];
		MagPixY = new float[maskIm.getWidth()*maskIm.getHeight()];
		MagPixI = new int[maskIm.getWidth()*maskIm.getHeight()];
		
		numPix = 0;
		for(int X=0; X<maskIm.getWidth(); X++){
			for (int Y=0; Y<maskIm.getHeight(); Y++){
				if(maskIm.getPixel(X,Y)>thresh && im.getPixel(X, Y)>thresh){
					MagPixX[numPix] = 0.5f+X+rect.x;
					MagPixY[numPix] = 0.5f+Y+rect.y;
					MagPixI[numPix] = im.getPixel(X, Y);
					
					numPix++;
				}
			}
		}
		MagPixX = Arrays.copyOfRange(MagPixX, 0, numPix);
		MagPixY = Arrays.copyOfRange(MagPixY, 0, numPix);
		MagPixI = Arrays.copyOfRange(MagPixI, 0, numPix);
		
		
		if(comm!=null){
			comm.message("Number of MagPix: "+numPix, VerbLevel.verb_debug);
		}
		
		if (numPix==0){
			return 0;
		}
		return 1;
		
	}
		
	private void setInitialClusterInfo(){
		clusterInds = new int[numPix];
		if (clusterMethod==0){
			setVoronoiClusters();
		} else if (clusterMethod==1){
			MagPixWold = new double[numBBPts][numPix];
			MagPixWnew = new double[numBBPts][numPix];
			setInitialWeights();
		}
	}
	
	private void setClusterInfo(){
		if (clusterMethod==0){
			setVoronoiClusters();
		} else if (clusterMethod==1){
			setGaussianMixtureWeights();
		}
	}
	
	
	/**
	 * Finds the nearest backbone point to each maggot pixel, stores index of bbOld for each pixel in clusterInds
	 */
	private void setVoronoiClusters(){
		
		if(bf!=null){
			bf.comm.message("Setting Voronoi clusters", VerbLevel.verb_debug);
		}
		
		//Assign cluster info 
		for (int pix=0; pix<numPix; pix++){
			
			double minDistSqr = java.lang.Double.POSITIVE_INFINITY;
			int minInd = -1;
			double distSqr;
			for(int cl=0; cl<numBBPts; cl++){
				
				distSqr = ((double)MagPixX[pix]-bbOld.xpoints[cl])*((double)MagPixX[pix]-bbOld.xpoints[cl]);
				distSqr+= ((double)MagPixY[pix]-bbOld.ypoints[cl])*((double)MagPixY[pix]-bbOld.ypoints[cl]);
				if(distSqr<minDistSqr){
					minDistSqr = distSqr;
					minInd = cl;
				}
				
			}
			if (minInd==-1) {
				bf.comm.message("Voronoi clusters went unset in frame "+frameNum, VerbLevel.verb_error);
			}
			clusterInds[pix] = minInd;
		}
		
	}
	
	private void setInitialWeights(){
		//clusterInds = new int[numPix];
	//	setVoronoiClusters();
		//Set the initial weights to be the voronoi clusters
		for (int pix=0; pix<numPix; pix++){
			MagPixWold[ clusterInds[pix] ][pix] = 1;
		}
		//clusterInds = null;
		
		setGaussianMixtureWeights();//Sets MagPixWnew, which is used for generation of the backbone
	}
	
	private void setGaussianMixtureWeights(){
		Timer.tic("SetGaussianMixtureWeights");
		double var = (gmmClusterVariance <= 0) ? calcVariance() : gmmClusterVariance;
		setVoronoiClusters();
		for (int pix=0; pix<numPix; pix++){
			for (int cl=0; cl<numBBPts; cl++){
				MagPixWnew[cl][pix] = 0;
			}
		}
		for (int pix=0; pix<numPix; pix++){
			double denom = 0;
			for (int cl = clusterInds[pix] - 1; cl < numBBPts && cl <= clusterInds[pix]+1; cl++ ) {
				if (cl < 0){
					continue;
				}
				MagPixWnew[cl][pix] = Math.exp(((-0.5)*calcDistSqrBtwnPixAndBBPt(cl, pix))/var);
				denom += MagPixWnew[cl][pix];
			}
			for (int cl = clusterInds[pix] - 1; cl < numBBPts&& cl <= clusterInds[pix]+1; cl++ ) {
				if (cl < 0){
					continue;
				}
				MagPixWnew[cl][pix] /= denom;
			}
		}	
		Timer.toc("SetGaussianMixtureWeights");
	}
	
	private double calcVariance(){
		double numer = 0;
		double denom = 0;
		for (int pix=0; pix<numPix; pix++){
			denom += MagPixI[pix];
			for (int cl=0; cl<numBBPts; cl++){
				numer+=MagPixWold[cl][pix]*calcDistSqrBtwnPixAndBBPt(cl, pix)*MagPixI[pix];
			}
		}
		return numer/denom;
	}
	
	private double calcDistSqrBtwnPixAndBBPt(int clusterInd, int pixInd){
		//dist^2 = x^2...
		double distSqr = ((double)MagPixX[pixInd]-bbOld.xpoints[clusterInd])*((double)MagPixX[pixInd]-bbOld.xpoints[clusterInd]);
		//...+y^2
		distSqr+= ((double)MagPixY[pixInd]-bbOld.ypoints[clusterInd])*((double)MagPixY[pixInd]-bbOld.ypoints[clusterInd]);
		
		return distSqr;
	}

	
	/**
	 * Returns the sum of the distance squared between each backbone coordinate
	 * <p>
	 * Calculation is made between bbNew and bbOld 
	 * @return
	 */
	public double calcPointShift(){
		//Calculate the change between the old and new backbones
		if (frozen) {
			return 0;
		}
		
		double shift = 0;
		for(int i=0; i<numBBPts; i++){
			double xs = bbNew.xpoints[i]-bbOld.xpoints[i];
			double ys = bbNew.ypoints[i]-bbOld.ypoints[i];
			shift += (xs*xs)+(ys*ys);
		}		
		return shift;
	}
	private void updateCumulativeShift() {
		for(int i=0; i<numBBPts; i++){
			double xs = bbNew.xpoints[i]-bbOld.xpoints[i];
			double ys = bbNew.ypoints[i]-bbOld.ypoints[i];
			cumulativeShift += Math.sqrt((xs*xs)+(ys*ys));
		}
		
	}
	
	

	protected void setHidden(boolean h){
		hidden = h;
	}
	
	
	protected void setFrozen(boolean f){
		frozen = f;
	}
	
	/**
	 * Stores a working backbone
	 * @param newBackbone The updated backbone
	 */
	protected void setBBNew(FloatPolygon newBackbone){
		bbNew = newBackbone;
	}

	/**
	 * Preps the working variables for the next iteration of the fitting algorithm
	 */
	protected void setupForNextRelaxationStep(){
		if (frozen) {
			return;
		}
		Timer.tic("setupForNextRelaxationStep");
		updateCumulativeShift();
		bbOld = bbNew;
		if (cumulativeShift >= minShiftToUpdateClusters) {
			cumulativeShift = 0;
			MagPixWold = MagPixWnew;
			setClusterInfo();//fills in MagPixWnew using MagPixWold 
		}
		Timer.toc("setupForNextRelaxationStep");
	}
	
	/**
	 * Stores the final backbone, and resets head, tail and midpoint
	 */
	protected void finalizeBackbone(){
		if (bbOld!=null){
			backbone = new PolygonRoi(bbOld, PolygonRoi.POLYLINE); 
			
			head = new ContourPoint(bbOld.xpoints[0]-rect.x, bbOld.ypoints[0]-rect.y);
			tail = new ContourPoint(bbOld.xpoints[bbOld.npoints-1]-rect.x, bbOld.ypoints[bbOld.npoints-1]-rect.y);
			midpoint = new ContourPoint(bbOld.xpoints[bbOld.npoints/2]-rect.x, bbOld.ypoints[bbOld.npoints/2]-rect.y);
		}else{
			backbone = new PolygonRoi(new FloatPolygon(), PolygonRoi.POLYLINE);
			//h, t, and mid are left as they were in MTP
		}
	}
	
	/**
	 * 
	 * @param energies
	 */
	public void storeEnergies(HashMap<String, Double> energies){
		this.energies = energies;
	}
	
	/**
	 * Returns the previous point in the track
	 */
	public BackboneTrackPoint getPrev(){
		return (BackboneTrackPoint)prev;
	}
	
	/**
	 * Returns the next point in the track
	 */
	public BackboneTrackPoint getNext(){
		return (BackboneTrackPoint)next;
	}
	
	public int getNumPix(){
		return numPix;
	}
	
	public float getMagPixX(int ind){
		return MagPixX[ind];
	}

	public float getMagPixY(int ind){
		return MagPixY[ind];
	}
	
	public int getMagPixI(int ind){
		return MagPixI[ind];
	}
	
	public int getClusterMethod(){
		return clusterMethod;
	}
	
	public int getClusterInds(int ind){
		return (clusterMethod==0)? clusterInds[ind]:-1;
	}
	
	public double getClusterWeight(int cl, int pix){
		return MagPixWnew[cl][pix];
	}
	
	public int getNumBBPoints(){
//		if (backbone!=null){
//			return backbone.getNCoordinates();
//		} else {
			return numBBPts;
//		}
	}
	
	public FloatPolygon getBbInit(){
		return (bbInit==null)? null : bbInit.duplicate();
	}
	
	/**
	 * Gets the finalized backbone. NOTE: during backbone fitting, this is not set; instead use bbOld/bbNew
	 * @return
	 */
	public double[][] getBackbone(){
		return CVUtils.fPoly2Array(backbone.getFloatPolygon(), rect.x, rect.y);
	}
	
	/**
	 * Interpolates the backbone to the given number of coordinates
	 * @param numCoords
	 * @return
	 */
	public double[][] getInterpdBackbone(int numCoords){
		if (numCoords==-1) numCoords = numBBPts;
		PolygonRoi newBB = getInterpolatedSegment(backbone, numCoords);
		if (newBB!=null){
			return CVUtils.fPoly2Array(newBB.getFloatPolygon(), 0, 0);
		}else {
			return null;
		}
	}
	
	public float[] getCOM(){
		
		return getClusterCOM(-1);
		
	}
	
	public float[] getClusterCOM(int cluster){
		
		if (MagPixI==null || MagPixI.length==0 || cluster>=numBBPts){
			float[] err = {-1,-1};
			return err;
		}
		
		float[] com = new float[2];
		float comNorm = 0;
		for (int i=0; i<MagPixI.length; i++){
			if (cluster==-1 || clusterInds[i]==cluster){
				int mpi = MagPixI[i];
				
				com[0]+=mpi*MagPixX[i];
				com[1]+=mpi*MagPixY[i];
				comNorm+=mpi;
			}
		}
		
		com[0]=com[0]/comNorm;
		com[1]=com[1]/comNorm;
		
		return com;
		
	}
	
	public boolean getArtificialMid(){
		return artificialMid;
	}
	
	public boolean diverged(int dc){
		
		int w = im.getWidth();
		int h = im.getHeight();
		
		int xL = rect.x-dc*w;
		int xU = rect.x+(dc+1)*w;
		int yL = rect.y-dc*h;
		int yU = rect.y+(dc+1)*h;
		
		for (int i=0; i<bbNew.npoints; i++){
			double xc = bbNew.xpoints[i];
			double yc = bbNew.ypoints[i];
			if (xc<xL || xc>xU || yc<yL || yc>yU){
				return true;
			}
		}
		return false;
	}
//	
//	public ImageProcessor getIm(){
//
//		return getIm(new MaggotDisplayParameters());
//	//	return getIm(MaggotDisplayParameters.DEFAULTexpandFac, MaggotDisplayParameters.DEFAULTclusters,MaggotDisplayParameters.DEFAULTmid, MaggotDisplayParameters.DEFAULTinitialBB, MaggotDisplayParameters.DEFAULTnewBB, 	
//		//		MaggotDisplayParameters.DEFAULTcontour, MaggotDisplayParameters.DEFAULTht, MaggotDisplayParameters.DEFAULTforces, MaggotDisplayParameters.DEFAULTbackbone);
//		
//	}
//	
//	public ImageProcessor getIm(MaggotDisplayParameters mdp){
//		
//		if (mdp==null){
//			return getIm();
//		} 
//		ImageProcessor ip = super.getIm(mdp);
//		
////		return drawFeatures(ip, mdp); 
//
//	}
//	
//	
//	public ImageProcessor getIm(int expandFac, boolean clusters, boolean mid, boolean initialBB, boolean newBB, boolean contour, boolean ht, boolean forces, boolean bb){
//
//		if (mid && MagPixX==null){
//			reloadMagPix();
//		}
//		
//		imOriginX = (int)x-(getTrackWindowWidth()/2);
//		imOriginY = (int)y-(getTrackWindowHeight()/2);
//		im.snapshot();
//		
//		ImageProcessor bigIm = im.resize(im.getWidth()*expandFac);
//		
//		int centerX = (int)((x-rect.x)*(expandFac));
//		int centerY = (int)((y-rect.y)*(expandFac));
//		ImageProcessor pIm = CVUtils.padAndCenter(new ImagePlus("Point "+pointID, bigIm), expandFac*getTrackWindowWidth(), expandFac*getTrackWindowHeight(), centerX, centerY);
//		int offX = (expandFac*getTrackWindowWidth())/2-centerX;
//		int offY = (expandFac*getTrackWindowHeight())/2-centerY;
////		int offX =  windowCenterX - centerX;//((int)x-rect.x)*expandFac;//rect.x-imOriginX;
////		int offY = windowCenterY - centerY;//((int)y-rect.y)*expandFac;//rect.y-imOriginY;
//		
//		
//		return drawFeatures(pIm, offX, offY, expandFac, clusters, mid, initialBB, newBB, contour, ht, forces, bb); 
//		
//	}

	public void reloadMagPix(){
		setInitialBB(backbone, numBBPts);
		setMagPix();
		setInitialClusterInfo();
	}
	
	protected ImageProcessor drawFeatures(ImageProcessor grayIm, MaggotDisplayParameters mdp){
		ImageProcessor im = super.drawFeatures(grayIm, mdp);
		if (im.isGrayscale()) {
			im = im.convertToRGB();
		} 
		int centerX = (int)((x-rect.x)*(mdp.expandFac));
		int centerY = (int)((y-rect.y)*(mdp.expandFac));
		int offX = (mdp.expandFac*getTrackWindowWidth())/2-centerX;
		int offY = (mdp.expandFac*getTrackWindowHeight())/2-centerY;
		
		//PIXEL CLUSTERS
		if (mdp.clusters) displayUtils.drawClusters(im, numPix, MagPixX, MagPixY, clusterInds, mdp.expandFac, offX, offY, rect);
		
		
		//INITIAL SPINE
		if (mdp.initialBB) displayUtils.drawBBInit(im, bbInit, offX, offY, rect, mdp.expandFac, Color.MAGENTA);
		
		if (mdp.newBB) displayUtils.drawBackbone(im, bbNew, mdp.expandFac, offX, offY, rect, Color.blue);
		
		FloatPolygon bbpoly = inHistory ? bbOld : backbone.getFloatPolygon();
		
		
		//FORCES
		if (mdp.forces && targetBackbones!=null && targetBackbones.size()>0) {
			displayUtils.drawTargets(im, targetBackbones, mdp.showForce, mdp.expandFac, offX, offY, rect, bbpoly);//TargetBackbones are absolute coords
		}
			
			
		//BACKBONE
		if (mdp.backbone) displayUtils.drawBackbone(im, bbpoly, mdp.expandFac, offX, offY, rect, Color.RED);
		
		if (inHistory) {
			im.setFont(new Font(im.getFont().getFontName(), Font.PLAIN, 14));
			im.drawString(historyMessage, 5, im.getFont().getSize()+5);
		}
		
		return im;
	}
	
	
	public String getTPDescription(){
		String s = super.getTPDescription();
		
		if (midline!=null && htValid) s+= "         ";
				
		if (artificialMid) s+=" M-f";
		return s;
	}
	
	public double bbInitDist(FloatPolygon otherbb){
		double totalDistSqr = 0;
		
		if (bbInit!=null && otherbb!=null && bbInit.npoints!=0 && otherbb.npoints!=0 && bbInit.npoints==otherbb.npoints){
			
			for (int i=0; i<bbInit.npoints; i++){
				totalDistSqr+= (bbInit.xpoints[i]-otherbb.xpoints[i])*(bbInit.xpoints[i]-otherbb.xpoints[i]);
				totalDistSqr+= (bbInit.ypoints[i]-otherbb.ypoints[i])*(bbInit.ypoints[i]-otherbb.ypoints[i]);
			}
			
		} else {
			return -1.0;	
		}
		
		return Math.sqrt(totalDistSqr);
	}
	
	
	public double getBackboneLength () {
		int[] x = backbone.getXCoordinates();
		int[] y = backbone.getYCoordinates();
		return MathUtils.curveLength(MathUtils.castIntArray2Double(x), MathUtils.castIntArray2Double(y));
	}
	
	public double[] getBackboneMean () {
		return new double[] {MathUtils.mean(backbone.getXCoordinates()), MathUtils.mean(backbone.getYCoordinates())};
	}
	
	
	public int toDisk(DataOutputStream dos, PrintWriter pw){
		
		//Write all ImTrackPoint data
		super.toDisk(dos, pw);
		
		try {
			//Write # of backbone points
			dos.writeShort(backbone.getNCoordinates());
			//Write backbone points
			FloatPolygon bfp = backbone.getFloatPolygon();
			float[] xp = backbone.getFloatPolygon().xpoints;
			float[] yp = backbone.getFloatPolygon().ypoints;
			
			
			for (int i=0; i<bfp.npoints; i++){
				dos.writeFloat(xp[i]);
				dos.writeFloat(yp[i]);
				
//				dos.writeFloat(bfp.xpoints[i]);
//				dos.writeFloat(bfp.ypoints[i]);
			}
			//Write artificial mid
			dos.writeByte(artificialMid ? 1:0);
			
			if (energies==null){
				dos.writeInt(0);
			} else {
				dos.writeInt(energies.keySet().size());
				for(String key : energies.keySet()){
					dos.writeUTF(key);
					dos.writeDouble(energies.get(key));
				}
			}
			
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing BackboneTrackPoint image for point "+pointID+"; aborting save");
			return 1;
		}
		
		return 0;
	}
	
	public int sizeOnDisk(){
		
		int size = super.sizeOnDisk();
		size += Short.SIZE/Byte.SIZE;//# backbone pts
		size += (2*backbone.getNCoordinates())*java.lang.Float.SIZE/Byte.SIZE;//Backbone coords
		size += Byte.SIZE/Byte.SIZE;//artificialmid flag
		
		size += Integer.SIZE/Byte.SIZE;//# energy values
		if (energies!=null){
			for(String key : energies.keySet()){
				size += 2*Byte.SIZE/Byte.SIZE; //UTF metadata
				size += key.getBytes().length; //UTF representation of energy name
				size += Double.SIZE/Byte.SIZE; //energy value
			}
		}
		
		return size;
	}

	public static BackboneTrackPoint fromDisk(DataInputStream dis, Track t, PrintWriter pw){
		
		BackboneTrackPoint btp = new BackboneTrackPoint();
		int resultCode = btp.loadFromDisk(dis,t,pw);
		if (resultCode==0){
			return btp;
		} else {
			System.out.println(resultCode);
			return null;
		}
	}
	
	protected int loadFromDisk(DataInputStream dis, Track t, PrintWriter pw){
		
		//Load all superclass info
		if (super.loadFromDisk(dis,t,pw)!=0){
			return 1;
		}
		
		//read new data
		try {
			//#bbPts
			numBBPts = dis.readShort();
			
			//backbone
			if (numBBPts>0){
				float[] bbX = new float[numBBPts];
				float[] bbY = new float[numBBPts];
				for (int i=0; i<numBBPts; i++){
					bbX[i] = dis.readFloat();
					bbY[i] = dis.readFloat();
				}
				backbone = new PolygonRoi(bbX,  bbY, PolygonRoi.POLYLINE);
			} else {
				backbone = new PolygonRoi(new FloatPolygon(),PolygonRoi.POLYLINE);
			}
			
			//artificialMid
			artificialMid = (dis.readByte()==1);
			
			// energies
			int numKeys = dis.readInt();
			if (numKeys>0) {
				energies = new HashMap<String, Double>();
				for (int i=0; i<numKeys; i++){
					String key = dis.readUTF();
					energies.put(key, dis.readDouble());
				}
			}
			
		} catch (Exception e) {
			if (pw!=null) pw.println("Error writing BackboneTrackPoint Info");
			return 2;
		}
		
		return 0;
	}
	
	
	public String getCSVfieldVal(int ind){
		
		if (ind<=CSVPrefs.maxInd(super.getTypeName())){
			return super.getCSVfieldVal(ind);
		}
		
		switch (ind-CSVPrefs.maxInd(super.getTypeName())) {
		case 1:
			return (artificialMid)? "TRUE":"FALSE";
		default: 
			return "";
		
		}
        
	}

	public void recordHistory() {
		if (null == bbHistory) {
			bbHistory = new Vector<FloatPolygon>();
			historyLabel = new Vector<String>();
		}
	}
	public void noHistory() {
		if (null == bbHistory) { return;}
		bbHistory.clear();
		bbHistory = null;
		historyLabel.clear();
		historyLabel = null;
	}
	public void storeBackbone(String s) {
		if (null == bbHistory) { return; }
		bbHistory.add(bbOld);
		if (frozen) {
			historyLabel.add(s + " frozen" );
		} else if (hidden) {
			historyLabel.add(s + " hidden");
		} else {
			historyLabel.add(s + " active");
		}
	}
	public void storeBackbone() {
		storeBackbone("");
	}
	public int getHistoryLength() {
		if (null == bbHistory) { return 0; }
		return bbHistory.size();
	}
	
	
	/**
	 * setTargetBackbones
	 * intended to be used only for display purposes
	 * changes state of BackboneTrackPoint, so WILL interfere with fitting
	 */
	public void setTargetBackbones (Vector<Force> Forces, Vector<BackboneTrackPoint> points, int history) {
		try{	
			int ind = track.getPointIndexFromID(points, pointID);
			if (ind < 0 || ind >= points.size()){
				return;
			}	
			if (targetBackbones == null) {
				targetBackbones = new Vector<FloatPolygon>();
			}
			targetBackbones.clear();
			//bbOld = CVUtils.fPolyAddOffset(backbone.getFloatPolygon(), rect.x, rect.y);
			if (null == bbHistory || history < 0 || history >= bbHistory.size()) {
				for (BackboneTrackPoint btp : points) {
					btp.bbOld = btp.backbone.getFloatPolygon();
				}
				inHistory = false;
			} else {
				for (BackboneTrackPoint btp : points) {
					btp.bbOld = btp.bbHistory.get(history);
				}
//				bbOld = bbHistory.get(history);
				if (null == bbOld) {
					bbOld = backbone.getFloatPolygon();
				}
				inHistory = true;
				historyMessage = historyLabel.get(history);
			}
			setMagPix();
			setInitialClusterInfo();
			for (int i=0; i<Forces.size(); i++){						
				FloatPolygon tb = Forces.get(i).getTargetPoints(ind, points);
				targetBackbones.add(tb);
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			comm.message("Error getting target backbones: \n"+sw.toString()+"\n", VerbLevel.verb_error);
		}
	}
	public void setTargetBackbones (Vector<Force> Forces, Vector<BackboneTrackPoint> points) {
		setTargetBackbones(Forces, points, -1);
	}
	/**
	 * setTargetBackbones
	 * slow, intended to be used only for display purposes
	 * changes state of BackboneTrackPoint, so WILL interfere with fitting
	 */
	public void setTargetBackbones (Vector<Force> Forces, int history) {
		Vector<BackboneTrackPoint> btps = new Vector<BackboneTrackPoint>();
		for (TrackPoint tp : track.points) {
			BackboneTrackPoint btp = (BackboneTrackPoint) tp;
			if (btp == null) { return; }
			btps.add(btp);
		}
		setTargetBackbones(Forces, btps, history);
	}
	public void setTargetBackbones (Vector<Force> Forces) {
		setTargetBackbones(Forces,-1);
	}
	
	public int getPointType(){
		return BackboneTrackPoint.pointType;
	}
	
	public String getTypeName(){
		if (backbone==null || backbone.getNCoordinates()==0){
			return "Empty BackboneTrackPoint";
		}
		return "BackboneTrackPoint";
	}

	public double getGmmClusterVariance() {
		return gmmClusterVariance;
	}

	public void setGmmClusterVariance(double gmmClusterVariance) {
		this.gmmClusterVariance = gmmClusterVariance;
	}
	
}
