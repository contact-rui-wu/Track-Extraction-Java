package TrackExtractionJava;

import ij.ImageJ;

import java.io.File;

public class TestRui {
	
	private static String OS = null;
	private static String getOSName() {
		if (OS==null) {
			OS = System.getProperty("os.name");
		}
		return OS;
	}
	private static boolean isWindows() {
		return getOSName().startsWith("Windows");
	}
	private static boolean isLinux() {
		return getOSName().startsWith("Linux");
	}

	public static void main(String[] args) {
		
		test_pipeline();
		
//		test_isDebugWorking();
		
//		test_checkOS();

	}
	
	public static void test_pipeline() {
		// set timer
		TicToc timer = new TicToc();
		timer.tic();
		
		// initialize imagej environment
		ImageJ ij = new ImageJ();
		
		// prepare file paths
		String exID = "sampleExp-copy";
		String srcDir = "";
		if (isWindows()) {
			srcDir = "D:\\Life Matters\\Research\\with Marc Gershow\\data\\code-test\\";
		} else if (isLinux()) {
			srcDir = "/home/data/rw1679/Documents/gershow-lab-local/data/java-pipeline/";
		}
		String srcPath = srcDir + exID + ".mmf";
		String dstDir = srcDir + exID + "_master" + File.separator; // add branch label

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
//		extrParams.startFrame = 1; // default=1
		extrParams.endFrame = 1000; // default=Integer.MAX_VALUE
		FittingParameters fitParams = new FittingParameters();
		fitParams.storeEnergies = false;
		
		// prepare processor
		Experiment_Processor ep = new Experiment_Processor();
		ep.prParams = prParams;
		ep.extrParams = extrParams;
		ep.fitParams = fitParams;
		
		// run extraction pipeline
		// syntax: run(String srcFileName, String dstDir, String dstName)
		ep.run(srcPath,dstDir,exID);
		
		// stop timer
		System.out.println("Pipeline time: "+timer.toc()/1000+"s");
	}

	public static void test_isDebugWorking() {
		System.out.println("Is debug working on master?");
	}
	
	public static void test_checkOS() {
//		System.out.println(System.getProperty("os.name"));
		if (isWindows()) {
			System.out.println("OS is Windows");
		} else {
			System.out.println("OS is not Windows");
		}
		if (isLinux()) {
			System.out.println("OS is Linux");
		} else {
			System.out.println("OS is not Linux");
		}
	}

}
