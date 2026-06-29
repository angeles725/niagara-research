package com.tridium.modbusCore;

import com.tridium.basicdriver.BBasicNetwork;
import com.tridium.modbusCore.enums.BDataByteOrder64BitEnum;
import com.tridium.modbusCore.enums.BDataByteOrderEnum;
import com.tridium.modbusCore.enums.BModbusDataModeEnum;
import com.tridium.modbusCore.server.BModbusServerNetwork;
import javax.baja.driver.BDevice;
import javax.baja.log.Log;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;
import javax.baja.sys.BComponentEvent;
import javax.baja.sys.Property;
import javax.baja.sys.Subscriber;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "floatByteOrder",
      type = "BDataByteOrderEnum",
      defaultValue = "BDataByteOrderEnum.order3210"
   ), @NiagaraProperty(
      name = "longByteOrder",
      type = "BDataByteOrderEnum",
      defaultValue = "BDataByteOrderEnum.order3210"
   ), @NiagaraProperty(
      name = "double64BitByteOrder",
      type = "BDataByteOrder64BitEnum",
      defaultValue = "BDataByteOrder64BitEnum.order76543210"
   ), @NiagaraProperty(
      name = "long64BitByteOrder",
      type = "BDataByteOrder64BitEnum",
      defaultValue = "BDataByteOrder64BitEnum.order76543210"
   )})
public abstract class BModbusNetwork extends BBasicNetwork {
   public static final Property floatByteOrder = newProperty(0, BDataByteOrderEnum.order3210, null);
   public static final Property longByteOrder = newProperty(0, BDataByteOrderEnum.order3210, null);
   public static final Property double64BitByteOrder = newProperty(0, BDataByteOrder64BitEnum.order76543210, null);
   public static final Property long64BitByteOrder = newProperty(0, BDataByteOrder64BitEnum.order76543210, null);
   public static final Type TYPE = Sys.loadType(BModbusNetwork.class);
   protected Subscriber subscriber;
   protected Log modbusLog = null;
   private long totalCrcErrors = 0L;
   private long totalLrcErrors = 0L;
   private long totalTransactionIdErrors = 0L;
   private long totalPartialRxMsgs = 0L;

   public BDataByteOrderEnum getFloatByteOrder() {
      return (BDataByteOrderEnum)this.get(floatByteOrder);
   }

   public void setFloatByteOrder(BDataByteOrderEnum v) {
      this.set(floatByteOrder, v, null);
   }

   public BDataByteOrderEnum getLongByteOrder() {
      return (BDataByteOrderEnum)this.get(longByteOrder);
   }

   public void setLongByteOrder(BDataByteOrderEnum v) {
      this.set(longByteOrder, v, null);
   }

   public BDataByteOrder64BitEnum getDouble64BitByteOrder() {
      return (BDataByteOrder64BitEnum)this.get(double64BitByteOrder);
   }

   public void setDouble64BitByteOrder(BDataByteOrder64BitEnum v) {
      this.set(double64BitByteOrder, v, null);
   }

   public BDataByteOrder64BitEnum getLong64BitByteOrder() {
      return (BDataByteOrder64BitEnum)this.get(long64BitByteOrder);
   }

