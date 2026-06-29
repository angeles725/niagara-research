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
      name = "pumpCtrlSfVoltageLow",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfVoltageHigh",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfPhase",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfNoFluid",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfPressLow",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfPressHigh",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfReserved16",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSfReserved17",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfMotorTemp",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfMotorFailure",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfPumpBlocked",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfElectTemp",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfElectFailureNf",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfElectFailure",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfSensorFailure",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlDfReserved27",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved307",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfValveBlocked",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfBlockedDirectionOpen",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfBlockedDirectionClose",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfPositionError",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfStrokeOutOfRange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfInitialization",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfVibrationCavitation",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosDfEdTooHigh",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved102",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 1, 1F, 0F, true, 2, 5, false, 0F, 3, null )")}
   ), @NiagaraProperty(
      name = "valvePosEeOscillating",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosEeValveTooLarge",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosEeValveTooSmall",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved267",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 1, 1F, 0F, true, 2, 0, false, 0F, 2, null )")}
   ), @NiagaraProperty(
      name = "valvePosReserved307",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosSfVoltageOutOfRange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosSfElectronicHighTemp",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosSfFrictionalResistance",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved446",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 1, 1F, 0F, true, 3, 1, false, 0F,3, null )")}
   ), @NiagaraProperty(
      name = "valvePosGeneralFault",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 0, 1, null)")}
   )})
