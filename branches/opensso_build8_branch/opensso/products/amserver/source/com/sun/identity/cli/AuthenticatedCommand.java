/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: AuthenticatedCommand.java,v 1.8.6.1 2009/07/30 05:32:57 veiming Exp $
 *
 */

package com.sun.identity.cli;


import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.AccessController;
import java.text.MessageFormat;
import java.util.logging.Level;

/**
 * This is the base class for all commands that require a user to be 
 * authenticated in order to execute a command.
 */
public abstract class AuthenticatedCommand extends CLICommandBase {
    private String adminID;
    private String adminPassword;
    private SSOToken ssoToken;
    
    private static String amadminUUID;

    static {
        String adminUser = SystemProperties.get(
            "com.sun.identity.authentication.super.user");
        if (adminUser != null) {
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMIdentity adminUserId = new AMIdentity(adminToken, adminUser,
                IdType.USER, "/", null);
            amadminUUID = adminUserId.getUniversalId();
        }

    }

    /**
     * Authenticates the administrator. Dervived classes needs to
     * call this method from the dervived method,
     * <code>handleRequest(RequestContext rc)</code>.
     *
     * @param rc Request Context.
     * @throws CLIException if authentication fails.
     * @Override this method to get user name and passowrd.
     */
    @Override
    public void handleRequest(RequestContext rc)
        throws CLIException
    {
        super.handleRequest(rc);
        ssoToken = rc.getCLIRequest().getSSOToken();
        
        if (ssoToken == null) {
            adminID = getStringOptionValue(
                AccessManagerConstants.ARGUMENT_ADMIN_ID);
            adminPassword = getPassword();
        }
    }

    private String getPassword()
        throws CLIException
    {
        String fileName = getStringOptionValue(
            AccessManagerConstants.ARGUMENT_PASSWORD_FILE);
        String password = CLIUtil.getFileContent(getCommandManager(),
            fileName, true);
        validatePwdFilePermissions(fileName);
        return password;
    }

    private void validatePwdFilePermissions(String fileName)
        throws CLIException {
        if (System.getProperty("path.separator").equals(":")) {
            try {
                String[] parameter = {"/bin/ls", "-l", fileName};
                Process p = Runtime.getRuntime().exec(parameter);
                BufferedReader stdInput = new BufferedReader(new
                    InputStreamReader(p.getInputStream()));
                String s = stdInput.readLine();
                if (s != null) {
                    int idx = s.indexOf(" ");
                    if (idx != -1) {
                        String permission = s.substring(0, idx);
                        if (!permission.equals("-r--------")) {
                            String msg = getCommandManager().getResourceBundle()
                                .getString(
                                    "error-message-password-file-not-readonly");
                            Object[] param = {fileName};
                            throw new CLIException(MessageFormat.format(
                                msg, param), 
                                ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
                            
                        }
                    }
                }
            } catch (IOException e) {
                //ignore, this should not happen because we are able to 
                // read the file in getPassword method.
            }
        }
    }

    protected String getAdminPassword() {
        return adminPassword;
    }

    protected String getAdminID() {
        return adminID;
    }

    protected SSOToken getAdminSSOToken() {
        return ssoToken;
    }

    protected void ldapLogin()
        throws CLIException
    {
        if (ssoToken == null) {
            Authenticator auth = Authenticator.getInstance();
            String bindUser = getAdminID();
            ssoToken = auth.ldapLogin(getCommandManager(), bindUser,
                getAdminPassword());
        } else {
            try {
                SSOTokenManager mgr = SSOTokenManager.getInstance();
                mgr.validateToken(ssoToken);
            } catch (SSOException e) {
                throw new CLIException(e, ExitCodes.SESSION_EXPIRED);
            }
        }
    }

    protected void superAdminUserValidation()
        throws CLIException {
        if (ssoToken != null) {
            try {
                AMIdentity user = new AMIdentity(ssoToken);
                if (user.getUniversalId().equalsIgnoreCase(amadminUUID)) {
                    return;
                }
            } catch (SSOException ex) {
                debugError("AuthenticatedCommand.isSuperAdminUser", ex);
            } catch (IdRepoException ex) {
                debugError("AuthenticatedCommand.isSuperAdminUser", ex);
            }
        }
        throw new CLIException(
            getResourceString("error-message-no-privilege"),
            ExitCodes.REQUEST_CANNOT_BE_PROCESSED);
    }

    @Override
    protected void writeLog(
        int type,
        Level level,
        String msgid,
        String[] msgdata
    ) throws CLIException {
        CommandManager mgr = getCommandManager();
        LogWriter.log(mgr, type, level, msgid, msgdata,getAdminSSOToken());
    }
}
