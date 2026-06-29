package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.enums.BBacnetErrorClass;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.ErrorType;
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
      name = "errorClass",
      type = "int",
      defaultValue = "-1",
      facets = {@Facet("BFacets.makeInt(0, 65535)")}
   ), @NiagaraProperty(
      name = "errorCode",
      type = "int",
      defaultValue = "-1",
      facets = {@Facet("BFacets.makeInt(0, 65535)")}
   )})
public final class BErrorType extends BStruct implements BIBacnetDataType, ErrorType {
   public static final Property errorClass = newProperty(0, -1, BFacets.makeInt(0, 65535));
   public static final Property errorCode = newProperty(0, -1, BFacets.makeInt(0, 65535));
   public static final Type TYPE = Sys.loadType(BErrorType.class);

   @Override
   public int getErrorClass() {
      return this.getInt(errorClass);
   }

   public void setErrorClass(int v) {
      this.setInt(errorClass, v, null);
   }

   @Override
   public int getErrorCode() {
      return this.getInt(errorCode);
   }

   public void setErrorCode(int v) {
      this.setInt(errorCode, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BErrorType() {
   }

   public BErrorType(int errClass, int errCode) {
      this.setErrorClass(errClass);
      this.setErrorCode(errCode);
   }

   @Override
   public void writeEncoded(AsnOutput out) {
      this.writeAsn(out);
   }

   @Override
   public void readEncoded(AsnInput in) throws AsnException {
      this.readAsn(in);
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeEnumerated(this.getErrorClass());
      out.writeEnumerated(this.getErrorCode());
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      int errorClass = in.readEnumerated();
      int errorCode = in.readEnumerated();
      this.setInt(BErrorType.errorClass, errorClass, noWrite);
      this.setInt(BErrorType.errorCode, errorCode, noWrite);
   }

   public boolean isDefault() {
      return this.getErrorClass() < 0 && this.getErrorCode() < 0;
   }

   public void setToDefault(Context cx) {
      this.setInt(errorClass, -1, cx);
      this.setInt(errorCode, -1, cx);
   }

   public String toString(Context cx) {
      if (this.getErrorClass() == -1 && this.getErrorCode() == -1) {
         return "";
      } else {
         try {
            return "Error:class=" + BBacnetErrorClass.tag(this.getErrorClass()) + " code=" + BBacnetErrorCode.tag(this.getErrorCode());
         } catch (Exception var3) {
            return "Invalid Error " + this.getErrorClass() + ":" + this.getErrorCode();
         }
      }
   }
}
