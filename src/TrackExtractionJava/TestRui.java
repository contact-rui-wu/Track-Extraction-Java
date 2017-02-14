package TrackExtractionJava;

import ij.IJ;
import ij.ImageJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
//import ij.process.ImageProcessor;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class TestRui {

	public static void main(String[] args) {
		
//		test_pipeline();
		
		test_viewExperiment();
		
//		test_relativeRect();
		
//		test_rawRect2ddtRect();
		
//		test_integerRange();
		
//		test_consoleOutput();
		
//		test_isDebugWorking();

	}
	
	@SuppressWarnings("unused")
	public static void test_pipeline() {
		// set timer
		TicToc timer = new TicToc();
		timer.tic();
		
		// initialize imagej environment
		ImageJ ij = new ImageJ();
		
		// prepare file paths
		// syntax: *Name - file name; *Dir - folder; *Path - folder+name
		String exID = "sampleExp-copy";
		String srcDir = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\code-test\\"; // on windows
		String srcPath = srcDir+exID+".mmf";
		String dstDir = srcDir+exID+"_feat-ddt-new"+File.separator; // add branch label
		
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = false;
		prParams.saveMagEx = true;
		prParams.doFitting = true;
		prParams.showFitEx = false;
		prParams.saveFitEx = true;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
//		extrParams.subset = true; // deprecated
//		extrParams.startFrame = 23842-1000; // default=1
		extrParams.endFrame = 1000; // default=Integer.MAX_VALUE
//		extrParams.doDdt = false; // default=true
//		extrParams.ddtBuffer = 0; // default=0
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
//		ep.runningFromMain = true; // deprecated
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		ep.setVerbosity(VerbLevel.verb_error); // default: verb_warning
		
		// run extraction pipeline
		ep.run(srcPath,dstDir,exID);
		
		/////////////////////
		// secondary tests //
		/////////////////////
		
		// image normalization diagnosis
//		test_playMovie(ep.ex.getTrack(7),0);
//		test_playMovie(ep.ex.getTrack(7),1);
//		test_playMovie(ep.ex.getTrack(13),0);
//		test_playMovie(ep.ex.getTrack(13),1);
//		test_playMovie(ep.ex.getTrack(18),0);
//		test_playMovie(ep.ex.getTrack(18),1);
//		test_playMovie(ep.ex.getTrack(21),0);
//		test_playMovie(ep.ex.getTrack(21),1);
		
		// check scaling
//		test_playMovie(ep.ex.getTrack(1),0); // meanArea=81
//		test_playMovie(ep.ex.getTrack(2),0); // meanArea=100
//		test_playMovie(ep.ex.getTrack(11),0); // meanArea=120
		
		// play raw and ddt movies for one track
//		test_playMovie(ep.ex.getTrack(10),0);
//		test_playMovie(ep.ex.getTrack(10),1);
		
		// save true-size, not-padded ddtIms for one track
//		String s = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\ddt-artifacts\\sampleExp_copy_track10_ddt\\";
//		test_saveDdtIms(ep.ex.getTrack(10),s);
		
		// save all padded raw+ddtIms for this experiment to a .bin file
//		String s = dstDir+exID+"_raw+ddtIms_full-length.bin";
//		test_saveIms2Bin(ep.ex,s); // only works when extrParams.doDdt=true (default);
		
		// stop timer and beep
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
		IJ.beep();
	}
	
	@SuppressWarnings("unused")
	public static void test_viewExperiment() {
		String srcDir = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\code-test\\"; // on windows
		String exID = "sampleExp-copy";
		// on master: different data structure versions, doesn't work
//		String srcPath = srcDir+exID+"_master"+File.separator+exID+".prejav";
		String srcPath = srcDir+exID+"_master"+File.separator+exID+".jav";
		// on feat-ddt-new:
//		String srcPath = srcDir+exID+"_feat-ddt-new"+File.separator+exID+".prejav";
//		String srcPath = srcDir+exID+"_feat-ddt-new"+File.separator+exID+".jav";
		
		Experiment ex = new Experiment(srcPath);
		
		// show experiment/track should only work when doDdt matches in pipeline and above
		// show entire experiment:
		ExperimentFrame exFrame = new ExperimentFrame(ex);
		exFrame.run(null);
		// show just one track:
//		/*
		ImageJ ij = new ImageJ();
		test_playMovie(ex.getTrackFromInd(5),0);
		test_playMovie(ex.getTrackFromInd(5),1);
//		*/
		
	}
	
	public static void test_relativeRect() {
		Rectangle oldRect = new Rectangle(1143,1128,26,17); //xRange:1143-1169, yRange:1128-1145
		System.out.println("oldrect: "+oldRect.toString());
		Rectangle newRect = new Rectangle(1155,1130,14,10); //xRange:1155-1168, yRange:1130-1140
		System.out.println("newrect: "+newRect.toString());
		// check if out of bound
		System.out.println("oldRect contains newRect: "+Boolean.toString(oldRect.contains(newRect)));
		// calculate relRect pivot
		int relX = newRect.x-oldRect.x;
		int relY = newRect.y-oldRect.y;
		Rectangle relRect = new Rectangle(relX,relY,newRect.width,newRect.height);
		System.out.println("relRect: "+relRect.toString());
		// outcode doesn't work
//		System.out.println("coordinates ("+newRect.x+","+newRect.y+") is outcode "+oldRect.outcode((double)newRect.x,(double)newRect.y));
	}
	
	public static void test_rawRect2ddtRect() {
		// get rawRect
		Rectangle rawRect = new Rectangle(3,3,17,12);
		System.out.println("rawRect: "+rawRect.toString());
		// grow ddtRect
		Rectangle ddtRect = (Rectangle)rawRect.clone();
		ddtRect.grow(3, 3);
		System.out.println("ddtRect: "+ddtRect.toString());
		// get back from ddtRect
		int x = ddtRect.x+3;
		int y = ddtRect.y+3;
		int w = ddtRect.width-6;
		int h = ddtRect.height-6;
		System.out.println("shrink it back: [x=" + x + ",y=" + y + ",width=" + w + ",height=" + h + "]");
	}
	
	public static void test_integerRange() {
		// java byte range:
		System.out.println("java byte range: "+Byte.MIN_VALUE+" to "+Byte.MAX_VALUE);
		// true ddt range: calculation is done in int
		int lIn = Byte.MIN_VALUE-Byte.MAX_VALUE;
		int mIn = Byte.MIN_VALUE-Byte.MIN_VALUE;
		int hIn = Byte.MAX_VALUE-Byte.MIN_VALUE;
		System.out.println("true ddt range: "+lIn+" ("+mIn+") "+hIn);
		// old method: +128 then (byte)
		/*
		int lOut = lIn+128;
		int mOut = mIn+128;
		int hOut = hIn+128;
		System.out.println("old method range (int): "+lOut+" ("+mOut+") "+hOut);
		System.out.println("old method range (byte): "+(byte)lOut+" ("+(byte)mOut+") "+(byte)hOut);
		*/
		// new method: (*In+255)/2 then (byte)
		int lOutNew = (lIn+255)/2;
		int mOutNew = (mIn+255)/2;
		int hOutNew = (hIn+255)/2;
		System.out.println("new method range (int): "+lOutNew+" ("+mOutNew+") "+hOutNew);
		System.out.println("new method range (byte): "+(byte)lOutNew+" ("+(byte)mOutNew+") "+(byte)hOutNew);
		int upper = (byte)hOutNew&0xff;
		System.out.println("upper bound retrieved by getPixel(): "+upper);
	}
	
	/**
	 * Larva area scaled
	 */
	public static void test_saveIms2Bin(Experiment ex, String dstPath) {
		try {
			File f = new File(dstPath);
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
			
			// if no track in ex, abort saving
			if (ex.getNumTracks()==0) {
				System.out.println("No tracks in experiment, save aborted");
				dos.close();
				return;
			}
			
			System.out.println("Saving raw+ddt images to .bin file...");
			IJ.showStatus("Saving raw+ddt images");
			
			// write number of tracks
//			System.out.println("...writing number of tracks ("+ex.getNumTracks()+")");
			dos.writeInt(ex.getNumTracks());
			
			// write each track
			for (int i=0;i<ex.getNumTracks();i++) {
				Track tr = ex.getTrackFromInd(i);
				double scaleFac = Math.sqrt(100/tr.meanArea());
				// write track number
//				System.out.println("...writing track # "+i+" ("+tr.getNumPoints()+" points)");
				IJ.showStatus("Writing track # "+i+" ("+tr.getNumPoints()+" points)");
				dos.writeInt(i);
				// write number of points in this track
				dos.writeInt(tr.getNumPoints());
				// write beginning and end frame numbers
				dos.writeInt(tr.getStart().getFrameNum());
				dos.writeInt(tr.getEnd().getFrameNum());
				// write each point
				for (int j=0;j<tr.getNumPoints();j++) {
					IJ.showProgress(j+1,tr.getNumPoints());
					ImTrackPoint itp = (ImTrackPoint)tr.getPoint(j);
					// write raw image
					if (itp.im!=null) {
						for (int m=0;m<30;m++) {
							for (int n=0;n<30;n++) {
								dos.writeByte(itp.getPadImNew(0,scaleFac).getPixel(m, n));
							}
						}
					} else {
						for (int k=0;k<900;k++) {
							dos.writeByte(0); // draw black placeholder for null rawIm
						}
					}
					// write ddt image
					if (itp.ddtIm!=null) {
						for (int p=0;p<30;p++) {
							for (int q=0;q<30;q++) {
								dos.writeByte(itp.getPadImNew(1,scaleFac).getPixel(p, q));
							}
						}
					} else {
						for (int r=0;r<900;r++) {
							dos.writeByte(127); // draw gray placeholder for null ddtIm
						}
					}
				}
			}
			
			// write end of file
			dos.writeInt(0);
			dos.close();
			System.out.println("Successfully saved raw+ddt images to .bin file");
			IJ.showStatus("Done saving raw+ddt images");
		} catch(Exception e) {
			System.out.println("Error saving raw+ddt images to .bin file");
			e.printStackTrace();
		}
	}
	
	/**
	 * Larva area not scaled
	 */
	public static void test_saveDdtIms(Track tr, String dstDir) {
		int nPts = tr.getNumPoints();
		FileSaver fs;
		for (int i=0;i<nPts;i++) {
			ImagePlus ddtPlus = new ImagePlus("Track "+tr.getTrackID()+"ddt movie: frame "+tr.getPoint(i).getFrameNum(),tr.getPoint(i).getImNew(1));
			fs = new FileSaver(ddtPlus);
			fs.saveAsTiff(dstDir+File.separator+i+".tif");
		}
	}
	
	/**
	 * Larva area scaled; interestingly, this affects play movie button
	 */
	public static void test_playMovie(Track tr, int imType) {
		double scaleFac = Math.sqrt(100/tr.meanArea());
//		double scaleFac = 1;
		System.out.println("Scaling larva area in track "+tr.getTrackID()+" by "+String.format("%.2f",scaleFac));
		String imTypeName;
		Color padColor;
		switch (imType) {
		case 0:
			imTypeName = "raw";
			padColor = Color.black;
			break;
		case 1:
			imTypeName = "ddt";
			padColor = new Color(127,127,127);
			break;
		default:
			imTypeName = "?";
			padColor = Color.white;;
		}
		ImTrackPoint itp = (ImTrackPoint)tr.getStart();
		int w = itp.getTrackWindowWidth();
		int h = itp.getTrackWindowHeight();
		int nPts = tr.getNumPoints();
		ImageStack movieStack = new ImageStack(w,h,nPts);
		for (int i=0;i<nPts;i++) {
			itp = (ImTrackPoint)tr.getPoint(i);
			try {
				movieStack.setProcessor(itp.getPadImNew(imType,scaleFac), i+1); // imagej index starts at 1
//				movieStack.setProcessor(itp.getIm(), i+1);
			} catch (Exception e) {
				System.out.println("TrackPoint "+itp.getPointID()+" has no valid "+imTypeName+" image for frame "+itp.getFrameNum()+", drawing placeholder");
				ByteProcessor placeholder = new ByteProcessor(w,h);
				placeholder.setColor(padColor);
				placeholder.fill();
				movieStack.setProcessor(placeholder, i+1);
				// warning: this shouldn't happen at all if doDdt=true; setDdtIm4Pts() already draws placeholder
			}
		}
		ImagePlus moviePlus = new ImagePlus("Track "+tr.getTrackID()+" "+imTypeName+" movie: frame "+tr.getStart().getFrameNum()+"-"+tr.getEnd().getFrameNum(),movieStack);
		moviePlus.show();
	}
	
	public static void test_consoleOutput() {
		byte[] x = new byte[] {0,0,0,0};
		String s = Arrays.toString(x);
		System.out.println("data version: "+x[0]+x[1]+x[2]+x[3]);
	}

	public static void test_isDebugWorking() {
		System.out.println("Is debug working on feat-ddt-new?");
	}

}
