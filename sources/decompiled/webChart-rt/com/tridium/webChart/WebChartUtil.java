package com.tridium.webChart;

import com.tridium.bql.util.BDynamicTimeRange;
import com.tridium.bql.util.BDynamicTimeRangeType;
import com.tridium.json.JSONException;
import com.tridium.json.JSONWriter;
import java.util.logging.Logger;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BTrendFlags;
import javax.baja.history.BTrendRecord;
import javax.baja.naming.ViewQuery;
import javax.baja.nav.BINavNode;
import javax.baja.space.BSpace;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BDouble;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFloat;
import javax.baja.sys.BINumeric;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.BObject;
import javax.baja.sys.BStation;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.LocalizableException;
import javax.baja.util.BAbsTimeRange;

public final class WebChartUtil {
   private static final Logger logger = Logger.getLogger("webChart");

   private WebChartUtil() {
   }

   public static String getNavDisplayPath(BObject o, Context cx) throws Exception {
      String path = "";
      if (o instanceof BINavNode) {
         for (BINavNode navNode = (BINavNode)o;
            navNode != null && !(navNode instanceof BStation) && !(navNode instanceof BSpace);
            navNode = navNode.getNavParent()
         ) {
            if (path.length() == 0) {
               path = navNode.getNavDisplayName(cx);
            } else {
               path = navNode.getNavDisplayName(cx) + "/" + path;
            }
         }
      }

      return path;
   }

   public static boolean encodeMinifiedGenericRecord(JSONWriter out, BComplex c, Context cx) throws Exception {
      return encodeMinifiedGenericRecord(out, c, BAbsTime.DEFAULT, BAbsTime.DEFAULT, cx);
   }

   public static boolean encodeMinifiedGenericRecord(JSONWriter out, BComplex c, BAbsTime start, BAbsTime end, Context cx) throws Exception {
      BAbsTime timestamp = (BAbsTime)c.get("timestamp");
      BTrendFlags trendFlags = (BTrendFlags)c.get("trendFlags");
      BStatus status = (BStatus)c.get("status");
      BValue value = c.get("value");
      return encodeMinifiedGenericValueRecord(out, timestamp, value, trendFlags, status, start, end, cx);
   }

   public static boolean encodeMinifiedGenericValueRecord(
      JSONWriter out, BAbsTime timestamp, BValue value, BTrendFlags trendFlags, BStatus status, BAbsTime start, BAbsTime end, Context cx
   ) throws Exception {
      if (value != null && (value.getType().is(BEnum.TYPE) || value.getType().is(BINumeric.TYPE))) {
         if (timestamp == null) {
            throw new LocalizableException("webChart", "webChart.msg.timestampRequired");
         } else if (start != BAbsTime.DEFAULT && start.isAfter(timestamp)) {
            return false;
         } else if (end != BAbsTime.DEFAULT && end.isBefore(timestamp)) {
            return false;
         } else {
            encodeMinifiedEntry(out, timestamp, value, trendFlags, status, cx);
            return true;
         }
      } else {
         throw new LocalizableException("webChart", "webChart.msg.valueRequired");
      }
   }

   public static void encodeMinifiedHistoryRecord(JSONWriter out, BHistoryRecord entry, Context cx) throws Exception {
      if (entry instanceof BTrendRecord) {
         BTrendRecord trendEntry = (BTrendRecord)entry;
         BAbsTime timestamp = trendEntry.getTimestamp();
         BValue value = trendEntry.get(trendEntry.getValueProperty());
         BTrendFlags trendFlags = trendEntry.getTrendFlags();
         BStatus status = trendEntry.getStatus();
         encodeMinifiedEntry(out, timestamp, value, trendFlags, status, cx);
      } else {
         encodeMinifiedGenericRecord(out, entry, cx);
      }
   }

   public static void encodeMinifiedEntry(JSONWriter out, BAbsTime timestamp, BValue value, BTrendFlags trendFlags, BStatus status, Context cx) throws Exception {
      out.object();
      out.key("t").value(timestamp.encodeToString());
      out.key("v");
      writeMinifiedValue(out, value, cx);
      if (trendFlags != null && !trendFlags.equals(BTrendFlags.DEFAULT)) {
         out.key("r").value(trendFlags.getBits());
      }

      if (status != null && !status.equals(BStatus.DEFAULT)) {
         out.key("s").value(status.getBits());
      }

      out.endObject();
   }

   public static void writeMinifiedValue(JSONWriter out, BValue value, Context cx) throws Exception {
      try {
         if (value instanceof BDouble) {
            out.value(((BDouble)value).getDouble());
         } else if (value instanceof BFloat) {
            out.value(((BFloat)value).getFloat());
         } else if (value instanceof BInteger) {
            out.value(((BInteger)value).getInt());
         } else if (value instanceof BLong) {
            out.value(((BLong)value).getLong());
         } else if (value instanceof BEnum) {
            out.value(((BEnum)value).getOrdinal());
         } else {
            out.value(value.asSimple().encodeToString());
         }
      } catch (JSONException var4) {
         if (!"JSON does not allow non-finite numbers.".equals(var4.getMessage())) {
            throw var4;
         }

         out.value(value.asSimple().encodeToString());
      }
   }

   public static BAbsTimeRange getAbsTimeRangeFromViewOrd(ViewQuery viewQuery, BAbsTime start, BAbsTime end) throws Exception {
      if (viewQuery != null) {
         String periodParam = viewQuery.getParameter("period", null);
         String startParam = viewQuery.getParameter("start", null);
         String endParam = viewQuery.getParameter("end", null);
         if (startParam != null) {
            start = (BAbsTime)BAbsTime.DEFAULT.decodeFromString(startParam);
         }

         if (endParam != null) {
            end = (BAbsTime)BAbsTime.DEFAULT.decodeFromString(endParam);
         }

         if (periodParam != null) {
            BAbsTime now = BAbsTime.now();
            BDynamicTimeRange timeRange = BDynamicTimeRange.make(BDynamicTimeRangeType.make(periodParam));
            start = timeRange.getStartTime(now);
            end = timeRange.getEndTime(now);
         }
      }

      return new BAbsTimeRange(start, end);
   }
}
