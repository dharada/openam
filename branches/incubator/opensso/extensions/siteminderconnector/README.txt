$Id: README.txt,v 1.1 2012/02/21 12:54:00 jah Exp $

This is the README for OpenAM connector for Computer Associates (CA) SiteMinder.

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

For installing the connector on OpenAM server the easiest option is to use
the create-overlay.sh script and copy the resulting file structure over an
already installed OpenAM server (webapp). You can also zip up the overly file
structure and just unzip the package on the target server to make deploying
to multiple servers easier.

For installing the connector on SiteMinder policy server there are multiple
steps to do:
1) Install and configure OpenAM client SDK on the SiteMinder policy server host.
2) Add the connector library (fam_sm_integration.jar) to the client SDK.
2) Change Java security settings.
3) Change SiteMinder JVM classpath to include the connector and
   OpenAM client SDK libraries.

Configuring the connector
TODO

Troubleshooting

The connector uses OpenAM debug logging facility and logs its actions
into "SiteMinder" debug log.

The setsmauthdebug.jsp JSP script can be used to view or change the log
level while the OpenAM server is running. To change the log level you need
to supply the new log level to the JSP as a query string parameter
debugLevel=<debug_level> where the debug level can be message, warning
or error.

NOTE! The setsmauthdebug.jsp does not require any authentication!

