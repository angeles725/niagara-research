package com.tridium.lonworks.datatypes;

import javax.baja.lonworks.datatypes.BProgramId;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.enums.BLonNvTypeCategoryEnum;
import javax.baja.lonworks.londata.BLonData;
import javax.baja.lonworks.londata.BLonElementQualifiers;
import javax.baja.lonworks.londata.LonFacetsUtil;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "nvIndex",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "nvTypeScpt",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "typeProgramId",
      type = "BProgramId",
      defaultValue = "BProgramId.DEFAULT"
   ), @NiagaraProperty(
      name = "typeScope",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "typeIndex",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "typeCategory",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "typeLength",
      type = "int",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "scalingFactorA",
      type = "int",
      defaultValue = "1"
   ), @NiagaraProperty(
      name = "scalingFactorB",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "scalingFactorC",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "update",
      type = "boolean",
      defaultValue = "false"
   )})
public class BChangeNvTypeParameter extends BStruct {
   public static final Property nvIndex = newProperty(0, "", null);
   public static final Property nvTypeScpt = newProperty(0, "", null);
   public static final Property typeProgramId = newProperty(0, BProgramId.DEFAULT, null);
   public static final Property typeScope = newProperty(0, -1, null);
   public static final Property typeIndex = newProperty(0, -1, null);
   public static final Property typeCategory = newProperty(0, -1, null);
   public static final Property typeLength = newProperty(0, -1, null);
   public static final Property scalingFactorA = newProperty(0, 1, null);
   public static final Property scalingFactorB = newProperty(0, 0, null);
   public static final Property scalingFactorC = newProperty(0, 0, null);
   public static final Property update = newProperty(0, false, null);
   public static final Type TYPE = Sys.loadType(BChangeNvTypeParameter.class);

   public String getNvIndex() {
      return this.getString(nvIndex);
   }

   public void setNvIndex(String v) {
      this.setString(nvIndex, v, null);
   }

   public String getNvTypeScpt() {
      return this.getString(nvTypeScpt);
   }

   public void setNvTypeScpt(String v) {
      this.setString(nvTypeScpt, v, null);
   }

   public BProgramId getTypeProgramId() {
      return (BProgramId)this.get(typeProgramId);
   }

   public void setTypeProgramId(BProgramId v) {
      this.set(typeProgramId, v, null);
   }

   public int getTypeScope() {
      return this.getInt(typeScope);
   }

   public void setTypeScope(int v) {
      this.setInt(typeScope, v, null);
   }

   public int getTypeIndex() {
      return this.getInt(typeIndex);
   }

   public void setTypeIndex(int v) {
      this.setInt(typeIndex, v, null);
   }

   public int getTypeCategory() {
      return this.getInt(typeCategory);
   }

   public void setTypeCategory(int v) {
      this.setInt(typeCategory, v, null);
   }

   public int getTypeLength() {
      return this.getInt(typeLength);
   }

   public void setTypeLength(int v) {
      this.setInt(typeLength, v, null);
   }

   public int getScalingFactorA() {
      return this.getInt(scalingFactorA);
   }

   public void setScalingFactorA(int v) {
      this.setInt(scalingFactorA, v, null);
   }

   public int getScalingFactorB() {
      return this.getInt(scalingFactorB);
   }

   public void setScalingFactorB(int v) {
      this.setInt(scalingFactorB, v, null);
   }

   public int getScalingFactorC() {
      return this.getInt(scalingFactorC);
   }

   public void setScalingFactorC(int v) {
      this.setInt(scalingFactorC, v, null);
   }

   public boolean getUpdate() {
      return this.getBoolean(update);
   }

   public void setUpdate(boolean v) {
      this.setBoolean(update, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void updateNvTypePara(BLonData dat) {
      int a = 1;
      int b = 0;
      int c = 0;
      BLonNvTypeCategoryEnum e = BLonNvTypeCategoryEnum.nvtCatNul;
      Property[] datProps = dat.getPropertiesArray();
      if (!datProps[0].getType().is(BLonData.TYPE) && datProps.length <= 1) {
         BFacets f = datProps[0].getFacets();
         BLonElementQualifiers eq = LonFacetsUtil.getQualifiers(f);
         BLonElementType type = eq.getElemtype();
         switch (type.getOrdinal()) {
            case 0:
            case 2:
               e = BLonNvTypeCategoryEnum.nvtCatUnsignedChar;
               break;
            case 1:
               e = BLonNvTypeCategoryEnum.nvtCatSignedChar;
               break;
            case 3:
               e = BLonNvTypeCategoryEnum.nvtCatSignedLong;
               break;
            case 4:
               e = BLonNvTypeCategoryEnum.nvtCatUnsignedLong;
               break;
            case 5:
               e = BLonNvTypeCategoryEnum.nvtCatSignedQuad;
               break;
            case 6:
            case 7:
               e = BLonNvTypeCategoryEnum.nvtCatEnum;
               break;
            case 8:
               e = BLonNvTypeCategoryEnum.nvtCatFloat;
               break;
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
               e = BLonNvTypeCategoryEnum.nvtCatBitfield;
               break;
            case 14:
            case 15:
               e = BLonNvTypeCategoryEnum.nvtCatArray;
         }

         if (eq.getResolution() != 1.0F) {
            double logRes = Math.log(eq.getResolution()) / Math.log(10.0);
            b = (int)Math.floor(logRes);
            a = (int)Math.rint(Math.pow(10.0, logRes - b));
         }

         if (eq.hasOffset()) {
            c = (int)eq.getOffset();
         }
      } else {
         e = BLonNvTypeCategoryEnum.nvtCatStruct;
      }

      this.setTypeCategory(e.getOrdinal());
      this.setTypeLength(dat.getByteLength());
      this.setScalingFactorA(a);
      this.setScalingFactorB(b);
      this.setScalingFactorC(c);
   }

   public String toDebugString() {
      return "nvIndex="
         + this.getNvIndex()
         + ", nvTypeScpt="
         + this.getNvTypeScpt()
         + ", typeProgramId="
         + this.getTypeProgramId()
         + ", typeScope="
         + this.getTypeScope()
         + ", typeIndex="
         + this.getTypeIndex()
         + ", typeCategory="
         + this.getTypeCategory()
         + ", typeLength="
         + this.getTypeLength()
         + ", scalingFactorA="
         + this.getScalingFactorA()
         + ", scalingFactorB="
         + this.getScalingFactorB()
         + ", scalingFactorC="
         + this.getScalingFactorC();
   }
}
