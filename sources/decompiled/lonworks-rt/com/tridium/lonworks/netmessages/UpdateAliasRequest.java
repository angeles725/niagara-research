package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BAliasConfigData;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class UpdateAliasRequest extends LonMessage implements NetMessages {
   private int nvIndex;
   private BAliasConfigData aliasData = null;

   public UpdateAliasRequest() {
      this.code = 107;
   }

   public UpdateAliasRequest(int nvIndex, BAliasConfigData aliasData) {
      this.code = 107;
      this.nvIndex = nvIndex;
      this.aliasData = aliasData;
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   public BAliasConfigData getConfigData() {
      return this.aliasData;
   }

   public void setConfigData(BAliasConfigData aliasData) {
      this.aliasData = aliasData;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.nvIndex > 254) {
         out.writeUnsigned8(255);
         out.writeUnsigned16(this.nvIndex);
      } else {
         out.writeUnsigned8(this.nvIndex);
      }

      this.aliasData.writeNetworkBytes(out);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 107) {
         throw new InvalidResponseException(code);
      } else {
         this.nvIndex = in.readUnsigned8();
         if (this.nvIndex == 255) {
            this.nvIndex = in.readUnsigned16();
         }

         this.aliasData = new BAliasConfigData();
         this.aliasData.fromInputStream(in);
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 11) {
         throw new FailedResponseException();
      } else if (code != 43) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(43);
      }
   }
}
