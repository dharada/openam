/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2011 ForgeRock AS. All rights reserved.
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
package org.forgerock.openam.wss.sts;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wss.trust.RequestSecurityToken;
import com.sun.identity.wss.trust.RequestSecurityTokenResponse;
import com.sun.xml.bind.AnyTypeAdapter;
import com.sun.xml.ws.security.trust.sts.BaseSTSImpl;
import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.ws.Action;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * This class uses JAX-WS annotations to expose it's methods as a web service, 
 * providing STS functionality. 
 * @author jonathan
 */
//@XmlSeeAlso({
//    STSMessageType.class
//})
@WebService(name = "SecurityTokenService", targetNamespace = "http://openam.forgerock.org/ns/sts")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@XmlJavaTypeAdapter(AnyTypeAdapter.class)
@XmlSeeAlso({
//    WSTrust13ObjectFactory.class
    com.sun.xml.ws.policy.impl.bindings.ObjectFactory.class
})
public class STSImpl extends BaseSTSImpl {
    
    public final static String STS_NAMESPACE = "http://openam.forgerock.org/ns/sts";
    public final static String STS_PORT_NAME = "SecurityTokenServicePort";
    public final static String STS_SERVICE_NAME = "STSImplService";
    private static final String LOG_PREFIX = "STSContextListener: ";
    public static Debug debug = Debug.getInstance("WebServicesSecurity");

    @Resource
    protected WebServiceContext context;    
    
    @WebMethod(operationName = "Issue", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
    @WebResult(name = "RequestSecurityTokenResponse", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "response")
    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue", output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/IssueFinal")
    public RequestSecurityTokenResponse issue(
            @WebParam(name = "RequestSecurityToken", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "request") RequestSecurityToken request) {
        System.out.println("JONATHAN: " + request.toString());
        
        
        // So - what do we have here?
        
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

//    @WebMethod(operationName = "Renew", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Renew")
//    @WebResult(name = "RequestSecurityTokenResponse", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "response")
//    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Renew", output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/RenewFinal")
//    public RequestSecurityTokenResponse renew(
//            @WebParam(name = "RequestSecurityToken", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "request") RequestSecurityToken request) {
//        System.out.println("JONATHAN: " + request.toString());
//        throw new UnsupportedOperationException("Not supported yet.");
//
//    }
//
//    @WebMethod(operationName = "Cancel", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Cancel")
//    @WebResult(name = "RequestSecurityTokenResponse", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "response")
//    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Cancel", output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/CancelFinal")
//    public RequestSecurityTokenResponse cancel(
//            @WebParam(name = "RequestSecurityToken", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "request") RequestSecurityToken request) {
//        System.out.println("JONATHAN: " + request.toString());
//        throw new UnsupportedOperationException("Not supported yet.");
//
//    }
//
//    @WebMethod(operationName = "Validate", action = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate")
//    @WebResult(name = "RequestSecurityTokenResponse", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "response")
//    @Action(input = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Validate", output = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RSTR/ValidateFinal")
//    public RequestSecurityTokenResponse validate(
//            @WebParam(name = "RequestSecurityToken", targetNamespace = "http://docs.oasis-open.org/ws-sx/ws-trust/200512", partName = "request") RequestSecurityToken request) {
//        System.out.println("JONATHAN: " + request.toString());
//        throw new UnsupportedOperationException("Not supported yet.");
//
//    }
    
    @Override
    protected MessageContext getMessageContext() {
        MessageContext msgCtx = context.getMessageContext();
        return msgCtx;
    }
}