   public void setLong64BitByteOrder(BDataByteOrder64BitEnum v) {
      this.set(long64BitByteOrder, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BModbusNetwork() {
      this.setFlags(upload, 4);
      this.setFlags(download, 4);
   }

   public void serviceStarted() throws Exception {
      super.serviceStarted();
      this.subscriber = new BModbusNetwork.NameSubscriber(this);
      this.subscriber.subscribe((BComponent)this.getParent());
   }

   public void serviceStopped() throws Exception {
      this.subscriber.unsubscribeAll();
      this.subscriber = null;
      super.serviceStopped();
   }

   public abstract int getModbusMode();

   public Log getModbusLog() {
      String modbusLogName = this.getName();
      if (!SlotPath.isValidName(modbusLogName)) {
         modbusLogName = SlotPath.escape(modbusLogName);
      }

      return Log.getLog(modbusLogName);
   }

   protected void updateLog() {
      String modbusLogName = this.getName();
      if (!SlotPath.isValidName(modbusLogName)) {
         modbusLogName = SlotPath.escape(modbusLogName);
      }

      Log newLog = Log.getLog(modbusLogName);
      if (this.modbusLog == null) {
         this.modbusLog = this.getModbusLog();
      }

      synchronized (this.modbusLog) {
         if (this.modbusLog != null) {
            int severity = this.modbusLog.getSeverity();
            if (!newLog.getLogName().equals(this.modbusLog.getLogName())) {
               Log.deleteLog(this.modbusLog.getLogName());
            }

            newLog.setSeverity(severity);
         }

         this.modbusLog = newLog;
      }
   }

   protected void processNameSubscriberEvent(BComponentEvent event) {
      try {
         if (event.getId() == 3 && event.getSlot().equals(this.getPropertyInParent())) {
            this.updateLog();
         }
      } catch (Exception var3) {
         var3.printStackTrace();
      }
   }

   protected Subscriber getNameSubscriber() {
      return this.subscriber;
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("ModbusNetwork", 2);
      out.prop("Total CRC Errors", this.totalCrcErrors);
      out.prop("Total LRC Errors", this.totalLrcErrors);
      out.prop("Total Transaction ID Errors", this.totalTransactionIdErrors);
      out.prop("Total Partial Rx Msgs", this.totalPartialRxMsgs);
      out.endProps();
      BDevice[] devices = this.getDevices();
      if (devices != null && devices.length != 0) {
         boolean isServer = this instanceof BModbusServerNetwork;
         out.startTable(true);
         if (isServer) {
            out.trTitle("Device Info", 4);
            out.w("<tr>").th("Device").th("Address").th("Mode").th("Messages").w("</tr>\n");
         } else {
            out.trTitle("Device Transaction Info (Retries not counted)", 7);
            out.w("<tr>").th("Device").th("Address").th("Mode").th("Request").th("NoResponse").th("CRC/LRC").th("TransactionId").w("</tr>\n");
         }

         for (int i = 0; i < devices.length; i++) {
            BModbusDevice device = (BModbusDevice)devices[i];
            int dataMode = device.getModbusMode();
            if (isServer) {
               out.tr(
                  SlotPath.unescape(device.getName()),
                  "" + device.getDeviceAddress(),
                  "" + (dataMode == 2 ? "TCP" : BModbusDataModeEnum.make(dataMode)),
                  "" + device.getTotalMessages()
               );
            } else {
               boolean useLRC = dataMode == 0;
               long crcLrcErrors = useLRC ? device.getTotalLrcErrors() : device.getTotalCrcErrors();
               out.tr(
                  SlotPath.unescape(device.getName()),
                  "" + device.getDeviceAddress(),
                  "" + (dataMode == 2 ? "TCP" : BModbusDataModeEnum.make(dataMode)),
                  "" + device.getTotalMessages(),
                  "" + device.getTotalTimeouts(),
                  "" + crcLrcErrors,
                  "" + device.getTotalTransactionIdErrors()
               );
            }
         }

         out.endTable();
      }
   }

   public final void incrementCrcErrors() {
      this.totalCrcErrors++;
   }

   public final void incrementLrcErrors() {
      this.totalLrcErrors++;
   }

   public final void incrementTransactionIdErrors() {
      this.totalTransactionIdErrors++;
   }

   public final void incrementPartialRxMsgs() {
      this.totalPartialRxMsgs++;
   }

   public final long getPartialRxMsgs() {
      return this.totalPartialRxMsgs;
   }

   public final long getTotalCrcErrors() {
      return this.totalCrcErrors;
   }

   public final long geTotalLrcErrors() {
      return this.totalLrcErrors;
   }

   public final long geTotalTransactionIdErrors() {
      return this.totalTransactionIdErrors;
   }

   private class NameSubscriber extends Subscriber {
      private BModbusNetwork net;

      public NameSubscriber(BModbusNetwork net) {
         this.net = net;
      }

      public void event(BComponentEvent event) {
         this.net.processNameSubscriberEvent(event);
      }
   }
}
