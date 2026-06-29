package com.tridium.bacnet.job;

import com.tridium.bacnet.datatypes.BTimeSynchConfig;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.network.BBacnetNetworkLayer;
import com.tridium.bacnet.stack.network.BNetworkPort;
import java.text.MessageFormat;
import java.util.logging.Level;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.bacnet.datatypes.BBacnetOctetString;
import javax.baja.bacnet.datatypes.BBacnetRecipient;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public class BTimeSynchJob extends BDeviceManagerJob {
   public static final Type TYPE = Sys.loadType(BTimeSynchJob.class);
   private BBacnetNetwork bacnet;
   private BTimeSynchConfig params;
   static Lexicon lex = Lexicon.make("bacnet");

   @Override
   public Type getType() {
      return TYPE;
   }

   public BTimeSynchJob() {
   }

   public BTimeSynchJob(BBacnetNetwork bacnet, BTimeSynchConfig params) {
      super(bacnet);
      this.bacnet = bacnet;
      this.params = params;
   }

   public void run(Context cx) throws Exception {
      if (this.bacnet == null) {
         throw new IllegalStateException("Must submit thru BacnetNetwork.submitDeviceManagerJob()");
      } else if (this.params != null) {
         boolean utc = this.params.getTimeSynchType();
         logger.info("Sending " + (utc ? "UTC" : "") + " time to BACnet network...");

         try {
            if (this.params.getAddressRange()) {
               BBacnetRecipient r = new BBacnetRecipient(BBacnetAddress.GLOBAL_BROADCAST_ADDRESS);
               if (utc) {
                  this.client().utcTimeSynch(r);
               } else {
                  this.client().timeSynch(r);
               }
            } else {
               BNetworkPort[] ports = (BNetworkPort[])this.network().getChildren(BNetworkPort.class);

               for (int i = 0; i < ports.length; i++) {
                  if (ports[i].getStatus().isOk()) {
                     BBacnetRecipient r = new BBacnetRecipient(new BBacnetAddress(ports[i].getNetworkNumber(), BBacnetOctetString.DEFAULT));
                     if (utc) {
                        this.client().utcTimeSynch(r);
                     } else {
                        this.client().timeSynch(r);
                     }
                  }
               }
            }
         } catch (Exception var6) {
            logger.log(Level.INFO, "Exception sending TimeSynch: " + var6, (Throwable)var6);
            String msg = MessageFormat.format(lex.getText("timeSynch.fail"), var6);
            this.add("failureCause", BString.make(var6.toString()));
            this.log().failed(msg);
            throw var6;
         }
      }
   }

   private BBacnetNetworkLayer network() {
      return ((BBacnetStack)this.bacnet.getBacnetComm()).getNetwork();
   }
}
