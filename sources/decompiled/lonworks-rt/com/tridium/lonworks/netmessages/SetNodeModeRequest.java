package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.FailedResponseException;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class SetNodeModeRequest extends LonMessage implements NetMessages {
   private int mode = 0;
   private int nodeState = 0;
   public static final int APPL_OFFLINE = 0;
   public static final int APPL_ONLINE = 1;
   public static final int APPL_RESET = 2;
   public static final int CHANGE_STATE = 3;
   public static final int APPL_UNCNFG = 2;
   public static final int NO_APPL_UNCNFG = 3;
   public static final int CNFG_ONLINE = 4;
   public static final int CNFG_OFFLINE = 6;

   public SetNodeModeRequest() {
      this.code = 108;
   }

   public SetNodeModeRequest(int mode, int nodeState) throws LonException {
      this.code = 108;
      this.setMode(mode);
      if (mode == 3) {
         switch (nodeState) {
            case 2:
            case 3:
            case 4:
            case 6:
               this.nodeState = nodeState;
               break;
            case 5:
            default:
               throw new IllegalArgumentException("Invalid nodeState " + Integer.toString(nodeState));
         }
      }
   }

   public SetNodeModeRequest(int mode) throws LonException {
      this.code = 108;
      this.setMode(mode);
   }

   public int getMode() {
      return this.mode;
   }

   public void setMode(int mode) throws LonException {
      switch (mode) {
         case 0:
         case 1:
         case 2:
         case 3:
            this.mode = mode;
            return;
         default:
            throw new IllegalArgumentException("Invalid mode " + Integer.toString(mode));
      }
   }

   public int getNodeState() {
      return this.nodeState;
   }

   public void setNodeState(int nodeState) throws LonException {
      switch (nodeState) {
         case 2:
         case 3:
         case 4:
         case 6:
            this.nodeState = nodeState;
            return;
         case 5:
         default:
            throw new IllegalArgumentException("Invalid ");
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.mode);
      if (this.mode == 3) {
         out.write(this.nodeState);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 108) {
         throw new InvalidResponseException(code);
      } else {
         this.setMode(in.read());
         if (this.mode == 3) {
            this.setNodeState(in.read());
         }
      }
   }

   @Override
   public LonMessage toResponse(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code == 12) {
         throw new FailedResponseException();
      } else if (code != 44) {
         throw new InvalidResponseException(code);
      } else {
         return new NoDataResponse(44);
      }
   }
}
