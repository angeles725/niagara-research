package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NErrorType;
import com.tridium.bacnet.asn.NReadPropertyResult;
import com.tridium.bacnet.stack.BBacnetStack;
import com.tridium.bacnet.stack.server.BBacnetExportFolder;
import com.tridium.bacnet.stack.server.BBacnetExportTable;
import com.tridium.bacnet.stack.server.BBacnetServerLayer;
import com.tridium.bacnet.stack.server.BEventHandler;
import com.tridium.bacnet.stack.server.BHashedEventBuffer;
import java.util.logging.Logger;
import javax.baja.alarm.BAlarmRecord;
import javax.baja.alarm.BAlarmTransitionBits;
import javax.baja.alarm.BIAlarmSource;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BOffnormalAlgorithm;
import javax.baja.alarm.ext.offnormal.BOutOfRangeAlgorithm;
import javax.baja.bacnet.BBacnetNetwork;
import javax.baja.bacnet.alarm.BBacnetStatusAlgorithm;
import javax.baja.bacnet.datatypes.BBacnetArray;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.datatypes.BBacnetTimeStamp;
import javax.baja.bacnet.enums.BBacnetNotifyType;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.control.BControlPoint;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.status.BStatus;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BFormat;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "status",
      type = "BStatus",
      defaultValue = "BStatus.ok",
      flags = 67
   ), @NiagaraProperty(
      name = "faultCause",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "eventDetectionEnable",
      type = "boolean",
      defaultValue = "true",
      flags = 72
   ), @NiagaraProperty(
      name = "dynamicallyCreated",
      type = "boolean",
      defaultValue = "false",
      flags = 77
   ), @NiagaraProperty(
      name = "bacnetStatusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      facets = {@Facet("BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS")}
   )})
public abstract class BBacnetEventSource extends BComponent implements BIBacnetExportObject {
   public static final Property status = newProperty(67, BStatus.ok, null);
   public static final Property faultCause = newProperty(3, "", null);
   public static final Property eventDetectionEnable = newProperty(72, true, null);
   public static final Property dynamicallyCreated = newProperty(77, false, null);
   public static final Property bacnetStatusFlags = newProperty(
      0, BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")), BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS
   );
   public static final Type TYPE = Sys.loadType(BBacnetEventSource.class);
   private boolean fatalFault = false;
   protected static final BBacnetBitString ACKED_TRANS_DEFAULT = BBacnetBitString.make(new boolean[]{true, true, true});
   protected static final int MESSAGE_TEXTS_COUNT = 3;
   static Logger logger = Logger.getLogger("bacnet.server");

   @Override
   public BStatus getStatus() {
      return (BStatus)this.get(status);
   }

   public void setStatus(BStatus v) {
      this.set(status, v, null);
   }

   public String getFaultCause() {
      return this.getString(faultCause);
   }

   public void setFaultCause(String v) {
      this.setString(faultCause, v, null);
   }

   public boolean getEventDetectionEnable() {
      return this.getBoolean(eventDetectionEnable);
   }

   public void setEventDetectionEnable(boolean v) {
      this.setBoolean(eventDetectionEnable, v, null);
   }

   public boolean getDynamicallyCreated() {
      return this.getBoolean(dynamicallyCreated);
   }

   public void setDynamicallyCreated(boolean v) {
      this.setBoolean(dynamicallyCreated, v, null);
   }

   public BBacnetBitString getBacnetStatusFlags() {
      return (BBacnetBitString)this.get(bacnetStatusFlags);
   }

