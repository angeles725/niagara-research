package com.tridium.bacnet.datatypes;

import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.Lexicon;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "use",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(0, BEnumRange.make(new int[] { USE_OBJECT_ID, USE_OBJECT_NAME }, useChoices))"
   ), @NiagaraProperty(
      name = "objectName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.DEFAULT"
   )})
public class BWhoHasConfig extends BDeviceDiscoveryConfig {
   private static Lexicon lex = Lexicon.make("bacnet");
   public static final int USE_OBJECT_ID = 0;
   public static final int USE_OBJECT_NAME = 1;
   private static String[] useChoices = new String[]{lex.getText("whoHas.useObjectId"), lex.getText("whoHas.useObjectName")};
   public static final Property use = newProperty(0, BDynamicEnum.make(0, BEnumRange.make(new int[]{0, 1}, useChoices)), null);
   public static final Property objectName = newProperty(0, "", null);
   public static final Property objectId = newProperty(0, BBacnetObjectIdentifier.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BWhoHasConfig.class);

   public BEnum getUse() {
      return (BEnum)this.get(use);
   }

   public void setUse(BEnum v) {
      this.set(use, v, null);
   }

   public String getObjectName() {
      return this.getString(objectName);
   }

   public void setObjectName(String v) {
      this.setString(objectName, v, null);
   }

   public BBacnetObjectIdentifier getObjectId() {
      return (BBacnetObjectIdentifier)this.get(objectId);
   }

   public void setObjectId(BBacnetObjectIdentifier v) {
      this.set(objectId, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   public String toString(Context cx) {
      StringBuilder sb = new StringBuilder();
      sb.append("Who-Has: ");
      if (this.getUse().getOrdinal() == 0) {
         sb.append(" Object ID " + this.getObjectId());
      } else {
         sb.append(" Object Name ").append(this.getObjectName());
      }

      return sb.toString();
   }
}
