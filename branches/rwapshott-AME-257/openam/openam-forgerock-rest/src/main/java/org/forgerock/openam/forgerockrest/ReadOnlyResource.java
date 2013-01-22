/*
 * Copyright 2013 ForgeRock, Inc.
 *
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
 * information: "Portions copyright [year] [name of copyright owner]".
 */
package org.forgerock.openam.forgerockrest;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.*;

/**
 * Represents a read only view of a resource.
 */
public abstract class ReadOnlyResource implements CollectionResourceProvider  {

    private NotSupportedException generateException(String type) {
        return new NotSupportedException(type + " are not supported for this Resource");
    }

    public void actionCollection(ServerContext ctx, ActionRequest request, ResultHandler<JsonValue> handler) {
        handler.handleError(generateException("Actions"));
    }

    public void actionInstance(ServerContext ctx, String resId, ActionRequest request,
                               ResultHandler<JsonValue> handler) {
        handler.handleError(generateException("Actions"));
    }

    public void createInstance(ServerContext ctx, CreateRequest request, ResultHandler<Resource> handler) {
        handler.handleError(generateException("Creates"));
    }

    public void deleteInstance(ServerContext ctx, String resId, DeleteRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(generateException("Deletes"));
    }

    public void patchInstance(ServerContext ctx, String resId, PatchRequest request, ResultHandler<Resource> handler) {
        handler.handleError(generateException("Patches"));
    }

    public void updateInstance(ServerContext ctx, String resId, UpdateRequest request,
                               ResultHandler<Resource> handler) {
        handler.handleError(generateException("Updates"));
    }
}
