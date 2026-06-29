package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.BacUtil;
import com.tridium.bacnet.asn.AsnInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetLifeSafetyState;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BBlob;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "choice",
   type = "int",
   defaultValue = "0"
)
public class BBacnetFaultParameter extends BComponent implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetFaultParameter.class);
   public static final int NONE = 0;
   public static final int FAULT_CHARACTERSTRING = 1;
   public static final int FAULT_CHARACTERSTRING_LIST_OF_FAULT_VALUES = 0;
   public static final int FAULT_EXTENDED = 2;
   public static final int FAULT_EXTENDED_VENDOR_ID = 0;
   public static final int FAULT_EXTENDED_EXTENDED_FAULT_TYPE = 1;
   public static final int FAULT_EXTENDED_PARAMETERS = 2;
   public static final int FAULT_LIFE_SAFETY = 3;
   public static final int FAULT_LIFE_SAFETY_LIST_OF_FAULT_VALUES = 0;
   public static final int FAULT_LIFE_SAFETY_MODE_PROPERTY_REFERENCE = 1;
   public static final int FAULT_STATE = 4;
   public static final int FAULT_STATE_LIST_OF_FAULT_VALUES = 0;
   public static final int FAULT_STATUS_FLAGS = 5;
   public static final int FAULT_STATUS_FLAGS_STATUS_FLAGS_REFERENCE = 0;
   public static final int FAULT_OUT_OF_RANGE = 6;
   private static final int MAX_FAULT_PARAMETER_CHOICE = 6;
   private static final Logger logger = Logger.getLogger("bacnet.datatypes");

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      int choice = this.getChoice();
      if (choice >= 0 && choice <= 6) {
         out.writeOpeningTag(choice);

         try {
            switch (choice) {
               case 0:
                  out.writeNull();
                  break;
               case 1:
                  out.writeOpeningTag(0);
                  ((BBacnetListOf)this.get("listOfFaultValues")).writeAsn(out);
                  out.writeClosingTag(0);
                  break;
               case 2:
                  out.writeUnsigned(0, (BBacnetUnsigned)this.get("vendorId"));
                  out.writeUnsigned(1, (BBacnetUnsigned)this.get("extendedFaultType"));
                  out.writeEncodedValue(2, ((BBlob)this.get("parameters")).copyBytes());
                  break;
               case 3:
                  out.writeOpeningTag(0);
                  ((BBacnetListOf)this.get("listOfFaultValues")).writeAsn(out);
                  out.writeClosingTag(0);
                  out.writeOpeningTag(1);
                  ((BBacnetDeviceObjectPropertyReference)this.get("modePropertyReference")).writeAsn(out);
                  out.writeClosingTag(1);
                  break;
               case 4:
                  out.writeOpeningTag(0);
                  ((BBacnetListOf)this.get("listOfFaultValues")).writeAsn(out);
                  out.writeClosingTag(0);
                  break;
               case 5:
                  out.writeOpeningTag(0);
                  ((BBacnetDeviceObjectPropertyReference)this.get("statusFlagsReference")).writeAsn(out);
                  out.writeClosingTag(0);
                  break;
               case 6:
                  out.writeOpeningTag(0);
                  this.writeFaultOutOfRangeValue("minNormalValue", out);
                  out.writeClosingTag(0);
                  out.writeOpeningTag(1);
                  this.writeFaultOutOfRangeValue("maxNormalValue", out);
                  out.writeClosingTag(1);
            }
         } catch (Exception var4) {
            logger.log(Level.SEVERE, "BBacnetFaultParameter writeAsn failure", (Throwable)var4);
            throw new IllegalStateException(var4);
         }

         out.writeClosingTag(choice);
      } else {
         throw new IllegalStateException("Invalid BACnetFaultParameter choice: " + choice);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (tag >= 0 && tag <= 6) {
         in.skipOpeningTag(tag);
         switch (tag) {
            case 0:
               in.readNull();
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               break;
            case 1: {
               BBacnetListOf listOfFaultValues = new BBacnetListOf(BString.TYPE);
               listOfFaultValues.readAsn(AsnInputStream.make(in.readEncodedValue(0)));
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "listOfFaultValues", listOfFaultValues, noWrite);
               break;
            }
            case 2:
               long vendorId = in.readUnsignedInteger(0);
               long extendedFaultType = in.readUnsignedInteger(1);
               byte[] parameters = in.readEncodedValue(2);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "vendorId", BBacnetUnsigned.make(vendorId), noWrite);
               BacUtil.setOrAdd(this, "extendedFaultType", BBacnetUnsigned.make(extendedFaultType), noWrite);
               BacUtil.setOrAdd(this, "parameters", BBlob.make(parameters), noWrite);
               break;
            case 3: {
               BBacnetListOf listOfFaultValues = new BBacnetListOf(BBacnetLifeSafetyState.TYPE);
               listOfFaultValues.readAsn(AsnInputStream.make(in.readEncodedValue(0)));
               BBacnetDeviceObjectPropertyReference modePropertyReference = new BBacnetDeviceObjectPropertyReference();
               in.skipOpeningTag(1);
               modePropertyReference.readAsn(in);
               in.skipClosingTag(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "listOfFaultValues", listOfFaultValues, noWrite);
               BacUtil.setOrAdd(this, "modePropertyReference", modePropertyReference, noWrite);
               break;
            }
            case 4: {
               BBacnetListOf listOfFaultValues = new BBacnetListOf(BBacnetPropertyStates.TYPE);
               listOfFaultValues.readAsn(AsnInputStream.make(in.readEncodedValue(0)));
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "listOfFaultValues", listOfFaultValues, noWrite);
               break;
            }
            case 5:
               BBacnetDeviceObjectPropertyReference statusFlagsReference = new BBacnetDeviceObjectPropertyReference();
               in.skipOpeningTag(0);
               statusFlagsReference.readAsn(in);
               in.skipClosingTag(0);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "statusFlagsReference", statusFlagsReference, noWrite);
               break;
            case 6:
               in.skipOpeningTag(0);
               BValue minNormalValue = readFaultOutOfRangeValue(in);
               in.skipClosingTag(0);
               in.skipOpeningTag(1);
               BValue maxNormalValue = readFaultOutOfRangeValue(in);
               in.skipClosingTag(1);
               in.skipClosingTag(tag);
               this.updateChoice(tag);
               BacUtil.setOrAdd(this, "minNormalValue", minNormalValue, noWrite);
               BacUtil.setOrAdd(this, "maxNormalValue", maxNormalValue, noWrite);
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   private void updateChoice(int choice) {
      if (choice != this.getChoice()) {
         this.removeAll(noWrite);
      }

      this.setInt(BBacnetFaultParameter.choice, choice, noWrite);
   }

   private static BValue readFaultOutOfRangeValue(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      switch (tag) {
         case 2:
            return in.readUnsigned();
         case 3:
            return in.readSigned();
         case 4:
            return BFloat.make(in.readReal());
         case 5:
            return BDouble.make(in.readDouble());
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   private void writeFaultOutOfRangeValue(String propertyName, AsnOutput out) {
      BValue value = this.get(propertyName);
      if (value instanceof BFloat) {
         out.writeReal((BFloat)value);
      } else if (value instanceof BBacnetUnsigned) {
         out.writeUnsigned((BBacnetUnsigned)value);
      } else if (value instanceof BDouble) {
         out.writeDouble((BDouble)value);
      } else {
         if (!(value instanceof BInteger)) {
            throw new IllegalStateException("Invalid Fault OutOfRange Value type: " + (value != null ? value.getType() : "null"));
         }

         out.writeSignedInteger((BInteger)value);
      }
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetFaultParameter", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }
}
