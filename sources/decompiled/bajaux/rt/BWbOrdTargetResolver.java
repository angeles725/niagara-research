package com.tridium.ux;

import com.tridium.file.util.BInputStreamFileStore;
import com.tridium.file.util.SupplierInputStream;
import com.tridium.web.BIOrdTargetResolver;
import com.tridium.web.RequireJsUtil;
import com.tridium.web.js.NiagaraRequireJsMapper;
import java.io.IOException;
import java.io.InputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.baja.file.FilePath;
import javax.baja.file.types.text.BCssFile;
import javax.baja.file.types.text.BJavascriptFile;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.nre.annotations.NiagaraSingleton;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.security.BPermissions;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BSingleton;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.web.js.JsInfo;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@NiagaraType
@NiagaraSingleton
public final class BWbOrdTargetResolver extends BSingleton implements BIOrdTargetResolver {
   public static final BWbOrdTargetResolver INSTANCE = new BWbOrdTargetResolver();
   public static final Type TYPE = Sys.loadType(BWbOrdTargetResolver.class);
   private static final BOrd profileCss = BOrd.make("module://webEditors/rc/wb/profile/profile.css");
   private static final BOrd bajauxContainerCss = BOrd.make("module://bajaux/rc/container/container.css");
   private static final BOrd baseThemeCss = BOrd.make("module://web/rc/theme/theme.css");
   private static final Pattern cssModuleIdPattern = Pattern.compile("^module://(.+)\\.css$", 2);
   private static final Pattern uriPattern = Pattern.compile("^.*/(app.(js|css))$");
   private static final Map<String, Collection<Supplier<InputStream>>> MODULE_TO_ADD_JS;
   private static final Map<String, List<String>> MODULE_TO_CSS_ORDS;
   private static final String[] REQUIRED_JS_MODULES;
   private static final String[] REQUIRED_CSS_MODULES;

   public Type getType() {
      return TYPE;
   }

   private BWbOrdTargetResolver() {
   }

   public OrdTarget resolve(HttpServletRequest request, HttpServletResponse response) throws IOException {
      return resolve(request, BOrd.NULL, BOrd.NULL, BOrd.NULL, BOrd.NULL);
   }

   public static OrdTarget resolve(HttpServletRequest request, BOrd coreJs, BOrd coreCss, BOrd containerJs, BOrd containerCss) throws IOException {
      return resolve(request, coreJs, coreCss, containerJs, containerCss, null);
   }

   public static OrdTarget resolve(HttpServletRequest request, BOrd coreJs, BOrd coreCss, BOrd containerJs, BOrd containerCss, Runnable runnable) throws IOException {
      request.setAttribute("snoop", "false");
      String pathInfo = Objects.toString(request.getPathInfo(), "");
      Matcher matcher = uriPattern.matcher(pathInfo);
      if (!matcher.matches()) {
         return null;
      } else {
         String fileName = matcher.group(1);
         BAbsTime lastRegBuildTime = Sys.getRegistry().getLastBuildTime();
         return fileName.toLowerCase().endsWith(".css")
            ? OrdTarget.unmounted(
               new BCssFile(
                  new BInputStreamFileStore(
                     new FilePath(fileName),
                     lastRegBuildTime,
                     BPermissions.all,
                     () -> {
                        if (runnable != null) {
                           runnable.run();
                        }

                        return new SupplierInputStream(
                           Arrays.asList(
                              add(coreCss),
                              add(bajauxContainerCss),
                              add(containerCss),
                              add(baseThemeCss),
                              add(profileCss),
                              () -> new SupplierInputStream(getAllRequiredCss().map(BWbOrdTargetResolver::add).collect(Collectors.toList()))
                           )
                        );
                     }
                  )
               )
            )
            : OrdTarget.unmounted(
               new BJavascriptFile(
                  new BInputStreamFileStore(
                     new FilePath(fileName),
                     lastRegBuildTime,
                     BPermissions.all,
                     () -> {
                        if (runnable != null) {
                           runnable.run();
                        }

                        Stream<Supplier<InputStream>> cssStubs = Stream.concat(
                              Stream.of(coreCss, bajauxContainerCss, containerCss, baseThemeCss, profileCss), getAllRequiredCss()
                           )
                           .map(BWbOrdTargetResolver::stubCss)
                           .map(BWbOrdTargetResolver::add);
                        Stream<Supplier<InputStream>> allJsAndCss = (Stream<Supplier<InputStream>>)Stream.of(
                              getAllRequiredJs(),
                              Stream.of(add(coreJs), addDefine(containerJs, containerJs.isNull() ? "" : JsInfo.toRequireJsId(containerJs))),
                              cssStubs
                           )
                           .reduce(Stream::concat)
                           .orElseGet(Stream::empty);
                        return new SupplierInputStream(allJsAndCss.collect(Collectors.toList()));
                     }
                  )
               )
            );
      }
   }

