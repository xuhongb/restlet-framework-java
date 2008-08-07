/**
 * Copyright 2005-2008 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of the following open
 * source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 (the "Licenses"). You can
 * select the license that you prefer but you may not use this file except in
 * compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.sun.com/cddl/cddl.html
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royaltee free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */
package org.restlet.test.jaxrs.services.tests;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.ApplicationConfig;

import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.test.jaxrs.services.resources.InjectionTestService;
import org.restlet.test.jaxrs.services.resources.InjectionTestService2;

/**
 * @author Stephan Koops
 * @see InjectionTestService
 * @see InjectionTestService2
 */
public class InjectionTest extends JaxRsTestCase {

    /**
     * @return
     */
    @Override
    protected ApplicationConfig getAppConfig() {
        final ApplicationConfig appConfig = new ApplicationConfig() {
            @Override
            public Set<Class<?>> getResourceClasses() {
                final Set<Class<?>> rrcs = new HashSet<Class<?>>();
                rrcs.add(getRootResourceClass());
                rrcs.add(InjectionTestService2.class);
                return rrcs;
            }
        };
        return appConfig;
    }

    @Override
    protected Class<?> getRootResourceClass() {
        return InjectionTestService.class;
    }

    public void testGet() {
        final Response response = get("?qp1=56");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
    }

    /** @see InjectionTestService2#get() */
    public void testGetWithIndex() throws IOException {
        Response response = get("two/56");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("56", response.getEntity().getText());

        response = get("two/97");
        sysOutEntityIfError(response);
        assertEquals(Status.SUCCESS_OK, response.getStatus());
        assertEquals("97", response.getEntity().getText());
    }
}