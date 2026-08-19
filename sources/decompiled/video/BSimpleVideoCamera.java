/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.videoDriver.BIVideoNetwork
 *  com.tridium.videoDriver.camera.BIVideoCamera
 *  com.tridium.videoDriver.dvr.BIVideoDvr
 *  com.tridium.videoDriver.event.BIVideoEventProvider
 *  javax.baja.license.LicenseException
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.sys.BAbsTime
 *  javax.baja.sys.BComplex
 *  javax.baja.sys.BIcon
 *  javax.baja.sys.Context
 *  javax.baja.sys.Property
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 */
package com.tridium.nvideo.camera;

import com.tridium.nvideo.BVideoDevice;
import com.tridium.videoDriver.BIVideoNetwork;
import com.tridium.videoDriver.camera.BIVideoCamera;
import com.tridium.videoDriver.dvr.BIVideoDvr;
import com.tridium.videoDriver.event.BIVideoEventProvider;
import javax.baja.license.LicenseException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BIcon;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BSimpleVideoCamera
extends BVideoDevice
implements BIVideoCamera {
    public static final Type TYPE = Sys.loadType(BSimpleVideoCamera.class);
    private static final BIcon icon = BIcon.make((String)"module://videoDriver/icons/camera.png");

    @Override
    public Type getType() {
        return TYPE;
    }

    public final void started() throws Exception {
        try {
            Sys.getLicenseManager().checkFeature("tridium", "videoDriver");
            this.videoCameraStarted();
        }
        catch (LicenseException fnle) {
            this.configFatal(fnle.toString());
        }
        super.started();
    }

    public void changed(Property prop, Context cx) {
        super.changed(prop, cx);
        if (prop.equals(status) && this instanceof BIVideoEventProvider) {
            ((BIVideoEventProvider)this).getEvents().updateStatus();
        }
    }

    public void videoCameraStarted() throws Exception {
    }

    public String getCameraDescription() {
        return this.toString();
    }

    public BIVideoDvr getDvr() {
        BComplex parent;
        for (parent = this.getParent(); parent != null && !(parent instanceof BIVideoDvr); parent = parent.getParent()) {
        }
        return (BIVideoDvr)parent;
    }

    public BIVideoNetwork getVideoNetwork() {
        BComplex parent;
        for (parent = this.getParent(); parent != null && !(parent instanceof BIVideoNetwork); parent = parent.getParent()) {
        }
        return (BIVideoNetwork)parent;
    }

    public abstract BAbsTime getCameraTime();

    public BIcon getIcon() {
        return icon;
    }
}

