package com.tridium.bacnet.stack.link.ip;

import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BComponent;
import javax.baja.sys.BValue;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "validate",
   flags = 20
)
public class BBroadcastDistributionTable extends BComponent {
   public static final Action validate = newAction(20, null);
   public static final Type TYPE = Sys.loadType(BBroadcastDistributionTable.class);
   static final Context bvllContext = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:bvllContext";
      }
   };
   static final Context noValidation = new BasicContext() {
      public boolean equals(Object obj) {
         return this == obj;
      }

      public int hashCode() {
         return 1;
      }

      public String toString() {
         return "Bacnet:noValidation";
      }
   };

   public void validate() {
      this.invoke(validate, null, null);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isParentLegal(BComponent parent) {
      return parent instanceof BBacnetIpLinkLayer;
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning()) {
         if (cx == bvllContext) {
            ((BBacnetIpLinkLayer)this.getParent()).checkBDT();
         } else if (cx != noValidation) {
            this.validate();
         }
      }
   }

   public void added(Property p, Context cx) {
      super.added(p, cx);
      if (this.isRunning()) {
         if (cx == bvllContext) {
            ((BBacnetIpLinkLayer)this.getParent()).checkBDT();
         } else if (cx != noValidation) {
            this.validate();
         }
      }
   }

   public void removed(Property p, BValue v, Context cx) {
      super.removed(p, v, cx);
      if (this.isRunning()) {
         if (cx == bvllContext) {
            ((BBacnetIpLinkLayer)this.getParent()).checkBDT();
         } else if (cx != noValidation) {
            this.validate();
         }
      }
   }

   public String toString(Context cx) {
      int count = this.getSlotCount(BBdtEntry.class);
      return "BDT: " + (count == 1 ? count + " entry" : count + " entries");
   }

   public void doValidate(Context cx) {
      boolean addedOurself = ((BBacnetIpLinkLayer)this.getParent()).checkBDT();
      if (cx != bvllContext && !addedOurself) {
         ((BBacnetIpLinkLayer)this.getParent()).updateAllBDTs();
      }
   }

   public boolean updateBDT(BBdtEntry[] newTable) {
      boolean forceBDTWrite = false;
      BBdtEntry[] old = (BBdtEntry[])this.getChildren(BBdtEntry.class);
      boolean[] keep = new boolean[old.length];
      boolean[] found = new boolean[newTable.length];

      for (int i = 0; i < newTable.length; i++) {
         for (int j = 0; j < old.length; j++) {
            if (old[j].equivalent(newTable[i])) {
               keep[j] = true;
               found[i] = true;
               break;
            }
         }
      }

      for (int jx = keep.length - 1; jx >= 0; jx--) {
         if (!keep[jx]) {
            if (old[jx].getName().equals("localDevice")) {
               forceBDTWrite = true;
            } else {
               this.removeEntry(jx, bvllContext);
            }
         }
      }

      for (int i = 0; i < found.length; i++) {
         if (!found[i]) {
            this.add(null, newTable[i], bvllContext);
         }
      }

      return forceBDTWrite;
   }

   public void removeEntry(int index, Context cx) {
      BBdtEntry[] kids = (BBdtEntry[])this.getChildren(BBdtEntry.class);
      if (kids[index].getName().equals("localDevice")) {
         throw new IllegalArgumentException("Cannot remove local device from the BDT!");
      } else {
         this.remove(kids[index].getPropertyInParent(), cx);
      }
   }

   public void modifyEntry(int index, BBdtEntry newEntry, Context cx) {
      BBdtEntry[] kids = (BBdtEntry[])this.getChildren(BBdtEntry.class);
      kids[index].copyFrom(newEntry, cx);
   }
}
