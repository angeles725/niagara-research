package javax.baja.bacnet.datatypes;

import com.tridium.bacnet.asn.AsnInputStream;
import java.util.logging.Logger;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDouble;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BInteger;
import javax.baja.sys.BSimple;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "ASN_NULL",
      flags = 4,
      facets = {@Facet("BFacets.makeInt(0,12)")}
   ), @NiagaraProperty(
      name = "value",
      type = "BSimple",
      defaultValue = "BBacnetNull.DEFAULT"
   )})
public final class BBacnetAny extends BComponent implements BIBacnetDataType {
   public static final Property choice = newProperty(4, 0, BFacets.makeInt(0, 12));
   public static final Property value = newProperty(0, BBacnetNull.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BBacnetAny.class);
   private static final AsnInputStream asnIn = new AsnInputStream();
   private static final Logger logger = Logger.getLogger("bacnet");

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public BSimple getValue() {
      return (BSimple)this.get(value);
   }

   public void setValue(BSimple v) {
      this.set(value, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetAny() {
   }

   public BBacnetAny(BSimple s) {
      this.setAny(s);
   }

   public static BBacnetAny make(byte[] encodedValue) throws AsnException {
      BBacnetAny any = new BBacnetAny();
      synchronized (asnIn) {
         asnIn.setBuffer(encodedValue);
         any.readAsn(asnIn);
         return any;
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isMounted() && this.isRunning()) {
         if (p != choice) {
            this.getParent().asComponent().changed(this.getPropertyInParent(), cx);
         }
      }
   }

   public BSimple getAny() {
      return this.getValue();
   }

   public void setAny(BSimple v) {
      this.setAny(v, null);
   }

   public void setAny(BValue v, Context cx) {
      if (v == null) {
         v = BBacnetNull.DEFAULT;
      }

      Type t = v.getType();
      if (t == BBacnetNull.TYPE) {
         this.setInt(choice, 0, cx);
      } else if (t == BBoolean.TYPE) {
         this.setInt(choice, 1, cx);
      } else if (t == BBacnetUnsigned.TYPE) {
         this.setInt(choice, 2, cx);
      } else if (t == BInteger.TYPE) {
         this.setInt(choice, 3, cx);
      } else if (t == BFloat.TYPE) {
         this.setInt(choice, 4, cx);
      } else if (t == BDouble.TYPE) {
         this.setInt(choice, 5, cx);
      } else if (t == BBacnetOctetString.TYPE) {
         this.setInt(choice, 6, cx);
      } else if (t == BString.TYPE) {
         this.setInt(choice, 7, cx);
      } else if (t == BBacnetBitString.TYPE) {
         this.setInt(choice, 8, cx);
      } else if (t.is(BEnum.TYPE)) {
         this.setInt(choice, 9, cx);
      } else if (t == BBacnetDate.TYPE) {
         this.setInt(choice, 10, cx);
      } else if (t == BBacnetTime.TYPE) {
         this.setInt(choice, 11, cx);
      } else {
         if (t != BBacnetObjectIdentifier.TYPE) {
            throw new IllegalArgumentException("Invalid type for BBacnetAny:" + t);
         }

         this.setInt(choice, 12, cx);
      }

      this.set(value, v, cx);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeNull();
            break;
         case 1:
            out.writeBoolean((BBoolean)this.getValue());
            break;
         case 2:
            out.writeUnsigned((BBacnetUnsigned)this.getValue());
            break;
         case 3:
            out.writeSignedInteger((BInteger)this.getValue());
            break;
         case 4:
            out.writeReal((BFloat)this.getValue());
            break;
         case 5:
            out.writeDouble((BDouble)this.getValue());
            break;
         case 6:
            out.writeOctetString((BBacnetOctetString)this.getValue());
            break;
         case 7:
            out.writeCharacterString((BString)this.getValue());
            break;
         case 8:
            out.writeBitString((BBacnetBitString)this.getValue());
            break;
         case 9:
            out.writeEnumerated((BEnum)this.getValue());
            break;
         case 10:
            out.writeDate((BBacnetDate)this.getValue());
            break;
         case 11:
            out.writeTime((BBacnetTime)this.getValue());
            break;
         case 12:
            out.writeObjectIdentifier((BBacnetObjectIdentifier)this.getValue());
            break;
         default:
            throw new IllegalStateException("Invalid any type:" + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      switch (tag) {
         case 0:
            this.setAny(in.readNull(), noWrite);
            break;
         case 1:
            this.setAny(BBoolean.make(in.readBoolean()), noWrite);
            break;
         case 2:
            this.setAny(in.readUnsigned(), noWrite);
            break;
         case 3:
            this.setAny(in.readSigned(), noWrite);
            break;
         case 4:
            this.setAny(in.readFloat(), noWrite);
            break;
         case 5:
            this.setAny(BDouble.make(in.readDouble()), noWrite);
            break;
         case 6:
            this.setAny(in.readBacnetOctetString(), noWrite);
            break;
         case 7:
            this.setAny(BString.make(in.readCharacterString()), noWrite);
            break;
         case 8:
            this.setAny(in.readBitString(), noWrite);
            break;
         case 9:
            this.setAny(BDynamicEnum.make(in.readEnumerated()), noWrite);
            break;
         case 10:
            this.setAny(in.readDate(), noWrite);
            break;
         case 11:
            this.setAny(in.readTime(), noWrite);
            break;
         case 12:
            this.setAny(in.readObjectIdentifier(), noWrite);
            break;
         default:
            logger.severe("Unknown any type: " + tag);
      }
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append(tag(this.getChoice(), cx)).append(':').append(this.getAny().toString(cx));
      return sb.toString();
   }

   public String toDebugString() {
      StringBuilder sb = new StringBuilder();
      sb.append(tag(this.getChoice(), null));
      sb.append(this.getAny().toString());
      return sb.toString();
   }

   private static String tag(int choice, Context cx) {
      return choice < 0 ? "INVALID" : ASN_PRIMITIVE_TAGS[choice];
   }
}
