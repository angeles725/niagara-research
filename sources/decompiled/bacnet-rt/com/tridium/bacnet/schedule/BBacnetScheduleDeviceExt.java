package com.tridium.bacnet.schedule;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.BacnetQuery;
import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.job.BBacnetDiscoverSchedulesJob;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.BacnetStackException;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BIBacnetObjectContainer;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.data.BIDataValue;
import javax.baja.driver.schedule.BScheduleDeviceExt;
import javax.baja.driver.schedule.BScheduleExport;
import javax.baja.driver.schedule.BScheduleImportExt;
import javax.baja.naming.BOrd;
import javax.baja.naming.OrdQuery;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BBooleanSchedule;
import javax.baja.schedule.BCalendarSchedule;
import javax.baja.schedule.BCompositeSchedule;
import javax.baja.schedule.BDailySchedule;
import javax.baja.schedule.BDateRangeSchedule;
import javax.baja.schedule.BDateSchedule;
import javax.baja.schedule.BDaySchedule;
import javax.baja.schedule.BEnumSchedule;
import javax.baja.schedule.BNumericSchedule;
import javax.baja.schedule.BScheduleReference;
import javax.baja.schedule.BTimeSchedule;
import javax.baja.schedule.BWeeklySchedule;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BWeekday;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "skipWriteOnError",
   type = "boolean",
   defaultValue = "false"
)
@NiagaraAction(
   name = "submitScheduleDiscoveryJob",
   returnType = "BOrd",
   flags = 4
)
public class BBacnetScheduleDeviceExt extends BScheduleDeviceExt implements BacnetConst, BIBacnetObjectContainer {
   public static final Property skipWriteOnError = newProperty(0, false, null);
   public static final Action submitScheduleDiscoveryJob = newAction(4, null);
   public static final Type TYPE = Sys.loadType(BBacnetScheduleDeviceExt.class);
   boolean loaded = false;
   private static Comparator<Object> specialEventComparator = new Comparator<Object>() {
      @Override
      public int compare(Object o1, Object o2) {
         if (o1 == null || o2 == null) {
            throw new NullPointerException();
         } else if (o1 instanceof BDailySchedule && o2 instanceof BDailySchedule) {
            BDailySchedule ds1 = (BDailySchedule)o1;
            BDailySchedule ds2 = (BDailySchedule)o2;
            Property p1 = ds1.getProperty("priority");
            Property p2 = ds2.getProperty("priority");
            return p1 != null && p2 != null ? ds1.getInt(p1) - ds2.getInt(p2) : 0;
         } else {
            throw new ClassCastException("Cannot compare " + o1.getClass() + " and " + o2.getClass());
         }
      }
   };
   private static final Context dumpCx = BFacets.make("showSeconds", BBoolean.TRUE, "showMilliseconds", BBoolean.TRUE);
   private static final Lexicon lex = Lexicon.make("bacnet");
   public static final String[] skipWritesProps0 = new String[]{
      BBacnetPropertyIdentifier.weeklySchedule.getTag(),
      BBacnetPropertyIdentifier.exceptionSchedule.getTag(),
      BBacnetPropertyIdentifier.effectivePeriod.getTag(),
      BBacnetPropertyIdentifier.priorityForWriting.getTag()
   };
   public static final String[] skipWritesProps4 = new String[]{
      BBacnetPropertyIdentifier.scheduleDefault.getTag(),
      BBacnetPropertyIdentifier.weeklySchedule.getTag(),
      BBacnetPropertyIdentifier.exceptionSchedule.getTag(),
      BBacnetPropertyIdentifier.effectivePeriod.getTag(),
      BBacnetPropertyIdentifier.priorityForWriting.getTag()
   };
   public static final BIDataValue[] skipWritesVals0 = new BIDataValue[]{BBoolean.FALSE, BBoolean.FALSE, BBoolean.FALSE, BBoolean.FALSE};
   public static final BIDataValue[] skipWritesVals4 = new BIDataValue[]{BBoolean.FALSE, BBoolean.FALSE, BBoolean.FALSE, BBoolean.FALSE, BBoolean.FALSE};
   public static final BFacets skipWrites0 = BFacets.make(skipWritesProps0, skipWritesVals0);
   public static final BFacets skipWrites4 = BFacets.make(skipWritesProps4, skipWritesVals4);
   private static final Logger logger = Logger.getLogger("bacnet.schedule");
   private ScheduleSupport0 supp = new ScheduleSupport16(this);

