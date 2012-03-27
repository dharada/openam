/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright © 2012 ForgeRock. All rights reserved.
 */

package org.forgerock.openam.oauth2.model.impl;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.restlet.ext.oauth2.model.RefreshToken;
import org.forgerock.restlet.ext.oauth2.model.SessionClient;
import org.forgerock.restlet.ext.oauth2.model.Token;

import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jonathan
 * Date: 26/3/12
 * Time: 2:37 PM
 * To change this template use File | Settings | File Templates.
 */
public class RefreshTokenImpl extends TokenImpl implements RefreshToken {

    private String parent;

    // TODO javadoc
    public RefreshTokenImpl(String id, String parent, String userID, SessionClient client, String realm, Set<String> scope, long expireTime) {
        super(id, userID, client, realm, scope, expireTime);
        this.parent = parent;
    }

    // TODO javadoc
    public RefreshTokenImpl(String id, Set<String> scope, long expireTime, Token token) {
        super(id, token.getUserID(), token.getClient(), token.getRealm(), scope, expireTime);
        this.parent = token.getToken();
    }

    public RefreshTokenImpl(JsonValue value) {
        super(value);
        this.parent = value.get("parent").asString();
    }

    // TODO javadoc
    public JsonValue asJson() {
        JsonValue value = super.asJson();
        value.put("type", "refresh_token");
        value.put("parent", parent);
        return value;
    }

    @Override
    public String getParentToken() {
        return parent;
    }
}
