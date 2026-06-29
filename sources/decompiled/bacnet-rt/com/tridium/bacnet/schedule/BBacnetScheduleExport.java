package com.tridium.bacnet.schedule;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.job.BBacnetScheduleTypeChangeJob;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.io.ErrorException;
import javax.baja.driver.schedule.BScheduleDeviceExt;
import javax.baja.driver.schedule.BScheduleExport;
import javax.baja.driver.util.BDescriptor;
import javax.baja.driver.util.BDescriptorState;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraActions;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BControlSchedule;
import javax.baja.schedule.BCustomSchedule;
import javax.baja.schedule.BDailySchedule;
import javax.baja.schedule.BDaySchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BTimeSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "supervisorId",
      type = "String",
      defaultValue = "",
      flags = 5,
      override = true
   ), @NiagaraProperty(
      name = "subordinateVersion",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 5,
      override = true
   ), @NiagaraProperty(
      name = "supervisorOrd",
      type = "BOrd",
      defaultValue = "BOrd.DEFAULT",
      facets = {@Facet(
         name = "BFacets.TARGET_TYPE",
         value = "\"schedule:AbstractSchedule\""
      )}
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.SCHEDULE)",
      facets = {@Facet("ScheduleSupport0.SCHEDULE_CALENDAR_OBJECT_ID_FACETS")}
   ), @NiagaraProperty(
      name = "dataType",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "priorityForWriting",
      type = "int",
      defaultValue = "16",
      facets = {@Facet("BFacets.makeInt(1,16)")}
   ), @NiagaraProperty(
      name = "skipWrites",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT"
   ), @NiagaraProperty(
      name = "writeEnumAs",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(0, ENUM_DATA_TYPE_RANGE)"
   ), @NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false"
   )})
@NiagaraActions({@NiagaraAction(
      name = "readFromDevice",
      flags = 16
   ), @NiagaraAction(
      name = "readChangeTypeParams",
      returnType = "BBacnetChangeTypeParm",
      flags = 4
   ), @NiagaraAction(
      name = "changeType",
      parameterType = "BBacnetChangeTypeParm",
      defaultValue = "new BBacnetChangeTypeParm()",
      returnType = "BOrd",
      flags = 4
   )})