   public boolean getSkipWriteOnError() {
      return this.getBoolean(skipWriteOnError);
   }

   public void setSkipWriteOnError(boolean v) {
      this.setBoolean(skipWriteOnError, v, null);
   }

   public BOrd submitScheduleDiscoveryJob() {
      return (BOrd)this.invoke(submitScheduleDiscoveryJob, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public ScheduleSupport0 getSupport() {
      return this.supp;
   }

   public void setSupport(int protocolRevision) {
      this.supp = ScheduleSupport0.makeForProtocolRevision(protocolRevision, this.supp);
      this.supp.setDeviceExt(this);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Client schedule support for " + this.device() + " is now " + this.supp.getVersion());
      }
   }

   public void doSetSupp(BInteger pr) {
      this.setSupport(pr.getInt());
   }

   public BOrd doSubmitScheduleDiscoveryJob(Context cx) {
      return this.device().isFatalFault() ? null : new BBacnetDiscoverSchedulesJob(this).submit(cx);
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if (this.get(p).getType().is(BBacnetScheduleExport.TYPE)) {
            BBacnetScheduleExport export = (BBacnetScheduleExport)this.get(p);
            if (this.device().getProtocolRevision() >= 4) {
               export.setSkipWrites(skipWrites4);
            } else {
               export.setSkipWrites(skipWrites0);
            }
         }
      }
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetDevice;
   }

   public BScheduleExport makeExport(String supervisorId) {
      return new BBacnetScheduleExport(supervisorId);
   }

   public BScheduleImportExt makeImportExt() {
      return new BBacnetScheduleImportExt();
   }

   final BBacnetDevice device() {
      return (BBacnetDevice)this.getDevice();
   }

   BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   @Deprecated
   public static final BBacnetScheduleImportExt getBacnetExt(BAbstractSchedule sch) {
      return getBacnetImportExt(sch);
   }

   public static final BBacnetScheduleImportExt getBacnetImportExt(BAbstractSchedule sch) {
      BObject o = sch.get("ext");
      if (o != null && o instanceof BBacnetScheduleImportExt) {
         return (BBacnetScheduleImportExt)o;
      } else {
         SlotCursor<Property> c = sch.getProperties();
         return c.next(BBacnetScheduleImportExt.class) ? (BBacnetScheduleImportExt)c.get() : null;
      }
   }

