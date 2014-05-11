package com.flipkart.foxtrot.server.resources;

import com.flipkart.foxtrot.common.Document;
import com.flipkart.foxtrot.core.querystore.QueryStore;
import com.flipkart.foxtrot.core.querystore.QueryStoreException;
import com.sun.istack.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.List;

/**
 * User: Santanu Sinha (santanu.sinha@flipkart.com)
 * Date: 15/03/14
 * Time: 10:55 PM
 */
@Path("/foxtrot/v1/document/{table}")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DocumentResource {
    private static final Logger logger = LoggerFactory.getLogger(DocumentResource.class.getSimpleName());

    private final QueryStore queryStore;

    public DocumentResource(QueryStore queryStore) {
        this.queryStore = queryStore;
    }

    @POST
    public Response saveDocument(@PathParam("table") final String table, @Valid final Document document) {
        try {
            queryStore.save(table, document);
        } catch (QueryStoreException ex) {
            logger.error("Save error : ", ex);
            switch (ex.getErrorCode()) {
                case NO_SUCH_TABLE:
                case INVALID_REQUEST:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();
                case DOCUMENT_SAVE_ERROR:
                default:
                    return Response.serverError()
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();

            }
        }
        return Response.created(URI.create("/" + document.getId())).build();
    }

    @POST
    @Path("/bulk")
    public Response saveDocuments(@PathParam("table") final String table, @Valid final List<Document> document) {
        try {
            queryStore.save(table, document);
        } catch (QueryStoreException ex) {
            logger.error("Save error: ", ex);
            switch (ex.getErrorCode()) {
                case NO_SUCH_TABLE:
                case INVALID_REQUEST:
                    return Response.status(Response.Status.BAD_REQUEST)
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();
                case DOCUMENT_SAVE_ERROR:
                default:
                    return Response.serverError()
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();

            }
        }
        return Response.created(URI.create("/" + table)).build();
    }

    @GET
    @Path("/{id}")
    public Response getDocument(@PathParam("table") final String table, @PathParam("id") @NotNull final String id) {
        try {
            return Response.ok(queryStore.get(table, id)).build();
        } catch (QueryStoreException ex) {
            logger.error("Get error : ", ex);
            switch (ex.getErrorCode()) {
                case DOCUMENT_NOT_FOUND:
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();
                case DOCUMENT_GET_ERROR:
                default:
                    return Response.serverError()
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();

            }
        }
    }

    @GET
    public Response getDocuments(@PathParam("table") final String table, @QueryParam("id") @NotNull final List<String> ids) {
        try {
            return Response.ok(queryStore.get(table, ids)).build();
        } catch (QueryStoreException ex) {
            logger.error("Get error : ", ex);
            switch (ex.getErrorCode()) {
                case DOCUMENT_NOT_FOUND:
                    return Response.status(Response.Status.NOT_FOUND)
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();
                case DOCUMENT_GET_ERROR:
                default:
                    return Response.serverError()
                            .entity(Collections.singletonMap("error", ex.getErrorCode()))
                            .build();

            }
        }
    }
}
