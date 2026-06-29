package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.lighting.BBacnetLightingOperation;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BLong;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "operation",
   type = "BBacnetLightingOperation",
   defaultValue = "BBacnetLightingOperation.DEFAULT"
)
public class BBacnetLightingCommand extends BComponent implements BIBacnetDataType {
   public static final Property operation = newProperty(0, BBacnetLightingOperation.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetLightingCommand.class);
   public static final int OPERATION_TAG = 0;
   public static final int TARGET_LEVEL_TAG = 1;
   public static final int RAMP_RATE_TAG = 2;
   public static final int STEP_INCREMENT_TAG = 3;
   public static final int FADE_TIME_TAG = 4;
   public static final int PRIORITY_TAG = 5;

   public BBacnetLightingOperation getOperation() {
      return (BBacnetLightingOperation)this.get(operation);
   }

   public void setOperation(BBacnetLightingOperation v) {
      this.set(operation, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetLightingCommand() {
   }

   public BBacnetLightingCommand(BBacnetLightingOperation operation) {
      this.setOperation(operation);
   }

   public BBacnetLightingCommand(BBacnetLightingOperation operation, Float targetLevel, Float rampRate, Float stepIncrement, Long fadeTime, Integer priority) {
      this.setOperation(operation);
      if (targetLevel != null) {
         this.setTargetLevel(targetLevel);
      }

      if (rampRate != null) {
         this.setRampRate(rampRate);
      }

      if (stepIncrement != null) {
         this.setStepIncrement(stepIncrement);
      }

      if (fadeTime != null) {
         this.setFadeTime(fadeTime);
      }

      if (priority != null) {
         this.setPriority(priority);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(0, this.getOperation());
      Float targetLevel = this.getTargetLevel();
      if (targetLevel != null) {
         out.writeReal(1, targetLevel.floatValue());
      }

      Float rampRate = this.getRampRate();
      if (rampRate != null) {
         out.writeReal(2, rampRate.floatValue());
      }

      Float stepIncrement = this.getStepIncrement();
      if (stepIncrement != null) {
         out.writeReal(3, stepIncrement.floatValue());
      }

      Long fadeTime = this.getFadeTime();
      if (fadeTime != null) {
         out.writeUnsignedInteger(4, fadeTime);
      }

      Integer priority = this.getPriority();
      if (priority != null) {
         out.writeUnsignedInteger(5, priority.intValue());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int operation = in.readEnumerated(0);
      float targetLevel = -1.0F;
      if (in.peekTag() == 1) {
         targetLevel = in.readReal(1);
      }

      float rampRate = -1.0F;
      if (in.peekTag() == 2) {
         rampRate = in.readReal(2);
      }

      float stepIncrement = -1.0F;
      if (in.peekTag() == 3) {
         stepIncrement = in.readReal(3);
      }

      long fadeTime = -1L;
      if (in.peekTag() == 4) {
         fadeTime = in.readUnsignedInteger(4);
      }

      int priority = -1;
      if (in.peekTag() == 5) {
         priority = in.readUnsignedInt(5);
      }

      this.set(BBacnetLightingCommand.operation, BBacnetLightingOperation.make(operation), noWrite);
      if (targetLevel >= 0.0F) {
         this.setTargetLevel(targetLevel);
      }

      if (rampRate >= 0.0F) {
         this.setRampRate(rampRate);
      }

      if (stepIncrement >= 0.0F) {
         this.setStepIncrement(stepIncrement);
      }

      if (fadeTime >= 0L) {
         this.setFadeTime(fadeTime);
      }

      if (priority >= 0) {
         this.setPriority(priority);
      }
   }

   public Float getTargetLevel() {
      return this.getFloat("targetLevel");
   }

   public void setTargetLevel(Float targetLevel) {
      this.add("targetLevel", BFloat.make(targetLevel));
   }

   public Float getRampRate() {
      return this.getFloat("rampRate");
   }

   public void setRampRate(Float rampRate) {
      this.add("rampRate", BFloat.make(rampRate));
   }

   public Float getStepIncrement() {
      return this.getFloat("stepIncrement");
   }

   public void setStepIncrement(Float stepIncrement) {
      this.add("stepIncrement", BFloat.make(stepIncrement));
   }

   public Long getFadeTime() {
      BLong fadeTime = (BLong)this.get("fadeTime");
      return fadeTime != null ? fadeTime.getLong() : null;
   }

   public void setFadeTime(Long fadeTime) {
      this.add("fadeTime", BLong.make(fadeTime));
   }

   public Integer getPriority() {
      BInteger priority = (BInteger)this.get("priority");
      return priority != null ? priority.getInt() : null;
   }

   public void setPriority(Integer priority) {
      this.add("priority", BInteger.make(priority));
   }

   public Float getFloat(String name) {
      BFloat bFloat = (BFloat)this.get(name);
      return bFloat != null ? bFloat.getFloat() : null;
   }

   public String toString(Context context) {
      StringBuilder sb = new StringBuilder("" + this.getOperation());
      sb.append("\n\tTargetLevel: ")
         .append(this.getTargetLevel())
         .append("\n\tRampRate: ")
         .append(this.getRampRate())
         .append("\n\tStepIncrement: ")
         .append(this.getStepIncrement())
         .append("\n\tFadeTime: ")
         .append(this.getFadeTime())
         .append("\n\tPriority: ")
         .append(this.getPriority());
      return sb.toString();
   }
}
