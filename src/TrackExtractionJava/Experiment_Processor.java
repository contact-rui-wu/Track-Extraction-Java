package TrackExtractionJava;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Hashtable;
import java.util.Scanner;
import java.util.Vector;

import edu.nyu.physics.gershowlab.mmf.mmf_Reader;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij.plugin.frame.RoiManager;
import ij.text.TextWindow;


public class Experiment_Processor implements PlugIn{


	protected String paramFileName;
	ProcessingParameters prParams; 
	ExtractionParameters extrParams;
	FittingParameters fitParams;
	CSVPrefs csvPrefs;
	
	protected String srcDir;
	private String srcName;
	//private String exName;
	private String dstDir;
	private String dstName;
	//private PrintWriter processLog;
	private Communicator comm;
	private VerbLevel verbosity = VerbLevel.verb_warning;
	
	private ImageWindow mmfWin;
	private ImagePlus mmfStack;
	private BackboneFitter bbf;
	protected Experiment ex;
	
//	protected boolean runningFromMain = false;
	private TicToc runTime;
	private int indentLevel;
	
	String currProcess = "none";
	
	private static void showStatus(String msg) {
		IJ.showStatus(msg);
		forceIJUpdate();
	}
	
	private static void forceIJUpdate() {
		Timer.tic("forcing update");
		IJ.getInstance().repaint();
		IJ.wait(1);
		Timer.toc("forcing update");
	}
	
	public static void main(String[] args){
		
		
		ImageJ imj = new ImageJ(ImageJ.NO_SHOW);
		
		Experiment_Processor ep = new Experiment_Processor();
//		ep.runningFromMain = true;
		
		ep.currProcess = "Parsing inputs";
		if (args!=null && args.length>=1){
			
			if (args.length>=2){
				ep.dstDir = args[1];
			}
			
			ep.run(args[0]);
		}
		
		imj.quit();
	}


	public Experiment_Processor(){
		runTime = new TicToc();
		runTime.tic();
	}
	
	public void run(String srcFileName, String dstDir, String dstName) {
		run(srcFileName, dstDir, dstName, null);
	}
	public void run(String srcFileName, String dstDir, String dstName, SimpleExtractionParameters sep){
		if (sep != null) {
			prParams = sep.getProcessingParameters();
			extrParams = sep.getExtractionParameters();
			fitParams = sep.getFittingParameters();
		}
		run(new String[] {srcFileName, dstDir, dstName});
	}
	
	public void run(String[] args){
		
		if (args.length>=2){
			dstDir = args[1];
		} 
		if(args.length>=3){
			dstName = args[2];
		}
		if(args.length>=4){
			paramFileName = args[3];
		}
		
		run(args[0]);
	}
	
	/**
	 * Opens a file (via argument or dialog); extracts if needed, and fits tracks; saves extracted and fit tracks
	 */
	public void run(String arg0) {
		indentLevel=0;
		runTime = new TicToc();
		
		
		currProcess = "Processor setup";
		if (dstDir==null) dstDir = srcDir;
		if (dstDir == null) {
			IJ.showMessage("no source or destination directory specified");
			return;
		}
		if (!(new File(dstDir).exists())) {
			new File(dstDir).mkdirs();
		}
		
		setupParams();
		if (prParams.saveSysOutToFile) setupSysOut();

		currProcess = "Loading File";
		boolean success = (loadFile(arg0));
		
		//IJ.showMessage("setupParams returned");
		try {
			runTime.tic();
			String logpathname = setupLog();
			System.out.println("Experiment Processing Log initiated at "+logpathname);
			log("Initiated log entry");
			if(success){
				if (ex==null){
					
					currProcess = "Extracting Tracks";
					showStatus("extracting tracks");
					log("Loaded mmf; Extracting tracks...");
					if (!extractTracks()) {
						log("Error extracting tracks; aborting experiment_processor.");
						return;
					}
					log("...done extracting tracks"); 
					if (prParams.closeMMF && mmfWin!=null) {
						log("Closing MMF Window");
						RoiManager.getInstance().reset();
						mmfWin.close();
						mmfWin=null;
					}
					if (prParams.saveMagEx) {
						currProcess = "Saving Extracted Tracks";
						log("Saving Maggot Tracks...", true);
						saveOldTracks();
						log("...done saving Maggot Tracks", true);
					}
					showStatus("Done Saving MaggotTrackPoint Experiment");
				}
				
				
				System.gc();
				showStatus(IJ.freeMemory());
				if (prParams.doFitting){
					
					currProcess = "Fitting Tracks";
					showStatus ("Fitting "+ex.getNumTracks()+" Tracks...");
					
					log("Fitting "+ex.getNumTracks()+" Tracks...");
										
					fitTracks();
					log("...done fitting tracks");
					if (prParams.saveFitEx) {
						log("Saving backbone tracks...", true);
						
						currProcess = "Saving Fit Tracks";
						if (saveNewTracks()){
							log("...done saving backbone tracks", true);
							
						} else {
							log("Error saving tracks", true);
						}
						showStatus("Done Saving BackboneTrackPoint Experiment");
					}
					
				}
				
				
				if(prParams.savetoCSV){
					currProcess = "Saving Params to CSV";
					Experiment.toCSV(ex, dstDir+File.separator+dstName+".csv", csvPrefs);
				}
				
				
			} else {
				log("...no success setting up log");
			}
			
			log("Done Processing");
			log(Timer.generateReport(true));
		} catch (Exception e){
			log("Error Thrown During Processing; Current Step: "+currProcess);
			log(e.getMessage());
		} finally {
			
			//if (processLog!=null) processLog.close();
			if (prParams.closeMMF && mmfWin!=null) {
				mmfWin.close();
				mmfWin=null;
			}
			
		}
		
		
		showStatus("Done Processing");
		
	}
	

