/*
 * (C) Copyright Janua 2010 - Author : Frédéric Aime (faime@janua.fr)
 * This code has been provided only for informational purpose
 * Deliverables may not be generated from this code,
 * License details are available upon express demand to : contact@janua.fr
 *
 * You can studdy this code as far as you want, using and/or modifying it is
 * subject to licence terms available upon demand.
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
