package com.tridium.bacnet.job;

import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.client.BBacnetClientLayer;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import java.util.logging.Logger;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.BacnetConst;
import javax.baja.job.BSimpleJob;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
public abstract class BDeviceManagerJob extends BSimpleJob implements BacnetConst {
   public static final Type TYPE = Sys.loadType(BDeviceManagerJob.class);
   public static final Logger logger = Logger.getLogger("bacnet.client");
   protected BBacnetNetwork bacnet;
   static Lexicon lex = Lexicon.make("bacnet");

   public Type getType() {
      return TYPE;
   }

   public BDeviceManagerJob() {
   }

   public BDeviceManagerJob(BBacnetNetwork bacnet) {
      this.bacnet = bacnet;
   }

   protected final BBacnetClientLayer client() {
      return ((BBacnetStack)this.bacnet.getBacnetComm()).getClient();
   }

   protected final BBacnetServerLayer server() {
      return ((BBacnetStack)this.bacnet.getBacnetComm()).getServer();
   }

   public void progress(int progres) {
      int progress = this.getProgress();
      if (progres > progress) {
         super.progress(progres);
      } else {
         this.heartbeat();
      }
   }

   protected void progress(int count, int size, int start, int end) {
      count++;
      int prog = start + (int)((float)count / size * (end - start));
      this.progress(prog);
   }

   protected void checkCancel() {
      this.checkCancel(lex.getText("deviceManager.canceled"));
   }

   protected void checkCancel(String msg) {
      if (!this.isAlive()) {
         this.log().message(msg);
         throw new BajaRuntimeException(msg);
      }
   }
}
