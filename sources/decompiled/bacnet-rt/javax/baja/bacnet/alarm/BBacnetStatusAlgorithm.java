package javax.baja.bacnet.alarm;

import java.util.Map;
import javax.baja.alarm.ext.BAlarmSourceExt;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.alarm.ext.BOffnormalAlgorithm;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Context;
import javax.baja.sys.NotRunningException;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "alarmValues",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.make(new boolean[] {false, false, true, true})",
      flags = 64,
      facets = {@Facet("BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS")}
   ), @NiagaraProperty(
      name = "lastMonitoredValue",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      flags = 69,
      facets = {@Facet("BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS")}
   )})
public class BBacnetStatusAlgorithm extends BOffnormalAlgorithm {
   public static final Property alarmValues = newProperty(
      64, BBacnetBitString.make(new boolean[]{false, false, true, true}), BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS
   );
   public static final Property lastMonitoredValue = newProperty(
      69, BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")), BacnetBitStringUtil.BACNET_STATUS_FLAGS_FACETS
   );
   public static final Type TYPE = Sys.loadType(BBacnetStatusAlgorithm.class);
   private BBacnetStatusAlgorithm.TwoState current = new BBacnetStatusAlgorithm.NormalState();
   BBacnetBitString monitoredValue;

   public BBacnetBitString getAlarmValues() {
      return (BBacnetBitString)this.get(alarmValues);
   }

   public void setAlarmValues(BBacnetBitString v) {
      this.set(alarmValues, v, null);
   }

   public BBacnetBitString getLastMonitoredValue() {
      return (BBacnetBitString)this.get(lastMonitoredValue);
   }

   public void setLastMonitoredValue(BBacnetBitString v) {
      this.set(lastMonitoredValue, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() {
      BAlarmState currentState = ((BAlarmSourceExt)this.getParent()).getAlarmState();
      if (currentState == BAlarmState.offnormal) {
         this.current = new BBacnetStatusAlgorithm.OffnormalState();
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         this.executePoint();
      }
   }

   public void writeAlarmData(BStatusValue out, Map map) {
      super.writeAlarmData(out, map);
   }

   public BAlarmState checkAlarms(BStatusValue out, long toAlarmTimeDelay, long toNormalTimeDelay) {
      BAlarmSourceExt parent = (BAlarmSourceExt)this.getParent();
      if (parent == null) {
         throw new NotRunningException("BacnetStatusAlgorithm has no parent AlarmSourceExt");
      } else {
         BAlarmState currentState = parent.getAlarmState();
         if (this.current instanceof BBacnetStatusAlgorithm.ValidateOffnormalState) {
            return this.current.evaluate(this.isNormal(out), toAlarmTimeDelay);
         } else if (this.current instanceof BBacnetStatusAlgorithm.ValidateReturnFromOffnormalState) {
            return this.current.evaluate(this.isNormal(out), toNormalTimeDelay);
         } else if (currentState == BAlarmState.offnormal) {
            this.current = new BBacnetStatusAlgorithm.OffnormalState();
            return this.current.evaluate(this.isNormal(out), toNormalTimeDelay);
         } else {
            this.current = new BBacnetStatusAlgorithm.NormalState();
            return this.current.evaluate(this.isNormal(out), toAlarmTimeDelay);
         }
      }
   }

   protected BBacnetStatusAlgorithm.BacnetStatus isNormal(BStatusValue out) {
      if (this.monitoredValue != null) {
         if (this.monitoredValue.equals(BBacnetBitString.emptyBitString(4))) {
            this.setLastMonitoredValue(this.monitoredValue);
            return BBacnetStatusAlgorithm.BacnetStatus.NORMAL;
         }

         if (this.checkForBitIncrease()) {
            this.setLastMonitoredValue(this.monitoredValue);
            return BBacnetStatusAlgorithm.BacnetStatus.OFFNORMAL;
         }

         this.setLastMonitoredValue(this.monitoredValue);
      }

      return BBacnetStatusAlgorithm.BacnetStatus.IGNORE;
   }

   private boolean checkForBitIncrease() {
      for (int i = 0; i < this.monitoredValue.getBits().length; i++) {
         if (this.monitoredValue.getBits()[i] != this.getLastMonitoredValue().getBits()[i]
            && !this.getLastMonitoredValue().getBits()[i]
            && this.getAlarmValues().getBit(i)) {
            return true;
         }
      }

      return false;
   }

   public void setStausFlags(BBacnetBitString bacnetStatusFlags) {
      int size = bacnetStatusFlags.getBits().length;
      boolean[] copyBits = new boolean[size];

      for (int i = 0; i < size; i++) {
         copyBits[i] = this.getAlarmValues().getBit(i) ? bacnetStatusFlags.getBit(i) : false;
      }

      this.monitoredValue = BBacnetBitString.make(copyBits);
   }

   public void setStatusFlagsOnExtWhenOutOfServiceIsChanged(boolean outOfService) {
      if (outOfService) {
         if (this.monitoredValue == null) {
            this.setLastMonitoredValue(BBacnetBitString.make(new boolean[]{false, false, false, false}));
            this.monitoredValue = BBacnetBitString.make(new boolean[]{false, false, false, true});
         } else {
            this.monitoredValue = BBacnetBitString.make(
               new boolean[]{this.getLastMonitoredValue().getBit(0), this.getLastMonitoredValue().getBit(1), this.getLastMonitoredValue().getBit(2), true}
            );
         }
      } else {
         this.monitoredValue = BBacnetBitString.make(
            new boolean[]{this.getLastMonitoredValue().getBit(0), this.getLastMonitoredValue().getBit(1), this.getLastMonitoredValue().getBit(2), false}
         );
      }
   }

   private void transition(BBacnetStatusAlgorithm.TwoState state) {
      this.current = state;
   }

   private BAlarmState getAlarmStateForOffnormalStatus(long timeDelay) {
      if (timeDelay == 0L) {
         this.transition(new BBacnetStatusAlgorithm.OffnormalState());
         return BAlarmState.offnormal;
      } else {
         this.transition(new BBacnetStatusAlgorithm.ValidateOffnormalState(timeDelay));
         return null;
      }
   }

   static enum BacnetStatus {
      NORMAL,
      OFFNORMAL,
      IGNORE;
   }

   private class NormalState extends BBacnetStatusAlgorithm.TwoState {
      private NormalState() {
      }

      @Override
      public String tag() {
         return "Normal";
      }

      @Override
      public BAlarmState evaluate(BBacnetStatusAlgorithm.BacnetStatus bacnetStatus, long timeDelay) {
         return bacnetStatus == BBacnetStatusAlgorithm.BacnetStatus.OFFNORMAL ? BBacnetStatusAlgorithm.this.getAlarmStateForOffnormalStatus(timeDelay) : null;
      }
   }

   private class OffnormalState extends BBacnetStatusAlgorithm.TwoState {
      private OffnormalState() {
      }

      @Override
      public String tag() {
         return "OffnormalState";
      }

      @Override
      public BAlarmState evaluate(BBacnetStatusAlgorithm.BacnetStatus bacnetStatus, long timeDelay) {
         if (bacnetStatus == BBacnetStatusAlgorithm.BacnetStatus.NORMAL) {
            if (timeDelay == 0L) {
               BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new NormalState());
               return BAlarmState.normal;
            } else {
               BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new ValidateReturnFromOffnormalState(timeDelay));
               return null;
            }
         } else {
            return bacnetStatus == BBacnetStatusAlgorithm.BacnetStatus.OFFNORMAL
               ? BBacnetStatusAlgorithm.this.getAlarmStateForOffnormalStatus(timeDelay)
               : null;
         }
      }
   }