public class BLonDevFault extends BLonData {
   public static final Property deviceSelect = newProperty(0, BLonEnum.make(BLonDeviceSelectEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property pumpCtrlSfVoltageLow = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property pumpCtrlSfVoltageHigh = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null));
   public static final Property pumpCtrlSfPhase = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null));
   public static final Property pumpCtrlSfNoFluid = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property pumpCtrlSfPressLow = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null));
   public static final Property pumpCtrlSfPressHigh = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null));
   public static final Property pumpCtrlSfReserved16 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null));
   public static final Property pumpCtrlSfReserved17 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null));
   public static final Property pumpCtrlDfMotorTemp = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null));
   public static final Property pumpCtrlDfMotorFailure = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null));
   public static final Property pumpCtrlDfPumpBlocked = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 5, 1, null));
   public static final Property pumpCtrlDfElectTemp = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null));
   public static final Property pumpCtrlDfElectFailureNf = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null));
   public static final Property pumpCtrlDfElectFailure = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null));
   public static final Property pumpCtrlDfSensorFailure = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 1, 1, null));
   public static final Property pumpCtrlDfReserved27 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 0, 1, null));
   public static final Property pumpCtrlReserved307 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null));
   public static final Property valvePosDfValveBlocked = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property valvePosDfBlockedDirectionOpen = newProperty(
      0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)
   );
   public static final Property valvePosDfBlockedDirectionClose = newProperty(
      0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)
   );
   public static final Property valvePosDfPositionError = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property valvePosDfStrokeOutOfRange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null));
   public static final Property valvePosDfInitialization = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null));
   public static final Property valvePosDfVibrationCavitation = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null));
   public static final Property valvePosDfEdTooHigh = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null));
   public static final Property valvePosReserved102 = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 1.0F, 1.0F, 0.0F, true, 2, 5, false, 0.0F, 3, null)
   );
   public static final Property valvePosEeOscillating = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null));
   public static final Property valvePosEeValveTooLarge = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null));
   public static final Property valvePosEeValveTooSmall = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null));
   public static final Property valvePosReserved267 = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 1.0F, 1.0F, 0.0F, true, 2, 0, false, 0.0F, 2, null)
   );
   public static final Property valvePosReserved307 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 7, 1, null));
   public static final Property valvePosSfVoltageOutOfRange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 6, 1, null));
   public static final Property valvePosSfElectronicHighTemp = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 5, 1, null));
   public static final Property valvePosSfFrictionalResistance = newProperty(
      0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 4, 1, null)
   );
   public static final Property valvePosReserved446 = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 1.0F, 1.0F, 0.0F, true, 3, 1, false, 0.0F, 3, null)
   );
   public static final Property valvePosGeneralFault = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 3, 0, 1, null));
   public static final Type TYPE = Sys.loadType(BLonDevFault.class);

   public BLonEnum getDeviceSelect() {
      return (BLonEnum)this.get(deviceSelect);
   }

   public void setDeviceSelect(BLonEnum v) {
      this.set(deviceSelect, v, null);
   }

   public BLonBoolean getPumpCtrlSfVoltageLow() {
      return (BLonBoolean)this.get(pumpCtrlSfVoltageLow);
   }

   public void setPumpCtrlSfVoltageLow(BLonBoolean v) {
      this.set(pumpCtrlSfVoltageLow, v, null);
   }

   public BLonBoolean getPumpCtrlSfVoltageHigh() {
      return (BLonBoolean)this.get(pumpCtrlSfVoltageHigh);
   }

   public void setPumpCtrlSfVoltageHigh(BLonBoolean v) {
      this.set(pumpCtrlSfVoltageHigh, v, null);
   }

   public BLonBoolean getPumpCtrlSfPhase() {
      return (BLonBoolean)this.get(pumpCtrlSfPhase);
   }

   public void setPumpCtrlSfPhase(BLonBoolean v) {
      this.set(pumpCtrlSfPhase, v, null);
   }

   public BLonBoolean getPumpCtrlSfNoFluid() {
      return (BLonBoolean)this.get(pumpCtrlSfNoFluid);
   }

   public void setPumpCtrlSfNoFluid(BLonBoolean v) {
      this.set(pumpCtrlSfNoFluid, v, null);
   }

   public BLonBoolean getPumpCtrlSfPressLow() {
      return (BLonBoolean)this.get(pumpCtrlSfPressLow);
   }

   public void setPumpCtrlSfPressLow(BLonBoolean v) {
      this.set(pumpCtrlSfPressLow, v, null);
   }

   public BLonBoolean getPumpCtrlSfPressHigh() {
      return (BLonBoolean)this.get(pumpCtrlSfPressHigh);
   }

   public void setPumpCtrlSfPressHigh(BLonBoolean v) {
      this.set(pumpCtrlSfPressHigh, v, null);
   }

   public BLonBoolean getPumpCtrlSfReserved16() {
      return (BLonBoolean)this.get(pumpCtrlSfReserved16);
   }

   public void setPumpCtrlSfReserved16(BLonBoolean v) {
      this.set(pumpCtrlSfReserved16, v, null);
   }

   public BLonBoolean getPumpCtrlSfReserved17() {
      return (BLonBoolean)this.get(pumpCtrlSfReserved17);
   }

   public void setPumpCtrlSfReserved17(BLonBoolean v) {
      this.set(pumpCtrlSfReserved17, v, null);
   }

   public BLonBoolean getPumpCtrlDfMotorTemp() {
      return (BLonBoolean)this.get(pumpCtrlDfMotorTemp);
   }

   public void setPumpCtrlDfMotorTemp(BLonBoolean v) {
      this.set(pumpCtrlDfMotorTemp, v, null);
   }

   public BLonBoolean getPumpCtrlDfMotorFailure() {
      return (BLonBoolean)this.get(pumpCtrlDfMotorFailure);
   }

   public void setPumpCtrlDfMotorFailure(BLonBoolean v) {
      this.set(pumpCtrlDfMotorFailure, v, null);
   }

   public BLonBoolean getPumpCtrlDfPumpBlocked() {
      return (BLonBoolean)this.get(pumpCtrlDfPumpBlocked);
   }

   public void setPumpCtrlDfPumpBlocked(BLonBoolean v) {
      this.set(pumpCtrlDfPumpBlocked, v, null);
   }

   public BLonBoolean getPumpCtrlDfElectTemp() {
      return (BLonBoolean)this.get(pumpCtrlDfElectTemp);
   }

   public void setPumpCtrlDfElectTemp(BLonBoolean v) {
      this.set(pumpCtrlDfElectTemp, v, null);
   }

   public BLonBoolean getPumpCtrlDfElectFailureNf() {
      return (BLonBoolean)this.get(pumpCtrlDfElectFailureNf);
   }

   public void setPumpCtrlDfElectFailureNf(BLonBoolean v) {
      this.set(pumpCtrlDfElectFailureNf, v, null);
   }

   public BLonBoolean getPumpCtrlDfElectFailure() {
      return (BLonBoolean)this.get(pumpCtrlDfElectFailure);
   }

   public void setPumpCtrlDfElectFailure(BLonBoolean v) {
      this.set(pumpCtrlDfElectFailure, v, null);
   }

   public BLonBoolean getPumpCtrlDfSensorFailure() {
      return (BLonBoolean)this.get(pumpCtrlDfSensorFailure);
   }

   public void setPumpCtrlDfSensorFailure(BLonBoolean v) {
      this.set(pumpCtrlDfSensorFailure, v, null);
   }

   public BLonBoolean getPumpCtrlDfReserved27() {
      return (BLonBoolean)this.get(pumpCtrlDfReserved27);
   }

   public void setPumpCtrlDfReserved27(BLonBoolean v) {
      this.set(pumpCtrlDfReserved27, v, null);
   }

   public BLonFloat getPumpCtrlReserved307() {
      return (BLonFloat)this.get(pumpCtrlReserved307);
   }

   public void setPumpCtrlReserved307(BLonFloat v) {
      this.set(pumpCtrlReserved307, v, null);
   }

   public BLonBoolean getValvePosDfValveBlocked() {
      return (BLonBoolean)this.get(valvePosDfValveBlocked);
   }

   public void setValvePosDfValveBlocked(BLonBoolean v) {
      this.set(valvePosDfValveBlocked, v, null);
   }

   public BLonBoolean getValvePosDfBlockedDirectionOpen() {
      return (BLonBoolean)this.get(valvePosDfBlockedDirectionOpen);
   }

   public void setValvePosDfBlockedDirectionOpen(BLonBoolean v) {
      this.set(valvePosDfBlockedDirectionOpen, v, null);
   }

   public BLonBoolean getValvePosDfBlockedDirectionClose() {
      return (BLonBoolean)this.get(valvePosDfBlockedDirectionClose);
   }

   public void setValvePosDfBlockedDirectionClose(BLonBoolean v) {
      this.set(valvePosDfBlockedDirectionClose, v, null);
   }

   public BLonBoolean getValvePosDfPositionError() {
      return (BLonBoolean)this.get(valvePosDfPositionError);
   }

   public void setValvePosDfPositionError(BLonBoolean v) {
      this.set(valvePosDfPositionError, v, null);
   }

   public BLonBoolean getValvePosDfStrokeOutOfRange() {
      return (BLonBoolean)this.get(valvePosDfStrokeOutOfRange);
   }

   public void setValvePosDfStrokeOutOfRange(BLonBoolean v) {
      this.set(valvePosDfStrokeOutOfRange, v, null);
   }

   public BLonBoolean getValvePosDfInitialization() {
      return (BLonBoolean)this.get(valvePosDfInitialization);
   }

   public void setValvePosDfInitialization(BLonBoolean v) {
      this.set(valvePosDfInitialization, v, null);
   }

   public BLonBoolean getValvePosDfVibrationCavitation() {
      return (BLonBoolean)this.get(valvePosDfVibrationCavitation);
   }

   public void setValvePosDfVibrationCavitation(BLonBoolean v) {
      this.set(valvePosDfVibrationCavitation, v, null);
   }

   public BLonBoolean getValvePosDfEdTooHigh() {
      return (BLonBoolean)this.get(valvePosDfEdTooHigh);
   }

   public void setValvePosDfEdTooHigh(BLonBoolean v) {
      this.set(valvePosDfEdTooHigh, v, null);
   }

   public BLonFloat getValvePosReserved102() {
      return (BLonFloat)this.get(valvePosReserved102);
   }

   public void setValvePosReserved102(BLonFloat v) {
      this.set(valvePosReserved102, v, null);
   }

   public BLonBoolean getValvePosEeOscillating() {
      return (BLonBoolean)this.get(valvePosEeOscillating);
   }

   public void setValvePosEeOscillating(BLonBoolean v) {
      this.set(valvePosEeOscillating, v, null);
   }

   public BLonBoolean getValvePosEeValveTooLarge() {
      return (BLonBoolean)this.get(valvePosEeValveTooLarge);
   }

   public void setValvePosEeValveTooLarge(BLonBoolean v) {
      this.set(valvePosEeValveTooLarge, v, null);
   }

   public BLonBoolean getValvePosEeValveTooSmall() {
      return (BLonBoolean)this.get(valvePosEeValveTooSmall);
   }

   public void setValvePosEeValveTooSmall(BLonBoolean v) {
      this.set(valvePosEeValveTooSmall, v, null);
   }

   public BLonFloat getValvePosReserved267() {
      return (BLonFloat)this.get(valvePosReserved267);
   }

   public void setValvePosReserved267(BLonFloat v) {
      this.set(valvePosReserved267, v, null);
   }

   public BLonBoolean getValvePosReserved307() {
      return (BLonBoolean)this.get(valvePosReserved307);
   }

   public void setValvePosReserved307(BLonBoolean v) {
      this.set(valvePosReserved307, v, null);
   }

   public BLonBoolean getValvePosSfVoltageOutOfRange() {
      return (BLonBoolean)this.get(valvePosSfVoltageOutOfRange);
   }

   public void setValvePosSfVoltageOutOfRange(BLonBoolean v) {
      this.set(valvePosSfVoltageOutOfRange, v, null);
   }

   public BLonBoolean getValvePosSfElectronicHighTemp() {
      return (BLonBoolean)this.get(valvePosSfElectronicHighTemp);
   }

   public void setValvePosSfElectronicHighTemp(BLonBoolean v) {
      this.set(valvePosSfElectronicHighTemp, v, null);
   }

   public BLonBoolean getValvePosSfFrictionalResistance() {
      return (BLonBoolean)this.get(valvePosSfFrictionalResistance);
   }

   public void setValvePosSfFrictionalResistance(BLonBoolean v) {
      this.set(valvePosSfFrictionalResistance, v, null);
   }

   public BLonFloat getValvePosReserved446() {
      return (BLonFloat)this.get(valvePosReserved446);
   }

   public void setValvePosReserved446(BLonFloat v) {
      this.set(valvePosReserved446, v, null);
   }

   public BLonBoolean getValvePosGeneralFault() {
      return (BLonBoolean)this.get(valvePosGeneralFault);
   }

   public void setValvePosGeneralFault(BLonBoolean v) {
      this.set(valvePosGeneralFault, v, null);
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
         this.primitiveToOutputStream(pumpCtrlSfVoltageLow, out);
         this.primitiveToOutputStream(pumpCtrlSfVoltageHigh, out);
         this.primitiveToOutputStream(pumpCtrlSfPhase, out);
         this.primitiveToOutputStream(pumpCtrlSfNoFluid, out);
         this.primitiveToOutputStream(pumpCtrlSfPressLow, out);
         this.primitiveToOutputStream(pumpCtrlSfPressHigh, out);
         this.primitiveToOutputStream(pumpCtrlSfReserved16, out);
         this.primitiveToOutputStream(pumpCtrlSfReserved17, out);
         this.primitiveToOutputStream(pumpCtrlDfMotorTemp, out);
         this.primitiveToOutputStream(pumpCtrlDfMotorFailure, out);
         this.primitiveToOutputStream(pumpCtrlDfPumpBlocked, out);
         this.primitiveToOutputStream(pumpCtrlDfElectTemp, out);
         this.primitiveToOutputStream(pumpCtrlDfElectFailureNf, out);
         this.primitiveToOutputStream(pumpCtrlDfElectFailure, out);
         this.primitiveToOutputStream(pumpCtrlDfSensorFailure, out);
         this.primitiveToOutputStream(pumpCtrlDfReserved27, out);
         this.primitiveToOutputStream(pumpCtrlReserved307, out);
      } else {
         this.primitiveToOutputStream(valvePosDfValveBlocked, out);
         this.primitiveToOutputStream(valvePosDfBlockedDirectionOpen, out);
         this.primitiveToOutputStream(valvePosDfBlockedDirectionClose, out);
         this.primitiveToOutputStream(valvePosDfPositionError, out);
         this.primitiveToOutputStream(valvePosDfStrokeOutOfRange, out);
         this.primitiveToOutputStream(valvePosDfInitialization, out);
         this.primitiveToOutputStream(valvePosDfVibrationCavitation, out);
         this.primitiveToOutputStream(valvePosDfEdTooHigh, out);
         this.primitiveToOutputStream(valvePosReserved102, out);
         this.primitiveToOutputStream(valvePosEeOscillating, out);
         this.primitiveToOutputStream(valvePosEeValveTooLarge, out);
         this.primitiveToOutputStream(valvePosEeValveTooSmall, out);
         this.primitiveToOutputStream(valvePosReserved267, out);
         this.primitiveToOutputStream(valvePosReserved307, out);
         this.primitiveToOutputStream(valvePosSfVoltageOutOfRange, out);
         this.primitiveToOutputStream(valvePosSfElectronicHighTemp, out);
         this.primitiveToOutputStream(valvePosSfFrictionalResistance, out);
         this.primitiveToOutputStream(valvePosReserved446, out);
         this.primitiveToOutputStream(valvePosGeneralFault, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(deviceSelect, in);
      int f = this.getDeviceSelect().getEnum().getOrdinal();
      if (f == 0) {
         this.primitiveFromInputStream(pumpCtrlSfVoltageLow, in);
         this.primitiveFromInputStream(pumpCtrlSfVoltageHigh, in);
         this.primitiveFromInputStream(pumpCtrlSfPhase, in);
         this.primitiveFromInputStream(pumpCtrlSfNoFluid, in);
         this.primitiveFromInputStream(pumpCtrlSfPressLow, in);
         this.primitiveFromInputStream(pumpCtrlSfPressHigh, in);
         this.primitiveFromInputStream(pumpCtrlSfReserved16, in);
         this.primitiveFromInputStream(pumpCtrlSfReserved17, in);
         this.primitiveFromInputStream(pumpCtrlDfMotorTemp, in);
         this.primitiveFromInputStream(pumpCtrlDfMotorFailure, in);
         this.primitiveFromInputStream(pumpCtrlDfPumpBlocked, in);
         this.primitiveFromInputStream(pumpCtrlDfElectTemp, in);
         this.primitiveFromInputStream(pumpCtrlDfElectFailureNf, in);
         this.primitiveFromInputStream(pumpCtrlDfElectFailure, in);
         this.primitiveFromInputStream(pumpCtrlDfSensorFailure, in);
         this.primitiveFromInputStream(pumpCtrlDfReserved27, in);
         this.primitiveFromInputStream(pumpCtrlReserved307, in);
      } else {
         this.primitiveFromInputStream(valvePosDfValveBlocked, in);
         this.primitiveFromInputStream(valvePosDfBlockedDirectionOpen, in);
         this.primitiveFromInputStream(valvePosDfBlockedDirectionClose, in);
         this.primitiveFromInputStream(valvePosDfPositionError, in);
         this.primitiveFromInputStream(valvePosDfStrokeOutOfRange, in);
         this.primitiveFromInputStream(valvePosDfInitialization, in);
         this.primitiveFromInputStream(valvePosDfVibrationCavitation, in);
         this.primitiveFromInputStream(valvePosDfEdTooHigh, in);
         this.primitiveFromInputStream(valvePosReserved102, in);
         this.primitiveFromInputStream(valvePosEeOscillating, in);
         this.primitiveFromInputStream(valvePosEeValveTooLarge, in);
         this.primitiveFromInputStream(valvePosEeValveTooSmall, in);
         this.primitiveFromInputStream(valvePosReserved267, in);
         this.primitiveFromInputStream(valvePosReserved307, in);
         this.primitiveFromInputStream(valvePosSfVoltageOutOfRange, in);
         this.primitiveFromInputStream(valvePosSfElectronicHighTemp, in);
         this.primitiveFromInputStream(valvePosSfFrictionalResistance, in);
         this.primitiveFromInputStream(valvePosReserved446, in);
         this.primitiveFromInputStream(valvePosGeneralFault, in);
      }
   }
}
