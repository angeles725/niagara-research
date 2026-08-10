package com.tridium.hx;

import com.tridium.hx.util.BenchmarkCommand;
import com.tridium.json.JSONArray;
import com.tridium.json.JSONObject;
import com.tridium.web.Template;
import com.tridium.web.WebEnv;
import com.tridium.web.servlets.ViewAllOrdServlet;
import com.tridium.webeditors.ux.wb.profile.BServletViewWidget;
import java.io.StringWriter;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.baja.agent.AgentFilter;
import javax.baja.agent.AgentList;
import javax.baja.file.BIFile;
import javax.baja.file.BajaFileUtil;
import javax.baja.hx.BHxProfile;
import javax.baja.hx.BHxView;
import javax.baja.hx.Command;
import javax.baja.hx.HxOp;
import javax.baja.hx.HxUtil;
import javax.baja.io.HtmlWriter;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIBoolean;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;
import javax.baja.web.BWebProfileConfig;
import javax.baja.web.js.BIWebResource;
import javax.baja.web.js.JsInfo;
import javax.baja.webeditors.ux.BWebEditorsJsBuild;
import org.owasp.encoder.Encode;

@NiagaraType
@NiagaraSingleton
public class BHTML5HxProfile extends BHxProfile {
   public static final BHTML5HxProfile INSTANCE = new BHTML5HxProfile();
   public static final Type TYPE = Sys.loadType(BHTML5HxProfile.class);
   private static final AgentFilter wbViewFilter = AgentFilter.is(WebEnv.wbView);
   private static final String navTreeSideBarKey = "navTreeSideBar";
   private static final String enableHxWbViewsKey = "hxWbViews";
   private static final String enableViewSelectionKey = "viewSelection";
   private static final String navFileTreeKey = "navFileTree";
   private static final String hierarchiesTreeKey = "hierarchiesTree";
   private static final String searchSideBarKey = "searchSideBar";
   private static final List<String> sideBarConfigOptions = Arrays.asList("navTreeSideBar", "searchSideBar", "paletteSideBar");
   private static final Map<String, String> keyToClassName = new HashMap<>();
   private static final List<String> treeConfigOptions = Arrays.asList("navFileTree", "configTree", "filesTree", "historiesTree", "hierarchiesTree");
   private static final List<String> configOptions = new ArrayList<>();
   private static final BFacets yesNoFacets = BFacets.make(
      "trueText", BString.make("%lexicon(bajaui:dialog.yes)%"), "falseText", BString.make("%lexicon(bajaui:dialog.no)%")
   );
   private static final boolean dynamicLoading = "true"
      .equals(AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.profile.dynamicLoading", "true"))));
   private static final boolean HIDE_MEDIA_COMMAND = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.profile.hideMediaCommand"))
   );
   private static final JsInfo profileJsInfo = JsInfo.make(BOrd.make("module://webEditors/rc/wb/profile/profile.js"), BWebEditorsJsBuild.TYPE);
   private static final JsInfo containerJsInfo = JsInfo.make(BOrd.make("module://hx/rc/container/hxContainer.js"), BHxContainerJsBuild.TYPE);
   private static final BOrd splitPaneCss = BOrd.make("module://js/rc/jquery/split-pane/split-pane.css");
   private static final BOrd profileCss = BOrd.make("module://webEditors/rc/wb/profile/profile.css");
   private static final BOrd bajauxContainerCss = BOrd.make("module://bajaux/rc/container/container.css");
   private static final BOrd coreThemeCss = BOrd.make("module://web/rc/theme/theme.css");
   private static final BOrd profileVm = BOrd.make("module://webEditors/rc/wb/profile/profile.vm");
   private static final BPermissions searchPermissions = BPermissions.operatorRead.or(BPermissions.operatorInvoke);
   private static Command benchmark;

   @Override
   public Type getType() {
      return TYPE;
   }

   protected BHTML5HxProfile() {
      this.registerEvent(benchmark = new BHTML5HxProfile.Html5BenchmarkCommand());
   }

   @Override
   public Command getBenchmarkCommand() {
      return benchmark;
   }

