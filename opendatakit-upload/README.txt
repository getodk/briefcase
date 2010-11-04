This project uses Maven, for more about Maven see http://maven.apache.org/

Getting it to build:

Make a keystore
===============
1) Go into a directory outside of the opendatakit-upload project where you would like your keystore to reside.
2) Run this command to generate a keystore and key pair:
	keytool -genkey -alias <alias> -keystore <keystore-name> 
		-keypass <password> -dname "cn=<some-name>" 
		-storepass <password>
3) Now edit src/main/config/jarSignerDetails.txt (create it if it doesn't exist) and add:
        # Note: you may use maven properties in the entries if you wish 
        # e.g. ${project.basedir} to reference the basedir of opendatakit-upload
	jarSigner.password=<password>
	jarSigner.keystore=<path-to-keystore>
	jarSigner.signAsAlias=<alias>
	jarSigner.certDir=<anything> #This is currently not used
4) That should set up everything properly for signing the build jars. 

Build the project
=================
mvn package -- compiles the source, runs unit tests, signs jars, and packages applet into zip file
