package TrackExtractionJava;

import ij.ImageJ;

import java.io.File;

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
		prParams.showMagEx = true;
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
		
		// stop timer
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
	}

	public static void test_isDebugWorking() {
		System.out.println("Is debug working on master?");
	}

}
