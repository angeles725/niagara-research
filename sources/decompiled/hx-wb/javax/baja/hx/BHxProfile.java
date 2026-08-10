package javax.baja.hx;

import com.tridium.fox.sys.BFoxSession;
import com.tridium.hx.BHxOrdTargetResolver;
import com.tridium.hx.BHxWebWidget;
import com.tridium.hx.ErrorDialog;
import com.tridium.hx.HxHyperlinkInfo;
import com.tridium.hx.px.BHxPxWbView;
import com.tridium.hx.util.BenchmarkCommand;
import com.tridium.hx.util.HxUtils;
import com.tridium.sys.registry.NAgentInfo;
import com.tridium.sys.registry.NTypeInfo;
import com.tridium.util.CustomThemeModuleManager;
import com.tridium.util.ThrowableUtil;
import com.tridium.util.PxUtil.PxHx;
import com.tridium.ux.NiagaraEnv;
import com.tridium.ux.NiagaraEnv.EnvType;
import com.tridium.web.IWebEnvProvider;
import com.tridium.web.RequireJsUtil;
import com.tridium.web.WebEnv;
import com.tridium.web.WebProcessException;
import com.tridium.web.WebUtil;
import com.tridium.web.filters.ViewFilter;
import com.tridium.web.servlets.WbServlet;
import com.tridium.web.session.NiagaraWebSession;
import com.tridium.web.session.WebSessionUtil;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentInfo;
import javax.baja.agent.AgentList;
import javax.baja.agent.BAbstractPxView;
import javax.baja.bajaux.BBajauxJsBuild;
import javax.baja.control.trigger.BManualTriggerMode;
import javax.baja.file.BExporter;
import javax.baja.file.types.image.BIImageFile;
import javax.baja.file.types.text.BCsvFile;
import javax.baja.file.types.text.BIHtmlFile;
import javax.baja.file.types.text.BPxFile;
import javax.baja.io.HtmlWriter;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.nav.BNavFileNode;
import javax.baja.nav.NavFileDecoder;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.session.INiagaraSuperSession;
import javax.baja.session.SessionUtil;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BSingleton;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Localizable;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.user.BUser;
import javax.baja.web.BIFormFactorMax;
import javax.baja.web.BIWebProfile;
import javax.baja.web.BServletView;
import javax.baja.web.BWebProfileConfig;
import javax.baja.web.BWebService;
import javax.baja.web.IWebEnv;
import javax.baja.web.WebDev;
import javax.baja.web.WebOp;
import javax.baja.web.hx.BIHxProfile;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.BIWebResource;
import javax.baja.web.mobile.BIMobileWebProfile;
import javax.baja.web.mobile.BIMobileWebView;
import javax.baja.web.mobile.BMobileWebProfileConfig;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpSession;