	protected void setupSysOut(){

		File f = new File(dstDir+"SystemDOTout.txt");
		if (!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			} 

		try {
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(f.getAbsolutePath()))));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Set ups the processing objects
	 */
	private void setupParams(){

		if (paramFileName!=null){
			
			readParamsFromFile();
		
		} else {
			
			paramFileName = srcDir+System.getProperty("file.separator")+"ProcessingParams.txt";
			
			//Any method of parameter input other than from file will generate a file here
			if (prParams==null){
				prParams = new ProcessingParameters();
			}
			if (extrParams==null){
				extrParams = new ExtractionParameters();
			}
			if (bbf==null){
				if (fitParams==null){
					fitParams = new FittingParameters();
				}
				//bbf = new BackboneFitter(fitParams);
			}
			if (csvPrefs==null){
				csvPrefs = new CSVPrefs();
			}
			
			//writeParamsToFile();
		}
	}
	
	
	/**
	 * Loads whichever object (experiment or imageStack) is indicated
	 * @param arg0 The complete filename, or null to open a file chooser dialog
	 */
	private boolean loadFile(String arg0){
		indentLevel++;
		
		String fileName=null;
		String dir=null;
		
		boolean success = false;
		
		//Get the path name...
		if (arg0==null || arg0.equals("")){ //...from a dialog
			OpenDialog od = new OpenDialog("Choose an experiment (.mmf or .jav)", null);
			dir = od.getDirectory();
			fileName = od.getFileName();
			
		} else {//...from the passed argument
			System.out.println("Loading file "+arg0);
			showStatus("Loading file "+arg0);
			File f = new File(arg0);
			fileName = f.getName();
			dir = f.getParent();
//			StringBuilder sb = new StringBuilder(arg0);
//			int sep = sb.lastIndexOf(System.getProperty("path.separator"));
//			if (sep>0){
//				fileName = sb.substring(sep+1);
//				dir = sb.substring(0, sep-1);
//			}
		}
		if (fileName.equalsIgnoreCase("current")) {
			success = useCurrentWindow();	
		} else if (dir!=null){
			//Open the file with the appropriate method
				
			//IJ.showMessage("entered dir!=null block");
			srcDir = dir;
			srcName = fileName;
			
//			System.out.println("Loading file "+fileName);
			showStatus("Loading file "+fileName);
			
			if (fileName.substring(fileName.length()-4).equalsIgnoreCase(".mmf")){
//				System.out.println("Recognized as mmf");
				success = openMMF(dir, fileName);
				//Read MMF metadata 
				
			} else if (fileName.substring(fileName.length()-4).equalsIgnoreCase(".jav")){
				success = openExp(dir, fileName);
			//	exName = new File(dir, fileName).getPath();
			} else if (fileName.substring(fileName.length()-7).equalsIgnoreCase(".prejav")){
				success = openExp(dir, fileName);
			//	exName = new File(dir, fileName).getPath();
				
			} else{ 
				
				success = openWithIJ(dir, fileName);
				
				if (!success){
					System.out.println("Experiment_Processor.loadFile error: did not recognize file type"); 
					IJ.showMessage("File unrecognized as a video stack");
				}
			}
		} else {
			IJ.showMessage("Could not load file; null directory");
			System.out.println("Experiment_Processor.loadFile error: could not parse file name"); 
			success = false;
		}
		
		indentLevel--;
		return success;
	}
	
	private boolean useCurrentWindow(){
		mmfWin = WindowManager.getCurrentWindow();
		//srcName = mmfWin.getTitle();
		srcDir = IJ.getDirectory("image");
		//IJ.getImage().getFileInfo().fileName;
		//srcName = new File(srcDir).getName();
	//	IJ.showMessage("srcDir = " + srcDir);
		mmfStack = mmfWin.getImagePlus();
		srcName = mmfStack.getTitle();
		//IJ.showMessage("srcName = " + srcName);
		//		mmfWin.close();
		return mmfStack!=null;
	}
	
	private boolean openWithIJ(String dir, String filename){
//		mmfStack = new Opener().openImage(dir, filename);
		mmfStack = IJ.openImage(dir+File.pathSeparator+filename);
		return (mmfStack!=null);// && mmfStack.getNSlices()>1);
	}
	
	/**
	 * Loads an imageStack into the processor from a .mmf file
	 * @param dir
	 * @param filename
	 * @return Status of the file opening: true=successful
	 */
	private boolean openMMF(String dir, String filename){
		indentLevel++;
		try{
			showStatus("Opening MMF...");		
			//			
			//			if (!runningFromMain){
			//				IJ.showMessage("running from main = false");
			//				IJ.run("Import MMF", "path=["+new File(dir, filename).getPath()+"]");
			////				useCurrentWindow()
			//				
			//				mmfWin = WindowManager.getCurrentWindow();
			//				mmfStack = mmfWin.getImagePlus();
			//			} else {
		//	IJ.showMessage("running from main = true");
			System.out.println("Opening mmf from code..");

			String path = new File(dir, filename).getPath();
			mmf_Reader mr = new mmf_Reader(path);
			if (mr.getMmfStack()==null) {
				System.out.println("null stack");
				return false;
			}
			
			String mdatname;
			if (dstName.lastIndexOf(".") <=0) {
				mdatname = dstName + ".mdat";
			} else {
				mdatname = dstName.replace(dstName.substring(dstName.lastIndexOf("."), dstName.length()), ".mdat");
			}
			File mdatfile = new File(dstDir, mdatname);
			if (!mdatfile.exists()) {
				mr.saveMetaData(mdatfile.getPath());
			}
			mmfStack = new ImagePlus(path, mr.getMmfStack());
			showStatus("MMF open");
			IJ.wait(1);
			IJ.freeMemory();
			
			return true;
		} catch (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			new TextWindow("Error opening mmf", sw.toString(), 500, 500);
			System.out.println("Experiment_processor.openMMF error");
			return false;
		} finally{
			indentLevel--;
		}
	}
	/**
	 * Loads an experiment into the processor from a .ser file
	 * @param dir
	 * @param filename
	 * @return Status of the file opening: true=successful
	 */
	private boolean openExp(String dir, String filename){
		indentLevel++;
		try {
			/*showStatus("Opening Experiment...");
			ex = new Experiment(Experiment.deserialize(new File(dir, filename).getPath())); 
			showStatus("Experiment open");*/
			showStatus("Opening Experiment...");
			ex = new Experiment(Experiment.fromPath(new File(dir, filename).getPath()));//Experiment.fromPath(new File(dir, filename).getPath());// 
			showStatus("Experiment open");
			return true;
		} catch (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			System.out.println("Experiment_processor.openExp error");
			new TextWindow("Error opening serialized experiment", sw.toString(), 500, 500);
			return false;
		} finally{
			indentLevel--;
		}
	}
	
	
	/**
	 * Sets up the Processing Log (A text file containing output RE processing events)
	 * <p>
	 * Log is saved in a txt file named according to the naming convention defined in the Processing Parameters. If the 
	 * file already exists, this log is appended to the end of that file
	 * @return Success of the creation of the log 
	 */
	private String setupLog(){
		
//		if (srcDir==null || srcName==null){
//			return "";
//		}
		
		indentLevel++;
		String[] logPathParts = {"not initialized", "not initialized"};
		try{
			logPathParts = prParams.setLogPath(srcDir, dstDir);
			String fname = new File(logPathParts[0], logPathParts[1]).getPath();
			PrintWriter processLog = new PrintWriter(new FileWriter(fname, true));
			
			processLog.println();
			processLog.println("----------------------------------------------");
			DateFormat df = new SimpleDateFormat("MM-dd-yyyy hh:mm z");
			processLog.println("Log entry at "+df.format(new Date()));
			processLog.println("----------------------------------------------");
			processLog.println();
			
			comm = new FileWriterCommunicator(processLog);
			comm.setVerbosity(verbosity);
			return fname;
		} catch (Exception e){
			new TextWindow("Log error", "Unable to create Processing Log file '"+logPathParts[1]+"' in '"+logPathParts[0]+"'", 500, 500);
			return "";
		} finally{
			indentLevel--;
		}
		
		
	}
	
	
	/**
	 * Runs track extraction on the imageStack
	 */
	private boolean extractTracks(){
		indentLevel++;
		String status = "";
		showStatus("Extracting tracks");
		if (mmfStack==null){
			status+="imageStack null; aborting extraction\n";
			return false;
		}
		extrParams.adjustBoundsForStack(mmfStack.getStack());
		status+="Running trackbuilder...\n";
		//comm.setVerbosity(VerbLevel.verb_debug);
		MaggotTrackBuilder tb = new MaggotTrackBuilder(mmfStack.getImageStack(), extrParams, comm);
		
		//IJ.showMessage("trackbuilder made");
		try {
			if (extrParams.doDdt) {
				log("Extracting tracks with ddt calculation", true);
			} else {
				log("Extracting tracks without ddt calculation", true);
			}
			//Extract the tracks
			tb.run();
			log("Converting Extracted Tracks to Experiment");
			status+="...Running complete! \n Converting to experiment... \n";
			ex = new Experiment(tb.toExperiment());
			status+="...Converted to Experiment\n";
			log("Track Extraction complete: Extracted "+ex.getNumTracks()+" tracks");
			
			//Show the extracted tracks
			if(prParams.showMagEx){
				showStatus("Showing Experiment");
				status+="Showing experiment...\n";
				ExperimentFrame exFrame = new ExperimentFrame(ex);
				exFrame.run(null);
				status+="...Experiment shown\n";
			}
			
			log("Extracted "+ex.getNumTracks()+" tracks", true);
			
			if (prParams.diagnosticIm){
				log("Generating diagnostic im");
				ImagePlus dIm1 = ex.getDiagnIm(mmfStack.getWidth(), mmfStack.getHeight());
				log("Done Generating diagnostic im; saving...");
				
				File diagDir = new File((dstDir!=null)? dstDir : srcDir, "diagnostics");
				if (!diagDir.exists()) {
					diagDir.mkdir();
				}
				String diagnPath = new File(diagDir, srcName).toString();
//				
//				String ps = File.separator;
//				String diagnPath = (dstDir!=null)? dstDir : srcDir;
//				File f = new File
//				diagnPath += "diagnostics"+ps;
//				if (!new File(diagnPath).exists()){
//					new File(diagnPath).mkdir();
//				}
//				diagnPath+= srcName;
				if (diagnPath.lastIndexOf(".") >= 0) {
					diagnPath = diagnPath.replace(diagnPath.substring(diagnPath.lastIndexOf("."), diagnPath.length()), " diagnostic foreground.bmp");
				} else{
					diagnPath = diagnPath + " diagnostic foreground.bmp";	
				}
				IJ.save(dIm1, diagnPath);
				log("Done saving diagnostic im");
			}
			
			if (prParams.saveFittingOutput){
				tb.comm.saveOutput((dstDir!=null)? dstDir : srcDir, "FittingOutput");
			}
			
			
		} catch  (Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log("Error opening experiment: \n"+sw.toString(), true);
			new TextWindow("Error extracting tracks", status+"\n"+sw.toString(), 500, 500);
			return false;
		} finally{
		//	tb.showCommOutput();
			indentLevel--;
		}
		return true;
	}
	/**
	 * Replaces each track in the experiment with a backbone-fitted track
	 */
	private void fitTracks(){
		indentLevel++;
		showStatus("Fitting Tracks...");
		log("Fitting "+ex.getNumTracks()+" Tracks", true);
		Track tr;
		Track newTr = null;
		Vector<Track> toRemove = new Vector<Track>();
		Vector<Track> errorsToSave = new Vector<Track>();
		
		int divergedCount=0;
		int shortCount=0;
		
		//Fit each track that is long enough for the course passes
		for (int i=0; i<ex.getNumTracks(); i++){
			
			if (i%fitParams.GCInterval==0){
				System.gc();
			}
			showStatus("Fitting Track "+(i+1)+"/"+ex.getNumTracks());
			tr = getTrackFromEx(i);//ex.getTrackFromInd(i);
			if (tr==null) { 
				log("Error loading track", true);
				break;
			}
			String trStr = "Track "+tr.getTrackID();
			if (tr.getNumPoints()>fitParams.minTrackLen) {//Check track length
				
//				bbf = new BackboneFitter(bbf.params);
//				newTr = fitTrack(tr);
				Timer.tic("fitTracks:bbf");

				BackboneFitter bbf = new BackboneFitter(tr, fitParams);
				if (prParams.fitType>0){
					bbf.fitTrackNewScheme();
				} else {
					bbf.fitTrack();
				}
				newTr = bbf.getTrack();
				
				String timStr = "(" + Timer.toc("fitTracks:bbf")/1000 + " s)";
				trStr+=" (#"+i+"/"+(ex.getNumTracks()-1)+")";
				if (newTr!=null){
					ex.replaceTrack(newTr, i);
					String msg = trStr+": done fitting "+timStr;
					if (bbf.wasClipped()) msg+=" (ends clipped)"; 
					if (newTr.suspicious) msg+=" (suspicious fit)";
//					if (prParams.doFitTiming) log("Track "+newTr.getTrackID()+": "+bbf.timingInfoToString(),true);
					log(msg, true);
				} else if (bbf.diverged()){
					divergedCount++;
					tr.setDiverged(true); 
					log(trStr+": diverged "+timStr, true);
					toRemove.add(tr);
					errorsToSave.add(tr);
				} else if (bbf.divergedGaps!=null && bbf.divergedGaps.size()>0){
					divergedCount++;
					log(trStr+": diverged, but was frozen "+timStr, true);
					errorsToSave.add(tr);
				} else {
					tr.setValid(false);
					log(trStr+": ERROR "+timStr, true);
					toRemove.add(tr);
//					errorsToSave.add(tr);
				}
				

				if (fitParams.storeEnergies){
					File f;
					if (!bbf.diverged()){
						f = new File(dstDir+"\\energyProfiles\\track"+i+"\\");
					} else {
						f = new File(dstDir+"\\energyProfiles\\diverged\\track"+i+"\\");
					}
					if (!f.exists()) f.mkdirs();
					bbf.saveEnergyProfiles(f.getAbsolutePath());
				}
				
				if (prParams.saveFittingOutput){
					
					File f = new File(dstDir+"\\FitterOutput\\track"+i+"\\");
					if (!f.exists()) f.mkdirs();
					bbf.saveCommOutput(f.getAbsolutePath());
					
				}
				
				
				IJ.showProgress(i+1, ex.getNumTracks());
			} else {
				toRemove.add(tr);
				tr.setValid(false);
				shortCount++;
				log(trStr+": too short to fit", true);
			//	toRemove.add(tr);
			}
			
			// TODO
//			bbf.saveCommOutput(dstDir);//bbf.showCommOutput();
		}
		
		
		
		
		
		showStatus("Done fitting tracks");
		log("Done fitting tracks: ", true);
		log(shortCount+"/"+ex.getNumTracks()+" were too short (minlength="+fitParams.minTrackLen+")", true);
		log(divergedCount+"/"+(ex.getNumTracks()-shortCount)+" remaining diverged", true);
		log((ex.getNumTracks()-toRemove.size()+shortCount)+"/"+(ex.getNumTracks()-shortCount-divergedCount)+" remaining were fit successfully", true);
		
		//Remove the tracks that couldn't be fit
		for(Track t : toRemove){
			ex.removeTrack(t);
		}
		
		if (prParams.diagnosticIm){
			
			int[] dim; 
			if (mmfStack!=null){
				dim = mmfStack.getDimensions();
			} else {
				dim = ex.getMaxPlateDimensions();
			}
			log("Generating diagnostic im (with fit)...", true);
			ImagePlus dIm1 = ex.getDiagnIm(dim[0], dim[1]);
			
			File diagDir = new File((dstDir!=null)? dstDir : srcDir, "fit diagnostics");
			if (!diagDir.exists()) {
				diagDir.mkdir();
			}
			String diagnPath = new File(diagDir, srcName).toString();
			if (diagnPath.lastIndexOf(".") >= 0) {
				diagnPath = diagnPath.replace(diagnPath.substring(diagnPath.lastIndexOf("."), diagnPath.length()), " diagnostic foreground.bmp");
			} else{
				diagnPath = diagnPath + " diagnostic foreground.bmp";	
			}
			IJ.save(dIm1, diagnPath);
			
			log("...Done generating diagnostic im", true);
			
		}
		
		if (prParams.saveErrors){
			log("Saving Error tracks...", true);
			saveErrorTracks(errorsToSave);
			log("...Done saving error tracks", true);
		}
		
		//Show the fitted tracks
		if (prParams.showFitEx){
			showStatus("Making experiment frame");
			ExperimentFrame exFrame = new ExperimentFrame(ex);
			showStatus("Experiment shown in frame");
			exFrame.run(null);
		} 
		indentLevel--;
	}
	
	
	private Track getTrackFromEx(int ind){
		
//		if (prParams.loadSingleTrackForFitting){
//			if (exName==null || exName.length()==0){
//				System.out.println("You told me to open one track at a time but there's no experiment file");
//				return null;
//			}
//			
//			return Experiment.getTrack(ind, exName);
//		} else {
		return ex.getTrackFromInd(ind);
		//return ex.tracks.get(ind);
//		}
		
		
		
	}
	

	
	/**
	 * Saves the maggotTracks according to the naming convention defined in the Processing Parameters
	 */
	private boolean saveOldTracks(){
		indentLevel++;
		File f = new File(prParams.setMagExPath(srcDir, srcName, dstDir, dstName));
		log("Saving MaggotTrack experiment to "+f.getPath(), true);
		boolean status;
		try{
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f))); 
			ex.setCommunicator(comm);
			ex.toDisk(dos);
			status=true;
			dos.close();
			//exName = f.getPath();
			
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			e.printStackTrace(pw);
			log("Error saving experiment after extraction: \n"+sw.toString(), true);
			status=false;
		}
		
		indentLevel--;
		return status;
	}
	/**
	 * Saves the backboneTracks according to the naming convention defined in the Processing Parameters
	 */
	private boolean saveNewTracks(){
		indentLevel++;
		
		File f = new File(prParams.setFitExPath(srcDir, srcName, dstDir, dstName));
		log("Saving LarvaTrack experiment to "+f.getPath(),true);
		boolean status;
		try{
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f))); 
			ex.setCommunicator(comm);
			ex.toDisk(dos);
			status=true;
			dos.close();
			log("Done saving LarvaTrack experiment", true);
		} catch(Exception e){
			status=false;
			log("Experiment_processor.saveNewTracks error");
		}
		
		indentLevel--;
		return status;
	}
	

	
	private void saveErrorTracks(Vector<Track> errTracks){
		String fDst = (dstDir!=null)? dstDir : srcDir;
		File f = new File(fDst+File.separator+"divergedTrackExp.jav");
		log("Saving error track experiment to "+f.getPath(), true);
		try{
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f))); 
			Experiment errEx = new Experiment(ex, errTracks);
			errEx.setCommunicator(comm);
			errEx.toDisk(dos);
			dos.close();
			log("Done saving error track experiment", true);
		} catch(Exception e){
			log("Experiment_processor.saveErrorTracks error", true);
		}
	}
	
	protected void readParamsFromFile(){

		//Overwrite any other parameters
		prParams = new ProcessingParameters();
		extrParams = new ExtractionParameters();
		fitParams = new FittingParameters();
		csvPrefs = new CSVPrefs();
		
		String delimiter = ":";
		
		Scanner s = null;
		try{
			//Open the file
			s = new Scanner(new File(paramFileName));
			
			//Read the data
			int paramType = -1;
			Hashtable<String, Integer> paramTable = initTable();

			String line;
			String nameStr;
			String valueStr;
			boolean success = true;

			while (s.hasNextLine() && success){
			
				line = s.nextLine();
				int dInd = line.indexOf(delimiter);
				nameStr = line.substring(0, dInd);
				int typeVal;
				if (paramTable.get(nameStr)==null){
					typeVal=-1;
				} else {
					typeVal = paramTable.get(nameStr);
				}
				
				if(typeVal>0){ //this is a new heading
					paramType = typeVal;
				} else { //this is a parameter
					valueStr = line.substring(dInd+1).replaceAll(" ", "");//Skip char for delimiter
					success = setParam(paramType, nameStr, valueStr);
					System.out.print("");
				}
				
			}
		} catch (Exception e){
			System.out.println(e.getMessage());
		} finally {
			if (s!=null){
				s.close();
			}
		}

		

	}
	
	private Hashtable<String, Integer> initTable(){
		
		Hashtable<String, Integer> paramTable = new Hashtable<String, Integer>();
		
		paramTable.put("PROCESSING", 1);
		paramTable.put("EXTRACTION", 2);
		paramTable.put("FITTING", 3);
		paramTable.put("CSV", 4);

		return paramTable;
	}
	
	private boolean setParam(int paramType, String paramName, String paramVal){
		
		//get the Field for the param
		Field f = null;
		Object oVal;
		try{
			switch (paramType) {
			case 1:
				f = ProcessingParameters.class.getField(paramName);
				oVal = toObject(f.getType(), paramVal);
				f.set(prParams, oVal);
				break;
			case 2:
				f = ExtractionParameters.class.getField(paramName);
				oVal = toObject(f.getType(), paramVal);
				f.set(extrParams, oVal);
				break;
			case 3:
				f = FittingParameters.class.getField(paramName);
				oVal = toObject(f.getType(), paramVal);
				f.set(fitParams, oVal);
				break;
			case 4:
				f = CSVPrefs.class.getField(paramName);
				oVal = toObject(f.getType(), paramVal);
				f.set(csvPrefs, oVal);
				break;
			default:
				break;
			}
			return true;
		} catch (Exception e){
			System.out.println("Error setting parameters from file");
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			System.out.println(sw.toString());
			return false;
		}
		
		
	}
	
	public static Object toObject( Class<?> clazz, String value ) {
		
		if(clazz.isArray()){
			return arrayToObject(clazz.getComponentType(), value);			
		}
		
	    if( Boolean.class == clazz || clazz.getName().equals("boolean")) return Boolean.parseBoolean( value );
	    if( Byte.class == clazz || clazz.getName().equals("byte")) return Byte.parseByte( value );
	    if( Short.class == clazz || clazz.getName().equals("short")) return Short.parseShort( value );
	    if( Integer.class == clazz || clazz.getName().equals("int")) return Integer.parseInt( value );
	    if( Long.class == clazz || clazz.getName().equals("long")) return Long.parseLong( value );
	    if( Float.class == clazz || clazz.getName().equals("float")) return Float.parseFloat( value );
	    if( Double.class == clazz  || clazz.getName().equals("double")) return Double.parseDouble( value );
	    return value;
	}
	
	public static Object arrayToObject(Class<?> clazz_in, String value){
		
		//remove all white space and and the outermost brackets
		String[] arr = stringToStringArray(value, !clazz_in.isArray());
		int nItems = arr.length;
		
		boolean arrofarr = false;
		Class<?> clazz;
		if(clazz_in.isArray()){
			arrofarr = true;
			clazz = clazz_in.getComponentType();
		} else {
			clazz = clazz_in;
		}
		
		if (clazz.isArray()){
			return null;
		} else if( Integer.class == clazz || clazz.getName().equals("int")){
	    	if (arrofarr){
	    		int[][] obj = new int[nItems][];
				for (int i=0; i<nItems; i++){
					int[] objrow= (int[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		int[] obj = new int[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Integer)toObject(clazz, arr[i]); 
				}
				return obj;
	    	}
	    } else if( Boolean.class == clazz || clazz.getName().equals("boolean")){
	    	if (arrofarr){
	    		boolean[][] obj = new boolean[nItems][];
				for (int i=0; i<nItems; i++){
					boolean[] objrow= (boolean[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		boolean[] obj = new boolean[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Boolean)toObject(clazz, arr[i]); 
				}
				return obj;
	    	}
		} else if( Byte.class == clazz || clazz.getName().equals("byte")){
			if (arrofarr){
				byte[][] obj = new byte[nItems][];
				for (int i=0; i<nItems; i++){
					byte[] objrow= (byte[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		byte[] obj = new byte[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Byte)toObject(clazz, arr[i]); 
				}
				return obj;
			}
	    } else if( Short.class == clazz || clazz.getName().equals("short")){
	    	if (arrofarr){
	    		short[][] obj = new short[nItems][];
				for (int i=0; i<nItems; i++){
					short[] objrow= (short[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		short[] obj = new short[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Short)toObject(clazz, arr[i]); 
				}
				return obj;
	    	}
	    } else if( Long.class == clazz || clazz.getName().equals("long")){
	    	if (arrofarr){
	    		short[][] obj = new short[nItems][];
				for (int i=0; i<nItems; i++){
					short[] objrow= (short[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		long[] obj = new long[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Long)toObject(clazz, arr[i]); 
				}
		    	return obj;
	    	}
	    } else if( Float.class == clazz || clazz.getName().equals("float")){
	    	if (arrofarr){
	    		float[][] obj = new float[nItems][((float[])toObject(clazz_in, arr[0])).length];
				for (int i=0; i<nItems; i++){
					float[] objrow= (float[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		float[] obj = new float[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Float)toObject(clazz, arr[i]); 
				}
				return obj;
	    	}
	    } else if( Double.class == clazz  || clazz.getName().equals("double")){
	    	if (arrofarr){
	    		double[][] obj = new double[nItems][];
				for (int i=0; i<nItems; i++){
					double[] objrow= (double[])toObject(clazz_in, arr[i]);
					for (int j=0; j<objrow.length; j++){
						obj[i][j] = objrow[j];
					}
				}
				return obj;
	    	} else{
	    		double[] obj = new double[nItems];
				for (int i=0; i<nItems; i++){
					obj[i] = (Double)toObject(clazz, arr[i]); 
				}
				return obj;
	    	}
	    } else {
	    	return null;
	    }
		
	}
	
	private static String[] stringToStringArray(String value, boolean oneDimArray){
		
		String arrStr = value.replaceAll("\\s+", "").substring(value.indexOf("[")+1, value.length()-1);
		String[] arr;
		
		if (oneDimArray){
			arr = arrStr.split(",");
		}else{
			ArrayList<String> subStrs = new ArrayList<String>();
			int nOnStack=0;
			int startInd = 0;
			for (int i=0; i<arrStr.length(); i++){
				//do something with the character/stack
				if (arrStr.charAt(i)=='['){
					nOnStack++;
				} else if (arrStr.charAt(i)==']'){
					nOnStack--;
					if (nOnStack==0){
						subStrs.add(arrStr.substring(startInd, i+1));
						startInd = i+2;//add 2 to skip comma
					}
				}
			}
			
			arr = new String[subStrs.size()];
			for (int j=0; j<subStrs.size(); j++){
				arr[j]=subStrs.get(j);
			}
		}
		
		return arr;
	}
	
	protected void writeParamsToFile(){
		
		//Open file 
		BufferedWriter bw = null;
		File f = new File(paramFileName);
		if(!f.exists()){
			try {
				f.createNewFile();
			} catch(Exception e) {
				System.out.println("Error creating File for param data");
				return;
			}
		}
		
		try {
			bw = new BufferedWriter(new FileWriter(f));
			
			bw.write("PROCESSING:");
			bw.newLine();
			classToDisk(bw,ProcessingParameters.class,prParams);
			bw.write("EXTRACTION:");
			bw.newLine();
			classToDisk(bw, ExtractionParameters.class, extrParams);
			bw.write("FITTING:");
			bw.newLine();
			classToDisk(bw, FittingParameters.class, fitParams);
			bw.write("CSV:");
			bw.newLine();
			classToDisk(bw, CSVPrefs.class, csvPrefs);
			
			bw.close();
			
		} catch (Exception e){
			System.out.println(e.getMessage());
		} finally {
			if (bw!=null){
			}
		}
		
		
	}
	
	public static void classToDisk(BufferedWriter bw, Class<?> c, Object obj ) throws IOException, IllegalArgumentException, IllegalAccessException{
		
		for (Field f : c.getFields()){
			String s;
			if (f.get(obj).getClass().isArray()){
				s = objectArrayToString(f.get(obj));
			} else {
				s = f.get(obj).toString();
			}
			
			bw.write(f.getName()+": "+s+System.getProperty("line.separator"));
		}
		
		
	}
	
	private static String objectArrayToString(Object obj){
		
		if (!obj.getClass().isArray()){
			//handle non-arrays
			return obj.toString();
		} else {
			char c = obj.getClass().getName().charAt(1);
			boolean arrofarr = false;
			
			//handle Object arrays
			if (c=='L'){
				return Arrays.deepToString((Object[])obj);
			}
			
			if (c=='['){
				//handle arrays of arrays
				arrofarr=true;
				c=obj.getClass().getName().charAt(2);
			}
			
			//handle primitive type arrays
			if (c=='['){
				return "multidimensional array (>2 levels)";
			} else if (c=='I'){
				return (arrofarr)? Arrays.deepToString((int[][])obj)	: Arrays.toString((int[])obj);
			} else if (c=='Z'){
				return (arrofarr)? Arrays.deepToString((boolean[][])obj): Arrays.toString((boolean[])obj);
			} else if (c=='B'){
				return (arrofarr)? Arrays.deepToString((byte[][])obj)	: Arrays.toString((byte[])obj);
			} else if (c=='C'){
				return (arrofarr)? Arrays.deepToString((char[][])obj)	: Arrays.toString((char[])obj);
			} else if (c=='D'){
				return (arrofarr)? Arrays.deepToString((double[][])obj)	: Arrays.toString((double[])obj);
			} else if (c=='F'){
				return (arrofarr)? Arrays.deepToString((float[][])obj)	: Arrays.toString((float[])obj);
			} else if (c=='J'){
				return (arrofarr)? Arrays.deepToString((long[][])obj)	: Arrays.toString((long[])obj);
			} else if (c=='S'){
				return (arrofarr)? Arrays.deepToString((short[][])obj)	: Arrays.toString((short[])obj);
			} else {
				return "array (error getting string)";
			}
			
		}
	}

	
	private void log(String message){
		log(message, false);
	}
	
	
	private void log(String message, boolean echo){
		if (echo) System.out.println(message);
		
		String indent = "";
		for (int i=0;i<indentLevel; i++) indent+="----";
		VerbLevel vb = comm.verbosity;
		comm.verbosity = VerbLevel.verb_message;
		comm.message(runTime.getElapsedTime()*0.001+" sec: "+indent+" "+message, VerbLevel.verb_message);
		comm.verbosity = vb;
	}

	public VerbLevel getVerbosity() {
		return verbosity;
	}

	public void setVerbosity(VerbLevel verbosity) {
		this.verbosity = verbosity;
		if (comm != null) {
			comm.setVerbosity(verbosity);
		}
	}
	
}


