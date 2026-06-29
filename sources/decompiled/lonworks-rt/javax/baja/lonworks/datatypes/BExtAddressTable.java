package javax.baja.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BExtAddressTable extends BComponent {
   public static final Type TYPE = Sys.loadType(BExtAddressTable.class);

   public Type getType() {
      return TYPE;
   }

   public BExtAddressEntry getAddressEntry(int index) {
      String name = this.propName(index);
      Property p = this.getProperty(name);
      return p == null ? BExtAddressEntry.DEFAULT : (BExtAddressEntry)this.get(p);
   }

   public boolean isEntryInUse(int index) {
      String name = this.propName(index);
      Property p = this.getProperty(name);
      return p != null;
   }

   public void setAddressEntry(int index, BIAddressEntry ie) {
      this.setAddressEntry(index, ie, null);
   }

   public void setAddressEntry(int index, BIAddressEntry ie, Context c) {
      BExtAddressEntry e = BExtAddressEntry.make(ie);
      String name = this.propName(index);
      Property p = this.getProperty(name);
      if (p == null) {
         this.add(name, e, c);
      } else {
         this.set(p, e, c);
      }
   }

   public void clearTable() {
      this.removeAll();
   }

   private String propName(int ndx) {
      return "adrEntry" + ndx;
   }
}