@NiagaraType
public abstract class BHxProfile extends BSingleton implements BIMobileWebProfile, IWebEnvProvider, BIHxProfile {
   public static final Type TYPE = Sys.loadType(BHxProfile.class);
   public static final String themeKey = "selectedHxTheme";
   public static final String fullScreenKey = "fullScreen";
   public static final String autoRefreshKey = "autoRefreshTrigger";
   public static final BOrd coreThemeCss = BOrd.make("module://web/rc/theme/theme.css");
   public static final BOrd coreHxCss = BOrd.make("module://hx/javax/baja/hx/default.css");
   public static final BOrd coreHxJs = BOrd.make("module://hx/javax/baja/hx/hx.js");
   public static final BOrd hxContainerCss = BOrd.make("module://hx/rc/container/container.css");
   public static final BOrd hxContainerJs = BOrd.make("module://hx/rc/container/hxContainer.built.min.js");
   public static final BOrd jQueryJs = BOrd.make("module://js/rc/jquery/jquery.min.js");
   public static final BOrd contextMenuCss = BOrd.make("module://js/rc/jquery/contextMenu/jquery.contextMenu.css");
   private int eventCounter = 0;
   HashMap<String, Event> events = new HashMap<>();
   private static Command benchmark;
   private static final TypeInfo hxPxWbView = BHxPxWbView.TYPE.getTypeInfo();
   private static final TypeInfo servlet = BServletView.TYPE.getTypeInfo();
   private static final TypeInfo exporter = BExporter.TYPE.getTypeInfo();
   private static final TypeInfo mobileView = BIMobileWebView.TYPE.getTypeInfo();
   private static final String UTF_8 = StandardCharsets.UTF_8.name();
   private static final BFacets THEME_FACETS = BFacets.make(
      "fieldEditor", BString.make("workbench:FrozenEnumFE"), "uxFieldEditor", BString.make("webEditors:FrozenEnumEditor")
   );
   private static final String[] DEV_VIEWS = new String[]{"bajaui:CollectionView"};
   private static final boolean AUTO_REFRESH_ENABLED = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.profile.hx.autoRefreshEnabled"))
   );
   private static final Map<String, Boolean> devViewsEnabled = new HashMap<>();
   private static boolean themeWarningWasLogged;

   public Type getType() {
      return TYPE;
   }

   protected BHxProfile() {
      this.registerEvent(benchmark = new BenchmarkCommand());
   }

   public String[] listConfig() {
      return AUTO_REFRESH_ENABLED ? new String[]{"selectedHxTheme", "autoRefreshTrigger"} : new String[]{"selectedHxTheme"};
   }

   public BValue getConfig(String key) {
      switch (key) {
         case "selectedHxTheme":
            return BFoxSession.getDefaultThemeEnumForSession();
         case "autoRefreshTrigger":
            return BManualTriggerMode.make();
         default:
            return null;
      }
   }

   public BFacets getConfigFacets(String key) {
      return key.equals("selectedHxTheme") ? THEME_FACETS : BFacets.NULL;
   }

   public void setConfig(String key, BValue value) {
   }

   public String getPageTitle(BHxView view, HxOp op) throws Exception {
      return view.getPageTitle(op);
   }

   public void doHead(HxOp op) throws Exception {
   }

   public void doBody(BHxView view, HxOp op) throws Exception {
      view.write(op);
      this.displayError(op);
   }

   public HxOp createOp(WebOp c) throws Exception {
      HxOp op = new HxOp(c);
      op.getResponse().setCharacterEncoding(UTF_8);
      return op;
   }

   public void destroyOp(HxOp op) throws Exception {
      op.deleteTempFiles();
   }

   public String getAppName() {
      return null;
   }

   public String[] getAppNames() {
      return this.getAppName() != null ? new String[]{this.getAppName()} : new String[0];
   }

   public boolean hasView(BObject target, AgentInfo agentInfo) {
      if (agentInfo.getAgentType().toString().equals("obixDriver:ObjectToObix")) {
         return false;
      } else {
         Boolean enabled = devViewsEnabled.get(agentInfo.getAgentId());
         return enabled == null || enabled;
      }
   }

   public AgentList getViews(HxOp op) {
      AgentList list = BHxProfile.WebEnvHolder.INSTANCE.getDefaultViews(op);

      for (String devView : DEV_VIEWS) {
         if (!devViewsEnabled.get(devView)) {
            list.remove(devView);
         }
      }

      BObject obj = op.get();
      if (!(obj instanceof BIHtmlFile) && !(obj instanceof BPxFile) && !(obj instanceof BIImageFile) && !(obj instanceof BCsvFile)) {
         int downloadIndex = list.indexOf("web:FileDownloadView");
         if (downloadIndex > -1) {
            int editorIndex = list.indexOf("webEditors:TextFileEditor");
            if (editorIndex > -1) {
               list.add(downloadIndex, "webEditors:TextFileEditor");
            }
         }
      }

      return list;
   }

   public boolean isFullScreen(BHxView view, HxOp op) {
      return "true".equals(op.getViewParameter("fullScreen", "false"));
   }

   @Deprecated
   protected boolean hasWebStartAddressBar() {
      return true;
   }

   @Deprecated
   protected boolean hasWebStartStatusBar() {
      return true;
   }

   private void doWebStartMetaTags(PrintWriter out) {
      out.println("<meta name='niagara-webstart-has-address-bar' content='" + this.hasWebStartAddressBar() + "' />");
      out.println("<meta name='niagara-webstart-has-status-bar' content='" + this.hasWebStartStatusBar() + "' />");
   }

   public void writeDocument(BHxView view, HxOp op) throws Exception {
      String themeName = this.setupTheme(op);
      op.getResponse().setCharacterEncoding("UTF-8");
      INiagaraSuperSession superSession = SessionUtil.getCurrentNiagaraSuperSession();
      if (superSession != null) {
         HxUtil.writeFormValue("csrfToken", superSession.getCsrfToken(), op);
      }

      this.doBody(view, op);
      if (!op.isRaw()) {
         if (!op.isErrorSent()) {
            String url = op.getRedirect();
            if (url != null) {
               op.getResponse().sendRedirect(url);
            } else {
               String[] htags = op.getHeadTags();
               BOrd[] styles = op.getStyleSheetOrds();
               BOrd[] scripts = op.getJavaScriptOrds();
               String[] global = op.getGlobal();
               op.setContentType("text/html");
               PrintWriter out = op.getResponse().getWriter();
               String title = "";
               String pageTitle = this.getPageTitle(view, op);
               if (pageTitle != null) {
                  title = pageTitle;
               }

               out.println("<!DOCTYPE html>");
               out.println("<html xmlns='http://www.w3.org/1999/xhtml' lang='en' xml:lang='en' style='overflow:auto;'>");
               out.println("<head>");
               out.println("<title>" + XWriter.safeToString(title, true) + "</title>");
               out.print("<meta http-equiv=\"X-UA-Compatible\" content=\"");
               out.print(XWriter.safeToString(WbServlet.xuaCompatibleContent, false));
               out.println("\">");
               out.println("<meta http-equiv='Content-type' content='text/html;charset=UTF-8' />");

               for (int i = 0; i < htags.length; i++) {
                  out.println(htags[i]);
               }

               this.doWebStartMetaTags(out);
               out.println("<link rel='shortcut icon' href='/favicon.ico' />");
               boolean bindResources = op.isBindResources();
               if (bindResources) {
                  long lastBuildTime = Sys.getRegistry().getLastBuildTime().getMillis();
                  String typeSpec = BHxOrdTargetResolver.TYPE.toString().replace(":", "%3A");
                  RequireJsUtil requireJsUtil = RequireJsUtil.make(false, op);
                  out.println("<script type='text/javascript'>");
                  requireJsUtil.requirejsNoHtml(out);
                  out.println("</script>");
                  out.println(
                     String.format("<link rel='stylesheet' type='text/css' href='/vfile/hx/app.css?typeSpec=%s&version=%d'/>", typeSpec, lastBuildTime)
                  );
                  out.println(String.format("<script type='text/javascript' src='/vfile/hx/app.js?typeSpec=%s&version=%d'></script>", typeSpec, lastBuildTime));
                  out.println(HxUtils.getInitSyncedSessionStorageScript());
                  out.println("<script type='text/javascript'>");
                  out.println(requireJsUtil.defineSystemProperties());
                  out.println("</script>");
               } else {
                  out.println("<link rel='stylesheet' type='text/css' href='" + WebUtil.toUri(op, op.getRequest(), coreThemeCss) + "'/>");
                  out.println("<link rel='stylesheet' type='text/css' href='" + WebUtil.toUri(op, op.getRequest(), coreHxCss) + "'/>");
                  out.println("<link rel='stylesheet' type='text/css' href='" + WebUtil.toUri(op, op.getRequest(), contextMenuCss) + "'/>");
                  out.println("<link rel='stylesheet' type='text/css' href='" + WebUtil.toUri(op, op.getRequest(), hxContainerCss) + "'/>");
               }

               if (!themeName.isEmpty()) {
                  BOrd ord = BOrd.make("module://theme" + themeName + "/hx/theme.css");

                  try {
                     ord.resolve(BLocalHost.INSTANCE, op);
                     out.println("<link rel='stylesheet' type='text/css' href='" + WebUtil.toUri(op, op.getRequest(), ord) + "'/>");
                  } catch (Throwable var20) {
                  }
               }

               for (int i = 0; i < styles.length; i++) {
                  String cssUrl = WebUtil.toUri(op, op.getRequest(), styles[i]);
                  out.println("<link rel='stylesheet' type='text/css' href='" + HxUtil.encodeURLForHref(cssUrl) + "'/>");
               }

               if (!bindResources) {
                  if (!RequireJsUtil.USE_NATIVE_PROMISES) {
                     out.println("<script type='text/javascript' src='" + WebUtil.toUri(op, op.getRequest(), this.getPromiseJS()) + "'></script>");
                  }

                  RequireJsUtil.make(false, op).requirejs(out);
                  out.println(HxUtils.getInitSyncedSessionStorageScript());
                  if (op.isJQuery()) {
                     out.println("<script type='text/javascript' src='" + WebUtil.toUri(op, op.getRequest(), jQueryJs) + "'></script>");
                     out.println("<script type='text/javascript'>jQuery.noConflict();</script>");
                  }

                  out.println("<script type='text/javascript' src='" + WebUtil.toUri(op, op.getRequest(), coreHxJs) + "'></script>");
               }

               for (int i = 0; i < scripts.length; i++) {
                  String javascriptUrl = WebUtil.toUri(op, op.getRequest(), scripts[i]);
                  out.println("<script type='text/javascript' src='" + javascriptUrl + "'></script>");
               }

               out.println("<script type='text/javascript'>");
               new NiagaraEnv(EnvType.HX).withProfile(op.getProfile().getType()).withWebOp(op).toJavaScript(out);
               out.println("hx.startActivityMonitor();");
               if (this.isFullScreen(view, op)) {
                  out.println("hx.setFullScreen(true);");
               }

               String agentId = ViewFilter.getViewId(op);
               String profileInfo = "{ viewId: '" + HxUtil.escapeJsStringLiteral(agentId) + "' }";
               out.print("hx.setProfileInfo(" + profileInfo + ");");
               if (global.length > 0) {
                  for (int i = 0; i < global.length; i++) {
                     out.println(global[i]);
                  }
               }

               if (BenchmarkCommand.isActive()) {
                  op.addOnload(this.getBenchmarkCommand().getInvokeCode(op));
               }

               out.print("function hxProfileOnload(){  require(['Promise'], function () { hx.started(" + op.isDynamic() + ", " + HxUtil.pollFreq + ");");
               String[] onload = op.getOnload();

               for (int i = 0; i < onload.length; i++) {
                  out.print(" " + HxUtil.unescapeJsForInvocation(onload[i]));
               }

               out.println("})}");
               String[] resize = op.getOnresize();
               if (resize.length > 0) {
                  out.print("function hxProfileOnresize(){");

                  for (int i = 0; i < resize.length; i++) {
                     out.print(" " + HxUtil.unescapeJsForInvocation(resize[i]));
                  }

                  out.println("}");
               }

               String[] onunload = op.getOnunload();
               if (onunload.length > 0) {
                  out.print("function hxProfileOnunload(){");

                  for (int i = 0; i < onunload.length; i++) {
                     out.print(" " + HxUtil.unescapeJsForInvocation(onunload[i]));
                  }

                  out.println("}");
               }

               out.print("try {");
               RequireJsUtil.withRequiredBuiltFiles(out, () -> {
                  out.print("  require([ 'bajaux/commands/UndoManager' ], function (UndoManager) {");
                  out.print("    UndoManager.$installGlobal()");
                  out.print("      .catch(function (err) { console.error(err); });");
                  out.print("  });");
               }, new BIWebResource[]{BBajauxJsBuild.INSTANCE});
               out.print("} catch (e) {");
               out.print("  console.error(e);");
               out.print("}");
               out.println("</script>");
               if (AUTO_REFRESH_ENABLED) {
                  HxUtils.writeAutoRefresh(op);
               }

               this.doHead(op);
               out.println("</head>");
               out.print("<body ");
               if (!themeName.isEmpty()) {
                  out.print("class=\"" + themeName + "\" ");
               }

               out.print(" onload='hxProfileOnload();' ");
               if (resize.length > 0) {
                  out.print("onresize='hxProfileOnresize();' ");
               }

               if (onunload.length > 0) {
                  out.print("onunload='hxProfileOnunload();' ");
               }

               out.println(">");
               out.print("<form class='hx " + XWriter.safeToString(String.valueOf(this.getCssClassName(op)), false) + "' method='post' action='/ord?");
               XWriter.safe(out, String.valueOf(op.getOrd()), false);
               out.print("'");
               if (op.isMultiPartForm()) {
                  out.print(" enctype='multipart/form-data'");
               }

               out.println(">");
               out.print(op.getContent().toString());
               out.println("<div style='display:none'><input type='submit' id='hx_submit' onclick='hx.$formClicked=true; return hx.$allowFormSubmit;'/></div>");
               out.println("</form>");
               out.println("</body>");
               out.println("</html>");
            }
         }
      }
   }

   public String getCssClassName(HxOp op) {
      return this.getType().getTypeSpec().getTypeName();
   }

   private BOrd getPromiseJS() {
      return WebDev.get("js").isEnabled() ? BOrd.make("module://js/rc/bluebird/bluebird.js") : BOrd.make("module://js/rc/bluebird/bluebird.min.js");
   }

   public void updateDocument(BHxView view, HxOp op) throws Exception {
      view.update(op);
   }

   public boolean processDocument(BHxView view, HxOp op) throws Exception {
      return view.process(op);
   }

   public void saveDocument(BHxView view, HxOp op) throws Exception {
      view.save(op);
   }

   public void setError(Throwable err, HxOp op) {
      String details = "";
      if (((BWebService)Sys.getService(BWebService.TYPE)).getShowStackTrace()) {
         details = ThrowableUtil.dumpToString(err);
      } else {
         details = err.toString();
      }

      String error = null;
      if (err instanceof Localizable) {
         error = ((Localizable)err).toString(op);
      } else {
         error = err.getMessage();
         if (error == null) {
            error = err.getClass().getName();
         }

         Localizable localizable = ThrowableUtil.toLocalizable(err);
         if (localizable != null) {
            String localizableToString = localizable.toString(op);
            if (localizableToString != null) {
               error = error + "\n" + localizableToString;
            }
         }
      }

      op.getRequest().getSession().setAttribute("hx.error", error);
      op.getRequest().getSession().setAttribute("hx.error.name", err.getClass().getName().substring(err.getClass().getName().lastIndexOf(46) + 1));
      op.getRequest().getSession().setAttribute("hx.error.details", details);
   }

   public void clearError(HxOp op) {
      op.getRequest().getSession().removeAttribute("hx.error");
      op.getRequest().getSession().removeAttribute("hx.error.name");
      op.getRequest().getSession().removeAttribute("hx.error.details");
   }

   public boolean displayError(HxOp op) throws Exception {
      String error = (String)op.getRequest().getSession().getAttribute("hx.error");
      String details = (String)op.getRequest().getSession().getAttribute("hx.error.details");
      String name = (String)op.getRequest().getSession().getAttribute("hx.error.name");
      if (error != null) {
         HtmlWriter out = op.getHtmlWriter();
         out.write("<script>");
         Dialog dialog = new ErrorDialog(name, error, details);
         dialog.open(op);
         out.write("</script>");
         this.clearError(op);
         return true;
      } else {
         return false;
      }
   }

   private String setupTheme(HxOp op) {
      String themeName = "";
      BDynamicEnum userTheme = (BDynamicEnum)op.getProfileConfig().get("selectedHxTheme");
      if (userTheme != null) {
         themeName = userTheme.getTag();
      }

      BDynamicEnum themesEnum = CustomThemeModuleManager.getModuleEnumForTag(themeName);

      try {
         themesEnum.getRange().get(themeName);
      } catch (InvalidEnumException var7) {
         themeName = themesEnum.getTag();
         if (!themeWarningWasLogged) {
            Logger.getLogger("hx").warning(String.format("There was a problem setting profile theme and was switched to the fallback %s theme.", themeName));
            themeWarningWasLogged = true;
         }
      }

      NiagaraWebSession session = WebSessionUtil.getSession(op.getRequest());
      if (!themeName.isEmpty()) {
         if (session.getAttribute("themeName") == null) {
            session.setAttribute("themeName", themeName);
         }
      } else {
         String sessionTheme = (String)session.getAttribute("themeName");
         if (sessionTheme != null && !sessionTheme.isEmpty()) {
            themeName = sessionTheme;
         }
      }

      if (!themeName.isEmpty()) {
         HttpSession httpSession = op.getRequest().getSession();
         if (httpSession.getAttribute("themeName") == null) {
            httpSession.setAttribute("themeName", themeName);
         }
      }

      return themeName;
   }

   public void registerEvent(Event event) {
      event.setId("profileEvent" + this.eventCounter++);
      this.events.put(event.getId(), event);
   }

   public static IWebEnv webEnv() {
      return BHxProfile.WebEnvHolder.INSTANCE;
   }

   public final IWebEnv getWebEnv(WebOp op) throws WebProcessException {
      return webEnv();
   }

   public Command getBenchmarkCommand() {
      return benchmark;
   }

   static {
      AccessController.doPrivileged((PrivilegedAction<Void>)(() -> {
         for (String s : DEV_VIEWS) {
            devViewsEnabled.put(s, Boolean.getBoolean("profile.enableDevView." + s));
         }

         return null;
      }));
   }

   private static class HxWebEnv extends WebEnv {
      private HxWebEnv() {
         this.filter = add(this.filter, pxView);
         this.filter = add(this.filter, BHxProfile.exporter);
         this.filter = AgentFilter.or(this.filter, new BHxProfile.HxWebEnv.HxFilter());
         this.filter = AgentFilter.and(this.filter, new BHxProfile.HxWebEnv.NoMobileFilter());
      }

      public AgentList getViews(WebOp op) {
         BIWebProfile profile = op.getWebEnv().getWebProfile(op);
         if (profile != null && profile instanceof BHxProfile) {
            try {
               HxOp hxOp = op instanceof HxOp ? (HxOp)op : new HxOp(op, false);
               return ((BHxProfile)profile).getViews(hxOp);
            } catch (Exception var4) {
               throw new BajaRuntimeException(var4);
            }
         } else {
            return this.getDefaultViews(op);
         }
      }

      private AgentList getDefaultViews(WebOp op) {
         AgentList agentList = op.get().getAgents(op).filter(AgentFilter.or(this.filter, new BHxProfile.HxWebEnv.HxWbFilter(this.getWebProfile(op))));
         agentList.toBottom(new HxHyperlinkInfo.DeprecatedFilter(op));
         return agentList;
      }

      public AgentInfo getDefaultView(WebOp op, AgentList views) {
         BObject obj = op.get();
         if (obj instanceof BPxFile) {
            return views.filter(AgentFilter.is(hxView)).getDefault();
         } else {
            for (int i = 0; i < views.size(); i++) {
               AgentInfo agent = views.get(i);
               TypeInfo agentType = agent.getAgentType();
               if (agentType.is(BHxProfile.servlet)) {
                  return agent;
               }

               if (agentType.is(pxView)) {
                  return agent;
               }

               if (agentType.is(wbView)) {
                  return agent;
               }

               if (agentType.is(BIFormFactorMax.TYPE) && agentType.is(BIJavaScript.TYPE)) {
                  return agent;
               }
            }

            return super.getDefaultView(op, views);
         }
      }

      public AgentInfo getView(AgentList allViews, String viewId) {
         if (viewId.equals("hx:HxActionView")) {
            TypeInfo actionInfo = Sys.getRegistry().getType("hx:HxActionView");
            return new NAgentInfo((NTypeInfo)actionInfo);
         } else {
            return super.getView(allViews, viewId);
         }
      }

      public AgentInfo translate(WebOp op, AgentInfo viewInfo) {
         TypeInfo viewTypeInfo = viewInfo.getAgentType();
         if (viewInfo instanceof BAbstractPxView) {
            AgentList agentList = Sys.getRegistry().getAgents(((BAbstractPxView)viewInfo).getType().getTypeInfo());
            agentList = agentList.filter(AgentFilter.is(hxView));
            TypeInfo typeInfo = agentList.getDefault().getAgentType();
            return new PxHx((BAbstractPxView)viewInfo, typeInfo);
         } else if (viewTypeInfo.is(wbView)) {
            AgentList hxViews = Sys.getRegistry().getAgents(viewTypeInfo);
            AgentList result = hxViews.filter(AgentFilter.and(AgentFilter.is(hxView), HxHyperlinkInfo.getViewsFilter(op)));
            return result.getDefault();
         } else {
            return viewTypeInfo.is(BIFormFactorMax.TYPE) && viewTypeInfo.is(BIJavaScript.TYPE) ? BHxWebWidget.TYPE.getTypeInfo().getAgentInfo() : viewInfo;
         }
      }

      public BWebProfileConfig makeWebProfileConfig() {
         return new BWebProfileConfig();
      }

      public BWebProfileConfig getWebProfileConfig(BUser user) {
         return (BWebProfileConfig)user.getMixIn(BWebProfileConfig.TYPE);
      }

      public BIWebProfile getWebProfile(WebOp op) {
         BUser user = op.getUser();
         BWebProfileConfig profileConfig = (BWebProfileConfig)op.getRequest().getSession(true).getAttribute("profileConfig");
         if (profileConfig == null) {
            profileConfig = (BWebProfileConfig)user.getMixIn(BWebProfileConfig.TYPE);
            op.getRequest().getSession(true).setAttribute("profileConfig", profileConfig);
         }

         return (BIWebProfile)profileConfig.make();
      }

      public BOrd getHomePage(WebOp op) {
         BWebProfileConfig profileConfig = op.getProfileConfig();
         if (profileConfig != null && profileConfig instanceof BMobileWebProfileConfig) {
            try {
               BOrd navFile = ((BMobileWebProfileConfig)profileConfig).getMobileNavFile();
               if (!navFile.isNull()) {
                  BNavFileNode root = NavFileDecoder.load(navFile).getRootNode();
                  return root.getOrdInSession();
               }
            } catch (Exception var5) {
               var5.printStackTrace();
            }
         }

         return op.getUser().getHomePage();
      }

      private static class HxFilter extends AgentFilter {
         private HxFilter() {
         }

         public boolean include(AgentInfo agent) {
            if (agent.getAgentType().is(WebEnv.hxView)) {
               return !agent.getAgentId().endsWith("/hx");
            } else {
               return agent.getAgentType().is(BHxProfile.servlet)
                  ? true
                  : agent.getAgentType().is(BIFormFactorMax.TYPE) && agent.getAgentType().is(BIJavaScript.TYPE);
            }
         }
      }

      private static class HxWbFilter extends AgentFilter {
         BIWebProfile profile;

         public HxWbFilter(BIWebProfile profile) {
            this.profile = profile;
         }

         public boolean include(AgentInfo agent) {
            if (agent.getAgentType().is(WebEnv.wbView)) {
               AgentList hxViews = Sys.getRegistry().getAgents(agent.getAgentType());
               hxViews = hxViews.filter(AgentFilter.is(WebEnv.hxView));

               for (int i = 0; i < hxViews.size(); i++) {
                  AgentInfo hxView = hxViews.get(i);
                  TypeInfo typeInfo = hxView.getAgentType();
                  if (!hxView.getAgentType().is(BHxProfile.hxPxWbView)) {
                     if (hxView.getAppName() == null || this.profile == null) {
                        return true;
                     }

                     String[] profileAppNames = this.profile.getAppNames();

                     for (int j = 0; j < profileAppNames.length; j++) {
                        if (profileAppNames[j].equals(hxView.getAppName())) {
                           return true;
                        }
                     }
                  }
               }
            }

            return false;
         }
      }

      private static class NoMobileFilter extends AgentFilter {
         private NoMobileFilter() {
         }

         public boolean include(AgentInfo agent) {
            return !agent.getAgentType().is(BHxProfile.mobileView);
         }
      }
   }

   private interface WebEnvHolder {
      BHxProfile.HxWebEnv INSTANCE = new BHxProfile.HxWebEnv();
   }
}
