package com.tridium.opc.jni.client.da;

public interface BrowseResult {
   int getDataType();

   String getId();

   String getName();

   OpcItem getItem(OpcItemProperties var1);

   boolean isItem();

   boolean isReadable();

   boolean isWritable();
}
