package javax.baja.lonworks.datatypes;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BModifyFlags extends BSimple {
   public static final BModifyFlags DEFAULT = make(true, false, false, false, false, false);
   public static final Type TYPE = Sys.loadType(BModifyFlags.class);
   private boolean readWrite_ = false;
   private boolean mfgOnly_ = false;
   private boolean reset_ = false;
   private boolean constant_ = false;
   private boolean offline_ = false;
   private boolean disabled_ = false;
   public static final int MFG_ONLY = 16;
   public static final int MUST_RESET = 8;
   public static final int CONSTANT = 4;
   public static final int MOD_OFFLINE = 2;
   public static final int MOD_DISABLED = 1;
   public static final BModifyFlags mfgOnly = fromFlags(16);
   public static final BModifyFlags mustReset = fromFlags(8);
   public static final BModifyFlags constant = fromFlags(4);
   public static final BModifyFlags modOffline = fromFlags(2);
   public static final BModifyFlags modDisabled = fromFlags(1);

   public Type getType() {
      return TYPE;
   }

   public static BModifyFlags make(boolean readWrite, boolean mfgOnly, boolean reset, boolean constant, boolean offline, boolean disabled) {
      return new BModifyFlags(readWrite, mfgOnly, reset, constant, offline, disabled);
   }

   public static BModifyFlags fromFlags(int flags) {
      boolean mfgOnly = (flags & 16) != 0;
      boolean reset = (flags & 8) != 0;
      boolean constant = (flags & 4) != 0;
      boolean offline = (flags & 2) != 0;
      boolean disabled = (flags & 1) != 0;
      boolean readWrite = !constant && !mfgOnly;
      return make(readWrite, mfgOnly, reset, constant, offline, disabled);
   }

   private BModifyFlags(boolean readWrite, boolean mfgOnly, boolean reset, boolean constant, boolean offline, boolean disabled) {
      this.readWrite_ = readWrite;
      this.mfgOnly_ = mfgOnly;
      this.reset_ = reset;
      this.constant_ = constant;
      this.offline_ = offline;
      this.disabled_ = disabled;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BModifyFlags)) {
         return false;
      } else {
         BModifyFlags comp = (BModifyFlags)obj;
         return comp.readWrite_ == this.readWrite_
            && comp.mfgOnly_ == this.mfgOnly_
            && comp.reset_ == this.reset_
            && comp.constant_ == this.constant_
            && comp.offline_ == this.offline_
            && comp.disabled_ == this.disabled_;
      }
   }

   public String toString(Context context) {
      return this.encodeToString();
   }

   public void encode(DataOutput out) throws IOException {
      out.writeBoolean(this.readWrite_);
      out.writeBoolean(this.mfgOnly_);
      out.writeBoolean(this.reset_);
      out.writeBoolean(this.constant_);
      out.writeBoolean(this.offline_);
      out.writeBoolean(this.disabled_);
   }

   public BObject decode(DataInput in) throws IOException {
      return new BModifyFlags(in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean(), in.readBoolean());
   }

   public String encodeToString() {
      StringBuilder sb = new StringBuilder();
      boolean comma = false;
      if (this.readWrite_) {
         sb.append("readWrite");
         comma = true;
      }

      if (this.mfgOnly_) {
         if (comma) {
            sb.append(",");
         }

         sb.append("mfgOnly");
         comma = true;
      }

      if (this.reset_) {
         if (comma) {
            sb.append(",");
         }

         sb.append("reset");
         comma = true;
      }

      if (this.constant_) {
         if (comma) {
            sb.append(",");
         }

         sb.append("constant");
         comma = true;
      }

      if (this.offline_) {
         if (comma) {
            sb.append(",");
         }

         sb.append("offline");
         comma = true;
      }

      if (this.disabled_) {
         if (comma) {
            sb.append(",");
         }

         sb.append("objDisable");
         comma = true;
      }

      return sb.toString();
   }

   public BObject decodeFromString(String s) throws IOException {
      boolean readWrite = false;
      boolean mfgOnly = false;
      boolean reset = false;
      boolean constant = false;
      boolean offline = false;
      boolean disabled = false;
      StringTokenizer st = new StringTokenizer(s, ",");

      while (st.hasMoreTokens()) {
         String flag = st.nextToken();
         if (flag.equals("readWrite")) {
            readWrite = true;
         } else if (flag.equals("mfgOnly")) {
            mfgOnly = true;
         } else if (flag.equals("reset")) {
            reset = true;
         } else if (flag.equals("constant")) {
            constant = true;
         } else if (flag.equals("offline")) {
            offline = true;
         } else if (flag.equals("objDisabl")) {
            disabled = true;
         } else if (flag.equals("objDisable")) {
            disabled = true;
         }
      }

      return make(readWrite, mfgOnly, reset, constant, offline, disabled);
   }

   public boolean isReadWrite() {
      return this.readWrite_;
   }

   public boolean isMfgOnly() {
      return this.mfgOnly_;
   }

   public boolean isReset() {
      return this.reset_;
   }

   public boolean isConst() {
      return this.constant_;
   }

   public boolean isOffline() {
      return this.offline_;
   }

   public boolean isDisabled() {
      return this.disabled_;
   }
}
