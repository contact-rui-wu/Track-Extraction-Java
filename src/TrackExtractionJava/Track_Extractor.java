package TrackExtractionJava;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import ij.IJ;
import ij.ImageJ;
//import ij.ImageJ;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
//import ij.plugin.frame.RoiManager;
import ij.text.TextWindow;


public class Track_Extractor implements PlugIn{
	
	// Set extraction parameters EXTRACTIONPARAMETERS
	//ExtractionParameters ep; 
	//SimpleExtractionParameters sep;
	
	// Load the mmfs into an imagestack
	//ImageStack IS;
	
	// Build the tracks TRACKBUILDER
	//TrackBuilder tb;
	//ep= new ExtractionParameters()
	//ExperimentFrame ef;
	
	public Track_Extractor() {
		//sep = new SimpleExtractionParameters();
	}
	
	public void run(String arg) {
				
		
		ExtractorFrame ef = new ExtractorFrame();
		ef.run(null);
		
	}
	
	
	public ImageStack getStack(){
		
		ImageStack stack = WindowManager.getCurrentWindow().getImagePlus().getImageStack();
//		WindowManager.getCurrentWindow().getImagePlus().setOverlay(null);
//		WindowManager.getCurrentWindow().close();
//		RoiManager.getInstance().move;
//		WindowManager.getCurrentWindow().close();
		return stack;
	}
	
	public static void main(String[] args) {
	// set the plugins.dir property to make the plugin appear in the Plugins menu
			Class<?> clazz = Track_Extractor.class; 
	        String url = clazz.getResource("/" + clazz.getName().replace('.', '/') + ".class").toString();
	        String pluginsDir = url.substring(5, url.length() - clazz.getName().length() - 6);
	        System.setProperty("plugins.dir", pluginsDir);
			
	        // start ImageJ
	        new ImageJ();

	        // run the plugin
	        IJ.runPlugIn(clazz.getName(), "");
		}
	
}



class ExtractorFrame extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JPanel mainPanel;
	//int tabPlacement = JTabbedPane.TOP;
	Dimension panelSize = new Dimension(500,650);
	String panelName = "Experiment Processor"; 
	
	InputPanel input;
	SimpleParamPanel params;
	OutputPanel output;
	JPanel buttonPanel;
	JButton runButton;
	SimpleExtractionParameters sep;
	CSVPrefs cp;
	boolean savetoCSV = true;
	boolean currentWindow = false;
	
	
	public ExtractorFrame () {
		this(false);
	}
	
	public ExtractorFrame(boolean currentWindow){
		this.currentWindow = currentWindow; //whether to use the current window or load from disk
		sep = new SimpleExtractionParameters();
		cp = new CSVPrefs();
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	
	public void run(String args){

		buildFrame();
		
		showFrame();
		
	}
	
	private void buildFrame(){
		
		//Build components
		output = new OutputPanel();
		params = new SimpleParamPanel(this);
		if (!currentWindow) {
			input = new InputPanel();	
			input.outputDirFld = output.dirTxFld;
			input.outputNameFld = output.nameTxFld;
		}
		runButton = new JButton("Run Extraction");
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int statecode = validRunState();
				if (statecode==0){
					runProcessor();
					//TODO close window, show progress
				} else {
					
					String message = "";
					switch(statecode){
					case 2:
						
						break;
					default: 
						message += "Unable to process";
					}
					
					new TextWindow("Processing Message", message, 200, 200);	
				}
			}
		} );
		runButton.getModel().addChangeListener(new ChangeListener() {
			
			@Override
			public void stateChanged(ChangeEvent e) {
				ButtonModel model = (ButtonModel) e.getSource();
				if(model.isPressed()){
					runButton.setText("Running...");
				} else {
					runButton.setText("Run Extraction");
				}
				
			}
		});
		
		buttonPanel = new JPanel();
		buttonPanel.add(runButton);
		
		//Add them to the MainPanel
		mainPanel = new JPanel(); //new JTabbedPane(tabPlacement);
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
		
		if (!currentWindow) {
			mainPanel.add(makeLabelPanel("Source"));
			mainPanel.add("Select input...", input);
			mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
		}
		mainPanel.add(makeLabelPanel("Parameters"));
		mainPanel.add("Parameters...", params);
		mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
		mainPanel.add(makeLabelPanel("Destination"));
		mainPanel.add("Select output...", output);
		mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
		mainPanel.add("Run Extraction", buttonPanel);
		
		//Add mainPanel to frame
		add(mainPanel);
		
		pack();
	}
	
	
	private void showFrame(){
		setSize(panelSize);
		setTitle(panelName);
		setVisible(true);
	}
	
	private int validRunState(){
		
		
		//TODO
		return 0;
	}
	
	private void runProcessor(){
		

//		ImageJ imj = new ImageJ(ImageJ.NO_SHOW);
		Experiment_Processor ep = new Experiment_Processor();
		
		
		//Set params from input
	//	ep.runningFromMain = false;
		ProcessingParameters ppr = sep.getProcessingParameters();
		ppr.savetoCSV = savetoCSV;
		
		ep.prParams = ppr;
		ep.extrParams = sep.getExtractionParameters();
		ep.fitParams = sep.getFittingParameters();
		ep.csvPrefs = cp;
		
		//Set src and dest
		String[] epArgs = new String[3];
		if (currentWindow) {
			epArgs[0] = "current";
		} else {
			epArgs[0] = input.txFld.getText(); //src - "current"
		}
		epArgs[1] = output.dirTxFld.getText(); //dstdir
		epArgs[2] = output.nameTxFld.getText(); //dstname
		
		//run in a different thread so as not to block UI execution
		new Thread(new EPRunner(ep, epArgs)).start();
		
		//ep.run(epArgs);

//		imj.quit();
	}
	
	public JPanel makeLabelPanel(String labelText){
		
		JPanel labelPanel = new JPanel();
		JLabel label = new JLabel(labelText);
		label.setFont(new Font(label.getFont().getName(), Font.BOLD, label.getFont().getSize()*2));
		labelPanel.add(label);
		
		return labelPanel;
	}
}







class InputPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	//Internal Elements
	JTextField txFld;
	JButton flChButton;
	JFileChooser flCh;
	
	Experiment ex;
	JPanel descPanel;
	JTextArea desc;
	
	//External Elements
	JTextField outputDirFld;
	JTextField outputNameFld;
	

	static String txFldDisplay = "Choose a file...";
	int txFldNColumns = 20;
	
	//Constructors
	public InputPanel(){
		
		buildPanel();
	}
	
	//Builders
	private void buildPanel(){
		
		buildComponents();
		
		//put components together
		JPanel srcChooserBox = new JPanel();
		srcChooserBox.add(txFld);
		srcChooserBox.add(flChButton);
		
		JPanel descBox = new JPanel();
		descBox.setSize(30, 5);
		descBox.add(desc);
		
		add(srcChooserBox);
//		add(descBox);
		
		
	}
	
	private void buildComponents(){

		//build the experiment description
		desc = new JTextArea("Experiment...",2, 20);
		
		
		//build the source name text field
		txFld = new JTextField(txFldDisplay,txFldNColumns);
		txFld.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//Get the file name
				openExpt(txFld.getText());
			}
		});

		//build the file choosing button & file chooser
		
		flCh = new JFileChooser();
		flChButton = new JButton("Browse...");
		
		flChButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = flCh.showOpenDialog(txFld);
				
				if (result==JFileChooser.APPROVE_OPTION){
					
					txFld.setText(flCh.getSelectedFile().getPath());
					openExpt(flCh.getSelectedFile().getPath());
					
					setOutput();
					
					
				}
			}
		});
		
		
		
	}
	
	
	//Auxiliary functions
	private void setOutput(){
		
		File f = new File(txFld.getText());
		String dir = f.getParent();
		String name = f.getName();
		
		//If no destination exists, make a suggestion 
		if(outputDirFld.getText().equals(OutputPanel.dirTxFldDisplay)){
			outputDirFld.setText(dir);
		}
		
		if (outputNameFld.getText().equals(OutputPanel.nameTxFldDisplay)){
			int i = name.lastIndexOf(".");
			if ( i > 0) {
				outputNameFld.setText(name.substring(0, i));
			}else {
				outputNameFld.setText(name);
			}
					
		}
	}
	
	private void openExpt(String path){
		//Try to open experiment
		desc.setText("Opening experiment...");
		ex = Experiment.fromPath(path);
		
		if (ex!=null){
			desc.setText("Experiment: "+ex.getNumTracks()+" tracks");
		} else{
			desc.setText("Could not open file");
		}
	}
	
}



class OutputPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	

	JTextField dirTxFld;
	JButton flChButton;
	JFileChooser flCh;
	
	JTextField nameTxFld;
	
	
	static String dirTxFldDisplay = "Choose save directory...";
	int dirTxFldNColumns = 20;
	static String nameTxFldDisplay = "Choose save name...";
	int nameTxFldNColumns = 20;
	
	public OutputPanel(){
		buildPanel();
	}
	
	private void buildPanel(){
		
		//Build components
		buildComponents();
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS)); 
		
		//Put them together
		JPanel dirChooserPanel = new JPanel();
		dirChooserPanel.add(dirTxFld);
		dirChooserPanel.add(flChButton);
		JPanel namePanel = new JPanel();
		namePanel.add(nameTxFld);
		
		add(dirChooserPanel);
		add(namePanel);
		
	}
	
	private void buildComponents(){
		
		
		
		//Build the dir  text field
		dirTxFld = new JTextField(dirTxFldDisplay,dirTxFldNColumns);
		
		//build the dir chooser and button
		flCh = new JFileChooser();
		flCh.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		flChButton = new JButton("Browse...");
		
		flChButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int result = flCh.showSaveDialog(dirTxFld);
				if (result==JFileChooser.APPROVE_OPTION){
					dirTxFld.setText(flCh.getSelectedFile().getPath());
				}
			}
		});
		
		//build the name text field
		nameTxFld = new JTextField(nameTxFldDisplay,nameTxFldNColumns);
	}
	
	
}
class SimpleParamPanel extends JPanel{
	/**
	 * 
	 */
	private static final long serialVersionUID = -6409294234655572186L;
	//CSVPrefs cPrefs;
	JButton cPrefButton;
	JFrame cPrefFrame;
	JPanel cPrefPanel;
	JPanel sepPanel;
	//SimpleExtractionParameters sep;
	//boolean savetoCSV;
	JCheckBox toCSVBox;
	String toCSVName = "Save track data to CSV";
	ExtractorFrame ef;
	
	public SimpleParamPanel (ExtractorFrame ef) {
		this.ef = ef;
		buildComponents();
		buildPanel();
	}
	
