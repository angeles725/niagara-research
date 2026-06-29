package com.tridium.opc.jni.client.da;

import com.tridium.opc.OpcException;
import com.tridium.opc.jni.ComObjectClient;

public class OpcGroup extends ComObjectClient {
   private long asyncCbCookie = 0L;
   private OpcGroup.GroupListener groupListener;
   private String name;
   public int serverHandle = 0;
   public int revisedUpdateRate = 0;

   public OpcGroup(String name, long peer) {
      this.name = name;
      this.peer = peer;
   }

   public void asyncRefresh(boolean fromCache) throws OpcException {
      OpcAsyncIo2 ifcAsyncIo2 = (OpcAsyncIo2)this.query(OpcAsyncIo2.IID);
      ifcAsyncIo2.refresh(fromCache);
      ifcAsyncIo2.release();
   }

   public OpcGroup.GroupListener getGroupListener() {
      return this.groupListener;
   }

   public OpcGroupStateMgt getGroupStateMgt() throws OpcException {
      return (OpcGroupStateMgt)this.query(OpcGroupStateMgt.IID);
   }

   public OpcItemMgt getItemMgt() throws OpcException {
      return (OpcItemMgt)this.query(OpcItemMgt.IID);
   }

   public String getName() {
      return this.name;
   }

   public OpcSyncIo getSyncIo() throws OpcException {
      OpcSyncIo ifcSyncIo = (OpcSyncIo)this.query(OpcSyncIo.IID);
      if (ifcSyncIo != null) {
         ifcSyncIo.group = this;
      }

      return ifcSyncIo;
   }

   public OpcAsyncIo2 getAsyncIo() throws OpcException {
      OpcAsyncIo2 ifcAsyncIo2 = (OpcAsyncIo2)this.query(OpcAsyncIo2.IID);
      if (ifcAsyncIo2 != null) {
         ifcAsyncIo2.group = this;
      }

      return ifcAsyncIo2;
   }

   public void registerAsyncCallback() throws OpcException {
      OpcAsyncIo2 ifcAsyncIo2 = (OpcAsyncIo2)this.query(OpcAsyncIo2.IID);
      this.asyncCbCookie = ifcAsyncIo2.registerCallback(this);
      ifcAsyncIo2.release();
   }

   @Override
   public final void release() {
   }

   public void setGroupListener(OpcGroup.GroupListener listener) {
      this.groupListener = listener;
   }

   public void unregisterAsyncCallback() throws OpcException {
      if (this.asyncCbCookie != 0L) {
         OpcAsyncIo2 ifcAsyncIo2 = (OpcAsyncIo2)this.query(OpcAsyncIo2.IID);
         ifcAsyncIo2.unregisterCallback(this.asyncCbCookie);
         ifcAsyncIo2.release();
         this.asyncCbCookie = 0L;
      }
   }

   void realRelease() {
      this.unregisterAsyncCallback();
      super.release();
   }

   private void asyncCallbackDeleted() {
      this.asyncCbCookie = 0L;
      if (this.groupListener != null) {
         this.groupListener.asyncCallbackDeleted();
      }
   }

   private void updateBoolean(int handle, boolean val, long utcTimestamp, int quality, int hresult, boolean isSync) {
      if (this.groupListener != null) {
         this.groupListener.updateBoolean(handle, val, utcTimestamp, quality, hresult, isSync);
      }
   }

   private void updateError(int handle, long utcTimestamp, int quality, int hresult, boolean isSync) {
      if (this.groupListener != null) {
         this.groupListener.updateError(handle, utcTimestamp, quality, hresult, isSync);
      }
   }

   private void updateNumeric(int handle, double val, long utcTimestamp, int quality, int hresult, boolean isSync) {
      if (this.groupListener != null) {
         this.groupListener.updateNumeric(handle, val, utcTimestamp, quality, hresult, isSync);
      }
   }

   private void updateString(int handle, String val, long utcTimestamp, int quality, int hresult, boolean isSync) {
      if (this.groupListener != null) {
         this.groupListener.updateString(handle, val, utcTimestamp, quality, hresult, isSync);
      }
   }

   private void OnWriteCompleted(int transId, int grpHandle, int hrMaster, int noOfItems, int[] clientHandle, int[] hresultArray) {
      if (this.groupListener != null) {
         this.groupListener.OnWriteCompleted(transId, grpHandle, hrMaster, noOfItems, clientHandle, hresultArray);
      }
   }

   private void OnReturnWrite(boolean isSync, int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
      if (this.groupListener != null) {
         this.groupListener.OnReturnWrite(isSync, hresult, noOfItems, transId, cancelId, serverHandle, hresultArray);
      }
   }

   private void OnReturnRead(int hresult, int noOfItems, int transId, int cancelId, int[] serverHandle, int[] hresultArray) {
      if (this.groupListener != null) {
         this.groupListener.OnReturnRead(hresult, noOfItems, transId, cancelId, serverHandle, hresultArray);
      }
   }

   private void OnReadCompleted(
      int transId,
      int hrMasterQty,
      int hrMaster,
      int noOfItems,
      int[] ClientHandles,
      int[] dataTypes,
      String[] values,
      int[] qualities,
      long[] timeStamps,
      int[] hresultArray
   ) {
      if (this.groupListener != null) {
         this.groupListener.OnReadCompleted(transId, hrMasterQty, hrMaster, noOfItems, ClientHandles, dataTypes, values, qualities, timeStamps, hresultArray);
      }
   }

   public interface GroupListener {
      void asyncCallbackDeleted();

      void updateBoolean(int var1, boolean var2, long var3, int var5, int var6, boolean var7);

      void updateError(int var1, long var2, int var4, int var5, boolean var6);

      void updateNumeric(int var1, double var2, long var4, int var6, int var7, boolean var8);

      void updateString(int var1, String var2, long var3, int var5, int var6, boolean var7);

      void OnWriteCompleted(int var1, int var2, int var3, int var4, int[] var5, int[] var6);

      void OnReturnWrite(boolean var1, int var2, int var3, int var4, int var5, int[] var6, int[] var7);

      void OnReturnRead(int var1, int var2, int var3, int var4, int[] var5, int[] var6);

      void OnReadCompleted(int var1, int var2, int var3, int var4, int[] var5, int[] var6, String[] var7, int[] var8, long[] var9, int[] var10);
   }
}
