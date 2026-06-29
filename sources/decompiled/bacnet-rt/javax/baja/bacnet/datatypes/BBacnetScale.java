package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
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
      facets = {@Facet("BFacets.makeInt(0, 1)")}
   ), @NiagaraProperty(
      name = "scale",
      type = "double",
      defaultValue = "1"
   )})
public final class BBacnetScale extends BStruct implements BIBacnetDataType {
   public static final Property choice = newProperty(0, 0, BFacets.makeInt(0, 1));
   public static final Property scale = newProperty(0, 1, null);
   public static final Type TYPE = Sys.loadType(BBacnetScale.class);
   public static final int FLOAT_SCALE_TAG = 0;
   public static final int INTEGER_SCALE_TAG = 1;

   public int getChoice() {
      return this.getInt(choice);
   }

   public void setChoice(int v) {
      this.setInt(choice, v, null);
   }

   public double getScale() {
      return this.getDouble(scale);
   }

   public void setScale(double v) {
      this.setDouble(scale, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetScale() {
   }

   public BBacnetScale(double v) {
      this.setChoice(0);
      this.setScale(v);
   }

   public BBacnetScale(int v) {
      this.setChoice(1);
      this.setScale((double)v);
   }

   public String toString(Context context) {
      double d = this.getScale();
      return this.getChoice() == 0 ? String.valueOf(d) : String.valueOf((int)d);
   }

   public void setScale(double v, Context cx) {
      this.setInt(choice, 0, cx);
      this.setDouble(scale, v, cx);
   }

   public void setScale(int v) {
      this.setScale(v, null);
   }

   public void setScale(int v, Context cx) {
      this.setInt(choice, 1, cx);
      this.setDouble(scale, v, cx);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      if (this.getChoice() == 0) {
         out.writeReal(0, this.getScale());
      } else {
         out.writeSignedInteger(1, (int)this.getScale());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int tag = in.peekTag();
      if (in.isValueTag(0)) {
         this.setScale((double)in.readReal(0), noWrite);
      } else {
         if (!in.isValueTag(1)) {
            throw new AsnException("Invalid tag: " + tag);
         }

         this.setScale(in.readSignedInteger(1), noWrite);
      }
   }
}
