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
		String dir = "/home/data/rw1679/Documents/Gershow_lab_local/data_version_handling/";
		String mmfname = "sampleShortExp_copy.mmf";
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.doFitting = true;
		prParams.showFitEx = true;
		prParams.saveMagEx = false;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		// run extraction
		Experiment_Processor ep = new Experiment_Processor();
		ep.runningFromMain = true;
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		ep.run(dir+mmfname);
		
		///// save experiment to disk /////
		
		///// load experiment from disk /////
	}
	
	public static void test_extraction() {
		ImageJ ij = new ImageJ();
		
		// set parameters
		ProcessingParameters prParams = new ProcessingParameters();
		prParams.showMagEx = false;
		prParams.saveMagEx = false;
		prParams.doFitting = true;
		prParams.showFitEx = true;
		prParams.saveFitEx = false;
		prParams.saveErrors = false;
		prParams.saveSysOutToFile = false;
		ExtractionParameters extrParams = new ExtractionParameters();
		extrParams.subset = true;
		extrParams.startFrame = 1;
		extrParams.endFrame = 1000;
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;

		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		Experiment_Processor ep = new Experiment_Processor();

		ep.runningFromMain = true;

		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;

		ep.run(path);
	}

}
