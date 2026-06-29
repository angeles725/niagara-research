package com.tridium.bacnet.stack.transport;

import com.tridium.bacnet.stack.BacnetStackErrorCodes;
import com.tridium.bacnet.stack.BacnetStackException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import javax.baja.bacnet.datatypes.BBacnetAddress;
import javax.baja.nre.util.Array;
import javax.baja.spy.SpyWriter;

public class IdManager implements BacnetStackErrorCodes {
   public static final int MAX_INVOKE_ID = 255;
   public static final int INVALID_INVOKE_ID = -1;
   private int lastIndex = -1;
   private BBacnetAddress serverAddress;
   private BitSet ids = new BitSet(256);
   private static HashMap<Integer, Array<IdManager>> table = new HashMap<>();

   public static int getInvokeId(BBacnetAddress server) throws BacnetStackException {
      synchronized (table) {
         IdManager mgr = getIdManager(server);
         if (mgr == null) {
            mgr = new IdManager(server);
            setIdManager(server, mgr);
         }

         return mgr.nextId();
      }
   }

   public static void releaseId(BBacnetAddress server, int invokeId) {
      synchronized (table) {
         IdManager mgr = getIdManager(server);
         if (mgr != null) {
            mgr.release(invokeId);
         }
      }
   }

   public static void dump(BBacnetAddress address) {
      synchronized (table) {
         IdManager mgr = getIdManager(address);
         if (mgr != null) {
            mgr.dump();
         }
      }
   }

   public static void dumpTable() {
      synchronized (table) {
         for (Array<IdManager> networkMap : table.values()) {
            for (IdManager mgr : networkMap) {
               if (mgr != null) {
                  mgr.dump();
               }
            }
         }
      }
   }

   public static IdManager getIdManager(BBacnetAddress address) {
      if (address == null) {
         return null;
      } else if (address.equals(BBacnetAddress.DEFAULT)) {
         return null;
      } else {
         IdManager idManager = null;
         synchronized (table) {
            int networkNumber = address.getNetworkNumber();
            Array<IdManager> networkManager = table.get(networkNumber);
            byte[] addr = address.getMacAddress().getAddr();
            if (networkManager != null) {
               for (int i = 0; i < networkManager.size(); i++) {
                  IdManager idm = (IdManager)networkManager.get(i);
                  if (Arrays.equals(addr, idm.getAddress().getMacAddress().getAddr())) {
                     return idm;
                  }
               }
            }

            return idManager;
         }
      }
   }

   public static void setIdManager(BBacnetAddress address, IdManager idManager) {
      synchronized (table) {
         int networkNumber = address.getNetworkNumber();
         Array<IdManager> networkManager = table.get(networkNumber);
         if (networkManager == null) {
            networkManager = new Array(IdManager.class);
            table.put(networkNumber, networkManager);
         }

         networkManager.remove(idManager);
         networkManager.add(idManager);
      }
   }

   public static void spy(SpyWriter out) throws Exception {
      out.startProps();
      out.trTitle("IdManager", 2);
      synchronized (table) {
         out.prop("table size", table.size());

         for (Array<IdManager> networkMap : table.values()) {
            for (IdManager mgr : networkMap) {
               if (mgr != null) {
                  mgr.spyMgr(out);
               }
            }
         }
      }

      out.endProps();
   }

   public void spyMgr(SpyWriter out) throws Exception {
      out.prop("server", this.serverAddress);
      out.prop("server.hashCode", Integer.toHexString(this.serverAddress.hash()));
      StringBuilder sb = new StringBuilder();

      for (int i = 0; i <= 255; i++) {
         sb.append((char)(this.ids.get(i) ? '-' : 'X'));
         if (i % 64 == 63) {
            sb.append(' ');
         }
      }

      out.prop("avail", sb.toString());
   }

   private IdManager(BBacnetAddress serverAddress) {
      for (int i = 0; i < 256; i++) {
         this.ids.set(i);
      }

      this.serverAddress = serverAddress;
   }

   private synchronized int nextId() throws BacnetStackException {
      int index = this.lastIndex++;
      int startIndex = index;

      try {
         do {
            if (++index > 255) {
               index = 0;
            }

            if (this.ids.get(index)) {
               this.ids.clear(index);
               this.lastIndex = index;
               return index;
            }
         } while (index != startIndex);

         throw new BacnetStackException("No Invoke IDs available");
      } catch (Exception var4) {
         throw new BacnetStackException("No Invoke IDs available");
      }
   }

   private synchronized void release(int invokeId) {
      if (invokeId > 255 || invokeId < 0) {
         throw new IllegalArgumentException();
      } else if (this.ids.get(invokeId)) {
         throw new IllegalStateException();
      } else {
         this.ids.set(invokeId);
      }
   }

   public BBacnetAddress getAddress() {
      return this.serverAddress;
   }

   @Override
   public String toString() {
      return this.serverAddress.toString(null) + " lst=" + this.lastIndex;
   }

   public void dump() {
      System.out.println("Dump for " + this + " (hc:" + this.serverAddress + ")");

      for (int i = 0; i < 255; i++) {
         System.out.print((char)(this.ids.get(i) ? '-' : 'X'));
         if (i % 64 == 63) {
            System.out.println();
         }
      }

      System.out.println();
   }
}
