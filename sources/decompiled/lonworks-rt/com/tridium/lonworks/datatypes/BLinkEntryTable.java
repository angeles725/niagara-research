package com.tridium.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BLinkEntryTable extends BComponent {
   public static final Type TYPE = Sys.loadType(BLinkEntryTable.class);

   public Type getType() {
      return TYPE;
   }

   public BLinkEntry[] getLinkEntries() {
      BLinkEntry[] temp = new BLinkEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BLinkEntry.class)) {
         BObject kid = c.get();
         temp[count++] = (BLinkEntry)kid;
      }

      BLinkEntry[] result = new BLinkEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public void addEntry(BLinkEntry entry, int ndx) {
      String name = "e" + ndx;
      this.add(name, entry);
   }
}
