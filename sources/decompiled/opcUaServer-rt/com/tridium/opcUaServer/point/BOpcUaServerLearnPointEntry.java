package com.tridium.opcUaServer.point;

import com.tridium.ndriver.discover.BINDiscoveryIcon;
import com.tridium.ndriver.discover.BINDiscoveryLeaf;
import com.tridium.ndriver.util.SfUtil;
import com.tridium.opcUaServer.enums.BImportExport;
import com.tridium.opcUaServer.export.BIOpcExport;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.baja.control.BBooleanPoint;
import javax.baja.control.BBooleanWritable;
import javax.baja.control.BControlPoint;
import javax.baja.control.BEnumPoint;
import javax.baja.control.BEnumWritable;
import javax.baja.control.BNumericPoint;
import javax.baja.control.BNumericWritable;
import javax.baja.control.BStringPoint;
import javax.baja.control.BStringWritable;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.naming.BOrd;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BComponent;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BIcon;
import javax.baja.sys.Flags;
import javax.baja.sys.Property;
import javax.baja.sys.Slot;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "point",
      type = "String",
      defaultValue = "",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "direction",
      type = "BImportExport",
      defaultValue = "BImportExport.Export",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "pointType",
      type = "BTypeSpec",
      defaultValue = "BTypeSpec.DEFAULT",
      flags = 5,
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "linkSlot",
      type = "BDynamicEnum",
      defaultValue = "BDynamicEnum.make(0, BEnumRange.NULL)",
      flags = 4
   )})
public class BOpcUaServerLearnPointEntry extends BComponent implements BINDiscoveryLeaf, BINDiscoveryIcon {
   public static final Property point = newProperty(5, "", SfUtil.incl());
   public static final Property direction = newProperty(0, BImportExport.Export, SfUtil.incl());
   public static final Property pointType = newProperty(5, BTypeSpec.DEFAULT, SfUtil.incl());
   public static final Property linkSlot = newProperty(4, BDynamicEnum.make(0, BEnumRange.NULL), null);
   public static final Type TYPE = Sys.loadType(BOpcUaServerLearnPointEntry.class);
   private static String[] INPUT_SLOTS = new String[]{"in15", "in14", "in13", "in12", "in11", "in10", "in9", "in7", "in6", "in5", "in4", "in3", "in2"};
   Logger logger = Logger.getLogger(BOpcUaServerLearnPointEntry.class.getName());

   public String getPoint() {
      return this.getString(point);
   }

   public void setPoint(String v) {
      this.setString(point, v, null);
   }

   public BImportExport getDirection() {
      return (BImportExport)this.get(direction);
   }

   public void setDirection(BImportExport v) {
      this.set(direction, v, null);
   }

   public BTypeSpec getPointType() {
      return (BTypeSpec)this.get(pointType);
   }

   public void setPointType(BTypeSpec v) {
      this.set(pointType, v, null);
   }

   public BDynamicEnum getLinkSlot() {
      return (BDynamicEnum)this.get(linkSlot);
   }

