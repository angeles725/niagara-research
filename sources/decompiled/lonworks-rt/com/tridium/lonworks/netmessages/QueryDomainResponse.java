package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.datatypes.BDomainId;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryDomainResponse extends LonMessage implements NetMessages {
   public byte[] id = new byte[6];
   public int subnet;
   public int node;
   public int len;
   public byte[] key = new byte[6];

   public QueryDomainResponse() {
   }

   public QueryDomainResponse(LonInputStream in) throws LonException {
      this.code = 42;
      this.fromInputStream(in);
   }

   public boolean isExtended() {
      return false;
   }

   public byte[] getDomainId() {
      return this.id;
   }

   public int getNodeId() {
      return this.node;
   }

   public int getSubnet() {
      return this.subnet;
   }

   public int getLen() {
      return this.len;
   }

   public byte[] getKey() {
      return this.key;
   }

   public boolean inUse() {
      return this.len != 255;
   }

   public boolean sameDomain(BDomainId comp) {
      if (this.len != comp.getLength()) {
         return false;
      } else {
         byte[] compId = comp.getDomainId();

         for (int i = 0; i < this.len; i++) {
            if (this.id[i] != compId[i]) {
               return false;
            }
         }

         return true;
      }
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      this.writeMessageData(out);
   }

   protected void writeMessageData(LonOutputStream out) {
      out.writeByteArray(this.id, 6);
      out.writeUnsigned8(this.subnet);
      out.writeUnsigned8(this.node | 128);
      out.writeUnsigned8(this.len);
      out.writeByteArray(this.key, 6);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 42) {
         throw new InvalidResponseException(code);
      } else {
         this.readMessageData(in);
      }
   }

   protected void readMessageData(LonInputStream in) {
      this.id = in.readByteArray(6);
      this.subnet = in.readUnsigned8();
      this.node = in.readUnsigned8() & -129;
      this.len = in.readUnsigned8();
      this.key = in.readByteArray(6);
   }

   @Override
   public String toString() {
      return " subnet = "
         + this.subnet
         + " node = "
         + this.node
         + " domain length = "
         + this.len
         + " domain = "
         + LonByteArrayUtil.toString(this.id, this.len)
         + " key = "
         + LonByteArrayUtil.toString(this.key)
         + "\n";
   }
}
