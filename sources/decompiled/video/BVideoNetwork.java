/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.ndriver.BNNetwork
 *  com.tridium.videoDriver.BIVideoNetwork
 *  javax.baja.license.LicenseException
 *  javax.baja.nre.annotations.NiagaraAction
 *  javax.baja.nre.annotations.NiagaraProperty
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.sys.Action
 *  javax.baja.sys.Property
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.util.IFuture
 */
package com.tridium.nvideo;

import com.tridium.ndriver.BNNetwork;
import com.tridium.videoDriver.BIVideoNetwork;
import javax.baja.license.LicenseException;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;

@NiagaraType
@NiagaraProperty(name="foxVideoStreamPreferred", type="boolean", defaultValue="false")
@NiagaraAction(name="ping", flags=4, override=true)
public abstract class BVideoNetwork
extends BNNetwork
implements BIVideoNetwork {
    public static final Property foxVideoStreamPreferred = BVideoNetwork.newProperty((int)0, (boolean)false, null);
    public static final Action ping = BVideoNetwork.newAction((int)4, null);
    public static final Type TYPE = Sys.loadType(BVideoNetwork.class);

    public boolean getFoxVideoStreamPreferred() {
        return this.getBoolean(foxVideoStreamPreferred);
    }

    public void setFoxVideoStreamPreferred(boolean v) {
        this.setBoolean(foxVideoStreamPreferred, v, null);
    }

    public Type getType() {
        return TYPE;
    }

    public Type getDeviceFolderType() {
        return null;
    }

    public Type getDeviceType() {
        return null;
    }

    public IFuture postAsync(Runnable r) {
        this.getAsync().post(r);
        return null;
    }

    public final void started() throws Exception {
        try {
            this.videoNetworkStarted();
        }
        catch (LicenseException fnle) {
            this.configFatal(fnle.toString());
        }
        super.started();
    }

    public void videoNetworkStarted() throws Exception {
    }
}

