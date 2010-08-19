/*
 * Copyright (C) 2010 University of Washington
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.opendatakit.briefcase;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

/**
 * Applet to fetch data from Aggregate and store it in csv and binary
 * files on the local machine.  This is an applet so that the user can
 * log into the Aggregate instance by manually browsing to the Aggregate 
 * instance.  The applet can then use the login credentials of the user
 * for the fetch without ever having to negotiate the login sequence
 * itself.
 * 
 * @author mitchellsundt@gmail.com
 *
 */
public class BriefcaseApplet extends JApplet implements ActionListener, CsvDownload.ActionListener {

	/** Briefcase servlet that embeds the applet in a web page ends with /Briefcase */
	private static final String BRIEFCASE_URL_ELEMENT = "Briefcase";

	/** serialization */
	private static final long serialVersionUID = 8523973495636927870L;

	/** logger for this applet */
	private static final Logger log = Logger.getLogger(BriefcaseApplet.class.getName());
	
	/** does all the actual work... */
	private CsvDownload worker = null;

	/** states reported in the status area of the UI */
	private enum ActivityState {
		IDLE, WORKING, FETCHING, DONE
	};

	/**************************************************************
	 * Data values used to report the activities of 
	 * the worker in the background thread.
	 */
	/** most recent URL being fetched */
	private volatile String currentUrl = "";
	/** retry count */
	private volatile int tries = 1;
	/** count of number of fetches of data for this URL (cursor spans) */
	private volatile int count = 0;
	/** if there was an exception thrown by the worker this is it */
	private volatile Exception eFetchFailure = null;
	/** fetchStatus is true on exit if fetch was successful */
	private volatile boolean fetchStatus = false;
	/** track what the status of the action is */
	private volatile ActivityState activityState = ActivityState.IDLE;
	
	/************************************************************************
	 * Swing controls for the user interface
	 */
	/** status display control */
	private JTextArea statusCtrl;
	private JTextField dirPathCtrl;
	private JTextField odkIdCtrl;
	private JTextField lastCursorCtrl;
	private JTextField lastKeyCtrl;
	private ButtonGroup binaryButtonGroup;
	private JRadioButton fetchBinaryCtrl;
	private JRadioButton convertBinaryCtrl;
	private JRadioButton asIsBinaryCtrl;
	private JCheckBox recursiveCtrl;
	private JButton executeCtrl;
	
	private CookieHandler mgr;
	/**************************************************************
	 * The user's request values.
	 */
	/** initial request URL */
	private String fullUrl = "";
	/** how to handle binary data */
	private CsvDownload.BinaryDataTreatment fetchBinaryData;
	/** whether or not to fetch nested repeated groups */
	private boolean fetchRecursively;
	/** what cursor to resume processing with */
	String lastCursor;
	/** what KEY to resume processing after */
	private String skipBeforeKey;
	
	/** background thread runs via an executor */
	private ExecutorService executor = Executors.newSingleThreadExecutor();

	/************************************************************************
	 * Handler for the background thread that is doing the fetching
	 */
	class Handler implements Runnable {

