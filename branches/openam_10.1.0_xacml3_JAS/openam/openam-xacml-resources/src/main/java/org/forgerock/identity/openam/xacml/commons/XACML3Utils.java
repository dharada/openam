/**
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 ForgeRock AS. All Rights Reserved
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
package org.forgerock.identity.openam.xacml.commons;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ResourceBundle;

/**
 * XACML
 * Simple Static Method utility Class for XACML3 Processing.
 *
 * @author jeff.schenk@forgerock.com
 */
public class XACML3Utils {
    /**
     * Define our Static resource Bundle for our debugger.
     */
    private static Debug debug = Debug.getInstance("libSAML2"); // TODO Need to create additional Message Bundle for XACML.

    //  SAML2 Resource bundle
    public static final String BUNDLE_NAME = "libSAML2";
    // The resource bundle for SAML 2.0 implementation.
    public static ResourceBundle bundle = Locale.getInstallResourceBundle(BUNDLE_NAME);


    /**
     * Returns first Element with given local name in samlp name space inside
     * SOAP message.
     * @param messageBody XML Element.
     * @param localName local name of the Element to be returned.
     * @return first Element matching the local name.
     * @throws com.sun.identity.saml2.common.SAML2Exception if the Element could not be found or there is
     * SOAP Fault present.
     */
    public static Element getSamlpElement(
            Element messageBody, String localName) throws SAML2Exception {
        if (messageBody == null)
            { return null; }
        NodeList nlBody = messageBody.getChildNodes();

        int blength = nlBody.getLength();
        if (blength <= 0) {
            debug.error("XACML3Utils.getSamlpElement: empty body");
            throw new SAML2Exception(bundle.getString("missingBody"));
        }
        Element retElem = null;
        Node node = null;
        for (int i = 0; i < blength; i++) {
            node = (Node) nlBody.item(i);
            if(node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }
            String nlName = node.getLocalName();
            if (nlName == null) {
                nlName = node.getNodeName();
            }
            if (debug.messageEnabled()) {
                debug.message("SAML2Utils.getSamlpElement: node=" +
                        nlName + ", nsURI=" + node.getNamespaceURI());
            }
            if ((nlName != null) && (nlName.equals("Fault")) ) {
                throw new SAML2Exception(SAML2Utils.bundle.getString(
                        "soapFaultInSOAPResponse"));
            } else if ((nlName != null) && (nlName.equals(localName) &&
                    SAML2Constants.PROTOCOL_NAMESPACE.equals(
                            node.getNamespaceURI()))) {
                retElem = (Element) node;
                break;
            }
        }
        if (retElem == null) {
            throw new SAML2Exception(bundle.getString("elementNotFound") +
                    localName);
        }
        return retElem;
    }


}
