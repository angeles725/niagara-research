package com.tridium.template;

import com.tridium.util.EscUtil;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BObject;
import javax.baja.sys.BSimple;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public final class BRelationInfo extends BSimple {
   public static final BRelationInfo DEFAULT = new BRelationInfo(false, "", "", "", "");
   public static final Type TYPE = Sys.loadType(BRelationInfo.class);
   boolean inbound = false;
   String relationId = "";
   String relateHints = "";
   String userTip = "";
   String slotPathScope = "";
   private static final BRelationInfo.RelationInfoEsc relInfoEscape = new BRelationInfo.RelationInfoEsc();

   public Type getType() {
      return TYPE;
   }

   public static BRelationInfo make(boolean inbound, String relationId, String relateHints, String userTip, String slotPathScope) {
      return new BRelationInfo(inbound, relationId, relateHints, userTip, slotPathScope);
   }

   private BRelationInfo(boolean inbound, String relationId, String relateHints, String userTip, String slotPathScope) {
      this.inbound = inbound;
      this.relationId = relationId;
      this.relateHints = relateHints;
      this.userTip = userTip;
      this.slotPathScope = slotPathScope;
   }

   public String getRelationId() {
      return this.relationId;
   }

   public String getRelateHints() {
      return this.relateHints;
   }

   public boolean getInbound() {
      return this.inbound;
   }

   public String getUserTip() {
      return this.userTip;
   }

   public String getSlotPathScope() {
      return this.slotPathScope;
   }

   public void setRelateHints(String hints) {
      this.relateHints = hints;
   }

   public void setInbound(boolean inbound) {
      this.inbound = inbound;
   }

   public void setUserTip(String userTip) {
      this.userTip = userTip;
   }

   public void setSlotPathScope(String slotPathScope) {
      this.slotPathScope = slotPathScope;
   }

   public boolean equals(Object obj) {
      if (!(obj instanceof BRelationInfo)) {
         return false;
      } else {
         BRelationInfo other = (BRelationInfo)obj;
         return other.inbound == this.inbound
            && other.relationId.equals(this.relationId)
            && other.relateHints.equals(this.relateHints)
            && other.userTip.equals(this.userTip)
            && other.slotPathScope.equals(this.slotPathScope);
      }
   }

   public void encode(DataOutput encoder) throws IOException {
      encoder.writeBoolean(this.inbound);
      encoder.writeUTF(this.relationId);
      encoder.writeUTF(this.relateHints);
      encoder.writeUTF(this.userTip);
      encoder.writeUTF(this.slotPathScope);
   }

   public BObject decode(DataInput decoder) throws IOException {
      boolean inbound = decoder.readBoolean();
      String relationId = decoder.readUTF();
      String relateHints = decoder.readUTF();
      String userTip = decoder.readUTF();
      String slotPathScope = "";

      try {
         slotPathScope = decoder.readUTF();
      } catch (Exception var8) {
      }

      return new BRelationInfo(inbound, relationId, relateHints, userTip, slotPathScope);
   }

   public String encodeToString() throws IOException {
      return (this.inbound ? "In," : "Out,")
         + this.relationId
         + ","
         + this.relateHints
         + ","
         + relInfoEscape.escape(this.userTip)
         + ","
         + relInfoEscape.escape(this.slotPathScope);
   }

   public BObject decodeFromString(String s) throws IOException {
      boolean inbound = false;
      String relateId = "";
      String relateHints = "";
      String userTip = "";
      String slotPathScope = "";
      String[] parms = s.split(",");
      int numParms = 5;
      if (parms.length < 5) {
         numParms = parms.length;
      }

      switch (numParms) {
         case 5:
            slotPathScope = relInfoEscape.unescape(parms[4].trim());
         case 4:
            userTip = relInfoEscape.unescape(parms[3].trim());
         case 3:
            relateHints = parms[2].trim();
         case 2:
            relateId = parms[1].trim();
         case 1:
            String var9 = parms[0];
            switch (var9) {
               case "In":
                  inbound = true;
                  break;
               case "Out":
                  inbound = false;
                  break;
               default:
                  throw new IllegalArgumentException("Invalid boolean: " + s);
            }
         default:
            return new BRelationInfo(inbound, relateId, relateHints, userTip, slotPathScope);
      }
   }

   public boolean hasValidRelateHints() {
      return !this.getRelateHints().isEmpty();
   }

   public String toString(Context cx) {
      return (this.inbound ? "In," : "Out,") + this.relationId + "," + this.relateHints + "," + this.userTip + "," + this.slotPathScope;
   }

   public static class RelationInfoEsc extends EscUtil {
      public boolean isStart(int c) {
         return c != 44 && c != 36;
      }

      public boolean isPart(int c) {
         return c != 44 && c != 36;
      }
   }
}
