package TrackExtractionJava;

import java.io.IOException;
import java.io.Writer;

class Timer {
	private static final TicTocTable tt = new TicTocTable();
	
	public static void enable(){
		tt.enable();
	}
	public static void disable() {
		tt.disable();
	}
	
	public static void tic(String name) {
		tt.tic(name);
	}
	public static void tic(String name, boolean notick) {
	    tt.tic(name, notick);
	}

	public static double toc(String name) {
		   return tt.toc(name);
		}
	
	public static double toc(String name, boolean notock) {
	   return tt.toc(name, notock);
	}

	public static double getElapsedTime (String name) {
	    return tt.getElapsedTime(name);
	 }
	
	 public static void generateReport(Writer w) throws IOException {
	    tt.generateReport(w);
	}
	public static String generateReport () {
		return tt.generateReport();
	}
	public static void generateReport(Writer w, boolean byTime) throws IOException {
	    tt.generateReport(w, byTime);
	}
	public static String generateReport (boolean byTime) {
		return tt.generateReport(byTime);
	}
	
	public static TicToc getTicToc (String name) {
		return tt.getTicToc(name);
	}
	
	public static void removeAllTimers() {
		tt.removeAllTimers();
	}
	
	public static void remove(String name) {
		tt.remove(name);
	}
	
	public static void reset(String name) {
		tt.reset(name);
	}
	
	public static void resetAllTimers() {
		tt.resetAllTimers();
	}
	
}
