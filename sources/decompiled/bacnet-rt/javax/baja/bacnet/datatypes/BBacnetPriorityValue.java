package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.AsnUtil;
import java.util.logging.Logger;
import javax.baja.bacnet.enums.BBacnetBinaryPv;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sync.Transaction;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "value",
   type = "BSimple",
   defaultValue = "BBacnetNull.DEFAULT",
   flags = 4
)
public final class BBacnetPriorityValue extends BStruct implements BIBacnetDataType {
   public static final Property value = newProperty(4, BBacnetNull.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetPriorityValue.class);
   private int choice;
   private static final Logger logger = Logger.getLogger("bacnet.debug");
   public static final String INVALID_CHOICE = "invalid choice";
   public static final String NULL_CHOICE = "NULL";
   public static final String REAL_CHOICE = "Real";
   public static final String DOUBLE_CHOICE = "Double";
   public static final String BINARY_CHOICE = "Binary";
   public static final String UNSIGNED_CHOICE = "Unsigned";
   public static final String INTEGER_CHOICE = "Integer";
   public static final String STRING_CHOICE = "String";
   public static final String OCTET_STRING_CHOICE = "OctetString";
   public static final String BIT_STRING_CHOICE = "BitString";
   public static final String DATE_CHOICE = "Date";
   public static final String TIME_CHOICE = "Time";
   public static final String DATE_TIME_CHOICE = "DateTime";
   public static final String CONSTRUCTED_VALUE_CHOICE = "ConstructedValue";
   public static final int CONSTRUCTED_VALUE_TAG = 0;
   private static final int DATE_TIME_TAG = 1;
   private static final int NULL_TAG = -1;
   private static final int REAL_TAG = -2;
   private static final int BINARY_TAG = -3;
   private static final int UNSIGNED_TAG = -4;
   private static final int DOUBLE_TAG = -5;
   private static final int STRING_TAG = -6;
   private static final int INTEGER_TAG = -7;
   private static final int OCTET_STRING_TAG = -8;
   private static final int BIT_STRING_TAG = -9;
   private static final int DATE_TAG = -10;
   private static final int TIME_TAG = -11;

   public BSimple getValue() {
      return (BSimple)this.get(value);
   }

