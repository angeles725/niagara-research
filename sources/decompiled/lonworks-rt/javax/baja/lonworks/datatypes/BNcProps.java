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
      name = "nvIndex",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "snvtType",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "configIndex",
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
public class BNcProps extends BStruct {
   public static final Property nvIndex = newProperty(1, 0, null);
   public static final Property snvtType = newProperty(1, 0, null);
   public static final Property configIndex = newProperty(1, 0, null);
   public static final Property mfgDefined = newProperty(1, false, null);
   public static final Property modifyFlag = newProperty(1, BModifyFlags.DEFAULT, null);
   public static final Property scope = newProperty(1, BLonConfigScope.node, null);
   public static final Property select = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BNcProps.class);

   public int getNvIndex() {
      return this.getInt(nvIndex);
   }

   public void setNvIndex(int v) {
      this.setInt(nvIndex, v, null);
   }

   public int getSnvtType() {
      return this.getInt(snvtType);
   }

   public void setSnvtType(int v) {
      this.setInt(snvtType, v, null);
   }

   public int getConfigIndex() {
      return this.getInt(configIndex);
   }

   public void setConfigIndex(int v) {
      this.setInt(configIndex, v, null);
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

   public void setUnbound() {
   }

   public String toString(Context c) {
      StringBuilder sb = new StringBuilder();
      sb.append("nv:").append(this.getNvIndex());
      if (this.getSnvtType() > 0) {
         sb.append(",snvt:").append(this.getSnvtType());
      }

      if (this.getConfigIndex() != 0) {
         sb.append(",cfgNdx:").append(this.getConfigIndex());
      }

      if (this.getMfgDefined()) {
         sb.append(",mfgDefn");
      }

      sb.append(",mod:").append(this.getModifyFlag());
      sb.append(",scope:").append(this.getScope());
      if (this.getScope() != BLonConfigScope.node) {
         sb.append(".").append(this.getSelect());
      }

      return sb.toString();
   }
}