   public void setLinkSlot(BDynamicEnum v) {
      this.set(linkSlot, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaServerLearnPointEntry() {
   }

   public BOpcUaServerLearnPointEntry(BControlPoint point, BImportExport direction) {
      this.setPoint(point.getSlotPath().toString());
      this.setPointType(point.getType().getTypeSpec());
      this.setDirection(direction);
      this.setLinkSlot(this.getLinkableSlots(point, direction));
   }

   public static BOpcUaServerLearnPointEntry make(BControlPoint point) {
      BOpcUaServerLearnPointEntry entry = new BOpcUaServerLearnPointEntry(point, BImportExport.Export);
      if (point.isWritablePoint()) {
         entry.add("d?", new BOpcUaServerLearnPointEntry(point, BImportExport.Import), 2);
      }

      return entry;
   }

   private BDynamicEnum getLinkableSlots(BControlPoint point, BImportExport direction) {
      BDynamicEnum inSelEnum = BDynamicEnum.DEFAULT;

      try {
         String[] inTags = this.getLinkableSlotsNames(point, direction);
         BEnumRange inRange = BEnumRange.make(inTags);
         return BDynamicEnum.make(0, inRange);
      } catch (Exception var6) {
         this.logger.severe("Exception while getting Linkable Slots: " + var6);
         return inSelEnum;
      }
   }

   private String[] getLinkableSlotsNames(BControlPoint point, BImportExport direction) {
      ArrayList<String> aList = new ArrayList<>();
      if (direction.equals(BImportExport.Export)) {
         aList.add("out");
      } else {
         for (String inputSlot : INPUT_SLOTS) {
            Slot slot = point.getSlot(inputSlot);
            if (slot == null) {
               break;
            }

            if (!Flags.isReadonly(point, slot) && (Flags.isFanIn(point, slot) || !point.isLinkTarget(slot))) {
               aList.add(inputSlot);
            }
         }
      }

      return aList.toArray(new String[0]);
   }

   public BIcon getDiscoveryIcon() {
      return this.getPointType().getInstance().getIcon();
   }

   public String getDiscoveryName() {
      String slotPath = this.getPoint();
      int c = slotPath.lastIndexOf("/");
      return slotPath.substring(c + 1);
   }

   public TypeInfo[] getValidDatabaseTypes() {
      boolean isExport = this.getDirection().equals(BImportExport.Export);
      BControlPoint instance = (BControlPoint)this.getPointType().getInstance();
      BStatusValue statusValue = instance.getOutStatusValue();
      if (statusValue instanceof BStatusBoolean) {
         return isExport ? new TypeInfo[]{BBooleanWritable.TYPE.getTypeInfo()} : new TypeInfo[]{BBooleanPoint.TYPE.getTypeInfo()};
      } else if (statusValue instanceof BStatusNumeric) {
         return isExport ? new TypeInfo[]{BNumericWritable.TYPE.getTypeInfo()} : new TypeInfo[]{BNumericPoint.TYPE.getTypeInfo()};
      } else if (statusValue instanceof BStatusEnum) {
         return isExport ? new TypeInfo[]{BEnumWritable.TYPE.getTypeInfo()} : new TypeInfo[]{BEnumPoint.TYPE.getTypeInfo()};
      } else if (statusValue instanceof BStatusString) {
         return isExport ? new TypeInfo[]{BStringWritable.TYPE.getTypeInfo()} : new TypeInfo[]{BStringPoint.TYPE.getTypeInfo()};
      } else {
         return new TypeInfo[0];
      }
   }

   public void updateTarget(BComponent target) {
      BControlPoint targetPoint = (BControlPoint)target;
      BAbstractProxyExt proxyExt = targetPoint.getProxyExt();
      if (proxyExt instanceof BOpcUaServerProxyExt) {
         BOpcUaServerProxyExt proxy = (BOpcUaServerProxyExt)proxyExt;
         BOrd bOrd = BOrd.make(this.getPoint());
         proxy.setLocalPoint(bOrd);
         proxy.setLocalPointSlot(this.getLinkSlot());
      }
   }

   public boolean isExisting(BComponent component) {
      if (!(component instanceof BControlPoint)) {
         return false;
      } else {
         BAbstractProxyExt proxyExt = ((BControlPoint)component).getProxyExt();
         if (!(proxyExt instanceof BOpcUaServerProxyExt)) {
            return false;
         } else {
            BOpcUaServerProxyExt opcProxyExt = (BOpcUaServerProxyExt)proxyExt;
            if (opcProxyExt.getLocalPoint().equals(BOrd.make(this.getPoint()))) {
               boolean moreChoices = opcProxyExt.getLocalPointSlot().getRange().getOrdinals().length > 1;
               if (component instanceof BIOpcExport && !moreChoices) {
                  return true;
               }
            }

            return false;
         }
      }
   }

   public void defaultTargetUpdate(BComponent target) {
   }
}
