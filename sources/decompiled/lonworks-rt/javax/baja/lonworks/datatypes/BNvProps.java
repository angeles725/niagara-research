package javax.baja.lonworks.datatypes;

import javax.baja.lonworks.enums.BLonSnvtType;
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
      name = "objectIndex",
      type = "int",
      defaultValue = "-1",
      flags = 1
   ), @NiagaraProperty(
      name = "memberIndex",
      type = "int",
      defaultValue = "-1",
      flags = 1
   ), @NiagaraProperty(
      name = "pollEnable",
      type = "boolean",
      defaultValue = "true"
   ), @NiagaraProperty(
      name = "polled",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "authConf",
      type = "boolean",
      defaultValue = "true",
      flags = 1
   ), @NiagaraProperty(
      name = "serviceConf",
      type = "boolean",
      defaultValue = "true",
      flags = 1
   ), @NiagaraProperty(
      name = "priorityConf",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "modifyOffline",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "sync",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "changeableType",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "boundToLocal",
      type = "boolean",
      defaultValue = "false",
      flags = 65
   )})
public class BNvProps extends BStruct {
   public static final Property nvIndex = newProperty(1, 0, null);
   public static final Property snvtType = newProperty(1, 0, null);
   public static final Property objectIndex = newProperty(1, -1, null);
   public static final Property memberIndex = newProperty(1, -1, null);
   public static final Property pollEnable = newProperty(0, true, null);
   public static final Property polled = newProperty(1, false, null);
   public static final Property authConf = newProperty(1, true, null);
   public static final Property serviceConf = newProperty(1, true, null);
   public static final Property priorityConf = newProperty(1, false, null);
   public static final Property modifyOffline = newProperty(5, false, null);
   public static final Property sync = newProperty(1, false, null);
   public static final Property changeableType = newProperty(1, false, null);
   public static final Property boundToLocal = newProperty(65, false, null);
   public static final Type TYPE = Sys.loadType(BNvProps.class);

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

   public int getObjectIndex() {
      return this.getInt(objectIndex);
   }

   public void setObjectIndex(int v) {
      this.setInt(objectIndex, v, null);
   }

   public int getMemberIndex() {
      return this.getInt(memberIndex);
   }

   public void setMemberIndex(int v) {
      this.setInt(memberIndex, v, null);
   }

   public boolean getPollEnable() {
      return this.getBoolean(pollEnable);
   }

   public void setPollEnable(boolean v) {
      this.setBoolean(pollEnable, v, null);
   }

   public boolean getPolled() {
      return this.getBoolean(polled);
   }

   public void setPolled(boolean v) {
      this.setBoolean(polled, v, null);
   }

   public boolean getAuthConf() {
      return this.getBoolean(authConf);
   }

   public void setAuthConf(boolean v) {
      this.setBoolean(authConf, v, null);
   }

   public boolean getServiceConf() {
      return this.getBoolean(serviceConf);
   }

   public void setServiceConf(boolean v) {
      this.setBoolean(serviceConf, v, null);
   }

   public boolean getPriorityConf() {
      return this.getBoolean(priorityConf);
   }

   public void setPriorityConf(boolean v) {
      this.setBoolean(priorityConf, v, null);
   }

   public boolean getModifyOffline() {
      return this.getBoolean(modifyOffline);
   }

   public void setModifyOffline(boolean v) {
      this.setBoolean(modifyOffline, v, null);
   }

   public boolean getSync() {
      return this.getBoolean(sync);
   }

   public void setSync(boolean v) {
      this.setBoolean(sync, v, null);
   }

   public boolean getChangeableType() {
      return this.getBoolean(changeableType);
   }

   public void setChangeableType(boolean v) {
      this.setBoolean(changeableType, v, null);
   }

   public boolean getBoundToLocal() {
      return this.getBoolean(boundToLocal);
   }

   public void setBoundToLocal(boolean v) {
      this.setBoolean(boundToLocal, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void setUnbound() {
      this.setBoundToLocal(false);
   }

   public String toString(Context c) {
      StringBuilder sb = new StringBuilder();
      sb.append("nv:").append(this.getNvIndex());
      if (this.getSnvtType() > 0) {
         sb.append(",snvt:").append(this.getSnvtType());
      }

      if (this.getObjectIndex() != -1) {
         sb.append(",obj:").append(this.getObjectIndex());
         sb.append(".").append(this.getMemberIndex());
      }

      if (this.getPollEnable()) {
         sb.append(",pollEnable");
      }

      if (this.getPolled()) {
         sb.append(",polled");
      }

      boolean dot = false;
      sb.append(",conf:");
      if (this.getAuthConf()) {
         sb.append("auth");
         dot = true;
      }

      if (this.getServiceConf()) {
         if (dot) {
            sb.append(".");
         }

         sb.append("srv");
         dot = true;
      }

      if (this.getPriorityConf()) {
         if (dot) {
            sb.append(".");
         }

         sb.append("pri");
      }

      if (this.getModifyOffline()) {
         sb.append(",modOffline");
      }

      if (this.getSync()) {
         sb.append(",sync");
      }

      if (this.getChangeableType()) {
         sb.append(",chngTyp");
      }

      return sb.toString();
   }

   public BLonSnvtType getSnvtTypeEnum() {
      int typ = this.getSnvtType();
      return typ > 0 && typ < 214 ? BLonSnvtType.make(typ) : BLonSnvtType.SnvtXxx;
   }
}