public class BBacnetScheduleExport extends BScheduleExport {
   private static final BEnumRange ENUM_DATA_TYPE_RANGE = BEnumRange.make(
      new String[]{AsnUtil.getAsnTypeName(2), AsnUtil.getAsnTypeName(9), AsnUtil.getAsnTypeName(3)}
   );
   public static final Property supervisorId = newProperty(5, "", null);
   public static final Property subordinateVersion = newProperty(5, BAbsTime.NULL, null);
   public static final Property supervisorOrd = newProperty(0, BOrd.DEFAULT, BFacets.make("targetType", "schedule:AbstractSchedule"));
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.make(17), ScheduleSupport0.SCHEDULE_CALENDAR_OBJECT_ID_FACETS);
   public static final Property dataType = newProperty(1, "", null);
   public static final Property priorityForWriting = newProperty(0, 16, BFacets.makeInt(1, 16));
   public static final Property skipWrites = newProperty(0, BFacets.DEFAULT, null);
   public static final Property writeEnumAs = newProperty(0, BDynamicEnum.make(0, ENUM_DATA_TYPE_RANGE), null);
   public static final Property outOfService = newProperty(0, false, null);
   public static final Action readFromDevice = newAction(16, null);
   public static final Action readChangeTypeParams = newAction(4, null);
   public static final Action changeType = newAction(4, new BBacnetChangeTypeParm(), null);
   public static final Type TYPE = Sys.loadType(BBacnetScheduleExport.class);
   private int asnType = -1;
   BEnum writeProperty = null;
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static final Logger logger = Logger.getLogger("bacnet.schedule");

   public BOrd getSupervisorOrd() {
      return (BOrd)this.get(supervisorOrd);
   }

   public void setSupervisorOrd(BOrd v) {
      this.set(supervisorOrd, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   public String getDataType() {
      return this.getString(dataType);
   }

   public void setDataType(String v) {
      this.setString(dataType, v, null);
   }

   public int getPriorityForWriting() {
      return this.getInt(priorityForWriting);
   }

   public void setPriorityForWriting(int v) {
      this.setInt(priorityForWriting, v, null);
   }

   public BFacets getSkipWrites() {
      return (BFacets)this.get(skipWrites);
   }

   public void setSkipWrites(BFacets v) {
      this.set(skipWrites, v, null);
   }

   public BEnum getWriteEnumAs() {
      return (BEnum)this.get(writeEnumAs);
   }

   public void setWriteEnumAs(BEnum v) {
      this.set(writeEnumAs, v, null);
   }

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   public void readFromDevice() {
      this.invoke(readFromDevice, null, null);
   }

   public BBacnetChangeTypeParm readChangeTypeParams() {
      return (BBacnetChangeTypeParm)this.invoke(readChangeTypeParams, null, null);
   }

   public BOrd changeType(BBacnetChangeTypeParm parameter) {
      return (BOrd)this.invoke(changeType, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetScheduleExport() {
   }

   public BBacnetScheduleExport(String sid) {
      this.setSupervisorId(sid);
   }

   public void started() throws Exception {
      super.started();
      this.setAsnType();
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (p.equals(supervisorOrd)) {
         this.setSupervisorId(this.getSupervisorOrd().toString());
         this.setDataType("");
      }

      if (p.equals(dataType)) {
         this.setAsnType();
      }
   }

   public String toString(Context cx) {
      return "BacnetScheduleExport_" + this.getObjectId().toString(cx);
   }

   public BAbstractSchedule getSupervisor() {
      BAbstractSchedule sch = null;

      try {
         sch = (BAbstractSchedule)this.getSupervisorOrd().resolve(this).get();
      } catch (Exception var3) {
         throw new BajaRuntimeException(this.getSupervisorId(), var3);
      }

      if (sch == null) {
         throw new IllegalArgumentException("Cannot resolve: " + this.getSupervisorId());
      } else {
         return sch;
      }
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      if (action == BDescriptor.execute) {
         if (this.isDisabled()) {
            return null;
         } else if (this.getState() != BDescriptorState.idle) {
            return null;
         } else if (this.getSupervisorOrd().equals(BOrd.DEFAULT)) {
            return null;
         } else if (objectId.isEquivalentToDefaultValue(this.getObjectId())) {
            return null;
         } else if (BScheduleDeviceExt.getVersionOf(this.getSupervisor()).equals(this.getSubordinateVersion())) {
            return null;
         } else {
            this.setLastAttempt(Clock.time());
            this.setState(BDescriptorState.pending);

            try {
               return this.postExecute(action, arg, cx);
            } catch (Exception var5) {
               this.executeFail(var5);
               return null;
            }
         }
      } else {
         return action == readFromDevice ? BBacnetNetwork.bacnet().postAsync(new Invocation(this, action, arg, cx)) : super.post(action, arg, cx);
      }
   }

   protected IFuture postExecute(Action action, BValue arg, Context cx) {
      return BBacnetNetwork.bacnet().postAsync(new Invocation(this, action, arg, cx));
   }

   public void doExecute() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BBacnetScheduleExport.doExecute on " + this);
      }

      BBacnetScheduleDeviceExt deviceExt = (BBacnetScheduleDeviceExt)this.getDeviceExt();
      this.executeInProgress();

      try {
         this.validateSchedule();
         deviceExt.writeRemote(this);
         this.setSubordinateVersion(BAbsTime.now());
         this.executeOk();
         this.getDevice().pingOk();
      } catch (BacnetException var7) {
         logger.log(
            Level.WARNING,
            "BacnetException writing supervisor schedule data for "
               + this.getSupervisor().getName()
               + " to "
               + this.getObjectId()
               + " in "
               + this.getDevice()
               + ": "
               + var7,
            (Throwable)var7
         );
         this.executeFail((this.writeProperty != null ? this.writeProperty.getTag() : "null") + "::" + var7.toString());
      } catch (Exception var8) {
         logger.log(
            Level.WARNING,
            "Exception writing supervisor schedule data for "
               + this.getSupervisor().getName()
               + " to "
               + this.getObjectId()
               + " in "
               + this.getDevice()
               + ": "
               + var8,
            (Throwable)var8
         );
         this.executeFail(var8);
      } finally {
         this.setState(BDescriptorState.idle);
      }
   }

   public void doReadFromDevice() {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("BBacnetScheduleExport.doReadFromDevice on " + this);
      }

      try {
         this.getSupervisor();
      } catch (Exception var5) {
         this.executeFail(var5);
      }

      BBacnetScheduleDeviceExt deviceExt = (BBacnetScheduleDeviceExt)this.getDeviceExt();

      try {
         BAbstractSchedule ret = deviceExt.readRemote(this);
         if (ret != null) {
            this.importRemoteSubordinate((BControlSchedule)ret);
         }

         this.getDevice().pingOk();
      } catch (BacnetException var3) {
         logger.log(
            Level.SEVERE,
            "BacnetException reading remote subordinate schedule data for " + this.getSupervisor().getName() + " from " + this.getObjectId() + ": " + var3,
            (Throwable)var3
         );
         this.executeFail(var3);
      } catch (Exception var4) {
         logger.log(
            Level.SEVERE,
            "Exception reading remote subordinate schedule data for " + this.getSupervisor().getName() + " from " + this.getObjectId() + ": " + var4,
            (Throwable)var4
         );
         this.executeFail(var4);
      }
   }

   public BOrd doChangeType(BBacnetChangeTypeParm param) {
      return new BBacnetScheduleTypeChangeJob(BBacnetNetwork.bacnet(), this, param).submit(null);
   }

   private void setAsnType() {
      if (this.getDataType() == "" || this.getDataType().equals(lex.getText("asn.unknown"))) {
         this.setDataTypeFromSupervisor();
      }

      this.asnType = AsnUtil.getAsnType(this.getDataType());
   }

   private void setDataTypeFromSupervisor() {
      try {
         Type t = this.getSupervisor().getType();
         if (t.is(BBooleanSchedule.TYPE)) {
            this.setDataType(AsnUtil.getAsnTypeName(1));
            this.setFlags(writeEnumAs, this.getFlags(writeEnumAs) | 4);
         } else if (t.is(BNumericSchedule.TYPE)) {
            this.setDataType(AsnUtil.getAsnTypeName(4));
            this.setFlags(writeEnumAs, this.getFlags(writeEnumAs) | 4);
         } else if (t.is(BEnumSchedule.TYPE)) {
            this.setDataType(AsnUtil.getAsnTypeName(2));
            this.setFlags(writeEnumAs, this.getFlags(writeEnumAs) & -5);
         } else {
            this.setDataType(AsnUtil.getAsnTypeName(7));
            this.setFlags(writeEnumAs, this.getFlags(writeEnumAs) | 4);
         }
      } catch (Exception var2) {
         logger.info("Cannot determine ASN type for schedule export " + this + ":invalid supervisor ord [" + this.getSupervisorOrd() + "]");
      }
   }

   public int getAsnType() {
      Type t = this.getSupervisor().getType();
      if (t.is(BEnumSchedule.TYPE)) {
         switch (this.getWriteEnumAs().getOrdinal()) {
            case 0:
               return 2;
            case 1:
               return 9;
            case 2:
               return 3;
         }
      }

      return this.asnType;
   }

   public void importRemoteSubordinate(BControlSchedule remoteSubordinate) {
      BControlSchedule localSupervisor = (BControlSchedule)this.getSupervisor();
      if (!localSupervisor.getClass().equals(remoteSubordinate.getClass())) {
         throw new IllegalStateException(
            "Incompatible Schedules: Import ["
               + remoteSubordinate.getClass().getName()
               + "] != "
               + localSupervisor.toPathString()
               + " ["
               + localSupervisor.getClass().getName()
               + "]"
         );
      } else {
         boolean cleanup = localSupervisor.getCleanupExpiredEvents();
         remoteSubordinate.setCleanupExpiredEvents(cleanup);
         BFacets existingFacets = localSupervisor.getFacets();
         copyOver(remoteSubordinate, localSupervisor);
         localSupervisor.setFacets(existingFacets);
      }
   }

   protected static void copyOver(BAbstractSchedule source, BAbstractSchedule target) {
      target.copyFrom(source, null);
      target.set("lastModified", BScheduleDeviceExt.getVersionOf(source));
   }

   protected void validateSchedule() {
      BAbstractSchedule as = this.getSupervisor();
      if (as instanceof BWeeklySchedule) {
         BWeeklySchedule ws = (BWeeklySchedule)as;
         BDailySchedule[] kids = ws.getSpecialEventsChildren();

         for (int i = 0; i < kids.length; i++) {
            BDailySchedule daily = kids[i];
            if (daily.getDays() instanceof BCustomSchedule) {
               throw new IllegalStateException("Cannot send CustomSchedule Special Events to BACnet");
            }

            BDaySchedule day = daily.getDay();
            SlotCursor<Property> sc = day.getProperties();

            while (sc.next(BTimeSchedule.class)) {
               BTimeSchedule ts = (BTimeSchedule)sc.get();
               if (ts.getEffectiveValue().getStatus().isNull()) {
                  throw new IllegalStateException("Cannot write NULL-valued TimeSchedules to BACnet");
               }
            }
         }
      }
   }

   public BBacnetChangeTypeParm doReadChangeTypeParams() {
      BBacnetScheduleDeviceExt deviceExt = (BBacnetScheduleDeviceExt)this.getDeviceExt();
      BBacnetDevice dev = deviceExt.device();
      BBacnetChangeTypeParm ctp = new BBacnetChangeTypeParm();
      ctp.setDataType(this.getDataType());
      ctp.setSupervisorOrd(this.getSupervisorOrd());

      try {
         BBacnetAddress adr = dev.getAddress();
         BBacnetObjectIdentifier oid = this.getObjectId();
         BBacnetClientLayer cl = deviceExt.client();
         AsnInputStream asnIn = new AsnInputStream();
         byte[] encodedValue = cl.readProperty(adr, oid, 174);
         asnIn.setBuffer(encodedValue);
         ctp.getScheduleDefault().readAsn(asnIn);
         encodedValue = cl.readProperty(adr, oid, 54);
         asnIn.setBuffer(encodedValue);
         ctp.getListOfObjectPropertyRefs().readAsn(asnIn);

         try {
            encodedValue = cl.readProperty(adr, oid, 123);
            asnIn.setBuffer(encodedValue);
            ctp.getWeeklySchedule().readAsn(asnIn);
            ctp.setHasWeeklySchedule(true);
         } catch (ErrorException var11) {
            ctp.setHasWeeklySchedule(false);
         }

         try {
            encodedValue = cl.readProperty(adr, oid, 38);
            asnIn.setBuffer(encodedValue);
            ctp.getExceptionSchedule().readAsn(asnIn);
            ctp.setHasExceptionSchedule(true);
         } catch (ErrorException var10) {
            ctp.setHasExceptionSchedule(false);
         }

         return ctp;
      } catch (Exception var12) {
         logger.log(Level.SEVERE, "doReadChangeTypeParams failed with exception", (Throwable)var12);
         return null;
      }
   }
}
