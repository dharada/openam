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

public class Params {
    private String isUrlEncoded = null;
    private String isPost = null;
    private String isVerbose = null;
    private String data = null;

    public Params(String[] args) throws Exception {
        if (args.length == 0) {
            return;
        }
        for (int i = 0; i + 1 < args.length; i += 2) {
            if (args[i].equals("-u") == true) {
                isUrlEncoded = args[i+1];
            } else if (args[i].equals("-p") == true) {
                isPost = args[i+1];
            } else if (args[i].equals("-v") == true) {
                isVerbose = args[i+1];
            } else if (args[i].equals("-d") == true) {
                data = args[i+1];
            } else {
                System.err.println("\nInvalid argument: " + args[i] + ", ignored");
            }
        }
        
        if (args.length % 2 != 0) {
            System.err.println("\nSorry, there is an additional argument I "
                    + "do not understand: " + args[args.length - 1] + ","
                    + " ignored");
            throw new Exception("There is a problem with the arguments passed");
        }
    }

    public Boolean getIsUrlEncoded(String defVal) {
        if (isUrlEncoded != null) {
            return Boolean.valueOf(isUrlEncoded);
        } else {
            return Boolean.valueOf(defVal);
        }
    }

    public Boolean getIsPost(String defVal) {
        if (isPost != null) {
            return Boolean.valueOf(isPost);
        } else {
            return Boolean.valueOf(defVal);
        }
    }

    public String getData(String defVal) {
        if (data != null) {
            return data;
        } else {
            return defVal;
        }
    }
    
    public Boolean getVerbosity(String defVal) {
        if (isVerbose != null) {
            return Boolean.valueOf(isVerbose);
        } else {
            return Boolean.valueOf(defVal);
        }
    }
}
