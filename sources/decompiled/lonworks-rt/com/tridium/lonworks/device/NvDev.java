package com.tridium.lonworks.device;

import com.tridium.lonworks.local.BPseudoNV;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.agent.AgentList;
import javax.baja.control.BControlPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.lonworks.BConfigParameter;
import javax.baja.lonworks.BINvContainer;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonLink;
import javax.baja.lonworks.BLonObject;
import javax.baja.lonworks.BMessageTag;
import javax.baja.lonworks.BNetworkConfig;
import javax.baja.lonworks.BNetworkVariable;
import javax.baja.lonworks.datatypes.BNvConfigData;
import javax.baja.lonworks.datatypes.BNvProps;
import javax.baja.lonworks.enums.BLonLinkType;
import javax.baja.lonworks.proxy.BLonProxyExt;
import javax.baja.naming.BOrd;
import javax.baja.sys.BComponent;
import javax.baja.sys.BLink;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Knob;
import javax.baja.sys.LinkCheck;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Type;

public final class NvDev {
   public static void knobAdded(BComponent nvCtnr, Knob knob, Context context) {
      BLink lnk = knob.getLink();
      if (lnk instanceof BLonLink) {
         BLonLink llk = (BLonLink)lnk;
         llk.lonActivate();
         if (!llk.getMessageTag()) {
            BNetworkVariable nv = (BNetworkVariable)nvCtnr.get(knob.getSourceSlot().asProperty());
            nv.lonKnobAdded(knob);
         }
      }
   }

   public static void knobRemoved(BComponent nvCtnr, Knob knob, Context context) {
      BLink lnk = knob.getLink();
      if (lnk instanceof BLonLink && !((BLonLink)lnk).getMessageTag()) {
         BValue src = nvCtnr.get(knob.getSourceSlot().asProperty());
         ((BNetworkVariable)src).lonKnobRemove(knob);
      }
   }

   public static LinkCheck doNvCheckLink(BComponent source, Slot sourceSlot, BComponent target, Slot targetSlot, Context cx) {
      if (source.getType().is(BLonDevice.TYPE)
         || source.getType().is(BLonObject.TYPE)
         || sourceSlot.isProperty() && sourceSlot.asProperty().getType().is(BPseudoNV.TYPE)) {
         if (!targetSlot.isProperty()) {
            return LinkCheck.makeValid();
         } else {
            Type tgtType = targetSlot.asProperty().getType();
            Type srcType = sourceSlot.asProperty().getType();
            if (tgtType.is(BConfigParameter.TYPE) || srcType.is(BConfigParameter.TYPE)) {
               return LinkCheck.makeInvalid("can not link cps");
            } else if (!tgtType.is(BNetworkConfig.TYPE) && !srcType.is(BNetworkConfig.TYPE)) {
               boolean isTgtNv = tgtType.is(BNetworkVariable.TYPE);
               boolean isTgtMt = tgtType.is(BMessageTag.TYPE);
               if (!isTgtNv && !isTgtMt) {
                  return LinkCheck.makeValid();
               } else {
                  linkUpdate(target, source);
                  if (!sourceSlot.isProperty()) {
                     return isTgtNv
                        ? LinkCheck.makeInvalid("can only link nvs to other nvs")
                        : LinkCheck.makeInvalid("can only link message tags to other message tags");
                  } else if (isTgtNv && !srcType.is(BNetworkVariable.TYPE)) {
                     return LinkCheck.makeInvalid("can only link nvs to other nvs");
                  } else if (!isTgtMt) {
                     BNetworkVariable s = (BNetworkVariable)source.get(sourceSlot.asProperty());
                     BNetworkVariable t = (BNetworkVariable)target.get(targetSlot.asProperty());
                     if (t.getNvConfigData().isOutput()) {
                        return LinkCheck.makeInvalid("target must be an input nv");
                     } else if (s.getNvConfigData().isInput()) {
                        return LinkCheck.makeInvalid("cannot link two inputs");
                     } else {
                        if (s.getSnvtType() <= 0 && t.getSnvtType() <= 0) {
                           if (s.getData().getByteLength() != t.getData().getByteLength()) {
                              return LinkCheck.makeInvalid("nv length mismatch");
                           }
                        } else if (s.getSnvtType() != t.getSnvtType()) {
                           return LinkCheck.makeInvalid("snvt type mismatch");
                        }

                        if (s.getNvProps().getPolled() && !t.getNvProps().getPolled()) {
                           return LinkCheck.makeInvalid("polled output must link to polled input");
                        } else {
                           BLonDevice sDev = s.getDevice();
                           return sDev != null && sDev.getDeviceData().getAliasTable().getAliasCount() == 0 && alreadyLinked(target, source, sourceSlot)
                              ? LinkCheck.makeInvalid("multiple links to same device require alias nvs")
                              : LinkCheck.makeValid();
                        }
                     }
                  } else if (!srcType.is(BMessageTag.TYPE)) {
                     return LinkCheck.makeInvalid("can only link message tags to other message tags");
                  } else {
                     BMessageTag s = (BMessageTag)source.get(sourceSlot.asProperty());
                     if (s.isInput()) {
                        return LinkCheck.makeInvalid("cannot link two inputs");
                     } else if (target.getKnobs(targetSlot).length > 0) {
                        return LinkCheck.makeInvalid("Message tag already linked as source");
                     } else {
                        return source.getLinks(sourceSlot).length > 0 ? LinkCheck.makeInvalid("Message tag already linked as target") : LinkCheck.makeValid();
                     }
                  }
               }
            } else {
               return LinkCheck.makeInvalid("can not link ncis");
            }
         }
      } else {
         return LinkCheck.makeValid();
      }
   }

