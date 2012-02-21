#!/bin/sh
# Portions Copyrighted 2011-2012 Progress Software Corporation
# $Id: create-overlay.sh,v 1.3 2012/02/17 11:23:57 jah Exp $
# This script will create an overlay directory structure that can be
# zipped up for installing over an existing OpenAM webapp.
if [ \( \! -d "${1}" \) -o \( \! -d "${1}/dist" \) -o \( ! -d "${2}" \) ]
then
  echo Usage: ${0} source-directory target-directory
  exit 1
fi
# Exit if a command returns an error
set -e
SOURCE="${1}"
TARGET="${2}"

if [ \! -d "${TARGET}/config/auth/default" ]
then
  mkdir -p "${TARGET}/config/auth/default"
fi
cp -p "${SOURCE}/config/SMAuthModule.xml" "${TARGET}/config/auth/default"

if [ \! -d "${TARGET}/config/auth/default_en" ]
then
  mkdir -p "${TARGET}/config/auth/default_en"
fi
cp -p "${SOURCE}/config/SMAuthModule.xml" "${TARGET}/config/auth/default_en"

if [ \! -d "${TARGET}/WEB-INF/classes" ]
then
  mkdir -p "${TARGET}/WEB-INF/classes"
fi
cp -p "${SOURCE}/config/SMAuth.properties" "${TARGET}/WEB-INF/classes"
cp -p "${SOURCE}/config/SMCreateSessionPlugin.properties" "${TARGET}/WEB-INF/classes"

if [ \! -d "${TARGET}/WEB-INF/lib" ]
then
  mkdir -p "${TARGET}/WEB-INF/lib"
fi
cp -p "${SOURCE}/dist/fam_sm_integration.jar" "${TARGET}/WEB-INF/lib"
cp -p "${SOURCE}/lib/smjavaagentapi.jar" "${TARGET}/WEB-INF/lib"
cp -p "${SOURCE}/lib/SmJavaApi.jar" "${TARGET}/WEB-INF/lib"

cp -p "${SOURCE}/tools/setsmauthdebug.jsp" "${TARGET}"
