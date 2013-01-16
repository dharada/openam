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
