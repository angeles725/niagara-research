package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonDeviceSelectEnum;
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

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "deviceSelect",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonDeviceSelectEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlServiceRequired",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlBearingsChange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlBearingsLubricate",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlShaftsealChange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved147",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 1, 0, 4, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved207",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 8, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved307",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null)")}
   ), @NiagaraProperty(
      name = "valvePosMotorMaint",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosPackingChange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosElectronicsCheck",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosPositioningCheck",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosLubricationCheck",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReturnCheck",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosBatteryCheck",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved17",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved207",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 8, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved306",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 1, 7, null)")}
   ), @NiagaraProperty(
      name = "valvePosGeneralMaint",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 0, 1, null)")}
   )})
public class BLonDevMaint extends BLonData {
   public static final Property deviceSelect = newProperty(0, BLonEnum.make(BLonDeviceSelectEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property pumpCtrlServiceRequired = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property pumpCtrlBearingsChange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null));
   public static final Property pumpCtrlBearingsLubricate = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null));
   public static final Property pumpCtrlShaftsealChange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property pumpCtrlReserved147 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 1, 0, 4, null));
   public static final Property pumpCtrlReserved207 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 8, null));
   public static final Property pumpCtrlReserved307 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null));
   public static final Property valvePosMotorMaint = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property valvePosPackingChange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null));
   public static final Property valvePosElectronicsCheck = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null));
   public static final Property valvePosPositioningCheck = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property valvePosLubricationCheck = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null));
   public static final Property valvePosReturnCheck = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null));
   public static final Property valvePosBatteryCheck = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null));
   public static final Property valvePosReserved17 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null));
   public static final Property valvePosReserved207 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 8, null));
   public static final Property valvePosReserved306 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 1, 7, null));
   public static final Property valvePosGeneralMaint = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 0, 1, null));
   public static final Type TYPE = Sys.loadType(BLonDevMaint.class);

   public BLonEnum getDeviceSelect() {
      return (BLonEnum)this.get(deviceSelect);
   }

   public void setDeviceSelect(BLonEnum v) {
      this.set(deviceSelect, v, null);
   }

   public BLonBoolean getPumpCtrlServiceRequired() {
      return (BLonBoolean)this.get(pumpCtrlServiceRequired);
   }

   public void setPumpCtrlServiceRequired(BLonBoolean v) {
      this.set(pumpCtrlServiceRequired, v, null);
   }

   public BLonBoolean getPumpCtrlBearingsChange() {
      return (BLonBoolean)this.get(pumpCtrlBearingsChange);
   }

   public void setPumpCtrlBearingsChange(BLonBoolean v) {
      this.set(pumpCtrlBearingsChange, v, null);
   }

   public BLonBoolean getPumpCtrlBearingsLubricate() {
      return (BLonBoolean)this.get(pumpCtrlBearingsLubricate);
   }

   public void setPumpCtrlBearingsLubricate(BLonBoolean v) {
      this.set(pumpCtrlBearingsLubricate, v, null);
   }

   public BLonBoolean getPumpCtrlShaftsealChange() {
      return (BLonBoolean)this.get(pumpCtrlShaftsealChange);
   }

   public void setPumpCtrlShaftsealChange(BLonBoolean v) {
      this.set(pumpCtrlShaftsealChange, v, null);
   }

   public BLonFloat getPumpCtrlReserved147() {
      return (BLonFloat)this.get(pumpCtrlReserved147);
   }

   public void setPumpCtrlReserved147(BLonFloat v) {
      this.set(pumpCtrlReserved147, v, null);
   }

   public BLonFloat getPumpCtrlReserved207() {
      return (BLonFloat)this.get(pumpCtrlReserved207);
   }

   public void setPumpCtrlReserved207(BLonFloat v) {
      this.set(pumpCtrlReserved207, v, null);
   }

   public BLonFloat getPumpCtrlReserved307() {
      return (BLonFloat)this.get(pumpCtrlReserved307);
   }

   public void setPumpCtrlReserved307(BLonFloat v) {
      this.set(pumpCtrlReserved307, v, null);
   }

   public BLonBoolean getValvePosMotorMaint() {
      return (BLonBoolean)this.get(valvePosMotorMaint);
   }

   public void setValvePosMotorMaint(BLonBoolean v) {
      this.set(valvePosMotorMaint, v, null);
   }

   public BLonBoolean getValvePosPackingChange() {
      return (BLonBoolean)this.get(valvePosPackingChange);
   }

   public void setValvePosPackingChange(BLonBoolean v) {
      this.set(valvePosPackingChange, v, null);
   }

   public BLonBoolean getValvePosElectronicsCheck() {
      return (BLonBoolean)this.get(valvePosElectronicsCheck);
   }

   public void setValvePosElectronicsCheck(BLonBoolean v) {
      this.set(valvePosElectronicsCheck, v, null);
   }

   public BLonBoolean getValvePosPositioningCheck() {
      return (BLonBoolean)this.get(valvePosPositioningCheck);
   }

   public void setValvePosPositioningCheck(BLonBoolean v) {
      this.set(valvePosPositioningCheck, v, null);
   }

   public BLonBoolean getValvePosLubricationCheck() {
      return (BLonBoolean)this.get(valvePosLubricationCheck);
   }

   public void setValvePosLubricationCheck(BLonBoolean v) {
      this.set(valvePosLubricationCheck, v, null);
   }

   public BLonBoolean getValvePosReturnCheck() {
      return (BLonBoolean)this.get(valvePosReturnCheck);
   }

   public void setValvePosReturnCheck(BLonBoolean v) {
      this.set(valvePosReturnCheck, v, null);
   }

   public BLonBoolean getValvePosBatteryCheck() {
      return (BLonBoolean)this.get(valvePosBatteryCheck);
   }

   public void setValvePosBatteryCheck(BLonBoolean v) {
      this.set(valvePosBatteryCheck, v, null);
   }

   public BLonBoolean getValvePosReserved17() {
      return (BLonBoolean)this.get(valvePosReserved17);
   }

   public void setValvePosReserved17(BLonBoolean v) {
      this.set(valvePosReserved17, v, null);
   }

   public BLonFloat getValvePosReserved207() {
      return (BLonFloat)this.get(valvePosReserved207);
   }

   public void setValvePosReserved207(BLonFloat v) {
      this.set(valvePosReserved207, v, null);
   }

   public BLonFloat getValvePosReserved306() {
      return (BLonFloat)this.get(valvePosReserved306);
   }

   public void setValvePosReserved306(BLonFloat v) {
      this.set(valvePosReserved306, v, null);
   }

   public BLonBoolean getValvePosGeneralMaint() {
      return (BLonBoolean)this.get(valvePosGeneralMaint);
   }

   public void setValvePosGeneralMaint(BLonBoolean v) {
      this.set(valvePosGeneralMaint, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(deviceSelect, out);
      int f = this.getDeviceSelect().getEnum().getOrdinal();
      if (f == 0) {
         this.primitiveToOutputStream(pumpCtrlServiceRequired, out);
         this.primitiveToOutputStream(pumpCtrlBearingsChange, out);
         this.primitiveToOutputStream(pumpCtrlBearingsLubricate, out);
         this.primitiveToOutputStream(pumpCtrlShaftsealChange, out);
         this.primitiveToOutputStream(pumpCtrlReserved147, out);
         this.primitiveToOutputStream(pumpCtrlReserved207, out);
         this.primitiveToOutputStream(pumpCtrlReserved307, out);
      } else {
         this.primitiveToOutputStream(valvePosMotorMaint, out);
         this.primitiveToOutputStream(valvePosPackingChange, out);
         this.primitiveToOutputStream(valvePosElectronicsCheck, out);
         this.primitiveToOutputStream(valvePosPositioningCheck, out);
         this.primitiveToOutputStream(valvePosLubricationCheck, out);
         this.primitiveToOutputStream(valvePosReturnCheck, out);
         this.primitiveToOutputStream(valvePosBatteryCheck, out);
         this.primitiveToOutputStream(valvePosReserved17, out);
         this.primitiveToOutputStream(valvePosReserved207, out);
         this.primitiveToOutputStream(valvePosReserved306, out);
         this.primitiveToOutputStream(valvePosGeneralMaint, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(deviceSelect, in);
      int f = this.getDeviceSelect().getEnum().getOrdinal();
      if (f == 0) {
         this.primitiveFromInputStream(pumpCtrlServiceRequired, in);
         this.primitiveFromInputStream(pumpCtrlBearingsChange, in);
         this.primitiveFromInputStream(pumpCtrlBearingsLubricate, in);
         this.primitiveFromInputStream(pumpCtrlShaftsealChange, in);
         this.primitiveFromInputStream(pumpCtrlReserved147, in);
         this.primitiveFromInputStream(pumpCtrlReserved207, in);
         this.primitiveFromInputStream(pumpCtrlReserved307, in);
      } else {
         this.primitiveFromInputStream(valvePosMotorMaint, in);
         this.primitiveFromInputStream(valvePosPackingChange, in);
         this.primitiveFromInputStream(valvePosElectronicsCheck, in);
         this.primitiveFromInputStream(valvePosPositioningCheck, in);
         this.primitiveFromInputStream(valvePosLubricationCheck, in);
         this.primitiveFromInputStream(valvePosReturnCheck, in);
         this.primitiveFromInputStream(valvePosBatteryCheck, in);
         this.primitiveFromInputStream(valvePosReserved17, in);
         this.primitiveFromInputStream(valvePosReserved207, in);
         this.primitiveFromInputStream(valvePosReserved306, in);
         this.primitiveFromInputStream(valvePosGeneralMaint, in);
      }
   }
}
