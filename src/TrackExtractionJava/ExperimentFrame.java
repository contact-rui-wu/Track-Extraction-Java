package TrackExtractionJava;

import ij.IJ;
import ij.io.SaveDialog;
import ij.text.TextWindow;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Vector;












import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
//import javax.xml.crypto.Data;



public class ExperimentFrame extends JFrame{

	/**
	 * Serialization ID
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * The experiment containing the tracks
	 */
	private Experiment ex;
	
	/**
	 * Display parameters for showing track movies
	 */
	MaggotDisplayParameters mdp;
	
	/**
	 * A list of tracks
	 */
	JList trackList;
	
	/**
	 * A TrackPanel to display info and provide track functions
	 */
	TrackPanel trackPanel;
	
	/**
	 * A panel to show a list of tracks and provide a save button
	 */
	JPanel exPanel;
	
	/**
	 * A panel to choose display options for the track.playMovie
	 */
	JPanel playPanel;
	
	SimpleExtractionParameters sep;
	
	Vector<TrackMovieVirtualStack> movies;
	/**
	 * 
	 */
	public ExperimentFrame(TrackBuilder tb){
		this(tb.toExperiment());
	}
	
	public ExperimentFrame(Experiment ex){
		this.ex = ex;
		init();
	}
	void init () {
		movies = new Vector<TrackMovieVirtualStack>();
		sep = new SimpleExtractionParameters();
	}
	
	protected void addMovie (TrackMovieVirtualStack vs) {
		movies.addElement(vs);
		cleanClosedMovies();
	}
	
	protected void updateMDP () {
		for (TrackMovieVirtualStack vs : movies) {
			vs.setMaggotDisplayParameters(mdp); //triggers redraw
		}
	}
	
	private void cleanClosedMovies () {
		Vector <TrackMovieVirtualStack> closedMovies = new Vector<TrackMovieVirtualStack>();
		for (TrackMovieVirtualStack vs : movies) {
			if (vs.windowClosed()) {closedMovies.add(vs);}
		}
		movies.removeAll(closedMovies);
	}
	
	public void run(String args){

		buildFrame();
		
		showFrame();
		
	}
	
	
	protected void buildFrame(){
		
		mdp = new MaggotDisplayParameters();
		
		//Build the trackPanel
		trackPanel = new TrackPanel(mdp, this);
		trackPanel.setSize(trackPanel.getWidth(), 500);
		
		//Build the trackList 
		buildExPanel();
		
		//Build the display option panel
		buildOpPanel();
		
		
		//Add components
		add(trackPanel, BorderLayout.CENTER);
		add(exPanel, BorderLayout.WEST);
		add(playPanel, BorderLayout.EAST);
//		add(new JScrollPane(trackList), BorderLayout.WEST);
		//setSize(1024, 768);
		pack();
		
		addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		        ex = null;
		        mdp=null;
		        trackList = null;//.removeAll();
		        trackPanel = null;//.removeAll();
		        exPanel = null;//.removeAll();
		        playPanel = null;//.removeAll();
		        
		    }
		});
	}
	
	protected void showFrame(){
		//setSize(750, 600);
		setTitle("("+ex.getNumTracks()+" tracks) Experiment "+ex.getFileName());
//		setTitle("("+Experiment.getNumTracks(ex.getFileName())+" tracks) Experiment "+ex.getFileName());
		setVisible(true);
	}
	
	
	protected void buildExPanel(){
		
		exPanel = new JPanel();
		exPanel.setLayout(new BorderLayout(5, 5));
		
		//Build the track list 
		trackList = new JList(trackNames());
		trackList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		trackList.addListSelectionListener(new ListSelectionListener() {
			
			@Override
			public void valueChanged(ListSelectionEvent e) {
				
				trackPanel.updateTrack(getCurrentTrack());//;ex.tracks.get(trackList.getSelectedIndex()));
				//TODO change to get the track id 
				
			}
		});
		trackList.addMouseListener(new MouseAdapter(){
		    @Override
		    public void mouseClicked(MouseEvent e){
		        if(e.getClickCount()==2){
		            trackPanel.updateTrack(getCurrentTrack());
		            trackPanel.playCurrentTrack();
		        }
		    }
		});
		JScrollPane trackListPanel = new JScrollPane(trackList);
		
		//Build the button
		JButton saveButton = new JButton("Save Experiment");
		saveButton.setSize(150, 40);
		saveButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				//Get the file name
				DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
				Date date = new Date();
				SaveDialog sd = new SaveDialog("Save Experiment", "ex"+df.format(date), ".ser");
				
				
				if (new File(sd.getDirectory(), sd.getFileName()).exists()) {
					new TextWindow("Message", "That file name already exists", 500, 500);
				} else {
					//TODO enforce that the file doesn't already exist 
					//Save the file 
					try{
						IJ.showStatus("Saving file...");
						ex.serialize(sd.getDirectory(), sd.getFileName());
						IJ.showStatus("File saved!");
					} catch (Exception exception){
						new TextWindow("Error", "could not save experiment at the given directory\n"+exception.getMessage(), 500, 500);
					}
				}
			}
		});
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(saveButton);
		
		
		//Add list and button to panel
		exPanel.add(trackListPanel, BorderLayout.CENTER);
