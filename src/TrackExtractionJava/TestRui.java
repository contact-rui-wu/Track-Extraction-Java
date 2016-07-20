package TrackExtractionJava;

import ij.ImageJ;

public class TestRui {

	public static void main(String[] args) {
		
		//test_dataVersion0Handling();
		
		test_extraction();

	}
	
	public static void test_dataVersion0Handling() {
		///// do extraction without saving /////
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/data_version_handling/sampleShortExp/";
		String fname = "sampleShortExp_copy";
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = false;
		prParams.saveMagEx = false;
		prParams.doFitting = true;
		prParams.showFitEx = true;
		prParams.saveFitEx = true;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		ep.runningFromMain = true;
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		// run processor
		ep.run(dir+fname+".mmf");
		// fetch experiment
		Experiment ex = ep.ex;
		///// save magEx to disk /////
		
		///// load magEx from disk /////
	}
	
	public static void test_extraction() {
		ImageJ ij = new ImageJ();
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/pipeline/sampleShortExp/";
		String mmfname = "sampleShortExp_copy.mmf";
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.diagnosticIm = false;
		prParams.showMagEx = true;
		prParams.saveMagEx = true;
		prParams.doFitting = true;
		prParams.showFitEx = true;
		prParams.saveFitEx = true;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		ep.runningFromMain = true;
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		// run processor
		ep.run(dir+mmfname);
	}

}
