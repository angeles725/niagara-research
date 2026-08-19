/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.tridium.json.JSONArray
 *  com.tridium.json.JSONObject
 *  com.tridium.json.JSONTokener
 *  com.tridium.json.JSONUtil
 *  com.tridium.ux.NiagaraEnv
 *  com.tridium.ux.NiagaraEnv$EnvType
 *  com.tridium.ux.UxUtil
 *  com.tridium.ux.WbWebWidgetServlet
 *  com.tridium.ux.WbWebWidgetServlet$WebWidgetInitInfo
 *  com.tridium.web.BICollectionSupport
 *  com.tridium.web.RequireJsUtil
 *  com.tridium.workbench.web.browser.BWebBrowser
 *  com.tridium.workbench.web.browser.BWebWidget
 *  javax.baja.agent.AgentFilter
 *  javax.baja.agent.AgentInfo
 *  javax.baja.gx.BSize
 *  javax.baja.io.HtmlWriter
 *  javax.baja.naming.BOrd
 *  javax.baja.nre.annotations.AgentOn
 *  javax.baja.nre.annotations.NiagaraSingleton
 *  javax.baja.nre.annotations.NiagaraType
 *  javax.baja.registry.TypeInfo
 *  javax.baja.sys.BAbsTime
 *  javax.baja.sys.BDynamicEnum
 *  javax.baja.sys.Context
 *  javax.baja.sys.Property
 *  javax.baja.sys.Sys
 *  javax.baja.sys.Type
 *  javax.baja.ui.BBinding
 *  javax.baja.ui.BWidget
 *  javax.baja.ui.util.WebProperty
 *  javax.baja.web.BIFormFactorMax
 *  javax.baja.web.WebOp
 *  javax.baja.web.js.BIJavaScript
 *  javax.baja.web.js.BIWebResource
 *  javax.baja.web.js.BIWebResource$DependencyGraph
 *  javax.baja.workbench.view.BWbView
 *  javax.baja.workbench.view.BWbViewBinding
 */
package com.tridium.hx;

import com.tridium.hx.BHxContainerJsBuild;
import com.tridium.hx.util.BenchmarkCommand;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONTokener;
import com.tridium.json.JSONUtil;
import com.tridium.ux.NiagaraEnv;
import com.tridium.ux.UxUtil;
import com.tridium.ux.WbWebWidgetServlet;
import com.tridium.web.BICollectionSupport;
import com.tridium.web.RequireJsUtil;
import com.tridium.workbench.web.browser.BWebBrowser;
import com.tridium.workbench.web.browser.BWebWidget;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.gx.BSize;
import javax.baja.hx.HxOp;
import javax.baja.hx.HxUtil;
import javax.baja.hx.px.BHxPxWidget;
import javax.baja.hx.px.MouseEventCommand;
import javax.baja.io.HtmlWriter;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.AgentOn;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.ui.BBinding;
import javax.baja.ui.BWidget;
import javax.baja.ui.util.WebProperty;
import javax.baja.web.BIFormFactorMax;
import javax.baja.web.WebOp;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.BIWebResource;
import javax.baja.workbench.view.BWbView;
import javax.baja.workbench.view.BWbViewBinding;

