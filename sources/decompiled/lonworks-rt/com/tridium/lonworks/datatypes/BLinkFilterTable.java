package com.tridium.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperty(
   name = "maxIndex",
   type = "int",
   defaultValue = "0"
)
public class BLinkFilterTable extends BComponent {
   public static final Property maxIndex = newProperty(0, 0, null);
   public static final Type TYPE = Sys.loadType(BLinkFilterTable.class);

   public int getMaxIndex() {
      return this.getInt(maxIndex);
   }

   public void setMaxIndex(int v) {
      this.setInt(maxIndex, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLinkFilterEntry[] getLinkFilterEntries() {
      BLinkFilterEntry[] temp = new BLinkFilterEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BLinkFilterEntry.class)) {
         BObject kid = c.get();
         temp[count++] = (BLinkFilterEntry)kid;
      }

      BLinkFilterEntry[] result = new BLinkFilterEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public synchronized void addEntry(BLinkFilterEntry entry) {
      this.setMaxIndex(this.getMaxIndex() + 1);
      String name = "e" + this.getMaxIndex();
      this.add(name, entry);
   }

   public synchronized void clearEntries() {
      this.removeAll(null);
      this.setMaxIndex(0);
   }
}
