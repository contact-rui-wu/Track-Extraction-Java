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
		//String dir = "/home/data/rw1679/Documents/Gershow_lab_local/pipeline/Java";
		//String dataID = "sampleLongExp_copy";
		//String dir = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\code-test\\";
		//String dataID = "sampleExp-copy";
		//String mmfName = dataID+".mmf";
		String exID = "sampleExp-copy";
		String srcDir = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\code-test\\"; // on windows
		String srcPath = srcDir+exID+".mmf";
		String dstDir = srcDir+exID+"_master"+File.separator; // add branch label
		//String dstPath = dstDir+exID;
		
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = true;
		prParams.saveMagEx = true;
		prParams.doFitting = false;
		prParams.showFitEx = false;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		//extrParams.subset = true; // deprecated
		//extrParams.startFrame = 1; // default=1
		//extrParams.endFrame = 10000; // default=Integer.MAX_VALUE
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		//ep.runningFromMain = true; // deprecated
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		
		// run extraction pipeline
		// syntax: run(String srcFileName, String dstDir, String dstName)
		//ep.run(dir+"/"+mmfName,dir,dir+"/"+dataID);
		ep.run(srcPath,dstDir,exID);
		
		// stop timer
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
	}

	public static void test_isDebugWorking() {
		System.out.println("Is debug working on master?");
	}

}
