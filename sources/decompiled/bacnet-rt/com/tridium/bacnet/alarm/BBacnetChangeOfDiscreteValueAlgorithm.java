package com.tridium.bacnet.alarm;

import java.util.logging.Logger;
import javax.baja.alarm.ext.BAlarmState;
import javax.baja.alarm.ext.BOffnormalAlgorithm;
import javax.baja.control.BControlPoint;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BValue;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BBacnetChangeOfDiscreteValueAlgorithm extends BOffnormalAlgorithm {
   public static final Type TYPE = Sys.loadType(BBacnetChangeOfDiscreteValueAlgorithm.class);
   private static final Logger logger = Logger.getLogger("bacnet.alarm");
   private boolean isTimerRunning;
   private BValue oldValue;

   public Type getType() {
      return TYPE;
   }

   public BAlarmState checkAlarms(BStatusValue out, long toAlarmTimeDelay, long toNormalTimeDelay) {
      if (out.getStatus().isNull()) {
         this.oldValue = null;
         this.cancelTimer();
         return null;
      } else {
         BValue newValue = out.getValueValue();
         if (this.oldValue == null) {
            this.oldValue = newValue;
            this.cancelTimer();
            return null;
         } else if (!this.oldValue.getType().equals(newValue.getType())) {
            BControlPoint point = this.getParentPoint();
            logger.warning(
               "Point "
                  + (point != null ? point.getSlotPath() : "null")
                  + " should not have changed from initial value type: "
                  + this.oldValue.getType()
                  + " to new value type: "
                  + newValue.getType()
            );
            this.oldValue = newValue;
            this.cancelTimer();
            return null;
         } else {
            if (this.oldValue.equals(newValue)) {
               this.cancelTimer();
            } else {
               if (toNormalTimeDelay <= 0L) {
                  this.oldValue = newValue;
                  this.cancelTimer();
                  return BAlarmState.normal;
               }

               if (this.isTimerRunning) {
                  if (this.isTimerExpired()) {
                     this.oldValue = newValue;
                     this.cancelTimer();
                     return BAlarmState.normal;
                  }
               } else {
                  this.startTimer(toNormalTimeDelay);
               }
            }

            return null;
         }
      }
   }

   protected void startTimer(long timeDelay) {
      super.startTimer(timeDelay);
      this.isTimerRunning = true;
   }

   protected void cancelTimer() {
      super.cancelTimer();
      this.isTimerRunning = false;
   }
}
