package TrackExtractionJava;


public class MaggotDisplayParameters {

	static boolean DEFAULTclusters = false;
	static boolean DEFAULTmid = true;
	static boolean DEFAULTinitialBB = false; 
	static boolean DEFAULTcontour = true;
	static boolean DEFAULTht = true;
	static boolean DEFAULTforces = false;
	static boolean DEFAULTbackbone = true;
	//Image, SpineLength, SpineSmooth, TimeLength, TimeSmooth
	static boolean[] DEFAULTshowForce = {true, true, true, true, true};
	
	boolean clusters;
	boolean mid;
	boolean initialBB; 
	boolean contour;
	boolean ht;
	boolean forces;
	boolean backbone;
	boolean[] showForce;
	
	
	
	public MaggotDisplayParameters(){
		setToDefault();
	}
	
	
	public void setToDefault(){
		clusters = DEFAULTclusters;
		mid = DEFAULTmid;
		initialBB = DEFAULTinitialBB;
		contour = DEFAULTcontour;
		ht = DEFAULTht;
		forces = DEFAULTforces;
		backbone = DEFAULTbackbone;
		showForce = DEFAULTshowForce;
	}
	
	public boolean getParam(String paramName){
		if (paramName.equalsIgnoreCase("clusters")) {
			return clusters;
		} else if (paramName.equalsIgnoreCase("mid")){
			return mid;
		} else if (paramName.equalsIgnoreCase("initialbb")){
			return initialBB;
		}else if (paramName.equalsIgnoreCase("contour")){
			return contour;
		}else if (paramName.equalsIgnoreCase("ht")){
			return ht;
		}else if (paramName.equalsIgnoreCase("forces")){
			return forces;
		}else if (paramName.equalsIgnoreCase("backbone")){
			return backbone;
		}else {
			return false;
		}
	}
	
	public void setParam(String paramName, boolean value){
		if (paramName.equalsIgnoreCase("clusters")) {
			clusters = value;
		} else if (paramName.equalsIgnoreCase("mid")){
			mid = value;
		} else if (paramName.equalsIgnoreCase("initialbb")){
			initialBB = value;
		}else if (paramName.equalsIgnoreCase("contour")){
			contour = value;
		}else if (paramName.equalsIgnoreCase("ht")){
			ht = value;
		}else if (paramName.equalsIgnoreCase("forces")){
			forces = value;
		}else if (paramName.equalsIgnoreCase("backbone")){
			backbone = value;
		}
	}
	
}