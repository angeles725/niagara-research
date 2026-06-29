package com.tridium.bacnet.stack.link.ip;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BComponent;
import javax.baja.sys.BRelTime;
import javax.baja.sys.Clock;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraAction(
   name = "removeEntry",
   parameterType = "BFdtEntry",
   defaultValue = "new BFdtEntry()",
   flags = 4
)
public class BForeignDeviceTable extends BComponent {
   public static final Action removeEntry = newAction(4, new BFdtEntry(), null);
   public static final Type TYPE = Sys.loadType(BForeignDeviceTable.class);
   private HashMap<String, Ticket> tickets = new HashMap<>();
   private static final Logger logger = Logger.getLogger("bacnet.link.ip");

   public void removeEntry(BFdtEntry parameter) {
      this.invoke(removeEntry, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   void addEntry(byte[] bIPAddr, int timeToLive) {
      SlotCursor<Property> c = this.getProperties();
      String bacnetIpAddress = BBacnetAddress.bytesToString(2, bIPAddr);
      int timeRemaining = timeToLive + 30;
      if (timeRemaining > 65535) {
         timeRemaining = 65535;
      }

      while (c.next(BFdtEntry.class)) {
         BFdtEntry e = (BFdtEntry)c.get();
         if (e.getBacnetIPAddress().equals(bacnetIpAddress)) {
            Ticket t = this.tickets.get(bacnetIpAddress);
            if (t != null) {
               t.cancel();
            }

            e.setTimeToLive(timeToLive);
            BRelTime regLife = BRelTime.make(timeRemaining * 1000L);
            e.setPurgeTime(BAbsTime.make().add(regLife));
            this.tickets.put(bacnetIpAddress, Clock.schedule(this, regLife, removeEntry, e));
            return;
         }
      }

      BRelTime regLife = BRelTime.make(timeRemaining * 1000L);
      BFdtEntry newEntry = new BFdtEntry(bacnetIpAddress, timeToLive, BAbsTime.make().add(regLife));
      this.add(SlotPath.escape("FD" + bacnetIpAddress), newEntry, 2);
      this.tickets.put(bacnetIpAddress, Clock.schedule(this, regLife, removeEntry, newEntry));
   }

   boolean deleteEntry(byte[] bIPAddr) {
      return this.deleteEntry(BBacnetAddress.bytesToString(2, bIPAddr));
   }

   boolean deleteEntry(String bIPAddr) {
      if (logger.isLoggable(Level.FINE)) {
         logger.fine("Deleting Foreign Device Entry at " + bIPAddr);
      }

      String fdName = SlotPath.escape("FD" + bIPAddr);
      if (this.get(fdName) == null) {
         return false;
      } else {
         try {
            this.remove(fdName);
            Ticket t = this.tickets.get(bIPAddr);
            if (t != null) {
               t.cancel();
            }

            this.tickets.remove(bIPAddr);
            return true;
         } catch (Exception var4) {
            return false;
         }
      }
   }

   public void doRemoveEntry(BFdtEntry entry) {
      if (entry.getTimeToLive() >= 0) {
         this.deleteEntry(entry.getBacnetIPAddress());
      }
   }

   public void deleteEntry(int index) {
      BFdtEntry[] kids = (BFdtEntry[])this.getChildren(BFdtEntry.class);
      if (kids != null && kids.length > index) {
         this.remove(kids[index]);
      }
   }

   public void modifyEntry(int index, BFdtEntry newEntry) {
      BFdtEntry[] kids = (BFdtEntry[])this.getChildren(BFdtEntry.class);
      if (kids != null && kids.length > index) {
         kids[index].copyFrom(newEntry);
      }
   }
}
