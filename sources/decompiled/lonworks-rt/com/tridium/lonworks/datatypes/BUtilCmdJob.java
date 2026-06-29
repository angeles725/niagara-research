package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.netmgmt.LonUtilRequest;
import java.io.PrintWriter;
import java.io.Writer;
import javax.baja.job.BSimpleJob;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraTopic;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BIcon;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Topic;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "getText",
   returnType = "BString",
   flags = 2048
)
@NiagaraTopic(
   name = "newText",
   eventType = "BString"
)
public class BUtilCmdJob extends BSimpleJob {
   public static final Action getText = newAction(2048, null);
   public static final Topic newText = newTopic(0, null);
   public static final Type TYPE = Sys.loadType(BUtilCmdJob.class);
   BUtilCmdJob.NetworkWriter writer = new BUtilCmdJob.NetworkWriter();
   public static BIcon icon = BIcon.std("wrench.png");
   BUtilitiesCommand cmd;
   BLonNetwork lonNetwork;

   public BString getText() {
      return (BString)this.invoke(getText, null, null);
   }

   public void fireNewText(BString event) {
      this.fire(newText, event, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BUtilCmdJob(BLonNetwork network, BUtilitiesCommand cmd) {
      this.lonNetwork = network;
      this.cmd = cmd;
   }

   public BUtilCmdJob() {
   }

   public void run(Context cx) throws Exception {
      LonUtilRequest r = new LonUtilRequest(this.cmd, this.lonNetwork.getLonNetmgmt(), new PrintWriter(this.writer), this);
      r.execute();
      this.writer.close();
      this.log().success("LonUtil: " + this.cmd.getCommand());
   }

   public BString doGetText() {
      String s = this.writer.getText();
      return s.length() == 0 && this.writer.close ? null : BString.make(s);
   }

   public void doCancel(Context cx) {
      this.lonNetwork.netMessageReceiver().cancelServicePin();
      super.doCancel(cx);
   }

   public void completed(int percent) {
      this.progress(percent);
   }

   public BIcon getIcon() {
      return icon;
   }

   static class NetworkWriter extends Writer {
      StringBuffer buf = new StringBuffer();
      boolean close = false;

      NetworkWriter() {
         this.buf = new StringBuffer();
      }

      @Override
      public void close() {
         this.close = true;
      }

      @Override
      public void flush() {
      }

      @Override
      public synchronized void write(char[] cbuf, int off, int len) {
         this.buf.append(cbuf, off, len);
      }

      synchronized String getText() {
         String s = this.buf.toString();
         if (s.length() > 0) {
            this.buf = new StringBuffer();
         }

         return s;
      }
   }
}
