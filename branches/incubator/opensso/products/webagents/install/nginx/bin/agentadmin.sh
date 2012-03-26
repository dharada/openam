#!/bin/sh

#
# install script for nginx policy agent
#

set -u
#set -x

AGENT_ADMIN=`dirname "$0"`
AGENT_HOME=`readlink -f ${AGENT_ADMIN}/..`
CRYPT_UTIL="${AGENT_HOME}/bin/crypt_util"

AGENT_CONF_DIR=${AGENT_HOME}/conf
AGENT_BOOTSTRAP=${AGENT_CONF_DIR}/OpenSSOAgentBootstrap.properties
AGENT_CONFIG=${AGENT_CONF_DIR}/OpenSSOAgentConfiguration.properties
AGENT_BOOTSTRAP_TEMPL=${AGENT_CONF_DIR}/OpenSSOAgentBootstrap.template
AGENT_CONFIG_TEMPL=${AGENT_CONF_DIR}/OpenSSOAgentConfiguration.template

#RANDOM_SOURCE=/dev/random
RANDOM_SOURCE=/dev/urandom

function usage(){
	echo "usage:"
    echo "        $0 --install"
}

function generate_key(){
    # EL6 have /usr/bin/base64 in coreutils
    # but we need to work for the other platform.
    head -c 24 ${RANDOM_SOURCE} | base64
}

function read_loop(){
    while true; do
        read -p "$1" "$2";
        echo
        eval "LEN=\${#$2}"
        if [ $LEN -ne 0 ]; then
            break
        fi
    done
}

function agent_install_input(){
    cat << EOF
************************************************************************
Welcome to the OpenSSO Policy Agent for NGINX

************************************************************************

EOF

    echo 'Enter the URL where the OpenAM server is running.'
    echo 'Please include the deployment URI also as shown below:'
    echo '(http://opensso.sample.com:58080/opensso)'
    read_loop "OpenSSO server URL: " OPENAM_URL

    echo 'Enter the Agent URL as shown below: (http://agent1.sample.com:1234)'
    read_loop "Agent URL: " AGENT_URL

    echo 'Enter the Agent profile name'
    read_loop "Enter the Agent Profile name: " AGENT_PROFILE_NAME

    echo 'Enter the password to be used for identifying the Agent.'
    echo '*THIS IS NOT PASSWORD FILE*'
    stty -echo
    read_loop "Enter the Agent Password: " AGENT_PASSWORD;
    stty echo

    cat << EOF
-----------------------------------------------
SUMMARY OF YOUR RESPONSES
-----------------------------------------------
OpenSSO server URL : ${OPENAM_URL}
Agent URL : ${AGENT_URL}
Agent Profile name : ${AGENT_PROFILE_NAME}
EOF
    echo 'Continue with Installation?'
    read_loop "[y/N]: " CONFIRM
    if [ ${CONFIRM} != "y" ]; then
        exit
    fi
}

function agent_install(){
    AGENT_ENCRYPT_KEY=`generate_key`
    AGENT_ENCRYPTED_PASSWORD=`${CRYPT_UTIL} ${AGENT_PASSWORD} ${AGENT_ENCRYPT_KEY}`
    AGENT_LOGS_DIR="${AGENT_HOME}/logs/"
    sed -e "s%@OPENAM_URL@%${OPENAM_URL}%" \
        -e "s%@AGENT_PROFILE_NAME@%${AGENT_PROFILE_NAME}%" \
        -e "s%@AGENT_ENCRYPTED_PASSWORD@%${AGENT_ENCRYPTED_PASSWORD}%" \
        -e "s%@AGENT_ENCRYPT_KEY@%${AGENT_ENCRYPT_KEY}%" \
        -e "s%@AGENT_LOGS_DIR@%${AGENT_LOGS_DIR}%" \
        ${AGENT_BOOTSTRAP_TEMPL} > ${AGENT_BOOTSTRAP}

    sed -e "s%@OPENAM_URL@%${OPENAM_URL}%" \
        -e "s%@AGENT_URL@%${AGENT_URL}%" \
        ${AGENT_CONFIG_TEMPL} > ${AGENT_CONFIG}
}

agent_install_input
agent_install
