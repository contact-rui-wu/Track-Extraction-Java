package TrackExtractionJava;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;

import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.lang.reflect.*;

public class SimpleExtractionParameters {
	//gives access to the most commonly used extraction and fitting parameters
	
	private int endFrame = Integer.MAX_VALUE;
	private int startFrame = 1;
	private int globalThreshValue = 30;
	private float frameRate = 20;
	private float typicalLarvaLengthInPixels = 20;
	private float timeLengthScaleFactor = 0.5f;
	private float timeSmoothScaleFactor = 0.25f;
	private float imageWeight = 1;
	private float spineLengthWeight = 0.5f;
	private float spineSmoothWeight = 0.25f;
	private float spineExpansionWeight = 1;
	private float minimumDuration = 10;
	private boolean gaussCluster = false;
	
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
		return fp;
	}
	
	public JFrame parameterFrame() {
		JFrame pf = new JFrame();
		pf.add(numberPanels());
		pf.pack();
		pf.setVisible(true);
		return pf;
	}
	
	private JPanel numberPanels() {
		Class<?> c = this.getClass();
		Field[] ff = c.getDeclaredFields();
		JPanel jp = new JPanel();
		for (Field f : ff) {
			if (f.getType().equals(Integer.TYPE)) {
				jp.add(integerEntry(f.getName(),null));
			}
			if (f.getType().equals(Float.TYPE)) {
				jp.add(floatEntry(f.getName(),null));
			}
		}
		final JCheckBox gb = new  JCheckBox("gaussCluster", gaussCluster);
		gb.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				gaussCluster = gb.isSelected();
							}
		});
		jp.add(gb);
		return jp;
	}

	private JPanel integerEntry (String fieldName, String label) {
		if (null == label || "" == label) {
			label = fieldName;
		}
		Class<?> c = this.getClass();
		final Field f;
		try {
			f = c.getDeclaredField(fieldName);
		} catch (Exception e) {
			return null;
		} 
		if (null == f) {
			return null;
		}
		int val;
		try {
			val = f.getInt(this);
		} catch (Exception e) {
			return null;
		} 
		final JFormattedTextField tf = new JFormattedTextField(NumberFormat.getIntegerInstance());
		tf.setValue(val); 
		tf.addPropertyChangeListener( new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				int v = ((Number)tf.getValue()).intValue();
				try {
					f.setInt(this, v);
				} catch (Exception e) {
					return;
				} 
			}
		});
		JLabel lab = new JLabel(label);
		JPanel pan = new JPanel(new BorderLayout());
		pan.add(tf, BorderLayout.WEST);
		pan.add(lab);
		return pan;
	}
	private JPanel floatEntry (String fieldName, String label) {
		if (null == label || "" == label) {
			label = fieldName;
		}
		Class<?> c = this.getClass();
		final Field f;
		try {
			f = c.getDeclaredField(fieldName);
		} catch (Exception e) {
			return null;
		} 
		if (null == f) {
			return null;
		}
		Float val;
		try {
			val = f.getFloat(this);
		} catch (Exception e) {
			return null;
		} 
		final JFormattedTextField tf = new JFormattedTextField(NumberFormat.getNumberInstance());
		tf.setValue(val); 
		tf.addPropertyChangeListener( new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				float v = ((Number)tf.getValue()).floatValue();
				try {
					f.setFloat(this, v);
				} catch (Exception e) {
					return;
				} 
			}
		});
		JLabel lab = new JLabel(label);
		JPanel pan = new JPanel(new BorderLayout());
		pan.add(tf, BorderLayout.WEST);
		pan.add(lab);
		return pan;
	}	
		
}