   public void setValue(BSimple v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetPriorityValue() {
   }

   public BBacnetPriorityValue(BBacnetNull bacnetNull) {
      this.choice = -1;
      this.setValue(BBacnetNull.DEFAULT);
   }

   public BBacnetPriorityValue(BFloat real) {
      this.choice = -2;
      this.setValue(real);
   }

   public BBacnetPriorityValue(BDouble dble) {
      this.choice = -5;
      this.setValue(dble);
   }

   public BBacnetPriorityValue(BBacnetBinaryPv binary) {
      this.choice = -3;
      this.setValue(binary);
   }

   public BBacnetPriorityValue(BInteger integer) {
      this.choice = -7;
      this.setValue(integer);
   }

   public BBacnetPriorityValue(BString str) {
      this.choice = -6;
      this.setValue(str);
   }

   public BBacnetPriorityValue(BSimple constructedValue) {
      this.choice = 0;
      this.setValue(constructedValue);
   }

   public int choice() {
      Type t = this.getValue().getType();
      if (t == BBacnetNull.TYPE) {
         this.choice = -1;
      } else if (t == BFloat.TYPE) {
         this.choice = -2;
      } else if (t == BDouble.TYPE) {
         this.choice = -5;
      } else if (t == BBacnetBinaryPv.TYPE) {
         this.choice = -3;
      } else if (t == BBacnetUnsigned.TYPE) {
         this.choice = -4;
      } else if (t == BInteger.TYPE) {
         this.choice = -7;
      } else if (t == BString.TYPE) {
         this.choice = -6;
      } else if (t == BBacnetOctetString.TYPE) {
         this.choice = -8;
      } else if (t == BBacnetBitString.TYPE) {
         this.choice = -9;
      } else if (t == BBacnetDate.TYPE) {
         this.choice = -10;
      } else if (t == BBacnetTime.TYPE) {
         this.choice = -11;
      } else if (t == BAbsTime.TYPE) {
         this.choice = 1;
      } else {
         this.choice = 0;
      }

      return this.choice;
   }

   public boolean isNull() {
      return this.choice() == -1;
   }

   public BValue getPriorityValue() {
      return this.getValue();
   }

   public void setPriorityValue(BValue v) {
      this.setPriorityValue(v, null);
   }

   public void setPriorityValue(BValue v, Context cx) {
      Context myCx = cx;
      BComponent c = this.getParentComponent();
      if ((c == null || !c.isMounted()) && cx != null && cx instanceof Transaction) {
         myCx = null;
      }

      Type t = v.getType();
      if (t == BBacnetNull.TYPE) {
         this.choice = -1;
         this.set(value, v, myCx);
      } else if (t == BFloat.TYPE) {
         this.choice = -2;
         this.set(value, v, myCx);
      } else if (t == BDouble.TYPE) {
         this.choice = -5;
         this.set(value, v, myCx);
      } else if (t == BBacnetBinaryPv.TYPE) {
         this.choice = -3;
         this.set(value, v, myCx);
      } else if (t == BBacnetUnsigned.TYPE) {
         this.choice = -4;
         this.set(value, v, myCx);
      } else if (t == BInteger.TYPE) {
         this.choice = -7;
         this.set(value, v, myCx);
      } else if (t == BString.TYPE) {
         this.choice = -6;
         this.set(value, v, myCx);
      } else if (t == BBacnetOctetString.TYPE) {
         this.choice = -8;
         this.set(value, v, myCx);
      } else if (t == BBacnetBitString.TYPE) {
         this.choice = -9;
         this.set(value, v, myCx);
      } else if (t == BBacnetDate.TYPE) {
         this.choice = -10;
         this.set(value, v, myCx);
      } else if (t == BBacnetTime.TYPE) {
         this.choice = -11;
         this.set(value, v, myCx);
      } else if (t == BAbsTime.TYPE) {
         this.choice = 1;
         this.set(value, v, myCx);
      } else {
         if (!(v instanceof BSimple)) {
            throw new IllegalArgumentException("BacnetPriorityValue:Cannot handle constructed types! v=" + v + " [" + v.getType() + "]");
         }

         this.choice = 0;
         this.set(value, v, myCx);
      }
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.choice()) {
         case -11:
            out.writeTime((BBacnetTime)this.getValue());
            break;
         case -10:
            out.writeDate((BBacnetDate)this.getValue());
            break;
         case -9:
            out.writeBitString((BBacnetBitString)this.getValue());
            break;
         case -8:
            out.writeOctetString((BBacnetOctetString)this.getValue());
            break;
         case -7:
            out.writeSignedInteger((BInteger)this.getValue());
            break;
         case -6:
            out.writeCharacterString((BString)this.getValue());
            break;
         case -5:
            out.writeDouble((BDouble)this.getValue());
            break;
         case -4:
            out.writeUnsigned((BBacnetUnsigned)this.getValue());
            break;
         case -3:
            out.writeEnumerated((BBacnetBinaryPv)this.getValue());
            break;
         case -2:
            out.writeReal((BFloat)this.getValue());
            break;
         case -1:
            out.writeNull();
            break;
         case 0:
            logger.fine("write constructed value in BacnetPriorityValue!");
            out.writeEncodedValue(0, AsnUtil.toAsn(this.getValue()));
            break;
         case 1:
            out.writeOpeningTag(1);
            BBacnetDateTime dateTime = new BBacnetDateTime((BAbsTime)this.getValue());
            dateTime.writeAsn(out);
            out.writeClosingTag(1);
            break;
         default:
            throw new IllegalStateException("Invalid priority value type:" + this.choice);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      switch (tag) {
         case 0:
            this.setPriorityValue(in.readNull(), noWrite);
            break;
         case 1:
         default:
            if (in.isOpeningTag(0)) {
               BValue o = AsnUtil.asnToValue(null, in.readEncodedValue(0));
               if (o instanceof BSimple) {
                  this.setPriorityValue(o, noWrite);
               } else {
                  logger.fine("non-BSimple constructed value not supported in BacnetPriorityValue!");
               }
            } else {
               if (!in.isOpeningTag(1)) {
                  throw new AsnException("Invalid tag: " + tag);
               }

               in.skipOpeningTag(1);
               BBacnetDateTime dateTime = new BBacnetDateTime();
               dateTime.readAsn(in);
               in.skipClosingTag(1);
               this.setPriorityValue(dateTime.toBAbsTime(), noWrite);
            }
            break;
         case 2:
            this.setPriorityValue(in.readUnsigned(), noWrite);
            break;
         case 3:
            this.setPriorityValue(in.readSigned(), noWrite);
            break;
         case 4:
            this.setPriorityValue(BFloat.make(in.readReal()), noWrite);
            break;
         case 5:
            this.setPriorityValue(BDouble.make(in.readDouble()), noWrite);
            break;
         case 6:
            this.setPriorityValue(in.readBacnetOctetString(), noWrite);
            break;
         case 7:
            this.setPriorityValue(BString.make(in.readCharacterString()), noWrite);
            break;
         case 8:
            this.setPriorityValue(in.readBitString(), noWrite);
            break;
         case 9:
            this.setPriorityValue(BBacnetBinaryPv.make(in.readEnumerated()), noWrite);
            break;
         case 10:
            this.setPriorityValue(in.readDate(), noWrite);
            break;
         case 11:
            this.setPriorityValue(in.readTime(), noWrite);
      }
   }

   public String toString(Context context) {
      return this.getValue().toString(context);
   }

   public String choiceName() {
      return choiceName(this.choice());
   }

   public static String choiceName(int choice) {
      switch (choice) {
         case -11:
            return "Time";
         case -10:
            return "Date";
         case -9:
            return "BitString";
         case -8:
            return "OctetString";
         case -7:
            return "Integer";
         case -6:
            return "String";
         case -5:
            return "Double";
         case -4:
            return "Unsigned";
         case -3:
            return "Binary";
         case -2:
            return "Real";
         case -1:
            return "NULL";
         case 0:
            return "ConstructedValue";
         case 1:
            return "DateTime";
         default:
            return "invalid choice";
      }
   }

   public static int choice(String choiceName) {
      if ("NULL".equals(choiceName)) {
         return -1;
      } else if ("Real".equals(choiceName)) {
         return -2;
      } else if ("Double".equals(choiceName)) {
         return -5;
      } else if ("Binary".equals(choiceName)) {
         return -3;
      } else if ("Unsigned".equals(choiceName)) {
         return -4;
      } else if ("Integer".equals(choiceName)) {
         return -7;
      } else if ("String".equals(choiceName)) {
         return -6;
      } else if ("OctetString".equals(choiceName)) {
         return -8;
      } else if ("BitString".equals(choiceName)) {
         return -9;
      } else if ("Date".equals(choiceName)) {
         return -10;
      } else if ("Time".equals(choiceName)) {
         return -11;
      } else {
         return "DateTime".equals(choiceName) ? 1 : 0;
      }
   }
}