//		exPanel.add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	protected void buildOpPanel(){
		playPanel = new JPanel();
		playPanel.setLayout(new BoxLayout(playPanel, BoxLayout.Y_AXIS));
		
		JLabel label = new JLabel("Play Options");
		label.setFont(new Font(label.getFont().getName(), Font.BOLD, label.getFont().getSize()*2));
		label.setAlignmentX(CENTER_ALIGNMENT);
		
		DisplayOpPanel dop = new DisplayOpPanel(mdp, this);
		JPanel ops = new JPanel();
		ops.setAlignmentX(CENTER_ALIGNMENT);
		ops.add(dop);
		
		playPanel.add(label);
		playPanel.add(ops);
		
	}
	
	protected Vector<String> trackNames(){
		
		Vector<String> names = new Vector<String>();
		
//		ListIterator<Track> trIt = ex.tracks.listIterator();
//		while(trIt.hasNext()){
		for (int i=0; i<ex.getNumTracks(); i++){
					
			Track t = ex.getTrackFromInd(i);//trIt.next();
			String name = "Track "+t.getTrackID()+" ("+t.getNumPoints()+")";
			if (t instanceof CollisionTrack) name+="*";
			names.add(name);
		}
		
		return names;
		
	}
	
	protected int getCurrentTrackID(){
		String name = (String) trackList.getSelectedValue();
		int beforeInd = name.indexOf(" ");
		int afterInd = name.indexOf(" ", beforeInd+1);
		return Integer.valueOf(name.substring(beforeInd+1, afterInd-1));
	}
	
	protected Track getCurrentTrack(){
//		return ex.getTrack(getCurrentTrackID());
		return ex.getTrackFromInd(trackList.getSelectedIndex());
	}
	
}

class TrackPanel extends JPanel {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ExperimentFrame ef;
	
	Track track;
	JTextArea trackDescription;
	JScrollPane descriptionPanel;
	
	MaggotDisplayParameters mdp;
	
	JButton playButton;
	JButton playDdtButton;
	JButton saveToExButton;
	JButton saveToCSVButton;
	JButton fitButton;
	JButton paramsButton;
	JPanel buttonPanel;
	
	public TrackPanel(MaggotDisplayParameters mdp, ExperimentFrame ef){
		this.mdp = mdp;
		this.ef = ef;
		buildTrackPanel();
	}
	
	
	public void buildTrackPanel(){
		
		setLayout(new BorderLayout(5, 5));
		setFont(Font.getFont("Courier New"));
		
		
		//Build & add the description panel
		trackDescription = new JTextArea(Track.emptyDescription());
		trackDescription.setLineWrap(false);
		trackDescription.setFont(new Font("Courier New", Font.PLAIN, trackDescription.getFont().getSize()));
		descriptionPanel = new JScrollPane(trackDescription);
		
		
		//Build and add the play button 
		playButton = new JButton("Play Track Movie");
		playButton.setSize(100, 40);
		playButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playCurrentTrack();
			}
		});
		
		playDdtButton = new JButton("Play ddt Movie");
		playDdtButton.setSize(100, 40);
		playDdtButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				playCurrentDdtTrack();
			}
		});
		
		fitButton = new JButton("Fit Track");
		fitButton.setSize(100, 40);
		fitButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				fitCurrentTrack();
			}
		});
		
		paramsButton = new JButton("Set Fit Params");
		paramsButton.setSize(100, 40);
		paramsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ef.sep.parameterFrame();
			}
		});
		
		saveToExButton = new JButton("Save Track to Experiment");
		saveToExButton.setSize(100, 40);
		saveToExButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveTrackToExperiment();
			}
		});
		
		
		saveToCSVButton = new JButton("Save Track to CSV");
		saveToCSVButton.setSize(100, 40);
		saveToCSVButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//set prefs
				saveTrackToCSV(new CSVPrefs());
			}
		});
		
		
		
		//Build and add the play button 
		buttonPanel = new JPanel();
		buttonPanel.add(playButton);
		buttonPanel.add(playDdtButton);
		buttonPanel.add(saveToCSVButton);
		buttonPanel.add(saveToExButton);
		buttonPanel.add(fitButton);
		buttonPanel.add(paramsButton);
