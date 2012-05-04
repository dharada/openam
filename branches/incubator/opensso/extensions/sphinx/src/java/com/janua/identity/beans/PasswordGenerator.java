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

import java.util.Random;

/**
 * This class is used to generate a new password for a user
 * @author faime
 */
public class PasswordGenerator {
	private Random rgen = new Random();
	private byte decision, numValue;
	private char charValue;
        private int generatedPasswordLength;

        /**
         * create a new instance from property generated_password_length through AppConfig
         * @see AppConfig
         */
        public PasswordGenerator() {
            String sGeneratedPasswordLength = AppConfig.getProperty("generated_password_length");
            if (sGeneratedPasswordLength != null) {
                generatedPasswordLength = Integer.parseInt(sGeneratedPasswordLength);
            } else {
                generatedPasswordLength = 10;
            }
        }


        /**
         * Generates a new password
         * @return the new password
         */
        public String generatePassword(){
		StringBuilder sb = new StringBuilder(generatedPasswordLength);
		while(sb.length() < generatedPasswordLength){
			decision = (byte)rgen.nextInt(2);
			numValue = (byte)rgen.nextInt(10);
			charValue = (char)(rgen.nextInt(25) + 65);
			sb.append( (decision%2 == 0) ? ( charValue + "" ) : ( numValue + "") );
		}
		return sb.toString();
	}
}
