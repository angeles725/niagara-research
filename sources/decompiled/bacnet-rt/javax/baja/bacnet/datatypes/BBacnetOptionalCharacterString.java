package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "choice",
      type = "int",
      defaultValue = "0",
      facets = {@Facet("BFacets.makeInt(0,1)")}
   ), @NiagaraProperty(
      name = "characterString",
      type = "String",
      defaultValue = ""
   )})
public final class BBacnetOptionalCharacterString extends BStruct implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 0, BFacets.makeInt(0, 1));
   public static final Property characterString = newProperty(0, "", null);
   public static final Type TYPE = Sys.loadType(BBacnetOptionalCharacterString.class);
   public static final int NULL_TAG = 0;
   public static final int CHARACTER_STRING_TAG = 1;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public String getCharacterString() {
      return this.getString(characterString);
   }

   public void setCharacterString(String v) {
      this.setString(characterString, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetOptionalCharacterString() {
      this.setChoice(0);
      this.setCharacterString("");
   }

   public BBacnetOptionalCharacterString(String string) {
      if (string == null) {
         this.setChoice(0);
         this.setCharacterString("");
      } else {
         this.setChoice(1);
         this.setCharacterString(string);
      }
   }

   public boolean isNull() {
      return this.getChoice() == 0;
   }

   public boolean isCharacterString() {
      return this.getChoice() == 1;
   }

   public BValue getCharacterStringValue() {
      return this.getChoice() == 0 ? null : BString.make(this.getCharacterString());
   }

   @Override
   public void writeAsn(AsnOutput out) {
      switch (this.getChoice()) {
         case 0:
            out.writeNull();
            break;
         case 1:
            out.writeCharacterString(this.getCharacterString());
            break;
         default:
            throw new IllegalStateException("Invalid recipient type:" + this.getChoice());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      switch (tag) {
         case 0:
            in.readNull();
            this.setChoice(0);
            this.setCharacterString("");
            break;
         case 7:
            String value = in.readCharacterString();
            this.setChoice(1);
            this.setCharacterString(value);
            break;
         default:
            throw new AsnException("Invalid tag: " + tag);
      }
   }

   public String toString(Context context) {
      return this.getCharacterString();
   }
}
