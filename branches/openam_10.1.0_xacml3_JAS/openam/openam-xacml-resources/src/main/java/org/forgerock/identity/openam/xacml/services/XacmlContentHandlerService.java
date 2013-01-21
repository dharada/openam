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
package org.forgerock.identity.openam.xacml.services;

import com.sun.identity.saml.common.SAMLUtils;
import com.sun.identity.saml.xmlsig.KeyProvider;
import com.sun.identity.saml2.assertion.Assertion;
import com.sun.identity.saml2.assertion.AssertionFactory;
import com.sun.identity.saml2.assertion.EncryptedAssertion;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.XACMLAuthzDecisionQueryDescriptorElement;
import com.sun.identity.saml2.key.EncInfo;
import com.sun.identity.saml2.key.KeyUtil;
import com.sun.identity.saml2.logging.LogUtil;
import com.sun.identity.saml2.meta.SAML2MetaException;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.protocol.RequestAbstract;
import com.sun.identity.saml2.protocol.Response;
import com.sun.identity.saml2.soapbinding.RequestHandler;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.xacml.context.ContextFactory;
import org.forgerock.identity.openam.xacml.model.XACML3Constants;
import org.forgerock.identity.openam.xacml.model.XACMLRequestInformation;
import org.forgerock.identity.openam.xacml.resources.XacmlHomeResource;
import org.json.JSONException;
import org.w3c.dom.Element;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MimeHeaders;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

/**
 * XACML Resource Router
 * <p/>
 * Provides main end-point for all XACML requests, either XML or JSON based over HTTP/HTTPS REST based.
 * This code was originally used from the @see com.sun.identity.saml2.soapbinding.QueryHandlerServlet.
 *
 * @author Jeff.Schenk@forgerock.com
 */
public class XacmlContentHandlerService extends HttpServlet implements XACML3Constants {
   /**
    * Define our Static resource Bundle for our debugger.
    */
    private static Debug debug = Debug.getInstance("libSAML2"); // TODO Need to create additional Message Bundle for XACML.

    /**
     * Defined and established Handlers.
     */
    private static HashMap handlers = new HashMap();

    /**
     * Preserve our Servlet Context PlaceHolder,
     * for referencing Artifacts.
     */
    private static ServletContext servletCtx;

