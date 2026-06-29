package com.tridium.lonworks.datatypes;

import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BTagLinkEntryTable extends BComponent {
   public static final Type TYPE = Sys.loadType(BTagLinkEntryTable.class);

   public Type getType() {
      return TYPE;
   }

   public BTagLinkEntryTable() {
   }

   public BTagLinkEntryTable(BTagLinkEntry[] a) {
      this.removeAll(null);

      for (int i = 0; i < a.length; i++) {
         this.add("e" + i, a[i]);
      }
   }

   public BTagLinkEntry[] getLinkEntries() {
      BTagLinkEntry[] temp = new BTagLinkEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BTagLinkEntry.class)) {
         BObject kid = c.get();
         temp[count++] = (BTagLinkEntry)kid;
      }

      BTagLinkEntry[] result = new BTagLinkEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }
}
