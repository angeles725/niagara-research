package com.tridium.template;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.ByteArrayUtil;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BTemplateSignature extends BSimple {
   public static final BTemplateSignature DEFAULT = new BTemplateSignature(new byte[0]);
   public static final Type TYPE = Sys.loadType(BTemplateSignature.class);
   private final byte[] signature;
   private final int version;
   public static final int DEFAULT_VERSION = 1;
   public static final int CURRENT_VERSION = 1;

   public Type getType() {
      return TYPE;
   }

   public BTemplateSignature(byte[] signature) {
      this(1, signature);
   }

   public BTemplateSignature(int version, byte[] signature) {
      this.signature = (byte[])signature.clone();
      this.version = version;
   }

   public boolean equals(Object obj) {
      return !(obj instanceof BTemplateSignature)
         ? false
         : this.version == ((BTemplateSignature)obj).version && Arrays.equals(this.signature, ((BTemplateSignature)obj).signature);
   }

   public void encode(DataOutput encoder) throws IOException {
      encoder.writeUTF(this.encodeToString());
   }

   public BObject decode(DataInput decoder) throws IOException {
      return this.decodeFromString(decoder.readUTF());
   }

   public String encodeToString() throws IOException {
      return this.version + ":" + ByteArrayUtil.toHexString(this.signature);
   }

   public BObject decodeFromString(String s) throws IOException {
      int version = 1;
      String signatureString = s;
      int colIndex = s.indexOf(58);
      if (colIndex >= 0) {
         String versionString = s.substring(0, colIndex);
         version = Integer.parseInt(versionString);
         signatureString = s.substring(colIndex + 1);
      }

      byte[] signatureBytes = ByteArrayUtil.hexStringToBytes(signatureString);
      return new BTemplateSignature(version, signatureBytes);
   }
}
