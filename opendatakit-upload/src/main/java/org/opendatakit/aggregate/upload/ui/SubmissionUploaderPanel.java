/*
 * Copyright (C) 2010 University of Washington.
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
package org.opendatakit.aggregate.upload.ui;

import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.apache.http.client.HttpClient;
import org.opendatakit.aggregate.upload.submission.SubmissionResult;
import org.opendatakit.aggregate.upload.submission.SubmissionUploader;
import org.opendatakit.aggregate.upload.utils.FindDirectoryStructure;
import org.opendatakit.aggregate.upload.utils.HttpClientFactory;

public class SubmissionUploaderPanel extends JPanel implements ActionListener{	
		
	private static final String COMMAND_CHANGE_SERVER = "Edit";
	private static final String COMMAND_REFRESH = "Refresh";
	private static final String COMMAND_SELECT = "Select";
	
	private static final long serialVersionUID = 6753077036860161654L;
	private static final String LOGGER = "SubmissionUploaderPanel";
	private static final String DEFAULT_DIRECTORY_STRUCTURE_TO_SEARCH_FOR = "/odk/instances";
	
	// So this runs first before JVM loads an L&F
	static
	{
		try 
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} 
		catch (Exception e) 
		{
			// Oh well, we will live with whatever L&F Java decides to use
		}
	}
	
	private Logger _logger;
	private String _directoryStructureToSearchFor;
	private List<String> _driveButtonActionCommands;
	
	// GUI components
	private JPanel _panel;
	private GridBagConstraints _c;
	
	private JLabel _serverLabel;
	private JTextField _serverLocationField;
	private JButton _serverChangeButton;
	
	private JLabel _driveLabel;
	private List<JRadioButton> _driveLocationButtons;
	private JButton _driveRefreshButton;
	
	private JLabel _manualDriveLabel;
	private JTextField _manualDriveLocationField;
	private JButton _manualDriveSelectButton;
	
	private enum Action
	{
		NONE,
		SEND_SUBMISSIONS,
		CHANGE_SERVER_FIELD,
		REFRESH_DRIVE_LOCATIONS;
	}
	
	public SubmissionUploaderPanel()
	{
		this(DEFAULT_DIRECTORY_STRUCTURE_TO_SEARCH_FOR);
	}
	
	public SubmissionUploaderPanel(String directoryStructureToSearchFor)
	{
        super(new BorderLayout());
		        
		Logger logger = Logger.getLogger(LOGGER);
		_logger = logger;
		
		_directoryStructureToSearchFor = FindDirectoryStructure.normalizePathString(directoryStructureToSearchFor);
		
		_panel = new JPanel();
		_panel.setLayout(new GridBagLayout());
		_c = new GridBagConstraints();
		// Set common constraint settings
		_c.anchor = GridBagConstraints.LINE_END;
		_c.fill = GridBagConstraints.BOTH;
		_c.insets = new Insets(15, 15, 15, 15);
		
		// Server settings components
		_serverLabel = new JLabel("Server submission location: ");
		_c.gridx = 0;
		_c.gridy = 0;
		_panel.add(_serverLabel, _c);
		
        _serverLocationField = new JTextField(20);
        _serverLocationField.addActionListener(this);
        _serverLocationField.setEditable(false);
        _c.gridx = 1;
        _c.gridy = 0;
        _panel.add(_serverLocationField);
        
        _serverChangeButton = new JButton("Change");
        _serverChangeButton.setActionCommand(COMMAND_CHANGE_SERVER);
        _serverChangeButton.addActionListener(this);
        _c.gridx = 2;
        _c.gridy = 0;
        _panel.add(_serverChangeButton, _c);
       
        // Set up components for finding the mounted drive
        setupDriveLocationComponents();
        
        _driveRefreshButton = new JButton("Refresh");
        _driveRefreshButton.setActionCommand(COMMAND_REFRESH);
        _driveRefreshButton.addActionListener(this);
        _c.gridx = 2;
        _c.gridy = 1;
        _panel.add(_driveRefreshButton, _c);

        _manualDriveLocationField = new JTextField(20);
        _manualDriveLocationField.addActionListener(this);
        _c.gridx = 1;
        _c.gridy = 2;
        _panel.add(_manualDriveLocationField, _c);
        
        _manualDriveSelectButton = new JButton("Select");
        _manualDriveSelectButton.setActionCommand(COMMAND_SELECT);
        _manualDriveSelectButton.addActionListener(this);
        _c.gridx = 2;
        _c.gridy = 2;
        _panel.add(_manualDriveSelectButton, _c);

        add(_panel, BorderLayout.LINE_START);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
	}
	
	private boolean setupDriveLocationComponents()
	{
		// Clear any previous components
		if (_driveLabel != null)
		{
			_panel.remove(_driveLabel);
		}
		if (_driveLocationButtons != null)
		{
			for (JRadioButton driveLocationButton : _driveLocationButtons)
			{
				_panel.remove(driveLocationButton);
			}
		}
		if (_manualDriveLabel != null)
		{
			_panel.remove(_manualDriveLabel);
		}
		
		// Components for autodetection of drive
        List<File> submissionParentDirs = FindDirectoryStructure.searchMountedDrives(_directoryStructureToSearchFor);
        boolean foundDrives = submissionParentDirs.size() != 0;
        
        if (!foundDrives)
        {
        	_driveLabel = new JLabel("No possible drives were autodetected.");
        	_driveLabel.setOpaque(true);
        	_c.gridx = 0;
        	_c.gridy = 1;
        	_panel.add(_driveLabel, _c);
        }
        else
        {        	
        	_driveLabel = new JLabel("Select location of Android phone: ");
        	_driveLabel.setOpaque(true);
        	_c.gridx = 0;
        	_c.gridy = 1;
        	_panel.add(_driveLabel, _c);
        	
            ButtonGroup buttonGroup = new ButtonGroup();
        	_driveLocationButtons = new ArrayList<JRadioButton>();
        	_driveButtonActionCommands = new ArrayList<String>();
	        for (File submissionPDir : submissionParentDirs)
	        {
	        	String name = submissionPDir.getAbsolutePath(); // name = "/media/disk/odk/instances"
	        	int index = name.indexOf(_directoryStructureToSearchFor);
	        	if (index == -1)
	        	{
	        		String error = String.format("Could not find %s in %s. Please enter location manually.", _directoryStructureToSearchFor, name);
	        		getLogger().severe(error);
	        		JOptionPane.showMessageDialog(this, error , "Error", JOptionPane.ERROR_MESSAGE);
	            	JLabel errorLabel = new JLabel("Error autodetecting phone.");
	            	errorLabel.setOpaque(true);
	            	_c.gridx = 0;
	            	_c.gridy = 1;
	            	_panel.add(errorLabel, _c);
	        	}
	        	else
	        	{
	        		name = name.substring(0, index); // name = "/media/disk"
	        	}

	        	JRadioButton submissionPDirButton = new JRadioButton(name);
	        	submissionPDirButton.setMnemonic(name.charAt(0));
	        	submissionPDirButton.setActionCommand(name);
	        	_driveButtonActionCommands.add(name);
	        	submissionPDirButton.addActionListener(this);
	        	buttonGroup.add(submissionPDirButton);
	        	_driveLocationButtons.add(submissionPDirButton);
	        	_c.gridx = 1;
	        	_c.gridy = 1;
	        	_panel.add(submissionPDirButton, _c);
	        } 
	    }
        

        
        // Components for manual location
        if (!foundDrives)
        {
	    	_manualDriveLabel = new JLabel("Location of Android phone: ");
	    	_manualDriveLabel.setOpaque(true);
	    	_c.gridx = 0;
	    	_c.gridy = 2;
	    	_panel.add(_manualDriveLabel, _c);
        }
        else
        {
        	_manualDriveLabel = new JLabel("Or enter location manually: ");
        	_manualDriveLabel.setOpaque(true);
        	_c.gridx = 0;
        	_c.gridy = 2;
        	_panel.add(_manualDriveLabel, _c);
        }
        
        // Show changes
        _panel.validate();
        return foundDrives;
	}
	
	private void setWaitCursor(boolean wait)
	{
		if (wait)
		{
			_panel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			Graphics2D g = (Graphics2D) getGraphics();
			g.setColor(Color.DARK_GRAY);
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
			g.fillRect(0, 0, getWidth(), getHeight());
			_panel.getRootPane().getGlassPane().paint(g);
			_panel.getRootPane().getGlassPane().setEnabled(true);
			_panel.getRootPane().getGlassPane().setVisible(true);
		}
		else
		{
			_panel.setCursor(Cursor.getDefaultCursor());
			_panel.getRootPane().getGlassPane().setVisible(false);
			_panel.getRootPane().getGlassPane().setEnabled(false);
		}
	}
	
	@Override
	public void actionPerformed(ActionEvent event) {
		String actionCommand = event.getActionCommand();
		Action action = Action.NONE;
		String driveLocationString = null;
		
		// Change server field to be editable
		if (actionCommand.equals(COMMAND_CHANGE_SERVER))
		{
			action = Action.CHANGE_SERVER_FIELD;
		}
		// Get radio button value
		else if (_driveButtonActionCommands != null && _driveButtonActionCommands.contains(actionCommand))
		{
			driveLocationString = event.getActionCommand();
			action = Action.SEND_SUBMISSIONS;
		}
		// Refresh drive locations
		else if (actionCommand.equals(COMMAND_REFRESH))
		{
			action = Action.REFRESH_DRIVE_LOCATIONS;
		}
		// Get text field
		else if (actionCommand.equals(COMMAND_SELECT))
		{
			driveLocationString = getDriveLocation();
			action = Action.SEND_SUBMISSIONS;
		}
		
		switch (action)
		{
		case NONE:
			break;
		case CHANGE_SERVER_FIELD:
			_serverChangeButton.setVisible(false);
			_serverLocationField.setEditable(true);
			break;
		case REFRESH_DRIVE_LOCATIONS:
			boolean foundDrives = setupDriveLocationComponents();
			if (!foundDrives)
			{
				JOptionPane.showMessageDialog(this, "Did not find any possible drives.");
			}
			break;
		case SEND_SUBMISSIONS:
			setWaitCursor(true);
			File submissionsParentDir = null;
			ArrayList<String> errors = new ArrayList<String>();

			// Add error message on invalid drive
			if (driveLocationString == null || driveLocationString.equals(""))
			{
				getLogger().warning("No location entered.");
				errors.add("No location entered.");
			}
			// Valid drive, proceed processing
			else
			{
				submissionsParentDir = new File(driveLocationString + _directoryStructureToSearchFor);
				if (!submissionsParentDir.exists())
				{
					getLogger().warning("Directory does not exist: " + submissionsParentDir);
					errors.add("Directory does not exist: " + submissionsParentDir);
				}
				else if(!submissionsParentDir.canRead())
				{
					getLogger().warning("Can't read " + submissionsParentDir);
					errors.add("Don't have permission to read " + submissionsParentDir);
				}
				else if (submissionsParentDir.list().length == 0)
				{
					getLogger().warning("Submissions parent directory is empty -- nothing to submit!");
					errors.add(submissionsParentDir + " is empty - nothing to submit!");
				}
			}
			
			URL serverURL = null;
			try 
			{
				serverURL = getServerURL();
			} 
			catch (MalformedURLException e) 
			{
				getLogger().warning("Bad URL for submission server location - " + e.getMessage());
				errors.add("Bad URL for server submission location.");
			}
			if (serverURL != null && submissionsParentDir != null && errors.isEmpty())
			{
				uploadSubmissions(serverURL, submissionsParentDir);
			}
			else if (!errors.isEmpty())
			{
				errors.add(0, "Please resolve the following problems:");
				JOptionPane.showMessageDialog(this, errors.toArray(), "Errors", JOptionPane.WARNING_MESSAGE);
			}
			setWaitCursor(false);
			break;
		}
	}

	/**
	 * Uploads the submissions, checks the results, and shows the results to the
	 * user.
	 * 
	 * @param submissionURL
	 *            the URL to post the submissions to
	 * @param submissionsParentDir
	 *            the directory containing multiple submissions (each submission
	 *            is a directory of its own)
	 */
	private void uploadSubmissions(URL submissionURL, File submissionsParentDir)
	{
		// Set up HttpClient
		HttpClient httpClient = HttpClientFactory.getHttpClient(submissionURL, getLogger());
        
        // Set up ExecutorService
        ExecutorService es = new ThreadPoolExecutor(0, 2, 30, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        
        // Submit submissions
		SubmissionUploader uploader = new SubmissionUploader(httpClient, es); 
		List<Future<SubmissionResult>> submissionResults = uploader.uploadSubmissions(submissionsParentDir, submissionURL);
		
		// Check submission results
		List<SubmissionResult> failedSubmissions = uploader.checkSubmissionResultsAndDeleteSubmissions(submissionResults);
		String resultMessage = "";
		if (failedSubmissions != null && failedSubmissions.size() > 0)
		{
			String failedSubmissionsFiles = "";
			for (SubmissionResult result : failedSubmissions)
			{
				failedSubmissionsFiles += "    " + result.getFile().toString() + "    (" + result.getFailureReason() + ")\n";
			}
			try 
			{
				resultMessage = "Failed submissions for URL: " + getServerURL() + "\n" + failedSubmissionsFiles;
			}
			catch (MalformedURLException e)
			{
				resultMessage = "Bad URL. Please fix the URL and resubmit.";
			}
			getLogger().severe(resultMessage);
		}
		else
		{
			resultMessage = "All submissions successfully uploaded!";
			getLogger().info(resultMessage);
		}
		JOptionPane.showMessageDialog(this, resultMessage);
		
		// Shut down resources
		httpClient.getConnectionManager().shutdown();
		es.shutdown();
	}
	
	public String getDriveLocation()
	{
		return _manualDriveLocationField.getText();
	}
	
	public void setDriveLocation(String driveLocation)
	{
		_manualDriveLocationField.setText(driveLocation);
	}

	public URL getServerURL() throws MalformedURLException
	{
		return new URL(_serverLocationField.getText());
	}
	
	public void setServerURL(URL serverURL)
	{
		_serverLocationField.setText(serverURL.toString());
	}
	
	public Logger getLogger() {
		if (_logger == null)
		{
			Logger logger = Logger.getLogger(LOGGER);
			logger.setLevel(Level.OFF);
			_logger = logger;
		}
			return _logger;
	}

	public void setLogger(Logger logger) {
		_logger = logger;
	}
	
    /**
     * Create the GUI and show it.
     */
    private static void createAndShowGUI() {
		SubmissionUploaderPanel panel = new SubmissionUploaderPanel();
		
		JFrame frame = new JFrame("SubmissionUploaderPanel");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		panel.setOpaque(true);
		frame.setContentPane(panel);
		
		frame.pack();
		frame.setVisible(true);
    }

	
	public static void main(String[] args)
	{
		try
		{
			javax.swing.SwingUtilities.invokeLater(new Runnable()
			{
				public void run()
				{
					createAndShowGUI();
				}
			});
		}
		catch (Exception e)
		{
			System.out.println(e.getMessage());
		}
	}

}
