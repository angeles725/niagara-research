/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.crypto.core.exchange.KeyExchange
 *  javax.baja.nre.annotations.AgentOn
 *  javax.baja.nre.annotations.NiagaraType
 */
package com.tridium.authn;

import com.tridium.authn.DigestLoginModule;
import com.tridium.authn.NiagaraLoginConfiguration;
import com.tridium.crypto.core.exchange.KeyExchange;
import java.util.HashMap;
import javax.baja.authn.BPasswordAuthenticationScheme;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

@NiagaraType(agent={@AgentOn(types={"baja:AuthenticationScheme"})})
public class BDigestAuthenticationScheme
extends BPasswordAuthenticationScheme {
    public static final Type TYPE = Sys.loadType(BDigestAuthenticationScheme.class);
    public static final int GET_USERNAME_PASSWORD = 0;
    public static final String SCHEME_NAME = "n4digest";
    private Configuration configuration = null;

    @Override
    public Type getType() {
        return TYPE;
    }

    @Override
    public String getSchemeName() {
        return SCHEME_NAME;
    }

    @Override
    public Configuration getLoginConfiguration() {
        if (this.configuration == null) {
            this.configuration = new NiagaraLoginConfiguration(DigestLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, new HashMap());
        }
        return this.configuration;
    }

    @Override
    public String getKeyExchangeMethodName() {
        return KeyExchange.getPreferredKeyExchangeMethods();
    }
}