   @Override
   public void writeDocument(BHxView view, HxOp op) throws Exception {
      op.addHeadTag("<meta name='viewport' content='width=device-width initial-scale=1.0 target-densityDpi=medium-dpi'/>");
      super.writeDocument(view, op);
   }

   @Override
   public void doBody(BHxView view, HxOp op) throws Exception {
      if (!op.isBindResources()) {
         op.addStyleSheet(bajauxContainerCss);
         op.addStyleSheet(hxContainerCss);
         op.addStyleSheet(coreThemeCss);
         op.addStyleSheet(splitPaneCss);
         op.addStyleSheet(profileCss);
      }

      BHxWebWidget.addUxThemeStyleSheet(op);
      BIFile fullScreenUtils = (BIFile)BOrd.make("module://hx/rc/container/fullScreenUtils.js").get();
      op.addGlobal(BajaFileUtil.readString(fullScreenUtils));
      op.addGlobal("window.fullScreenUtils.validate();");
      if ("true".equals(op.getViewParameter("previewMedia", "false"))) {
         op.addGlobal("window.name = 'fullScreen' + new Date().getTime();");
      }

      boolean fullScreen = this.isFullScreen(view, op);
      if (fullScreen) {
         op.addGlobal("require(['nmodule/webEditors/rc/wb/profile/fullScreen'],function(){});");
         HtmlWriter out = op.getHtmlWriter();
         out.w("<div class='window-bg hx-fullscreen'>");
         super.doBody(view, op);
         out.w("</div>");
      } else {
         if (!(view instanceof BHxWebWidget)) {
            op.setAttribute("hxWebWidget", BServletViewWidget.INSTANCE);
         }

         String content = HxUtil.marshal(sOp -> {
            BHxWebWidget.INSTANCE.write(sOp);
            this.displayError(sOp);
         }, op);
         Lexicon lex = Lexicon.make(BServletViewWidget.TYPE.getModule(), op);
         Map<String, String> map = new HashMap<>();
         map.put("containerJs", containerJsInfo.getJsId());
         map.put("profileJs", profileJsInfo.getJsId());
         JSONArray dependencies = BIWebResource.resolve(Arrays.asList(BWebEditorsJsBuild.INSTANCE, BHxContainerJsBuild.INSTANCE)).toJSON();
         map.put("deps", dependencies.toString());
         map.put("content", content);
         map.put("bajaux", Boolean.toString(view instanceof BHxWebWidget));
         map.put("dynamicLoading", Boolean.toString(dynamicLoading));
         map.put("displayName", view.getPageTitle(op).replace("\\", "\\\\").replace("\"", "\\\""));
         map.put("ord", op.getOrdWithoutViewQuery().toString());
         map.put("stationName", Encode.forHtml(Sys.getStation().getStationName()));
         map.put("viewInfo", ViewAllOrdServlet.writeViewList(op, new StringWriter()).toString());
         map.put("loading", Encode.forJavaScriptAttribute(lex.getHtmlSafe("loading")));
         map.put("logoff", lex.getHtmlSafe("profileLogoff"));
         map.put("home", op.getWebEnv().getHomePage(op).toString());
         BWebProfileConfig config = op.getProfileConfig();
         JSONObject profileConfig = getProfileConfigOptions(config, op);
         map.put(
            "sideBarConfigOptions",
            "["
               + sideBarConfigOptions.stream()
                  .filter(profileConfig::getBoolean)
                  .map(key -> String.format("{'key':'%s','displayName':'%s','className':'%s'}", key, lex.getHtmlSafe(key), keyToClassName.get(key)))
                  .collect(Collectors.joining(","))
               + "]"
         );
         map.put("isMediaCommandAvailable", Boolean.toString(!HIDE_MEDIA_COMMAND && profileConfig.getBoolean("viewSelection")));
         map.put("configOptions", profileConfig.toString());
         map.put("viewSelectorElement", profileConfig.getBoolean("viewSelection") ? "<div class=\"controls\"></div>" : "");
         op.addHeadTag(
            "<script type='text/javascript'>\n  var require = typeof require === 'undefined' ? {} : require;\n  require.config = require.config || {};\n  require.config.baja = require.config.baja || {};\n  require.config.baja.start = require.config.baja.start || {};\n  require.config.baja.start.navFile = true;\n</script>"
         );
         op.getWriter().write(Template.process(map, profileVm));
      }
   }

