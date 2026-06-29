package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class WinkRequest extends LonMessage implements NetMessages {
   private int subCommand = -1;
   private int networkInterface = 0;
   public static final int WINK = 0;
   public static final int SEND_ID_INFO = 1;
   public static final int NV_DEFINE = 2;
   public static final int NV_REMOVE = 3;
   public static final int QUERY_NV_INFO = 4;
   public static final int QUERY_NODE_INFO = 5;
   public static final int UPDATE_NV_INFO = 6;
   public static final int UNUSED_SUBCOMMAND = -1;

   public WinkRequest() {
      this.code = 112;
   }

   public WinkRequest(int subCommand, int networkInterface) throws LonException {
      this.code = 112;
      this.setSubCommand(subCommand);
      if (subCommand == 1) {
         this.networkInterface = networkInterface;
      }
   }

   public int getSubCommand() {
      return this.subCommand;
   }

   public void setSubCommand(int subCommand) throws LonException {
      this.subCommand = subCommand;
   }

   public int getNetworkInterface() {
      return this.networkInterface;
   }

   public void setNetworkInterface(int networkInterface) {
      this.networkInterface = networkInterface;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.subCommand != -1) {
         out.write(this.subCommand);
         if (this.subCommand == 1) {
            out.write(this.networkInterface);
         }
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 112) {
         throw new InvalidResponseException(code);
      } else {
         if (in.available() > 0) {
            this.subCommand = in.read();
            switch (this.subCommand) {
               case 0:
               case 1:
                  this.networkInterface = in.read();
            }
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 16) {
         throw new FailedResponseException();
      } else if (code != 48) {
         throw new InvalidResponseException(code);
      } else {
         in.reset();
         return new WinkResponse(in);
      }
   }
}
