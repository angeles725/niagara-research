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
      name = "pumpCtrlDeviceFault",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSupplyFault",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved12",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSpeedLow",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSpeedHigh",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved15",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlSetptOutOfRange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved17",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlLocalControl",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved21",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlRunning",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved23",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlRemotePress",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlRemoteFlow",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlRemoteTemp",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 1, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved27",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 0, 1, null)")}
   ), @NiagaraProperty(
      name = "pumpCtrlReserved307",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null)")}
   ), @NiagaraProperty(
      name = "valvePosRunning",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosAdapting",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosInitializing",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosLocalControl",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosSetptOutOfRange",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosRemoteCtrlSignal",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved167",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 1, 1F, 0F, true, 1, 0, false, 0F, 2, null)")}
   ), @NiagaraProperty(
      name = "valvePosHwEmergency",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosSwEmergency",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved227",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 1, 1F, 0F, true, 2, 0, false, 0F, 6, null)")}
   ), @NiagaraProperty(
      name = "valvePosReserved307",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null)")}
   )})
public class BLonDevStatus extends BLonData {
   public static final Property deviceSelect = newProperty(0, BLonEnum.make(BLonDeviceSelectEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property pumpCtrlDeviceFault = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property pumpCtrlSupplyFault = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null));
   public static final Property pumpCtrlReserved12 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null));
   public static final Property pumpCtrlSpeedLow = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property pumpCtrlSpeedHigh = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null));
   public static final Property pumpCtrlReserved15 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null));
   public static final Property pumpCtrlSetptOutOfRange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 1, 1, null));
   public static final Property pumpCtrlReserved17 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 0, 1, null));
   public static final Property pumpCtrlLocalControl = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null));
   public static final Property pumpCtrlReserved21 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null));
   public static final Property pumpCtrlRunning = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 5, 1, null));
   public static final Property pumpCtrlReserved23 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 4, 1, null));
   public static final Property pumpCtrlRemotePress = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 3, 1, null));
   public static final Property pumpCtrlRemoteFlow = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 2, 1, null));
   public static final Property pumpCtrlRemoteTemp = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 1, 1, null));
   public static final Property pumpCtrlReserved27 = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 0, 1, null));
   public static final Property pumpCtrlReserved307 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null));
   public static final Property valvePosRunning = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 7, 1, null));
   public static final Property valvePosAdapting = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 6, 1, null));
   public static final Property valvePosInitializing = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 5, 1, null));
   public static final Property valvePosLocalControl = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 4, 1, null));
   public static final Property valvePosSetptOutOfRange = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 3, 1, null));
   public static final Property valvePosRemoteCtrlSignal = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 1, 2, 1, null));
   public static final Property valvePosReserved167 = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 1.0F, 1.0F, 0.0F, true, 1, 0, false, 0.0F, 2, null)
   );
   public static final Property valvePosHwEmergency = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 7, 1, null));
   public static final Property valvePosSwEmergency = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 2, 6, 1, null));
   public static final Property valvePosReserved227 = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 1.0F, 1.0F, 0.0F, true, 2, 0, false, 0.0F, 6, null)
   );
   public static final Property valvePosReserved307 = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 8, null));
   public static final Type TYPE = Sys.loadType(BLonDevStatus.class);

   public BLonEnum getDeviceSelect() {
      return (BLonEnum)this.get(deviceSelect);
   }

   public void setDeviceSelect(BLonEnum v) {
      this.set(deviceSelect, v, null);
   }

   public BLonBoolean getPumpCtrlDeviceFault() {
      return (BLonBoolean)this.get(pumpCtrlDeviceFault);
   }

   public void setPumpCtrlDeviceFault(BLonBoolean v) {
      this.set(pumpCtrlDeviceFault, v, null);
   }

   public BLonBoolean getPumpCtrlSupplyFault() {
      return (BLonBoolean)this.get(pumpCtrlSupplyFault);
   }

   public void setPumpCtrlSupplyFault(BLonBoolean v) {
      this.set(pumpCtrlSupplyFault, v, null);
   }

   public BLonBoolean getPumpCtrlReserved12() {
      return (BLonBoolean)this.get(pumpCtrlReserved12);
   }

   public void setPumpCtrlReserved12(BLonBoolean v) {
      this.set(pumpCtrlReserved12, v, null);
   }

   public BLonBoolean getPumpCtrlSpeedLow() {
      return (BLonBoolean)this.get(pumpCtrlSpeedLow);
   }

   public void setPumpCtrlSpeedLow(BLonBoolean v) {
      this.set(pumpCtrlSpeedLow, v, null);
   }

   public BLonBoolean getPumpCtrlSpeedHigh() {
      return (BLonBoolean)this.get(pumpCtrlSpeedHigh);
   }

   public void setPumpCtrlSpeedHigh(BLonBoolean v) {
      this.set(pumpCtrlSpeedHigh, v, null);
   }

   public BLonBoolean getPumpCtrlReserved15() {
      return (BLonBoolean)this.get(pumpCtrlReserved15);
   }

   public void setPumpCtrlReserved15(BLonBoolean v) {
      this.set(pumpCtrlReserved15, v, null);
   }

   public BLonBoolean getPumpCtrlSetptOutOfRange() {
      return (BLonBoolean)this.get(pumpCtrlSetptOutOfRange);
   }

   public void setPumpCtrlSetptOutOfRange(BLonBoolean v) {
      this.set(pumpCtrlSetptOutOfRange, v, null);
   }

   public BLonBoolean getPumpCtrlReserved17() {
      return (BLonBoolean)this.get(pumpCtrlReserved17);
   }

   public void setPumpCtrlReserved17(BLonBoolean v) {
      this.set(pumpCtrlReserved17, v, null);
   }

   public BLonBoolean getPumpCtrlLocalControl() {
      return (BLonBoolean)this.get(pumpCtrlLocalControl);
   }

   public void setPumpCtrlLocalControl(BLonBoolean v) {
      this.set(pumpCtrlLocalControl, v, null);
   }

   public BLonBoolean getPumpCtrlReserved21() {
      return (BLonBoolean)this.get(pumpCtrlReserved21);
   }

   public void setPumpCtrlReserved21(BLonBoolean v) {
      this.set(pumpCtrlReserved21, v, null);
   }

   public BLonBoolean getPumpCtrlRunning() {
      return (BLonBoolean)this.get(pumpCtrlRunning);
   }

   public void setPumpCtrlRunning(BLonBoolean v) {
      this.set(pumpCtrlRunning, v, null);
   }

   public BLonBoolean getPumpCtrlReserved23() {
      return (BLonBoolean)this.get(pumpCtrlReserved23);
   }

   public void setPumpCtrlReserved23(BLonBoolean v) {
      this.set(pumpCtrlReserved23, v, null);
   }

   public BLonBoolean getPumpCtrlRemotePress() {
      return (BLonBoolean)this.get(pumpCtrlRemotePress);
   }

   public void setPumpCtrlRemotePress(BLonBoolean v) {
      this.set(pumpCtrlRemotePress, v, null);
   }

   public BLonBoolean getPumpCtrlRemoteFlow() {
      return (BLonBoolean)this.get(pumpCtrlRemoteFlow);
   }

   public void setPumpCtrlRemoteFlow(BLonBoolean v) {
      this.set(pumpCtrlRemoteFlow, v, null);
   }

   public BLonBoolean getPumpCtrlRemoteTemp() {
      return (BLonBoolean)this.get(pumpCtrlRemoteTemp);
   }

   public void setPumpCtrlRemoteTemp(BLonBoolean v) {
      this.set(pumpCtrlRemoteTemp, v, null);
   }

   public BLonBoolean getPumpCtrlReserved27() {
      return (BLonBoolean)this.get(pumpCtrlReserved27);
   }

   public void setPumpCtrlReserved27(BLonBoolean v) {
      this.set(pumpCtrlReserved27, v, null);
   }

   public BLonFloat getPumpCtrlReserved307() {
      return (BLonFloat)this.get(pumpCtrlReserved307);
   }

   public void setPumpCtrlReserved307(BLonFloat v) {
      this.set(pumpCtrlReserved307, v, null);
   }

   public BLonBoolean getValvePosRunning() {
      return (BLonBoolean)this.get(valvePosRunning);
   }

   public void setValvePosRunning(BLonBoolean v) {
      this.set(valvePosRunning, v, null);
   }

   public BLonBoolean getValvePosAdapting() {
      return (BLonBoolean)this.get(valvePosAdapting);
   }

   public void setValvePosAdapting(BLonBoolean v) {
      this.set(valvePosAdapting, v, null);
   }

   public BLonBoolean getValvePosInitializing() {
      return (BLonBoolean)this.get(valvePosInitializing);
   }

   public void setValvePosInitializing(BLonBoolean v) {
      this.set(valvePosInitializing, v, null);
   }

   public BLonBoolean getValvePosLocalControl() {
      return (BLonBoolean)this.get(valvePosLocalControl);
   }

   public void setValvePosLocalControl(BLonBoolean v) {
      this.set(valvePosLocalControl, v, null);
   }

   public BLonBoolean getValvePosSetptOutOfRange() {
      return (BLonBoolean)this.get(valvePosSetptOutOfRange);
   }

   public void setValvePosSetptOutOfRange(BLonBoolean v) {
      this.set(valvePosSetptOutOfRange, v, null);
   }

   public BLonBoolean getValvePosRemoteCtrlSignal() {
      return (BLonBoolean)this.get(valvePosRemoteCtrlSignal);
   }

   public void setValvePosRemoteCtrlSignal(BLonBoolean v) {
      this.set(valvePosRemoteCtrlSignal, v, null);
   }

   public BLonFloat getValvePosReserved167() {
      return (BLonFloat)this.get(valvePosReserved167);
   }

   public void setValvePosReserved167(BLonFloat v) {
      this.set(valvePosReserved167, v, null);
   }

   public BLonBoolean getValvePosHwEmergency() {
      return (BLonBoolean)this.get(valvePosHwEmergency);
   }

   public void setValvePosHwEmergency(BLonBoolean v) {
      this.set(valvePosHwEmergency, v, null);
   }

   public BLonBoolean getValvePosSwEmergency() {
      return (BLonBoolean)this.get(valvePosSwEmergency);
   }

   public void setValvePosSwEmergency(BLonBoolean v) {
      this.set(valvePosSwEmergency, v, null);
   }

   public BLonFloat getValvePosReserved227() {
      return (BLonFloat)this.get(valvePosReserved227);
   }

   public void setValvePosReserved227(BLonFloat v) {
      this.set(valvePosReserved227, v, null);
   }

   public BLonFloat getValvePosReserved307() {
      return (BLonFloat)this.get(valvePosReserved307);
   }

   public void setValvePosReserved307(BLonFloat v) {
      this.set(valvePosReserved307, v, null);
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
         this.primitiveToOutputStream(pumpCtrlDeviceFault, out);
         this.primitiveToOutputStream(pumpCtrlSupplyFault, out);
         this.primitiveToOutputStream(pumpCtrlReserved12, out);
         this.primitiveToOutputStream(pumpCtrlSpeedLow, out);
         this.primitiveToOutputStream(pumpCtrlSpeedHigh, out);
         this.primitiveToOutputStream(pumpCtrlReserved15, out);
         this.primitiveToOutputStream(pumpCtrlSetptOutOfRange, out);
         this.primitiveToOutputStream(pumpCtrlReserved17, out);
         this.primitiveToOutputStream(pumpCtrlLocalControl, out);
         this.primitiveToOutputStream(pumpCtrlReserved21, out);
         this.primitiveToOutputStream(pumpCtrlRunning, out);
         this.primitiveToOutputStream(pumpCtrlReserved23, out);
         this.primitiveToOutputStream(pumpCtrlRemotePress, out);
         this.primitiveToOutputStream(pumpCtrlRemoteFlow, out);
         this.primitiveToOutputStream(pumpCtrlRemoteTemp, out);
         this.primitiveToOutputStream(pumpCtrlReserved27, out);
         this.primitiveToOutputStream(pumpCtrlReserved307, out);
      } else {
         this.primitiveToOutputStream(valvePosRunning, out);
         this.primitiveToOutputStream(valvePosAdapting, out);
         this.primitiveToOutputStream(valvePosInitializing, out);
         this.primitiveToOutputStream(valvePosLocalControl, out);
         this.primitiveToOutputStream(valvePosSetptOutOfRange, out);
         this.primitiveToOutputStream(valvePosRemoteCtrlSignal, out);
         this.primitiveToOutputStream(valvePosReserved167, out);
         this.primitiveToOutputStream(valvePosHwEmergency, out);
         this.primitiveToOutputStream(valvePosSwEmergency, out);
         this.primitiveToOutputStream(valvePosReserved227, out);
         this.primitiveToOutputStream(valvePosReserved307, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(deviceSelect, in);
      int f = this.getDeviceSelect().getEnum().getOrdinal();
      if (f == 0) {
         this.primitiveFromInputStream(pumpCtrlDeviceFault, in);
         this.primitiveFromInputStream(pumpCtrlSupplyFault, in);
         this.primitiveFromInputStream(pumpCtrlReserved12, in);
         this.primitiveFromInputStream(pumpCtrlSpeedLow, in);
         this.primitiveFromInputStream(pumpCtrlSpeedHigh, in);
         this.primitiveFromInputStream(pumpCtrlReserved15, in);
         this.primitiveFromInputStream(pumpCtrlSetptOutOfRange, in);
         this.primitiveFromInputStream(pumpCtrlReserved17, in);
         this.primitiveFromInputStream(pumpCtrlLocalControl, in);
         this.primitiveFromInputStream(pumpCtrlReserved21, in);
         this.primitiveFromInputStream(pumpCtrlRunning, in);
         this.primitiveFromInputStream(pumpCtrlReserved23, in);
         this.primitiveFromInputStream(pumpCtrlRemotePress, in);
         this.primitiveFromInputStream(pumpCtrlRemoteFlow, in);
         this.primitiveFromInputStream(pumpCtrlRemoteTemp, in);
         this.primitiveFromInputStream(pumpCtrlReserved27, in);
         this.primitiveFromInputStream(pumpCtrlReserved307, in);
      } else {
         this.primitiveFromInputStream(valvePosRunning, in);
         this.primitiveFromInputStream(valvePosAdapting, in);
         this.primitiveFromInputStream(valvePosInitializing, in);
         this.primitiveFromInputStream(valvePosLocalControl, in);
         this.primitiveFromInputStream(valvePosSetptOutOfRange, in);
         this.primitiveFromInputStream(valvePosRemoteCtrlSignal, in);
         this.primitiveFromInputStream(valvePosReserved167, in);
         this.primitiveFromInputStream(valvePosHwEmergency, in);
         this.primitiveFromInputStream(valvePosSwEmergency, in);
         this.primitiveFromInputStream(valvePosReserved227, in);
         this.primitiveFromInputStream(valvePosReserved307, in);
      }
   }
}
