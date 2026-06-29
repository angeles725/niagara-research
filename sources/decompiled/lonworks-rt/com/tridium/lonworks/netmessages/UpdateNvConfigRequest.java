package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class UpdateNvConfigRequest extends LonMessage implements NetMessages {
   private int nvIndex;
   private BNvConfigData configData = null;
   private int primary = -1;

   public UpdateNvConfigRequest() {
      this.code = 107;
   }

   public UpdateNvConfigRequest(int nvIndex, BNvConfigData configData) {
      this.code = 107;
      this.nvIndex = nvIndex;
      this.configData = configData;
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   public BNvConfigData getConfigData() {
      return this.configData;
   }

   public void setConfigData(BNvConfigData configData) {
      this.configData = configData;
   }

   public int getPrimary() {
      return this.primary;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.nvIndex > 254) {
         out.write(255);
         out.writeUnsigned16(this.nvIndex);
      } else {
         out.write(this.nvIndex);
      }

      this.configData.writeNetworkBytes(out);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 107) {
         throw new InvalidResponseException(code);
      } else {
         this.nvIndex = in.read();
         if (this.nvIndex == 255) {
            this.nvIndex = in.readUnsigned16();
         }

         this.configData = new BNvConfigData();
         this.configData.fromInputStream(in);
         if (in.available() > 0) {
            this.primary = in.readUnsigned8();
            if (this.primary == 255) {
               this.primary = in.readUnsigned16();
            }
         }
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