   private abstract class TwoState {
      public TwoState() {
         BBacnetStatusAlgorithm.this.cancelTimer();
      }

      public abstract String tag();

      public abstract BAlarmState evaluate(BBacnetStatusAlgorithm.BacnetStatus var1, long var2);
   }

   private class ValidateOffnormalState extends BBacnetStatusAlgorithm.ValidateState {
      public ValidateOffnormalState(long timeDelay) {
         BBacnetStatusAlgorithm.this.startTimer(timeDelay);
      }

      @Override
      public String tag() {
         return "ValidateOffnormalState";
      }

      @Override
      public BAlarmState evaluate(BBacnetStatusAlgorithm.BacnetStatus bacnetStatus, long timeDelay) {
         if (bacnetStatus == BBacnetStatusAlgorithm.BacnetStatus.NORMAL) {
            BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new NormalState());
            return null;
         } else if (BBacnetStatusAlgorithm.this.isTimerExpired()) {
            BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new OffnormalState());
            return BAlarmState.offnormal;
         } else {
            return null;
         }
      }
   }

   private class ValidateReturnFromOffnormalState extends BBacnetStatusAlgorithm.ValidateState {
      public ValidateReturnFromOffnormalState(long timeDelay) {
         BBacnetStatusAlgorithm.this.startTimer(timeDelay);
      }

      @Override
      public String tag() {
         return "ValidateReturnFromOffnormalState";
      }

      @Override
      public BAlarmState evaluate(BBacnetStatusAlgorithm.BacnetStatus bacnetStatus, long timeDelay) {
         if (bacnetStatus == BBacnetStatusAlgorithm.BacnetStatus.NORMAL) {
            if (BBacnetStatusAlgorithm.this.isTimerExpired()) {
               BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new NormalState());
               return BAlarmState.normal;
            } else {
               return null;
            }
         } else {
            BBacnetStatusAlgorithm.this.transition(BBacnetStatusAlgorithm.this.new OffnormalState());
            return null;
         }
      }
   }

   private abstract class ValidateState extends BBacnetStatusAlgorithm.TwoState {
      private ValidateState() {
      }
   }
}