   public void setBacnetStatusFlags(BBacnetBitString v) {
      this.set(bacnetStatusFlags, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
      this.checkFatalFault();
      if (!this.getEventDetectionEnable()) {
         this.removeEventFromEventBuffer();
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (p.equals(eventDetectionEnable) && !this.getEventDetectionEnable()) {
            this.removeEventFromEventBuffer();
         }

         if (p.equals(bacnetStatusFlags)) {
            this.bacnetStatusFlagChanged();
         }
      }
   }

   protected void bacnetStatusFlagChanged() {
      BControlPoint point = this.getPoint();
      BAlarmSourceExt alarmExt = this.setStatusFlagsOnBAcnetStatusAlgo(point);
      if (null != alarmExt) {
      }
   }

   private BAlarmSourceExt setStatusFlagsOnBAcnetStatusAlgo(BControlPoint point) {
      if (null != point) {
         BAlarmSourceExt[] c = (BAlarmSourceExt[])point.getChildren(BAlarmSourceExt.class);

         for (int i = 0; i < c.length; i++) {
            BAlarmSourceExt alarmSourceExt = c[i];
            if (alarmSourceExt.getOffnormalAlgorithm().getType().is(BBacnetStatusAlgorithm.TYPE)) {
               ((BBacnetStatusAlgorithm)alarmSourceExt.getOffnormalAlgorithm()).setStausFlags(this.getBacnetStatusFlags());
               return alarmSourceExt;
            }
         }
      }

      return null;
   }

   void checkValid() {
   }

   protected BBacnetExportFolder getSvo() {
      for (BComplex parent = this; parent != null; parent = parent.getParent()) {
         if (parent instanceof BBacnetExportFolder) {
            return (BBacnetExportFolder)parent;
         }
      }

      return null;
   }

   public abstract boolean isValidAlarmExt(BIAlarmSource var1);

   @Deprecated
   protected abstract void updateAlarmInhibit();

   public abstract boolean isEventInitiationEnabled();

   @Override
   public abstract BBacnetObjectIdentifier getObjectId();

   public abstract BEnum getEventState();

   public abstract BControlPoint getPoint();

   public abstract BBacnetBitString getAckedTransitions();

   public abstract BBacnetTimeStamp[] getEventTimeStamps();

   public abstract BBacnetNotifyType getNotifyType();

   public abstract BBacnetBitString getEventEnable();

   public abstract int[] getEventPriorities();

   public abstract BBacnetNotificationClassDescriptor getNotificationClass();

   public abstract BEnum getEventType();

   @Override
   public final boolean isFatalFault() {
      return this.fatalFault;
   }

   private void checkFatalFault() {
      BBacnetExportTable exports = null;
      BLocalBacnetDevice local = null;
      BBacnetNetwork network = null;
      if (!this.fatalFault) {
         for (BComplex parent = this.getParent(); parent != null; parent = parent.getParent()) {
            if (parent instanceof BBacnetExportTable) {
               exports = (BBacnetExportTable)parent;
            } else if (parent instanceof BLocalBacnetDevice) {
               local = (BLocalBacnetDevice)parent;
               break;
            }
         }

         if (exports == null || local == null) {
            this.fatalFault = true;
            this.setFaultCause("Not under LocalBacnetDevice Export Table");
         } else if (local.isFatalFault()) {
            this.fatalFault = true;
            this.setFaultCause("LocalDevice fault: " + local.getFaultCause());
         } else {
            network = (BBacnetNetwork)local.getParent();
            if (network == null) {
               this.fatalFault = true;
               this.setFaultCause("Not under BacnetNetwork");
            } else if (network.isFatalFault()) {
               this.fatalFault = true;
               this.setFaultCause("Network fault: " + network.getFaultCause());
            } else if (!network.hasServerLicense()) {
               this.fatalFault = true;
               this.setFaultCause("Server capability not licensed");
            } else {
               this.setFaultCause("");
            }
         }
      }
   }

   protected PropertyValue readEventMessageTexts(int ndx) {
      if (ndx >= -1 && ndx <= 3) {
         BBacnetObjectIdentifier deviceId = BBacnetNetwork.localDevice().getObjectId();
         BEventHandler eventHandler = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler();
         AsnOutputStream out = new AsnOutputStream();
         boolean used = true;
         switch (ndx) {
            case -1:
               used = false;
            case 1:
               out.writeCharacterString(this.readEventMessageTextFromEventBuffer(eventHandler, 2, deviceId, "toOffNormalMsgText"));
               if (used) {
                  break;
               }
            case 2:
               out.writeCharacterString(this.readEventMessageTextFromEventBuffer(eventHandler, 1, deviceId, "toFaultMsgText"));
               if (used) {
                  break;
               }
            case 3:
               out.writeCharacterString(this.readEventMessageTextFromEventBuffer(eventHandler, 0, deviceId, "toNormalMsgText"));
               if (used) {
               }
               break;
            case 0:
               out.writeUnsignedInteger(3L);
         }

         return new NReadPropertyResult(351, ndx, out.toByteArray());
      } else {
         return new NReadPropertyResult(351, ndx, new NErrorType(2, 42));
      }
   }

   protected PropertyValue readEventMessageTextsConfig(String toOffnormalText, String toFaultText, String toNormalText, int ndx) {
      if (ndx >= -1 && ndx <= 3) {
         AsnOutputStream out = new AsnOutputStream();
         boolean used = true;
         switch (ndx) {
            case -1:
               used = false;
            case 1:
               out.writeCharacterString(toOffnormalText);
               if (used) {
                  break;
               }
            case 2:
               out.writeCharacterString(toFaultText);
               if (used) {
                  break;
               }
            case 3:
               out.writeCharacterString(toNormalText);
               break;
            case 0:
               out.writeUnsignedInteger(3L);
         }

         return new NReadPropertyResult(352, ndx, out.toByteArray());
      } else {
         return new NReadPropertyResult(352, ndx, new NErrorType(2, 42));
      }
   }

   protected static ErrorType writeEventMessageTextsConfig(int ndx, byte[] val, BAlarmSourceExt almExt) throws AsnException {
      if (ndx >= -1 && ndx <= 3) {
         switch (ndx) {
            case -1:
               BBacnetArray textsConfig = new BBacnetArray(BString.TYPE, 3);
               AsnUtil.fromAsn(-4, val, textsConfig);
               Context context = BLocalBacnetDevice.getBacnetContext();
               almExt.set(BAlarmSourceExt.toOffnormalText, BFormat.make(textsConfig.getElement(1).toString(null)), context);
               almExt.set(BAlarmSourceExt.toFaultText, BFormat.make(textsConfig.getElement(2).toString(null)), context);
               almExt.set(BAlarmSourceExt.toNormalText, BFormat.make(textsConfig.getElement(3).toString(null)), context);
               resetOutOfRangeTexts(almExt);
               break;
            case 0:
               return new NErrorType(2, 40);
            case 1:
               almExt.set(BAlarmSourceExt.toOffnormalText, BFormat.make(AsnUtil.fromAsnCharacterString(val)), BLocalBacnetDevice.getBacnetContext());
               resetOutOfRangeTexts(almExt);
               break;
            case 2:
               almExt.set(BAlarmSourceExt.toFaultText, BFormat.make(AsnUtil.fromAsnCharacterString(val)), BLocalBacnetDevice.getBacnetContext());
               break;
            case 3:
               almExt.set(BAlarmSourceExt.toNormalText, BFormat.make(AsnUtil.fromAsnCharacterString(val)), BLocalBacnetDevice.getBacnetContext());
         }

         return null;
      } else {
         return new NErrorType(2, 42);
      }
   }

   protected static void resetOutOfRangeTexts(BAlarmSourceExt almExt) {
      BOffnormalAlgorithm offnormal = almExt.getOffnormalAlgorithm();
      if (offnormal instanceof BOutOfRangeAlgorithm) {
         BOutOfRangeAlgorithm outOfRange = (BOutOfRangeAlgorithm)offnormal;
         outOfRange.setHighLimitText(BFormat.DEFAULT);
         outOfRange.setLowLimitText(BFormat.DEFAULT);
      }
   }

   protected PropertyValue readEventTimeStamps(BAbsTime lastOffnormalTime, BAbsTime lastFaultTime, BAbsTime lastToNormalTime, int ndx) {
      if (ndx >= -1 && ndx <= 3) {
         BBacnetObjectIdentifier deviceId = BBacnetNetwork.localDevice().getObjectId();
         BEventHandler eh = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler();
         AsnOutputStream asnOut = new AsnOutputStream();
         boolean used = true;
         switch (ndx) {
            case -1:
               used = false;
            case 1:
               if (this.getAlarmRecordFromEventBuffer(eh, 2, deviceId) != null) {
                  BBacnetTimeStamp.encodeTimeStamp(lastOffnormalTime, asnOut);
               } else {
                  BBacnetTimeStamp.encodeTimeStamp(BAbsTime.NULL, asnOut);
               }

               if (used) {
                  break;
               }
            case 2:
               if (this.getAlarmRecordFromEventBuffer(eh, 1, deviceId) != null) {
                  BBacnetTimeStamp.encodeTimeStamp(lastFaultTime, asnOut);
               } else {
                  BBacnetTimeStamp.encodeTimeStamp(BAbsTime.NULL, asnOut);
               }

               if (used) {
                  break;
               }
            case 3:
               if (this.getAlarmRecordFromEventBuffer(eh, 0, deviceId) != null) {
                  BBacnetTimeStamp.encodeTimeStamp(lastToNormalTime, asnOut);
               } else {
                  BBacnetTimeStamp.encodeTimeStamp(BAbsTime.NULL, asnOut);
               }

               if (used) {
               }
               break;
            case 0:
               asnOut.writeUnsignedInteger(3L);
         }

         return new NReadPropertyResult(130, ndx, asnOut.toByteArray());
      } else {
         return new NReadPropertyResult(130, ndx, new NErrorType(2, 42));
      }
   }

   protected BAlarmTransitionBits readEventTransition(BAlarmTransitionBits alarmTransitionBits) {
      int bits = 0;
      BBacnetObjectIdentifier deviceId = BBacnetNetwork.localDevice().getObjectId();
      BEventHandler eh = ((BBacnetStack)BBacnetNetwork.bacnet().getBacnetComm()).getServer().getEventHandler();
      if (this.getAlarmRecordFromEventBuffer(eh, 2, deviceId) != null) {
         if (alarmTransitionBits.isToOffnormal()) {
            bits |= 1;
         }
      } else {
         bits |= 1;
      }

      if (this.getAlarmRecordFromEventBuffer(eh, 1, deviceId) != null) {
         if (alarmTransitionBits.isToFault()) {
            bits |= 2;
         }
      } else {
         bits |= 2;
      }

      if (this.getAlarmRecordFromEventBuffer(eh, 0, deviceId) != null) {
         if (alarmTransitionBits.isToNormal()) {
            bits |= 4;
         }
      } else {
         bits |= 4;
      }

      return BAlarmTransitionBits.make(bits);
   }

   public void statusChanged() {
   }

   protected void removeEventFromEventBuffer() {
      BEventHandler eventHandler = BBacnetServerLayer.getServerLayer().getEventHandler();
      eventHandler.removeAllRecordFromEventBuffer(BBacnetNetwork.localDevice().getObjectId(), this.getObjectId(), 0L);
      eventHandler.removeEventSummary(this.getObjectId());
   }

   private String readEventMessageTextFromEventBuffer(BEventHandler eventHandler, int eventStateOrdinal, BBacnetObjectIdentifier deviceId, String msgTextKey) {
      BAlarmRecord rec = this.getAlarmRecordFromEventBuffer(eventHandler, eventStateOrdinal, deviceId);
      return rec != null ? rec.getAlarmData().gets(msgTextKey, "") : "";
   }

   private BAlarmRecord getAlarmRecordFromEventBuffer(BEventHandler eventHandler, int eventStateOrdinal, BBacnetObjectIdentifier deviceId) {
      BHashedEventBuffer eventBuffer = null;
      switch (eventStateOrdinal) {
         case 0:
            eventBuffer = eventHandler.getToNormalBuffer();
            break;
         case 1:
            eventBuffer = eventHandler.getToFaultBuffer();
            break;
         case 2:
            eventBuffer = eventHandler.getToOffnormalBuffer();
      }

      return eventBuffer == null ? null : eventBuffer.getRecord(deviceId, this.getObjectId(), 0L, false);
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetEventSource", 2);
      out.prop("fatalFault", this.fatalFault);
      out.endProps();
   }
}
