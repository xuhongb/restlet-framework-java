/*
 * Copyright 2005-2008 Noelios Consulting.
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the "License"). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the license at
 * http://www.opensource.org/licenses/cddl1.txt See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and
 * include the License file at http://www.opensource.org/licenses/cddl1.txt If
 * applicable, add the following below this CDDL HEADER, with the fields
 * enclosed by brackets "[]" replaced with your own identifying information:
 * Portions Copyright [yyyy] [name of copyright owner]
 */

package com.noelios.restlet.http;

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.restlet.Context;
import org.restlet.data.CookieSetting;
import org.restlet.data.Dimension;
import org.restlet.data.Encoding;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.util.DateUtils;
import org.restlet.util.Series;

import com.noelios.restlet.authentication.AuthenticationUtils;
import com.noelios.restlet.util.CookieUtils;

/**
 * Converter of low-level HTTP server calls into high-level uniform calls.
 * 
 * @author Jerome Louvel (contact@noelios.com)
 */
public class HttpServerConverter extends HttpConverter {
    /**
     * Copies the entity headers from the {@link Representation} to the
     * {@link Series}.
     * 
     * @param entity
     *                The {@link Representation} to copy the headers from.
     * @param responseHeaders
     *                The {@link Series} to copie the headers to.
     */
    public static void addEntityHeaders(Representation entity,
            Series<Parameter> responseHeaders) {
        if (entity == null) {
            responseHeaders.add(HttpConstants.HEADER_CONTENT_LENGTH, "0");
        } else {
            if (entity.getExpirationDate() != null) {
                responseHeaders.add(HttpConstants.HEADER_EXPIRES, HttpCall
                        .formatDate(entity.getExpirationDate(), false));
            }

            if (!entity.getEncodings().isEmpty()) {
                StringBuilder value = new StringBuilder();
                for (Encoding encoding : entity.getEncodings()) {
                    if (!encoding.equals(Encoding.IDENTITY)) {
                        if (value.length() > 0)
                            value.append(", ");
                        value.append(encoding.getName());
                    }
                    responseHeaders.add(HttpConstants.HEADER_CONTENT_ENCODING,
                            value.toString());
                }
            }

            if (!entity.getLanguages().isEmpty()) {
                StringBuilder value = new StringBuilder();
                for (int i = 0; i < entity.getLanguages().size(); i++) {
                    if (i > 0)
                        value.append(", ");
                    value.append(entity.getLanguages().get(i).getName());
                }
                responseHeaders.add(HttpConstants.HEADER_CONTENT_LANGUAGE,
                        value.toString());
            }

            if (entity.getMediaType() != null) {
                StringBuilder contentType = new StringBuilder(entity
                        .getMediaType().getName());

                if (entity.getCharacterSet() != null) {
                    // Specify the character set parameter
                    contentType.append("; charset=").append(
                            entity.getCharacterSet().getName());
                }

                responseHeaders.add(HttpConstants.HEADER_CONTENT_TYPE,
                        contentType.toString());
            }

            if (entity.getModificationDate() != null) {
                responseHeaders
                        .add(HttpConstants.HEADER_LAST_MODIFIED,
                                HttpCall.formatDate(entity
                                        .getModificationDate(), false));
            }

            if (entity.getTag() != null) {
                responseHeaders.add(HttpConstants.HEADER_ETAG, entity.getTag()
                        .format());
            }

            if (entity.getSize() != Representation.UNKNOWN_SIZE) {
                responseHeaders.add(HttpConstants.HEADER_CONTENT_LENGTH, Long
                        .toString(entity.getSize()));
            }

            if (entity.getIdentifier() != null) {
                responseHeaders.add(HttpConstants.HEADER_CONTENT_LOCATION,
                        entity.getIdentifier().toString());
            }

            if (entity.isDownloadable() && (entity.getDownloadName() != null)) {
                responseHeaders.add(HttpConstants.HEADER_CONTENT_DISPOSITION,
                        HttpServerCall.formatContentDisposition(entity
                                .getDownloadName()));
            }
        }
    }

    /**
     * Copies the headers from the {@link Response} to the given {@link Series}.
     * 
     * @param response
     * @param responseHeaders
     * @throws IllegalArgumentException
     */
    public static void addResponseHeaders(Response response,
            Series<Parameter> responseHeaders) throws IllegalArgumentException {
        if (response.getStatus().equals(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED)
                || Method.OPTIONS.equals(response.getRequest().getMethod())) {
            // Format the "Allow" header
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (Method method : response.getAllowedMethods()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }

                sb.append(method.getName());
            }

            responseHeaders.add(HttpConstants.HEADER_ALLOW, sb.toString());
        }

