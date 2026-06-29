package javax.baja.lonworks.londata;

import java.io.IOException;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BEnum;
import javax.baja.sys.BSimple;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public abstract class BLonPrimitive extends BSimple {
   public static final Type TYPE = Sys.loadType(BLonPrimitive.class);

   public Type getType() {
      return TYPE;
   }

   public abstract void toOutputStream(LonOutputStream var1, BLonElementQualifiers var2);

   public abstract BLonPrimitive fromInputStream(LonInputStream var1, BLonElementQualifiers var2);

   public double getDataAsDouble() {
      throw new BajaRuntimeException("Conversion to float not supported.");
   }

   public boolean getDataAsBoolean() {
      throw new BajaRuntimeException("Conversion to boolean not supported.");
   }

   public String getDataAsString() {
      throw new BajaRuntimeException("Conversion to string not supported.");
   }

   public BEnum getDataAsEnum(BEnum en) {
      Thread.dumpStack();
      throw new BajaRuntimeException("Conversion to enum not supported.");
   }

   @Deprecated
   public BLonPrimitive makeFromDouble(double v) {
      return this.makeFromDouble(v, null);
   }

   public BLonPrimitive makeFromDouble(double val, BLonElementQualifiers e) {
      throw new BajaRuntimeException("Conversion from float not supported.");
   }

   public BLonPrimitive makeFromBoolean(boolean v) {
      throw new BajaRuntimeException("Conversion from boolean not supported.");
   }

   public BLonPrimitive makeFromString(String v) {
      throw new BajaRuntimeException("Conversion from string not supported.");
   }

   public BLonPrimitive makeFromEnum(BEnum v) {
      Thread.dumpStack();
      throw new BajaRuntimeException("Conversion to enum not supported.");
   }

   public boolean isNumeric() {
      return false;
   }

   public String toDebugString() {
      return this.getType().getDisplayName(null) + " value=" + this.toString(null);
   }

   protected String encodeClass(BSimple s) {
      return s.getType().getTypeSpec().toString();
   }

   protected BSimple decodeClass(String cl) throws IOException {
      try {
         String module = cl.substring(0, cl.indexOf(58));
         String classname = cl.substring(cl.indexOf(58) + 1);
         return (BSimple)Sys.loadModule(module).getType(classname).getInstance();
      } catch (Throwable var4) {
         throw new IOException("Unable to getInstance for " + cl + "\n" + var4.toString());
      }
   }
}
