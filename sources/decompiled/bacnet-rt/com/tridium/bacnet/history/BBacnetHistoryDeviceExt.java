package com.tridium.bacnet.history;

import com.tridium.bacnet.job.BBacnetDiscoverTrendLogsJob;
import com.tridium.driver.util.StringUtil;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetDate;
import javax.baja.bacnet.datatypes.BBacnetDateTime;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTime;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.control.trigger.BManualTriggerMode;
import javax.baja.driver.history.BHistoryDeviceExt;
import javax.baja.history.BHistoryId;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Cursor;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.timezone.BTimeZone;
import javax.baja.timezone.TimeZoneDatabase;

@NiagaraType
@NiagaraActions({@NiagaraAction(
      name = "submitTrendLogDiscoveryJob",
      returnType = "BOrd",
      flags = 4
   ), @NiagaraAction(
      name = "getImportTypes",
      returnType = "BString",
      flags = 4
   )})
public class BBacnetHistoryDeviceExt extends BHistoryDeviceExt implements BacnetConst {
   public static final Action submitTrendLogDiscoveryJob = newAction(4, null);
   public static final Action getImportTypes = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetHistoryDeviceExt.class);
   public static final Logger logger = Logger.getLogger("bacnet.history");
   private BTimeZone timeZone = BTimeZone.getLocal();
   private boolean timeZoneChecked = false;

   public BOrd submitTrendLogDiscoveryJob() {
      return (BOrd)this.invoke(submitTrendLogDiscoveryJob, null, null);
   }

   public BString getImportTypes() {
      return (BString)this.invoke(getImportTypes, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetDevice;
   }

   public void added(Property p, Context c) {
      super.added(p, c);
      if (this.isRunning()) {
         BObject o = this.get(p);
         if (o instanceof BBacnetHistoryImport) {
            BBacnetHistoryImport himp = (BBacnetHistoryImport)o;
            if (himp.getExecutionTime().getTriggerMode() != BManualTriggerMode.DEFAULT) {
               ((BBacnetHistoryImport)o).execute();
            }
         }
      }
   }

   public void subscribed() {
      logger.fine("Determining time zone for history imports");
      this.getTimeZone();
   }

   public BOrd doSubmitTrendLogDiscoveryJob(Context cx) {
      return this.device().isFatalFault() ? null : new BBacnetDiscoverTrendLogsJob(this).submit(cx);
   }

   public BString doGetImportTypes() {
      TypeInfo[] types = Sys.getRegistry().getConcreteTypes(BBacnetHistoryImport.TYPE.getTypeInfo());
      String[] infos = new String[types.length];

      for (int i = 0; i < infos.length; i++) {
         infos[i] = types[i].toString();
      }

      String list = StringUtil.toString(infos, ";");
      return BString.make(list);
   }

   public final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   public Cursor<BHistoryId> getHistoryIds(BOrd ord) {
      throw new UnsupportedOperationException("Unsupported operation BBacnetHistoryDeviceExt.getHistoryIds()");
   }

   public Type getImportDescriptorType() {
      return BBacnetHistoryImport.TYPE;
   }

   public Type getExportDescriptorType() {
      return null;
   }

   public final BBacnetHistoryImport[] findImportDescriptors(BBacnetObjectIdentifier objectId) {
      SlotCursor<Property> c = this.getProperties();
      ArrayList<BBacnetHistoryImport> a = new ArrayList<>();

      while (c.next(BBacnetHistoryImport.class)) {
         BBacnetHistoryImport pt = (BBacnetHistoryImport)c.get();
         if (pt.getObjectId().equals(objectId)) {
            a.add(pt);
         }
      }

      return a.toArray(new BBacnetHistoryImport[0]);
   }

   private static BTimeZone getTimeZone(int utcOffsetInMinutesReversed, boolean daylightSavingsStatus, BAbsTime deviceBAbsTime) {
      int utcOffset = (int)(-utcOffsetInMinutesReversed * 60000L);
      BTimeZone[] allTZs = TimeZoneDatabase.get().getTimeZones();

      for (int i = 0; i < allTZs.length; i++) {
         BTimeZone tz = allTZs[i];
         if (tz.getUtcOffset() == utcOffset) {
            BAbsTime deviceBAbsTimeInTz = BAbsTime.make(deviceBAbsTime, tz);
            boolean deviceInDSTime = deviceBAbsTimeInTz.inDaylightTime();
            if (daylightSavingsStatus == deviceInDSTime) {
               return tz;
            }
         }
      }

      if (logger.isLoggable(Level.FINE)) {
         logger.fine("no time zone found.");
      }

      return null;
   }

   public BTimeZone getTimeZone() {
      BBacnetDeviceObject deviceObject = this.device().getConfig().getDeviceObject();
      if (!this.timeZoneChecked) {
         try {
            BInteger utcOff = (BInteger)deviceObject.readBacnetProperty(BBacnetPropertyIdentifier.utcOffset);
            BBoolean dss = (BBoolean)deviceObject.readBacnetProperty(BBacnetPropertyIdentifier.daylightSavingsStatus);
            if (utcOff != null) {
               BBacnetDate deviceDate = (BBacnetDate)deviceObject.readBacnetProperty(BBacnetPropertyIdentifier.localDate);
               BBacnetTime deviceTime = (BBacnetTime)deviceObject.readBacnetProperty(BBacnetPropertyIdentifier.localTime);
               BAbsTime deviceBAbsTime = BBacnetDateTime.makeBAbsTime(deviceDate, deviceTime);
               BTimeZone tz = getTimeZone(utcOff.getInt(), dss.getBoolean(), deviceBAbsTime);
               this.timeZone = tz;
               this.timeZoneChecked = true;
            } else {
               this.timeZoneChecked = true;
            }
         } catch (Exception var8) {
            logger.warning("Exception reading time zone info from device:" + var8);
         }

         if (this.timeZone == null) {
            logger.warning("TimeZone determined from TimeZoneDatabase and device values is null, resetting to default!");
            this.timeZone = BTimeZone.getLocal();
         }
      }

      return this.timeZone;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetHistoryDeviceExt", 2);
      out.prop("timeZone", this.timeZone);
      out.prop("timeZoneChecked", this.timeZoneChecked);
      out.endProps();
   }
}
