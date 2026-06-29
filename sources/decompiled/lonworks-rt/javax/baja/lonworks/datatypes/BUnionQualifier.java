package javax.baja.lonworks.datatypes;

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
      name = "branch",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "branchProps",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "conditions",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
public final class BUnionQualifier extends BStruct {
   public static final Property branch = newProperty(1, "", null);
   public static final Property branchProps = newProperty(1, "", null);
   public static final Property conditions = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BUnionQualifier.class);

   public String getBranch() {
      return this.getString(branch);
   }

   public void setBranch(String v) {
      this.setString(branch, v, null);
   }

   public String getBranchProps() {
      return this.getString(branchProps);
   }

   public void setBranchProps(String v) {
      this.setString(branchProps, v, null);
   }

   public String getConditions() {
      return this.getString(conditions);
   }

   public void setConditions(String v) {
      this.setString(conditions, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public String toString(Context c) {
      return this.getConditions();
   }
}