		/**
		 * Does the meat of the processing.  Runs in a background
		 * thread managed by an executor.
		 * <p>
		 * Transitions activityState from WORKING to FETCHING.
		 * Invokes CsvDownload to do the work.  On return, 
		 * saves any exception that may have been thrown, 
		 * transitions activityState to DONE, and triggers
		 * a UI update of the status, an error pop-up if an 
		 * exception was thrown, and the re-enabling of the 
		 * UI.
		 */
		@Override
		public void run() {
			// transition from WORKING to FETCHING...
			activityState = ActivityState.FETCHING;
			try {
				worker.fetchCsvRecursively(fullUrl, fetchBinaryData, fetchRecursively, skipBeforeKey );
				fetchStatus = true;
			} catch (Exception e) {
				eFetchFailure = e;
			} finally {
				// clean up -- this will not throw any exceptions
				worker.closeAllFilesAndManifest(fetchStatus);
				// transition from FETCHING to DONE
				activityState = ActivityState.DONE;
				// update UI...
				statusCtrl.setText(getStatus());
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						if ( eFetchFailure != null ) {
							errorDialog("error while accessing server", eFetchFailure.getMessage());
						}
						toggleEnable(true);
					}
				});

			}
		}
	}

	/**
	 * Compute the status string.
	 * Synchronized to give a hint to the compiler that we are accessing 
	 * concurrently-updated data values (which are also marked volatile
	 * for an extra dose of compiler hinting).
	 * 
	 * @return string summarizing the current processing status.
	 */
	public synchronized String getStatus() {
		switch (activityState) {
		case IDLE:
			return "Idle - fill in a request and hit `Retrieve`";
		case WORKING:
			return "Working...";
		case FETCHING:
			return "Fetching (" + Integer.toString(count) + ":" + Integer.toString(tries) 
					+ ") - " + currentUrl;
		case DONE:
			if ( fetchStatus ) {
				return "Outcome = SUCCESS";
			} else {
				return "Outcome = FAILURE: " + eFetchFailure.getMessage();
			}
		}
		return "Bad State - please close all browser windows.";
	}

	/**
	 * The action listener callback for CsvDownload.
	 * Notifies the UI of each URL access done during the download process.
	 */
	@Override
	public synchronized void beforeFetchUrl(String currentUrl, int tries, int count) {
		this.currentUrl = currentUrl;
		this.tries = tries;
		this.count = count;
		// update UI...
		statusCtrl.setText(getStatus());
	}

	private void addUI(Component ui, GridBagConstraints c) {
		Container p = getContentPane();
		GridBagLayout gb = (GridBagLayout) p.getLayout();
		p.add(ui);
		gb.setConstraints(ui, c);
	}
	/**
	 * Called during the initialization of the applet frame.  
	 * Lays out the controls.  Sets up the (sole) action 
	 * listener for the "Retrieve" button.
	 */
	public void init() {
		mgr = CookieHandler.getDefault();
		if ( mgr == null ) {
			log.severe("No default CookieManager -- creating our own!");
			mgr = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
			CookieHandler.setDefault(mgr);
		} else {
			log.info("Found a default CookieManager -- using it!");
		}
		
		Container pane = getContentPane();
		Font f = pane.getFont();
		FontMetrics mf = pane.getFontMetrics(f);
		Font fStatus = f.deriveFont(Font.PLAIN, (float) (mf.getHeight() * 1.5));
		pane.setLayout(new GridBagLayout());

		GridBagConstraints c = new GridBagConstraints();
		c.gridx = GridBagConstraints.LINE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.insets = new Insets(0,4,0,4);
		c.weightx = 1.0;

		final int VERT_SPACE = mf.getHeight();
		JLabel label;
		label = new JLabel("<html><font size=\"+2\"><b>ODK Briefcase Applet </b></font><font size=\"3\">Version " + CsvDownload.APP_VERSION + "</font></html>", JLabel.LEFT);
		addUI(label,c);
		addUI(Box.createVerticalStrut(2*VERT_SPACE),c);
		statusCtrl = new JTextArea(4,0);
		statusCtrl.setText(getStatus());
		statusCtrl.setEditable(false);
		statusCtrl.setLineWrap(true);
		statusCtrl.setWrapStyleWord(false);
		statusCtrl.setFont(fStatus);
		statusCtrl.setForeground(Color.BLUE);
		statusCtrl.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
		GridBagConstraints cc = (GridBagConstraints) c.clone();
		cc.fill = GridBagConstraints.BOTH;
		addUI(statusCtrl,cc);
		addUI(Box.createVerticalStrut(2*VERT_SPACE),c);
		label = new JLabel("Directory path in which to store the data:");
		addUI(label,c);
		dirPathCtrl = new JTextField("C:\\dataspace\\");
		addUI(dirPathCtrl,c);
		recursiveCtrl = new JCheckBox("Download nested repeat groups");
		addUI(recursiveCtrl,c);
		label = new JLabel("Binary data handling:");
		addUI(label,c);
		binaryButtonGroup = new ButtonGroup();
		fetchBinaryCtrl = new JRadioButton("Download binary data and replace server URL with the local filename in the csv.");
		convertBinaryCtrl = new JRadioButton("Replace server URL with the local filename in the csv.");
		asIsBinaryCtrl = new JRadioButton("Keep server URL unchanged in the csv.", true);// default
		binaryButtonGroup.add(fetchBinaryCtrl);
		addUI(fetchBinaryCtrl,c);
		binaryButtonGroup.add(convertBinaryCtrl);
		addUI(convertBinaryCtrl,c);
		binaryButtonGroup.add(asIsBinaryCtrl);
		addUI(asIsBinaryCtrl,c);
		addUI(Box.createVerticalStrut(VERT_SPACE),c);
		label = new JLabel("odkId:");
		addUI(label,c);
		odkIdCtrl = new JTextField("HouseholdSurvey1/HouseholdSurvey");
		addUI(odkIdCtrl,c);
		addUI(Box.createVerticalStrut(VERT_SPACE),c);
		addUI(Box.createVerticalGlue(),c);
		addUI(new JSeparator(SwingConstants.HORIZONTAL),c);
		addUI(Box.createVerticalStrut(20),c);
		addUI(Box.createVerticalGlue(),c);
		label = new JLabel("Parameters for resumption of a failed download attempt:");
		addUI(label,c);
		Box b = new Box(BoxLayout.LINE_AXIS);
		b.setAlignmentX(LEFT_ALIGNMENT);
		b.add(Box.createHorizontalStrut(3*VERT_SPACE));
		Box sub = new Box(BoxLayout.PAGE_AXIS);
		sub.setAlignmentX(LEFT_ALIGNMENT);
		b.add(sub);
		sub.add(Box.createVerticalStrut(VERT_SPACE));
		label = new JLabel("LastCursor-x:");
		sub.add(label);
		lastCursorCtrl = new JTextField("");
		sub.add(lastCursorCtrl);
		label = new JLabel("LastKEY-x:");
		sub.add(label);
		lastKeyCtrl = new JTextField("");
		sub.add(lastKeyCtrl);
		addUI(b,c);
		addUI(Box.createVerticalStrut(VERT_SPACE),c);
		addUI(Box.createVerticalGlue(),c);
		addUI(new JSeparator(SwingConstants.HORIZONTAL),c);
		addUI(Box.createVerticalStrut(VERT_SPACE),c);
		executeCtrl = new JButton("Retrieve");
		executeCtrl.addActionListener(this);
		addUI(executeCtrl,c);
		addUI(Box.createVerticalStrut(VERT_SPACE),c);
	}

	/**
	 * Toggles the UI controls so that they can be disabled during 
	 * processing then re-enabled once the worker has completed 
	 * the download or failed.
	 * 
	 * @param isEnabled
	 */
	public void toggleEnable(boolean isEnabled) {
		dirPathCtrl.setEditable(isEnabled);
		odkIdCtrl.setEditable(isEnabled);
		fetchBinaryCtrl.setEnabled(isEnabled);
		convertBinaryCtrl.setEnabled(isEnabled);
		asIsBinaryCtrl.setEnabled(isEnabled);
		recursiveCtrl.setEnabled(isEnabled);
		lastCursorCtrl.setEditable(isEnabled);
		lastKeyCtrl.setEditable(isEnabled);
		executeCtrl.setEnabled(isEnabled);		
	}
	
	/**
	 * Action listener for the UI.
	 * <p>
	 * Handles the pressing of the "Retrieve" button.
	 * <p>
	 * This resets the processing status values, transitions
	 * the activity state to WORKING and then cleans up the user- 
	 * supplied server URL and extracts the values for the
	 * fetch request.  It then creates a new CsvWorker and fires
	 * off a background thread to do the actual work.
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == executeCtrl) {
			boolean workerFired = false;
			String candidateUrl = this.getDocumentBase().toString();
			try {
				/** reset the status outcome variables */
				fetchStatus = false;
				eFetchFailure = null;
				activityState = ActivityState.WORKING;
				toggleEnable(false);
				
				URL url = this.getDocumentBase();

				// The document URL always ends with a /briefcase/ element...
				// strip that off to get the base URL for the server...
				String path = url.getPath();
				log.info("path is: " + path);
				int idx = path.lastIndexOf(BRIEFCASE_URL_ELEMENT);
				if ( idx == -1 ) {
					// this will happen if Aggregate's document tree is rearranged somehow...
					log.severe("failed searching for: " + BRIEFCASE_URL_ELEMENT +
							 " in full url: " + url.toString());
					errorDialog("build error", "bad pass-through of server URL: " + url.toString());
					return;
				}
				// everything including slash before 'briefcase/'
				path = path.substring(0, idx);
				
				URL serverUrl = new URL(url.getProtocol(), url.getHost(), url.getPort(), path);
				candidateUrl = serverUrl.toString();
				log.info("base Url: " + candidateUrl);

				log.info("trying to figure out cookies");
				boolean found = false;
				try {
					Map<String,List<String>> rh = new HashMap<String,List<String>> ();
					Map<String,List<String>> cookieStrings = mgr.get(serverUrl.toURI(), rh);
					List<String> cookies = cookieStrings.get("Cookie");
					for ( String c : cookies ) {
						found = true; 
						log.info("found cookie: " + c );
					}
				} catch ( Exception eIgnore) {
				}

				if ( !found ) {
					log.severe("no authentication cookie!");
				}
				
				String destDir = dirPathCtrl.getText().trim();
				if ( destDir.length() == 0 ) {
					errorDialog("bad directory path", destDir);
					return;
				}
				String odkId = odkIdCtrl.getText().trim();
				if ( odkId.length() == 0 ) {
					errorDialog("bad odkId", odkId);
					return;
				}
				
				if ( fetchBinaryCtrl.isSelected() ) {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.DOWNLOAD_BINARY_DATA;
				} else if ( convertBinaryCtrl.isSelected() ) {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.REPLACE_WITH_LOCAL_FILENAME;
				} else {
					fetchBinaryData = CsvDownload.BinaryDataTreatment.RETAIN_BINARY_DATA_URL;
				}
				
				fetchRecursively = recursiveCtrl.isSelected(); 
				/**
				 * Resumption of data processing is supported through the 
				 * cursor and skipBeforeKey values.  The cursor should be 
				 * set to the LastCursor string from the failed Manifest
				 * and the skipBeforeKey should be set to the LastKey string.
				 */
				lastCursor = lastCursorCtrl.getText().trim();
				if ( lastCursor.length() == 0 ) lastCursor = null;
				skipBeforeKey = lastKeyCtrl.getText().trim();
				if ( skipBeforeKey.length() == 0 ) skipBeforeKey = null;

				worker = new CsvDownload(candidateUrl, destDir, this);
				
				Map<String, String> params = new HashMap<String, String>();
				params.put(CsvDownload.ODK_ID, odkId);
				params.put(CsvDownload.NUM_ENTRIES, Integer.toString(1));
				params.put(CsvDownload.CURSOR, lastCursor);

				fullUrl = worker.createCsvFragmentLinkWithProperties(params);
				
				// launch worker...
				workerFired = true;
				executor.execute(new Handler());
			} catch (MalformedURLException eIgnore) {
				errorDialog("bad server url", candidateUrl);
			} catch (Exception eFail) {
				errorDialog("bad settings", eFail.getClass().getName() + ":" + eFail.getMessage());
			} finally {
				toggleEnable(!workerFired);
				if (!workerFired) {
					activityState = ActivityState.DONE;
				}
			}
		}
	}

	/**
	 * Simplistic pop-up error dialog.
	 * 
	 * @param error
	 * @param value
	 */
	private void errorDialog(String error, String value) {
		String msgError = error.substring(0,1).toUpperCase() + error.substring(1) + ". " + value;
		JOptionPane.showMessageDialog(this, msgError, error,
										JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Called when the applet container (browser) is being shut down.
	 */
	public void stop() {
		executor.shutdownNow();
		super.stop();
	}
}
