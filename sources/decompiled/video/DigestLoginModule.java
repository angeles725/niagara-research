/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.nre.auth.ScramServer
 */
package com.tridium.authn;

import com.tridium.authn.AbstractNiagaraLoginModule;
import com.tridium.authn.NiagaraFailedLoginException;
import com.tridium.authn.ScramServerCallback;
import com.tridium.nre.auth.ScramServer;
import java.util.logging.Level;
import javax.baja.authn.AuthenticationUtil;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

public class DigestLoginModule
extends AbstractNiagaraLoginModule {
    @Override
    public boolean doLogin() throws LoginException {
        FailedLoginException e;
        String username;
        ScramServer scramServer;
        if (this.callbackHandler == null) {
            throw new LoginException("CallbackHandler cannot be null for username and password login");
        }
        Callback[] callbacks = new Callback[]{new ScramServerCallback()};
        try {
            this.callbackHandler.handle(callbacks);
            scramServer = ((ScramServerCallback)callbacks[0]).getServer();
            username = ((ScramServerCallback)callbacks[0]).getUsername();
            if (scramServer == null || username == null) {
                return false;
            }
        }
        catch (UnsupportedCallbackException e2) {
            String msg = "Provided callback handler could not handle: " + e2.getCallback().toString();
            AuthenticationUtil.debug(Level.SEVERE, msg, e2);
            throw new LoginException(msg);
        }
        catch (Exception e3) {
            String msg = "Error handling callbacks:" + e3.toString();
            AuthenticationUtil.debug(Level.SEVERE, msg, e3);
            throw new LoginException(msg);
        }
        this.user = this.getUserService().getUser(username);
        if (this.user != null && scramServer.isAuthenticated()) {
            this.succeeded = true;
            return true;
        }
        if (this.user != null) {
            e = new NiagaraFailedLoginException(this.user.getName(), "Login failed: Invalid username or password.");
            this.user = null;
        } else {
            e = new FailedLoginException("Login failed: Invalid username or password.");
        }
        throw e;
    }
}

