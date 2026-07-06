/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.ui.util.ValidationUtil
 *  com.tridium.workbench.web.browser.BWebWidget
 *  javax.baja.agent.AgentFilter
 *  javax.baja.agent.AgentInfo
 *  javax.baja.agent.AgentList
 *  javax.baja.agent.NoSuchAgentException
 *  javax.baja.nre.annotations.NiagaraSingleton
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.registry.TypeInfo
 *  javax.baja.sys.Context
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.ui.BBinding
 *  javax.baja.ui.BNullWidget
 *  javax.baja.ui.BWidget
 *  javax.baja.ui.px.BPxMedia
 *  javax.baja.util.LexiconModule
 *  javax.baja.web.BIWebProfile
 *  javax.baja.workbench.BWbPlugin
 *  javax.baja.workbench.view.BWbView
 */
package com.tridium.hx;

import com.tridium.hx.BHTML5HxProfile;
import com.tridium.hx.px.BHxPxWbView;
import com.tridium.ui.util.ValidationUtil;
import com.tridium.workbench.web.browser.BWebWidget;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.agent.NoSuchAgentException;
import javax.baja.hx.BHxView;
import javax.baja.hx.px.BHxPxWidget;
import javax.baja.hx.px.binding.BHxPxBinding;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BNullWidget;
import javax.baja.ui.BWidget;
import javax.baja.ui.px.BPxMedia;
import javax.baja.util.LexiconModule;
import javax.baja.web.BIWebProfile;
import javax.baja.workbench.BWbPlugin;
import javax.baja.workbench.view.BWbView;

@NiagaraType
@NiagaraSingleton
public class BHxPxMedia
extends BPxMedia {
    public static final BHxPxMedia INSTANCE = new BHxPxMedia();
    public static final Type TYPE = Sys.loadType(BHxPxMedia.class);
    private Type genericFE = null;
    private static final AgentFilter plugin = new PluginFilter();
    private static final AgentFilter hxPx = AgentFilter.is((Type)BHxPxWidget.TYPE);
    private static final AgentFilter hxPxBinding = AgentFilter.is((Type)BHxPxBinding.TYPE);
    private static final LexiconModule LEX = LexiconModule.make(BHxPxMedia.class);

    public Type getType() {
        return TYPE;
    }

    private BHxPxMedia() {
        try {
            this.genericFE = Sys.getType((String)"kitPx:GenericFieldEditor");
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    public boolean isWidgetSupported(TypeInfo type) {
        AgentList list;
        if (type.is(BNullWidget.TYPE)) {
            return true;
        }
        if (type.is(BWebWidget.TYPE)) {
            return true;
        }
        if (this.genericFE != null && type.is(this.genericFE)) {
            return true;
        }
        if (type.is(BWidget.TYPE) && !type.isAbstract()) {
            BHxPxWidget hxPxWidget;
            BWidget widget = null;
            try {
                widget = (BWidget)type.getInstance();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (widget != null && (hxPxWidget = BHxPxWidget.ux(widget, (BIWebProfile)BHTML5HxProfile.INSTANCE, null)) != null) {
                return true;
            }
        }
        return type.is(BWbPlugin.TYPE) ? (list = Sys.getRegistry().getAgents(type).filter(plugin)).size() > 0 : (list = Sys.getRegistry().getAgents(type).filter(hxPx)).size() > 0;
    }

    public boolean isBindingSupported(TypeInfo type) {
        return true;
    }

    public String validateBinding(BBinding binding, Context cx) {
        BHxPxBinding hxPxBinding;
        String customWarning;
        StringBuilder b = new StringBuilder();
        TypeInfo type = binding.getType().getTypeInfo();
        AgentList list = Sys.getRegistry().getAgents(type).filter(BHxPxMedia.hxPxBinding);
        if (list.size() > 0 && (customWarning = (hxPxBinding = (BHxPxBinding)list.getDefault().getInstance()).validateBinding(binding, cx)) != null) {
            if (b.length() > 0) {
                b.append('\n');
            }
            b.append(customWarning);
        }
        if (b.length() > 0) {
            return b.toString();
        }
        return null;
    }

    public String validateWidget(BWidget widget, Context cx) {
        BHxPxWidget hxPxWidget;
        String customWarning;
        StringBuilder b = new StringBuilder();
        String webWarnings = ValidationUtil.getWebWarnings((BWidget)widget, (Context)cx);
        if (webWarnings != null) {
            b.append(webWarnings);
        }
        TypeInfo type = widget.getType().getTypeInfo();
        AgentList list = Sys.getRegistry().getAgents(type).filter(hxPx);
        if (list.size() > 0 && (customWarning = (hxPxWidget = (BHxPxWidget)list.getDefault().getInstance()).validateWidget(widget, cx)) != null) {
            if (b.length() > 0) {
                b.append('\n');
            }
            b.append(customWarning);
        }
        if (b.length() > 0) {
            return b.toString();
        }
        return null;
    }

    public BWidget[] getChildWidgetsToValidate(BWidget widget, Context cx) {
        try {
            AgentList list;
            TypeInfo type = widget.getType().getTypeInfo();
            if (widget instanceof BWbView) {
                try {
                    BHxView view = BHxPxWbView.getAgent((BWbView)widget, null, cx);
                    if (view instanceof BHxPxWidget) {
                        BHxPxWidget hxPxWidget = (BHxPxWidget)view;
                        return hxPxWidget.getChildWidgets(widget, cx);
                    }
                }
                catch (NoSuchAgentException view) {
                    // empty catch block
                }
            }
            if ((list = Sys.getRegistry().getAgents(type).filter(hxPx)).size() > 0) {
                BHxPxWidget hxPxWidget = (BHxPxWidget)list.getDefault().getInstance();
                return hxPxWidget.getChildWidgets(widget, cx);
            }
        }
        catch (Exception e) {
            Logger.getLogger("hx").log(Level.SEVERE, "Cannot obtain HxPx child widgets for validation", e);
        }
        return widget.getChildWidgets();
    }

    public static class PluginFilter
    extends AgentFilter {
        public boolean include(AgentInfo agent) {
            TypeInfo type = agent.getAgentType();
            return type.is(BHxView.TYPE) && !type.is(BHxPxWidget.TYPE);
        }
    }
}

