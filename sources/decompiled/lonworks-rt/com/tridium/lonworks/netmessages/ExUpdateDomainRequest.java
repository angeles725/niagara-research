package com.tridium.lonworks.netmessages;

import com.tridium.lonworks.util.LonByteArrayUtil;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.io.LonOutputStream;

public class ExUpdateDomainRequest extends ExNetMgmtCommand {
   private static final int VALID_DOMAIN_LEN0 = 0;
   private static final int VALID_DOMAIN_LEN1 = 1;
   private static final int VALID_DOMAIN_LEN3 = 3;
   private static final int VALID_DOMAIN_LEN6 = 6;
   public static final int DOMAIN_NOT_USED_LEN = 255;
   public int domainIndex;
   public byte[] id = new byte[6];
   public int subnet;
   public int node;
   public int len;
   public byte[] key = new byte[6];

   public ExUpdateDomainRequest() {
      this.code = 112;
      this.setAppCommand(37);
      this.setResource(2);
   }

   public ExUpdateDomainRequest(int domainIndex) {
      this(domainIndex, 0, 0);
      this.len = 255;
   }

   public ExUpdateDomainRequest(int domainIndex, int subnet, int node) {
      this();
      this.domainIndex = domainIndex;
      this.subnet = subnet;
      this.node = node;
      this.len = 0;

      for (int index = 0; index < 6; index++) {
         this.id[index] = 0;
      }

      for (int var5 = 0; var5 < 6; var5++) {
         this.key[var5] = 0;
      }
   }

   public ExUpdateDomainRequest(int domainIndex, byte[] nid, int subnet, int node, int len, byte[] key) throws LonException {
      this();
      this.domainIndex = domainIndex;
      this.len = len;
      switch (len) {
         case 1:
         case 3:
         case 6:
            System.arraycopy(nid, 0, this.id, 0, len);
         case 0:
            for (int index = len; index < 6; index++) {
               this.id[index] = 0;
            }

            this.subnet = subnet;
            this.node = node;
            if (key.length != 6) {
               throw new IllegalArgumentException("Invalid ");
            } else {
               System.arraycopy(key, 0, this.key, 0, 6);
               return;
            }
         case 2:
         case 4:
         case 5:
         default:
            throw new IllegalArgumentException("Invalid ");
      }
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   @Override
   public void setDomainIndex(int domainIndex) {
      this.domainIndex = domainIndex;
   }

   public byte[] getId() {
      return this.id;
   }

   public void setId(byte[] id) {
      System.arraycopy(id, 0, this.id, 0, id.length);
   }

   public int getSubnet() {
      return this.subnet;
   }

   public void setSubnet(int subnet) {
      this.subnet = subnet;
   }

   public int getNode() {
      return this.node;
   }

   public void setNode(int node) {
      this.node = node;
   }

   public int getLength() {
      return this.len;
   }

   public void setLength(int len) {
      this.len = len;
   }

   public byte[] getKey() {
      return this.key;
   }

   public void setKey(byte[] key) {
      System.arraycopy(key, 0, this.key, 0, key.length);
   }

   @Override
   public void writeMessageData(LonOutputStream out) {
      out.writeUnsigned16(this.domainIndex);
      out.writeByteArray(this.id);
      out.writeUnsigned8(this.subnet);
      out.writeUnsigned8(this.node | 128);
      out.writeUnsigned8(this.len);
      out.writeByteArray(this.key);
   }

   @Override
   public String toString() {
      return "ExUpdateDomainRequest: domainIndex = "
         + this.domainIndex
         + " id = "
         + LonByteArrayUtil.toString(this.id, ':')
         + " subnet = "
         + this.subnet
         + " node = "
         + this.node
         + " len = "
         + this.len
         + " key = "
         + LonByteArrayUtil.toString(this.key, ':');
   }
}
