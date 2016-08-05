package TrackExtractionJava;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.Field;
import java.text.NumberFormat;

import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class NumberChangerPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2173752323845094448L;
	private Object obj;
	private String fieldName;
	private String label;
	private Field f;
	private JFormattedTextField tf;
	
	public static JPanel numericalFieldsPanel(Object obj) {
		Class<?> c = obj.getClass();
		Field[] ff = c.getDeclaredFields();
		JPanel jp = new JPanel();
		jp.setLayout(new GridLayout(0, 4));
		for (Field f : ff) {
			if (f.getType().equals(Integer.TYPE) || f.getType().equals(Long.TYPE) 
					|| f.getType().equals(Float.TYPE) || f.getType().equals(Double.TYPE)) {
				jp.add(new NumberChangerPanel(obj, f.getName(),  null));
			}
		}

		return jp;
	}
	
	public NumberChangerPanel(Object obj, String fieldName, String label) {
		this.obj = obj;
		this.label = label;
		this.fieldName = fieldName;
		init();
	}

	public NumberChangerPanel(Object obj, String fieldName, String label, LayoutManager arg0) {
		super(arg0);
		this.obj = obj;
		this.label = label;
		this.fieldName = fieldName;
		init();
	}

	public NumberChangerPanel(Object obj, String fieldName, String label, boolean arg0) {
		super(arg0);
		this.obj = obj;
		this.label = label;
		this.fieldName = fieldName;
		init();
	}

	public NumberChangerPanel(Object obj, String fieldName, String label, LayoutManager arg0, boolean arg1) {
		super(arg0, arg1);
		this.obj = obj;
		this.label = label;
		this.fieldName = fieldName;
		init();
	}
	
	private void setTextField() throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().equals(Integer.TYPE) || f.getType().equals(Long.TYPE)) {
			if (tf == null) {
				tf = new JFormattedTextField(NumberFormat.getIntegerInstance());
				tf.setColumns(10);
			}
			tf.setValue(f.getInt(obj));
			return;
		}
		if (f.getType().equals(Double.TYPE) || f.getType().equals(Float.TYPE)) {
			if (tf == null) {
				tf = new JFormattedTextField(NumberFormat.getNumberInstance());
				tf.setColumns(10);
			}
			tf.setValue(f.getDouble(obj));
			return;
		}
	}
	
	private void getFieldValue() throws IllegalArgumentException, IllegalAccessException {
		if (f.getType().equals(Integer.TYPE) || f.getType().equals(Long.TYPE)) {
			f.setInt(obj,((Number)tf.getValue()).intValue());
			return;
		}
		if (f.getType().equals(Double.TYPE) || f.getType().equals(Float.TYPE)) {
			f.setFloat(obj,((Number)tf.getValue()).floatValue());
			return;
		}
	}
	
	private void init() {
		if (null == label || "" == label) {
			label = fieldName;
		}
		Class<?> c = obj.getClass();
		try {
			f = c.getDeclaredField(fieldName);
		} catch (Exception e) {
			return;
		} 
		if (null == f) {
			return;
		}
		try {
			setTextField();
		}catch (Exception e) {
			return;
		} 
		if (null == tf) {
			return;
		}
		
		tf.addPropertyChangeListener( new PropertyChangeListener() {
			
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				try {
					getFieldValue();
				} catch (Exception e) {
					return;
				} 
			}
		});
		JLabel lab = new JLabel(label);
		//JPanel pan = new JPanel(new BorderLayout());
		add(tf, BorderLayout.WEST);
		add(lab);
	}

}
