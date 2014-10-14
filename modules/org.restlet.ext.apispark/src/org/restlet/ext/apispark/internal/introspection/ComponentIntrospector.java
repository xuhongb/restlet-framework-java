/**
 * Copyright 2005-2014 Restlet
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: Apache 2.0 or LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL
 * 1.0 (the "Licenses"). You can select the license that you prefer but you may
 * not use this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the Apache 2.0 license at
 * http://www.opensource.org/licenses/apache-2.0
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://restlet.com/products/restlet-framework
 * 
 * Restlet is a registered trademark of Restlet S.A.S.
 */

package org.restlet.ext.apispark.internal.introspection;

import org.restlet.Application;
import org.restlet.Component;
import org.restlet.Restlet;
import org.restlet.data.Protocol;
import org.restlet.data.Reference;
import org.restlet.ext.apispark.internal.model.Endpoint;
import org.restlet.ext.apispark.internal.utils.IntrospectionUtils;
import org.restlet.routing.*;

/**
 * Publish the documentation of a Restlet-based Application to the APISpark
 * console.
 * 
 * @author Thierry Boileau
 */
public class ComponentIntrospector extends IntrospectionUtils {

    /** Internal logger. */
//    protected static Logger LOGGER = Logger.getLogger(ComponentIntrospector.class
//            .getName());

    /**
     * Returns an instance of what must be a subclass of {@link org.restlet.Component}.
     * Returns null in case of errors.
     *
     * @param className
     *            The name of the component class.
     * @return An instance of what must be a subclass of {@link org.restlet.Component}.
     */
    public static Component getComponent(String className) {
        if (className == null) {
            return null;
        }

        try {
            Class<?> clazz = Class.forName(className);
            if (Component.class.isAssignableFrom(clazz)) {
                return (Component) clazz.getConstructor().newInstance();
            } else {
                throw new RuntimeException(className
                        + " does not seem to a valid subclass of "
                        + Component.class.getName() + " class.");
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Cannot locate the component class.", e);
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate the component class. " +
                    "Check that the component class has an empty constructor.", e);
        }
    }

    /**
     * Returns the endpoint to which the application is attached.
     *
     * @param virtualHost
     *            The virtual host to which this application may be attached.
     * @param application
     *            The application.
     * @return The endpoint.
     */
    public static Endpoint getEndpoint(VirtualHost virtualHost,
                                 Application application) {
        Endpoint result = null;

        for (Route route : virtualHost.getRoutes()) {
            if (route.getNext() != null) {
                Application app = getNextApplication(route.getNext());
                if (app != null
                        && application.getClass().equals(app.getClass())) {
                    String hostDomain = null;
                    if (virtualHost.getHostDomain() != null
                            && !".*".equals(virtualHost.getHostDomain())) {
                        if (virtualHost.getHostDomain().contains("|")) {
                            hostDomain = virtualHost.getHostDomain().split("|")[0];
                        } else {
                            hostDomain = virtualHost.getHostDomain();
                        }
                    }
                    if (hostDomain != null) {
                        Protocol scheme = null;
                        if (!".*".equals(virtualHost.getHostScheme())) {
                            scheme = Protocol.valueOf(virtualHost
                                    .getHostScheme());
                        }
                        if (scheme == null) {
                            scheme = Protocol.HTTP;
                        }
                        Reference ref = new Reference();
                        ref.setProtocol(scheme);
                        ref.setHostDomain(hostDomain);
                        if (route instanceof TemplateRoute) {
                            ref.addSegment(((TemplateRoute) route)
                                    .getTemplate().getPattern());
                        }
                        try {
                            ref.setHostPort(Integer.parseInt(virtualHost
                                    .getHostPort()));
                        } catch (Exception e) {
                            // Nothing
                        }
                        // Concatenate in order to get the endpoint
                        result = new Endpoint(ref.getHostDomain(),
                                ref.getHostPort(), ref.getSchemeProtocol()
                                .getSchemeName(), ref.getPath(), null);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Returns the next application available.
     *
     * @param current
     *            The current Restlet to inspect.
     * @return The first application available.
     */
    private static Application getNextApplication(Restlet current) {
        Application result = null;
        if (current instanceof Application) {
            result = (Application) current;
        } else if (current instanceof Filter) {
            result = getNextApplication(((Filter) current).getNext());
        } else if (current instanceof Router) {
            Router router = (Router) current;
            for (Route route : router.getRoutes()) {
                result = getNextApplication(route.getNext());
                if (result != null) {
                    break;
                }
            }
        }

        return result;
    }

}