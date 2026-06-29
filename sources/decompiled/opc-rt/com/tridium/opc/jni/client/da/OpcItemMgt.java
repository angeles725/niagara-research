package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;
import java.util.Vector;

public class OpcItemMgt extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a54-011e-11d0-9675-0020afd8adb3}", OpcItemMgt.class);

   public OpcItemMgt.ItemResult[] addItems(String[] itemIds, int[] clientHandles, boolean[] active, int[] dataTypes) throws ArrayIndexOutOfBoundsException, OpcException {
      int len = itemIds.length;
      if (clientHandles.length == len && active.length == len && dataTypes.length == len) {
         OpcItemMgt.ItemResults results = new OpcItemMgt.ItemResults();
         this.addItems(this.getPeer(), itemIds, clientHandles, active, dataTypes, results);
         return results != null ? results.toArray() : null;
      } else {
         throw new ArrayIndexOutOfBoundsException();
      }
   }

   public int[] removeItems(int[] serverHandles) {
      return this.removeItems(this.getPeer(), serverHandles);
   }

   public int[] setActiveState(int[] serverHandles, boolean active) {
      return this.setActiveState(this.getPeer(), serverHandles, active);
   }

   public synchronized OpcItemMgt.ItemResult[] validateItems(String[] itemIds, int[] clientHandles, boolean[] active, int[] dataTypes) throws ArrayIndexOutOfBoundsException, OpcException {
      int len = itemIds.length;
      if (clientHandles.length == len && active.length == len && dataTypes.length == len) {
         OpcItemMgt.ItemResults results = new OpcItemMgt.ItemResults();
         this.validateItems(this.getPeer(), itemIds, clientHandles, active, dataTypes, results);
         return results != null ? results.toArray() : null;
      } else {
         throw new ArrayIndexOutOfBoundsException();
      }
   }

   private native void addItems(long var1, String[] var3, int[] var4, boolean[] var5, int[] var6, OpcItemMgt.ItemResults var7);

   private native int[] removeItems(long var1, int[] var3);

   private native int[] setActiveState(long var1, int[] var3, boolean var4);

   private native void validateItems(long var1, String[] var3, int[] var4, boolean[] var5, int[] var6, OpcItemMgt.ItemResults var7);

   public class ItemResult {
      public int access;
      public int dataType;
      public int hresult;
      public String itemId;
      public int serverHandle;
      public int actualdataType;
   }

   private class ItemResults {
      Vector<OpcItemMgt.ItemResult> results = new Vector<>();

      private ItemResults() {
      }

      public void addResult(String itemId, int serverHandle, int dataType, int accessRights, int hresult) {
         OpcItemMgt.ItemResult r = OpcItemMgt.this.new ItemResult();
         r.access = accessRights;
         r.dataType = dataType;
         r.hresult = hresult;
         r.itemId = itemId;
         r.serverHandle = serverHandle;
         this.results.addElement(r);
      }

      public OpcItemMgt.ItemResult firstResult() {
         return this.results.elementAt(0);
      }

      public OpcItemMgt.ItemResult[] toArray() {
         OpcItemMgt.ItemResult[] ret = new OpcItemMgt.ItemResult[this.results.size()];
         this.results.copyInto(ret);
         return ret;
      }
   }
}
