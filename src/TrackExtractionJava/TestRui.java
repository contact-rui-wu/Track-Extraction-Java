package TrackExtractionJava;

import ij.IJ;
import ij.ImageJ;
import ij.ImageStack;
import ij.ImagePlus;
import ij.io.FileSaver;
//import ij.process.ImageProcessor;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class TestRui {

	public static void main(String[] args) {
		
		test_pipeline();
		
		//test_isDebugWorking();

	}
	
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
//		String dstPath = dstDir+exID;
		
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = false;
		prParams.saveMagEx = false;
		prParams.doFitting = false;
		prParams.showFitEx = false;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
//		extrParams.subset = true; // deprecated
//		extrParams.startFrame = 23842-1000; // default=1
		extrParams.endFrame = 1000; // default=Integer.MAX_VALUE
//		extrParams.doDdt = false; // default=true
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
//		ep.runningFromMain = true; // deprecated
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
//		ep.setVerbosity(VerbLevel.verb_debug); // default: verb_warning
		
		// run extraction pipeline
		ep.run(srcPath,dstDir,exID);
		
		// secondary tests:
//		test_playDdtMovie(ep.ex.getTrack(10));
//		String s = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\ddt-artifacts\\sampleExp_copy_track10_ddt\\";
//		test_saveDdtIms(ep.ex.getTrack(10),s);
		String s = dstDir+exID+"_raw+ddtIms.bin";
		test_saveIms2Bin(ep.ex,s); // only works when extrParams.doDdt=true (default);
		
		// stop timer
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
	}
	
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
//			System.out.println("...writing number of trakcs ("+ex.getNumTracks()+")");
			dos.writeInt(ex.getNumTracks());
			
			// write each track
			for (int i=0;i<ex.getNumTracks();i++) {
				Track tr = ex.getTrackFromInd(i);
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
								dos.writeByte(itp.getPadImNew(0).getPixel(m, n));
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
								dos.writeByte(itp.getPadImNew(1).getPixel(p, q));
							}
						}
					} else {
						for (int r=0;r<900;r++) {
							dos.writeByte(128); // draw gray placeholder for null ddtIm
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
	
	public static void test_saveDdtIms(Track tr, String dstDir) {
		int nPts = tr.getNumPoints();
		FileSaver fs;
		for (int i=0;i<nPts;i++) {
			ImagePlus ddtPlus = new ImagePlus("Track "+tr.getTrackID()+"ddt movie: frame "+tr.getPoint(i).getFrameNum(),tr.getPoint(i).getImNew(1));
			fs = new FileSaver(ddtPlus);
			fs.saveAsTiff(dstDir+File.separator+i+".tif");
		}
	}
	
	public static void test_playDdtMovie(Track tr) {
		ImTrackPoint itp = (ImTrackPoint)tr.getStart();
		int w = itp.getTrackWindowWidth();
		int h = itp.getTrackWindowHeight();
		int nPts = tr.getNumPoints();
		ImageStack ddtStack = new ImageStack(w,h,nPts);
		for (int i=0;i<nPts;i++) {
			itp = (ImTrackPoint)tr.getPoint(i);
			ddtStack.setProcessor(itp.getPadImNew(1), i+1);
		}
		ImagePlus ddtPlus = new ImagePlus("Track "+tr.getTrackID()+" ddt movie: frame "+tr.getStart().getFrameNum()+"-"+tr.getEnd().getFrameNum(),ddtStack);
		ddtPlus.show();
	}

	public static void test_isDebugWorking() {
		System.out.println("Is debug working on feat-ddt-new?");
	}

}
