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

import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

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

	/** serialization */
	private static final long serialVersionUID = 8523973495636927870L;

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
	private JLabel statusCtrl;
	private JTextField dirPathCtrl;
	private JTextField serverUrlCtrl;
	private JTextField odkIdCtrl;
	private JTextField lastCursorCtrl;
	private JTextField lastKeyCtrl;
	private ButtonGroup binaryButtonGroup;
	private JRadioButton fetchBinaryCtrl;
	private JRadioButton convertBinaryCtrl;
	private JRadioButton asIsBinaryCtrl;
	private JCheckBox recursiveCtrl;
	private JButton executeCtrl;
	
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
				// force update of UI...
				EventQueue.invokeLater(new Runnable() {
					@Override
					public void run() {
						statusCtrl.setText(getStatus());
						if ( eFetchFailure != null ) {
							errorDialog("fetch failed", eFetchFailure.getMessage());
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
		// force update of UI...
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				statusCtrl.setText(getStatus());
			}
		});
	}

	/**
	 * Called during the initialization of the applet frame.  
	 * Lays out the controls.  Sets up the (sole) action 
	 * listener for the "Retrieve" button.
	 */
	public void init() {
		Container pane = getContentPane();
		Font f = pane.getFont();
		FontMetrics mf = pane.getFontMetrics(f);
		Font fLarge = f.deriveFont(Font.BOLD, (float) (mf.getHeight() * 2));

		BoxLayout vertical = new BoxLayout(pane, BoxLayout.PAGE_AXIS);
		pane.setLayout(vertical);
		JLabel label;
		label = new JLabel("ODK Briefcase Applet", JLabel.CENTER);
		label.setFont(fLarge);
		pane.add(label);
		label = new JLabel(" ");
		label.setFont(fLarge);
		pane.add(label);
		statusCtrl = new JLabel(getStatus());
		pane.add(statusCtrl);
		label = new JLabel(" ");
		label.setFont(fLarge);
		pane.add(label);
		label = new JLabel("Directory path in which to store the data:");
		pane.add(label);
		dirPathCtrl = new JTextField("C:\\dataspace\\");
		pane.add(dirPathCtrl);
		recursiveCtrl = new JCheckBox("Download nested repeat groups");
		pane.add(recursiveCtrl);
		label = new JLabel("Binary data handling:");
		pane.add(label);
		binaryButtonGroup = new ButtonGroup();
		fetchBinaryCtrl = new JRadioButton("Download binary data and replace server URL with the local filename in the csv.");
		convertBinaryCtrl = new JRadioButton("Replace server URL with the local filename in the csv.");
		asIsBinaryCtrl = new JRadioButton("Keep server URL unchanged in the csv.", true);// default
		binaryButtonGroup.add(fetchBinaryCtrl);
		pane.add(fetchBinaryCtrl);
		binaryButtonGroup.add(convertBinaryCtrl);
		pane.add(convertBinaryCtrl);
		binaryButtonGroup.add(asIsBinaryCtrl);
		pane.add(asIsBinaryCtrl);
		label = new JLabel(" ");
		pane.add(label);
		label = new JLabel(
				"Aggregate URL (paste it in from a browser address line):");
		pane.add(label);
		serverUrlCtrl = new JTextField("http://localhost:8888/forms");
		pane.add(serverUrlCtrl);
		label = new JLabel(" ");
		pane.add(label);
		label = new JLabel("odkId:");
		pane.add(label);
		odkIdCtrl = new JTextField("HouseholdSurvey1/HouseholdSurvey");
		pane.add(odkIdCtrl);
		label = new JLabel(" ");
		pane.add(label);
		pane.add(new JSeparator(SwingConstants.HORIZONTAL));
		label = new JLabel("Parameters for resumption of a failed download attempt");
		pane.add(label);
		label = new JLabel(" ");
		pane.add(label);
		label = new JLabel("LastCursor-x:");
		pane.add(label);
		lastCursorCtrl = new JTextField("");
		pane.add(lastCursorCtrl);
		label = new JLabel("LastKEY-x:");
		pane.add(label);
		lastKeyCtrl = new JTextField("");
		pane.add(lastKeyCtrl);
		executeCtrl = new JButton("Retrieve");
		executeCtrl.addActionListener(this);
		pane.add(executeCtrl);
		label = new JLabel(" ");
		pane.add(label);
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
		serverUrlCtrl.setEditable(isEnabled);
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
			String candidateUrl = serverUrlCtrl.getText();
			try {
				/** reset the status outcome variables */
				fetchStatus = false;
				eFetchFailure = null;
				activityState = ActivityState.WORKING;
				toggleEnable(false);
				
				// First, verify the url is ok...
				URL url = new URL(candidateUrl);
				if (url.getProtocol().compareToIgnoreCase("http") != 0) {
					errorDialog("bad server url", candidateUrl);
					return;
				}
				// figure out the path we are going to be using...
				String path = url.getPath();
				while (path.endsWith("/")) {
					path = path.substring(0, path.length() - 1);
				}
				int idx = path.lastIndexOf("/");
				if (idx != -1) {
					path = path.substring(0, idx);
				}
				if ( path.length() == 0 ) {
					path = "/";
				}
				candidateUrl = "generated";
				URL serverUrl = new URL(url.getProtocol(), url.getHost(), url
						.getPort(), path);
				candidateUrl = serverUrl.toString();
				
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
				errorDialog("bad settings", eFail.getMessage());
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
		JOptionPane.showMessageDialog(this, error + " " + value, error,
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
