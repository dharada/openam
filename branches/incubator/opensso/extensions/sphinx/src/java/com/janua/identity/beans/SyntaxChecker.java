/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) Janua 2011
 * Copyright (c) 2010 ForgeRock AS. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 */
package com.janua.identity.beans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author faime
 */
public class SyntaxChecker {

    /**
     * Check if a desired identifier match the given syntax rule
     * @param aIdentifier the identifier to be checked
     * @return true on success false otherwise
     */
    public boolean checkIdentifier(String aIdentifier) {
        boolean result = true;
        if (aIdentifier == null) {
            return false;
        }

        if (aIdentifier.length() < 4 || aIdentifier.length() > 30) {
            return false;
        }

        for (char c : aIdentifier.toCharArray()) {
            if (!Character.isLetterOrDigit(c)) {
                if (c != '.') {
                    result = false;
                    break;
                }
            }
        }
        return result;
    }

    /**
     * Check if the provided password respect the defined password policies
     * @param aPassword the password to be checked
     * @return true on success false otherwise
     */
    public boolean checkPassword(String aPassword) {
        boolean result = true;

        while (true) {
            if (aPassword.length() < 8) {
                result = false;
                break;
            } else {
                boolean upper = false;
                boolean lower = false;
                boolean number = false;
                for (char c : aPassword.toCharArray()) {
                    if (Character.isUpperCase(c)) {
                        upper = true;
                    } else if (Character.isLowerCase(c)) {
                        lower = true;
                    } else if (Character.isDigit(c)) {
                        number = true;
                    }
                }
                if (!lower && !upper) { // #DTSI-23 : soit lower soit upper mais l'un des deux...
                    // must contain at least one lowercase character
                    result = false;
                    break;
                } else if (!number) {
                    // must contain at least one number
                    result = false;
                    break;
                } else {
                    break;
                }
            }
        }

        return result;
    }

    /**
     * Check if the telephone syntax is correct
     * @param aTelephone the telephone number to be checked
     * @return true on success false otherwise
     */
    public boolean checkTelephone(String aTelephone) {
        boolean isValid = false;

        String expression = "^[+]?[0-9][0-9- ]*$";
        CharSequence inputStr = aTelephone;
        Pattern pattern = Pattern.compile(expression);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }
}
