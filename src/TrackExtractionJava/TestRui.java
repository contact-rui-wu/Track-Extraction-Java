package TrackExtractionJava;

import ij.ImageJ;

public class TestRui {

	public static void main(String[] args) {
		
		//test_isDebugWorking();
		
		test_pipeline();

	}
	
	public static void test_isDebugWorking() {
		System.out.println("Is debug working on master?");
	}
	
	public static void test_pipeline() {
		// set timer
		TicToc timer = new TicToc();
		timer.tic();
		// prepare file names
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/pipeline/Java";
		String dataID = "sampleLongExp_copy";
		String mmfName = dataID+".mmf";
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
		//extrParams.subset = true; // deprecated
		//extrParams.startFrame = 1;
		//extrParams.endFrame = 10000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		//ep.runningFromMain = true; // deprecated
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		// run processor
		// syntax: run(String srcFileName, String dstDir, String dstName)
		ep.run(dir+"/"+mmfName,dir,dir+"/"+dataID);
		// stop timer
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
	}

}