    /**
     * Initialize our Servlet
     *
     * @param config
     * @throws ServletException
     */
    public void init(ServletConfig config) throws ServletException {
        servletCtx = config.getServletContext();
        debug.error("Initialization of XACML Content Resource Router, Server Information: "+servletCtx.getServerInfo());
        super.init(config);
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws java.io.IOException
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:doGet";
        debug.error(classMethod + " processing context path:[" + request.getContextPath() + "]");

        // ************************************************************
        // Authorized?

        // ************************************************************
        // Accept a pre-determined entry point for the Home Documents
        try {
            if (XacmlHomeResource.getHome(request, response)) {
                // Request was satisfied.
                return;
            }
        } catch(JSONException je) {
            debug.error("JSON processing Exception: "+je.getMessage(),je);
        }
        // ***************************************************************
        // returning here, indicates we still need to process the request
        // now found the requested relation from the context.
        XACMLRequestInformation xacmlRequestInformation = this.parseRequestInformation(request);

        // Determine based upon the contentType on how to consume and respond to the incoming Request.


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:status
         ￼
         Normative Source
         ￼
         GET on the home location MUST return status code 200
         ￼
         Target
         ￼
         Response to GET request on the home location
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:body
         ￼
         Normative Source
         ￼
         GET on the home location MUST return a home document
         ￼
         Target
         ￼
         Response to GET request on the home location
         ￼
         Predicate
         ￼
         The HTTP body in the [response] follows the home document schema
         [HomeDocument]
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:home:pdp
         ￼
         Normative Source
         ￼
         The XACML entry point representation SHOULD contain a link to the PDP
         ￼
         Target
         ￼
         Response to GET request on the home location
         ￼
         Predicate
         ￼
         The home document in the [response] body contains a resource with link relation http://docs.oasis-open.org/ns/xacml/relation/pdp and a valid URL
         ￼
         Prescription Level
         ￼
         mandatory
         */


        // Check our query string.
        String queryParam = request.getQueryString();
        if ((queryParam != null) && (queryParam.equalsIgnoreCase("wsdl"))) {
            // Nothing here yet.....
        } else {
            // Formulate the Home Document.
            StringBuilder sb = new StringBuilder();
            sb.append("<resources xmlns=\042http://ietf.org/ns/home-documents\042\n");
            sb.append("xmlns:atom=\042http://www.w3.org/2005/Atom\042>\n");
            sb.append("<resource rel=\042http://docs.oasis-open.org/ns/xacml/relation/pdp\042>");
            sb.append("<atom:link href=\042/authorization/pdp\042/>");  // TODO Static?
            sb.append("</resource>");
            sb.append(" </resources>");

            // TODO Determine if there are any other Get Request Types we need to deal with,
            // TODO otherwise pass along.

            try {
                response.setContentType("application/xml");
                response.setCharacterEncoding("UTF-8");
                PrintWriter out = response.getWriter();
                out.write(sb.toString());
                out.flush();
                out.close();
            } catch (IOException ioe) {
                // Debug and return null
            }
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @throws ServletException    if the request could not be
     *                             handled.
     * @throws java.io.IOException if an input or output error occurs.
     */
    @Override
    public void doPost(
            HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:doPost";
        debug.error(classMethod + " processing context path:[" + request.getContextPath() + "]");

        // Authorized?


        XACMLRequestInformation xacmlRequestInformation = this.parseRequestInformation(request);

        // POST operations to PDP.

        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:status
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return status code 200
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:body
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return a valid XACML response in the body
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP body in the [response] is a valid XACML response
         ￼
         Prescription Level
         ￼
         mandatory
         */


        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:xacml:invalid
         ￼
         Normative Source
         ￼
         POST on the PDP with an invalid XACML request MUST return status code 400 (Bad Request)
         ￼
         Target
         ￼
         Response to POST request on the PDP location with invalid XACML request in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 400
         ￼
         Prescription Level
         ￼
         mandatory
         */

        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:status
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return status code 200
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request wrapped in a
         xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 200
         ￼
         Prescription Level
         ￼
         optional
         */


        /**
         * Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:body
         ￼
         Normative Source
         ￼
         POST on the PDP with a valid XACML request MUST return a valid XACML response in the body
         ￼
         Target
         ￼
         Response to POST request on the PDP location with valid XACML request wrapped in a
         xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP body in the [response] is a valid XACML response wrapped in a
         samlp:Response
         ￼
         Prescription Level
         ￼
         optional
         */


        /**
         * ￼
         Id
         ￼
         urn:oasis:names:tc:xacml:3.0:profile:rest:assertion:pdp:saml:invalid
         ￼
         Normative Source
         ￼
         POST on the PDP with an invalid XACML request MUST return status code 400 (Bad Request)
         ￼
         Target
         ￼
         Response to POST request on the PDP location with invalid XACML request wrapped in a xacml-samlp:XACMLAuthzDecisionQuery in the body
         ￼
         Predicate
         ￼
         The HTTP status code in the [response] is 400
         ￼
         Prescription Level
         ￼
         optional
         */


        processPDPRequest(request, response);
    }

    /**
     * Processes the POST Request for the PDP.
     *
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    private void processPDPRequest(HttpServletRequest request,
                                   HttpServletResponse response)
            throws ServletException, IOException {
        String classMethod = "XacmlContentHandlerService:processPostRequest";
        try {

            XACMLRequestInformation xacmlRequestInformation = this.parseRequestInformation(request);

            // Get all the headers from the HTTP request
            MimeHeaders headers = SAML2Utils.getHeaders(request);

            // Get the body of the HTTP request
            InputStream is = request.getInputStream();

            // TODO We have no SOAP Envelope for XACML to eliminate any overhead.

            //create SOAPMessage
            SOAPMessage soapMsg = SAML2Utils.mf.createMessage(headers, is);
            Element soapBody = SAML2Utils.getSOAPBody(soapMsg);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "SOAPMessage received.:"
                        + XMLUtils.print(soapBody));
            }

            SOAPMessage reply = null;

            reply = onMessage(soapMsg, request, response, xacmlRequestInformation.getRealm(), xacmlRequestInformation.getPdpEntityID());

            if (reply != null) {
                if (reply.saveRequired()) {
                    reply.saveChanges();
                }
                response.setStatus(HttpServletResponse.SC_OK);
                SAML2Utils.putHeaders(reply.getMimeHeaders(), response);
            } else {
                // Error
                debug.error(classMethod + "SOAPMessage is null");
                response.setStatus(HttpServletResponse.SC_NO_CONTENT);
                reply = SAML2Utils.createSOAPFault(
                        SAML2Constants.SERVER_FAULT, "invalidQuery", null);
            }





            // Write out the message on the response stream
            OutputStream os = response.getOutputStream();
            reply.writeTo(os);
            os.flush();




        } catch (SAML2Exception ex) {
            debug.error(classMethod, ex);
            SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "failedToProcessRequest", ex.getMessage());
        } catch (SOAPException soap) {
            debug.error(classMethod, soap);
            SAMLUtils.sendError(request, response,
                    HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "failedToProcessRequest", soap.getMessage());
        }
    }

    /**
     * Process the incoming SOAP message containing the Query Request and
     * generates outgoing SOAP message containing the Query Response.
     *
     * @param soapMsg     incoming SOAP message.
     * @param request     HTTP servlet request.
     * @param response    HTTP servlet response.
     * @param realm       realm of the Policy Decision Point (PDP).
     * @param pdpEntityID Entity ID of the Policy Decision Point (PDP).
     * @return SOAP message containing the outgoing Response.
     */
    public SOAPMessage onMessage(
            SOAPMessage soapMsg,
            HttpServletRequest request,
            HttpServletResponse response,
            String realm,
            String pdpEntityID) throws SOAPException {

        String classMethod = "XacmlContentHandlerService:onMessage:";
        SOAPMessage soapMessage = null;
        String pepEntityID = null;
        try {
            Element soapBody = SAML2Utils.getSOAPBody(soapMsg);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "SOAPMessage recd. :"
                        + XMLUtils.print(soapBody));
            }
            Element reqAbs = SAML2Utils.getSamlpElement(soapMsg,
                    REQUEST_ABSTRACT);

            Response samlResponse =
                    processSAMLRequest(realm, pdpEntityID, reqAbs, request, soapMsg);
            soapMessage = SAML2Utils.createSOAPMessage(
                    samlResponse.toXMLString(true, true), false);




        } catch (SAML2Exception se) {
            debug.error(classMethod + "XACML Response Error SOAP Fault", se);
            soapMessage = SAML2Utils.createSOAPFault(
                    SAML2Constants.SERVER_FAULT, "invalidQuery", se.getMessage());
        }
        return soapMessage;
    }

