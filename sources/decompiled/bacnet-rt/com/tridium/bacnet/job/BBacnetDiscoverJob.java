package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NReadAccessSpec;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.BacnetException;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetPropertyIdentifier;
import javax.baja.bacnet.io.AsnException;
import javax.baja.driver.BDeviceExt;
import javax.baja.job.BSimpleJob;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.nre.util.IntHashMap;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "deviceOrd",
   type = "BOrd",
   defaultValue = "BOrd.DEFAULT"
)
public abstract class BBacnetDiscoverJob extends BSimpleJob implements BacnetConst {
   public static final Property deviceOrd = newProperty(0, BOrd.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetDiscoverJob.class);
   public static final Logger logger = Logger.getLogger("bacnet.client");
   protected static final int[] DESCRIPTION_ONLY = new int[]{28};
   static Lexicon lex = Lexicon.make("bacnet");
   BBacnetDeviceObject deviceObject;
   BBacnetDevice device;

   public BOrd getDeviceOrd() {
      return (BOrd)this.get(deviceOrd);
   }

   public void setDeviceOrd(BOrd v) {
      this.set(deviceOrd, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetDiscoverJob() {
   }

   public BBacnetDiscoverJob(BDeviceExt deviceExt) {
      this.device = (BBacnetDevice)deviceExt.getParent();
      this.deviceObject = this.device.getConfig().getDeviceObject();
      this.setDeviceOrd(this.device.getSlotPathOrd());
   }

   protected boolean doForId(BBacnetObjectIdentifier objectId) {
      return true;
   }

   @Deprecated
   void processId(BBacnetObjectIdentifier objectId, String objectName) throws Exception {
   }

   int[] getDiscoveryPropIds(BBacnetObjectIdentifier objectId) {
      int objectType = objectId.getObjectType();
      switch (objectType) {
         case 8:
            return new int[]{112, 28};
         case 9:
            return new int[]{36, 28};
         case 10:
            return new int[]{42, 28};
         case 11:
         case 12:
         case 13:
         case 14:
         case 17:
         case 19:
         case 21:
         case 22:
         case 23:
         case 24:
         case 26:
         case 28:
         default:
            return new int[]{85, 28};
         case 15:
            return new int[]{17, 28};
         case 16:
            return new int[]{92, 28};
         case 18:
            return new int[]{125, 28};
         case 20:
         case 25:
         case 27:
            return new int[]{141, 28};
         case 29:
            return new int[]{208, 28};
      }
   }

   void addDiscoveryChild(BBacnetDiscoverJob.IdVals iv) {
   }

   public void run(Context cx) throws Exception {
      long t0 = 0L;

      try {
         this.log().start(lex.getText("discover.begin"));
         super.progress(0);
         t0 = Clock.ticks();
         if (!this.readObjectList()) {
            this.log().failed(" ERROR: Unable to read the Bacnet object list for " + this.device);
            logger.severe("Unable to read the Bacnet object list for " + this.device);
            this.log().failed(lex.getText("discover.fail"));
            this.failed(null);
            throw new BajaRuntimeException(lex.getText("discover.fail"));
         } else {
            this.checkCancel();
            int total = this.deviceObject.getObjectList().getSize();
            if (total <= 0) {
               int var19 = true;
            }

            String msg = MessageFormat.format(lex.getText("discover.finishedList"), this.deviceObject.getObjectList().getSize());
            this.log().message(msg);
            this.progress(10);
            this.log().message("Reading Object_Names...");
            SlotCursor<Property> c = this.deviceObject.getObjectList().getProperties();
            Array<BBacnetDiscoverJob.IdVals> lrnIds = new Array(BBacnetDiscoverJob.IdVals.class);

            while (c.next(BBacnetObjectIdentifier.class)) {
               BBacnetObjectIdentifier objectId = (BBacnetObjectIdentifier)c.get();
               if (this.doForId(objectId)) {
                  lrnIds.add(new BBacnetDiscoverJob.IdVals(objectId, 77));
               }
            }

            if (lrnIds.size() == 0) {
               this.log().message("No objects of an appropriate type were found!");
            } else {
               this.checkCancel();
               this.readObjectVals(lrnIds, 15, 40);
               this.checkCancel();
               this.log().message("Reading Object properties...");
               Iterator<BBacnetDiscoverJob.IdVals> it = lrnIds.iterator();

               while (it.hasNext() && this.isAlive()) {
                  BBacnetDiscoverJob.IdVals iv = it.next();
                  iv.name = iv.get(77).toString();
                  iv.clear();
                  int[] propids = this.getDiscoveryPropIds(iv.id);

                  for (int i = 0; i < propids.length; i++) {
                     iv.addPV(propids[i]);
                  }
               }

               this.checkCancel();
               this.readObjectVals(lrnIds, 45, 70);
               this.checkCancel();
               this.log().message("Creating discovery children...");
               it = lrnIds.iterator();
               int size = lrnIds.size();
               int count = 0;

               while (it.hasNext()) {
                  this.checkCancel();
                  BBacnetDiscoverJob.IdVals iv = it.next();
                  this.addDiscoveryChild(iv);
                  this.progress(++count, size, 70, 100);
               }

               this.log().success(lex.getText("discover.end"));
               long tf = Clock.ticks();
               int olsize = this.deviceObject.getObjectList().getSize();
               int secs = (int)((tf - t0) / 1000L);
               String statsmsg = lex.getText("discover.stats");
               if (olsize == 1) {
                  statsmsg = lex.getText("discover.stats.1obj");
               }

               String stats = MessageFormat.format(statsmsg, olsize, secs);
               this.log().message(stats);
            }
         }
      } catch (Exception var17) {
         this.log().failed(" ERROR: Unable to learn Bacnet objects in " + this.device + ": " + var17);
         logger.log(Level.SEVERE, "Unable to learn Bacnet objects in " + this.device + ": " + var17, (Throwable)var17);
         throw var17;
      }
   }

   private void checkCancel() {
      if (!this.isAlive()) {
         this.log().message("discover.canceled");
         throw new BajaRuntimeException(lex.getText("discover.canceled"));
      }
   }

   public void progress(int progres) {
      int progress = this.getProgress();
      if (progres > progress) {
         super.progress(progres);
      } else {
         this.heartbeat();
      }
   }

   private void progress(int count, int size, int start, int end) {
      count++;
      int prog = start + (int)((float)count / size * (end - start));
      this.progress(prog);
   }

   private boolean readObjectList() {
      try {
         this.deviceObject.readProperty(BBacnetDeviceObject.objectList);
         return true;
      } catch (Exception var2) {
         return false;
      }
   }

   private BValue decodeVal(BBacnetObjectIdentifier objectId, int propertyId, byte[] encodedValue) {
      String propName = BBacnetPropertyIdentifier.tag(propertyId);

      BValue objectVal;
      try {
         objectVal = AsnUtil.asnToValue(this.device.getPropertyInfo(objectId.getObjectType(), propertyId), encodedValue);
      } catch (AsnException var7) {
         this.log()
            .message(
               "   ERROR: Unable to convert Asn-encoded value ("
                  + ByteArrayUtil.toHexString(encodedValue)
                  + ") for object "
                  + objectId
                  + ", property "
                  + propName
            );
         logger.info("Unable to convert Asn-encoded value (" + ByteArrayUtil.toHexString(encodedValue) + ") for object " + objectId + ", property " + propName);
         objectVal = BString.make("ASN error:" + objectId.toString() + " [" + propName + "]");
      }

      return objectVal;
   }

   private void readVal(BBacnetObjectIdentifier objectId, BBacnetDiscoverJob.PropVal pv) {
      String propName = BBacnetPropertyIdentifier.tag(pv.propId);

      try {
         byte[] encodedValue = client().readProperty(this.device.getAddress(), objectId, pv.propId);
         pv.len = encodedValue.length + 4;
         pv.val = this.decodeVal(objectId, pv.propId, encodedValue);
      } catch (BacnetException var5) {
         logger.info("BacnetException reading property " + propName + " for " + objectId + " :: " + var5);
         pv.val = BString.make(var5.toString() + ":" + objectId + " [" + propName + "]");
         pv.err = true;
      }
   }

   private void readObjectValsRP(Array<BBacnetDiscoverJob.IdVals> a, int startProg, int endProg) {
      int asize = a.size();
      int count = 0;
      this.progress(startProg);

      for (BBacnetDiscoverJob.IdVals iv : a) {
         javax.baja.nre.util.IntHashMap.Iterator pit = iv.propVals.iterator();

         while (pit.hasNext()) {
            this.checkCancel();
            this.readVal(iv.id, (BBacnetDiscoverJob.PropVal)pit.next());
         }

         this.progress(++count, asize, startProg, endProg);
      }
   }

   private void readObjectVals(Array<BBacnetDiscoverJob.IdVals> a, int startProg, int endProg) {
      int asize = a.size();
      int count = 0;
      this.progress(startProg);
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("readObjectVals: a.size=" + asize);
      }

      boolean gotVals = false;
      ListIterator<BBacnetDiscoverJob.IdVals> it = a.iterator();
      if (!this.device.isServiceSupported("readPropertyMultiple")) {
         logger.fine("  no RPM; just use RP");
         this.log().message("Device does not support ReadPropertyMultiple; using ReadProperty...");
         this.readObjectValsRP(a, startProg, endProg);
      } else {
         if (this.device.getSegmentationSupported().isSegmentedTransmit()) {
            logger.fine("segmented transmit ok, try one RPM!");
            Array<NReadAccessSpec> specs = new Array(NReadAccessSpec.class);

            while (it.hasNext()) {
               BBacnetDiscoverJob.IdVals iv = it.next();
               int[] propids = new int[iv.propVals.size()];
               javax.baja.nre.util.IntHashMap.Iterator pit = iv.propVals.iterator();
               int ndx = 0;

               while (pit.hasNext()) {
                  propids[ndx++] = ((BBacnetDiscoverJob.PropVal)pit.next()).propId;
               }

               NReadAccessSpec spec = new NReadAccessSpec(iv.id, propids);
               specs.add(spec);
            }

            try {
               this.checkCancel();
               Iterator rars = client().readPropertyMultiple(this.device.getAddress(), specs);
               it = a.iterator();

               while (it.hasNext()) {
                  this.checkCancel();
                  if (!rars.hasNext()) {
                     throw new IllegalStateException("results do not match specs!");
                  }

                  BBacnetDiscoverJob.IdVals iv = it.next();
                  javax.baja.nre.util.IntHashMap.Iterator pvs = iv.propVals.iterator();

                  while (pvs.hasNext() && rars.hasNext()) {
                     NReadPropertyResult rpr = (NReadPropertyResult)rars.next();
                     BBacnetDiscoverJob.PropVal pv = (BBacnetDiscoverJob.PropVal)pvs.next();
                     if (!iv.id.equals(rpr.getObjectId())) {
                        throw new IllegalStateException("objectId mismatch: spec=" + iv.id + "; rpr=" + rpr.getObjectId());
                     }

                     if (pv.propId != rpr.getPropertyId()) {
                        throw new IllegalStateException("propertyId mismatch: spec=" + pv.propId + "; rpr=" + rpr.getPropertyId());
                     }

                     if (rpr.isError()) {
                        pv.val = BString.make(rpr.getPropertyAccessError() + ":" + rpr.getObjectId() + " [" + BBacnetPropertyIdentifier.tag(pv.propId) + "]");
                        pv.err = true;
                     } else {
                        pv.val = this.decodeVal(iv.id, pv.propId, rpr.getPropertyValue());
                     }
                  }

                  this.progress(++count, asize, startProg, endProg);
               }

               gotVals = true;
            } catch (Exception var25) {
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Error reading object values with single RPM message:" + var25.toString());
               }

               this.log().message("Error reading object values with single RPM message:" + var25.toString());
            }
         }

         this.checkCancel();
         if (!gotVals) {
            int maxApdu = Math.min(BBacnetNetwork.localDevice().getMaxAPDULengthAccepted(), this.device.getMaxAPDULengthAccepted());
            int numVals = maxApdu / 50;
            int maxLen = 1;
            int totLen = 0;
            int totVals = 0;
            int avgLen = 0;
            int next = 0;
            int lastP1 = 0;
            it = a.iterator();
            count = 0;

            do {
               this.checkCancel();
               lastP1 = next + numVals;
               Array<NReadAccessSpec> specs = new Array(NReadAccessSpec.class);
               if (lastP1 > a.size()) {
                  lastP1 = a.size();
               }

               this.log().message("Reading object values in groups: items " + next + " to " + (lastP1 - 1));
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine(
                     "Reading object values in groups: maxApdu="
                        + maxApdu
                        + "; numVals="
                        + numVals
                        + "; maxLen="
                        + maxLen
                        + "; totLen="
                        + totLen
                        + "; totVals="
                        + totVals
                        + "; avgLen="
                        + avgLen
                        + "; next="
                        + next
                        + "; lastP1="
                        + lastP1
                  );
               }

               for (int i = next; i < lastP1; i++) {
                  if (it.hasNext()) {
                     BBacnetDiscoverJob.IdVals iv = it.next();
                     if (iv != null) {
                        int[] propids = new int[iv.propVals.size()];
                        javax.baja.nre.util.IntHashMap.Iterator pidit = iv.propVals.iterator();
                        int j = 0;

                        while (pidit.hasNext() && j < propids.length) {
                           propids[j++] = ((BBacnetDiscoverJob.PropVal)pidit.next()).propId;
                        }

                        NReadAccessSpec spec = new NReadAccessSpec(iv.id, propids);
                        specs.add(spec);
                     }
                  }
               }

               boolean groupOk = true;

               try {
                  Iterator rprs = client().readPropertyMultiple(this.device.getAddress(), specs);

                  for (int ix = next; ix < lastP1; ix++) {
                     BBacnetDiscoverJob.IdVals iv = (BBacnetDiscoverJob.IdVals)a.get(ix);
                     javax.baja.nre.util.IntHashMap.Iterator pvs = iv.propVals.iterator();

                     while (pvs.hasNext() && rprs.hasNext()) {
                        BBacnetDiscoverJob.PropVal pvx = (BBacnetDiscoverJob.PropVal)pvs.next();
                        NReadPropertyResult rprx = (NReadPropertyResult)rprs.next();
                        if (pvx.propId != rprx.getPropertyId()) {
                           groupOk = false;
                        } else if (rprx.isError()) {
                           this.log().message("Error reading " + iv.id + " [" + pvx.dbg() + "]");
                           pvx.val = BString.make(
                              rprx.getPropertyAccessError() + ":" + rprx.getObjectId() + " [" + BBacnetPropertyIdentifier.tag(pvx.propId) + "]"
                           );
                           pvx.err = true;
                           pvx.len = 8;
                        } else {
                           pvx.len = rprx.getPropertyValue().length + 4;
                           pvx.val = this.decodeVal(iv.id, pvx.propId, rprx.getPropertyValue());
                        }
                     }

                     this.progress(++count, asize, startProg, endProg);
                  }
               } catch (BacnetException var24) {
                  if (numVals > 4) {
                     this.log().message("BacnetException reading group: " + var24 + "; retrying with smaller group size..");
                     if (logger.isLoggable(Level.FINE)) {
                        logger.fine("BacnetException reading group: " + var24 + "; retrying with smaller group size..");
                     }

                     for (int ix = next; ix < lastP1; ix++) {
                        it.previous();
                     }

                     numVals /= 2;
                     lastP1 = next + numVals;
                     continue;
                  }

                  this.log().message("BacnetException reading group: " + var24);
                  groupOk = false;
               }

               this.checkCancel();
               if (!groupOk) {
                  logger.fine("!groupOk - RP for the items in this group.");
                  this.log().message("Reading items individually for ids " + next + " through " + lastP1 + "...");
                  count = next;

                  for (int ix = next; ix < lastP1; ix++) {
                     this.checkCancel();
                     BBacnetDiscoverJob.IdVals iv = (BBacnetDiscoverJob.IdVals)a.get(ix);
                     javax.baja.nre.util.IntHashMap.Iterator pvs = iv.propVals.iterator();

                     while (pvs.hasNext()) {
                        BBacnetDiscoverJob.PropVal pvx = (BBacnetDiscoverJob.PropVal)pvs.next();
                        if (pvx.val == null) {
                           this.readVal(iv.id, pvx);
                        }
                     }

                     this.progress(++count, asize, startProg, endProg);
                  }
               }

               for (int ix = next; ix < lastP1; ix++) {
                  BBacnetDiscoverJob.IdVals iv = (BBacnetDiscoverJob.IdVals)a.get(ix);
                  totVals++;
                  int len = iv.getLength();
                  if (len > maxLen) {
                     maxLen = len;
                  }

                  totLen += len;
               }

               if (totVals > 0) {
                  avgLen = totLen / totVals;
               }

               if (totVals >= a.size()) {
                  break;
               }

               next = lastP1;
               int len = maxLen;
               if (avgLen != 0 && maxLen > 3 * avgLen) {
                  len = avgLen;
               }

               numVals = (maxApdu - 3) / len;
               if (logger.isLoggable(Level.FINE)) {
                  logger.fine("Configure next group read: maxLen=" + maxLen + "; avgLen=" + avgLen + "  --> numVals=" + numVals);
               }
            } while (lastP1 <= a.size());
         }
      }
   }

   static final BBacnetClientLayer client() {
      return ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getClient();
   }

   protected final BBacnetDevice device() {
      return this.device;
   }

   static class IdVals {
      public BBacnetObjectIdentifier id;
      String name;
      public IntHashMap propVals;
      int pri = 85;

      public IdVals(BBacnetObjectIdentifier id) {
         this(id, new int[0]);
      }

      public IdVals(BBacnetObjectIdentifier id, int propid) {
         this(id, new int[]{propid});
      }

      public IdVals(BBacnetObjectIdentifier id, int[] propids) {
         this.id = id;
         this.propVals = new IntHashMap();

         for (int i = 0; i < propids.length; i++) {
            this.propVals.put(propids[i], new BBacnetDiscoverJob.PropVal(propids[i]));
         }

         if (propids.length > 0) {
            this.pri = propids[0];
         }
      }

      public void addPV(int propid) {
         if (this.propVals.size() == 0) {
            this.pri = propid;
         }

         this.propVals.put(propid, new BBacnetDiscoverJob.PropVal(propid));
      }

      public void clear() {
         this.propVals.clear();
         this.pri = 85;
      }

      public BBacnetDiscoverJob.PropVal get(int propid) {
         return (BBacnetDiscoverJob.PropVal)this.propVals.get(propid);
      }

      public BBacnetDiscoverJob.PropVal primary() {
         return (BBacnetDiscoverJob.PropVal)this.propVals.get(this.pri);
      }

      public int getLength() {
         javax.baja.nre.util.IntHashMap.Iterator pvs = this.propVals.iterator();
         int len = 7;

         while (pvs.hasNext()) {
            len += ((BBacnetDiscoverJob.PropVal)pvs.next()).len;
         }

         return len;
      }

      @Override
      public String toString() {
         StringBuilder s = new StringBuilder();
         s.append(this.id.toShortString()).append(" {");
         javax.baja.nre.util.IntHashMap.Iterator it = this.propVals.iterator();

         while (it.hasNext()) {
            s.append(((BBacnetDiscoverJob.PropVal)it.next()).dbg());
            if (it.hasNext()) {
               s.append(',');
            }
         }

         s.append('}');
         return s.toString();
      }
   }

   static class PropVal {
      int propId;
      BValue val;
      int len = 0;
      boolean err = false;

      public PropVal(int pid) {
         this.propId = pid;
      }

      @Override
      public String toString() {
         return this.val != null ? this.val.toString() : "--";
      }

      public String dbg() {
         return this.propId + "=" + (this.val != null ? this.val.toString() + " [" + this.val.getType() + "]" : "--");
      }
   }
}