//		buttonPanel.setSize(700, 60);
		
		add(descriptionPanel, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		
	}
	
	
	public void updateTrack(Track track){
		
		//Update the track
		this.track = track;
		//Update the message and scroll to the top
		trackDescription.setText(track.description());
		trackDescription.setCaretPosition(0);
		//TODO Set ePlotPaneltrack
	}
	
	public void playCurrentTrack(){
		try{
			if (track!=null){
				TrackMovieVirtualStack vs = track.getVirtualMovieStack(mdp);
				vs.getImagePlus().show();
				ef.addMovie(vs);
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			new TextWindow("PlayMovie Error", "Could not play track "+track.getTrackID()+" movie\n"+sw.toString()+"\n", 500, 500);
		}
	}
	
	public void playCurrentDdtTrack(){
		try{
			if (track!=null){
				TrackDdtVirtualStack vs = track.getVirtualDdtStack();
				vs.getImagePlus().show();
				ef.addMovie(vs);
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			new TextWindow("PlayDdtMovie Error", "Could not play track "+track.getTrackID()+" movie\n"+sw.toString()+"\n", 500, 500);
		}
	}
	
	public void fitCurrentTrack(){
		try{
			if (track!=null){
				new Thread () {
					public void run() {
						BackboneFitter bbf = new BackboneFitter(track, ef.sep.getFittingParameters()); 
						bbf.recordHistory();
						IJ.showStatus("fitting track");
						bbf.fitTrackNewScheme( ef.sep.getFittingParameters());//TODO adjust fitting parameters
						TrackMovieVirtualStack vs = bbf.getTrack().getVirtualMovieStack(mdp, true);
						vs.setForces(bbf.Forces);
						vs.getImagePlus().show();
						ef.addMovie(vs);
						IJ.showStatus("done fitting track");
					}
				}.start();
				
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			new TextWindow("PlayMovie Error", "Could not play track "+track.getTrackID()+" movie\n"+sw.toString()+"\n", 500, 500);
		}
	}
	
	public void saveTrackToExperiment(){
		String dir = "[unassigned]";
		
		try{
			if (track!=null && track.points!=null && track.points.size()>0){
				
				Vector<Track> tvec = new Vector<Track>();
				tvec.add(track);
				Experiment ex = new Experiment(track.exp, tvec);
				
				String name = "track"+track.getTrackID()+"Ex";
				String ext = (track.points.firstElement().getPointType()<BackboneTrackPoint.pointType)? 
							".prejav" : ".jav";
				
				//open a directory selector
				SaveDialog sd = new SaveDialog("Choose a directory...", name, ext);
				dir = sd.getDirectory();
				
				if (dir!=null && dir !=""){
					File f = new File(dir+name+ext);
					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(f)));
					ex.toDisk(dos);
					dos.close();
				}
				
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			new TextWindow("SaveTrackToEx Error", "Could not save track "+track.getTrackID()+" to Experiment in "+dir+"\n"+sw.toString()+"\n", 500, 500);
		}
	}
	
	public void saveTrackToCSV(CSVPrefs prefs){
		String dir = "[unassigned]";
		
		try{
			if (track!=null && track.points!=null && track.points.size()>0){
				
				Vector<Track> tvec = new Vector<Track>();
				tvec.add(track);
				Experiment ex = new Experiment(track.exp, tvec);
				
				String name = "track"+track.getTrackID();
				String ext = ".csv";
				
				//open a directory selector
				SaveDialog sd = new SaveDialog("Choose a directory...", name, ext);
				dir = sd.getDirectory();
				name = sd.getFileName();
				
				if (dir!=null && dir !=""){
					File f = new File(dir+name+ext);
					Experiment.toCSV(ex, f.getAbsolutePath(), prefs);//ex.totoDisk(dos, null);
				}
				
			}
		} catch(Exception e){
			StringWriter sw = new StringWriter();
			PrintWriter prw = new PrintWriter(sw);
			e.printStackTrace(prw);
			new TextWindow("SaveTrackToCSV Error", "Could not save track "+track.getTrackID()+" to CSV in "+dir+"\n"+sw.toString()+"\n", 500, 500);
		}
	}
	
}


class DisplayOpPanel extends JPanel{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private MaggotDisplayParameters mdp;
	HashMap<JCheckBox, String> paramNames;
	private JCheckBox clusterBox;
	private JCheckBox midBox;
//	private JCheckBox initialBBBox;
	private JCheckBox contourBox;
	private JCheckBox htBox;
	private JCheckBox forcesBox;
	private JCheckBox backboneBox;
	
	private ExperimentFrame ef;

	Vector <JCheckBox> indivForces;
	/**
	 * Constructs a Display option panel with the given display parameters
	 * @param mdp
	 */
	public DisplayOpPanel(MaggotDisplayParameters mdp, ExperimentFrame ef){
		this.mdp = mdp;//new MaggotDisplayParameters();
		mdp.initialBB = false;
		this.ef = ef;
		buildDisplayOpPanel();
	}
	
	private void buildDisplayOpPanel(){
		setLayout(new GridLayout(7, 1));
		
		buildCheckBoxes();
		add(clusterBox);
//		add(initialBBBox);
		add(htBox);
		add(contourBox);
		add(midBox);
		add(backboneBox);
		add(forcesBox);
		for (JCheckBox b : indivForces) {
			add(b);
		}
	}
	
	private void buildCheckBoxes(){
		//paramNames.put(clusterBox, "clusters");
	//	buildCheckBox(clusterBox, "Clusters");
		clusterBox = new JCheckBox("Clusters");
		clusterBox.setSelected(mdp.getParam("clusters"));
		clusterBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("clusters", clusterBox.isSelected());
				ef.updateMDP();
			}
		});
		
