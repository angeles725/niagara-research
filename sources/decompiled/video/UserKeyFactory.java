/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle
 *  com.tridium.nre.auth.ScramServer$IUserKeyFactory
 */
package com.tridium.authn;

import com.tridium.crypto.core.bundle.CryptographicAlgorithmBundle;
import com.tridium.nre.auth.ScramServer;
import javax.baja.authn.BAuthenticationScheme;
import javax.baja.security.BPasswordCache;
import javax.baja.security.BPbkdf2HmacSha256PasswordEncoder;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.user.BUserService;

public class UserKeyFactory
implements ScramServer.IUserKeyFactory {
    private final Type schemeType;

    public UserKeyFactory(Type schemeType) {
        this.schemeType = schemeType;
    }

    public String getUserKey(String username) {
        BPbkdf2HmacSha256PasswordEncoder encoder;
        BUserService service = (BUserService)Sys.getService(BUserService.TYPE);
        BUser user = service.getUser(username);
        boolean canLogin = true;
        if (user == null) {
            canLogin = false;
        } else {
            BAuthenticationScheme scheme = user.getAuthenticationScheme();
            if (scheme == null || !scheme.getType().is(this.schemeType)) {
                canLogin = false;
            }
        }
        if (!canLogin) {
            encoder = BPbkdf2HmacSha256PasswordEncoder.makeFake(username);
            return CryptographicAlgorithmBundle.extractData((String)encoder.getValue());
        }
        BPbkdf2HmacSha256PasswordEncoder.makeFake(username);
        encoder = (BPbkdf2HmacSha256PasswordEncoder)((BPasswordCache)user.getAuthenticator()).getPasswordEncoder();
        return CryptographicAlgorithmBundle.extractData((String)encoder.getValue());
    }
}

