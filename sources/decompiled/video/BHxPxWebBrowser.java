/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.web.WebUtil
 *  com.tridium.workbench.web.browser.BWebBrowser
 *  javax.baja.naming.BOrd
 *  javax.baja.naming.OrdQuery
 *  javax.baja.nre.annotations.AgentOn
 *  javax.baja.nre.annotations.NiagaraSingleton
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.sys.Context
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.ui.BWidget
 */
package com.tridium.hx.px;

import com.tridium.web.WebUtil;
import com.tridium.workbench.web.browser.BWebBrowser;
import java.io.PrintWriter;
import javax.baja.hx.HxOp;
import javax.baja.hx.HxUtil;
import javax.baja.hx.px.BHxPxWidget;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BWidget;

@NiagaraType(agent={@AgentOn(types={"workbench:WebBrowser"}, requiredPermissions="r")})
@NiagaraSingleton
public final class BHxPxWebBrowser
extends BHxPxWidget {
    public static final BHxPxWebBrowser INSTANCE = new BHxPxWebBrowser();
    public static final Type TYPE = Sys.loadType(BHxPxWebBrowser.class);
    private static final BWidget[] NO_WIDGETS = new BWidget[0];

    @Override
    public Type getType() {
        return TYPE;
    }

    private BHxPxWebBrowser() {
    }

    @Override
    public void write(HxOp op) throws Exception {
        OrdQuery[] queries;
        PrintWriter out = op.getWriter();
        BWebBrowser browser = (BWebBrowser)op.get();
        BOrd ord = browser.getOrd();
        String uri = ord.toString();
        if (!(ord.isNull() || (queries = ord.parse()).length <= 0 || queries[0].getScheme().equals("http") || queries[0].getScheme().equals("https"))) {
            uri = WebUtil.toUri(null, (String)"/ord/", (BOrd)ord);
        }
        out.write("<iframe name='webBrowser.fullScreen' style='width: 100%; height: 100%;' frameborder='0' id='");
        out.write(op.scope("webBrowser"));
        out.write("' src='");
        out.write(HxUtil.encodeURLForHref(uri));
        out.write("'></iframe>");
    }

    @Override
    public BWidget[] getChildWidgets(BWidget widget, Context cx) {
        return NO_WIDGETS;
    }

    @Override
    public boolean isMouseEnabled(HxOp op) {
        return false;
    }
}