   public final BBacnetScheduleExport getBacnetExportExt(BAbstractSchedule sch) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BBacnetScheduleExport.class)) {
         BBacnetScheduleExport e = (BBacnetScheduleExport)c.get();
         if (sch == e.getSupervisor()) {
            return e;
         }
      }

      return null;
   }

   @Override
   public BObject lookupBacnetObject(BBacnetObjectIdentifier objectId, int propertyId, int propertyArrayIndex, String domain) {
      if (objectId == null) {
         return null;
      } else {
         if (!this.isRunning() && !this.loaded) {
            this.getComponentSpace().update(this, Integer.MAX_VALUE);
            this.loaded = true;
         }

         SlotCursor<Property> sc = this.getProperties();

         while (sc.nextComponent()) {
            BComponent o = sc.get().asComponent();
            if (o instanceof BBacnetScheduleExport) {
               BBacnetScheduleExport exp = (BBacnetScheduleExport)o;
               if (objectId.equals(exp.getObjectId())) {
                  return exp.getSupervisor();
               }
            } else if (o instanceof BAbstractSchedule) {
               BBacnetScheduleImportExt imp = (BBacnetScheduleImportExt)o.get("ext");
               if (imp != null && objectId.equals(imp.getObjectId())) {
                  return o;
               }
            }
         }

         return null;
      }
   }

   public BBacnetScheduleImportExt lookupImport(BBacnetObjectIdentifier objectId) {
      if (objectId == null) {
         return null;
      } else {
         SlotCursor<Property> sc = this.getProperties();

         while (sc.next(BAbstractSchedule.class)) {
            BAbstractSchedule sch = (BAbstractSchedule)sc.get();
            BBacnetScheduleImportExt imp = (BBacnetScheduleImportExt)sch.get("ext");
            if (imp != null && objectId.equals(imp.getObjectId())) {
               return imp;
            }
         }

         return null;
      }
   }

   public BBacnetScheduleExport lookupExport(BBacnetObjectIdentifier objectId) {
      if (objectId == null) {
         return null;
      } else {
         SlotCursor<Property> sc = this.getProperties();

         while (sc.next(BBacnetScheduleExport.class)) {
            BBacnetScheduleExport exp = (BBacnetScheduleExport)sc.get();
            if (objectId.equals(exp.getObjectId())) {
               return exp;
            }
         }

         return null;
      }
   }

   BAbstractSchedule readRemote(BBacnetScheduleImportExt local) throws Exception {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("readRemote(" + local + ")");
      }

      BAbstractSchedule c = local.getSubordinate();
      BAbstractSchedule ret = null;
      if (c instanceof BCalendarSchedule) {
         ret = this.readCalendar(local.getObjectId());
      } else {
         ret = this.readSchedule(local, c.getType());
      }

      this.validate(ret);
      return ret;
   }

   BAbstractSchedule readRemote(BBacnetScheduleExport local) throws Exception {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("readRemote(" + local + ")");
      }

      BAbstractSchedule c = local.getSupervisor();
      BAbstractSchedule ret = null;
      if (c instanceof BCalendarSchedule) {
         ret = this.readCalendar(local.getObjectId());
      } else {
         ret = this.readSchedule(local, c.getType());
      }

      this.validate(ret);
      return ret;
   }

   void writeRemote(BBacnetScheduleExport local) throws Exception {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("writeRemote(" + local + ")");
      }

      if (!this.device().isServiceSupported("writeProperty")) {
         throw new UnsupportedOperationException(lex.getText("serviceNotSupported.writeProperty"));
      } else {
         BAbstractSchedule c = local.getSupervisor();
         if (c instanceof BCalendarSchedule) {
            this.sendCalendar(local, (BCalendarSchedule)c);
         } else {
            setPrioritiesByOrder(((BWeeklySchedule)c).getSpecialEvents());
            this.sendSchedule(local, (BWeeklySchedule)c);
         }
      }
   }

   private void validate(BAbstractSchedule s) throws BacnetException {
      if (s == null) {
         throw new IllegalArgumentException("Return schedule is null!");
      } else {
         if (!(s instanceof BCalendarSchedule)) {
            BWeeklySchedule w = (BWeeklySchedule)s;
            this.checkForCalendarReferences(w.getSpecialEvents());
         }
      }
   }

   private BCalendarSchedule readCalendar(BBacnetObjectIdentifier objectId) throws BacnetException {
      byte[] encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 23);
      AsnInputStream asnIn = AsnInputStream.make(encodedValue);

      BCalendarSchedule var4;
      try {
         var4 = this.supp.decodeDateList(asnIn);
      } finally {
         asnIn.release();
      }

      return var4;
   }

   private BWeeklySchedule readSchedule(BComplex local, Type t) throws BacnetException {
      BBacnetObjectIdentifier objectId = (BBacnetObjectIdentifier)local.get("objectId");
      BWeeklySchedule sch = (BWeeklySchedule)t.getInstance();
      byte[] encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 85);
      if (encodedValue != null) {
         AsnInputStream asnIn = AsnInputStream.make(encodedValue);

         try {
            int tag = asnIn.peekApplicationTag();
            switch (tag) {
               case 0:
               case 2:
               case 3:
               case 5:
               case 6:
               case 7:
               case 8:
               default:
                  break;
               case 1:
                  if (!t.is(BBooleanSchedule.TYPE)) {
                     logger.warning(MessageFormat.format(lex.getText("wrongClientScheduleType"), t, "BOOLEAN"));
                  }
                  break;
               case 4:
                  if (!t.is(BNumericSchedule.TYPE)) {
                     logger.warning(MessageFormat.format(lex.getText("wrongClientScheduleType"), t, "REAL"));
                  }
                  break;
               case 9:
                  if (!t.is(BEnumSchedule.TYPE)) {
                     logger.warning(MessageFormat.format(lex.getText("wrongClientScheduleType"), t, "Enumerated"));
                  }
            }
         } finally {
            asnIn.release();
         }
      }

      if (this.device().getProtocolRevision() >= 4) {
         encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 111);
         if (encodedValue != null) {
            ((BStatusValue)sch.get("out")).setStatus(AsnUtil.asnStatusFlagsToBStatus(encodedValue));
         }

         encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 174);
         if (encodedValue != null) {
            AsnInputStream asnIn = AsnInputStream.make(encodedValue);

            try {
               sch.setDefaultOutput(this.supp.decodeScheduleDefault(sch.getDefaultOutput(), asnIn, -1));
            } finally {
               asnIn.release();
            }
         }
      }

      boolean weeklyOk = false;
      boolean excOk = false;
      BCompositeSchedule exceptionSchedule = null;
      if (this.device().getSegmentationSupported().isSegmentedTransmit()) {
         try {
            encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 123);
            if (encodedValue != null) {
               AsnInputStream asnIn = AsnInputStream.make(encodedValue);

               try {
                  for (int i = 1; i <= 7; i++) {
                     ((BDailySchedule)sch.getWeek().get(BWeekday.make(i % 7).getTag()))
                        .setDay(this.supp.decodeDailySchedule(sch.getDefaultOutput(), asnIn, -1));
                  }
               } finally {
                  asnIn.release();
               }
            }

            weeklyOk = true;
         } catch (BacnetException var109) {
            logger.info("Unable to retrieve Weekly_Schedule from " + objectId + " in bulk:" + var109);
         }

         try {
            encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 38);
            if (encodedValue != null) {
               AsnInputStream asnIn = AsnInputStream.make(encodedValue);

               try {
                  exceptionSchedule = this.supp.decodeExceptionSchedule(sch.getDefaultOutput(), asnIn, this.device().getObjectId(), -1);
                  sortEventsByPriority(exceptionSchedule);
                  sch.getSchedule().set("specialEvents", exceptionSchedule);
               } finally {
                  asnIn.release();
               }
            }

            excOk = true;
         } catch (BacnetException var103) {
            logger.info("Unable to retrieve Exception_Schedule from " + objectId + " in bulk:" + var103);
         }
      }

      if (!weeklyOk) {
         try {
            for (int i = 1; i <= 7; i++) {
               encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 123, i);
               if (encodedValue != null) {
                  AsnInputStream asnIn = AsnInputStream.make(encodedValue);

                  try {
                     ((BDailySchedule)sch.getWeek().get(BWeekday.make(i % 7).getTag()))
                        .setDay(this.supp.decodeDailySchedule(sch.getDefaultOutput(), asnIn, -1));
                  } finally {
                     asnIn.release();
                  }
               }
            }
         } catch (BacnetException var107) {
            logger.info("Unable to retrieve Weekly_Schedule from " + objectId + " individually:" + var107);
         }
      }

      if (!excOk) {
         try {
            encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 38, 0);
            int len = AsnUtil.fromAsnUnsignedInt(encodedValue);
            BDailySchedule[] events = new BDailySchedule[len];
            exceptionSchedule = new BCompositeSchedule();

            for (int ix = 0; ix < len; ix++) {
               encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 38, ix + 1);
               if (encodedValue != null) {
                  BDailySchedule event = null;
                  AsnInputStream asnIn = AsnInputStream.make(encodedValue);

                  try {
                     event = this.supp.decodeSpecialEvent(sch.getDefaultOutput(), asnIn, this.device().getObjectId(), -1, ix);
                  } finally {
                     asnIn.release();
                  }

                  if (event != null) {
                     int eventPriority = ((BInteger)event.get("priority")).getInt();
                     boolean inserted = false;

                     for (int j = 0; j < ix; j++) {
                        BDailySchedule existing = events[j];
                        Property p = existing.getProperty("priority");
                        int existingPriority = 1;
                        if (p != null) {
                           existingPriority = existing.getInt(p);
                        }

                        if (eventPriority < existingPriority) {
                           for (int k = ix; k > j; k--) {
                              events[k] = events[k - 1];
                           }

                           events[j] = event;
                           inserted = true;
                           break;
                        }
                     }

                     if (!inserted) {
                        events[ix] = event;
                     }
                  }
               }
            }

            for (int ixx = 0; ixx < len; ixx++) {
               if (events[ixx] != null) {
                  exceptionSchedule.add(events[ixx]);
               }
            }

            sch.getSchedule().set("specialEvents", exceptionSchedule);
         } catch (BacnetException var106) {
            logger.info("Unable to retrieve Exception_Schedule from " + objectId + " individually:" + var106);
         }
      }

      this.checkForCalendarReferences(exceptionSchedule);
      encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 32);
      if (encodedValue != null) {
         AsnInputStream asnIn = AsnInputStream.make(encodedValue);

         try {
            sch.setEffective(this.supp.decodeDateRange(asnIn));
         } finally {
            asnIn.release();
         }
      }

      encodedValue = this.client().readProperty(this.device().getAddress(), objectId, 88);
      if (encodedValue != null) {
         local.set("priorityForWriting", BInteger.make(AsnUtil.fromAsnUnsignedInt(encodedValue)));
      }

      return sch;
   }

   private void sendCalendar(BBacnetScheduleExport local, BCalendarSchedule cal) throws BacnetException {
      BBacnetObjectIdentifier objectId = local.getObjectId();
      local.writeProperty = BBacnetPropertyIdentifier.dateList;
      AsnOutputStream asnOut = AsnOutputStream.make();

      byte[] encodedValue;
      try {
         this.supp.encodeDateList(cal, asnOut);
         encodedValue = asnOut.toByteArray();
      } finally {
         asnOut.release();
      }

      this.client().writeProperty(this.device().getAddress(), objectId, 23, encodedValue);
   }

   private void sendSchedule(BBacnetScheduleExport local, BWeeklySchedule schedule) throws BacnetException {
      BacnetException err = null;
      BBacnetObjectIdentifier objectId = local.getObjectId();
      if (this.device().getProtocolRevision() >= 4 && !isSkipWrite(local, BBacnetPropertyIdentifier.scheduleDefault)) {
         local.writeProperty = BBacnetPropertyIdentifier.scheduleDefault;
         AsnOutputStream asnOut = AsnOutputStream.make();

         byte[] encodedValue;
         try {
            this.supp.encodeScheduleDefault(schedule.getDefaultOutput(), asnOut, local.getAsnType());
            encodedValue = asnOut.toByteArray();
         } finally {
            asnOut.release();
         }

         try {
            this.client().writeProperty(this.device().getAddress(), objectId, 174, encodedValue);
         } catch (BacnetStackException var198) {
            err = var198;
         } catch (BacnetException var199) {
            err = var199;
            if (this.getSkipWriteOnError()) {
               setSkipWrites(local, BBacnetPropertyIdentifier.scheduleDefault, true);
            }
         }
      }

      boolean weeklyOk = false;
      boolean excOk = false;
      if (!isSkipWrite(local, BBacnetPropertyIdentifier.weeklySchedule)) {
         local.writeProperty = BBacnetPropertyIdentifier.weeklySchedule;
         if (!isSkipWrite(local, "weeklyScheduleEntire")) {
            try {
               AsnOutputStream asnOut = AsnOutputStream.make();

               byte[] encodedValue;
               try {
                  for (int i = 1; i <= 7; i++) {
                     this.supp.encodeDailySchedule(schedule.get(BWeekday.make(i % 7)), schedule.getDefaultOutput(), asnOut, local.getAsnType());
                  }

                  encodedValue = asnOut.toByteArray();
               } finally {
                  asnOut.release();
               }

               this.client().writeProperty(this.device().getAddress(), objectId, 123, encodedValue);
               weeklyOk = true;
            } catch (BacnetStackException var196) {
               logger.info("Unable to send Weekly_Schedule to " + objectId + " in bulk:" + var196);
            } catch (BacnetException var197) {
               logger.info("Unable to send Weekly_Schedule to " + objectId + " in bulk:" + var197);
               setSkipWrites(local, "weeklyScheduleEntire", true);
            }
         }

         if (!weeklyOk) {
            for (int i = 1; i <= 7; i++) {
               AsnOutputStream asnOut = AsnOutputStream.make();

               byte[] encodedValue;
               try {
                  this.supp.encodeDailySchedule(schedule.get(BWeekday.make(i % 7)), schedule.getDefaultOutput(), asnOut, local.getAsnType());
                  encodedValue = asnOut.toByteArray();
               } finally {
                  asnOut.release();
               }

               try {
                  this.client().writeProperty(this.device().getAddress(), objectId, 123, i, encodedValue);
               } catch (BacnetStackException var193) {
                  if (err == null) {
                     err = var193;
                  }
                  break;
               } catch (BacnetException var194) {
                  if (err == null) {
                     err = var194;
                  }

                  if (this.getSkipWriteOnError()) {
                     setSkipWrites(local, BBacnetPropertyIdentifier.weeklySchedule, true);
                  }
                  break;
               }
            }
         }
      }

      if (!isSkipWrite(local, BBacnetPropertyIdentifier.exceptionSchedule)) {
         local.writeProperty = BBacnetPropertyIdentifier.exceptionSchedule;
         if (!isSkipWrite(local, "exceptionScheduleEntire")) {
            try {
               AsnOutputStream asnOut = AsnOutputStream.make();

               byte[] encodedValue;
               try {
                  this.supp
                     .encodeExceptionSchedule(schedule.getSpecialEvents(), schedule.getDefaultOutput(), asnOut, local.getAsnType(), this.device().getObjectId());
                  encodedValue = asnOut.toByteArray();
               } finally {
                  asnOut.release();
               }

               this.client().writeProperty(this.device().getAddress(), objectId, 38, encodedValue);
               excOk = true;
            } catch (BacnetStackException var181) {
               logger.info("Unable to send Exception_Schedule to " + objectId + " in bulk:" + var181);
            } catch (BacnetException var182) {
               logger.info("Unable to send Exception_Schedule to " + objectId + " in bulk:" + var182);
               setSkipWrites(local, "exceptionScheduleEntire", true);
            }
         }

         if (!excOk) {
            int excSchedSize = ((BDailySchedule[])schedule.getSpecialEvents().getChildren(BDailySchedule.class)).length;
            boolean resizeOk = false;

            try {
               this.client().writeProperty(this.device().getAddress(), objectId, 38, 0, AsnUtil.toAsnUnsigned(excSchedSize));
               resizeOk = true;
            } catch (BacnetStackException var191) {
               if (err == null) {
                  err = var191;
               }
            } catch (BacnetException var192) {
               if (err == null) {
                  err = var192;
               }

               if (this.getSkipWriteOnError()) {
                  setSkipWrites(local, BBacnetPropertyIdentifier.exceptionSchedule, true);
               }
            }

            if (resizeOk) {
               SlotCursor<Property> c = schedule.getSpecialEvents().getProperties();
               int index = 0;

               while (c.next(BDailySchedule.class)) {
                  AsnOutputStream asnOut = AsnOutputStream.make();

                  byte[] encodedValue;
                  try {
                     this.supp
                        .encodeSpecialEvent((BDailySchedule)c.get(), schedule.getDefaultOutput(), asnOut, local.getAsnType(), this.device().getObjectId());
                     encodedValue = asnOut.toByteArray();
                  } finally {
                     asnOut.release();
                  }

                  try {
                     this.client().writeProperty(this.device().getAddress(), objectId, 38, ++index, encodedValue);
                  } catch (BacnetStackException var189) {
                     if (err == null) {
                        err = var189;
                     }
                     break;
                  } catch (BacnetException var190) {
                     if (err == null) {
                        err = var190;
                     }

                     if (this.getSkipWriteOnError()) {
                        setSkipWrites(local, BBacnetPropertyIdentifier.exceptionSchedule, true);
                     }
                     break;
                  }
               }
            }
         }
      }

      if (!isSkipWrite(local, BBacnetPropertyIdentifier.effectivePeriod)) {
         local.writeProperty = BBacnetPropertyIdentifier.effectivePeriod;
         AsnOutputStream asnOut = AsnOutputStream.make();

         byte[] encodedValue;
         try {
            this.supp.encodeDateRange(schedule.getEffective(), asnOut);
            encodedValue = asnOut.toByteArray();
         } finally {
            asnOut.release();
         }

         try {
            this.client().writeProperty(this.device().getAddress(), objectId, 32, encodedValue);
         } catch (BacnetStackException var187) {
            if (err == null) {
               err = var187;
            }
         } catch (BacnetException var188) {
            if (err == null) {
               err = var188;
            }

            if (this.getSkipWriteOnError()) {
               setSkipWrites(local, BBacnetPropertyIdentifier.effectivePeriod, true);
            }
         }
      }

      if (!isSkipWrite(local, BBacnetPropertyIdentifier.priorityForWriting)) {
         local.writeProperty = BBacnetPropertyIdentifier.priorityForWriting;
         AsnOutputStream asnOut = AsnOutputStream.make();

         byte[] encodedValue;
         try {
            asnOut.writeUnsignedInteger(local.getPriorityForWriting());
            encodedValue = asnOut.toByteArray();
         } finally {
            asnOut.release();
         }

         try {
            this.client().writeProperty(this.device().getAddress(), objectId, 88, encodedValue);
         } catch (BacnetStackException var185) {
            if (err == null) {
               err = var185;
            }
         } catch (BacnetException var186) {
            if (err == null) {
               err = var186;
            }

            if (this.getSkipWriteOnError()) {
               setSkipWrites(local, BBacnetPropertyIdentifier.priorityForWriting, true);
            }
         }
      }

      local.writeProperty = null;
      if (err != null) {
         throw err;
      }
   }

   private void checkForCalendarReferences(BCompositeSchedule excSch) throws BacnetException {
      if (excSch != null) {
         BDailySchedule[] events = (BDailySchedule[])excSch.getChildren(BDailySchedule.class);

         for (int i = 0; i < events.length; i++) {
            BAbstractSchedule days = events[i].getDays();
            if (days instanceof BScheduleReference) {
               BOrd ref = ((BScheduleReference)days).getRef();
               OrdQuery[] oqs = ref.parse();
               int len = oqs.length;
               OrdQuery oq = oqs[len - 1];
               BBacnetObjectIdentifier objectId = null;
               if (oq instanceof BacnetQuery) {
                  BacnetQuery query = (BacnetQuery)oq;

                  try {
                     objectId = (BBacnetObjectIdentifier)BBacnetObjectIdentifier.DEFAULT.decodeFromString(query.getObject());
                  } catch (IOException var13) {
                  }

                  BBacnetScheduleImportExt imp = this.lookupImport(objectId);
                  BBacnetScheduleExport exp = this.lookupExport(objectId);
                  if (objectId != null && imp == null && exp == null) {
                     this.addCalendarImport(objectId);
                  }
               }
            }
         }
      }
   }

   private void addCalendarImport(BBacnetObjectIdentifier objectId) throws BacnetException {
      String calName = AsnUtil.fromAsnCharacterString(this.client().readProperty(this.device().getAddress(), objectId, 77));
      BCalendarSchedule cal = new BCalendarSchedule();
      BBacnetScheduleImportExt ext = new BBacnetScheduleImportExt();
      ext.setObjectId(objectId);
      cal.add("ext", ext);
      this.add(SlotPath.escape(calName), cal);
   }

   private static boolean isSkipWrite(BBacnetScheduleExport export, BEnum prop) {
      BObject dv = export.getSkipWrites().getFacet(prop.getTag());
      return dv != null && dv.getType() == BBoolean.TYPE ? ((BBoolean)dv).getBoolean() : false;
   }

   public static void setPrioritiesByOrder(BCompositeSchedule exc) {
      BDailySchedule[] events = (BDailySchedule[])exc.getChildren(BDailySchedule.class);
      int len = events.length;
      int pri = 1;

      for (int i = 0; i < len; i++) {
         BacUtil.setOrAdd(events[i], "priority", BInteger.make(pri), null);
         if (pri < 16) {
            pri++;
         }
      }
   }

   public static void sortEventsByPriority(BCompositeSchedule exc) {
      BDailySchedule[] events = (BDailySchedule[])exc.getChildren(BDailySchedule.class);
      Arrays.sort(events, specialEventComparator);
      String[] names = new String[events.length];

      for (int i = 0; i < names.length; i++) {
         names[i] = events[i].getName();
      }

      for (int i = 0; i < events.length; i++) {
         exc.remove(events[i]);
         exc.add(names[i], events[i]);
      }
   }

   private static boolean isSkipWrite(BBacnetScheduleExport export, String facetName) {
      BObject dv = export.getSkipWrites().getFacet(facetName);
      return dv != null && dv.getType() == BBoolean.TYPE ? ((BBoolean)dv).getBoolean() : false;
   }

   private static void setSkipWrites(BBacnetScheduleExport export, BEnum prop, boolean skip) {
      BFacets f = export.getSkipWrites();
      export.setSkipWrites(BFacets.make(f, prop.getTag(), BBoolean.make(skip)));
   }

   private static void setSkipWrites(BBacnetScheduleExport export, String facetName, boolean skip) {
      BFacets f = export.getSkipWrites();
      export.setSkipWrites(BFacets.make(f, facetName, BBoolean.make(skip)));
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetScheduleDeviceExt", 2);
      out.prop("support", this.supp.getVersion());
      out.endProps();
   }

   private static void d(String s) {
      System.out.println(s);
   }

   private static void dd(String s) {
      System.out.print(s);
   }

   void dumpSchedule(BWeeklySchedule s) {
      d("NiagaraSchedule dump:");
      d(" type:" + s.getType());
      SlotCursor<Property> sc = s.getProperties();

      while (sc.next()) {
         dd(sc.property().getName() + "[" + sc.property().getType() + "]:");
         dump(sc.get());
      }
   }

   static void dump(BAbstractSchedule s) {
      SlotCursor<Property> sc = s.getProperties();

      while (sc.next()) {
         dd(sc.property().getName() + "[" + sc.property().getType() + "]:");
         dump(sc.get());
      }
   }

   static void dump(BObject o) {
      d(o.toString());
   }

   static void dump(BDateRangeSchedule s) {
      dd("start:");
      dump(s.getStart());
      dd("end:");
      dump(s.getEnd());
   }

   static void dump(BDateSchedule s) {
      dd("" + s.getYear() + "-" + s.getMonth() + "-" + s.getDay() + " " + s.getWeekday());
   }

   static void dump(BDailySchedule s) {
      d("DailySchedule dump:");
      dd("day:");
      dump(s.getDay());
      dd("days:");
      dump(s.getDays());
      Property pri = s.getProperty("priority");
      if (pri != null) {
         d("priority:" + s.getInt(pri));
      }
   }

   static void dump(BDaySchedule s) {
      d("DaySchedule dump:");
      SlotCursor<Property> c = s.getProperties();
      int i = 0;

      while (c.next(BTimeSchedule.class)) {
         BTimeSchedule ts = (BTimeSchedule)c.get();
         dd("timeSched " + i++ + ":");
         dump(ts);
      }
   }

   static void dump(BTimeSchedule s) {
      d(s.getStart().toString(dumpCx) + " - " + s.getFinish().toString(dumpCx) + " = " + s.getEffectiveValue());
   }
}
