package com.tridium.lonworks;

import com.tridium.lonworks.loncomm.NLonComm;
import com.tridium.lonworks.netmessages.UnprocessedNV;
import javax.baja.lonworks.BINetworkVariable;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonNetwork;
import javax.baja.lonworks.LonComm;
import javax.baja.lonworks.LonListener;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BSubnetNode;
import javax.baja.nre.util.IntHashMap;
import javax.baja.nre.util.IntHashMap.Iterator;
import javax.baja.spy.SpyWriter;
import javax.baja.sys.BComponent;

public class NvManager implements LonListener {
   private BLonNetwork lonworks;
   private LonComm lonComm;
   private IntHashMap hash = new IntHashMap(500);

   public NvManager(BLonNetwork lon) {
      this.lonworks = lon;
      this.lonComm = this.lonworks.lonComm();
      ((NLonComm)this.lonComm).registerNvListener(this);
   }

   @Override
   public void receiveLonMessage(LonMessage lm) {
      UnprocessedNV msg = (UnprocessedNV)lm;
      BSubnetNode srcAdr = (BSubnetNode)lm.getSourceAddress();
      int sel = msg.getNvSelector();
      BComponent o = (BComponent)this.hash.get(this.hashCode(sel, srcAdr));
      if (o == null) {
         if (!this.lonworks.getLocalLonDevice().receiveNvUpdate(sel, msg)) {
            this.lonworks.log().fine("Received unmapped nv update for selector=" + sel + " from " + srcAdr);
         }
      } else {
         BINetworkVariable nv = (BINetworkVariable)o;
         nv.receiveUpdate(msg.getData());
      }
   }

   private int hashCode(int selector, BSubnetNode sn) {
      return (sn.getSubnetId() << 22) + (sn.getNodeId() << 15) + selector;
   }

   public void registerSelector(int selector, BINetworkVariable nv, BLonDevice dev) {
      BComponent o = (BComponent)this.hash.put(this.hashCode(selector, dev.getSubnetNodeAddress()), nv);
      if (o != null && o != nv) {
         System.out
            .println(
               "INTERNAL ERROR: registerSelector selector="
                  + selector
                  + " yielded nv "
                  + o.getParent().getDisplayName(null)
                  + ":"
                  + o.getDisplayName(null)
                  + " instead of "
                  + ((BComponent)nv).getParent().getDisplayName(null)
                  + ":"
                  + ((BComponent)nv).getDisplayName(null)
            );
      }
   }

   public void unregisterSelector(int selector, BINetworkVariable nv, BLonDevice dev) {
      BComponent o = (BComponent)this.hash.remove(this.hashCode(selector, dev.getSubnetNodeAddress()));
      if (o == null) {
         Iterator it = this.hash.iterator();

         while (it.hasNext()) {
            if (it.next() == nv) {
               this.hash.remove(it.key());
            }
         }
      }
   }

   public void spy(SpyWriter out) throws Exception {
      out.startProps("NvManager hash table");
      Iterator it = this.hash.iterator();

      while (it.hasNext()) {
         BComponent nv = (BComponent)it.next();
         int k = it.key();
         out.prop(Integer.toString(k & 16383, 16), nv.getParent().getDisplayName(null) + ":" + nv.getDisplayName(null));
      }

      out.endProps();
   }
}
