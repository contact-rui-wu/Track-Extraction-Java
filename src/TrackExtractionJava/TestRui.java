package TrackExtractionJava;

import ij.ImageJ;

public class TestRui {

	public static void main(String[] args) {
				
		test_pipeline();

	}
	
	public static void test_pipeline() {
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
