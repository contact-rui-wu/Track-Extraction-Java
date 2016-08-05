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
	
	
	public void run(String arg) {
				
		
		ExtractorFrame ef = new ExtractorFrame(true);
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







