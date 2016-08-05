package TrackExtractionJava;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SimpleExtractionParameters {
	//gives access to the most commonly used extraction and fitting parameters
	
	public int endFrame = Integer.MAX_VALUE;
	public int startFrame = 1;
	public int globalThreshValue = 30;
	public float frameRate = 20;
	public float typicalLarvaLengthInPixels = 20;
	public float timeLengthScaleFactor = 0.5f;
	public float timeSmoothScaleFactor = 0.25f;
	public float imageWeight = 1;
	public float spineLengthWeight = 1.0f;
	public float spineSmoothWeight = 0.5f;
	public float spineExpansionWeight = 1;
	public float minimumDuration = 10;
	public boolean gaussCluster = false;
	
	public SimpleExtractionParameters() {
		// TODO Auto-generated constructor stub
	}
	
	public ProcessingParameters getProcessingParameters() {
		ProcessingParameters pp = new ProcessingParameters();
		//pp.minTrackLen = (int) (minimumDuration*frameRate);
		return pp;
	}
	
	public ExtractionParameters getExtractionParameters () {
		ExtractionParameters ep = new ExtractionParameters();
		ep.endFrame = endFrame;
		ep.startFrame = startFrame;
		ep.globalThreshValue = globalThreshValue;
		ep.minArea = typicalLarvaLengthInPixels;
		ep.maxArea = typicalLarvaLengthInPixels*typicalLarvaLengthInPixels;
		ep.maxMatchDist = typicalLarvaLengthInPixels/2 + typicalLarvaLengthInPixels/frameRate;
		ep.framesBtwnContSegs = (int) (1.5*frameRate); //max 1.5 seconds to lose HT 
		return ep;
	}
	
	public FittingParameters getFittingParameters() {
		FittingParameters fp = new FittingParameters();
		fp.imageWeight = imageWeight;
		fp.spineExpansionWeight = spineExpansionWeight;
		fp.spineLengthWeight = spineLengthWeight;
		fp.spineSmoothWeight = spineSmoothWeight;
		fp.timeLengthWeight = new float[] {(float) (timeLengthScaleFactor*frameRate/20)};
		fp.timeSmoothWeight = new float[] {(float) (timeSmoothScaleFactor*frameRate/20)};
		fp.minTrackLen = (int) (minimumDuration * frameRate);
		fp.clusterMethod = gaussCluster ? 1 : 0;
		fp.minValidSegmentLen = (int) (1.5*frameRate);
		return fp;
	}
	
	public JFrame parameterFrame(FundamentalSettingsPanel fsp) {
		return new SEPFrame(this, fsp);
	}
	public JFrame parameterFrame() {
		return new SEPFrame(this);
	}
	
	public JPanel fundamentalSettingsPanel () {
		return new FundamentalSettingsPanel(this);
	}
	
	JPanel numberPanels() {
		return NumberChangerPanel.numericalFieldsPanel(this);
	}
}

class SEPFrame extends JFrame {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -8002121773033141804L;
	FundamentalSettingsPanel fsp;
	public SEPFrame(SimpleExtractionParameters sep, FundamentalSettingsPanel fspan) {
		this.fsp = fspan;
		add(sep.numberPanels());
		addWindowListener(new WindowListener() {
			
			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void windowDeactivated(WindowEvent e) {
				if(fsp != null) {
					fsp.updateFields();
				}
				
			}
			
			@Override
			public void windowClosing(WindowEvent e) {
				if(fsp != null) {
					fsp.updateFields();
				}
				
			}
			
			@Override
			public void windowClosed(WindowEvent e) {
				if(fsp != null) {
					fsp.updateFields();
				}
				
			}
			
			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
		});
//		JButton close = new JButton("close");
//		
//		close.addActionListener(new ActionListener() {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				if(fsp != null) {
//					fsp.updateFields();
//				}
//				dispose();
//			}
//		});
//		JPanel buttonP = new JPanel();
//		buttonP.add(close);
//		add(buttonP);
		pack();
		setVisible(true);
	}
	public SEPFrame(SimpleExtractionParameters sep) {
		this(sep, null);
	}
}

class FundamentalSettingsPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = -8213016771194898699L;
	SimpleExtractionParameters sep;
	JFormattedTextField frameRateField;
	JPanel frameRatePanel;
	JLabel frameRateLabel;
	JFormattedTextField typicalLarvaLengthInPixelsField;
	JPanel typicalLarvaLengthInPixelsPanel;
	JLabel typicalLarvaLengthInPixelsLabel;
	
	JFormattedTextField globalThreshValueField;
	JPanel globalThreshValuePanel;
	JLabel globalThreshValueLabel;
	
	JButton moreParamsButton;
	
	public FundamentalSettingsPanel(SimpleExtractionParameters sep) {
		this.sep = sep;
		init();
	}
	private void init() {
		frameRateField = new JFormattedTextField(NumberFormat.getInstance());
		frameRateField.setValue(sep.frameRate); 
		frameRateField.setColumns(4);
		frameRateField.addPropertyChangeListener( new PropertyChangeListener() {		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				sep.frameRate = ((Number) frameRateField.getValue()).floatValue();
			}
		});		
		frameRateLabel = new JLabel("frame rate (Hz)");
		frameRatePanel = new JPanel(new BorderLayout());
		frameRatePanel.add(frameRateField, BorderLayout.WEST);
		frameRatePanel.add(frameRateLabel);
		
		typicalLarvaLengthInPixelsField = new JFormattedTextField(NumberFormat.getInstance());
		typicalLarvaLengthInPixelsField.setValue(sep.typicalLarvaLengthInPixels); 
		typicalLarvaLengthInPixelsField.setColumns(4);
		typicalLarvaLengthInPixelsField.addPropertyChangeListener( new PropertyChangeListener() {		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				sep.typicalLarvaLengthInPixels = ((Number) typicalLarvaLengthInPixelsField.getValue()).floatValue();
			}
		});		
		typicalLarvaLengthInPixelsLabel = new JLabel("typical larva length (pixels)");
		typicalLarvaLengthInPixelsPanel = new JPanel(new BorderLayout());
		typicalLarvaLengthInPixelsPanel.add(typicalLarvaLengthInPixelsField, BorderLayout.WEST);
		typicalLarvaLengthInPixelsPanel.add(typicalLarvaLengthInPixelsLabel);
		
		globalThreshValueField = new JFormattedTextField(NumberFormat.getIntegerInstance());
		globalThreshValueField.setValue(sep.globalThreshValue); 
		globalThreshValueField.setColumns(4);
		globalThreshValueField.addPropertyChangeListener( new PropertyChangeListener() {		
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				sep.globalThreshValue = ((Number) globalThreshValueField.getValue()).intValue();
			}
		});		
		globalThreshValueLabel = new JLabel("Global Threshold");
		globalThreshValuePanel = new JPanel(new BorderLayout());
		globalThreshValuePanel.add(globalThreshValueField, BorderLayout.WEST);
		globalThreshValuePanel.add(globalThreshValueLabel);
		
		moreParamsButton = new JButton("More Settings");
		//saveToExButton.setSize(100, 40);
		moreParamsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				newParameterFrame();
			}
		});
		
		add(frameRatePanel);
		add(typicalLarvaLengthInPixelsPanel);
		add(globalThreshValuePanel);
		add(moreParamsButton);
	}
	public void updateFields() {
		globalThreshValueField.setValue(sep.globalThreshValue); 
		typicalLarvaLengthInPixelsField.setValue(sep.typicalLarvaLengthInPixels); 
		frameRateField.setValue(sep.frameRate);
	}
	private void newParameterFrame() {
		sep.parameterFrame(this);
	}
}
