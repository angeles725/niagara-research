package com.tridium.lonworks.discover;

import com.tridium.util.CompUtil;
import java.util.HashMap;
import javax.baja.data.BIDataValue;
import javax.baja.lonworks.BLonComponent;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.BLonObject;
import javax.baja.lonworks.BLonObjectFolder;
import javax.baja.lonworks.londata.BLonBigInteger;
import javax.baja.lonworks.londata.BLonBoolean;
import javax.baja.lonworks.londata.BLonByteArray;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonDouble;
import javax.baja.lonworks.londata.BLonEnum;
import javax.baja.lonworks.londata.BLonFloat;
import javax.baja.lonworks.londata.BLonInteger;
import javax.baja.lonworks.londata.BLonLong;
import javax.baja.lonworks.londata.BLonPrimitive;
import javax.baja.lonworks.londata.BLonSimple;
import javax.baja.lonworks.londata.BLonString;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComponent;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BIcon;
import javax.baja.sys.BInteger;
import javax.baja.sys.BNumber;
import javax.baja.sys.BObject;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.BUnit;
import javax.baja.util.BTypeSpec;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "targetName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "elementName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "pointType",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "units",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "targetType",
      type = "BTypeSpec",
      defaultValue = "BTypeSpec.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "isTargetComponentWritable",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "targetFacets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "defaultName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "targetPathName",
      type = "String",
      defaultValue = "",
      flags = 1
   )})
public class BLonCreationEntry extends BComponent {
   public static final Property targetName = newProperty(1, "", null);
   public static final Property elementName = newProperty(1, "", null);
   public static final Property pointType = newProperty(1, 0, null);
   public static final Property units = newProperty(1, "", null);
   public static final Property targetType = newProperty(1, BTypeSpec.DEFAULT, null);
   public static final Property isTargetComponentWritable = newProperty(1, false, null);
   public static final Property targetFacets = newProperty(1, BFacets.DEFAULT, null);
   public static final Property defaultName = newProperty(1, "", null);
   public static final Property targetPathName = newProperty(1, "", null);
   public static final Type TYPE = Sys.loadType(BLonCreationEntry.class);
   private BComponent target;
   private Property targetProp;
   private static BIcon FloatElementIcon = BIcon.make("module://icons/x16/statusNumeric.png");
   private static BIcon BooleanElementIcon = BIcon.make("module://icons/x16/statusBoolean.png");
   private static BIcon EnumElementIcon = BIcon.make("module://icons/x16/statusEnum.png");
   private static BIcon StringElementIcon = BIcon.make("module://icons/x16/statusString.png");
   private static BIcon LonObjectIcon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/object.png");
   private static BIcon LonObjectFolderIcon = BIcon.make("module://lonworks/com/tridium/lonworks/ui/icons/objectFolder.png");

   public String getTargetName() {
      return this.getString(targetName);
   }

   public void setTargetName(String v) {
      this.setString(targetName, v, null);
   }

   public String getElementName() {
      return this.getString(elementName);
   }

   public void setElementName(String v) {
      this.setString(elementName, v, null);
   }

   public int getPointType() {
      return this.getInt(pointType);
   }

   public void setPointType(int v) {
      this.setInt(pointType, v, null);
   }

   public String getUnits() {
      return this.getString(units);
   }

   public void setUnits(String v) {
      this.setString(units, v, null);
   }

   public BTypeSpec getTargetType() {
      return (BTypeSpec)this.get(targetType);
   }

   public void setTargetType(BTypeSpec v) {
      this.set(targetType, v, null);
   }

   public boolean getIsTargetComponentWritable() {
      return this.getBoolean(isTargetComponentWritable);
   }

   public void setIsTargetComponentWritable(boolean v) {
      this.setBoolean(isTargetComponentWritable, v, null);
   }

   public BFacets getTargetFacets() {
      return (BFacets)this.get(targetFacets);
   }

   public void setTargetFacets(BFacets v) {
      this.set(targetFacets, v, null);
   }

   public String getDefaultName() {
      return this.getString(defaultName);
   }

   public void setDefaultName(String v) {
      this.setString(defaultName, v, null);
   }

   public String getTargetPathName() {
      return this.getString(targetPathName);
   }

