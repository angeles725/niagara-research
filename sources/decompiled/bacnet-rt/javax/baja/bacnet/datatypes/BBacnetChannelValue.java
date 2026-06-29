package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.virtual.BacnetVirtualUtil;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperty(
   name = "choice",
   type = "int",
   defaultValue = "0",
   flags = 4
)
public class BBacnetChannelValue extends BComponent implements BIBacnetDataType {
   public static final Property choice = newProperty(4, 0, null);
   public static final Type TYPE = Sys.loadType(BBacnetChannelValue.class);
   private static final int MAX_TAG = 254;
   public static final int NULL_TAG = 0;
   public static final int REAL_TAG = 1;
   public static final int ENUMERATED_TAG = 2;
   public static final int UNSIGNED_TAG = 3;
   public static final int BOOLEAN_TAG = 4;
   public static final int SIGNED_TAG = 5;
   public static final int DOUBLE_TAG = 6;
   public static final int TIME_TAG = 7;
   public static final int CHARACTERSTRING_TAG = 8;
   public static final int OCTETSTRING_TAG = 9;
   public static final int BITSTRING_TAG = 10;
   public static final int DATE_TAG = 11;
   public static final int OBJECTID_TAG = 12;
   public static final int LIGHTCOMMAND_TAG = 13;
   public static final int LIGHTCOMMAND_SUB_TAG = 0;
   private static final int MAX_DEFINED_CHOICE = 13;
   private static final int MAX_ASHRAE_CHOICE = 63;
   private static final Lexicon lex = Lexicon.make("bacnet");
   private static String[] tags = new String[]{
      lex.getText("BacnetChannel.null"),
      lex.getText("BacnetChannel.real"),
      lex.getText("BacnetChannel.enumerated"),
      lex.getText("BacnetChannel.unsigned"),
      lex.getText("BacnetChannel.boolean"),
      lex.getText("BacnetChannel.signed"),
      lex.getText("BacnetChannel.double"),
      lex.getText("BacnetChannel.time"),
      lex.getText("BacnetChannel.characterString"),
      lex.getText("BacnetChannel.octetString"),
      lex.getText("BacnetChannel.bitString"),
      lex.getText("BacnetChannel.date"),
      lex.getText("BacnetChannel.objectId"),
      lex.getText("BacnetChannel.lightingCommand"),
      lex.getText("BacnetChannel.ashrae"),
      lex.getText("BacnetChannel.proprietary"),
      lex.getText("BacnetChannel.invalid")
   };
   private static String[] slotNames = new String[]{
      "null",
      "real",
      "enumerated",
      "unsigned",
      "boolean",
      "signed",
      "double",
      "time",
      "characterString",
      "octetString",
      "bitString",
      "date",
      "objectId",
      "lightingCommand"
   };
   private static final int ASHRAE_CHOICE_INDEX = 14;
   private static final int PROPRIETARY_CHOICE_INDEX = 15;
   private static final int INVALID_CHOICE_INDEX = 16;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getTag());
      int choice = this.getChoice();
      if (choice > 0 && choice < slotNames.length) {
         sb.append(slotNames[choice]).append(":").append(this.get(slotNames[choice]));
      }

      return sb.toString();
   }

   @Override
   public void writeAsn(AsnOutput out) {
      int tag = this.getChoice();
      switch (tag) {
         case 1:
            out.writeReal(tag, (BNumber)this.get("real"));
            break;
         case 2:
            out.writeEnumerated(tag, ((BInteger)this.get("enumerated")).getInt());
            break;
         case 3:
            out.writeUnsigned(tag, (BBacnetUnsigned)this.get("unsigned"));
            break;
         case 4:
            out.writeBoolean(tag, (BBoolean)this.get("boolean"));
            break;
         case 5:
            out.writeSignedInteger(tag, (BInteger)this.get("signed"));
            break;
         case 6:
            out.writeDouble(tag, (BDouble)this.get("double"));
            break;
         case 7:
            out.writeTime(tag, (BBacnetTime)this.get("time"));
            break;
         case 8:
            out.writeCharacterString(tag, (BString)this.get("characterString"));
            break;
         case 9:
            out.writeOctetString(tag, (BBacnetOctetString)this.get("octetString"));
            break;
         case 10:
            out.writeBitString(tag, (BBacnetBitString)this.get("bitString"));
            break;
         case 11:
            out.writeDate(tag, (BBacnetDate)this.get("date"));
            break;
         case 12:
            out.writeObjectIdentifier(tag, (BBacnetObjectIdentifier)this.get("objectId"));
            break;
         case 13:
            BBacnetLightingCommand lightingCommand = (BBacnetLightingCommand)this.get("lightCommand");
            out.writeOpeningTag(0);
            lightingCommand.writeAsn(out);
            out.writeClosingTag(0);
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (tag >= 0 && tag <= 254) {
         this.setInt(choice, tag, noWrite);
         this.removeAll(noWrite);
         switch (tag) {
            case 1:
               this.add("real", BFloat.make(in.readReal(tag)), noWrite);
               break;
            case 2:
               this.add("enumerated", BInteger.make(in.readEnumerated(tag)), noWrite);
               break;
            case 3:
               this.add("unsigned", BBacnetUnsigned.make(in.readEnumerated(tag)), noWrite);
               break;
            case 4:
               this.add("unsigned", BBoolean.make(in.readBoolean(tag)), noWrite);
               break;
            case 5:
               this.add("signed", in.readSigned(tag), noWrite);
               break;
            case 6:
               this.add("signed", BDouble.make(in.readDouble(tag)), noWrite);
               break;
            case 7:
               this.add("time", in.readTime(7), noWrite);
               break;
            case 8:
               this.add("characterString", BString.make(in.readCharacterString(7)), noWrite);
               break;
            case 9:
               this.add("octetString", in.readBacnetOctetString(9), noWrite);
               break;
            case 10:
               this.add("bitString", in.readBitString(10), noWrite);
               break;
            case 11:
               this.add("date", in.readDate(11), noWrite);
               break;
            case 12:
               this.add("objectId", in.readObjectIdentifier(12), noWrite);
               break;
            case 13:
               in.skipOpeningTag(0);
               BBacnetLightingCommand lightCommand = new BBacnetLightingCommand();
               lightCommand.readAsn(in);
               this.add("lightCommand", lightCommand, noWrite);
               in.skipClosingTag(0);
               break;
            default:
               in.skipTag();
         }
      } else {
         throw new AsnException("Invalid tag: " + tag);
      }
   }

   private final void addEnum(BEnum defEnum, AsnInput in, int tag) throws AsnException {
      this.add(slotNames[tag], BDynamicEnum.make(in.readEnumerated(tag), defEnum.getRange()), noWrite);
   }

   public void spy(SpyWriter out) throws Exception {
      super.spy(out);
      out.startProps();
      out.trTitle("BacnetPropertyStates", 2);
      out.prop("virtual", BacnetVirtualUtil.isVirtual(this));
      out.endProps();
   }

   private String getTag() {
      int ch = this.getChoice();
      if (ch < 0 || ch > 254) {
         ch = 16;
      }

      if (ch <= 13) {
         return tags[ch];
      } else {
         ch = ch <= 63 ? 14 : 15;
         return tags[ch];
      }
   }
}
