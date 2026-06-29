package com.tridium.bacnet.job;

import com.tridium.bacnet.datatypes.BDiscoveryNetworks;
import com.tridium.bacnet.datatypes.BWhoHasConfig;
import com.tridium.bacnet.services.unconfirmed.IHaveRequest;
import com.tridium.bacnet.stack.IHaveListener;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetUnconfirmedServiceChoice;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BWhoHasJob extends BDeviceManagerJob implements IHaveListener {
   public static final Type TYPE = Sys.loadType(BWhoHasJob.class);
   private BWhoHasConfig params;
   private ArrayList<BWhoHasJob.IHave> iHaves = new ArrayList<>();

   @Override
   public Type getType() {
      return TYPE;
   }

   public BWhoHasJob() {
   }

   public BWhoHasJob(BBacnetNetwork bacnet, BWhoHasConfig params) {
      super(bacnet);
      this.params = params;
      this.add(SlotPath.escape("Who Has"), BString.make(params.toString()));
   }

   public void run(Context cx) throws Exception {
      if (this.bacnet == null) {
         throw new IllegalStateException("Must submit thru BacnetNetwork.submitDeviceManagerJob()");
      } else if (this.params != null) {
         this.server().registerIHaveListener(this);
         BDiscoveryNetworks networks = this.params.getNetworks();
         if (networks.isAllNetworks()) {
            BBacnetAddress addr = BBacnetAddress.GLOBAL_BROADCAST_ADDRESS;

            try {
               if (this.params.isDefaultRange()) {
                  if (this.params.getUse().getOrdinal() == 0) {
                     this.client().whoHas(addr, this.params.getObjectId());
                  } else {
                     this.client().whoHas(addr, this.params.getObjectName(), this.bacnet.getLocalDevice().getCharacterSet());
                  }
               } else if (this.params.getUse().getOrdinal() == 0) {
                  this.client().whoHas(addr, this.params.getObjectId(), this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
               } else {
                  this.client()
                     .whoHas(
                        addr,
                        this.params.getObjectName(),
                        this.bacnet.getLocalDevice().getCharacterSet(),
                        this.params.getDeviceLowLimit(),
                        this.params.getDeviceHighLimit()
                     );
               }
            } catch (Exception var15) {
               logger.log(Level.SEVERE, "WhoHasJob runnable failed with exception", (Throwable)var15);
               return;
            }
         } else {
            try {
               int[] nets = networks.getNetworks();

               for (int i = 0; i < nets.length; i++) {
                  BBacnetAddress addr = new BBacnetAddress(nets[i], (BBacnetOctetString)null);
                  if (this.params.isDefaultRange()) {
                     if (this.params.getUse().getOrdinal() == 0) {
                        this.client().whoHas(addr, this.params.getObjectId());
                     } else {
                        this.client().whoHas(addr, this.params.getObjectName(), this.bacnet.getLocalDevice().getCharacterSet());
                     }
                  } else if (this.params.getUse().getOrdinal() == 0) {
                     this.client().whoHas(addr, this.params.getObjectId(), this.params.getDeviceLowLimit(), this.params.getDeviceHighLimit());
                  } else {
                     this.client()
                        .whoHas(
                           addr,
                           this.params.getObjectName(),
                           this.bacnet.getLocalDevice().getCharacterSet(),
                           this.params.getDeviceLowLimit(),
                           this.params.getDeviceHighLimit()
                        );
                  }
               }
            } catch (Exception var16) {
               logger.log(Level.SEVERE, "WhoHasJob runnable failed with exception", (Throwable)var16);
               return;
            }
         }

         BAbsTime start = BAbsTime.make();
         long startMillis = start.getMillis();
         BRelTime wait = BRelTime.makeSeconds(this.params.getWaitResponseTime());
         BAbsTime end = start.add(wait);
         double waitMillis = end.getMillis() - startMillis;
         BAbsTime now = BAbsTime.make();
         int count = 0;

         while (now.isBefore(end) || count < this.iHaves.size()) {
            if (count == this.iHaves.size()) {
               try {
                  Thread.sleep(500L);
               } catch (InterruptedException var14) {
               }
            } else {
               BWhoHasJob.IHave iHave = this.iHaves.get(count);
               IHaveRequest request = iHave.iHave;
               this.add(null, request.toJob());
               count++;
            }

            now = BAbsTime.make();
            int progres = (int)((now.getMillis() - startMillis) / waitMillis * 100.0);
            if (progres > 100) {
               progres = 100;
            }

            this.progress(progres);
         }

         this.server().unregisterIHaveListener(this);
      }
   }

   @Override
   public void receiveIHave(IHaveRequest request, BBacnetAddress sourceAddress) {
      this.iHaves.add(new BWhoHasJob.IHave(request, sourceAddress));
      this.log()
         .message(
            MessageFormat.format(
               lex.getText("whoHas.found"), BacnetUnconfirmedServiceChoice.TAGS[1], sourceAddress, request.getObjectId(), request.getObjectName()
            )
         );
   }

   static class IHave {
      IHaveRequest iHave;
      BBacnetAddress addr;

      IHave(IHaveRequest iHave, BBacnetAddress addr) {
         this.iHave = iHave;
         this.addr = addr;
      }
   }
}