@NiagaraType(agent={@AgentOn(types={"workbench:WebWidget"}, requiredPermissions="r")})
@NiagaraSingleton
public final class BHxWebWidget
extends BHxPxWidget {
    public static final BHxWebWidget INSTANCE = new BHxWebWidget();
    public static final Type TYPE = Sys.loadType(BHxWebWidget.class);
    private static final String scopedId = "bajaux";
    private static final Pattern queryPattern = Pattern.compile("\\?(.+)$");
    public static final BOrd bajauxContainerCss = BOrd.make((String)"module://bajaux/rc/container/container.css");
    public static final BOrd hxContainerCss = BOrd.make((String)"module://hx/rc/container/container.css");
    public static final BOrd coreThemeCss = BOrd.make((String)"module://web/rc/theme/theme.css");
    private static final String updateJs = "require(['jquery'],function($){var jq=$(document.getElementById('%s')),func;if(jq.length){func=jq.data('hxBajauxUpdate');if(typeof func==='function'){func(%s%s);}}});";
    private static final BWidget[] NO_WIDGETS = new BWidget[0];
    private static String PROCESS_SYNC = "application/x-niagara-hx-bux-sync-props";
    private static String PROCESS_PROP = "application/x-niagara-hx-bux-prop-change";
    private static String PROCESS_META = "application/x-niagara-hx-bux-meta-change";

    @Override
    public Type getType() {
        return TYPE;
    }

    private BHxWebWidget() {
    }

    public static BWebWidget getWebWidget(HxOp op) {
        if (op.get() instanceof BWebWidget) {
            return (BWebWidget)op.get();
        }
        if (op.get() instanceof BWidget) {
            BWidget widget = (BWidget)op.get();
            AgentInfo info = widget.getAgents().filter(AgentFilter.and((AgentFilter)AgentFilter.is((Type)BIJavaScript.TYPE), (AgentFilter)AgentFilter.is((Type)BIFormFactorMax.TYPE))).getDefault();
            return new BWebWidget(info);
        }
        throw new IllegalStateException(op.get().getType() + " is not supported");
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void write(HxOp op) throws Exception {
        WbWebWidgetServlet.WebWidgetInitInfo info;
        BOrd jsOrd;
        BOrd dataOrd = BOrd.NULL;
        List<BOrd> multiLoadOrds = null;
        JSONObject props = null;
        if (BHxWebWidget.isHxPx(op)) {
            BBinding binding;
            op.setDynamic();
            BWebWidget widget = BHxWebWidget.getWebWidget(op);
            jsOrd = widget.getJs();
            Optional wTypeInfo = widget.getTypeInfoFromJs();
            if (wTypeInfo.isPresent() && ((TypeInfo)wTypeInfo.get()).is(BICollectionSupport.TYPE)) {
                multiLoadOrds = this.getLoadOrds(widget, op.getOrd());
            }
            if ((binding = BHxWebWidget.getBinding(op)) != null) {
                dataOrd = BWebWidget.getOrdFromBinding((BBinding)binding, (BOrd)op.getOrd());
            }
            props = BHxWebWidget.getPropertyChanges((BWidget)widget);
        } else {
            AgentInfo agentInfo = (AgentInfo)op.getAttribute("hxAgentInfo");
            dataOrd = op.getOrd();
            jsOrd = BWebWidget.createJsOrd((AgentInfo)agentInfo, (BOrd)dataOrd);
        }
        BIJavaScript jsWebWidget = (BIJavaScript)op.getAttribute("hxWebWidget");
        WbWebWidgetServlet.WebWidgetInitInfo webWidgetInitInfo = info = jsWebWidget == null ? WbWebWidgetServlet.getWebWidgetInitInfo((BOrd)jsOrd, (Context)op) : WbWebWidgetServlet.getWebWidgetInitInfo((BIJavaScript)jsWebWidget, (Context)op);
        if (info == null) {
            throw new Exception("Could not load HxWebWidget!");
        }
        boolean bindResources = op.isBindResources();
        if (!bindResources) {
            op.addStyleSheet(bajauxContainerCss);
            op.addStyleSheet(hxContainerCss);
            op.addStyleSheet(coreThemeCss);
        }
        BHxWebWidget.addUxThemeStyleSheet(op);
        JSONObject params = info.getParameters();
        if (dataOrd == null) {
            dataOrd = BOrd.NULL;
        }
        HtmlWriter out = new HtmlWriter((Writer)op.getWriter());
        String containerElementId = op.scope(scopedId);
        out.w((Object)"<div class='bajaux-container ux-root ux-fullscreen-support' id='").w((Object)containerElementId).w((Object)"'>");
        out.w((Object)"<div class='bajaux-toolbar-outer'><div class='bajaux-toolbar'></div></div>");
        out.w((Object)"<div class='bajaux-widget-container'><div class='bajaux-widget'></div></div>");
        if (!BHxWebWidget.isHxPx(op)) {
            String url = jsWebWidget != null ? BHxWebWidget.addFullScreenParam(BHxWebWidget.getFullUri(op)) : "about:blank";
            out.w((Object)"<iframe name='servletViewWidget.fullScreen' id='servletViewWidget' ").attr("src", url).w((Object)" width='100%' height='100%'></iframe>");
        }
        out.w((Object)"<div class='bajaux-error'></div>");
        out.w((Object)"</div>").nl();
        Writer writer = HxUtil.startOnloadWriter(op);
        try {
            out = op.getHtmlWriter();
            BHxWebWidget.createNiagaraOptions(out, op);
            this.writeContainerInitScript(out, info, containerElementId, props, params, multiLoadOrds, dataOrd);
        }
        finally {
            HxUtil.finishOnloadWriter(writer, op);
        }
    }

    private void writeContainerInitScript(HtmlWriter out, WbWebWidgetServlet.WebWidgetInitInfo info, String containerElementId, JSONObject props, JSONObject params, List<BOrd> multiLoadOrds, BOrd dataOrd) {
        BIWebResource.DependencyGraph deps = info.resolveDependencies(new BIWebResource[]{BHxContainerJsBuild.INSTANCE});
        boolean benchmark = BenchmarkCommand.isActive();
        String requireJsId = HxUtil.escapeJsStringLiteral(info.getJsId());
        if (benchmark) {
            out.w((Object)"(function() {var complete; hx.waitForLoadTime(function(){ return complete;});");
        }
        RequireJsUtil.withRequiredBuiltFiles((PrintWriter)out, () -> {
            out.w((Object)"  require(['Promise', 'nmodule/hx/rc/container/hxContainer', '").w((Object)requireJsId).w((Object)"'], ");
            out.w((Object)"function (Promise, container, Widget) {").nl();
            out.w((Object)"  if (Widget.__esModule) { Widget = Widget.default || Widget; }").nl();
            out.w((Object)"    container.initialize(").w((Object)"new Widget('").w((Object)info.getModuleName()).w((Object)"','").w((Object)info.getTypeName()).w((Object)"'),'").w((Object)HxUtil.escapeJsStringLiteral(containerElementId)).w((Object)"',").w((Object)(props == null ? "null" : props.toString())).w((Object)",").w((Object)(params == null ? "null" : params.toString())).w((Object)")");
            if (multiLoadOrds != null) {
                this.generateMultiLoadScript(out, multiLoadOrds);
            } else if (!dataOrd.isNull()) {
                out.nl().w((Object)"      .then(function(widget){").nl().w((Object)"        return container.load(widget,'").w((Object)HxUtil.escapeJsStringLiteral(dataOrd.toString())).w((Object)"');").nl().w((Object)"      })");
            }
            if (benchmark) {
                out.w((Object)".finally(function(){ complete=new Date();})");
            }
            out.w((Object)";").nl();
            out.w((Object)"});").nl();
        }, (BIWebResource.DependencyGraph)deps);
        if (benchmark) {
            out.w((Object)"})();");
        }
    }

    public static BWbViewBinding getWbViewBinding(HxOp op) {
        BWidget widget = (BWidget)op.get();
        for (BBinding binding : widget.getBindings()) {
            if (!(binding instanceof BWbViewBinding) || !binding.isBound()) continue;
            return (BWbViewBinding)binding;
        }
        return null;
    }

    public static BBinding getBinding(HxOp op) {
        BWidget widget = (BWidget)op.get();
        if (widget instanceof BWbView) {
            return BHxWebWidget.getWbViewBinding(op);
        }
        for (BBinding binding : widget.getBindings()) {
            if (!binding.isBound()) continue;
            return binding;
        }
        return null;
    }

    public static String addFullScreenParam(String url) {
        if (url.isEmpty()) {
            return "";
        }
        String newUrl = Objects.toString(url, "").replace("/ord?", "/ord/");
        Matcher matcher = queryPattern.matcher(newUrl);
        newUrl = matcher.find() && matcher.group(1).contains("fullScreen=") ? newUrl.replace("fullScreen=false", "fullScreen=true") : newUrl + "%7Cview:?fullScreen=true";
        try {
            return new URI(newUrl).toASCIIString();
        }
        catch (URISyntaxException e) {
            log.log(Level.FINE, "Cannot add full screen parameter", e);
            return "";
        }
    }

    private static String getFullUri(HxOp op) {
        String uri = op.getRequest().getRequestURI();
        if (uri == null) {
            return "";
        }
        StringBuilder b = new StringBuilder();
        b.append(uri);
        String queryString = op.getRequest().getQueryString();
        if (queryString != null && !queryString.isEmpty()) {
            b.append('?').append(queryString);
        }
        return b.toString();
    }

    private static void createNiagaraOptions(HtmlWriter out, HxOp op) {
        boolean hxPx = BHxWebWidget.isHxPx(op);
        new NiagaraEnv(hxPx ? NiagaraEnv.EnvType.HX_PX : NiagaraEnv.EnvType.HX).withProfile(op.getProfile().getType()).withWebOp((WebOp)op).withCustomProperty("profileBrowser", (Object)BWebBrowser.profileLog.isLoggable(Level.FINE)).toJavaScript((PrintWriter)out);
        if (hxPx) {
            out.w((Object)"window.niagara.env.type='").w((Object)NiagaraEnv.EnvType.HX_PX.getKey()).w((Object)"';");
        }
    }

    @Override
    @Deprecated
    public void update(HxOp op) throws Exception {
        if (BHxWebWidget.isHxPx(op)) {
            BHxWebWidget.sendWebPropertiesToClient(null, op);
        }
    }

    @Override
    public void update(int width, int height, boolean forceUpdate, HxOp op) throws Exception {
        if (BHxWebWidget.isHxPx(op)) {
            BHxWebWidget.sendWebPropertiesToClient(BSize.make((double)width, (double)height), op);
        }
    }

    @Override
    public boolean needsUpdate(BAbsTime lastUpdate, BAbsTime lastModified) {
        return true;
    }

    @Override
    public boolean isMouseEnabled(HxOp op) {
        return false;
    }

    @Override
    public boolean process(HxOp op) throws Exception {
        String contentType;
        if (BHxWebWidget.isHxPx(op) && op.getRequest().getMethod().equals("POST") && (contentType = op.getRequest().getContentType()) != null) {
            if (contentType.startsWith(PROCESS_SYNC)) {
                return BHxWebWidget.syncProperties(op);
            }
            if (contentType.startsWith(PROCESS_PROP)) {
                return BHxWebWidget.propertyValueChange(op);
            }
            if (contentType.startsWith(PROCESS_META)) {
                return BHxWebWidget.propertyMetadataChange(op);
            }
        }
        return super.process(op);
    }

    @Override
    public BWidget[] getChildWidgets(BWidget widget, Context cx) {
        return NO_WIDGETS;
    }

    @Override
    public MouseEventCommand getMouseEventHandler() {
        return null;
    }

    public static void addUxThemeStyleSheet(HxOp op) {
        BOrd ord = BHxWebWidget.getUxThemeStyleSheet(op);
        if (!ord.isNull()) {
            op.addStyleSheet(ord);
        }
    }

    public static BOrd getUxThemeStyleSheet(WebOp op) {
        String themeName;
        BOrd ord = BOrd.NULL;
        BDynamicEnum theme = (BDynamicEnum)op.getProfileConfig().get("selectedHxTheme");
        if (theme != null && (themeName = theme.getTag()) != null && !themeName.isEmpty()) {
            ord = UxUtil.checkThemeOrd((String)themeName, (Context)op);
        }
        return ord;
    }

    private static boolean syncProperties(HxOp op) throws Exception {
        JSONTokener tok = new JSONTokener(op.getUnsafePostBody());
        JSONObject obj = new JSONObject(tok);
        BWebWidget widget = BHxWebWidget.getWebWidget(op);
        if (!op.scope(scopedId).equals(JSONUtil.getString((JSONObject)obj, (String)"id"))) {
            return false;
        }
        JSONArray props = obj.getJSONArray("props");
        ArrayList<WebProperty> webProps = new ArrayList<WebProperty>(props.length());
        for (int i = 0; i < props.length(); ++i) {
            JSONObject p = props.getJSONObject(i);
            WebProperty wp = new WebProperty(JSONUtil.getString((JSONObject)p, (String)"name"), p.has("hidden") && p.getBoolean("hidden"), p.has("readonly") && p.getBoolean("readonly"), p.has("transient") ? p.getBoolean("transient") : p.has("trans") && p.getBoolean("trans"), p.has("typeSpec") ? JSONUtil.getString((JSONObject)p, (String)"typeSpec") : "", JSONUtil.getString((JSONObject)p, (String)"value"), p.has("metadata") ? p.getJSONObject("metadata") : null);
            webProps.add(wp);
        }
        Map map = WebProperty.sync((BWidget)widget, webProps);
        if (!map.isEmpty()) {
            op.setContentType("application/json");
            op.getResponse().getWriter().write(new JSONObject(map).toString());
        }
        return true;
    }

    private static JSONObject getPropertyChanges(BWidget widget) throws IOException {
        Map map = WebProperty.getPropertyChanges((BWidget)widget);
        return map.isEmpty() ? null : new JSONObject(map);
    }

    private static boolean propertyValueChange(HxOp op) throws Exception {
        JSONTokener tok = new JSONTokener(op.getUnsafePostBody());
        JSONObject obj = new JSONObject(tok);
        BWebWidget widget = BHxWebWidget.getWebWidget(op);
        if (!op.scope(scopedId).equals(JSONUtil.getString((JSONObject)obj, (String)"id"))) {
            return false;
        }
        Property prop = WebProperty.setProperty((BWidget)widget, (String)JSONUtil.getString((JSONObject)obj, (String)"name"), (String)JSONUtil.getString((JSONObject)obj, (String)"value"));
        if (prop != null) {
            op.setContentType("application/json");
            JSONObject resp = new JSONObject();
            resp.put(JSONUtil.getString((JSONObject)obj, (String)"name"), (Object)WebProperty.encodeProperty((BWidget)widget, (Property)prop));
            op.getResponse().getWriter().write(resp.toString());
        }
        return true;
    }

    private static boolean propertyMetadataChange(HxOp op) throws Exception {
        JSONTokener tok = new JSONTokener(op.getUnsafePostBody());
        JSONObject obj = new JSONObject(tok);
        BWebWidget widget = BHxWebWidget.getWebWidget(op);
        if (!op.scope(scopedId).equals(JSONUtil.getString((JSONObject)obj, (String)"id"))) {
            return false;
        }
        String propName = JSONUtil.getString((JSONObject)obj, (String)"name");
        Property prop = WebProperty.setFacets((BWidget)widget, (String)propName, (Object)obj.getJSONObject("metadata"));
        if (prop != null) {
            op.setContentType("application/json");
            JSONObject resp = new JSONObject();
            resp.put(JSONUtil.getString((JSONObject)obj, (String)"name"), (Object)WebProperty.encodeProperty((BWidget)widget, (Property)prop));
            op.getWriter().write(resp.toString());
        }
        return true;
    }

    private static void sendWebPropertiesToClient(BSize size, HxOp op) throws IOException {
        BWidget widget = (BWidget)op.get();
        BHxWebWidget.sendWebPropertiesToClient(size, widget, op);
    }

    public static void sendWebPropertiesToClient(BSize size, BWidget widget, HxOp op) throws IOException {
        JSONObject propChanges = BHxWebWidget.getPropertyChanges(widget);
        op.setContentType("application/json");
        String sizeString = "";
        if (size != null) {
            sizeString = "," + size.width + "," + size.height;
        }
        op.getWriter().write(String.format(updateJs, op.scope(scopedId), propChanges, sizeString));
    }

    private List<BOrd> getLoadOrds(BWebWidget widget, BOrd base) {
        return widget.getViewBindings().stream().map(binding -> BWebWidget.getOrdFromBinding((BBinding)binding, (BOrd)base)).collect(Collectors.toCollection(ArrayList::new));
    }

    private void generateMultiLoadScript(HtmlWriter out, List<BOrd> dataOrd) {
        out.nl().w((Object)"      .then(function(widget){").nl();
        out.w((Object)"         var prom = Promise.resolve();").nl();
        dataOrd.forEach(ord -> out.w((Object)("         prom = prom.then(function () { return container.load(widget, '" + HxUtil.escapeJsStringLiteral(ord.toString()) + "'); });")).nl());
        out.w((Object)"         return prom;").nl();
        out.w((Object)"       })").nl();
    }

    private static boolean isHxPx(HxOp op) {
        return op.get() instanceof BWidget;
    }
}

