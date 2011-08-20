/**
 * Copyright 2005-2009 Noelios Technologies.
 * 
 * The contents of this file are subject to the terms of one of the following
 * open source licenses: LGPL 3.0 or LGPL 2.1 or CDDL 1.0 or EPL 1.0 (the
 * "Licenses"). You can select the license that you prefer but you may not use
 * this file except in compliance with one of these Licenses.
 * 
 * You can obtain a copy of the LGPL 3.0 license at
 * http://www.opensource.org/licenses/lgpl-3.0.html
 * 
 * You can obtain a copy of the LGPL 2.1 license at
 * http://www.opensource.org/licenses/lgpl-2.1.php
 * 
 * You can obtain a copy of the CDDL 1.0 license at
 * http://www.opensource.org/licenses/cddl1.php
 * 
 * You can obtain a copy of the EPL 1.0 license at
 * http://www.opensource.org/licenses/eclipse-1.0.php
 * 
 * See the Licenses for the specific language governing permissions and
 * limitations under the Licenses.
 * 
 * Alternatively, you can obtain a royalty free commercial license with less
 * limitations, transferable or non-transferable, directly at
 * http://www.noelios.com/products/restlet-engine
 * 
 * Restlet is a registered trademark of Noelios Technologies.
 */

package org.restlet.ext.jxta;

import net.jxta.peergroup.PeerGroup;
import net.jxta.pipe.PipeService;
import net.jxta.protocol.PipeAdvertisement;

import org.restlet.Server;
import org.restlet.engine.http.StreamServerHelper;

/**
 * Base JXTA connector.
 * 
 * @author Jerome Louvel
 */
public abstract class JxtaServerHelper extends StreamServerHelper {

    /** The JXTA network handler. */
    private volatile NetworkHandler networkHandler;

    /** The JXTA peer group. */
    private volatile PeerGroup peerGroup;

    /** The JXTA pipe advertisement. */
    private volatile PipeAdvertisement pipeAdvertisement;

    /**
     * Constructor.
     * 
     * @param server
     *            The parent server connector.
     */
    public JxtaServerHelper(Server server) {
        super(server);
    }

    /**
     * Returns the JXTA connection name. Defaults to "restlet".
     * 
     * @return The JXTA connection name.
     */
    public String getConnectionName() {
        return getHelpedParameters().getFirstValue("connectionName", "restlet");
    }

    /**
     * Returns the JXTA network handler.
     * 
     * @return The JXTA network handler.
     */
    public NetworkHandler getNetworkHandler() {
        return this.networkHandler;
    }

    /**
     * Returns the JXTA peer group.
     * 
     * @return The JXTA peer group.
     */
    public PeerGroup getPeerGroup() {
        return this.peerGroup;
    }

    /**
     * Returns the JXTA pipe advertisement.
     * 
     * @return The JXTA pipe advertisement.
     */
    public PipeAdvertisement getPipeAdvertisement() {
        return this.pipeAdvertisement;
    }

    @Override
    public void start() throws Exception {
        // Start the network handler
        this.networkHandler = new NetworkHandler();
        getNetworkHandler().start();

        // Initialize the JXTA context
        this.peerGroup = getNetworkHandler().getNetwork().getNetPeerGroup();
        this.pipeAdvertisement = PipeUtility.createPipeAdvertisement(
                getConnectionName(), PipeService.UnicastType, this.peerGroup,
                PipeUtility.createPipeID(this.peerGroup));

        // Continue standard start
        super.start();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        getNetworkHandler().stop();
    }

}