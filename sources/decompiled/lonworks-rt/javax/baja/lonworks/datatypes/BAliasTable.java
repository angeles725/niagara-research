package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "aliasCount",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "aliasOffset",
      type = "int",
      defaultValue = "0",
      flags = 4
   )})
public class BAliasTable extends BComponent {
   public static final Property aliasCount = newProperty(1, 0, null);
   public static final Property aliasOffset = newProperty(4, 0, null);
   public static final Type TYPE = Sys.loadType(BAliasTable.class);

   public int getAliasCount() {
      return this.getInt(aliasCount);
   }

   public void setAliasCount(int v) {
      this.setInt(aliasCount, v, null);
   }

   public int getAliasOffset() {
      return this.getInt(aliasOffset);
   }

   public void setAliasOffset(int v) {
      this.setInt(aliasOffset, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BAliasTable() {
   }

   public BAliasTable(int count) {
      this.setAliasCount(count);
      this.verifyAliasCount();
   }

   public void started() throws Exception {
      super.started();
      if (this.getAliasCount() > 0) {
         this.verifyAliasCount();
      }
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning()) {
         if (prop == aliasCount) {
            this.verifyAliasCount();
         }
      }
   }

   public void verifyAliasCount() {
      int count = this.getAliasCount();

      int i;
      for (i = 0; i < count; i++) {
         String entryName = this.propName(i);
         Property prop = this.getProperty(entryName);
         if (prop == null) {
            this.add(entryName, new BAliasConfigData(), null);
         }
      }

      Property prop;
      for (String entryName = this.propName(i++); (prop = this.getProperty(entryName)) != null; entryName = this.propName(i++)) {
         this.remove(prop);
      }
   }

   public void clearTable() {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BAliasConfigData.class)) {
         ((BAliasConfigData)c.get()).clearData();
      }
   }

   public BAliasConfigData getAliasEntry(int index) {
      return (BAliasConfigData)this.get(this.propName(index));
   }

   public void setAliasEntry(int index, BAliasConfigData ad) {
      this.set(this.propName(index), ad);
   }

   public BAliasConfigData[] getAliasArray() {
      BAliasConfigData[] a = new BAliasConfigData[this.getAliasCount()];

      for (int i = 0; i < a.length; i++) {
         a[i] = this.getAliasEntry(i);
      }

      return a;
   }

   private String propName(int ndx) {
      return "entry" + ndx;
   }
}
