/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.baja.gx.BImage
 *  javax.baja.naming.BOrd
 *  javax.baja.naming.BOrdList
 *  javax.baja.naming.OrdTarget
 *  javax.baja.nav.BINavNode
 *  javax.baja.nre.annotations.Adapter
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.sys.BObject
 *  javax.baja.sys.Context
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.util.BConverter
 */
package com.tridium.kitpx;

import javax.baja.gx.BImage;
import javax.baja.naming.BOrd;
import javax.baja.naming.BOrdList;
import javax.baja.naming.OrdTarget;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.Adapter;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BConverter;

@NiagaraType(adapter=@Adapter(from="baja:Ord", to="gx:Image"))
public class BOrdToImage
extends BConverter {
    public static final Type TYPE = Sys.loadType(BOrdToImage.class);
    Object lock = new Object();
    BObject cacheFrom;
    BImage cacheImage;

    public Type getType() {
        return TYPE;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public BObject convert(BObject from, BObject to, Context cx) {
        Object object = this.lock;
        synchronized (object) {
            if (from == this.cacheFrom) {
                return this.cacheImage;
            }
            BImage image = this.toImage(from);
            if (cx instanceof OrdTarget) {
                OrdTarget target = (OrdTarget)cx;
                BOrd baseOrd = target.getOrd();
                BObject base = target.get();
                if (target.getComponent() != null) {
                    baseOrd = target.getComponent().getNavOrd();
                } else if (base instanceof BINavNode) {
                    baseOrd = ((BINavNode)base).getNavOrd();
                }
                image.setBaseOrd(baseOrd);
            }
            this.cacheFrom = from;
            this.cacheImage = image;
            return image;
        }
    }

    BImage toImage(BObject from) {
        if (from instanceof BOrd) {
            return BImage.make((BOrd)((BOrd)from));
        }
        if (from instanceof BOrdList) {
            return BImage.make((BOrdList)((BOrdList)from));
        }
        if (from instanceof BImage) {
            return (BImage)from;
        }
        return BImage.make((String)from.toString());
    }
}

