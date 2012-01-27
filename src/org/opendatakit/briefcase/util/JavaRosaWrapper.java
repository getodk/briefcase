/*
 * Copyright (C) 2011 University of Washington.
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

package org.opendatakit.briefcase.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;

import org.javarosa.core.model.DataBinding;
import org.javarosa.core.model.FormDef;
import org.javarosa.core.model.SubmissionProfile;
import org.javarosa.core.model.instance.FormInstance;
import org.javarosa.core.model.instance.TreeElement;
import org.javarosa.xform.parse.IXFormParserFactory;
import org.javarosa.xform.parse.XFormParser;
import org.javarosa.xform.util.XFormUtils;
import org.kxml2.kdom.Document;
import org.kxml2.kdom.Element;
import org.opendatakit.briefcase.util.JavaRosaWrapper.BadFormDefinition.Reason;

public class JavaRosaWrapper {

	static Logger log = Logger.getLogger(JavaRosaWrapper.class.getName());
	private static final String BASE64_RSA_PUBLIC_KEY = "base64RsaPublicKey";
	private static final String NAMESPACE_ODK = "http://www.opendatakit.org/xforms";
   private static final String ENCRYPTED_FORM_DEFINITION = "<?xml version=\"1.0\"?>"
	      + "<h:html xmlns=\"http://www.w3.org/2002/xforms\" xmlns:h=\"http://www.w3.org/1999/xhtml\" xmlns:ev=\"http://www.w3.org/2001/xml-events\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:odk=\""
	      + NAMESPACE_ODK
	      + "\" xmlns:jr=\"http://openrosa.org/javarosa\">"
	      + "<h:head>"
	      + "<h:title>Encrypted Form</h:title>"
	      + "<model>"
	      + "<instance>"
	      + "<data id=\"encrypted\" xmlns=\"http://www.opendatakit.org/xforms/encrypted\" xmlns:orx=\"http://openrosa.org/xforms\">"
	      + "<base64EncryptedKey/>"
	      + "<orx:meta>"
	      + "<orx:instanceID/>"
	      + "</orx:meta>"
	      + "<media>"
	      + "<file/>"
	      + "</media>"
	      + "<encryptedXmlFile/>"
	      + "<base64EncryptedElementSignature/>"
	      + "</data>"
	      + "</instance>"
	      + "<bind nodeset=\"/data/base64EncryptedKey\" type=\"string\" odk:length=\"2048\" />"
	      + "<bind nodeset=\"/data/meta/instanceID\" type=\"string\"/>"
	      + "<bind nodeset=\"/data/media/file\" type=\"binary\"/>"
	      + "<bind nodeset=\"/data/encryptedXmlFile\" type=\"binary\"/>"
	      + "<bind nodeset=\"/data/base64EncryptedElementSignature\" type=\"string\" odk:length=\"2048\" />"
	      + "</model>"
	      + "</h:head>"
	      + "<h:body>"
	      + "<input ref=\"base64EncryptedKey\"><label>Encrypted Symmetric Key</label></input>"
	      + "<input ref=\"meta/instanceID\"><label>InstanceID</label></input>"
	      + "<repeat nodeset=\"/data/media\">"
	      + "<upload ref=\"file\" mediatype=\"image/*\"><label>media file</label></upload>"
	      + "</repeat>"
	      + "<upload ref=\"encryptedXmlFile\" mediatype=\"image/*\"><label>submission</label></upload>"
	      + "<input ref=\"base64EncryptedElementSignature\"><label>Encrypted Element Signature</label></input>"
	      + "</h:body>" + "</h:html>";
	  
	  private static class XFormParserWithBindEnhancementsFactory implements IXFormParserFactory {

		  JavaRosaWrapper parser;
	    
	    XFormParserWithBindEnhancementsFactory(JavaRosaWrapper parser) {
	      this.parser = parser;
	    }
	    
	    @Override
	    public XFormParser getXFormParser(Reader form) {
	      return new XFormParserWithBindEnhancements(parser, form);
	    }

	    @Override
	    public XFormParser getXFormParser(Document form) {
	      return new XFormParserWithBindEnhancements(parser, form);
	    }

	    @Override
	    public XFormParser getXFormParser(Reader form, Reader instance) {
	      return new XFormParserWithBindEnhancements(parser, form, instance);
	    }

	    @Override
	    public XFormParser getXFormParser(Document form, Document instance) {
	      return new XFormParserWithBindEnhancements(parser, form, instance);
	    }
	  }
	  
	  private static class XFormParserWithBindEnhancements extends XFormParser {
	    private JavaRosaWrapper parser;
	    
	    public XFormParserWithBindEnhancements(JavaRosaWrapper parser, Reader form) {
	      super(form);
	      this.parser = parser;
	    }

	    public XFormParserWithBindEnhancements(JavaRosaWrapper parser, Document form) {
	      super(form);
	      this.parser = parser;
	    }
	    
	    public XFormParserWithBindEnhancements(JavaRosaWrapper parser, Reader form, Reader instance) {
	      super(form, instance);
	      this.parser = parser;
	    }
	    
	    public XFormParserWithBindEnhancements(JavaRosaWrapper parser, Document form, Document instance) {
	      super(form, instance);
	      this.parser = parser;
	    }

	    protected void parseBind(Element e) {
	      Vector<String> usedAtts = new Vector<String>();

	      DataBinding binding = processStandardBindAttributes( usedAtts, e);

	      String value = e.getAttributeValue(NAMESPACE_ODK, "length");
	      if ( value != null ) {
	         e.setAttribute(NAMESPACE_ODK, "length", null);
	      }
	      
	      if ( value != null ) {
	         log.info("Calling handle found value " + ((value == null) ? "null" : value));

	         Integer iValue = Integer.valueOf(value);
	         parser.setNodesetStringLength(e.getAttributeValue(null, "nodeset"), iValue);
	      }
	      
	      //print unused attribute warning message for parent element
	      if(XFormUtils.showUnusedAttributeWarning(e, usedAtts)){
	         System.out.println(XFormUtils.unusedAttWarning(e, usedAtts));
	      }

	      addBinding(binding);
	    }
	  }

	public static class BadFormDefinition extends Exception {

		public enum Reason {
			UNKNOWN, TITLE_MISSING, ID_MISSING, ID_MALFORMED, MISSING_XML, BAD_FILE, BAD_JR_PARSE;
		}

		private Reason reason;

		/**
		 * Serial number for serialization
		 */
		private static final long serialVersionUID = -8894929454515911356L;

		/**
		 * Default constructor
		 */
		public BadFormDefinition() {
			super();
			reason = Reason.UNKNOWN;
		}

		/**
		 * Construct exception with the error message
		 * 
		 * @param message
		 *            exception message
		 */
		public BadFormDefinition(String message) {
			super(message);
			reason = Reason.UNKNOWN;
		}

		/**
		 * Construction exception with error message and throwable cause
		 * 
		 * @param message
		 *            exception message
		 * @param cause
		 *            throwable cause
		 */
		public BadFormDefinition(String message, Throwable cause) {
			super(message, cause);
			reason = Reason.UNKNOWN;
		}

		/**
		 * Construction exception with throwable cause
		 * 
		 * @param cause
		 *            throwable cause
		 */
		public BadFormDefinition(Throwable cause) {
			super(cause);
			reason = Reason.UNKNOWN;
		}

		/**
		 * Default constructor with reason
		 * 
		 * @param exceptionReason
		 *            exception reason
		 */
		public BadFormDefinition(Reason exceptionReason) {
			super();
			reason = exceptionReason;
		}

		/**
		 * Construct exception with the error message and reason
		 * 
		 * @param message
		 *            exception message
		 * @param exceptionReason
		 *            exception reason
		 */
		public BadFormDefinition(String message, Reason exceptionReason) {
			super(message);
			reason = exceptionReason;
		}

		/**
		 * Construction exception with error message, throwable cause, and
		 * reason
		 * 
		 * @param message
		 *            exception message
		 * @param cause
		 *            throwable cause
		 * @param exceptionReason
		 *            exception reason
		 */
		public BadFormDefinition(String message, Throwable cause,
				Reason exceptionReason) {
			super(message, cause);
			reason = exceptionReason;
		}

		/**
		 * Construction exception with throwable cause and reason
		 * 
		 * @param cause
		 *            throwable cause
		 * @param exceptionReason
		 *            exception reason
		 */
		public BadFormDefinition(Throwable cause, Reason exceptionReason) {
			super(cause);
			reason = exceptionReason;
		}

		/**
		 * Get the reason why the exception was generated
		 * 
		 * @return the reason
		 */
		public Reason getReason() {
			return reason;
		}

	};

	public static final class XFormParameters implements Comparable<Object> {

		public final String formId;
		public final Long modelVersion;
		public final Long uiVersion;

		public XFormParameters(String formId, Long modelVersion, Long uiVersion) {
			if (formId == null) {
				throw new IllegalArgumentException("formId cannot be null");
			}
			this.formId = formId;
			this.modelVersion = modelVersion;
			this.uiVersion = uiVersion;
		}

		@Override
		public String toString() {
			return "XFormParameters[formId="
					+ formId
					+ " and version="
					+ (modelVersion == null ? "null" : Long
							.toString(modelVersion)) + " and uiVersion="
					+ (uiVersion == null ? "null" : Long.toString(uiVersion))
					+ "]";
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof XFormParameters))
				return false;
			XFormParameters p = (XFormParameters) obj;
			return formId.equals(p.formId)
					&& ((modelVersion == null) ? p.modelVersion == null
							: ((p.modelVersion != null) && modelVersion
									.equals(p.modelVersion)))
					&& ((uiVersion == null) ? p.uiVersion == null
							: ((p.uiVersion != null) && uiVersion
									.equals(p.uiVersion)));
		}

		@Override
		public int hashCode() {
			return Long.valueOf(
					formId.hashCode()
							+ ((modelVersion == null) ? 20480L
									: 37 * modelVersion)
							+ ((uiVersion == null) ? 40965L : 91 * uiVersion))
					.hashCode();
		}

		@Override
		public int compareTo(Object obj) {
			if (obj == null || !(obj instanceof XFormParameters))
				return -1;
			XFormParameters p = (XFormParameters) obj;
			int cmp = formId.compareTo(p.formId);
			if (cmp != 0)
				return cmp;
			if (((modelVersion == null) ? (p.modelVersion == null)
					: (p.modelVersion != null && modelVersion
							.equals(p.modelVersion)))) {
				if (((uiVersion == null) ? (p.uiVersion == null)
						: (p.uiVersion != null && uiVersion.equals(p.uiVersion)))) {
					return 0;
				} else if (uiVersion == null) {
					return 1;
				} else if (p.uiVersion == null) {
					return -1;
				} else {
					return uiVersion.compareTo(p.uiVersion);
				}
			} else if (modelVersion == null) {
				return 1;
			} else if (p.modelVersion == null) {
				return -1;
			} else {
				return modelVersion.compareTo(p.modelVersion);
			}
		}
	}

	public static final class ParserConsts {

		public static final String DEFAULT_NAMESPACE = "ODK_DEFAULT";

		public static final String FORM_ID_ATTRIBUTE_NAME = "id";

		public static final String MODEL_VERSION_ATTRIBUTE_NAME = "version";

		public static final String UI_VERSION_ATTRIBUTE_NAME = "uiVersion";

		public static final String NAMESPACE_ATTRIBUTE = "xmlns";

		public static final String FORWARD_SLASH = "/";

		public static final String FORWARD_SLASH_SUBSTITUTION = "&frasl;";

		public static final String VALUE_FORMATTED = "  Value: ";

		public static final String ATTRIBUTE_FORMATTED = " Attribute> ";

		public static final String NODE_FORMATTED = "Node: ";

		/**
		 * The max file size that can be uploaded/parsed
		 */
		public final static int FILE_SIZE_MAX = 5000000;
	}


	  private static synchronized final FormDef parseFormDefinition(String xml, JavaRosaWrapper parser) throws BadFormDefinition {

	    IXFormParserFactory oldFactory = XFormUtils.setXFormParserFactory(new XFormParserWithBindEnhancementsFactory(parser));
		    
		    FormDef formDef = null;
		    try {
		      formDef = XFormUtils.getFormFromInputStream(new ByteArrayInputStream(xml.getBytes()));
		    } catch (Exception e) {
		      throw new BadFormDefinition(e, Reason.BAD_JR_PARSE);
		    } finally {
		      XFormUtils.setXFormParserFactory(oldFactory);
		    }
		    
		    return formDef;
	  }

	/**
	 * The ODK Id that uniquely identifies the form
	 */
	private final XFormParameters rootElementDefn;
	private final XFormParameters submissionElementDefn;
	boolean isEncryptedForm = false;
	boolean isNotUploadableForm = false;
	boolean isInvalidFormXmlns = false; // true if legacy 0.9.8 form with improper xmlns form id.
	private final TreeElement submissionElement;
	
	// if encrypted, the top level element of the encrypted structure...
	private final TreeElement encryptedSubmissionElement;
	
	private String title;

	/**
	 * The XForm definition in XML
	 */
	private final String xml;
	private final Map<String, Integer> stringLengths = new HashMap<String, Integer>();
	private File formDefinitionFile;
	private String md5Hash;

	private void setNodesetStringLength(String nodeset, Integer length) {
		stringLengths.put(nodeset, length);
	}

	/**
	 * Extract the form id, version and uiVersion.
	 * 
	 * @param rootElement
	 *            - the tree element that is the root submission.
	 * @param defaultFormIdValue
	 *            - used if no "id" attribute found. This should already be
	 *            slash-substituted.
	 * @return
	 */
	private XFormParameters extractFormParameters(TreeElement rootElement,
			String defaultFormIdValue) {

		String formIdValue = null;
		String versionString = rootElement.getAttributeValue(null, "version");
		String uiVersionString = rootElement.getAttributeValue(null,
				"uiVersion");

		// search for the "id" attribute
		for (int i = 0; i < rootElement.getAttributeCount(); i++) {
			String name = rootElement.getAttributeName(i);
			if (name.equals(ParserConsts.FORM_ID_ATTRIBUTE_NAME)) {
				formIdValue = rootElement.getAttributeValue(i);
				formIdValue = formIdValue.replaceAll(
						ParserConsts.FORWARD_SLASH,
						ParserConsts.FORWARD_SLASH_SUBSTITUTION);
				break;
			}
		}

		return new XFormParameters((formIdValue == null) ? defaultFormIdValue
				: formIdValue, (versionString == null) ? null
				: Long.valueOf(versionString), (uiVersionString == null) ? null
				: Long.valueOf(uiVersionString));
	}

	public final static String newMD5HashUri(byte[] asBytes) {
        try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(asBytes);
			
            byte[] messageDigest = md.digest();

            BigInteger number = new BigInteger(1, messageDigest);
            String md5 = number.toString(16);
            while (md5.length() < 32)
                md5 = "0" + md5;
            return "md5:" + md5;
        } catch (NoSuchAlgorithmException e) {
        	throw new IllegalStateException("Unexpected problem computing md5 hash", e);
		}
	}

	/**
	 * Constructor that parses and xform from the input stream supplied and
	 * creates the proper ODK Aggregate Form definition in the gae datastore.
	 * 
	 * @param formDefinitionFile the file holding the form definition
	 * @throws ODKFormAlreadyExistsException
	 * @throws ODKIncompleteSubmissionData
	 * @throws ODKConversionException
	 * @throws ODKDatastoreException
	 * @throws ODKParseException
	 */
	public JavaRosaWrapper(File formDefinitionFile) throws BadFormDefinition {

		this.formDefinitionFile = formDefinitionFile;
		StringBuilder xmlBuilder = new StringBuilder();
		BufferedReader rdr = null;
		try {
			rdr = new BufferedReader(new FileReader(formDefinitionFile));
			String line = rdr.readLine();
			while (line != null) {
				xmlBuilder.append(line);
				line = rdr.readLine();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new BadFormDefinition(Reason.BAD_FILE);
		} catch (IOException e) {
			e.printStackTrace();
			throw new BadFormDefinition(Reason.BAD_FILE);
		} finally {
			if (rdr != null) {
				try {
					rdr.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		String inputXml = xmlBuilder.toString();
		if (inputXml == null || inputXml.length() == 0) {
			throw new BadFormDefinition(Reason.MISSING_XML);
		}

		xml = inputXml;
		md5Hash = newMD5HashUri(xml.getBytes());
		FormDef formDef = parseFormDefinition(xml, this);

		if (formDef == null) {
			throw new BadFormDefinition(
					"Javarosa failed to construct a FormDef.  Is this an XForm definition?",
					Reason.BAD_JR_PARSE);
		}
		FormInstance dataModel = formDef.getInstance();
		if (dataModel == null) {
			throw new BadFormDefinition(
					"Javarosa failed to construct a FormInstance.  Is this an XForm definition?",
					Reason.BAD_JR_PARSE);
		}
		TreeElement rootElement = dataModel.getRoot();

		isInvalidFormXmlns = false;
		String schemaValue = dataModel.schema;
		if (schemaValue != null) {
			int idx = schemaValue.indexOf(":");
			if (idx != -1) {
				if (schemaValue.indexOf("/") < idx) {
					// malformed...  let the bad value pass...
					schemaValue = schemaValue.replaceAll(
							ParserConsts.FORWARD_SLASH,
							ParserConsts.FORWARD_SLASH_SUBSTITUTION);
					isInvalidFormXmlns = true;
				} else {
					// need to escape all slashes... for xpath processing...
					schemaValue = schemaValue.replaceAll(
							ParserConsts.FORWARD_SLASH,
							ParserConsts.FORWARD_SLASH_SUBSTITUTION);
				}
			} else {
				// malformed...
				schemaValue = schemaValue.replaceAll(
						ParserConsts.FORWARD_SLASH,
						ParserConsts.FORWARD_SLASH_SUBSTITUTION);
				isInvalidFormXmlns = true;
			}
		}
		try {
			rootElementDefn = extractFormParameters(rootElement, schemaValue);
		} catch (IllegalArgumentException e) {
			if (isInvalidFormXmlns) {
				throw new BadFormDefinition(
						"xmlns attribute for the data model is not well-formed: '"
								+ dataModel.schema
								+ "' should be of the form xmlns=\"http://your.domain.org/formId\"\nConsider defining the formId using the 'id' attribute instead of the 'xmlns' attribute (id=\"formId\")",
						Reason.ID_MALFORMED);
			} else {
				throw new BadFormDefinition(
						"The data model does not have an id or xmlns attribute.  Add an id=\"your.domain.org:formId\" attribute to the top-level instance data element of your form.",
						Reason.ID_MISSING);
			}
		}

		String formName = null;
		if ( formDefinitionFile.getName().endsWith(".xml") ) {
			formName = formDefinitionFile.getName().substring(0,formDefinitionFile.getName().length()-4);
		}
		// obtain form title either from the xform itself or from user entry
		title = formDef.getTitle();
		if (title == null) {
			if (formName == null) {
				throw new BadFormDefinition(Reason.TITLE_MISSING);
			} else {
				title = formName;
			}
		}
		// clean illegal characters from title
		title = title.replace(ParserConsts.FORWARD_SLASH, "");

		TreeElement trueSubmissionElement;
		// Determine the information about the submission...
		SubmissionProfile p = formDef.getSubmissionProfile();
		if (p == null || p.getRef() == null) {
			trueSubmissionElement = rootElement;
			submissionElementDefn = rootElementDefn;
		} else {
			trueSubmissionElement = formDef.getInstance().resolveReference(
					p.getRef());
			if ( trueSubmissionElement == null ) {
				trueSubmissionElement = rootElement;
				submissionElementDefn = rootElementDefn;
			} else {
				try {
					submissionElementDefn = extractFormParameters(
							trueSubmissionElement, null);
				} catch (Exception e) {
					throw new BadFormDefinition(
							"The non-root submission element in the data model does not have an id attribute.  Add an id=\"your.domain.org:formId\" attribute to the submission element of your form.",
							Reason.ID_MISSING);
				}
			}
		}

		if (p != null) {
			String altUrl = p.getAction();
			isNotUploadableForm = (altUrl == null || !altUrl.startsWith("http")
					|| p.getMethod() == null || !p.getMethod().equals(
					"form-data-post"));
		}

		if (isNotUploadableForm) {
			log.info("Form "
					+ submissionElementDefn.formId
					+ " is not uploadable (submission method is not form-data-post or does not have an http: or https: url. ");
		}

		String publicKey = null;
		if (p != null) {
			publicKey = p.getAttribute(BASE64_RSA_PUBLIC_KEY);
		}

		// now see if we are encrypted -- if so, fake the submission element to
		// be
		// the parsing of the ENCRYPTED_FORM_DEFINITION
		if (publicKey == null || publicKey.length() == 0) {
			// not encrypted...
			submissionElement = trueSubmissionElement;
			encryptedSubmissionElement = null;
		} else {
			isEncryptedForm = true;
			submissionElement = trueSubmissionElement;
			
			// encrypted -- use the encrypted form template (above) to define
			// the storage for this form.
			formDef = parseFormDefinition(ENCRYPTED_FORM_DEFINITION, this);

			if (formDef == null) {
				throw new BadFormDefinition(
						"Javarosa failed to construct Encrypted FormDef!",
						Reason.BAD_JR_PARSE);
			}
			dataModel = formDef.getInstance();
			if (dataModel == null) {
				throw new BadFormDefinition(
						"Javarosa failed to construct Encrypted FormInstance!",
						Reason.BAD_JR_PARSE);
			}
			encryptedSubmissionElement = dataModel.getRoot();
		}
	}

	public XFormParameters getRootElementDefn() {
		return rootElementDefn;
	}

	public XFormParameters getSubmissionElementDefn() {
		return submissionElementDefn;
	}
	
	public String getSubmissionKey(String uri) {
		return submissionElementDefn.formId +
		"[@version=" + submissionElementDefn.modelVersion +
		" and @uiVersion=" + submissionElementDefn.uiVersion +
		"]/" + ( isEncryptedForm() ? "data" : submissionElement.getName()) + 
		"[@key=" + uri + "]";
	}

	public TreeElement getSubmissionElement() {
		return submissionElement;
	}
	
	public boolean isEncryptedForm() {
		return isEncryptedForm;
	}

	public boolean isNotUploadableForm() {
		return isNotUploadableForm;
	}
	
	public boolean isInvalidFormXmlns() {
		return isInvalidFormXmlns;
	}
	
	public String getFormName() {
		return title;
	}
	
	public File getFormDefinitionFile() {
		return formDefinitionFile;
	}
	
	public String getMD5Hash() {
		return md5Hash;
	}
}
