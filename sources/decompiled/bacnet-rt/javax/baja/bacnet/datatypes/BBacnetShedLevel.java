package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,2)")}
   ), @NiagaraProperty(
      name = "percent",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   ), @NiagaraProperty(
      name = "level",
      type = "BBacnetUnsigned",
      defaultValue = "BBacnetUnsigned.DEFAULT"
   ), @NiagaraProperty(
      name = "amount",
      type = "float",
      defaultValue = "0.0f"
   )})
public final class BBacnetShedLevel extends BStruct implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 0, BFacets.makeInt(0, 2));
   public static final Property percent = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Property level = newProperty(0, BBacnetUnsigned.DEFAULT, null);
   public static final Property amount = newProperty(0, 0.0F, null);
   public static final Type TYPE = Sys.loadType(BBacnetShedLevel.class);
   private static final int PERCENT_TAG = 0;
   private static final int LEVEL_TAG = 1;
   private static final int AMOUNT_TAG = 2;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public BBacnetUnsigned getPercent() {
      return (BBacnetUnsigned)this.get(percent);
   }

   public void setPercent(BBacnetUnsigned v) {
      this.set(percent, v, null);
   }

   public BBacnetUnsigned getLevel() {
      return (BBacnetUnsigned)this.get(level);
   }

   public void setLevel(BBacnetUnsigned v) {
      this.set(level, v, null);
   }

   public float getAmount() {
      return this.getFloat(amount);
   }

   public void setAmount(float v) {
      this.setFloat(amount, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeUnsigned(0, this.getPercent());
            break;
         case 1:
            out.writeUnsigned(1, this.getLevel());
            break;
         case 2:
            out.writeReal(2, this.getAmount());
            break;
         default:
            throw new IllegalStateException("Invalid BACnetShedLevel type: " + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (in.isValueTag(0)) {
         this.set(percent, in.readUnsigned(0), noWrite);
      } else if (in.isValueTag(1)) {
         this.set(level, in.readUnsigned(1), noWrite);
      } else {
         if (!in.isValueTag(2)) {
            throw new AsnException("Invalid tag: " + tag);
         }

         this.setFloat(amount, in.readReal(2), noWrite);
      }

      this.setInt(choice, tag, noWrite);
   }

   public String toString(Context context) {
      switch (this.getChoice()) {
         case 0:
            return "BACnetShedLevel:percent = " + this.getPercent().toString(context);
         case 1:
            return "BACnetShedLevel:level = " + this.getLevel().toString(context);
         case 2:
            return "BACnetShedLevel:amount = " + BFloat.toString(this.getAmount(), context);
         default:
            return "BACnetShedLevel:unknown";
      }
   }
}
