package com.tridium.ux;

import com.tridium.json.JSONObject;
import com.tridium.json.JSONUtil;
import com.tridium.net.HttpUtil;
import com.tridium.session.NiagaraSuperSession;
import com.tridium.session.SessionManager;
import com.tridium.web.RequireJsUtil;
import com.tridium.web.session.NiagaraWebSession;
import com.tridium.web.session.WebSessionUtil;
import java.io.IOException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.baja.bajaux.BBajauxJsBuild;
import javax.baja.file.BFileSystem;
import javax.baja.file.BIFile;
import javax.baja.io.HtmlWriter;
import javax.baja.naming.BLocalHost;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.naming.ViewQuery;
import javax.baja.nre.util.TextUtil;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.web.BFormFactorEnum;
import javax.baja.web.WebDev;
import javax.baja.web.js.BIJavaScript;
import javax.baja.web.js.BIWebResource;
import javax.baja.web.js.BJsBuild;
import javax.baja.web.js.JsInfo;
import javax.baja.web.js.BIWebResource.DependencyGraph;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class WbWebWidgetServlet extends HttpServlet {
   public static final String themeNameKey = "theme";
   public static final String useLocalWbRcKey = "useLocalWbRc";
   public static final String attachAfterInitKey = "attachAfterInit";
   private static final boolean forceUseAsyncTimeout = AccessController.doPrivileged(
      (PrivilegedAction<Boolean>)(() -> Boolean.getBoolean("niagara.forceAsyncTimeout"))
   );
   private static final Pattern ordPattern = Pattern.compile("([^/]+:.+)$");

   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      String pathInfo = req.getPathInfo();
      if (pathInfo != null && !pathInfo.isEmpty() && !pathInfo.equals("/")) {
         String uri = pathInfo;
         if (pathInfo.startsWith("/")) {
            uri = pathInfo.substring(1, pathInfo.length());
         }

         Matcher matcher = ordPattern.matcher(uri);
         if (!matcher.find()) {
            resp.sendError(404, "Invalid ORD!");
         } else {
            uri = matcher.group(1);
            if (req.getQueryString() != null && uri.startsWith("view:")) {
               uri = uri + "?" + req.getQueryString();
            }

            BOrd jsOrd = BOrd.make(HttpUtil.decodeUrl(uri));
            Context cx = (Context)req.getAttribute("niagara.context");
            WbWebWidgetServlet.WebWidgetInitInfo info = getWebWidgetInitInfo(jsOrd, cx);
            if (info == null) {
               resp.sendError(404);
            } else {
               JSONObject params = null;
               Map<String, String[]> paramMap = req.getParameterMap();
               if (paramMap != null && paramMap.size() > 0) {
                  Set<String> keys = paramMap.keySet();
                  String[] paramNames = keys.toArray(new String[0]);
                  String[] values = new String[paramNames.length];

                  for (int i = 0; i < paramNames.length; i++) {
                     String[] vals = paramMap.get(paramNames[i]);
                     values[i] = vals != null && vals.length > 0 ? vals[0] : "";
                  }

                  params = paramsToJsonObject(paramNames, values);
               }

               resp.setContentType("text/html");
               HtmlWriter out = null;

               try {
                  out = new HtmlWriter(resp.getWriter());
                  out.w("<!DOCTYPE html>").nl();
                  out.w("<html>").nl();
                  out.w("<head>").nl();
                  out.w("<title>").w(info.getDisplayName()).w("</title>").nl();
                  boolean isJavaFx = Objects.toString(req.getHeader("User-Agent"), "").contains("JavaFX");
                  writeNiagaraAsyncFunction(out, isJavaFx);
                  boolean useLocalWbRc = params != null && params.getBoolean("useLocalWbRc");
                  boolean bindResources = !useLocalWbRc
                     && !WebDev.get("js").isEnabled()
                     && !WebDev.get("search").isEnabled()
                     && !WebDev.get("webChart").isEnabled()
                     && !WebDev.get("webEditors").isEnabled()
                     && !WebDev.get("bajaScript").isEnabled()
                     && !WebDev.get("hierarchy-ux").isEnabled();
                  String themeName = params != null && params.has("theme") ? JSONUtil.getString(params, "theme") : "";
                  RequireJsUtil requireJsUtil = RequireJsUtil.make(useLocalWbRc, cx);
                  if (bindResources) {
                     long lastBuildTime = Sys.getRegistry().getLastBuildTime().getMillis();
                     String typeSpec = BWbOrdTargetResolver.TYPE.toString().replace(":", "%3A");
                     out.println("<script type='text/javascript'>");
                     requireJsUtil.requirejsNoHtml(out);
                     out.println("</script>");
                     out.w(String.format("<link rel='stylesheet' type='text/css' href='/vfile/wb/app.css?typeSpec=%s&version=%d'/>", typeSpec, lastBuildTime));
                     out.w(String.format("<script type='text/javascript' src='/vfile/wb/app.js?typeSpec=%s&version=%d'></script>", typeSpec, lastBuildTime));
                     out.println("<script type='text/javascript'>");
                     out.println(requireJsUtil.defineSystemProperties());
                     out.println("</script>");
                  } else {
                     requireJsUtil.requirejs(out);
                     String moduleStr = RequireJsUtil.getModulePrefix(useLocalWbRc);
                     out.w("<link rel='stylesheet' type='text/css' href='").w(moduleStr).w("bajaux/rc/container/container.css' />").nl();
                     out.w("<link rel='stylesheet' type='text/css' href='").w(moduleStr).w("web/rc/theme/theme.css' />").nl();
                  }

                  BFormFactorEnum formFactor = BFormFactorEnum.max;
                  boolean attachAfterInit = false;
                  if (params != null) {
                     if (params.has("formFactor")) {
                        formFactor = (BFormFactorEnum)BFormFactorEnum.DEFAULT.getRange().get(JSONUtil.getString(params, "formFactor"));
                     }

                     if (!themeName.isEmpty()) {
                        BOrd ord = UxUtil.checkThemeOrd(themeName, cx);
                        if (!ord.isNull()) {
                           out.w("<link rel='stylesheet' type='text/css' href='")
                              .w(ord.toString().replaceAll("module://", RequireJsUtil.getModulePrefix(useLocalWbRc)))
                              .w("' />")
                              .nl();
                        }

                        NiagaraWebSession session = WebSessionUtil.getSession(req, true);
                        if (session != null) {
                           session.setAttribute("themeName", themeName);
                        }
                     }

                     if (params.has("attachAfterInit")) {
                        attachAfterInit = params.getBoolean("attachAfterInit");
                     }
                  }

                  out.w("<script type='text/javascript'>").nl();
                  out.w("'use strict';").nl();
                  out.w("window.wbLoaded = function () {").nl();
                  out.w("if (window.wbLoadedInvoked) { return; }").nl();
                  out.w("window.wbLoadedInvoked = true;").nl();
                  NiagaraSuperSession superSession = SessionManager.getCurrentNiagaraSuperSession();
                  new NiagaraEnv(NiagaraEnv.EnvType.WORKBENCH)
                     .withCustomProperty("useLocalWbRc", useLocalWbRc)
                     .withCustomProperty("modulePrefix", RequireJsUtil.getModulePrefix(useLocalWbRc))
                     .withCustomProperty("csrfToken", superSession == null ? null : superSession.getCsrfToken())
                     .toJavaScript(out);
                  this.writeContainerInitScript(out, info, formFactor, params, isJavaFx, attachAfterInit);
                  out.w("};").nl();
                  out.w("if (window.wbHasLoaded) { window.wbLoaded(); }").nl();
                  out.w("</script>").nl();
                  out.w("</head>").nl();
                  out.w("<body class='niagara-env-wb" + (!themeName.isEmpty() ? " " + themeName : "") + "'>").nl();
                  out.w("<div id='bajaux-container' class='bajaux-container ux-root ux-fullscreen-support'>").nl();
                  out.w("  <div id='bajaux-toolbar-outer' class='bajaux-toolbar-outer'>").nl();
                  out.w("    <div id='bajaux-toolbar' class='bajaux-toolbar'></div>").nl();
                  out.w("  </div>").nl();
                  out.w("  <div id='bajaux-widget-outer' class='bajaux-widget-container'>").nl();
                  out.w("    <div id='bajaux-widget' class='bajaux-widget'></div>").nl();
                  out.w("  </div>").nl();
                  out.w("  <div id='bajaux-error' class='bajaux-error'></div>").nl();
                  out.w("</div>").nl();
                  out.w("</body>").nl();
                  out.w("</html>");
               } finally {
                  if (out != null) {
                     out.flush();
                     out.close();
                  }
               }
            }
         }
      } else {
         resp.sendError(404, "No path info found!");
      }
   }

   private void writeContainerInitScript(
      HtmlWriter out, WbWebWidgetServlet.WebWidgetInitInfo info, BFormFactorEnum formFactor, JSONObject params, boolean isJavaFx, boolean attachAfterInit
   ) {
      RequireJsUtil.withRequiredBuiltFiles(
         out,
         () -> {
            out.w("require(['Promise', 'bajaux/container/wb/wbContainer', '").w(info.getJsId()).w("'], ");
            out.w("function (Promise, container, Widget) {").nl();
            out.w("  if (Widget.__esModule) { Widget = Widget.default || Widget; }").nl();
            if (isJavaFx) {
               out.w("typeof Promise.setScheduler === 'function' && Promise.setScheduler(window.niagaraAsync);").nl();
            }

            out.w(
                  String.join(
                     "\n",
                     "(function () {",
                     "  var FUNCTIONS_THAT_RETURN_PROMISES = [ 'hyperlink', 'toHyperlink', 'reload', 'updateTransformOptions' ];",
                     "  function createNamespace(name, func) {",
                     "    var res = name.split('_'),",
                     "        i,",
                     "        token,",
                     "        obj = window;",
                     "",
                     "    for (i = 0; i < res.length; ++i) {",
                     "      token = res[i];",
                     "      if (i === res.length - 1) {",
                     "        if (FUNCTIONS_THAT_RETURN_PROMISES.includes(token)) {",
                     "          obj[token] = function () {",
                     "            var that = this;",
                     "            var args = Array.prototype.slice.call(arguments);",
                     "            return Promise.resolve()",
                     "              .then(function () { return func.apply(that, args); });",
                     "          }",
                     "        } else {",
                     "          obj[token] = func;",
                     "        }",
                     "      } else {",
                     "        obj[token] = obj[token] || {};",
                     "      }",
                     "      obj = obj[token];",
                     "    }",
                     "  }",
                     "",
                     "  var p;",
                     "  for (p in window) {",
                     "    if (window.hasOwnProperty(p) && ",
                     "        /^niagara_.*/.test(p) && ",
                     "        typeof window[p] === 'function') {",
                     "      createNamespace(p, window[p]);",
                     "    }",
                     "  }",
                     "}());"
                  )
               )
               .nl();
            out.w("  container.initializeContainer({attachAfterInit:" + attachAfterInit + "});").nl();
            String[] widgetConstructorParams = new String[]{"'" + info.getModuleName() + "'", "'" + info.getTypeName() + "'", "'" + formFactor.getTag() + "'"};
            out.w("  container.initialize(").w("new Widget(").w(String.join(", ", widgetConstructorParams)).w("), ").w(params).w(");").nl();
            out.w("});").nl();
         },
         info.resolveDependencies(BBajauxJsBuild.INSTANCE)
      );
   }

   private static void writeNiagaraAsyncFunction(HtmlWriter out, boolean isJavaFx) {
      if (isJavaFx) {
         out.w("<script type='text/javascript'>").nl().w("window.niagaraAsync = (function () {\n");
         if (forceUseAsyncTimeout) {
            out.w("return function (func) {\n  setTimeout(func, 0);\n};\n");
         } else {
            out.w(
               "  var queue = [],\n      messageName = \"niagara-async-post-message\";\n\n  function handleMessage(event) {\n    if (event.source === window && event.data === messageName) {\n      event.stopPropagation();\n      if (queue.length > 0) {\n        queue.shift()();\n      }\n    }\n  }\n\n  window.addEventListener(\"message\", handleMessage, true);\n\n  return function (func) {\n    queue.push(func);\n    window.postMessage(messageName, \"*\");\n  };\n"
            );
         }

         out.w("}());").nl().w("</script>").nl();
      }
   }

   public static WbWebWidgetServlet.WebWidgetInitInfo getWebWidgetInitInfo(BOrd jsOrd, Context cx) throws IOException {
      WbWebWidgetServlet.WebWidgetInitInfo info = null;
      if (jsOrd.isNull()) {
         return null;
      } else {
         OrdTarget target = jsOrd.resolve(BLocalHost.INSTANCE, cx);
         BObject obj = target.get();
         if (obj instanceof BIFile && ((BIFile)obj).getFileSpace() == BFileSystem.INSTANCE) {
            info = new WbWebWidgetServlet.WebWidgetInitInfo();
            BIFile file = (BIFile)obj;
            info.jsId = "/file/" + TextUtil.join(file.getFilePath().getNames(), '/') + "?lm=" + file.getLastModified().getMillis();
         } else if (target.getViewQuery() != null) {
            TypeInfo widgetTypeInfo = Sys.getRegistry().getType(target.getViewQuery().getViewId());
            if (widgetTypeInfo.is(BIJavaScript.TYPE)) {
               info = getWebWidgetInitInfo((BIJavaScript)widgetTypeInfo.getInstance(), cx);
            }
         }

         if (info != null) {
            info.params = getParameters(target);
         }

         return info;
      }
   }

   public static WbWebWidgetServlet.WebWidgetInitInfo getWebWidgetInitInfo(BIJavaScript js, Context cx) {
      WbWebWidgetServlet.WebWidgetInitInfo info = new WbWebWidgetServlet.WebWidgetInitInfo();
      TypeInfo typeInfo = js.getType().getTypeInfo();
      info.displayName = typeInfo.getDisplayName(cx);
      info.moduleName = typeInfo.getModuleName();
      info.typeName = typeInfo.getTypeName();
      JsInfo jsInfo = js.getJsInfo(cx);
      info.buildId = jsInfo.getBuildId();
      info.jsId = jsInfo.getJsId();
      return info;
   }

   public static JSONObject getParameters(OrdTarget target) throws IOException {
      ViewQuery query = target.getViewQuery();
      String[] paramNames = null;
      String[] values = null;
      if (query != null) {
         String[] names = query.getParameterNames();
         if (names != null && names.length > 0) {
            paramNames = names;
            values = new String[names.length];

            for (int i = 0; i < paramNames.length; i++) {
               values[i] = query.getParameter(paramNames[i]);
            }
         }
      }

      return paramNames == null ? null : paramsToJsonObject(paramNames, values);
   }

   private static JSONObject paramsToJsonObject(String[] paramNames, String[] values) {
      JSONObject obj = new JSONObject();

      for (int i = 0; i < paramNames.length; i++) {
         obj.put(SlotPath.unescape(paramNames[i]), values[i]);
      }

      return obj;
   }

   public static final class WebWidgetInitInfo {
      private String displayName = "";
      private String moduleName = "";
      private String typeName = "";
      private String jsId = "";
      private String buildId;
      private JSONObject params;

      public String getDisplayName() {
         return this.displayName;
      }

      public String getModuleName() {
         return this.moduleName;
      }

      public String getTypeName() {
         return this.typeName;
      }

      public String getJsId() {
         return this.jsId;
      }

      public JSONObject getParameters() {
         return this.params;
      }

      public Optional<String> getBuildId() {
         return Optional.ofNullable(this.buildId);
      }

      public DependencyGraph resolveDependencies(BIWebResource... additionalDependencies) {
         Collection<BIWebResource> builds = this.getBuildId().<BJsBuild>flatMap(BJsBuild::forId).map(build -> {
            List<BIWebResource> resources = new ArrayList<>();
            resources.add(build);
            resources.addAll(Arrays.asList(additionalDependencies));
            return resources;
         }).orElseGet(() -> Arrays.asList(additionalDependencies));
         return BIWebResource.resolve(builds);
      }
   }
}