   private static Supplier<InputStream> wrapDefine(BOrd ord, String id) {
      return () -> new SupplierInputStream(
         Arrays.asList(
            () -> SupplierInputStream.toInputStream(
               "(function(){\nvar define=function(){var args=Array.prototype.slice.call(arguments);if(args.length === 1){return window.define.apply(this,['"
                  + id
                  + "'].concat(args));} else {return typeof args[0] === 'string' ? window.define.apply(this, args) : window.define.apply(this,['"
                  + id
                  + "'].concat(args));"
                  + '}'
                  + "};define.amd=true;"
            ),
            () -> SupplierInputStream.toInputStream(ord),
            () -> SupplierInputStream.toInputStream("}());")
         )
      );
   }

   private static Supplier<InputStream> add(BOrd ord) {
      return () -> SupplierInputStream.toInputStream(ord);
   }

   private static Supplier<InputStream> addDefine(BOrd ord, String id) {
      return ord.isNull()
         ? SupplierInputStream::toEmptyStream
         : () -> new SupplierInputStream(
            Arrays.asList(() -> SupplierInputStream.toInputStream(ord), () -> SupplierInputStream.toInputStream("define('" + id + "',[],undefined);"))
         );
   }

   private static Supplier<InputStream> add(String str) {
      return () -> SupplierInputStream.toInputStream(str);
   }

   private static String stubCss(BOrd cssOrd) {
      if (cssOrd.isNull()) {
         return "";
      } else {
         Matcher cssMatcher = cssModuleIdPattern.matcher(cssOrd.toString());
         if (cssMatcher.find()) {
            String cssPath = cssMatcher.group(1);
            String cssId = NiagaraRequireJsMapper.getInstance().pathToRequireJsModuleId(cssPath);
            return "define('css!" + cssId + "',[],undefined);";
         } else {
            throw new BajaRuntimeException("Illegal module ORD: " + cssOrd);
         }
      }
   }

   private static Stream<Supplier<InputStream>> getAllRequiredJs() {
      return Arrays.stream(REQUIRED_JS_MODULES).map(moduleName -> MODULE_TO_ADD_JS.get(moduleName)).filter(Objects::nonNull).flatMap(Collection::stream);
   }

   private static Stream<BOrd> getAllRequiredCss() {
      return Arrays.stream(REQUIRED_CSS_MODULES)
         .map(moduleName -> MODULE_TO_CSS_ORDS.get(moduleName))
         .filter(Objects::nonNull)
         .flatMap(Collection::stream)
         .<BOrd>map(BOrd::make)
         .filter(o -> !o.isNull());
   }

   private static String[] split(String str) {
      return str.trim().split(",\\s*");
   }

