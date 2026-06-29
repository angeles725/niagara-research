package com.tridium.lonworks.datatypes;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.enums.BLonRouterMode;
import com.tridium.lonworks.enums.BLonRouterType;
import javax.baja.lonworks.datatypes.BDeviceData;
import javax.baja.lonworks.datatypes.BNeuronId;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public class BRouterEntryTable extends BComponent {
   public static final Type TYPE = Sys.loadType(BRouterEntryTable.class);

   public Type getType() {
      return TYPE;
   }

   public BRouterEntry[] getRouterEntries() {
      BRouterEntry[] temp = new BRouterEntry[this.getSlotCount()];
      int count = 0;
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BRouterEntry.class)) {
         BObject kid = c.get();
         temp[count++] = (BRouterEntry)kid;
      }

      BRouterEntry[] result = new BRouterEntry[count];
      System.arraycopy(temp, 0, result, 0, count);
      return result;
   }

   public void addEntry(BLonRouter rtr) {
      BDeviceData ddn = rtr.getNearDeviceData();
      BDeviceData ddf = rtr.getFarDeviceData();
      this.addEntry(
         rtr.getDisplayName(null),
         rtr.getRouterType(),
         rtr.getRouterMode(),
         ddn.getNodeState(),
         ddn.getChannelId(),
         ddn.getSubnetNodeId(),
         ddf.getChannelId(),
         ddf.getSubnetNodeId(),
         ddn.getNeuronId(),
         ddf.getNeuronId()
      );
   }

   public void addEntry(
      String rtrName,
      BLonRouterType type,
      BLonRouterMode mode,
      BLonNodeState state,
      int nearChannel,
      BSubnetNode nearAddress,
      int farChannel,
      BSubnetNode farAddress,
      BNeuronId neuronId,
      BNeuronId farNeuronId
   ) {
      String entryName = rtrName != null && rtrName.length() != 0 ? rtrName : null;
      this.add(entryName, new BRouterEntry(rtrName, type, mode, state, nearChannel, nearAddress, farChannel, farAddress, neuronId, farNeuronId));
   }

   public void clearEntries() {
      this.removeAll(null);
   }

   public void removeEntry(BRouterEntry e) {
      this.remove(e.getPropertyInParent(), null);
   }

   public BRouterEntry findEntry(BNeuronId nid) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BRouterEntry.class)) {
         BRouterEntry e = (BRouterEntry)c.get();
         if (e.getNeuronId().equals(nid)) {
            return e;
         }
      }

      return null;
   }
}
