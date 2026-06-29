package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcEnv;
import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;
import java.util.Vector;
import javax.baja.log.Log;

public class OpcBrowseServerAddressSpace extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39c13a4f-011e-11d0-9675-0020afd8adb3}", OpcBrowseServerAddressSpace.class);
   Log opcLog = Log.getLog("OpcDaLog");

   public long queryOrganization() {
      return this.queryOrganization(this.getPeer());
   }

   public synchronized OpcBrowseServerAddressSpace.Result[] browseAllItems(OpcItemMgt mgt) {
      OpcBrowseServerAddressSpace.Result[] items = this.getItems("", true);
      int len = items.length;
      String[] ids = new String[len];
      int[] handles = new int[len];
      boolean[] active = new boolean[len];
      int[] dataTypes = new int[len];

      for (int i = 0; i < len; i++) {
         ids[i] = items[i].id;
         handles[i] = i;
         active[i] = false;
         dataTypes[i] = 0;
      }

      if (len > 0) {
         OpcItemMgt.ItemResult[] res = mgt.validateItems(ids, handles, active, dataTypes);
         if (res != null) {
            for (int i = 0; i < len; i++) {
               items[i].dataType = res[i].dataType;
               items[i].isReadable = OpcEnv.isReadable(res[i].access);
               items[i].isWritable = OpcEnv.isWritable(res[i].access);
            }
         }
      }

      return items;
   }

   public OpcBrowseServerAddressSpace.Result[] browse(String branch, boolean flat, OpcItemMgt mgt) {
      OpcBrowseServerAddressSpace.Result[] groups = this.getGroups(branch);
      OpcBrowseServerAddressSpace.Result[] items = this.getItems(branch, false);
      int len = items.length;
      String[] ids = new String[len];
      int[] handles = new int[len];
      boolean[] active = new boolean[len];
      int[] dataTypes = new int[len];

      for (int i = 0; i < len; i++) {
         ids[i] = items[i].id;
         handles[i] = i;
         active[i] = false;
         dataTypes[i] = 0;
      }

      if (len > 0) {
         OpcItemMgt.ItemResult[] res = mgt.validateItems(ids, handles, active, dataTypes);
         if (res != null) {
            for (int i = 0; i < len; i++) {
               items[i].dataType = res[i].dataType;
               items[i].isReadable = OpcEnv.isReadable(res[i].access);
               items[i].isWritable = OpcEnv.isWritable(res[i].access);
            }
         }
      }

      len += groups.length;
      OpcBrowseServerAddressSpace.Result[] ret = new OpcBrowseServerAddressSpace.Result[len];
      System.arraycopy(groups, 0, ret, 0, groups.length);
      System.arraycopy(items, 0, ret, groups.length, items.length);
      return ret;
   }

   public OpcBrowseServerAddressSpace.Result[] browse(String[] branches, OpcItemMgt mgt) {
      OpcBrowseServerAddressSpace.Result[] groups = this.getGroups(branches);
      OpcBrowseServerAddressSpace.Result[] items = this.getItems(branches);
      int len = items.length;
      String[] ids = new String[len];
      int[] handles = new int[len];
      boolean[] active = new boolean[len];
      int[] dataTypes = new int[len];

      for (int i = 0; i < len; i++) {
         ids[i] = items[i].id;
         handles[i] = i;
         active[i] = false;
         dataTypes[i] = 0;
      }

      if (len > 0) {
         OpcItemMgt.ItemResult[] res = mgt.validateItems(ids, handles, active, dataTypes);
         if (res != null) {
            for (int i = 0; i < len; i++) {
               items[i].dataType = res[i].dataType;
               items[i].isReadable = OpcEnv.isReadable(res[i].access);
               items[i].isWritable = OpcEnv.isWritable(res[i].access);
            }
         }
      }

      len += groups.length;
      OpcBrowseServerAddressSpace.Result[] ret = new OpcBrowseServerAddressSpace.Result[len];
      System.arraycopy(groups, 0, ret, 0, groups.length);
      System.arraycopy(items, 0, ret, groups.length, items.length);
      return ret;
   }

   public synchronized OpcBrowseServerAddressSpace.Result[] getGroups(String branch) {
      if (branch == null) {
         branch = "";
      }

      OpcBrowseServerAddressSpace.Items ret = new OpcBrowseServerAddressSpace.Items(true);
      this.getGroups(this.getPeer(), branch, ret);
      return (OpcBrowseServerAddressSpace.Result[])ret.toArray();
   }

   public synchronized OpcBrowseServerAddressSpace.Result[] getGroups(String[] path) {
      this.goRoot(this.getPeer());
      if (path != null) {
         for (int i = 0; i < path.length; i++) {
            this.goDown(this.getPeer(), path[i]);
         }
      }

      OpcBrowseServerAddressSpace.Items ret = new OpcBrowseServerAddressSpace.Items(true);
      this.listBranches(this.getPeer(), ret);
      return (OpcBrowseServerAddressSpace.Result[])ret.toArray();
   }

   public synchronized OpcBrowseServerAddressSpace.Result[] getItems(String branch, boolean flat) {
      if (branch == null) {
         branch = "";
      }

      OpcBrowseServerAddressSpace.Items ret = new OpcBrowseServerAddressSpace.Items(false);
      this.getItems(this.getPeer(), branch, flat, ret);
      return (OpcBrowseServerAddressSpace.Result[])ret.toArray();
   }

   public synchronized OpcBrowseServerAddressSpace.Result[] getItems(String[] path) {
      this.goRoot(this.getPeer());
      if (path != null) {
         for (int i = 0; i < path.length; i++) {
            this.goDown(this.getPeer(), path[i]);
         }
      }

      OpcBrowseServerAddressSpace.Items ret = new OpcBrowseServerAddressSpace.Items(false);
      this.listItems(this.getPeer(), ret);
      return (OpcBrowseServerAddressSpace.Result[])ret.toArray();
   }

   private native void goDown(long var1, String var3);

   private native void goRoot(long var1);

   private native void getGroups(long var1, String var3, OpcBrowseServerAddressSpace.Items var4);

   private native void getItems(long var1, String var3, boolean var4, OpcBrowseServerAddressSpace.Items var5);

   private native void listBranches(long var1, OpcBrowseServerAddressSpace.Items var3);

   private native void listItems(long var1, OpcBrowseServerAddressSpace.Items var3);

   private native long queryOrganization(long var1);

   private static class Items {
      Vector<OpcBrowseServerAddressSpace.Result> elements = new Vector<>();
      boolean isGroup;

      Items(boolean isGroup) {
         this.isGroup = isGroup;
      }

      public void add(String name, String id) {
         if (this.isGroup) {
            this.elements.addElement(new OpcBrowseServerAddressSpace.Result(name, id, !this.isGroup));
         } else {
            this.elements.addElement(new OpcBrowseServerAddressSpace.Result(name, id, !this.isGroup));
         }
      }

      public Object[] toArray() {
         Object[] ret = null;
         Object[] var2 = new OpcBrowseServerAddressSpace.Result[this.elements.size()];
         this.elements.copyInto(var2);
         return var2;
      }
   }

   public static class Result implements BrowseResult {
      public String id;
      public String name;
      public boolean isItem;
      public boolean isReadable;
      public boolean isWritable;
      public int dataType;

      public Result(String name, String id, boolean isItem) {
         this.name = name;
         this.id = id;
         this.isItem = isItem;
      }

      @Override
      public int getDataType() {
         return this.dataType;
      }

      @Override
      public OpcItem getItem(OpcItemProperties props) {
         if (!this.isItem()) {
            throw new IllegalStateException("Not an item: " + this.name);
         } else {
            OpcItem ret = new OpcItem(this.name, this.id);
            props.queryAvailableProperties(ret);
            return ret;
         }
      }

      @Override
      public String getName() {
         return this.name;
      }

      @Override
      public String getId() {
         return this.id;
      }

      @Override
      public boolean isItem() {
         return this.isItem;
      }

      @Override
      public boolean isReadable() {
         return this.isReadable;
      }

      @Override
      public boolean isWritable() {
         return this.isWritable;
      }
   }
}