   static {
      Map<String, Collection<Supplier<InputStream>>> moduleToJs = new HashMap<>();
      moduleToJs.put(
         "js",
         Arrays.asList(
            add(BOrd.make("module://js/rc/d3/d3.min.js")),
            add(BOrd.make("module://js/com/tridium/js/ext/require/require.min.js")),
            add("define('d3', [], d3);"),
            add(BOrd.make("module://js/rc/jquery/jquery.min.js")),
            add("jQuery.noConflict();"),
            RequireJsUtil.USE_NATIVE_PROMISES ? add("") : wrapDefine(BOrd.make("module://js/rc/bluebird/bluebird.min.js"), "Promise"),
            add(BOrd.make("module://js/rc/js.built.min.js")),
            add("define('nmodule/js/rc/js.built.min', [], function () {});"),
            wrapDefine(BOrd.make("module://js/com/tridium/js/ext/require/css.js"), "css"),
            wrapDefine(
               BOrd.make("module://js/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown.js"),
               "nmodule/js/rc/jquery/Relevant-Dropdowns/js/jquery.relevant-dropdown"
            ),
            wrapDefine(BOrd.make("module://js/rc/jquery/contextMenu/jquery.contextMenu.min.js"), "nmodule/js/rc/jquery/contextMenu/jquery.contextMenu"),
            wrapDefine(BOrd.make("module://js/rc/jquery/contextMenu/jquery.ui.position.min.js"), "nmodule/js/rc/jquery/contextMenu/jquery.ui.position"),
            wrapDefine(BOrd.make("module://js/rc/handlebars/handlebars.min.js"), "Handlebars"),
            add(BOrd.make("module://js/rc/require-handlebars-plugin/hbs.built.min.js")),
            add(BOrd.make("module://js/rc/jquery/split-pane/split-pane.js")),
            add("define('nmodule/js/rc/jquery/split-pane/split-pane', [], undefined);"),
            add(BOrd.make("module://js/rc/underscore/underscore.min.js")),
            wrapDefine(BOrd.make("module://js/rc/moment/moment.min.js"), "moment"),
            wrapDefine(BOrd.make("module://js/rc/pikaday/pikaday.js"), "nmodule/js/rc/pikaday/pikaday")
         )
      );
      moduleToJs.put("alarm", Collections.singletonList(addDefine(BOrd.make("module://alarm/rc/alarm.built.min.js"), "nmodule/alarm/rc/alarm.built.min")));
      moduleToJs.put(
         "bajaScript",
         Arrays.asList(
            add(BOrd.make("module://bajaScript/rc/bs.built.min.js")),
            add("define('bajaScript/bs.built.min', [], undefined);"),
            wrapDefine(BOrd.make("module://bajaScript/rc/plugin/baja.js"), "baja")
         )
      );
      moduleToJs.put("bajaui", Collections.singletonList(addDefine(BOrd.make("module://bajaui/rc/bajaui.built.min.js"), "nmodule/bajaui/rc/bajaui.built.min")));
      moduleToJs.put("bajaux", Collections.singletonList(addDefine(BOrd.make("module://bajaux/rc/bajaux.built.min.js"), "bajaux/bajaux.built.min")));
      moduleToJs.put("bql", Collections.singletonList(addDefine(BOrd.make("module://bql/rc/bql.built.min.js"), "nmodule/bql/rc/bql.built.min")));
      moduleToJs.put(
         "control", Collections.singletonList(addDefine(BOrd.make("module://control/rc/control.built.min.js"), "nmodule/control/rc/control.built.min"))
      );
      moduleToJs.put(
         "converters",
         Collections.singletonList(addDefine(BOrd.make("module://converters/rc/converters.built.min.js"), "nmodule/converters/rc/converters.built.min"))
      );
      moduleToJs.put("export", Collections.singletonList(addDefine(BOrd.make("module://export/rc/export.built.min.js"), "nmodule/export/rc/export.built.min")));
      moduleToJs.put("gx", Collections.singletonList(addDefine(BOrd.make("module://gx/rc/gx.built.min.js"), "nmodule/gx/rc/gx.built.min")));
      moduleToJs.put(
         "hierarchy",
         Collections.singletonList(addDefine(BOrd.make("module://hierarchy/rc/hierarchy.built.min.js"), "nmodule/hierarchy/rc/hierarchy.built.min"))
      );
      moduleToJs.put(
         "history", Collections.singletonList(addDefine(BOrd.make("module://history/rc/history.built.min.js"), "nmodule/history/rc/history.built.min"))
      );
      moduleToJs.put(
         "kitControl",
         Collections.singletonList(addDefine(BOrd.make("module://kitControl/rc/kitControl.built.min.js"), "nmodule/kitControl/rc/kitControl.built.min"))
      );
      moduleToJs.put("kitPx", Collections.singletonList(addDefine(BOrd.make("module://kitPx/rc/kitPx.built.min.js"), "nmodule/kitPx/rc/kitPx.built.min")));
      moduleToJs.put("search", Collections.singletonList(addDefine(BOrd.make("module://search/rc/search.built.min.js"), "nmodule/search/rc/search.built.min")));
      moduleToJs.put("web", Collections.singletonList(wrapDefine(BOrd.make("module://web/rc/util/activityMonitor.js"), "nmodule/web/rc/util/activityMonitor")));
      moduleToJs.put(
         "webChart", Collections.singletonList(addDefine(BOrd.make("module://webChart/rc/webChart.built.min.js"), "nmodule/webChart/rc/webChart.built.min"))
      );
      moduleToJs.put(
         "webEditors",
         Collections.singletonList(addDefine(BOrd.make("module://webEditors/rc/webEditors.built.min.js"), "nmodule/webEditors/rc/webEditors.built.min"))
      );
      Map<String, Collection<Supplier<InputStream>>> orderedMap = new LinkedHashMap<>();

      for (String moduleName : Arrays.asList(
         "js",
         "web",
         "bajaScript",
         "bajaux",
         "export",
         "converters",
         "webEditors",
         "gx",
         "bajaui",
         "bql",
         "history",
         "webChart",
         "hierarchy",
         "search",
         "control",
         "alarm",
         "kitControl",
         "kitPx"
      )) {
         orderedMap.put(moduleName, moduleToJs.get(moduleName));
      }

      if (!orderedMap.keySet().equals(moduleToJs.keySet())) {
         throw new IllegalArgumentException("When adding modules to app.js, ensure that ordering is respected");
      } else {
         MODULE_TO_ADD_JS = orderedMap;
         moduleToJs = new HashMap<>();
         moduleToJs.put(
            "alarm",
            Arrays.asList(
               "module://alarm/com/tridium/alarm/hx/alarm.css",
               "module://alarm/rc/console/templates/alarmConsole.css",
               "module://alarm/rc/db/templates/view.css",
               "module://alarm/rc/fe/alarmEditors.css"
            )
         );
         moduleToJs.put("bajaui", Collections.singletonList("module://bajaui/rc/bajaui.css"));
         moduleToJs.put("bql", Collections.singletonList("module://bql/rc/bql.css"));
         moduleToJs.put("gx", Collections.singletonList("module://gx/rc/gx.css"));
         moduleToJs.put("hierarchy", Collections.singletonList("module://hierarchy/rc/hierarchy.css"));
         moduleToJs.put("history", Collections.singletonList("module://history/rc/history.css"));
         moduleToJs.put("icons", Collections.singletonList("module://icons/sprite/sprite.css"));
         moduleToJs.put(
            "js",
            Arrays.asList(
               "module://js/rc/dialogs/dialogs.css",
               "module://js/rc/jquery/split-pane/split-pane.css",
               "module://js/rc/pikaday/pikaday.css",
               "module://js/rc/jquery/contextMenu/jquery.contextMenu.css",
               "module://js/rc/jquery/Relevant-Dropdowns/css/style.css"
            )
         );
         moduleToJs.put("kitPx", Collections.singletonList("module://kitPx/rc/kitPx.css"));
         moduleToJs.put("search", Collections.singletonList("module://search/rc/search.css"));
         moduleToJs.put(
            "webChart",
            Arrays.asList(
               "module://webChart/rc/gauge/CircularGaugeWidgetStyle.css",
               "module://webChart/rc/fe/color/colorChooserStyle.css",
               "module://webChart/rc/line/LineStyle.css",
               "module://webChart/rc/ChartWidgetStyle.css"
            )
         );
         moduleToJs.put(
            "webEditors",
            Arrays.asList(
               "module://webEditors/rc/wb/mixin/PropertySheetRowDragSupport.css",
               "module://webEditors/rc/fe/webEditors-structure.css",
               "module://webEditors/rc/wb/composite/style/CompositeEditor.css",
               "module://webEditors/rc/wb/tree/NavTreeStyle.css",
               "module://webEditors/rc/wb/mixin/PropertySheetRowLinkSupport.css",
               "module://webEditors/rc/wb/mixin/PropertySheetDragSupport.css"
            )
         );
         moduleToJs.put("wiresheet", Collections.singletonList("module://wiresheet/rc/wiresheet.css"));
         orderedMap = new LinkedHashMap<>();

         for (String moduleName : Arrays.asList(
            "icons", "js", "webEditors", "gx", "history", "webChart", "hierarchy", "search", "alarm", "wiresheet", "bajaui", "kitPx", "bql"
         )) {
            orderedMap.put(moduleName, moduleToJs.get(moduleName));
         }

         if (!orderedMap.keySet().equals(moduleToJs.keySet())) {
            throw new IllegalArgumentException("When adding modules to app.css, ensure that ordering is respected");
         } else {
            MODULE_TO_CSS_ORDS = orderedMap;
            String jsProp = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.web.bundle.includeJsModules")));
            String cssProp = AccessController.doPrivileged((PrivilegedAction<String>)(() -> System.getProperty("niagara.web.bundle.includeCssModules")));
            if (jsProp == null) {
               REQUIRED_JS_MODULES = MODULE_TO_ADD_JS.keySet().toArray(new String[0]);
            } else {
               REQUIRED_JS_MODULES = split(jsProp);
            }

            if (cssProp == null) {
               REQUIRED_CSS_MODULES = MODULE_TO_CSS_ORDS.keySet().toArray(new String[0]);
            } else {
               REQUIRED_CSS_MODULES = split(cssProp);
            }
         }
      }
   }
}
