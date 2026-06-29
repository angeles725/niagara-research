package com.tridium.opc.jni.client.da;

import com.tridium.opc.jni.ComObjectClient;
import com.tridium.opc.jni.OpcInterface;
import java.util.Vector;

public class OpcBrowse extends ComObjectClient {
   public static final OpcInterface IID = new OpcInterface("{39227004-A18F-4b57-8B0A-5235670F4468}", OpcBrowse.class);

   public OpcBrowse.Result[] browse(String id) {
      OpcBrowse.Elements s = new OpcBrowse.Elements();
      this.browse(this.getPeer(), id, s);
      OpcBrowse.Result[] ret = new OpcBrowse.Result[s.elements.size()];
      s.elements.copyInto(ret);
      return ret;
   }

   private native void browse(long var1, String var3, OpcBrowse.Elements var4);

   private static class Elements {
      Vector<OpcBrowse.Result> elements = new Vector<>();

      private Elements() {
      }

      public void add(String name, String id, boolean isItem, boolean hasChildren, int dataType, boolean isReadable, boolean isWritable) {
         OpcBrowse.Result e = new OpcBrowse.Result();
         e.name = name;
         e.id = id;
         e.isItem = isItem;
         e.isReadable = isReadable;
         e.isWritable = isWritable;
         e.hasChildren = hasChildren;
         e.dataType = dataType;
      }
   }

   public static class Result implements BrowseResult {
      String id;
      String name;
      boolean isItem;
      boolean isReadable;
      boolean isWritable;
      boolean hasChildren;
      int dataType;

      @Override
      public int getDataType() {
         return this.dataType;
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
      public OpcItem getItem(OpcItemProperties props) {
         if (!this.isItem()) {
            throw new IllegalStateException("Not an item>");
         } else {
            OpcItem ret = new OpcItem(this.name, this.id);
            props.queryAvailableProperties(ret);
            return ret;
         }
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