        // Add the date
        responseHeaders.add(HttpConstants.HEADER_DATE, DateUtils.format(
                new Date(), DateUtils.FORMAT_RFC_1123.get(0)));

        // Add the cookie settings
        List<CookieSetting> cookies = response.getCookieSettings();
        for (int i = 0; i < cookies.size(); i++) {
            responseHeaders.add(HttpConstants.HEADER_SET_COOKIE, CookieUtils
                    .format(cookies.get(i)));
        }

        // Set the location URI (for redirections or creations)
        if (response.getLocationRef() != null) {
            responseHeaders.add(HttpConstants.HEADER_LOCATION, response
                    .getLocationRef().toString());
        }

        // Set the security data
        if (response.getChallengeRequest() != null) {
            responseHeaders.add(HttpConstants.HEADER_WWW_AUTHENTICATE,
                    AuthenticationUtils.format(response.getChallengeRequest()));
        }

        // Send the Vary header only to none-MSIE user agents as MSIE seems
        // to support partially and badly this header (cf issue 261).
        if (!(response.getRequest().getClientInfo().getAgent() != null && response
                .getRequest().getClientInfo().getAgent().contains("MSIE"))) {
            // Add the Vary header if content negotiation was used
            Set<Dimension> dimensions = response.getDimensions();
            String vary = createVaryHeader(dimensions);
            if (vary != null)
                responseHeaders.add(HttpConstants.HEADER_VARY, vary);
        }
    }

    /**
     * Creates a vary header from the given dimensions.
     * 
     * @param dimensions
     *                The dimensions to copy to the response.
     * @return Returns a vary header.
     */
    public static String createVaryHeader(Collection<Dimension> dimensions) {
        String vary = null;
        if (dimensions != null && !dimensions.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;

            if (dimensions.contains(Dimension.CLIENT_ADDRESS)
                    || dimensions.contains(Dimension.TIME)
                    || dimensions.contains(Dimension.UNSPECIFIED)) {
                // From an HTTP point of view the representations can
                // vary in unspecified ways
                vary = "*";
            } else {
                for (Dimension dim : dimensions) {
                    if (first) {
                        first = false;
                    } else {
                        sb.append(", ");
                    }

                    if (dim == Dimension.CHARACTER_SET) {
                        sb.append(HttpConstants.HEADER_ACCEPT_CHARSET);
                    } else if (dim == Dimension.CLIENT_AGENT) {
                        sb.append(HttpConstants.HEADER_USER_AGENT);
                    } else if (dim == Dimension.ENCODING) {
                        sb.append(HttpConstants.HEADER_ACCEPT_ENCODING);
                    } else if (dim == Dimension.LANGUAGE) {
                        sb.append(HttpConstants.HEADER_ACCEPT_LANGUAGE);
                    } else if (dim == Dimension.MEDIA_TYPE) {
                        sb.append(HttpConstants.HEADER_ACCEPT);
                    } else if (dim == Dimension.AUTHORIZATION) {
                        sb.append(HttpConstants.HEADER_AUTHORIZATION);
                    }
                }
                vary = sb.toString();
            }
        }
        return vary;
    }

    /**
     * Constructor.
     * 
     * @param context
     *                The client context.
     */
    public HttpServerConverter(Context context) {
        super(context);
    }

    /**
     * Adds the entity headers for the handled uniform call.
     * 
     * @param response
     *                The response returned.
     */
    @SuppressWarnings("unchecked")
    protected void addEntityHeaders(HttpResponse response) {
        Series<Parameter> responseHeaders = response.getHttpCall()
                .getResponseHeaders();
        Representation entity = response.getEntity();
        addEntityHeaders(entity, responseHeaders);
    }

    /**
     * Adds the response headers for the handled uniform call.
     * 
     * @param response
     *                The response returned.
     */
    @SuppressWarnings("unchecked")
    protected void addResponseHeaders(HttpResponse response) {
        // Add all the necessary response headers
        Series<Parameter> responseHeaders = response.getHttpCall()
                .getResponseHeaders();
        try {
            addResponseHeaders(response, responseHeaders);

            // Add user-defined extension headers
            Series<Parameter> additionalHeaders = (Series<Parameter>) response
                    .getAttributes().get(HttpConstants.ATTRIBUTE_HEADERS);
            addAdditionalHeaders(responseHeaders, additionalHeaders);

            // Set the server name again
            response.getHttpCall().getResponseHeaders().add(
                    HttpConstants.HEADER_SERVER,
                    response.getServerInfo().getAgent());

            // Set the status code in the response
            if (response.getStatus() != null) {
                response.getHttpCall().setStatusCode(
                        response.getStatus().getCode());
                response.getHttpCall().setReasonPhrase(
                        response.getStatus().getDescription());
            }
        } catch (Exception e) {
            getLogger().log(Level.INFO,
                    "Exception intercepted while adding the response headers",
                    e);
            response.getHttpCall().setStatusCode(
                    Status.SERVER_ERROR_INTERNAL.getCode());
            response.getHttpCall().setReasonPhrase(
                    Status.SERVER_ERROR_INTERNAL.getDescription());
        }
    }

    /**
     * Commits the changes to a handled uniform call back into the original HTTP
     * call. The default implementation first invokes the "addResponseHeaders"
     * then asks the "htppCall" to send the response back to the client.
     * 
     * @param response
     *                The high-level response.
     */
    public void commit(HttpResponse response) {
        try {
            if (response.getRequest().getMethod().equals(Method.HEAD)) {
                addEntityHeaders(response);
                response.setEntity(null);
            } else if (response.getStatus().equals(Status.SUCCESS_NO_CONTENT)) {
                addEntityHeaders(response);
                if (response.getEntity() != null) {
                    getLogger()
                            .fine(
                                    "Responses with a 204 (No content) status generally don't have an entity. Only adding entity headers for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);
                }
            } else if (response.getStatus()
                    .equals(Status.SUCCESS_RESET_CONTENT)) {
                if (response.getEntity() != null) {
                    getLogger()
                            .warning(
                                    "Responses with a 205 (Reset content) status can't have an entity. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);
                }
            } else if (response.getStatus().equals(
                    Status.SUCCESS_PARTIAL_CONTENT)) {
                if (response.getEntity() != null) {
                    getLogger()
                            .warning(
                                    "Responses with a 206 (Partial content) status aren't supported yet. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);

                }
            } else if (response.getStatus().equals(
                    Status.REDIRECTION_NOT_MODIFIED)) {
                addEntityHeaders(response);
                if (response.getEntity() != null) {
                    getLogger()
                            .warning(
                                    "Responses with a 304 (Not modified) status can't have an entity. Only adding entity headers for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);
                }
            } else if (response.getStatus().isInformational()) {
                if (response.getEntity() != null) {
                    getLogger()
                            .warning(
                                    "Responses with an informational (1xx) status can't have an entity. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);
                }
            } else {
                addEntityHeaders(response);
                if ((response.getEntity() != null)
                        && !response.getEntity().isAvailable()) {
                    // An entity was returned but isn't really available
                    getLogger()
                            .warning(
                                    "A response with an unavailable entity was returned. Ignoring the entity for resource \""
                                            + response.getRequest()
                                                    .getResourceRef() + ".");
                    response.setEntity(null);
                }
            }

            // Add the response headers
            addResponseHeaders(response);

            // Send the response to the client
            response.getHttpCall().sendResponse(response);
            response.getHttpCall().complete();
        } catch (Exception e) {
            getLogger().log(Level.INFO, "Exception intercepted", e);
            response.getHttpCall().setStatusCode(
                    Status.SERVER_ERROR_INTERNAL.getCode());
            response.getHttpCall().setReasonPhrase(
                    "An unexpected exception occured");
        }
    }

    /**
     * Converts a low-level HTTP call into a high-level uniform request.
     * 
     * @param httpCall
     *                The low-level HTTP call.
     * @return A new high-level uniform request.
     */
    public HttpRequest toRequest(HttpServerCall httpCall) {
        HttpRequest result = new HttpRequest(getContext(), httpCall);
        result.getAttributes().put(HttpConstants.ATTRIBUTE_HEADERS,
                httpCall.getRequestHeaders());

        if (httpCall.getVersion() != null) {
            result.getAttributes().put(HttpConstants.ATTRIBUTE_VERSION,
                    httpCall.getVersion());
        }

        if (httpCall.isConfidential()) {
            List<Certificate> clientCertificates = httpCall
                    .getSslClientCertificates();
            if (clientCertificates != null) {
                result.getAttributes().put(
                        HttpConstants.ATTRIBUTE_HTTPS_CLIENT_CERTIFICATES,
                        clientCertificates);
            }

            String cipherSuite = httpCall.getSslCipherSuite();
            if (cipherSuite != null) {
                result.getAttributes()
                        .put(HttpConstants.ATTRIBUTE_HTTPS_CIPHER_SUITE,
                                cipherSuite);
            }

            Integer keySize = httpCall.getSslKeySize();
            if (keySize != null) {
                result.getAttributes().put(
                        HttpConstants.ATTRIBUTE_HTTPS_KEY_SIZE, keySize);
            }
        }

        return result;
    }
}
