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
