package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.app.AppRegistry;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.util.KeyedList;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;

public class AppListServlet extends DaemonServlet {
   public AppListServlet() {
      super("applist");
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      content.w("<apps>\n");
      AppRegistry stationRegistry = NiagaraDaemon.getInstance().getStationRegistry();
      if (stationRegistry != null) {
         content.w("  <app ").attr("type", "station").w(">\n");
         stationRegistry.listApps(content);
         content.w("</app>\n");
      }

      content.w("</apps>\n");
      return 200;
   }
}
