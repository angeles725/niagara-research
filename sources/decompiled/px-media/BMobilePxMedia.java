/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  javax.baja.naming.BOrd
 *  javax.baja.nre.annotations.NiagaraSingleton
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.registry.TypeInfo
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.ui.px.BPxMedia
 *  javax.baja.util.BTypeSpec
 */
package com.tridium.mobile.px.ui;

import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.px.BPxMedia;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraSingleton
public final class BMobilePxMedia
extends BPxMedia {
    public static final BMobilePxMedia INSTANCE = new BMobilePxMedia();
    public static final Type TYPE = Sys.loadType(BMobilePxMedia.class);
    public static final BOrd mobilePxFile = BOrd.make((String)"file:!workbench/newfiles/MobilePxFile.px");
    private static final Type[] supportedTypes = new Type[]{BTypeSpec.make((String)"bajaui:RootContainer").getResolvedType(), BTypeSpec.make((String)"bajaui:ScrollPane").getResolvedType(), BTypeSpec.make((String)"bajaui:ScrollBar").getResolvedType(), BTypeSpec.make((String)"bajaui:CanvasPane").getResolvedType(), BTypeSpec.make((String)"mobile:BasicMobilePane").getResolvedType(), BTypeSpec.make((String)"mobile:MobileGridPane").getResolvedType(), BTypeSpec.make((String)"bajaui:Button").getResolvedType(), BTypeSpec.make((String)"bajaui:Label").getResolvedType(), BTypeSpec.make((String)"bajaui:Slider").getResolvedType(), BTypeSpec.make((String)"kitPx:GenericFieldEditor").getResolvedType(), BTypeSpec.make((String)"kitPx:SetPointFieldEditor").getResolvedType(), BTypeSpec.make((String)"bajaui:Picture").getResolvedType(), BTypeSpec.make((String)"bajaui:BoundTable").getResolvedType(), BTypeSpec.make((String)"kitPx:Bargraph").getResolvedType()};

    public Type getType() {
        return TYPE;
    }

    protected BMobilePxMedia() {
    }

    public boolean isWidgetSupported(TypeInfo info) {
        Type type = info.getTypeSpec().getResolvedType();
        for (int i = 0; i < supportedTypes.length; ++i) {
            if (!type.is(supportedTypes[i])) continue;
            return true;
        }
        return false;
    }

    public boolean isBindingSupported(TypeInfo type) {
        return true;
    }

    public BOrd getPxFileOrd() {
        return mobilePxFile;
    }
}