    /**
     * Signs an <code>Assertion</code>.
     *
     * @param realm       the realm name of the Policy Decision Point (PDP).
     * @param pdpEntityID the entity id of the policy decision provider.
     * @param assertion   the <code>Assertion</code> to be signed.
     * @throws <code>SAML2Exception</code> it there is an error signing
     *                                     the assertion.
     */
    static void signAssertion(String realm, String pdpEntityID,
                              Assertion assertion) throws SAML2Exception {
        String classMethod = "XacmlContentHandlerService.signAssertion: ";

        // Don't load the KeyProvider object in static block as it can
        // cause issues when doing a container shutdown/restart.
        KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
        if (keyProvider == null) {
            debug.error(classMethod +
                    "Unable to get a key provider instance.");
            throw new SAML2Exception("nullKeyProvider");
        }
        String pdpSignCertAlias = SAML2Utils.getAttributeValueFromXACMLConfig(
                realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                SAML2Constants.SIGNING_CERT_ALIAS);
        if (pdpSignCertAlias == null) {
            debug.error(classMethod +
                    "Unable to get the hosted PDP signing certificate alias.");
            String[] data = {realm, pdpEntityID};
            LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
            throw new SAML2Exception("missingSigningCertAlias");
        }
        assertion.sign(keyProvider.getPrivateKey(pdpSignCertAlias),
                keyProvider.getX509Certificate(pdpSignCertAlias));
    }

