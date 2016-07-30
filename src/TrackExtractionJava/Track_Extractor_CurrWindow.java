package TrackExtractionJava;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


//import ij.ImageJ;
import ij.ImageStack;
import ij.WindowManager;
import ij.plugin.PlugIn;
import ij.text.TextWindow;
import TrackExtractionJava.Track_Extractor.*;

@SuppressWarnings("unused")
public class Track_Extractor_CurrWindow implements PlugIn{
	
	// Set extraction parameters EXTRACTIONPARAMETERS
	ExtractionParameters ep; 
	
	// Load the mmfs into an imagestack
	ImageStack IS;
	
	// Build the tracks TRACKBUILDER
	TrackBuilder tb;
	//ep= new ExtractionParameters()
	ExperimentFrame ef;
	
	public void run(String arg) {
				
		
		ExtractorFrameCW ef = new ExtractorFrameCW();
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
        
		Track_Extractor_CurrWindow te = new Track_Extractor_CurrWindow();
		te.run("");
}
	
}



class ExtractorFrameCW extends JFrame{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	JPanel mainPanel;
	//int tabPlacement = JTabbedPane.TOP;
	Dimension panelSize = new Dimension(500,650);
	String panelName = "Experiment Processor"; 
	
	InputPanel input;
	ParamPanel params;
	OutputPanel output;
	JPanel buttonPanel;
	JButton runButton;
	
	public ExtractorFrameCW(){
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
	
	
	public void run(String args){

		buildFrame();
		
		showFrame();
		
	}
	
	private void buildFrame(){
		
		//Build components
		input = new InputPanel();
		output = new OutputPanel();
		params = new ParamPanel();
		input.outputDirFld = output.dirTxFld;
		input.outputNameFld = output.nameTxFld;
		
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
		
		mainPanel.add(new JSeparator(JSeparator.HORIZONTAL));
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
		ep.runningFromMain = false;
		ep.prParams = params.procParams;
		ep.extrParams = params.extrParams;
		ep.fitParams = params.fitParams;
		ep.csvPrefs = params.cPrefs;
		
		//Set src and dest
		String[] epArgs = new String[3];
		epArgs[0] = "current"; //input.txFld.getText(); //src - "current"
		epArgs[1] = output.dirTxFld.getText(); //dstdir
		epArgs[2] = output.nameTxFld.getText(); //dstname
		
		ep.run(epArgs);

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







