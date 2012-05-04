#!/bin/sh

#
#
# (C) Copyright Janua 2011 - Author : Frédéric Aime (faime@janua.fr)
#  This code has been provided only for informational purpose
#  Deliverables may not be generated from this code,
#  License details are available upon express demand to : contact@janua.fr
#
#  You can studdy this code as far as you want, using and/or modifying it is
#  subject to licence terms available upon demand.

# Deployment script for Sphinx application

ANT="/usr/bin/ant"
JAVA_HOME=""

echo Installation de Sphinx
echo
echo "Warning : Tomcat will be stopped during the installation"
echo -n "Tomcat directory: "
read TOMCAT_HOME
export TOMCAT_HOME

if [ -d ${TOMCAT_HOME}/webapps ]
then
  echo "Tomcat found"
  echo -n "Stopping Tomcat..."
  ${TOMCAT_HOME}/bin/shutdown.sh > /dev/null 2>&1
  echo "Ok"
else
  echo "Could not find Tomcat in ${TOMCAT_HOME}"
  exit
fi

echo -n "OpenAM directory [ ${TOMCAT_HOME}/webapps/openam ]: "
read OPENAM_HOME
export OPENAM_HOME

if [ -z "${OPENAM_HOME}" ]
then
  OPENAM_HOME=${TOMCAT_HOME}/webapps/openam
fi

if [ -d ${OPENAM_HOME} ]
then
  echo "OpenAM found"
else
  echo "Could not find OpenAM in ${OPENAM_HOME}"
fi

echo -n "Sphinx deployment name [ SphynxWAR ] : "
read SPHYNX_WARNAME

if [ -z "${SPHYNX_WARNAME}" ]
then
  SPHYNX_WARNAME=SphynxWAR
fi


if [ -r "dist/SphynxWAR.war" ]
then
  echo war ok
  echo -n "renaming SphynxWAR to ${SPHYNX_WARNAME}.war ..."
  cp "dist/SphynxWAR.war" "dist/${SPHYNX_WARNAME}.war"
  echo "Ok"
else
  echo "The war file is not available in the dist directory, do you want to rebuild it ? [O/n] : "
  read response
  if [ -z "$response" ]
  then
    response=O
  fi

  if [ "$response" = "O" ]
  then
    echo -n "Building WAR..."
    ${ANT} dist > /tmp/sphinx.log 2>&1
    if [ -r dist/${SPHYNX_WARNAME} ]
    then
      echo "Ok"
    else
      echo "${SPHYNX_WARNAME} building failed, please read /tmp/sphinx.log for more information"
      exit
    fi
  else
    echo "You need the WAR to complete the installation"
    exit
  fi
fi

echo "Configuration of sphinx.properties"
echo
echo -n "Copying the model configuration file into tomcat ..."
cp sphinx.properties ${TOMCAT_HOME}/conf
echo "Ok"

echo -n "Press Enter when you are ready to configure sphinx.properties"
read dummy
vi ${TOMCAT_HOME}/conf/sphinx.properties
echo "The configuration has been saved to ${TOMCAT_HOME}/conf/sphinx.properties"

echo "Openam installation of the new Login page"
echo -n "Applying customizations to Login.jsp..."
cat web/Login.jsp | sed -e "s/SphynxWAR/${SPHYNX_WARNAME}/" > /tmp/Login.jsp 2>/dev/null
echo "Ok"
echo -n "Copying Login.jsp to ${OPENAM_HOME}/config/auth/default/Login.jsp..."
cp /tmp/Login.jsp ${OPENAM_HOME}/config/auth/default/Login.jsp
echo "Ok"

echo -n "Deploying ${SPHYNX_WARNAME}.war ..."
cp dist/${SPHYNX_WARNAME}.war ${TOMCAT_HOME}/webapps
echo Ok

echo -n "Sphinx install completed, do you want to restart tomcat ? [O/n] : "
read response
if [ -z "${response}" ]
then
  response=O
fi

if [ "${response}" = "O" ]
then
  echo -n "Starting tomcat..."
  ${TOMCAT_HOME}/bin/startup.sh
  echo "Ok"
fi

echo "Sphinx was successfully installed"
