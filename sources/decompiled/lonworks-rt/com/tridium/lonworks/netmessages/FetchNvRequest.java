package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class FetchNvRequest extends LonMessage implements NetMessages {
   private int nvIndex;

   public FetchNvRequest() {
      this.code = 115;
   }

   public FetchNvRequest(int nvIndex) {
      this.code = 115;
      this.nvIndex = nvIndex;
   }

   public int getNvIndex() {
      return this.nvIndex;
   }

   public void setNvIndex(int nvIndex) {
      this.nvIndex = nvIndex;
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
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 115) {
         throw new InvalidResponseException(code);
      } else {
         this.nvIndex = in.read();
         if (this.nvIndex == 255) {
            this.nvIndex = in.readUnsigned16();
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 19) {
         throw new FailedResponseException();
      } else if (code != 51) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new FetchNvResponse(in);
      }
   }
}
