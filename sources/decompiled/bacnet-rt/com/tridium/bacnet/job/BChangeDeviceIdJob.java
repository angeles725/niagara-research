package com.tridium.bacnet.job;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.datatypes.BChangeDeviceIdConfig;
import com.tridium.bacnet.services.unconfirmed.IAmRequest;
import com.tridium.bacnet.stack.IAmListener;
import java.util.Iterator;
import javax.baja.bacnet.BBacnetDevice;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.config.BBacnetDeviceObject;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Clock;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "config",
   type = "BChangeDeviceIdConfig",
   defaultValue = "new BChangeDeviceIdConfig()"
)
public class BChangeDeviceIdJob extends BDeviceManagerJob implements IAmListener {
   public static final Property config = newProperty(0, new BChangeDeviceIdConfig(), null);
   public static final Type TYPE = Sys.loadType(BChangeDeviceIdJob.class);
   boolean exists = false;
   Array<String> dups = new Array(String.class);
   int[] currs;
   BBacnetDevice[] currDevs;
   int[] news;
   BBacnetAddress[] found;

   public BChangeDeviceIdConfig getConfig() {
      return (BChangeDeviceIdConfig)this.get(config);
   }

   public void setConfig(BChangeDeviceIdConfig v) {
      this.set(config, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public BChangeDeviceIdJob() {
   }

   public BChangeDeviceIdJob(BBacnetNetwork bacnet, BChangeDeviceIdConfig params) {
      super(bacnet);
      this.setConfig(params);
      SlotCursor<Property> sc = this.getConfig().getProperties();
      Array<BBacnetObjectIdentifier> c = new Array(BBacnetObjectIdentifier.class);
      Array<BBacnetObjectIdentifier> n = new Array(BBacnetObjectIdentifier.class);

      while (sc.next(BBacnetObjectIdentifier.class)) {
         Property p = sc.property();
         if (p.getName().startsWith("currentId")) {
            c.add((BBacnetObjectIdentifier)sc.get());
         } else if (p.getName().startsWith("changeTo")) {
            n.add((BBacnetObjectIdentifier)sc.get());
         }
      }

      int csiz = c.size();
      int nsiz = n.size();
      this.currs = new int[csiz];
      this.news = new int[nsiz];

      for (int i = 0; i < csiz; i++) {
         this.currs[i] = ((BBacnetObjectIdentifier)c.get(i)).getInstanceNumber();
      }

      for (int i = 0; i < nsiz; i++) {
         this.news[i] = ((BBacnetObjectIdentifier)n.get(i)).getInstanceNumber();
      }
   }

   @Override
   public void receiveIAm(IAmRequest request, BBacnetAddress sourceAddress) {
      int inst = request.getObjectId().getInstanceNumber();

      for (int i = 0; i < this.news.length; i++) {
         if (inst == this.news[i]) {
            String msg = "I-Am " + request.getObjectId() + " from " + sourceAddress + "  matches proposed newId for device " + this.currDevs[i];
            this.log().failed(msg);
            this.dups.add(msg);
            this.found[i] = sourceAddress;
            this.exists = true;
         }
      }
   }

   public void run(Context cx) throws Exception {
      try {
         this.log().message("Sanity checks on current and new device IDS...");
         this.server().registerIAmListener(this);
         if (this.currs.length != this.news.length) {
            throw new IllegalArgumentException("Current ids length != New ids length");
         }

         this.currDevs = new BBacnetDevice[this.currs.length];

         for (int i = 0; i < this.currDevs.length; i++) {
            BBacnetObjectIdentifier cid = BBacnetObjectIdentifier.make(8, this.currs[i]);
            BBacnetDevice d = this.bacnet.lookupDeviceById(cid);
            if (d == null) {
               throw new IllegalArgumentException("Unable to locate existing device for id " + cid);
            }

            this.currDevs[i] = d;
         }

         this.found = new BBacnetAddress[this.currs.length];

         for (int i = 0; i < this.found.length; i++) {
            this.found[i] = null;
         }

         for (int i = 0; i < this.news.length; i++) {
            int n = this.news[i];

            for (int j = i + 1; j < this.news.length; j++) {
               if (n == this.news[j]) {
                  throw new IllegalArgumentException("Duplicate instance in list of new IDs");
               }
            }

            BBacnetDevice dup = this.bacnet.doLookupDeviceById(BBacnetObjectIdentifier.make(8, n));
            if (dup != null) {
               throw new IllegalArgumentException("New instance " + n + " is a duplicate of database device " + dup);
            }

            this.log().message("Who-Is for instance " + n);
            this.client().whoIs(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS, n, n);
         }

         BAbsTime start = Clock.time();
         BAbsTime end = start.add(BRelTime.makeSeconds(5));
         BAbsTime now = start;

         while (now.isBefore(end)) {
            int prog = (int)((float)(now.getMillis() - start.getMillis()) / (float)(end.getMillis() - start.getMillis()) * 100.0F);
            this.log().message("waiting on I-Ams (" + prog + "%%)...");
            this.progress(prog);
            now = Clock.time();

            try {
               Thread.sleep(1000L);
            } catch (InterruptedException var15) {
            }
         }

         if (this.exists) {
            throw new IllegalStateException("Existing devices were detected for some ids!");
         }

         for (int i = 0; i < this.currs.length; i++) {
            BBacnetDevice dev = this.currDevs[i];
            BBacnetObjectIdentifier newId = BBacnetObjectIdentifier.make(8, this.news[i]);
            this.log().message("Changing deviceId on " + dev + " from " + dev.getObjectId() + " to " + newId);
            BBacnetDeviceObject devObj = dev.getConfig().getDeviceObject();
            this.client().writeProperty(dev.getAddress(), dev.getObjectId(), 75, -1, AsnUtil.toAsnObjectId(newId), -1);
            devObj.setObjectId(newId);
            this.log().message("Successfully changed local and remote device ID to " + newId + " for device " + dev);
         }
      } catch (Exception var16) {
         try {
            Thread.sleep(2000L);
         } catch (InterruptedException var14) {
         }

         String mesg;
         if (this.exists && this.dups.size() != 0) {
            StringBuilder sb = new StringBuilder(var16.getMessage());
            Iterator<String> it = this.dups.iterator();

            while (it.hasNext()) {
               sb.append("\n").append(it.next());
            }

            mesg = sb.toString();
         } else {
            mesg = var16.toString();
         }

         this.add("failureCause", BString.make(mesg), 1, BFacets.make("multiLine", true), null);
         throw var16;
      } finally {
         this.server().unregisterIAmListener(this);
      }
   }

   public String toString(Context cx) {
      return this.getConfig().toString(cx);
   }
}
