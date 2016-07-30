package TrackExtractionJava;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;

public class FileWriterCommunicator extends Communicator {

	private PrintWriter w;
	private FileWriterCommunicator() {
		// TODO Auto-generated constructor stub
	}
	FileWriterCommunicator(Writer w) {
		this.w = new PrintWriter(w);
	}
	FileWriterCommunicator(String filename) throws IOException {
		this(new FileWriter(filename));
	}
	
	public void message(String message, VerbLevel messVerb){
		if (messVerb==null){
			return;
		} else if (messVerb.compareTo(verbosity) <=0 ){
			w.println(messVerb.toString()+": "+message);
			w.flush();
		}
	}
	
	public void saveOutput(String dstDir, String fileName){
		//does nothing; there is no text to save here
	}
}
