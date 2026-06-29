package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonCamActEnum;
import javax.baja.lonworks.enums.BLonCamFuncEnum;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.units.UnitDatabase;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "receiverId",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, null)")}
   ), @NiagaraProperty(
      name = "controllerId",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, null)")}
   ), @NiagaraProperty(
      name = "controllerPrio",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 100, 1, null)")}
   ), @NiagaraProperty(
      name = "camFunction",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonCamFuncEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "camAction",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonCamActEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "valueNumber",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 7)")}
   ), @NiagaraProperty(
      name = "valueAbsposPan",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.s16, -359.97998F, 360.0F, 0.02F, 32767.0F, 7, 0, UnitDatabase.getUnit(\"degrees angular\"))")}
   ), @NiagaraProperty(
      name = "valueAbsposTilt",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.s16, -359.97998F, 360.0F, 0.02F, 32767.0F, 9, 0, UnitDatabase.getUnit(\"degrees angular\"))")}
   ), @NiagaraProperty(
      name = "valueAbsposZoom",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.s16, -163.84F, 163.83F, 0.0050F, 32767.0F, 11, 0, UnitDatabase.getUnit(\"percent\"))")}
   )})
public class BLonPosCtrl extends BLonData {
   public static final Property receiverId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, null));
   public static final Property controllerId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, null));
   public static final Property controllerPrio = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 100, 1, null));
   public static final Property camFunction = newProperty(0, BLonEnum.make(BLonCamFuncEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property camAction = newProperty(0, BLonEnum.make(BLonCamActEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property valueNumber = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 7));
   public static final Property valueAbsposPan = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.s16, -359.97998F, 360.0F, 0.02F, 32767.0F, 7, 0, UnitDatabase.getUnit("degrees angular"))
   );
   public static final Property valueAbsposTilt = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.s16, -359.97998F, 360.0F, 0.02F, 32767.0F, 9, 0, UnitDatabase.getUnit("degrees angular"))
   );
   public static final Property valueAbsposZoom = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.s16, -163.84F, 163.83F, 0.005F, 32767.0F, 11, 0, UnitDatabase.getUnit("percent"))
   );
   public static final Type TYPE = Sys.loadType(BLonPosCtrl.class);

   public BLonFloat getReceiverId() {
      return (BLonFloat)this.get(receiverId);
   }

   public void setReceiverId(BLonFloat v) {
      this.set(receiverId, v, null);
   }

   public BLonFloat getControllerId() {
      return (BLonFloat)this.get(controllerId);
   }

   public void setControllerId(BLonFloat v) {
      this.set(controllerId, v, null);
   }

   public BLonFloat getControllerPrio() {
      return (BLonFloat)this.get(controllerPrio);
   }

   public void setControllerPrio(BLonFloat v) {
      this.set(controllerPrio, v, null);
   }

   public BLonEnum getCamFunction() {
      return (BLonEnum)this.get(camFunction);
   }

   public void setCamFunction(BLonEnum v) {
      this.set(camFunction, v, null);
   }

   public BLonEnum getCamAction() {
      return (BLonEnum)this.get(camAction);
   }

   public void setCamAction(BLonEnum v) {
      this.set(camAction, v, null);
   }

   public BLonFloat getValueNumber() {
      return (BLonFloat)this.get(valueNumber);
   }

   public void setValueNumber(BLonFloat v) {
      this.set(valueNumber, v, null);
   }

   public BLonFloat getValueAbsposPan() {
      return (BLonFloat)this.get(valueAbsposPan);
   }

   public void setValueAbsposPan(BLonFloat v) {
      this.set(valueAbsposPan, v, null);
   }

   public BLonFloat getValueAbsposTilt() {
      return (BLonFloat)this.get(valueAbsposTilt);
   }

   public void setValueAbsposTilt(BLonFloat v) {
      this.set(valueAbsposTilt, v, null);
   }

   public BLonFloat getValueAbsposZoom() {
      return (BLonFloat)this.get(valueAbsposZoom);
   }

   public void setValueAbsposZoom(BLonFloat v) {
      this.set(valueAbsposZoom, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(receiverId, out);
      this.primitiveToOutputStream(controllerId, out);
      this.primitiveToOutputStream(controllerPrio, out);
      this.primitiveToOutputStream(camFunction, out);
      this.primitiveToOutputStream(camAction, out);
      int f = this.getCamFunction().getEnum().getOrdinal();
      if (f == 2) {
         this.primitiveToOutputStream(valueAbsposPan, out);
         this.primitiveToOutputStream(valueAbsposTilt, out);
         this.primitiveToOutputStream(valueAbsposZoom, out);
      } else {
         this.primitiveToOutputStream(valueNumber, out);
         out.writeUnsigned8(0);
         out.writeUnsigned16(0);
         out.writeUnsigned16(0);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(receiverId, in);
      this.primitiveFromInputStream(controllerId, in);
      this.primitiveFromInputStream(controllerPrio, in);
      this.primitiveFromInputStream(camFunction, in);
      this.primitiveFromInputStream(camAction, in);
      int f = this.getCamFunction().getEnum().getOrdinal();
      if (f == 2) {
         this.primitiveFromInputStream(valueAbsposPan, in);
         this.primitiveFromInputStream(valueAbsposTilt, in);
         this.primitiveFromInputStream(valueAbsposZoom, in);
      } else {
         this.primitiveFromInputStream(valueNumber, in);
      }
   }
}