//		paramNames.put(midBox, "mid");
//		buildCheckBox(midBox, "Midline");
		midBox = new JCheckBox("Midline");
		midBox.setSelected(mdp.getParam("mid"));
		midBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("mid", midBox.isSelected());
				ef.updateMDP();
			}
		});
		
//		paramNames.put(initialBBBox, "initialBB");
//		buildCheckBox(initialBBBox, "Initial Backbone Guess");
	//	initialBBBox = new JCheckBox("Initial Backbone Guess");
	//	initialBBBox.setSelected(mdp.getParam("initialBB"));
	//	initialBBBox.addActionListener(new ActionListener() {
	//		@Override
	//		public void actionPerformed(ActionEvent e) {
	//			mdp.setParam("initialBB", initialBBBox.isSelected());
	//			ef.updateMDP();
	//		}
	//	});
		
//		paramNames.put(contourBox, "contour");
//		buildCheckBox(clusterBox, "Contour");clusterBox = new JCheckBox("Clusters");
		contourBox = new JCheckBox("Contour");
		contourBox.setSelected(mdp.getParam("contour"));
		contourBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("contour", contourBox.isSelected());
				ef.updateMDP();
			}
		});

		
//		paramNames.put(htBox, "ht");
//		buildCheckBox(htBox, "Head & Tail");
		htBox = new JCheckBox("Head & Tail");
		htBox.setSelected(mdp.getParam("ht"));
		htBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("ht", htBox.isSelected());
				ef.updateMDP();
			}
		});

		
//		paramNames.put(forcesBox, "forces");
//		buildCheckBox(forcesBox, "Forces");
		forcesBox = new JCheckBox("Forces");
		forcesBox.setSelected(mdp.getParam("forces"));
		forcesBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("forces", forcesBox.isSelected());
				ef.updateMDP();
			}
		});

		
//		paramNames.put(backboneBox, "backbone");
//		buildCheckBox(backboneBox, "Backbone");
		backboneBox = new JCheckBox("Backbone");
		backboneBox.setSelected(mdp.getParam("backbone"));
		backboneBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.setParam("backbone", backboneBox.isSelected());
				ef.updateMDP();
			}
		});

		indivForces = new Vector<JCheckBox>();
		for (int i = 0; i < mdp.showForce.length; ++i) {
			indivForces.add(new FCheckBox(i, mdp, ef));
		}
	}
	
	
//	private void buildCheckBox(JCheckBox box, String title){
//		
//		box = new JCheckBox(title);
//		box.setSelected(mdp.getParam(paramNames.get(box)));
//		box.addActionListener(new ActionListener() {
//			
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				mdp.setParam(paramNames.get(box), box.isSelected());
//			}
//		});
//		
//		
//	}
	
	
}

class FCheckBox extends JCheckBox {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3914619681299717774L;
	private MaggotDisplayParameters mdp;
	private int whichForce;
	ExperimentFrame ef;
	public FCheckBox(int whichForce, MaggotDisplayParameters mdp, ExperimentFrame ef) {
		super (new FittingParameters().getForceNames().get(whichForce));
		this.whichForce = whichForce;
		this.mdp = mdp;
		this.ef = ef;
		init();
	}
	void init() {
		setSelected(mdp.showForce[whichForce]);
		addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				mdp.showForce[whichForce] = isSelected();
				ef.updateMDP();
			}
		});
	}
}


