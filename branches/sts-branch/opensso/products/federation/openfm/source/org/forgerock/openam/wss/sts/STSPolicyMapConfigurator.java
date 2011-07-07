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

import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicyMapKey;
import com.sun.xml.ws.policy.PolicySubject;
import com.sun.xml.ws.policy.jaxws.spi.PolicyMapConfigurator;
import com.sun.xml.ws.policy.sourcemodel.PolicyModelTranslator;
import com.sun.xml.ws.policy.sourcemodel.PolicySourceModel;
import com.sun.xml.ws.policy.subject.WsdlBindingSubject;
import java.util.ArrayList;
import java.util.Collection;
import javax.xml.namespace.QName;

/**
 *
 * @author jonathan
 */
public class STSPolicyMapConfigurator implements PolicyMapConfigurator {

    private static PolicySourceModel policySourceModel;
    private static PolicySourceModel inputPolicySourceModel;
    private static PolicySourceModel outputPolicySourceModel;

    public static void setPolicySourceModelForNextEndpoint(PolicySourceModel policySourceModel, PolicySourceModel inputPolicySourceModel, PolicySourceModel outputPolicySourceModel) {
        STSPolicyMapConfigurator.policySourceModel = policySourceModel;
        STSPolicyMapConfigurator.inputPolicySourceModel = inputPolicySourceModel;
        STSPolicyMapConfigurator.outputPolicySourceModel = outputPolicySourceModel;
        
        System.out.println("JONATHAN: setPolicySourceModel... " + policySourceModel);
        
    }

    public synchronized Collection<PolicySubject> update(PolicyMap pm, SEIModel seim, WSBinding wsb) throws PolicyException {

        System.out.println("JONATHAN: called update in STSPolicyMapConfigurator");
        System.out.println("Binding: " + wsb.toString());

        if (policySourceModel == null || inputPolicySourceModel == null || outputPolicySourceModel == null) {
            return new ArrayList<PolicySubject>();
        }
        
        Policy p = PolicyModelTranslator.getTranslator().translate(policySourceModel);
        Policy pInput = PolicyModelTranslator.getTranslator().translate(inputPolicySourceModel);
        Policy pOutput = PolicyModelTranslator.getTranslator().translate(outputPolicySourceModel);
        
//        PolicySubject ps = new PolicySubject("JonathanTestSubject", p);
        PolicySubject ps = new PolicySubject(WsdlBindingSubject.createBindingSubject(new QName(STSImpl.STS_NAMESPACE, "SecurityTokenServicePortBinding")), p);
//        PolicySubject ps2 = new PolicySubject(WsdlBindingSubject.createBindingOperationSubject(new QName(STSImpl.STS_NAMESPACE, STSImpl.STS_SERVICE_NAME), new QName(STSImpl.STS_NAMESPACE, "Issue")), p);
//        PolicySubject ps3 = new PolicySubject(WsdlBindingSubject.createBindingMessageSubject(new QName(STSImpl.STS_NAMESPACE, STSImpl.STS_SERVICE_NAME), new QName(STSImpl.STS_NAMESPACE, "Issue"), new QName(STSImpl.STS_NAMESPACE, "Issue"), WsdlBindingSubject.WsdlMessageType.INPUT), pInput);
//        PolicySubject ps4 = new PolicySubject(WsdlBindingSubject.createBindingMessageSubject(new QName(STSImpl.STS_NAMESPACE, STSImpl.STS_SERVICE_NAME), new QName(STSImpl.STS_NAMESPACE, "Issue"), new QName(STSImpl.STS_NAMESPACE, "IssueResponse"), WsdlBindingSubject.WsdlMessageType.OUTPUT), pOutput);
//        PolicySubject ps5 = new PolicySubject(WsdlBindingSubject.createBindingSubject(new QName(STSImpl.STS_NAMESPACE, STSImpl.STS_PORT_NAME)), p);
        
        ArrayList<PolicySubject> subjectList = new ArrayList<PolicySubject>();
        subjectList.add(ps);
//        subjectList.add(ps2);
//        subjectList.add(ps3);
//        subjectList.add(ps4);
//        subjectList.add(ps5);
        
        return subjectList;

    }
}
