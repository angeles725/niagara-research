package com.tridium.webChart;

import com.tridium.history.BHistory;
import com.tridium.json.JSONObject;
import com.tridium.json.JSONWriter;
import com.tridium.json.quick.QuickJSONWriter;
import java.io.StringWriter;
import java.util.List;
import javax.baja.file.BIFile;
import javax.baja.history.BHistoryRecord;
import javax.baja.history.BHistorySpace;
import javax.baja.history.BIHistory;
import javax.baja.history.BTrendRecord;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdTarget;
import javax.baja.naming.UnresolvedException;
import javax.baja.nav.BINavNode;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.rpc.NiagaraRpc;
import javax.baja.rpc.Transport;
import javax.baja.rpc.TransportType;
import javax.baja.schedule.BControlSchedule;
import javax.baja.security.BIProtected;
import javax.baja.security.BPermissions;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BObject;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;

@NiagaraType
public final class BWebChartQueryRpc extends BComponent {
   public static final Type TYPE = Sys.loadType(BWebChartQueryRpc.class);

   public Type getType() {
      return TYPE;
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getCurrentTime(Context cx) {
      JSONObject obj = new JSONObject();
      obj.put("currentTime", BAbsTime.now().encodeToString());
      return obj;
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getSourceList(String ordString, Context cx) {
      BOrd ord = BOrd.NULL;
      if (ordString != null) {
         ord = BOrd.make(ordString);
      }

      StringWriter writer = new StringWriter();
      BHistorySpace historySpace = (BHistorySpace)BOrd.make("history:").get(null, cx);
      if (!historySpace.getPermissions(cx).hasOperatorRead()) {
         return null;
      } else {
         Array<BINavNode> navNodes = new Array(BINavNode.class);
         Array<String> a = new Array(String.class);
         if (ord.isNull()) {
            navNodes.addAll(historySpace.listDevices());
         } else {
            BObject o = ord.get(null, cx);
            if (o instanceof BINavNode) {
               BINavNode folder = (BINavNode)o;
               navNodes.addAll(folder.getNavChildren());
            }
         }

         for (BINavNode navNode : (BINavNode[])navNodes.trim()) {
            if (navNode instanceof BIHistory) {
               BIHistory history = (BIHistory)navNode;
               if (history.getRecordType().getResolvedType().is(BTrendRecord.TYPE) && history.getPermissions(cx).hasOperatorRead()) {
                  a.add(history.getNavOrd().toString());
               }
            }

            for (BINavNode navChild : navNode.getNavChildren()) {
               if (navChild instanceof BIHistory) {
                  BIHistory history = (BIHistory)navChild;
                  if (history.getRecordType().getResolvedType().is(BTrendRecord.TYPE) && history.getPermissions(cx).hasOperatorRead()) {
                     a.add(history.getNavOrd().toString());
                  }
               }
            }
         }

         JSONWriter out = QuickJSONWriter.make(writer);
         out.object();
         out.key("sources").array();

         for (String value : (String[])a.trim()) {
            out.value(value);
         }

         out.endArray();
         out.endObject();
         return new JSONObject(writer.toString());
      }
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getInfo(List<String> args, Context cx) throws Exception {
      StringWriter writer = new StringWriter();
      Array<String> displayPaths = new Array(String.class);
      Array<String> capacities = new Array(String.class);
      Array<String> valueFacets = new Array(String.class);
      Array<String> recordTypes = new Array(String.class);
      Array<String> timeZoneIds = new Array(String.class);

      for (int i = 0; i < args.size(); i++) {
         String string = args.get(i);
         if (!string.equals("")) {
            BOrd ord = BOrd.make(string).relativizeToSession().normalize();
            BObject o = ord.get(null, cx);
            if (o instanceof BIProtected && !((BIProtected)o).getPermissions(cx).hasOperatorRead()) {
               return null;
            }

            if (o instanceof BHistory) {
               BHistory history = (BHistory)o;
               capacities.add("" + history.getConfig().getCapacity().getMaxRecords());
               BHistoryRecord record = history.getConfig().makeRecord();
               if (record instanceof BTrendRecord) {
                  BTrendRecord trendRecord = (BTrendRecord)record;
                  BFacets facets = (BFacets)history.getConfig().get(trendRecord.getValueProperty().getName() + "Facets");
                  if (facets != null) {
                     valueFacets.add(facets.encodeToString());
                  } else {
                     valueFacets.add("");
                  }

                  recordTypes.add("" + trendRecord.getValueProperty().getType());
               }

               timeZoneIds.add(history.getConfig().getTimeZone().getId());
            } else if (o instanceof BControlSchedule) {
               BControlSchedule schedule = (BControlSchedule)o;
               BStatusValue statusValue = (BStatusValue)schedule.getProperty("out").getType().getInstance();
               recordTypes.add("" + statusValue.getValueValue().getType());
               BFacets facets = schedule.getFacets();
               if (facets != null) {
                  valueFacets.add(facets.encodeToString());
               } else {
                  valueFacets.add("");
               }
            }

            displayPaths.add(WebChartUtil.getNavDisplayPath(o, cx));
         }
      }

      JSONWriter out = QuickJSONWriter.make(writer);
      out.object();
      out.key("capacities").array();

      for (int ix = 0; ix < capacities.size(); ix++) {
         out.value(capacities.get(ix));
      }

      out.endArray();
      out.key("valueFacets").array();

      for (int ix = 0; ix < valueFacets.size(); ix++) {
         out.value(valueFacets.get(ix));
      }

      out.endArray();
      out.key("recordTypes").array();

      for (int ix = 0; ix < recordTypes.size(); ix++) {
         out.value(recordTypes.get(ix));
      }

      out.endArray();
      out.key("displayPaths").array();

      for (int ix = 0; ix < displayPaths.size(); ix++) {
         out.value(displayPaths.get(ix));
      }

      out.endArray();
      out.key("timezoneIDs").array();

      for (int ix = 0; ix < timeZoneIds.size(); ix++) {
         out.value(timeZoneIds.get(ix));
      }

      out.endArray();
      out.endObject();
      return new JSONObject(writer.toString());
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static String getStationsTimeZoneId(Context cx) {
      return BTimeZone.getLocal().getId();
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getPermissions(String ordString, Context cx) throws Exception {
      StringWriter writer = new StringWriter();
      BOrd ord = BOrd.make(ordString);
      JSONWriter out = QuickJSONWriter.make(writer);
      out.object();
      out.key("permissions");
      BPermissions permissions = getPermissionsOnOrd(ord, cx);
      out.value(permissions.encodeToString());
      out.endObject();
      return new JSONObject(writer.toString());
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getDisplayPath(String path, Context cx) throws Exception {
      StringWriter writer = new StringWriter();
      BOrd ord = BOrd.make(path).relativizeToSession().normalize();
      BObject o = ord.get(null, cx);
      if (o instanceof BIProtected && !((BIProtected)o).getPermissions(cx).hasOperatorRead()) {
         return null;
      } else {
         JSONWriter out = QuickJSONWriter.make(writer);
         out.object();
         out.key("displayPath");
         out.value(WebChartUtil.getNavDisplayPath(o, cx));
         out.endObject();
         return new JSONObject(writer.toString());
      }
   }

   @NiagaraRpc(
      permissions = "unrestricted",
      transports = {@Transport(
         type = TransportType.box
      ), @Transport(
         type = TransportType.web
      )}
   )
   public static JSONObject getChartSettings(String ordString, Context cx) {
      try {
         BOrd ord = BOrd.make(ordString);
         BIFile file = (BIFile)ord.get(null, cx);
         return new JSONObject(new String(file.read()));
      } catch (Exception var4) {
         return null;
      }
   }

   static BPermissions getPermissionsOnOrd(BOrd ord, Context cx) {
      try {
         OrdTarget target = ord.resolve(null, cx);
         return target.getPermissionsForTarget();
      } catch (UnresolvedException var4) {
         BOrd parentPath = ord.getParent();
         if (parentPath != null) {
            return getPermissionsOnOrd(parentPath, cx);
         }
      } catch (Exception var5) {
         var5.printStackTrace();
      }

      return BPermissions.none;
   }
}
