package javax.baja.lonworks;

import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BConversionLink;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BNullConverter;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "linkType",
      type = "BLonLinkType",
      defaultValue = "BLonLinkType.standard"
   ), @NiagaraProperty(
      name = "priority",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "messageTag",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "remoteLink",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "pseudoLink",
      type = "boolean",
      defaultValue = "false"
   )})
public class BLonLink extends BConversionLink {
   public static final Property linkType = newProperty(0, BLonLinkType.standard, null);
   public static final Property priority = newProperty(0, false, null);
   public static final Property messageTag = newProperty(0, false, null);
   public static final Property remoteLink = newProperty(0, false, null);
   public static final Property pseudoLink = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BLonLink.class);

   public BLonLink(BComponent source, Slot sourceSlot, Slot targetSlot) {
      super(source, sourceSlot, targetSlot, new BNullConverter());
   }

   public BLonLink(BOrd sourceOrd, String sourceSlot, String targetSlot, boolean enabled) {
      super(sourceOrd, sourceSlot, targetSlot, enabled, new BNullConverter());
   }

   public BLonLink() {
   }

   public BLonLinkType getLinkType() {
      return (BLonLinkType)this.get(linkType);
   }

   public void setLinkType(BLonLinkType v) {
      this.set(linkType, v, null);
   }

   public boolean getPriority() {
      return this.getBoolean(priority);
   }

   public void setPriority(boolean v) {
      this.setBoolean(priority, v, null);
   }

   public boolean getMessageTag() {
      return this.getBoolean(messageTag);
   }

   public void setMessageTag(boolean v) {
      this.setBoolean(messageTag, v, null);
   }

   public boolean getRemoteLink() {
      return this.getBoolean(remoteLink);
   }

   public void setRemoteLink(boolean v) {
      this.setBoolean(remoteLink, v, null);
   }

   public boolean getPseudoLink() {
      return this.getBoolean(pseudoLink);
   }

   public void setPseudoLink(boolean v) {
      this.setBoolean(pseudoLink, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void lonActivate() {
      if (!this.getMessageTag()) {
         this.getDestinationNv();
      }
   }

   void propagateNv(BNetworkVariable srcNv) {
      if (!this.getMessageTag()) {
         BNetworkVariable destNv = this.getDestinationNv();
         if (this.getRemoteLink() || this.getPseudoLink() || !this.isBound(srcNv, destNv)) {
            BLonData srcData = srcNv.getData();
            BLonData destData = destNv.getData();
            if (!this.propagateObjects(srcData, destData)) {
               destData.fromNetBytes(srcData.toNetBytes());
            }

            destNv.dataChanged(null);
         }
      }
   }

   public boolean isBound(BNetworkVariable srcNv) {
      return this.getMessageTag() ? false : this.isBound(srcNv, this.getDestinationNv());
   }

   private boolean isBound(BNetworkVariable srcNv, BNetworkVariable destNv) {
      BNvConfigData srcNvCfg = srcNv.getNvConfigData();
      BNvConfigData destNvCfg = destNv.getNvConfigData();
      if (srcNvCfg.isBoundNv() && destNvCfg.isBoundNv()) {
         if (srcNvCfg.getSelector() == destNvCfg.getSelector()) {
            return true;
         }

         if (srcNv.lonDevice().getAlias(srcNv, 0) != null) {
            return true;
         }
      }

      return false;
   }

   public BNetworkVariable getDestinationNv() {
      BComponent t = this.getTargetComponent();
      Property tProp = this.getTargetSlot().asProperty();
      return (BNetworkVariable)t.get(tProp);
   }

   private BNetworkVariable getSourceNv() {
      BComponent s = this.getSourceComponent();
      Property sProp = this.getSourceSlot().asProperty();
      return (BNetworkVariable)s.get(sProp);
   }

   private boolean propagateObjects(BLonData src, BLonData dest) {
      Property[] sa = src.getPropertiesArray();
      Property[] da = dest.getPropertiesArray();
      if (sa.length != da.length) {
         return false;
      } else {
         try {
            for (int i = 0; i < sa.length; i++) {
               Property srcProp = sa[i];
               Property destProp = da[i];
               if (srcProp.getType() != destProp.getType()) {
                  return false;
               }

               if (srcProp.getType().is(BLonPrimitive.TYPE)) {
                  dest.set(destProp, src.get(srcProp), BLonNetwork.lonNoWrite);
               } else if (srcProp.getType().is(BLonData.TYPE) && !this.propagateObjects((BLonData)src.get(srcProp), (BLonData)dest.get(destProp))) {
                  return false;
               }
            }

            return true;
         } catch (Throwable var8) {
            System.out.println("Error in propagateObjects." + this.toString(null) + "\n" + var8);
            return false;
         }
      }
   }

   public final void propagate(BValue arg) {
      if (!this.getMessageTag()) {
         BNetworkVariable srcNv = this.getSourceNv();
         if (srcNv.isRunning()) {
            this.propagateNv(srcNv);
         }
      }
   }

   public String toString(Context c) {
      return (this.getMessageTag() ? "msgTag " : "") + super.toString(c) + "| linkType = " + this.getLinkType() + " priority = " + this.getPriority();
   }
}