	private void buildComponents(){
		
		cPrefButton = new JButton("Set CSV Saving Preferences");
		cPrefButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				setCSVPrefs();
			}
		});
		cPrefPanel = new JPanel();
		
		

		toCSVBox = new  JCheckBox(toCSVName, ef.savetoCSV);
		toCSVBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				ef.savetoCSV = toCSVBox.isSelected();
				
			}
		});
		cPrefPanel.add(cPrefButton);
		cPrefPanel.add(toCSVBox);
		sepPanel = ef.sep.fundamentalSettingsPanel();
	
	}
	private void buildPanel(){
		//Build the components
		buildComponents();
		
		//Add components to the panel
		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
		
		add(cPrefPanel);
		add(sepPanel);
		
	}
	
	private void setCSVPrefs(){
		
		cPrefFrame = new JFrame();
		
		//Build components
		csvPrefPanel cpp = new csvPrefPanel(ef.cp);
		JButton okButton = new JButton("OK");
		okButton.addActionListener(new ActionListener() {
			
			public void actionPerformed(ActionEvent e) {
				ef.savetoCSV = true;
				toCSVBox.setSelected(true);
				cPrefFrame.dispose();
			}
		});
		
		//Display components in frame
		cPrefFrame.setLayout(new BorderLayout());
		cPrefFrame.add(cpp, BorderLayout.CENTER);
		cPrefFrame.add(okButton, BorderLayout.SOUTH);
		
		cPrefFrame.pack();

		cPrefFrame.setTitle("Test Frame for CSV preferences");
		cPrefFrame.setVisible(true);
	}
	
}
//
//class ParamPanel extends JPanel{
//
//	/**
//	 * 
//	 */
//	private static final long serialVersionUID = 1L;
//	
//	ProcessingParameters procParams;
//	ProcPanel pp;
//	ExtractionParameters extrParams;
//	extrPanel ep;
//	FittingParameters fitParams;
//	
//	CSVPrefs cPrefs;
//	JButton cPrefButton;
//	JFrame cPrefFrame;
//	JPanel cPrefPanel;
//	
//	public ParamPanel(){
////		init(sep, cp);
//		init(null, null, null, null);
//		buildPanel();
//	}
//	
//	public ParamPanel(ProcessingParameters pp, ExtractionParameters ep, FittingParameters fp, CSVPrefs cp){
//		init(pp, ep, fp, cp);
//		buildPanel();
//	}
//	
//	private void init(ProcessingParameters pp, ExtractionParameters ep, FittingParameters fp, CSVPrefs cp){
//		
//		if (pp==null){
//			procParams = new ProcessingParameters();
//		} else {
//			procParams = pp;
//		}
//		
//		if (ep==null){
//			extrParams = new ExtractionParameters();
//		} else {
//			extrParams = ep;
//		}
//		
//		if (fp==null){
//			fitParams = new FittingParameters();
//		} else {
//			fitParams = fp;
//		}
//		
//		if (cp==null){
//			cPrefs = new CSVPrefs();
//		} else {
//			cPrefs = cp;
//		}
//		
//	}
//	
//	private void buildPanel(){
//		//Build the components
//		buildComponents();
//		
//		//Add components to the panel
//		setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
//		
//		add(pp);
//		add(cPrefPanel);
//		add(ep);
//		
//	}
//	
//	private void buildComponents(){
//		
//		pp = procParams.getPanel();
//		pp.setAlignmentX(Component.CENTER_ALIGNMENT);
//		ep = extrParams.getPanel();
//		ep.setAlignmentX(Component.CENTER_ALIGNMENT);
//		
//		cPrefButton = new JButton("Set CSV Saving Preferences");
//		cPrefButton.addActionListener(new ActionListener() {
//			
//			public void actionPerformed(ActionEvent e) {
//				setCSVPrefs();
//			}
//		});
//		cPrefPanel = new JPanel();
//		cPrefPanel.add(cPrefButton);
//		
//	}
//	
//	private void setCSVPrefs(){
//		
//		cPrefFrame = new JFrame();
//		
//		//Build components
//		csvPrefPanel cpp = new csvPrefPanel(cPrefs);
//		JButton okButton = new JButton("OK");
//		okButton.addActionListener(new ActionListener() {
//			
//			public void actionPerformed(ActionEvent e) {
//				procParams.savetoCSV = true;
//				pp.toCSVBox.setSelected(true);
//				cPrefFrame.dispose();
//			}
//		});
//		
//		//Display components in frame
//		cPrefFrame.setLayout(new BorderLayout());
//		cPrefFrame.add(cpp, BorderLayout.CENTER);
//		cPrefFrame.add(okButton, BorderLayout.SOUTH);
//		
//		cPrefFrame.pack();
//
//		cPrefFrame.setTitle("Test Frame for CSV preferences");
//		cPrefFrame.setVisible(true);
//	}
//	
//	
//}

class EPRunner implements Runnable {

	Experiment_Processor ep;
	String[] epArgs;
	
	public EPRunner (Experiment_Processor ep, String[] epArgs) {
		this.ep = ep;
		this.epArgs = epArgs;
	}
	@Override
	public void run() {
		this.ep.run(epArgs);
		
	}
	
}

class ProgressFrame extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JLabel progLabel;
	
	public ProgressFrame() {
		setTitle("Progress");
	}
	
	
	
	
	public static void updateProgress(ProgressFrame pf, String statusUpdate){
		if (pf!=null){
			
		}
	}
}