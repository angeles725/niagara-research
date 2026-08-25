package com.tridium.niagarad.servlet;

import com.tridium.niagarad.NiagaraDaemon;
import com.tridium.niagarad.http.Http;
import com.tridium.niagarad.log.ErrorHandler;
import com.tridium.niagarad.log.MessageBundle;
import com.tridium.niagarad.util.CsrfTokenUtil;
import com.tridium.niagarad.util.DaemonAuthUtil;
import com.tridium.niagarad.util.KeyedList;
import com.tridium.nre.platform.IPlatformProvider;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.baja.xml.XWriter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class TimeServlet extends DaemonServlet {
   private final IPlatformProvider platformProvider;

   public TimeServlet(IPlatformProvider platformProvider) {
      super("time");
      this.platformProvider = platformProvider;
   }

   @Override
   public int doGet(HttpServletRequest request, ErrorHandler handler, KeyedList query, XWriter content) {
      if (query == null || !query.containsKey("update")) {
         return sendTime(content, this.platformProvider);
      } else if (!DebugServlet.debugEnabled && !CsrfTokenUtil.verifyCsrfToken(request, query.get("csrfToken", null))) {
         MessageBundle msg = new MessageBundle("invalid CSRF token in request");
         handler.error(msg);
         return 403;
      } else {
         return update(handler, query, this.platformProvider);
      }
   }

   @Override
   public boolean useDefaultAuthentication() {
      return false;
   }

   @Override
   public boolean authenticate(HttpServletRequest req, HttpServletResponse resp) {
      boolean requireAdmin = false;
      String queryString = req.getQueryString();
      if (queryString != null) {
         KeyedList query = Http.getGetForm(queryString);
         requireAdmin = query.containsKey("update");
      }

      return requireAdmin
         ? DaemonAuthUtil.authAdmin(this.getServer().getAuthenticator(), req, resp)
         : DaemonAuthUtil.authUser(this.getServer().getAuthenticator(), req, resp);
   }

   private static int update(ErrorHandler handler, KeyedList query, IPlatformProvider platformProvider) {
      if (platformProvider.isSystemTimeReadonly()) {
         MessageBundle msg = new MessageBundle("TimeServlet: System time readonly");
         handler.error(msg);
         return 400;
      }

      if (query != null && query.containsKey("tzId")) {
         String tzId = query.get("tzId", null);
         if (tzId != null && TimeZone.getTimeZone(tzId) != null) {
            if (NiagaraDaemon.getFilter().isLoggable(Level.FINEST)) {
               NiagaraDaemon.getFilter().finest("setting system time zone to '" + tzId + "' [defaultTimeZone='" + TimeZone.getDefault().getID() + "']");
            }

            if (platformProvider.setNativeTimeZone(tzId) != 0) {
               MessageBundle msg = new MessageBundle("TimeServlet: Failed to set system time zone");
               handler.error(msg);
               return 500;
            }

            NiagaraDaemon.getTimeZoneId(true);
            if (NiagaraDaemon.getFilter().isLoggable(Level.FINEST)) {
               NiagaraDaemon.getFilter().finest("system time zone set to '" + tzId + "' [defaultTimeZone='" + TimeZone.getDefault().getID() + "']");
            }
         }
      }

      if (query != null && query.containsKey("time")) {
         String millisString = query.get("time", "-1");

         long millis;
         try {
            millis = Long.parseLong(millisString);
         } catch (NumberFormatException nfe) {
            MessageBundle msg = new MessageBundle("TimeServlet: Invalid system time value \"" + millisString + "\" specified");
            handler.error(msg);
            return 400;
         }

         if (millis > 0L && platformProvider.setSystemTime(millis) != 0) {
            MessageBundle msg = new MessageBundle("TimeServlet: Failed to set system time");
            handler.error(msg);
            return 500;
         }
      }

      return 200;
   }

   private static int sendTime(XWriter content, IPlatformProvider platformProvider) {
      long systemTime = System.currentTimeMillis();
      Calendar calendar = new GregorianCalendar();
      calendar.setTime(new Date(systemTime));
      int year = calendar.get(1);
      int month = calendar.get(2);
      int day = calendar.get(5);
      int hour = calendar.get(11);
      int minute = calendar.get(12);
      int second = calendar.get(13);
      boolean readonly = platformProvider.isSystemTimeReadonly();
      content.w("<currentTime")
         .w(' ')
         .attr("readonly", String.valueOf(readonly))
         .w(' ')
         .attr("year", String.valueOf(year))
         .w(' ')
         .attr("month", String.valueOf(month))
         .w(' ')
         .attr("day", String.valueOf(day))
         .w(' ')
         .attr("hour", String.valueOf(hour))
         .w(' ')
         .attr("minute", String.valueOf(minute))
         .w(' ')
         .attr("second", String.valueOf(second))
         .w(' ')
         .attr("millis", String.valueOf(systemTime))
         .w(' ')
         .attr("timeZone", NiagaraDaemon.getTimeZoneId())
         .w("/>\n");
      return 200;
   }
}
