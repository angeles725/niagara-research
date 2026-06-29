package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.util.LonStringUtil;
import java.util.ArrayList;
import javax.baja.lonworks.londata.BLonDataUnion;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "conditionProp",
   type = "String",
   defaultValue = "",
   flags = 1
)
public class BUnionQualifiers extends BComponent {
   public static final Property conditionProp = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BUnionQualifiers.class);
   BUnionQualifier[] uqa = null;
   Property[][] active = (Property[][])null;
   Object sync = new Object();

   public String getConditionProp() {
      return this.getString(conditionProp);
   }

   public void setConditionProp(String v) {
      this.setString(conditionProp, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public Property[] getActiveProperties(BObject cond) {
      this.initActiveHash();

      for (int i = 0; i < this.uqa.length; i++) {
         if (this.isActive(this.uqa[i], cond)) {
            return this.active[i];
         }
      }

      return this.getParent().getPropertiesArray();
   }

   public boolean isSameBranch(BObject cond1, BObject cond2) {
      for (int i = 0; i < this.uqa.length; i++) {
         boolean actv1 = this.isActive(this.uqa[i], cond1);
         boolean actv2 = this.isActive(this.uqa[i], cond2);
         if (actv1 != actv2) {
            return false;
         }

         if (actv1 && actv2) {
            return true;
         }
      }

      return false;
   }

   private boolean isActive(BUnionQualifier uq, BObject cond) {
      BLonPrimitive lp = (BLonPrimitive)cond;
      int cval = (int)lp.getDataAsDouble();
      String valS = uq.getConditions();
      valS = valS.substring(valS.indexOf("=") + 1);
      int[] valA = LonStringUtil.getIntArray(valS);

      for (int i = 0; i < valA.length; i++) {
         if (valA[i] == cval) {
            return true;
         }
      }

      return false;
   }

   private void initActiveHash() {
      synchronized (this.sync) {
         if (this.active == null) {
            BLonDataUnion ld = (BLonDataUnion)this.getParent();
            this.uqa = (BUnionQualifier[])this.getChildren(BUnionQualifier.class);
            ArrayList<Array<Property>> aSet = new ArrayList<>(this.uqa.length);

            for (int i = 0; i < this.uqa.length; i++) {
               aSet.add(i, new Array(ld.getPropertiesArray()));
            }

            for (int i = 0; i < this.uqa.length; i++) {
               BUnionQualifier uq = this.uqa[i];
               String[] props = LonStringUtil.getStringArray(uq.getBranchProps());

               for (int n = 0; n < props.length; n++) {
                  Property p = ld.getProperty(props[n]);

                  for (int j = 0; j < this.uqa.length; j++) {
                     if (j != i) {
                        aSet.get(j).remove(p);
                     }
                  }
               }
            }

            this.active = new Property[this.uqa.length][];

            for (int i = 0; i < this.uqa.length; i++) {
               this.active[i] = (Property[])aSet.get(i).trim();
            }
         }
      }
   }
}
