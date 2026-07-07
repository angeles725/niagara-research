package com.tridium.webChart;

import com.tridium.history.BHistoryDeltaQuery;
import com.tridium.history.BHistoryTimeQuery;
import com.tridium.history.db.BLocalHistoryDatabase;
import com.tridium.history.util.HistoryUtil;
import com.tridium.json.JSONWriter;
import com.tridium.json.quick.QuickJSONWriter;
import com.tridium.nre.util.Version;
import com.tridium.web.RestUtil;
import com.tridium.web.WebUtil;
import com.tridium.web.RestUtil.Accept;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.baja.collection.BITable;
import javax.baja.collection.Column;
import javax.baja.collection.Row;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistoryService;
import javax.baja.history.BIHistory;
import javax.baja.history.HistoryCursor;
import javax.baja.history.db.HistoryDatabaseConnection;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.SlotPath;
import javax.baja.naming.ViewQuery;
import javax.baja.schedule.BControlSchedule;
import javax.baja.security.PermissionException;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Cursor;
import javax.baja.util.BAbsTimeRange;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class WebChartQueryServlet extends HttpServlet {
   private static final Pattern dataPattern = Pattern.compile("/data/.+");
   private static final Pattern schedulePattern = Pattern.compile("/schedule/.+");
   private static final Pattern boxTablePattern = Pattern.compile("/boxTable/.+");
   private static final Logger log = Logger.getLogger("webChart");
   private static final Version version = new Version("1");

   protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
      resp.setHeader("transfer-encoding", "chunked");

      try {
         Context cx = (Context)req.getAttribute("niagara.context");
         RestUtil util = new RestUtil(req, resp);
         if (!util.acceptJson()) {
            resp.sendError(406);
            return;
         }

         Accept accept = util.getJsonAccept();
         if (accept.isCustomMediaType() && (!accept.getCustomProtocol().equals("webChart") || !accept.getCustomVersion().equals(version))) {
            resp.sendError(406);
            return;
         }

         if (!util.matches(dataPattern) && !util.matches(schedulePattern)) {
            util.respond(accept, "application/json").respondNoCache();
         } else {
            util.respond(accept, "text/plain").respondNoCache();
         }

         Writer writer = null;
         if (util.matches(dataPattern)) {
            String escapedOrd = util.get(1);
            BOrd ord = BOrd.make(SlotPath.unescape(escapedOrd)).relativizeToSession().normalize();
            if (ord == null) {
               resp.sendError(404);
               return;
            }

            writer = this.getWriter(resp);
            this.encodeHistoryData(writer, ord, cx);
         } else if (util.matches(schedulePattern)) {
            String escapedOrd = util.get(1);
            BOrd ord = BOrd.make(SlotPath.unescape(escapedOrd)).relativizeToSession().normalize();
            ViewQuery viewQuery = ord.resolve().getViewQuery();
            BAbsTime end = BAbsTime.now();
            BAbsTime start = BAbsTime.make(end.getYear(), end.getMonth(), end.getDay());
            BAbsTimeRange range = WebChartUtil.getAbsTimeRangeFromViewOrd(viewQuery, start, end);
            OrdTarget target = ord.resolve(null, cx);
            if (!target.canRead()) {
               resp.sendError(404);
            }

            BControlSchedule schedule = (BControlSchedule)target.get();
            writer = this.getWriter(resp);
            this.encodeSchedule(writer, schedule, range.getStartTime(), range.getEndTime(), cx);
         } else if (util.matches(boxTablePattern)) {
            String escapedOrd = util.get(1);
            BOrd ord = BOrd.make(SlotPath.unescape(escapedOrd)).relativizeToSession().normalize();
            ViewQuery viewQuery = ord.resolve().getViewQuery();
            BAbsTime end = BAbsTime.DEFAULT;
            BAbsTime start = BAbsTime.DEFAULT;
            BAbsTimeRange range = WebChartUtil.getAbsTimeRangeFromViewOrd(viewQuery, start, end);
            OrdTarget target = ord.resolve(null, cx);
            if (!target.canRead()) {
               resp.sendError(404);
            }

            BITable<?> dataTable = (BITable<?>)target.get();
            writer = this.getWriter(resp);
            this.encodeTableData(writer, dataTable, range.getStartTime(), range.getEndTime(), cx);
         } else {
            resp.sendError(404);
         }
      } catch (Exception var15) {
         if (WebUtil.isAbortException(var15)) {
            log.log(Level.FINE, "Connection Aborted", (Throwable)var15);
            return;
         }

         log.log(Level.SEVERE, "ServletException", (Throwable)var15);
         if (!(var15 instanceof ServletException) && !(var15 instanceof IOException)) {
            throw new ServletException(var15.getMessage(), var15);
         }
      }
   }

   public void encodeHistoryData(Writer out, BOrd ord, Context cx) throws Exception {
      OrdTarget target = ord.resolve(null, cx);
      if (!target.canRead()) {
         throw new PermissionException();
      } else {
         BObject historyObject = target.get();
         Cursor<BHistoryRecord> historyCursor = this.retrieveHistoryEntries(historyObject);
         Throwable var7 = null;

         try {
            BFacets cursorFacets = null;
            Context cursorContext = historyCursor.getContext();
            boolean archiveLimitExceeded = HistoryCursor.archiveLimitExceeded(cursorContext);
            if (archiveLimitExceeded) {
               HistoryUtil.writeArchiveLimitExceededWarning(out, historyObject);
            }

            if (cursorContext != null) {
               cursorFacets = cursorContext.getFacets();
            }

            boolean isFirstLineWritten = false;
            if (cursorFacets != null) {
               BHistoryRecord preRecord = HistoryCursor.extractPreRecord(cursorFacets);
               if (preRecord != null) {
                  JSONWriter json = QuickJSONWriter.make(out);
                  WebChartUtil.encodeMinifiedHistoryRecord(json, preRecord, cx);
                  isFirstLineWritten = true;
               }
            }

            while (historyCursor.next()) {
               if (!isFirstLineWritten) {
                  isFirstLineWritten = true;
               } else {
                  out.append('\n');
               }

               JSONWriter json = QuickJSONWriter.make(out);
               WebChartUtil.encodeMinifiedHistoryRecord(json, (BHistoryRecord)historyCursor.get(), cx);
            }

            if (cursorFacets != null) {
               BHistoryRecord postRecord = HistoryCursor.extractPostRecord(cursorFacets);
               if (postRecord != null) {
                  if (isFirstLineWritten) {
                     out.append('\n');
                  }

                  JSONWriter json = QuickJSONWriter.make(out);
                  WebChartUtil.encodeMinifiedHistoryRecord(json, postRecord, cx);
               }
            }
         } catch (Throwable var21) {
            var7 = var21;
            throw var21;
         } finally {
            if (historyCursor != null) {
               if (var7 != null) {
                  try {
                     historyCursor.close();
                  } catch (Throwable var20) {
                     var7.addSuppressed(var20);
                  }
               } else {
                  historyCursor.close();
               }
            }
         }
      }
   }

   private Cursor<BHistoryRecord> retrieveHistoryEntries(BObject o) throws Exception {
      if (o instanceof BIHistory) {
         BLocalHistoryDatabase db = new BLocalHistoryDatabase((BHistoryService)null);
         HistoryDatabaseConnection conn = db.getDbConnection(null);
         Throwable var4 = null;

         Cursor var5;
         try {
            var5 = conn.scan((BIHistory)o);
         } catch (Throwable var14) {
            var4 = var14;
            throw var14;
         } finally {
            if (conn != null) {
               if (var4 != null) {
                  try {
                     conn.close();
                  } catch (Throwable var13) {
                     var4.addSuppressed(var13);
                  }
               } else {
                  conn.close();
               }
            }
         }

         return var5;
      } else if (o instanceof BHistoryTimeQuery) {
         return ((BHistoryTimeQuery)o).cursor();
      } else if (o instanceof BHistoryDeltaQuery) {
         return ((BHistoryDeltaQuery)o).cursor();
      } else {
         throw new Exception("servlet not available for history of type " + o.getType());
      }
   }

   public void encodeScheduleEntry(JSONWriter out, BAbsTime time, BValue value, Context cx) throws Exception {
      BStatusValue statusValue = (BStatusValue)value;
      WebChartUtil.encodeMinifiedEntry(out, time, statusValue.getValueValue(), null, statusValue.getStatus(), cx);
   }

   public void encodeSchedule(Writer writer, BControlSchedule schedule, BAbsTime start, BAbsTime end, Context cx) throws Exception {
      JSONWriter json = QuickJSONWriter.make(writer);
      this.encodeScheduleEntry(json, start, schedule.getOutput(start), cx);
      writer.append('\n');
      BAbsTime time = start;

      while (time != null && time.isBefore(end)) {
         time = schedule.nextEvent(time);
         if (time != null && time.isBefore(end)) {
            json = QuickJSONWriter.make(writer);
            this.encodeScheduleEntry(json, time, schedule.getOutput(time), cx);
            writer.append('\n');
         }
      }

      json = QuickJSONWriter.make(writer);
      this.encodeScheduleEntry(json, end, schedule.getOutput(end), cx);
   }

   public void encodeTableData(Writer out, BITable<?> table, BAbsTime start, BAbsTime end, Context cx) throws Exception {
      Column timestamp = table.getColumns().get("timestamp");
      Column value = table.getColumns().get("value");
      Cursor<?> cursor = table.cursor();
      Throwable var9 = null;

      try {
         boolean first = false;
         boolean skip = false;

         while (cursor.next()) {
            if (!skip) {
               if (!first) {
                  first = true;
               } else {
                  out.append('\n');
               }
            }

            JSONWriter json = QuickJSONWriter.make(out);
            Object entry = cursor.get();
            boolean used = false;
            if (entry instanceof Row) {
               Row<?> row = (Row<?>)entry;
               skip = !WebChartUtil.encodeMinifiedGenericValueRecord(json, (BAbsTime)row.cell(timestamp), (BValue)row.cell(value), null, null, start, end, cx);
               used = true;
            } else if (entry instanceof BComponent) {
               BComponent c = (BComponent)entry;
               BValue t = c.get("timestamp");
               BValue v = c.get("value");
               if (t instanceof BAbsTime && v instanceof BValue) {
                  BAbsTime timestampValue = (BAbsTime)t;
                  skip = !WebChartUtil.encodeMinifiedGenericValueRecord(json, timestampValue, v, null, null, start, end, cx);
                  used = true;
               }
            }

            if (!used) {
               throw new IllegalStateException(
                  "Cursor.get() must return a javax.baja.collection.Row or javax.baja.sys.BComponent with timestamp and value properties: " + entry.getClass()
               );
            }
         }
      } catch (Throwable var26) {
         var9 = var26;
         throw var26;
      } finally {
         if (cursor != null) {
            if (var9 != null) {
               try {
                  cursor.close();
               } catch (Throwable var25) {
                  var9.addSuppressed(var25);
               }
            } else {
               cursor.close();
            }
         }
      }
   }

   private Writer getWriter(HttpServletResponse resp) throws IOException {
      return resp.getWriter();
   }
}
