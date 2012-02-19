/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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

package org.forgerock.openam.cli.saml2requestdecoder;



import com.sun.identity.shared.encode.Base64;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import org.w3c.dom.Document;

/**
 *
 * @author victor
 */
public class SAMLRequestDecoder {

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            return;
        }
        
        
        Params params = null;
        try {
            params = new Params(args);
        } catch (Exception ex) {
            System.err.println("\nThere was a problem with the parameters");
            printUsage();
            return;
        }
        
        String SAML2RequestEncoded = params.getData("");
        Boolean isUrlEncoded = params.getIsUrlEncoded("false");
        Boolean isItPOST = params.getIsPost("false");
        Boolean verbose = params.getVerbosity("false");
        
        if (verbose) {
               System.out.println("URL Encoded : " + isUrlEncoded + "\nPOSTed : "
                       + isItPOST + "\nVerbose: " + verbose);
        }
        
        if (isItPOST) {
            ByteArrayInputStream bis = null;
            try {
                byte[] raw = Base64.decode(SAML2RequestEncoded);
                if (raw != null) {
                    if (verbose) {
                       System.out.print("\nIt was Base 64 Decoded");
                    }
                    bis = new ByteArrayInputStream(raw);
                    Document doc = XMLUtils.toDOMDocument(bis, null);
                    if (doc != null) {
                       if (verbose) {
                          System.out.println("\nDocument: " + doc.toString() );
                       }
                       return;
                    } else {
                       System.err.println("\nDoc was null, could not parse");
                       return; 
                    }
                } else {
                   System.err.println("\nCould not decode B64");
                   return;
                }
            } catch (Exception e) {
                System.err.println("Error:" + e);
                return;
            }
        }
        
        String urldecoded = SAML2RequestEncoded;
        if (isUrlEncoded) {
           urldecoded = URLEncDec.decode(SAML2RequestEncoded);
        } 
        
        if (verbose) {
           System.out.println("\nURL Decoded=" + urldecoded);
        }
        
        String nonewlines = removeNewLineChars(urldecoded);
        
        if (verbose) {        
            System.out.println("\nNew lines removed=" + nonewlines);
        }
        
        byte[] input = Base64.decode(nonewlines);
        if (input==null || input.length==0) {
            System.err.println("\nBase 64 decoded result is null");
        }
        // Decompress the bytes
        Inflater inflater = new Inflater(true);
        inflater.setInput(input);
        int resultLen = 2048;

        byte[] result = new byte[resultLen];
        int resultLength = 0;
        try {
            resultLength = inflater.inflate(result);
        } catch (DataFormatException dfe) {
            System.err.println("\nCannot inflate SAMLRequest: ");
        }
        inflater.end();

        // Decode the bytes into a String
        String outputString = null;
        try {
            outputString = new
                    String(result, 0, resultLength, "UTF-8");
        } catch (UnsupportedEncodingException uee) {
            System.err.println("Error: Cannot convert byte array to string.");
        }

        System.out.println("\n\n SAML Request :\n\n"  + outputString + "\n");
        
    }

    public static String removeNewLineChars(String s) {
        String retString = null;
        if ((s != null) && (s.length() > 0) && (s.indexOf('\n') != -1)) {
            char[] chars = s.toCharArray();
            int len = chars.length;
            StringBuffer sb = new StringBuffer(len);
            for (int i = 0; i < len; i++) {
                char c = chars[i];
                if (c != '\n') {
                    sb.append(c);
                }
            }
            retString = sb.toString();
        } else {
            retString = s;
        }
        return retString;
    }


    public static void printUsage() {
        System.out.println("\nUsage: \n java -jar  SAMLRequestDecoder.jar -u "
                + "<true|false> -p <true|false> "
                + "-v <true|false> -d <YOUR_ENCODED_SAML_REQUEST>\n\n"
                + "Where: \n     -u : Is the dat URL Encoded \n"
                + "     -p : Is the data POSTED (Base 64 encoded)\n"
                + "     -d : Is your SAML 2.0 Encoded request\n"
                + "     -v : Verbose");
    }
}

