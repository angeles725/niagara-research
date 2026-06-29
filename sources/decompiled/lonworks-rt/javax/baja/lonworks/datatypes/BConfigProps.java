package javax.baja.lonworks.datatypes;

import javax.baja.lonworks.enums.BLonConfigScope;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "configIndex",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "offset",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "length",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "mfgDefined",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "modifyFlag",
      type = "BModifyFlags",
      defaultValue = "BModifyFlags.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "scope",
      type = "BLonConfigScope",
      defaultValue = "BLonConfigScope.node",
      flags = 1
   ), @NiagaraProperty(
      name = "select",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
public class BConfigProps extends BStruct {
   public static final Property configIndex = newProperty(1, 0, null);
   public static final Property offset = newProperty(1, 0, null);
   public static final Property length = newProperty(1, 0, null);
   public static final Property mfgDefined = newProperty(1, false, null);
   public static final Property modifyFlag = newProperty(1, BModifyFlags.DEFAULT, null);
   public static final Property scope = newProperty(1, BLonConfigScope.node, null);
   public static final Property select = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BConfigProps.class);

   public int getConfigIndex() {
      return this.getInt(configIndex);
   }

   public void setConfigIndex(int v) {
      this.setInt(configIndex, v, null);
   }

   public int getOffset() {
      return this.getInt(offset);
   }

   public void setOffset(int v) {
      this.setInt(offset, v, null);
   }

   public int getLength() {
      return this.getInt(length);
   }

   public void setLength(int v) {
      this.setInt(length, v, null);
   }

   public boolean getMfgDefined() {
      return this.getBoolean(mfgDefined);
   }

   public void setMfgDefined(boolean v) {
      this.setBoolean(mfgDefined, v, null);
   }

   public BModifyFlags getModifyFlag() {
      return (BModifyFlags)this.get(modifyFlag);
   }

   public void setModifyFlag(BModifyFlags v) {
      this.set(modifyFlag, v, null);
   }

   public BLonConfigScope getScope() {
      return (BLonConfigScope)this.get(scope);
   }

   public void setScope(BLonConfigScope v) {
      this.set(scope, v, null);
   }

   public String getSelect() {
      return this.getString(select);
   }

   public void setSelect(String v) {
      this.setString(select, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context c) {
      StringBuilder sb = new StringBuilder();
      sb.append(!this.getMfgDefined() ? "scpt:" : "ucpt:");
      sb.append(this.getConfigIndex());
      sb.append(",").append(this.getScope());
      if (this.getScope() != BLonConfigScope.node) {
         sb.append(".").append(this.getSelect());
      }

      sb.append(",").append(this.getModifyFlag());
      return sb.toString();
   }
}
