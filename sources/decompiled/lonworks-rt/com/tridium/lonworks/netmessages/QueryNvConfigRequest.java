package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryNvConfigRequest extends LonMessage implements NetMessages {
   private int nvIndex;

   public QueryNvConfigRequest() {
      this.code = 104;
   }

   public QueryNvConfigRequest(int nvIndex) {
      this.code = 104;
      this.nvIndex = nvIndex;
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
   }

   @Override
   public String toString() {
      return "nvIndex=" + this.nvIndex;
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
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 104) {
         throw new InvalidResponseException(code);
      } else {
         this.nvIndex = in.readUnsigned8();
         if (this.nvIndex == 255) {
            this.nvIndex = in.readUnsigned16();
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 8) {
         throw new FailedResponseException();
      } else if (code != 40) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new QueryNvConfigResponse(in);
      }
   }
}
