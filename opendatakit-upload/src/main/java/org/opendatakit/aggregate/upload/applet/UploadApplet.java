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
package org.opendatakit.aggregate.upload.applet;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JApplet;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.opendatakit.aggregate.upload.ui.SubmissionUploaderPanel;
import org.opendatakit.aggregate.upload.utils.BaseURLFinder;

public class UploadApplet extends JApplet{	

	public static final String ODK_INSTANCES_DIR = "/odk/instances";
	public static final String AGGREGATE_SUBMISSION_SERVLET = "/submission";
	
	public static String LOGGER = "UploadApplet";
	private static final long serialVersionUID = 1067499473847917508L;
	
	private Logger _logger; 
	private URL _submissionURL; 
	
	@Override
	public void init() 
	{
		// Collect errors during setup
		ArrayList<String> errors = new ArrayList<String>();
		
        // Set up Logger
        Logger logger = Logger.getLogger(LOGGER);
        logger.setLevel(Level.ALL);
        _logger = logger;
		
        // Get submission URL
        try 
        {
			URL baseURL = BaseURLFinder.getBaseURL(this.getDocumentBase());
			URL submissionURL = new URL(baseURL.toString() + AGGREGATE_SUBMISSION_SERVLET);
			_submissionURL = submissionURL;
		} 
        catch (MalformedURLException e) 
		{
        	errors.add("Bad URL: " + this.getCodeBase());
        	getLogger().severe("Bad URL: " + this.getCodeBase());
		}
        
        // Set up GUI
        try 
        {
            SwingUtilities.invokeAndWait(new Runnable() 
            {
                public void run() 
                {
                    createSubmissionUploaderPanel(_submissionURL);
                }
            });
        } 
        catch (Exception e) 
        { 
        	errors.add("Could not create applet interface.");
            getLogger().severe("Could not create GUI.");
        }
        
        // If we have errors, show them to the user
        if (!errors.isEmpty())
        	JOptionPane.showMessageDialog(this, errors.toArray());
	}
	
	private void createSubmissionUploaderPanel(URL submissionURL)
	{
		SubmissionUploaderPanel panel = new SubmissionUploaderPanel(ODK_INSTANCES_DIR);
		panel.setServerURL(submissionURL);
		panel.setOpaque(true);
		setContentPane(panel);
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
}
