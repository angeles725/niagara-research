package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryNvConfigResponse extends LonMessage implements NetMessages {
   public static final int QUERY_CONFIG_RESP_NETLEN = 3;
   BNvConfigData configData = null;
   boolean isAlias = false;

   public QueryNvConfigResponse() {
   }

   public QueryNvConfigResponse(BNvConfigData configData) {
      this.code = 40;
      this.configData = configData;
   }

   public QueryNvConfigResponse(LonInputStream in) throws LonException {
      this.code = 40;
      this.fromInputStream(in);
   }

   public BNvConfigData getConfigData() {
      return this.configData;
   }

   public void setConfigData(BNvConfigData configData) {
      this.configData = configData;
   }

   public boolean isExtended() {
      return false;
   }

   public boolean isAlias() {
      return this.isAlias;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.configData != null) {
         this.configData.writeNetworkBytes(out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 40) {
         throw new InvalidResponseException(code);
      } else {
         this.configData = new BNvConfigData();
         this.configData.fromInputStream(in);
      }
   }
}