   public void setTargetPathName(String v) {
      this.setString(targetPathName, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BLonCreationEntry() {
   }

   public BLonCreationEntry(BComponent lo) {
      this.target = lo;
      this.setTargetName(lo.getName());
      BIcon targetIcon = this.getTargetIcon();
      if (targetIcon != null) {
         this.add("icon", targetIcon);
      }

      this.setTargetType(this.getTarget().getType().getTypeSpec());
      this.setTargetFacets(this.getFacets());
      this.setDefaultName(this.getDefaultProxyName());
      this.setTargetPathName(this.getDefaultTargetPathName());
      BLonComponent lonComponent = this.getLonComponent();
      if (lonComponent != null) {
         this.setIsTargetComponentWritable(lonComponent.isWriteable());
      }
   }

   public BLonCreationEntry(BLonData ldat, String elementName, String units) {
      this.target = ldat;
      this.targetProp = ldat.getProperty(elementName);
      String targetName = this.getPathToParentType(this.target, BLonComponent.TYPE, true, "/");
      this.setTargetName(SlotPath.unescape(targetName));
      this.setElementName(SlotPath.unescape(elementName));
      this.setUnits(units);
      BIcon targetIcon = this.getTargetIcon();
      if (targetIcon != null) {
         this.add("icon", targetIcon);
      }

      this.setTargetType(this.getTarget().getType().getTypeSpec());
      this.setTargetFacets(this.getFacets());
      this.setDefaultName(this.getDefaultProxyName());
      this.setTargetPathName(this.getDefaultTargetPathName());
      BLonComponent lonComponent = this.getLonComponent();
      if (lonComponent != null) {
         this.setIsTargetComponentWritable(lonComponent.isWriteable());
      }
   }

   private String getPathToParentType(BComponent tgt, Type baseTyp, boolean noDataInPath, String delimiter) {
      BComponent c = tgt;
      String path = "";
      boolean end = false;

      while (!end && !c.getType().is(BLonDevice.TYPE)) {
         if (!noDataInPath || !c.getName().equals("data") || !c.getParent().getType().is(BLonComponent.TYPE)) {
            if (path.length() == 0) {
               path = c.getName();
            } else {
               path = c.getName() + delimiter + path;
            }
         }

         if (c.getType().is(baseTyp)) {
            end = true;
         } else {
            c = (BComponent)c.getParent();
         }
      }

      return path;
   }

   public BObject getTarget() {
      if (this.target.getType().is(BLonObject.TYPE)) {
         return this.target;
      } else {
         return (BObject)(this.target.getType().is(BLonObjectFolder.TYPE) ? this.target : this.target.get(this.getElementName()));
      }
   }

   public BLonComponent getLonComponent() {
      return (BLonComponent)CompUtil.closestAncestor(this.target, BLonComponent.class).orElse(null);
   }

   public String getDefaultProxyName() {
      String n = this.getPathToParentType(this.target, BLonDevice.TYPE, true, "_");
      BComponent lonData = this.target;
      int cnt = 0;
      lonData = this.target;
      Property[] a = lonData.getPropertiesArray();

      for (int i = 0; i < a.length; i++) {
         if (BLonData.isDataProp(a[i])) {
            cnt++;
         }
      }

      if (cnt > 1) {
         n = n + "_" + this.getElementName();
      }

      return n;
   }

   private String getDefaultTargetPathName() {
      return this.getPathToParentType(this.target, BLonDevice.TYPE, false, "/");
   }

   public BFacets getFacets() {
      Type type = this.getTarget().getType();
      if (type == BLonFloat.TYPE || type == BLonInteger.TYPE || type == BLonDouble.TYPE || type == BLonLong.TYPE || type == BLonBigInteger.TYPE) {
         return this.getFloatFacets();
      } else {
         return type == BLonEnum.TYPE ? this.getEnumFacets() : BFacets.NULL;
      }
   }

   private BFacets getFloatFacets() {
      HashMap<String, BIDataValue> map = new HashMap<>();
      BFacets f = this.targetProp.getFacets();
      BObject s = f.getFacet("units");
      if (s != null) {
         map.put("units", (BUnit)s);
      }

      s = f.getFacet("precision");
      map.put("precision", s != null ? (BInteger)s : BInteger.make(2));
      s = f.getFacet("min");
      map.put("min", (BIDataValue)(s != null ? (BNumber)s : BFloat.NEGATIVE_INFINITY));
      s = f.getFacet("max");
      map.put("max", (BIDataValue)(s != null ? (BNumber)s : BFloat.POSITIVE_INFINITY));
      return BFacets.make(map);
   }

   private BFacets getEnumFacets() {
      BLonPrimitive p = (BLonPrimitive)this.target.get(this.targetProp);
      if (p.getType() == BLonEnum.TYPE) {
         BEnum e = ((BLonEnum)p).getEnum();
         return BFacets.makeEnum(e.getRange());
      } else {
         return BFacets.makeEnum(BEnumRange.DEFAULT);
      }
   }

   public void setChildren(BLonCreationEntry[] children) {
      for (BLonCreationEntry child : children) {
         this.add(null, child);
      }
   }

   private BIcon getTargetIcon() {
      if (this.target != null) {
         Type type = this.getTarget().getType();
         if (type.is(BLonObject.TYPE)) {
            return LonObjectIcon;
         }

         if (type.is(BLonObjectFolder.TYPE)) {
            return LonObjectFolderIcon;
         }

         if (type == BLonFloat.TYPE || type == BLonInteger.TYPE || type == BLonDouble.TYPE || type == BLonLong.TYPE || type == BLonBigInteger.TYPE) {
            return FloatElementIcon;
         }

         if (type == BLonBoolean.TYPE) {
            return BooleanElementIcon;
         }

         if (type == BLonEnum.TYPE) {
            return EnumElementIcon;
         }

         if (type == BLonString.TYPE || type == BLonByteArray.TYPE || type == BLonSimple.TYPE) {
            return StringElementIcon;
         }
      }

      return null;
   }
}
