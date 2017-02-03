package TrackExtractionJava;

import java.awt.image.ColorModel;
//import java.util.Vector;

import ij.ImagePlus;
import ij.VirtualStack;
//import ij.process.ImageProcessor;

public class TrackDdtVirtualStack extends VirtualStack {
	
	private Track tr;
	private ImagePlus imp = null;
	
	public TrackDdtVirtualStack(Track tr, int width, int height, ColorModel cm, String path) {
		super(width,height,cm,path);
		this.tr = tr;
	}

	public ImagePlus getImagePlus() {
		if (null == imp){
			imp = new ImagePlus("Track "+tr.getTrackID()+" (ddt): frames "+tr.points.firstElement().frameNum+"-"+tr.points.lastElement().frameNum,this);
		}
		return imp;
	}

}
