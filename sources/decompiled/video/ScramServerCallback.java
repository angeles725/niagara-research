/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.nre.auth.ScramServer
 */
package com.tridium.authn;

import com.tridium.nre.auth.ScramServer;
import javax.security.auth.callback.Callback;

public final class ScramServerCallback
implements Callback {
    ScramServer server = null;
    String username = null;

    public void setServer(ScramServer server) {
        this.server = server;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public ScramServer getServer() {
        return this.server;
    }

    public String getUsername() {
        return this.username;
    }
}

