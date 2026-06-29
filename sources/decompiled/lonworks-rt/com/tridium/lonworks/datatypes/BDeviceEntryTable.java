package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BDeviceEntryTable extends BComponent {
   public static final Type TYPE = Sys.loadType(BDeviceEntryTable.class);

   public Type getType() {
      return TYPE;
   }

   public BDeviceEntry[] getDeviceEntries() {
      BDeviceEntry[] temp = new BDeviceEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDeviceEntry.class)) {
         BObject kid = c.get();
         temp[count++] = (BDeviceEntry)kid;
      }

      BDeviceEntry[] result = new BDeviceEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public void addEntry(BLonDevice dev) {
      BDeviceData devDat = dev.getDeviceData();
      this.addEntry(
         dev.getDisplayName(null),
         devDat.getNodeState(),
         devDat.getSubnetNodeId().getSubnetId(),
         devDat.getSubnetNodeId().getNodeId(),
         devDat.getNeuronId(),
         devDat.getProgramId(),
         devDat.getChannelId(),
         devDat.getAuthenticate(),
         devDat.getWorkingDomain()
      );
   }

   public void addEntry(
      String devName, BLonNodeState state, int subnet, int node, BNeuronId neuronId, BProgramId programId, int channelId, boolean auth, int wrkDmn
   ) {
      String entryName = devName != null && devName.length() != 0 ? devName : null;
      this.add(entryName, new BDeviceEntry(devName, state, subnet, node, neuronId, programId, channelId, auth, wrkDmn));
   }

   public void clearEntries() {
      this.removeAll(null);
   }

   public void removeEntry(BDeviceEntry e) {
      this.remove(e.getPropertyInParent(), null);
   }

   public BDeviceEntry findEntry(BNeuronId nid) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BDeviceEntry.class)) {
         BDeviceEntry e = (BDeviceEntry)c.get();
         if (e.getNeuronId().equals(nid)) {
            return e;
         }
      }

      return null;
   }
}