    /**
     * Returns the SAMLv2 <code>Response</code> received in response to
     * the Request.
     *
     * @param realm       the realm of the entity.
     * @param pdpEntityID entity identifier of the Policy Decision Point.
     * @param reqAbs      the Document Element object.
     * @param request     the <code>HttpServletRequest</code> object.
     * @param soapMsg     the <code>SOAPMessage</code> object
     * @return the <code>Response</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request.
     */
    Response processSAMLRequest(String realm, String pdpEntityID, Element reqAbs,
                                HttpServletRequest request, SOAPMessage soapMsg)
            throws SAML2Exception {
        String classMethod = "XacmlContentHandlerService:processSAMLRequest";
        Response samlResponse = null;
        if (reqAbs != null) {
            String xsiType = reqAbs.getAttribute(XSI_TYPE_ATTR);
            if (debug.messageEnabled()) {
                debug.message(classMethod + "xsi type is : " + xsiType);
            }
            if (xsiType != null && xsiType.indexOf(XACML_AUTHZ_QUERY) != -1) {
                RequestAbstract samlRequest =
                        ContextFactory.getInstance()
                                .createXACMLAuthzDecisionQuery(reqAbs);
                String requestStr = samlRequest.toXMLString(true, true);
                String[] data = {requestStr, pdpEntityID};
                LogUtil.access(Level.FINE, LogUtil.REQUEST_MESSAGE, data);

                Issuer issuer = samlRequest.getIssuer();
                String pepEntityID = null;
                if (issuer != null) {
                    pepEntityID = issuer.getValue().trim();
                }
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Issuer is:" + pepEntityID);
                }
                boolean isTrusted = false;
                try {
                    isTrusted = SAML2Utils.getSAML2MetaManager().
                            isTrustedXACMLProvider(realm, pdpEntityID, pepEntityID,
                                    SAML2Constants.PDP_ROLE);
                } catch (SAML2MetaException sme) {
                    debug.error("Error retreiving meta", sme);
                }
                if (!isTrusted) {
                    if (debug.messageEnabled()) {
                        debug.message(classMethod +
                                "Issuer in Request is not valid." + pepEntityID);
                    }
                    String[] args = {realm, pepEntityID, pdpEntityID};
                    LogUtil.error(Level.INFO,
                            LogUtil.INVALID_ISSUER_IN_PEP_REQUEST,
                            args);
                    throw new SAML2Exception("invalidIssuerInRequest");
                }
                samlResponse =
                        processXACMLResponse(realm, pdpEntityID, samlRequest, request,
                                soapMsg);

            }
        }
        return samlResponse;
    }

    /**
     * Returns the received Response to the Requester.
     * Validates the message signature if signed and invokes the
     * Request Handler to pass the request for further processing.
     *
     * @param realm       realm of the entity.
     * @param pdpEntityID entity identifier of Policy Decision Point (PDP).
     * @param samlRequest the <code>RequestAbstract</code> object.
     * @param request     the <code>HttpServletRequest</code> object.
     * @param soapMsg     the <code>SOAPMessage</code> object.
     * @throws <code>SAML2Exception</code> if there is an error processing
     *                                     the request and returning a  response.
     */
    Response processXACMLResponse(String realm, String pdpEntityID,
                                  RequestAbstract samlRequest, HttpServletRequest request,
                                  SOAPMessage soapMsg) throws SAML2Exception {

        String classMethod = "XacmlContentHandlerService:processXACMLResponse";
        Response samlResponse = null;
        String path = request.getPathInfo();
        String key = path.substring(path.indexOf(METAALIAS_KEY) + 10);
        String pepEntityID = samlRequest.getIssuer().getValue();
        if (debug.messageEnabled()) {
            debug.message(classMethod + "SOAPMessage KEY . :" + key);
            debug.message(classMethod + "pepEntityID is :" + pepEntityID);
        }
        //Retreive metadata
        boolean pdpWantAuthzQuerySigned =
                SAML2Utils.getWantXACMLAuthzDecisionQuerySigned(realm,
                        pdpEntityID, SAML2Constants.PDP_ROLE);

        if (debug.messageEnabled()) {
            debug.message(classMethod + "PDP wantAuthzQuerySigned:" +
                    pdpWantAuthzQuerySigned);
        }
        if (pdpWantAuthzQuerySigned) {
            if (samlRequest.isSigned()) {
                XACMLAuthzDecisionQueryDescriptorElement pep =
                        SAML2Utils.getSAML2MetaManager().
                                getPolicyEnforcementPointDescriptor(
                                        realm, pepEntityID);
                X509Certificate cert =
                        KeyUtil.getPEPVerificationCert(pep, pepEntityID);
                if (cert == null ||
                        !samlRequest.isSignatureValid(cert)) {
                    // error
                    debug.error(classMethod + "Invalid signature in message");
                    throw new SAML2Exception("invalidQuerySignature");

                } else {
                    debug.message(classMethod + "Valid signature found");
                }
            } else {
                debug.error("Request not signed");
                throw new SAML2Exception("nullSig");
            }
        }

        //getRequestHandlerClass
        RequestHandler handler =
                (RequestHandler) XacmlContentHandlerService.handlers.get(key);  // TODO -- THis was referencing handlers in the SOAPBindingService class.
        if (handler != null) {
            if (debug.messageEnabled()) {
                debug.message(classMethod + "Found handler");
            }

            samlResponse = handler.handleQuery(pdpEntityID, pepEntityID,
                    samlRequest, soapMsg);
            // set response attributes
            samlResponse.setID(SAML2Utils.generateID());
            samlResponse.setVersion(SAML2Constants.VERSION_2_0);
            samlResponse.setIssueInstant(new Date());
            Issuer issuer = AssertionFactory.getInstance().createIssuer();
            issuer.setValue(pdpEntityID);
            samlResponse.setIssuer(issuer);
            // end set Response Attributes

            //set Assertion attributes
            List assertionList = samlResponse.getAssertion();
            Assertion assertion = (Assertion) assertionList.get(0);

            assertion.setID(SAML2Utils.generateID());
            assertion.setVersion(SAML2Constants.VERSION_2_0);
            assertion.setIssueInstant(new Date());
            assertion.setIssuer(issuer);
            // end assertion set attributes

            // check if assertion needs to be encrypted,signed.
            String wantAssertionEncrypted =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                            realm, SAML2Constants.PEP_ROLE,
                            pepEntityID,
                            SAML2Constants.WANT_ASSERTION_ENCRYPTED);


            XACMLAuthzDecisionQueryDescriptorElement
                    pepDescriptor = SAML2Utils.
                    getSAML2MetaManager().
                    getPolicyEnforcementPointDescriptor(realm,
                            pepEntityID);

            EncInfo encInfo = null;
            boolean wantAssertionSigned = pepDescriptor.isWantAssertionsSigned();

            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        " wantAssertionSigned :" + wantAssertionSigned);
            }
            if (wantAssertionSigned) {
                signAssertion(realm, pdpEntityID, assertion);
            }

            if (wantAssertionEncrypted != null
                    && wantAssertionEncrypted.equalsIgnoreCase
                    (SAML2Constants.TRUE)) {
                encInfo = KeyUtil.getPEPEncInfo(pepDescriptor, pepEntityID);

                // encrypt the Assertion
                EncryptedAssertion encryptedAssertion =
                        assertion.encrypt(
                                encInfo.getWrappingKey(),
                                encInfo.getDataEncAlgorithm(),
                                encInfo.getDataEncStrength(),
                                pepEntityID);
                if (encryptedAssertion == null) {
                    debug.error(classMethod + "Assertion encryption failed.");
                    throw new SAML2Exception("FailedToEncryptAssertion");
                }
                assertionList = new ArrayList();
                assertionList.add(encryptedAssertion);
                samlResponse.setEncryptedAssertion(assertionList);
                //reset Assertion list
                samlResponse.setAssertion(new ArrayList());
                if (debug.messageEnabled()) {
                    debug.message(classMethod + "Assertion encrypted.");
                }
            } else {
                List assertionsList = new ArrayList();
                assertionsList.add(assertion);
                samlResponse.setAssertion(assertionsList);
            }
            signResponse(samlResponse, realm, pepEntityID, pdpEntityID);

        } else {
            // error -  missing request handler.
            debug.error(classMethod + "RequestHandler not found");
            throw new SAML2Exception("missingRequestHandler");
        }
        return samlResponse;
    }

    /**
     * Signs the <code>Response</code>.
     *
     * @param response    the <code>Response<code> object.
     * @param realm       the realm of the entity.
     * @param pepEntityID Policy Enforcement Point Entity Identitifer.
     * @param pdpEntityID Policy Decision Point Entity Identifier.
     * @throws <code>SAML2Exception</code> if there is an exception.
     */
    static void signResponse(Response response,
                             String realm, String pepEntityID,
                             String pdpEntityID)
            throws SAML2Exception {
        String classMethod = "signResponse : ";
        String attrName = "wantXACMLAuthzDecisionResponseSigned";
        String wantResponseSigned =
                SAML2Utils.getAttributeValueFromXACMLConfig(realm,
                        SAML2Constants.PEP_ROLE, pepEntityID, attrName);

        if (wantResponseSigned == null ||
                wantResponseSigned.equalsIgnoreCase("false")) {
            if (debug.messageEnabled()) {
                debug.message(classMethod +
                        "Response doesn't need to be signed.");
            }
        } else {
            String pdpSignCertAlias =
                    SAML2Utils.getAttributeValueFromXACMLConfig(
                            realm, SAML2Constants.PDP_ROLE, pdpEntityID,
                            SAML2Constants.SIGNING_CERT_ALIAS);
            if (pdpSignCertAlias == null) {
                debug.error(classMethod + "PDP certificate alias is null.");
                String[] data = {realm, pdpEntityID};
                LogUtil.error(Level.INFO, LogUtil.NULL_PDP_SIGN_CERT_ALIAS, data);
                throw new SAML2Exception("missingSigningCertAlias");
            }

            if (debug.messageEnabled()) {
                debug.message(classMethod + "realm is : " + realm);
                debug.message(classMethod + "pepEntityID is :" + pepEntityID);
                debug.message(classMethod + "pdpEntityID : " + pdpEntityID);
                debug.message(classMethod + "wantResponseSigned" +
                        wantResponseSigned);
                debug.message(classMethod + "Cert Alias:" + pdpSignCertAlias);
            }
            // Don't load the KeyProvider object in static block as it can
            // cause issues when doing a container shutdown/restart.
            KeyProvider keyProvider = KeyUtil.getKeyProviderInstance();
            if (keyProvider == null) {
                debug.error(classMethod +
                        "Unable to get a key provider instance.");
                throw new SAML2Exception("nullKeyProvider");
            }
            PrivateKey signingKey = keyProvider.getPrivateKey(pdpSignCertAlias);
            X509Certificate signingCert =
                    keyProvider.getX509Certificate(pdpSignCertAlias);

            if (signingKey != null) {
                response.sign(signingKey, signingCert);
            } else {
                debug.error("Incorrect configuration for Signing Certificate.");
                throw new SAML2Exception("metaDataError");
            }
        }
    }


    /**
     * Provide common Entry point Method for Parsing Initial Requests
     * to obtain information on how to process and route the request.
     *
     * @param request
     * @return XACMLRequestInformation - Object returned with Parsed Request Information.
     * @throws ServletException
     */
    private final XACMLRequestInformation parseRequestInformation(HttpServletRequest request) throws ServletException {
        final String classMethod = "XacmlContentHandlerService:parseRequestInformation: ";
        try {
            // handle DOS attack
            SAMLUtils.checkHTTPContentLength(request);
            // Get PDP entity ID
            String requestURI = request.getRequestURI();
            String queryMetaAlias =
                    SAML2MetaUtils.getMetaAliasByUri(requestURI);

            String pdpEntityID =
                    SAML2Utils.getSAML2MetaManager().getEntityByMetaAlias(
                            queryMetaAlias);
            String realm = SAML2MetaUtils.getRealmByMetaAlias(queryMetaAlias);



            // Return with newly created POJO from parsing initial request.
            return new XACMLRequestInformation(requestURI, queryMetaAlias, pdpEntityID, realm);
        } catch (SAML2MetaException s2me) {
            debug.error("XACML MetaException: " + s2me.getMessage(), s2me);
            // TODO
            return null;
        }
    }

}