   private static boolean alreadyLinked(BComponent target, BComponent source, Slot sourceSlot) {
      if (!(target instanceof BINvContainer)) {
         return false;
      } else {
         BINvContainer[] ca = ((BINvContainer)target).getLonDevice().getNvContainers();

         for (int n = 0; n < ca.length; n++) {
            BLink[] a = ca[n].asComponent().getLinks();
            BOrd sourceOrd = source.getOrdInSpace();

            for (int i = 0; i < a.length; i++) {
               BLink l = a[i];
               if (l.getSourceOrd().equals(sourceOrd) && l.getSourceSlotName().equals(sourceSlot.getName())) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static void linkUpdate(BComponent target, BComponent source) {
      if (target.getType().is(BINvContainer.TYPE)) {
         ((BINvContainer)target).linkUpdate();
      }

      if (source.getType().is(BINvContainer.TYPE)) {
         ((BINvContainer)source).linkUpdate();
      }
   }

   public static boolean requiresLonLink(Slot targetSlot) {
      if (!targetSlot.isProperty()) {
         return false;
      } else {
         Type tgtType = targetSlot.asProperty().getType();
         return tgtType.is(BMessageTag.TYPE) || tgtType.is(BLonComponent.TYPE);
      }
   }

   public static BLink makeLonLink(BComponent source, Slot sourceSlot, BComponent target, Slot targetSlot, Context cx) {
      boolean messageTag = targetSlot.asProperty().getType().is(BMessageTag.TYPE);
      BOrd srcOrd = source.getHandleOrd();
      BLonLink lnk = new BLonLink(srcOrd, sourceSlot.getName(), targetSlot.getName(), true);
      lnk.setMessageTag(messageTag);
      boolean remoteLink = false;
      if (source instanceof BLonDevice) {
         remoteLink = !((BINvContainer)target).getLonNetwork().getHandle().equals(((BINvContainer)source).getLonNetwork().getHandle());
      }

      lnk.setRemoteLink(remoteLink);
      if (sourceSlot.asProperty().getType().is(BPseudoNV.TYPE) || targetSlot.asProperty().getType().is(BPseudoNV.TYPE)) {
         lnk.setPseudoLink(true);
      }

      if (source.isRunning() && sourceSlot.asProperty().getType().is(BNetworkVariable.TYPE)) {
         BNetworkVariable nv = (BNetworkVariable)source.get(sourceSlot.asProperty());
         BLonLinkType lt = getLinkType(nv);
         if (lt == BLonLinkType.unknown) {
            nv = (BNetworkVariable)target.get(targetSlot.asProperty());
            lt = getLinkType(nv);
         }

         if (lt == BLonLinkType.unknown) {
            lt = BLonLinkType.standard;
         }

         lnk.setLinkType(lt);
      }

      return lnk;
   }

   public static BLonLinkType getLinkType(BNetworkVariable nv) {
      BNvConfigData cfg = nv.getNvConfigData();
      if (cfg.isBoundNv()) {
         return NmUtil.getLinkType(cfg);
      } else {
         Knob[] ks = nv.getKnobs();

         for (int i = 0; ks != null && i < ks.length; i++) {
            BLink lk = ks[i].getLink();
            if (lk instanceof BLonLink) {
               BLonLinkType t = ((BLonLink)lk).getLinkType();
               if (t != BLonLinkType.unknown) {
                  return t;
               }
            }
         }

         BControlPoint[] cps = nv.getData().getProxies(true);

         for (int ix = 0; cps != null && ix < cps.length; ix++) {
            BAbstractProxyExt pe = cps[ix].getProxyExt();
            if (pe instanceof BLonProxyExt) {
               BLonLinkType t = ((BLonProxyExt)pe).getLinkType();
               if (t != BLonLinkType.unknown) {
                  return t;
               }
            }
         }

         return BLonLinkType.unknown;
      }
   }

   public static AgentList fixWireSheet(AgentList agLst, Context cx) {
      int ndx = agLst.indexOf("wiresheet:WireSheet");
      if (ndx >= 0) {
         agLst.add(ndx, "lonworks:WireSheet");
         agLst.remove("wiresheet:WireSheet");
         agLst.add(ndx + 1, "lonworks:LonWebWiresheet");
      }

      agLst.remove("wiresheet:WebWiresheet");
      return agLst;
   }

   public static NvDev.SaveNv checkRemove(BComponent nvCtnr, Property prop, Context context) {
      if (nvCtnr.isRunning() && prop.getType().is(BLonLink.TYPE)) {
         BLonLink lnk = (BLonLink)nvCtnr.get(prop);
         if (!lnk.getMessageTag()) {
            BNetworkVariable nv = null;

            try {
               nv = lnk.getDestinationNv();
            } catch (Throwable var6) {
            }

            if (nv == null) {
               return null;
            }

            NvDev.SaveNv snv = new NvDev.SaveNv();
            snv.saveNv = nv;
            snv.nvProp = nv.getPropertyInParent();
            snv.frozenNv = nv.getPropertyInParent().isFrozen();
            return snv;
         }
      }

      return null;
   }

   public static void removed(BComponent nvCtnr, NvDev.SaveNv snv, Property prop, BValue value, Context context) {
      if (nvCtnr.isRunning() && prop.getType().is(BLonLink.TYPE)) {
         BLonLink lnk = (BLonLink)nvCtnr.get(prop);
         if (!lnk.getMessageTag()) {
            try {
               if (snv != null) {
                  BNetworkVariable nv = (BNetworkVariable)nvCtnr.get(snv.nvProp);
                  if (snv.frozenNv) {
                     nv.setNvProps((BNvProps)snv.saveNv.getNvProps().newCopy(true));
                     nv.setNvConfigData((BNvConfigData)snv.saveNv.getNvConfigData().newCopy(true));
                  }

                  nv.lonLinkRemoved();
               }
            } catch (Exception var7) {
               Thread.dumpStack();
            }
         }
      }
   }

   public static class SaveNv {
      BNetworkVariable saveNv = null;
      Property nvProp = null;
      boolean frozenNv = false;
   }
}
