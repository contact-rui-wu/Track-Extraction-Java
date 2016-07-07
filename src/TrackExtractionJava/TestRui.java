package TrackExtractionJava;

import ij.ImageJ;

public class TestRui {

	public static void main(String[] args) {
		
		test_extraction();

	}
	
	public static void test_extraction() {
		ImageJ ij = new ImageJ();
		TicToc timer = new TicToc();

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

		String path = "/home/data/rw1679/Documents/Gershow_lab_local/sampleShortExp_copy.mmf";
		Experiment_Processor ep = new Experiment_Processor();

		ep.runningFromMain = true;

		ep.prParams = prParams;
		ep.extrParams = extrParams;

		timer.tic();
		ep.run(path);
		System.out.println("Runtime: "+timer.toc()/1000+" seconds");
	}

}
