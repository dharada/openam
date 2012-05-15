$Id: README.txt,v 1.4 2012/03/13 09:31:39 jah Exp $

This is the README for OpenAM connector for Computer Associates (CA) SiteMinder.

The OpenAM SiteMinder connector can be used to integrate OpenAM with
SiteMinder. It can create OpenAM sessions from a user's SiteMinder session
and vice versa, making it possible to have single signon between these systems.
This functionality is useful when adding OpenAM based features into an existing
SiteMinder infrastructure, using OpenAM federation with SiteMinder or when
migrating between these systems.

Note that this README is not complete documentation for the connector and
it should be used together with OpenAM and SiteMinder documentation.

This README describes the components of the connector and how to build, install and configure it.

Components
==========

The SiteMinder connector has these main components:
* OpenAM authentication scheme for SiteMinder policy server.
  This allows user to authenticate to SiteMinder with their OpenAM session.

* SiteMinder authentication module for OpenAM.
  This allows user to authenticate to OpenAM with their SiteMinder session.

* OpenAM Post-authentication plugin for creating a SiteMinder session.
  This will create a SiteMinder session on succesful OpenAM login.

* OpenAM SAML2 SP plugin for creating a SiteMinder session.
  This will create a SiteMinder session on succesful SAML2 SSO.

* Utility class for SiteMinder session management.
  This is a utility class used by other components.

Building the connector
======================

See lib/readme_lib.txt for libraries needed for building the connector.
Some of the libraries can be simply copied from existing OpenAM installation,
others need to be copied from SiteMinder SDK or the servlet container used
for running OpenAM.

The connector is built using Apache ant. A succesful build results in file
dist/fam_sm_integration.jar that contains all the connector code. This
same jar file needs to be installed on OpenAM and SiteMinder policy servers.

The tools/create-overlay.sh script can be used to create an overlay directory
structure that can simply be copied over an existing OpenAM installation
in order to install the connector.

Installing the connector
========================

The connector need to be installed on all OpenAM servers and all SiteMinder
policy servers that you intend to use with it.

Any OpenAM server host running the connector needs to have either SiteMinder
SDK or Web Agent installed. The SiteMinder agent has to be configured and
registered as trusted host on the SiteMinder policy server. Generally the
easiest way to do this is to install the web agent and run its configuration
script to do the necessary trusted host registration. Note that it is not
necessary to configure any web servers with the web agent, just doing the
trusted host registration is enough. Refer to SiteMinder documentation for
more information.

For installing the connector on OpenAM server the easiest option is to use
the create-overlay.sh script and copy the resulting file structure over an
already installed OpenAM server (webapp). You can also zip up the overlay file
structure and just unzip the package on the target server to make deploying
to multiple servers easier.

For installing the connector on SiteMinder policy server there are multiple
steps to do:

1) Install and configure OpenAM client SDK on the SiteMinder policy server
   host. Refer to OpenAM documentation for more information. The CommandLineSSO
   script can be used to verify that users can be authenticated with the
   installed client SDK.

2) Add the connector library (fam_sm_integration.jar) to the client SDK.
   This can be done by simply copying the fam_sm_integration.jar to the
   lib directory of the client SDK installation.

2) Change Java security settings.
   The SiteMinder policy server seems to do something that breaks SSL
   connections using the normal Java security settings. To work around this
   the Java security provider list needs to be changed so that
   sun.security.pkcs11.SunPKCS11 is not used. Edit the
   $JAVA_HOME/jre/lib/security/java.security file and remove the SunPKCS11
   provider from the list of configured providers. Remember to re-number the
   remaining providers.

3) Change SiteMinder JVM classpath to include the connector and
   OpenAM client SDK libraries. This is done by editing the file
   <siteminder_install_directory>/config/JVMOptions.txt. Add the following
   libraries to the classpath in JVMOptions.txt:
   <openam_clientsdk_directory>/lib/fam_sm_integration.jar
   <openam_clientsdk_directory>/lib/openssoclientsdk.jar
   <openam_clientsdk_directory>/lib/j2ee.jar
   <openam_clientsdk_directory>/lib/opensso-sharedlib.jar
   <openam_clientsdk_directory>/resources

4) Restart the SiteMinder policy server.

Configuring the connector
=========================

The connector requires configuration in both SiteMinder and OpenAM.

* Creating SiteMinder sessions from OpenAM (logging users into SiteMinder with
  their OpenAM session) requires appropriate custom login scheme and realm
  configuration within SiteMinder. Since the SiteMinder sessions are created
  by simply accessing a web resource protected by SiteMinder the configuration
  needed is similar to any SiteMinder protected website. The only major
  difference is that the protected resource must be configured to use OpenAM
  custom authentication scheme. On OpenAM side you need to configure the
  appropriate post-authentication plugins.

* Creating OpenAM sessions from SiteMinder (logging users into OpenAM with
  their SiteMinder session) requires the SMAuthModule to be configured
  in OpenAM and a working SiteMinder web agent installation on the OpenAM
  server host.

SiteMinder configuration

1) Create OpenAM custom authentication scheme. The parameters should be:
   Name: OpenAM
   Authentication Scheme Type: Custom Template
   Library: smjavaapi
   Parameter: com.sun.identity.authentication.siteminder.OpenAMAuthScheme debug
   (The "debug" parameter enables debug logging and should be removed for
   production installations.)

2) Create OpenAM domain, realm, rule and policy for session management:
   * Create domain OpenAM and add the user directory.
   * Create realm OpenAM SiteMinder and add the configuration for a resource
     on a SiteMinder protected web server. The server and URL don't really
     matter, the important thing is that the resource is configured to use
     OpenAM custom authentication scheme.
   * Create rule getOpenAMLogin and assign the resource to a file/URL on the
     SiteMinder protected web server, for example "/login.html*". Note that
     you need to add wildcard "*" at the end of the filename because
     SiteMinder adds session query string to the URL when accessing it. Assign
     web agent action GET to this rule.
   * Create policy OpenAM Login Policy and assign users from the directory and
     rule "getOpenAMLogin".

OpenAM configuration

TODO

Troubleshooting
===============

The connector uses OpenAM debug logging facility and logs its actions
into "SiteMinder" debug log.

The setsmauthdebug.jsp JSP script can be used to view or change the log
level while the OpenAM server is running. To change the log level you need
to supply the new log level to the JSP as a query string parameter
debugLevel=<debug_level> where the debug level can be message, warning
or error.

NOTE! The setsmauthdebug.jsp does not require any authentication!