   private static JSONObject getProfileConfigOptions(BWebProfileConfig config, Context cx) {
      boolean navTreeSideBarEnabled = isEnabled(config, "navTreeSideBar")
         && treeConfigOptions.stream().filter(key -> isEnabled(config, key)).toArray().length > 0;
      JSONObject obj = new JSONObject();
      Function<String, Boolean> shouldBeEnabled = key -> {
         if (key.equals("navTreeSideBar")) {
            return navTreeSideBarEnabled;
         } else if (!navTreeSideBarEnabled && treeConfigOptions.contains(key)) {
            return false;
         } else if (key.equals("navFileTree") && cx.getUser().getNavFile().isNull()) {
            return false;
         } else if (key.equals("hierarchiesTree") && !isHierarchyAvailable(cx)) {
            return false;
         } else {
            return key.equals("searchSideBar") && !isSearchAvailable(cx) ? false : isEnabled(config, key);
         }
      };
      configOptions.forEach(key -> obj.put(key, shouldBeEnabled.apply(key)));
      return obj;
   }

   private static boolean isSearchAvailable(Context cx) {
      return isServiceAvailable("search:SearchService", searchPermissions, cx);
   }

   private static boolean isHierarchyAvailable(Context cx) {
      return isServiceAvailable("hierarchy:HierarchyService", BPermissions.operatorRead, cx);
   }

   private static boolean isServiceAvailable(String serviceTypeSpec, BPermissions requiredPermissions, Context cx) {
      try {
         BComponent c = Sys.getService(Sys.getType(serviceTypeSpec));
         return c.getPermissions(cx).has(requiredPermissions);
      } catch (Throwable var4) {
         return false;
      }
   }

   @Override
   public AgentList getViews(HxOp op) {
      AgentList list = super.getViews(op);
      list.remove("mobile:MobileFieldEditorView");
      list.remove("web:FileUploadView");
      BBoolean enableHxWbViews = (BBoolean)op.getProfileConfig().get("hxWbViews");
      if (enableHxWbViews != null && !enableHxWbViews.getBoolean()) {
         list = list.filter(AgentFilter.not(wbViewFilter));
      }

      int propertySheetIndex = list.indexOf("workbench:PropertySheet");
      int multiSheetIndex = list.indexOf("webEditors:MultiSheet");
      if (propertySheetIndex >= 0 && propertySheetIndex < multiSheetIndex) {
         list.swap(propertySheetIndex, multiSheetIndex);
      }

      return list;
   }

   @Override
   public String getAppName() {
      return "webShell";
   }

   private static boolean isEnabled(BWebProfileConfig config, String key) {
      BIBoolean val = (BIBoolean)config.get(key);
      return val == null ? ((BIBoolean)INSTANCE.getConfig(key)).getBoolean() : val.getBoolean();
   }

   @Override
   public String[] listConfig() {
      List<String> list = new ArrayList<>();
      String[] configList = super.listConfig();
      if (configList != null) {
         Collections.addAll(list, configList);
      }

      list.addAll(configOptions);
      return list.toArray(new String[0]);
   }

   @Override
   public BValue getConfig(String key) {
      return (BValue)(configOptions.contains(key) ? BBoolean.TRUE : super.getConfig(key));
   }

   @Override
   public BFacets getConfigFacets(String key) {
      return configOptions.contains(key) ? yesNoFacets : super.getConfigFacets(key);
   }

   static {
      keyToClassName.put("navTreeSideBar", "icon-icons-x16-listView");
      keyToClassName.put("searchSideBar", "icon-icons-x16-magnifyingGlass");
      keyToClassName.put("paletteSideBar", "icon-icons-x16-openPalette");
      configOptions.add("hxWbViews");
      configOptions.addAll(sideBarConfigOptions);
      configOptions.addAll(treeConfigOptions);
      configOptions.add("viewSelection");
   }

   private class Html5BenchmarkCommand extends BenchmarkCommand {
      private Html5BenchmarkCommand() {
      }

      @Override
      public String getInvokeCode(HxOp op) {
         return "fullScreenUtils.loadTime(\"" + this.getId() + "\");";
      }
   }
}
