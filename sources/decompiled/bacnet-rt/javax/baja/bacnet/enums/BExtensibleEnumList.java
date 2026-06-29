package javax.baja.bacnet.enums;

import java.util.ArrayList;
import javax.baja.bacnet.enums.access.BBacnetAccessAuthenticationFactorDisable;
import javax.baja.bacnet.enums.access.BBacnetAccessCredentialDisable;
import javax.baja.bacnet.enums.access.BBacnetAccessCredentialDisableReason;
import javax.baja.bacnet.enums.access.BBacnetAccessEvent;
import javax.baja.bacnet.enums.access.BBacnetAccessUserType;
import javax.baja.bacnet.enums.access.BBacnetAccessZoneOccupancyState;
import javax.baja.bacnet.enums.access.BBacnetAuthorizationExemption;
import javax.baja.bacnet.enums.access.BBacnetAuthorizationMode;
import javax.baja.bacnet.enums.access.BBacnetDoorAlarmState;
import javax.baja.bacnet.enums.access.BBacnetDoorStatus;
import javax.baja.bacnet.enums.lighting.BBacnetBinaryLightingPv;
import javax.baja.bacnet.enums.lighting.BBacnetLightingOperation;
import javax.baja.bacnet.enums.lighting.BBacnetLightingTransition;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.TextUtil;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.BFacets;
import javax.baja.sys.BStruct;
import javax.baja.sys.InvalidEnumException;
import javax.baja.sys.Property;
import javax.baja.sys.SlotCursor;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.xml.XElem;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "errorClassFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetErrorClass.TYPE))"
   ), @NiagaraProperty(
      name = "errorCodeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetErrorCode.TYPE))"
   ), @NiagaraProperty(
      name = "abortReasonFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAbortReason.TYPE))"
   ), @NiagaraProperty(
      name = "accessAuthenticationFactorDisableFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessAuthenticationFactorDisable.TYPE))"
   ), @NiagaraProperty(
      name = "accessCredentialDisableFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessCredentialDisable.TYPE))"
   ), @NiagaraProperty(
      name = "accessCredentialDisableReasonFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessCredentialDisableReason.TYPE))"
   ), @NiagaraProperty(
      name = "accessEventFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessEvent.TYPE))"
   ), @NiagaraProperty(
      name = "accessUserTypeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessUserType.TYPE))"
   ), @NiagaraProperty(
      name = "accessZoneOccupancyStateFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAccessZoneOccupancyState.TYPE))"
   ), @NiagaraProperty(
      name = "authorizationExemptionFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAuthorizationExemption.TYPE))"
   ), @NiagaraProperty(
      name = "authorizationModeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetAuthorizationMode.TYPE))"
   ), @NiagaraProperty(
      name = "binaryLightingPvFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetBinaryLightingPv.TYPE))"
   ), @NiagaraProperty(
      name = "deviceStatusFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetDeviceStatus.TYPE))"
   ), @NiagaraProperty(
      name = "doorAlarmStateFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetDoorAlarmState.TYPE))"
   ), @NiagaraProperty(
      name = "doorStatusFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetDoorStatus.TYPE))"
   ), @NiagaraProperty(
      name = "engineeringUnitsFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetEngineeringUnits.TYPE))"
   ), @NiagaraProperty(
      name = "eventStateFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetEventState.TYPE))"
   ), @NiagaraProperty(
      name = "eventTypeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetEventType.TYPE))"
   ), @NiagaraProperty(
      name = "lifeSafetyModeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyMode.TYPE))"
   ), @NiagaraProperty(
      name = "lifeSafetyOperationFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyOperation.TYPE))"
   ), @NiagaraProperty(
      name = "lifeSafetyStateFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyState.TYPE))"
   ), @NiagaraProperty(
      name = "lightingOperationFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLightingOperation.TYPE))"
   ), @NiagaraProperty(
      name = "lightingTransitionFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLightingTransition.TYPE))"
   ), @NiagaraProperty(
      name = "loggingTypeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetLoggingType.TYPE))"
   ), @NiagaraProperty(
      name = "maintenanceFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetMaintenance.TYPE))"
   ), @NiagaraProperty(
      name = "objectTypeFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetObjectType.TYPE))"
   ), @NiagaraProperty(
      name = "programErrorFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetProgramError.TYPE))"
   ), @NiagaraProperty(
      name = "propertyIdFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetPropertyIdentifier.TYPE))"
   ), @NiagaraProperty(
      name = "rejectReasonFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetRejectReason.TYPE))"
   ), @NiagaraProperty(
      name = "reliabilityFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetReliability.TYPE))"
   ), @NiagaraProperty(
      name = "restartReasonFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetRestartReason.TYPE))"
   ), @NiagaraProperty(
      name = "silencedStateFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetSilencedState.TYPE))"
   ), @NiagaraProperty(
      name = "vtClassFacets",
      type = "BFacets",
      defaultValue = "BFacets.makeEnum(BEnumRange.make(BBacnetVtClass.TYPE))"
   )})
public class BExtensibleEnumList extends BStruct {
   public static final Property errorClassFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetErrorClass.TYPE)), null);
   public static final Property errorCodeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetErrorCode.TYPE)), null);
   public static final Property abortReasonFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAbortReason.TYPE)), null);
   public static final Property accessAuthenticationFactorDisableFacets = newProperty(
      0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessAuthenticationFactorDisable.TYPE)), null
   );
   public static final Property accessCredentialDisableFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessCredentialDisable.TYPE)), null);
   public static final Property accessCredentialDisableReasonFacets = newProperty(
      0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessCredentialDisableReason.TYPE)), null
   );
   public static final Property accessEventFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessEvent.TYPE)), null);
   public static final Property accessUserTypeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessUserType.TYPE)), null);
   public static final Property accessZoneOccupancyStateFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAccessZoneOccupancyState.TYPE)), null);
   public static final Property authorizationExemptionFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAuthorizationExemption.TYPE)), null);
   public static final Property authorizationModeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetAuthorizationMode.TYPE)), null);
   public static final Property binaryLightingPvFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetBinaryLightingPv.TYPE)), null);
   public static final Property deviceStatusFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetDeviceStatus.TYPE)), null);
   public static final Property doorAlarmStateFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetDoorAlarmState.TYPE)), null);
   public static final Property doorStatusFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetDoorStatus.TYPE)), null);
   public static final Property engineeringUnitsFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetEngineeringUnits.TYPE)), null);
   public static final Property eventStateFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetEventState.TYPE)), null);
   public static final Property eventTypeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetEventType.TYPE)), null);
   public static final Property lifeSafetyModeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyMode.TYPE)), null);
   public static final Property lifeSafetyOperationFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyOperation.TYPE)), null);
   public static final Property lifeSafetyStateFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLifeSafetyState.TYPE)), null);
   public static final Property lightingOperationFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLightingOperation.TYPE)), null);
   public static final Property lightingTransitionFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLightingTransition.TYPE)), null);
   public static final Property loggingTypeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetLoggingType.TYPE)), null);
   public static final Property maintenanceFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetMaintenance.TYPE)), null);
   public static final Property objectTypeFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetObjectType.TYPE)), null);
   public static final Property programErrorFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetProgramError.TYPE)), null);
   public static final Property propertyIdFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetPropertyIdentifier.TYPE)), null);
   public static final Property rejectReasonFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetRejectReason.TYPE)), null);
   public static final Property reliabilityFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetReliability.TYPE)), null);
   public static final Property restartReasonFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetRestartReason.TYPE)), null);
   public static final Property silencedStateFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetSilencedState.TYPE)), null);
   public static final Property vtClassFacets = newProperty(0, BFacets.makeEnum(BEnumRange.make(BBacnetVtClass.TYPE)), null);
   public static final Type TYPE = Sys.loadType(BExtensibleEnumList.class);
   public static final BExtensibleEnumList niagaraEnums = new BExtensibleEnumList();

   public BFacets getErrorClassFacets() {
      return (BFacets)this.get(errorClassFacets);
   }

   public void setErrorClassFacets(BFacets v) {
      this.set(errorClassFacets, v, null);
   }

   public BFacets getErrorCodeFacets() {
      return (BFacets)this.get(errorCodeFacets);
   }

   public void setErrorCodeFacets(BFacets v) {
      this.set(errorCodeFacets, v, null);
   }

   public BFacets getAbortReasonFacets() {
      return (BFacets)this.get(abortReasonFacets);
   }

   public void setAbortReasonFacets(BFacets v) {
      this.set(abortReasonFacets, v, null);
   }

   public BFacets getAccessAuthenticationFactorDisableFacets() {
      return (BFacets)this.get(accessAuthenticationFactorDisableFacets);
   }

   public void setAccessAuthenticationFactorDisableFacets(BFacets v) {
      this.set(accessAuthenticationFactorDisableFacets, v, null);
   }

   public BFacets getAccessCredentialDisableFacets() {
      return (BFacets)this.get(accessCredentialDisableFacets);
   }

   public void setAccessCredentialDisableFacets(BFacets v) {
      this.set(accessCredentialDisableFacets, v, null);
   }

   public BFacets getAccessCredentialDisableReasonFacets() {
      return (BFacets)this.get(accessCredentialDisableReasonFacets);
   }

   public void setAccessCredentialDisableReasonFacets(BFacets v) {
      this.set(accessCredentialDisableReasonFacets, v, null);
   }

   public BFacets getAccessEventFacets() {
      return (BFacets)this.get(accessEventFacets);
   }

   public void setAccessEventFacets(BFacets v) {
      this.set(accessEventFacets, v, null);
   }

   public BFacets getAccessUserTypeFacets() {
      return (BFacets)this.get(accessUserTypeFacets);
   }

   public void setAccessUserTypeFacets(BFacets v) {
      this.set(accessUserTypeFacets, v, null);
   }

   public BFacets getAccessZoneOccupancyStateFacets() {
      return (BFacets)this.get(accessZoneOccupancyStateFacets);
   }

   public void setAccessZoneOccupancyStateFacets(BFacets v) {
      this.set(accessZoneOccupancyStateFacets, v, null);
   }

   public BFacets getAuthorizationExemptionFacets() {
      return (BFacets)this.get(authorizationExemptionFacets);
   }

   public void setAuthorizationExemptionFacets(BFacets v) {
      this.set(authorizationExemptionFacets, v, null);
   }

   public BFacets getAuthorizationModeFacets() {
      return (BFacets)this.get(authorizationModeFacets);
   }

   public void setAuthorizationModeFacets(BFacets v) {
      this.set(authorizationModeFacets, v, null);
   }

   public BFacets getBinaryLightingPvFacets() {
      return (BFacets)this.get(binaryLightingPvFacets);
   }

   public void setBinaryLightingPvFacets(BFacets v) {
      this.set(binaryLightingPvFacets, v, null);
   }

   public BFacets getDeviceStatusFacets() {
      return (BFacets)this.get(deviceStatusFacets);
   }

   public void setDeviceStatusFacets(BFacets v) {
      this.set(deviceStatusFacets, v, null);
   }

   public BFacets getDoorAlarmStateFacets() {
      return (BFacets)this.get(doorAlarmStateFacets);
   }

   public void setDoorAlarmStateFacets(BFacets v) {
      this.set(doorAlarmStateFacets, v, null);
   }

   public BFacets getDoorStatusFacets() {
      return (BFacets)this.get(doorStatusFacets);
   }

   public void setDoorStatusFacets(BFacets v) {
      this.set(doorStatusFacets, v, null);
   }

   public BFacets getEngineeringUnitsFacets() {
      return (BFacets)this.get(engineeringUnitsFacets);
   }

   public void setEngineeringUnitsFacets(BFacets v) {
      this.set(engineeringUnitsFacets, v, null);
   }

   public BFacets getEventStateFacets() {
      return (BFacets)this.get(eventStateFacets);
   }

   public void setEventStateFacets(BFacets v) {
      this.set(eventStateFacets, v, null);
   }

   public BFacets getEventTypeFacets() {
      return (BFacets)this.get(eventTypeFacets);
   }

   public void setEventTypeFacets(BFacets v) {
      this.set(eventTypeFacets, v, null);
   }

   public BFacets getLifeSafetyModeFacets() {
      return (BFacets)this.get(lifeSafetyModeFacets);
   }

   public void setLifeSafetyModeFacets(BFacets v) {
      this.set(lifeSafetyModeFacets, v, null);
   }

   public BFacets getLifeSafetyOperationFacets() {
      return (BFacets)this.get(lifeSafetyOperationFacets);
   }

   public void setLifeSafetyOperationFacets(BFacets v) {
      this.set(lifeSafetyOperationFacets, v, null);
   }

   public BFacets getLifeSafetyStateFacets() {
      return (BFacets)this.get(lifeSafetyStateFacets);
   }

   public void setLifeSafetyStateFacets(BFacets v) {
      this.set(lifeSafetyStateFacets, v, null);
   }

   public BFacets getLightingOperationFacets() {
      return (BFacets)this.get(lightingOperationFacets);
   }

   public void setLightingOperationFacets(BFacets v) {
      this.set(lightingOperationFacets, v, null);
   }

   public BFacets getLightingTransitionFacets() {
      return (BFacets)this.get(lightingTransitionFacets);
   }

   public void setLightingTransitionFacets(BFacets v) {
      this.set(lightingTransitionFacets, v, null);
   }

   public BFacets getLoggingTypeFacets() {
      return (BFacets)this.get(loggingTypeFacets);
   }

   public void setLoggingTypeFacets(BFacets v) {
      this.set(loggingTypeFacets, v, null);
   }

   public BFacets getMaintenanceFacets() {
      return (BFacets)this.get(maintenanceFacets);
   }

   public void setMaintenanceFacets(BFacets v) {
      this.set(maintenanceFacets, v, null);
   }

   public BFacets getObjectTypeFacets() {
      return (BFacets)this.get(objectTypeFacets);
   }

   public void setObjectTypeFacets(BFacets v) {
      this.set(objectTypeFacets, v, null);
   }

   public BFacets getProgramErrorFacets() {
      return (BFacets)this.get(programErrorFacets);
   }

   public void setProgramErrorFacets(BFacets v) {
      this.set(programErrorFacets, v, null);
   }

   public BFacets getPropertyIdFacets() {
      return (BFacets)this.get(propertyIdFacets);
   }

   public void setPropertyIdFacets(BFacets v) {
      this.set(propertyIdFacets, v, null);
   }

   public BFacets getRejectReasonFacets() {
      return (BFacets)this.get(rejectReasonFacets);
   }

   public void setRejectReasonFacets(BFacets v) {
      this.set(rejectReasonFacets, v, null);
   }

   public BFacets getReliabilityFacets() {
      return (BFacets)this.get(reliabilityFacets);
   }

   public void setReliabilityFacets(BFacets v) {
      this.set(reliabilityFacets, v, null);
   }

   public BFacets getRestartReasonFacets() {
      return (BFacets)this.get(restartReasonFacets);
   }

   public void setRestartReasonFacets(BFacets v) {
      this.set(restartReasonFacets, v, null);
   }

   public BFacets getSilencedStateFacets() {
      return (BFacets)this.get(silencedStateFacets);
   }

   public void setSilencedStateFacets(BFacets v) {
      this.set(silencedStateFacets, v, null);
   }

   public BFacets getVtClassFacets() {
      return (BFacets)this.get(vtClassFacets);
   }

   public void setVtClassFacets(BFacets v) {
      this.set(vtClassFacets, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BExtensibleEnumList() {
   }

   public BExtensibleEnumList(XElem xlm) {
      XElem[] enums = xlm.elems();

      for (int i = 0; i < enums.length; i++) {
         XElem[] vals = enums[i].elems("value");
         String enumName = enums[i].name();
         Property enumProp = this.loadSlots().getProperty(enumName + "Facets");
         BFacets enumFacets = (BFacets)this.get(enumProp);
         BEnumRange r = (BEnumRange)enumFacets.get("range");
         int[] newOrds = new int[vals.length];
         String[] newTags = new String[vals.length];

         for (int j = 0; j < vals.length; j++) {
            newTags[j] = vals[j].get("n");
            newOrds[j] = vals[j].geti("v");
         }

         ArrayList<Integer> vOrds = new ArrayList<>();
         ArrayList<String> vTags = new ArrayList<>();
         int[] ords = r.getOrdinals();
         String[] tags = this.getTags(r);

         for (int j = 0; j < ords.length; j++) {
            if (r.isDynamicOrdinal(ords[j])) {
               vOrds.add(ords[j]);
               vTags.add(tags[j]);
            }
         }

         int[] mergeOrds = new int[vOrds.size() + newOrds.length];
         String[] mergeTags = new String[vTags.size() + newTags.length];

         for (int jx = 0; jx < vOrds.size(); jx++) {
            mergeOrds[jx] = vOrds.get(jx);
         }

         System.arraycopy(newOrds, 0, mergeOrds, vOrds.size(), newOrds.length);

         for (int jx = 0; jx < vTags.size(); jx++) {
            mergeTags[jx] = vTags.get(jx);
         }

         System.arraycopy(newTags, 0, mergeTags, vTags.size(), newTags.length);
         this.set(enumProp, BFacets.makeEnum(BEnumRange.make(r.getFrozenType(), mergeOrds, mergeTags)));
      }
   }

   public BDynamicEnum getErrorClass() {
      return BDynamicEnum.make(0, this.getErrorClassRange());
   }

   public BDynamicEnum getErrorCode() {
      return BDynamicEnum.make(0, this.getErrorCodeRange());
   }

   public BDynamicEnum getAbortReason() {
      return BDynamicEnum.make(0, this.getAbortReasonRange());
   }

   public BDynamicEnum getAccessAuthenticationFactorDisable() {
      return BDynamicEnum.make(0, this.getAccessAuthenticationFactorDisableRange());
   }

   public BDynamicEnum getAccessCredentialDisable() {
      return BDynamicEnum.make(0, this.getAccessCredentialDisableRange());
   }

   public BDynamicEnum getAccessCredentialDisableReason() {
      return BDynamicEnum.make(0, this.getAccessCredentialDisableReasonRange());
   }

   public BDynamicEnum getAccessEvent() {
      return BDynamicEnum.make(0, this.getAccessEventRange());
   }

   public BDynamicEnum getAccessUserType() {
      return BDynamicEnum.make(0, this.getAccessUserTypeRange());
   }

   public BDynamicEnum getAccessZoneOccupancyState() {
      return BDynamicEnum.make(0, this.getAccessZoneOccupancyStateRange());
   }

   public BDynamicEnum getAuthorizationExemption() {
      return BDynamicEnum.make(0, this.getAuthorizationExemptionRange());
   }

   public BDynamicEnum getAuthorizationMode() {
      return BDynamicEnum.make(0, this.getAuthorizationModeRange());
   }

   public BDynamicEnum getBinaryLightingPv() {
      return BDynamicEnum.make(0, this.getBinaryLightingPvRange());
   }

   public BDynamicEnum getDeviceStatus() {
      return BDynamicEnum.make(0, this.getDeviceStatusRange());
   }

   public BDynamicEnum getDoorAlarmState() {
      return BDynamicEnum.make(0, this.getDoorAlarmStateRange());
   }

   public BDynamicEnum getDoorStatus() {
      return BDynamicEnum.make(0, this.getDoorStatusRange());
   }

   public BDynamicEnum getEngineeringUnits() {
      return BDynamicEnum.make(0, this.getEngineeringUnitsRange());
   }

   public BDynamicEnum getEventState() {
      return BDynamicEnum.make(0, this.getEventStateRange());
   }

   public BDynamicEnum getEventType() {
      return BDynamicEnum.make(0, this.getEventTypeRange());
   }

   public BDynamicEnum getLifeSafetyMode() {
      return BDynamicEnum.make(0, this.getLifeSafetyModeRange());
   }

   public BDynamicEnum getLifeSafetyOperation() {
      return BDynamicEnum.make(0, this.getLifeSafetyOperationRange());
   }

   public BDynamicEnum getLifeSafetyState() {
      return BDynamicEnum.make(0, this.getLifeSafetyStateRange());
   }

   public BDynamicEnum getLightingOperation() {
      return BDynamicEnum.make(0, this.getLightingOperationRange());
   }

   public BDynamicEnum getLightingTransition() {
      return BDynamicEnum.make(0, this.getLightingTransitionRange());
   }

   public BDynamicEnum getLoggingType() {
      return BDynamicEnum.make(0, this.getLoggingTypeRange());
   }

   public BDynamicEnum getMaintenance() {
      return BDynamicEnum.make(0, this.getMaintenanceRange());
   }

   public BDynamicEnum getObjectType() {
      return BDynamicEnum.make(0, this.getObjectTypeRange());
   }

   public BDynamicEnum getProgramError() {
      return BDynamicEnum.make(0, this.getProgramErrorRange());
   }

   public BDynamicEnum getPropertyId() {
      return BDynamicEnum.make(0, this.getPropertyIdRange());
   }

   public BDynamicEnum getRejectReason() {
      return BDynamicEnum.make(0, this.getRejectReasonRange());
   }

   public BDynamicEnum getReliability() {
      return BDynamicEnum.make(0, this.getReliabilityRange());
   }

   public BDynamicEnum getRestartReason() {
      return BDynamicEnum.make(0, this.getRestartReasonRange());
   }

   public BDynamicEnum getSilencedState() {
      return BDynamicEnum.make(0, this.getSilencedStateRange());
   }

   public BDynamicEnum getVtClass() {
      return BDynamicEnum.make(0, this.getVtClassRange());
   }

   public BEnumRange getErrorClassRange() {
      return this.getRange(errorClassFacets);
   }

   public BEnumRange getErrorCodeRange() {
      return this.getRange(errorCodeFacets);
   }

   public BEnumRange getAbortReasonRange() {
      return this.getRange(abortReasonFacets);
   }

   public BEnumRange getAccessAuthenticationFactorDisableRange() {
      return this.getRange(accessAuthenticationFactorDisableFacets);
   }

   public BEnumRange getAccessCredentialDisableRange() {
      return this.getRange(accessCredentialDisableFacets);
   }

   public BEnumRange getAccessCredentialDisableReasonRange() {
      return this.getRange(accessCredentialDisableReasonFacets);
   }

   public BEnumRange getAccessEventRange() {
      return this.getRange(accessEventFacets);
   }

   public BEnumRange getAccessUserTypeRange() {
      return this.getRange(accessUserTypeFacets);
   }

   public BEnumRange getAccessZoneOccupancyStateRange() {
      return this.getRange(accessZoneOccupancyStateFacets);
   }

   public BEnumRange getAuthorizationExemptionRange() {
      return this.getRange(authorizationExemptionFacets);
   }

   public BEnumRange getAuthorizationModeRange() {
      return this.getRange(authorizationModeFacets);
   }

   public BEnumRange getBinaryLightingPvRange() {
      return this.getRange(binaryLightingPvFacets);
   }

   public BEnumRange getDeviceStatusRange() {
      return this.getRange(deviceStatusFacets);
   }

   public BEnumRange getDoorAlarmStateRange() {
      return this.getRange(doorAlarmStateFacets);
   }

   public BEnumRange getDoorStatusRange() {
      return this.getRange(doorStatusFacets);
   }

   public BEnumRange getEngineeringUnitsRange() {
      return this.getRange(engineeringUnitsFacets);
   }

   public BEnumRange getEventStateRange() {
      return this.getRange(eventStateFacets);
   }

   public BEnumRange getEventTypeRange() {
      return this.getRange(eventTypeFacets);
   }

   public BEnumRange getLifeSafetyModeRange() {
      return this.getRange(lifeSafetyModeFacets);
   }

   public BEnumRange getLifeSafetyOperationRange() {
      return this.getRange(lifeSafetyOperationFacets);
   }

   public BEnumRange getLifeSafetyStateRange() {
      return this.getRange(lifeSafetyStateFacets);
   }

   public BEnumRange getLightingOperationRange() {
      return this.getRange(lightingOperationFacets);
   }

   public BEnumRange getLightingTransitionRange() {
      return this.getRange(lightingTransitionFacets);
   }

   public BEnumRange getLoggingTypeRange() {
      return this.getRange(loggingTypeFacets);
   }

   public BEnumRange getMaintenanceRange() {
      return this.getRange(maintenanceFacets);
   }

   public BEnumRange getObjectTypeRange() {
      return this.getRange(objectTypeFacets);
   }

   public BEnumRange getProgramErrorRange() {
      return this.getRange(programErrorFacets);
   }

   public BEnumRange getPropertyIdRange() {
      return this.getRange(propertyIdFacets);
   }

   public BEnumRange getRejectReasonRange() {
      return this.getRange(rejectReasonFacets);
   }

   public BEnumRange getReliabilityRange() {
      return this.getRange(reliabilityFacets);
   }

   public BEnumRange getRestartReasonRange() {
      return this.getRange(restartReasonFacets);
   }

   public BEnumRange getSilencedStateRange() {
      return this.getRange(silencedStateFacets);
   }

   public BEnumRange getVtClassRange() {
      return this.getRange(vtClassFacets);
   }

   private BEnumRange getRange(Property property) {
      return (BEnumRange)((BFacets)this.get(property)).getFacet("range");
   }

   public BEnumRange getEnumRange(String type) {
      if (type == null || !type.startsWith("bacnet:Bacnet")) {
         return null;
      } else if (type.equals(BBacnetPropertyIdentifier.TYPE.toString())) {
         return this.getPropertyIdRange();
      } else {
         String propertyName = type.substring("bacnet:Bacnet".length()) + "Facets";
         Property property = this.getProperty(TextUtil.decapitalize(propertyName));
         return property != null ? this.getRange(property) : null;
      }
   }

   public void addNewErrorClass(int enumValue) {
      this.addNewErrorClass(BBacnetErrorClass.tag(enumValue), enumValue);
   }

   public void addNewErrorClass(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getErrorClassRange(), enumValue, enumName);
      this.setErrorClassFacets(BFacets.makeEnum(newRange));
   }

   public void addNewErrorCode(int enumValue) {
      this.addNewErrorCode(BBacnetErrorCode.tag(enumValue), enumValue);
   }

   public void addNewErrorCode(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getErrorCodeRange(), enumValue, enumName);
      this.setErrorCodeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAbortReason(int enumValue) {
      this.addNewAbortReason(BBacnetAbortReason.tag(enumValue), enumValue);
   }

   public void addNewAbortReason(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAbortReasonRange(), enumValue, enumName);
      this.setAbortReasonFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessAuthenticationFactorDisable(int enumValue) {
      this.addNewAccessAuthenticationFactorDisable(BBacnetAccessAuthenticationFactorDisable.tag(enumValue), enumValue);
   }

   public void addNewAccessAuthenticationFactorDisable(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessAuthenticationFactorDisableRange(), enumValue, enumName);
      this.setAccessAuthenticationFactorDisableFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessCredentialDisable(int enumValue) {
      this.addNewAccessCredentialDisable(BBacnetAccessCredentialDisable.tag(enumValue), enumValue);
   }

   public void addNewAccessCredentialDisable(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessCredentialDisableRange(), enumValue, enumName);
      this.setAccessCredentialDisableFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessCredentialDisableReason(int enumValue) {
      this.addNewAccessCredentialDisableReason(BBacnetAccessCredentialDisableReason.tag(enumValue), enumValue);
   }

   public void addNewAccessCredentialDisableReason(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessCredentialDisableReasonRange(), enumValue, enumName);
      this.setAccessCredentialDisableReasonFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessEvent(int enumValue) {
      this.addNewAccessEvent(BBacnetAccessEvent.tag(enumValue), enumValue);
   }

   public void addNewAccessEvent(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessEventRange(), enumValue, enumName);
      this.setAccessEventFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessUserType(int enumValue) {
      this.addNewAccessUserType(BBacnetAccessUserType.tag(enumValue), enumValue);
   }

   public void addNewAccessUserType(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessUserTypeRange(), enumValue, enumName);
      this.setAccessUserTypeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAccessZoneOccupancyState(int enumValue) {
      this.addNewAccessZoneOccupancyState(BBacnetAccessZoneOccupancyState.tag(enumValue), enumValue);
   }

   public void addNewAccessZoneOccupancyState(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAccessZoneOccupancyStateRange(), enumValue, enumName);
      this.setAccessZoneOccupancyStateFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAuthorizationExemption(int enumValue) {
      this.addNewAuthorizationExemption(BBacnetAuthorizationExemption.tag(enumValue), enumValue);
   }

   public void addNewAuthorizationExemption(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAuthorizationExemptionRange(), enumValue, enumName);
      this.setAuthorizationExemptionFacets(BFacets.makeEnum(newRange));
   }

   public void addNewAuthorizationMode(int enumValue) {
      this.addNewAuthorizationMode(BBacnetAuthorizationMode.tag(enumValue), enumValue);
   }

   public void addNewAuthorizationMode(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getAuthorizationModeRange(), enumValue, enumName);
      this.setAuthorizationModeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewBinaryLightingPv(int enumValue) {
      this.addNewBinaryLightingPv(BBacnetBinaryLightingPv.tag(enumValue), enumValue);
   }

   public void addNewBinaryLightingPv(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getBinaryLightingPvRange(), enumValue, enumName);
      this.setBinaryLightingPvFacets(BFacets.makeEnum(newRange));
   }

   public void addNewDeviceStatus(int enumValue) {
      this.addNewDeviceStatus(BBacnetDeviceStatus.tag(enumValue), enumValue);
   }

   public void addNewDeviceStatus(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getDeviceStatusRange(), enumValue, enumName);
      this.setDeviceStatusFacets(BFacets.makeEnum(newRange));
   }

   public void addNewDoorAlarmState(int enumValue) {
      this.addNewDoorAlarmState(BBacnetDoorAlarmState.tag(enumValue), enumValue);
   }

   public void addNewDoorAlarmState(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getDoorAlarmStateRange(), enumValue, enumName);
      this.setDoorAlarmStateFacets(BFacets.makeEnum(newRange));
   }

   public void addNewDoorStatus(int enumValue) {
      this.addNewDoorStatus(BBacnetDoorStatus.tag(enumValue), enumValue);
   }

   public void addNewDoorStatus(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getDoorStatusRange(), enumValue, enumName);
      this.setDoorStatusFacets(BFacets.makeEnum(newRange));
   }

   public void addNewEngineeringUnits(int enumValue) {
      this.addNewEngineeringUnits(BBacnetEngineeringUnits.tag(enumValue), enumValue);
   }

   public void addNewEngineeringUnits(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getEngineeringUnitsRange(), enumValue, enumName);
      this.setEngineeringUnitsFacets(BFacets.makeEnum(newRange));
   }

   public void addNewEventState(int enumValue) {
      this.addNewEventState(BBacnetEventState.tag(enumValue), enumValue);
   }

   public void addNewEventState(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getEventStateRange(), enumValue, enumName);
      this.setEventStateFacets(BFacets.makeEnum(newRange));
   }

   public void addNewEventType(int enumValue) {
      this.addNewEventType(BBacnetEventType.tag(enumValue), enumValue);
   }

   public void addNewEventType(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getEventTypeRange(), enumValue, enumName);
      this.setEventTypeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLifeSafetyMode(int enumValue) {
      this.addNewLifeSafetyMode(BBacnetLifeSafetyMode.tag(enumValue), enumValue);
   }

   public void addNewLifeSafetyMode(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLifeSafetyModeRange(), enumValue, enumName);
      this.setLifeSafetyModeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLifeSafetyOperation(int enumValue) {
      this.addNewLifeSafetyOperation(BBacnetLifeSafetyOperation.tag(enumValue), enumValue);
   }

   public void addNewLifeSafetyOperation(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLifeSafetyOperationRange(), enumValue, enumName);
      this.setLifeSafetyOperationFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLifeSafetyState(int enumValue) {
      this.addNewLifeSafetyState(BBacnetLifeSafetyState.tag(enumValue), enumValue);
   }

   public void addNewLifeSafetyState(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLifeSafetyStateRange(), enumValue, enumName);
      this.setLifeSafetyStateFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLightingOperation(int enumValue) {
      this.addNewLightingOperation(BBacnetLightingOperation.tag(enumValue), enumValue);
   }

   public void addNewLightingOperation(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLightingOperationRange(), enumValue, enumName);
      this.setLightingOperationFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLightingTransition(int enumValue) {
      this.addNewLightingTransition(BBacnetLightingTransition.tag(enumValue), enumValue);
   }

   public void addNewLightingTransition(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLightingTransitionRange(), enumValue, enumName);
      this.setLightingTransitionFacets(BFacets.makeEnum(newRange));
   }

   public void addNewLoggingType(int enumValue) {
      this.addNewLoggingType(BBacnetLoggingType.tag(enumValue), enumValue);
   }

   public void addNewLoggingType(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getLoggingTypeRange(), enumValue, enumName);
      this.setLoggingTypeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewMaintenance(int enumValue) {
      this.addNewMaintenance(BBacnetMaintenance.tag(enumValue), enumValue);
   }

   public void addNewMaintenance(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getMaintenanceRange(), enumValue, enumName);
      this.setMaintenanceFacets(BFacets.makeEnum(newRange));
   }

   public void addNewObjectType(int enumValue) {
      this.addNewObjectType(BBacnetObjectType.tag(enumValue), enumValue);
   }

   public void addNewObjectType(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getObjectTypeRange(), enumValue, enumName);
      this.setObjectTypeFacets(BFacets.makeEnum(newRange));
   }

   public void addNewProgramError(int enumValue) {
      this.addNewProgramError(BBacnetProgramError.tag(enumValue), enumValue);
   }

   public void addNewProgramError(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getProgramErrorRange(), enumValue, enumName);
      this.setProgramErrorFacets(BFacets.makeEnum(newRange));
   }

   public void addNewPropertyId(int enumValue) {
      this.addNewPropertyId(BBacnetPropertyIdentifier.tag(enumValue), enumValue);
   }

   public void addNewPropertyId(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getPropertyIdRange(), enumValue, enumName);
      this.setPropertyIdFacets(BFacets.makeEnum(newRange));
   }

   public void addNewRejectReason(int enumValue) {
      this.addNewRejectReason(BBacnetRejectReason.tag(enumValue), enumValue);
   }

   public void addNewRejectReason(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getRejectReasonRange(), enumValue, enumName);
      this.setRejectReasonFacets(BFacets.makeEnum(newRange));
   }

   public void addNewReliability(int enumValue) {
      this.addNewReliability(BBacnetReliability.tag(enumValue), enumValue);
   }

   public void addNewReliability(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getReliabilityRange(), enumValue, enumName);
      this.setReliabilityFacets(BFacets.makeEnum(newRange));
   }

   public void addNewRestartReason(int enumValue) {
      this.addNewRestartReason(BBacnetRestartReason.tag(enumValue), enumValue);
   }

   public void addNewRestartReason(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getRestartReasonRange(), enumValue, enumName);
      this.setRestartReasonFacets(BFacets.makeEnum(newRange));
   }

   public void addNewSilencedState(int enumValue) {
      this.addNewSilencedState(BBacnetSilencedState.tag(enumValue), enumValue);
   }

   public void addNewSilencedState(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getSilencedStateRange(), enumValue, enumName);
      this.setSilencedStateFacets(BFacets.makeEnum(newRange));
   }

   public void addNewVtClass(int enumValue) {
      this.addNewVtClass(BBacnetVtClass.tag(enumValue), enumValue);
   }

   public void addNewVtClass(String enumName, int enumValue) {
      BEnumRange newRange = this.addNewEnum(this.getVtClassRange(), enumValue, enumName);
      this.setVtClassFacets(BFacets.makeEnum(newRange));
   }

   private BEnumRange addNewEnum(BEnumRange r, int enumValue, String enumName) {
      if (r.isOrdinal(enumValue)) {
         throw new InvalidEnumException("Enum value already used by " + r.getTag(enumValue));
      } else if (r.isTag(enumName)) {
         throw new InvalidEnumException("Enum name already used by " + r.tagToOrdinal(enumName));
      } else {
         int[] o = r.getOrdinals();
         ArrayList<Integer> olist = new ArrayList<>();
         ArrayList<String> tlist = new ArrayList<>();
         int count = 0;

         for (int i = 0; i < o.length; i++) {
            if (r.isDynamicOrdinal(o[i])) {
               count++;
               olist.add(o[i]);
               tlist.add(r.getTag(o[i]));
            }
         }

         olist.add(enumValue);
         tlist.add(enumName);
         int[] ords = new int[count + 1];

         for (int ix = 0; ix <= count; ix++) {
            ords[ix] = olist.get(ix);
         }

         String[] tags = tlist.toArray(new String[count + 1]);
         return BEnumRange.make(r.getFrozenType(), ords, tags);
      }
   }

   private String[] getTags(BEnumRange r) {
      int[] ordinals = r.getOrdinals();
      String[] tags = new String[ordinals.length];

      for (int i = 0; i < tags.length; i++) {
         tags[i] = r.getTag(ordinals[i]);
      }

      return tags;
   }

   public void merge(BExtensibleEnumList list) {
      SlotCursor<Property> c = this.getProperties();

      while (c.next(BFacets.class)) {
         Property p = c.property();
         if (p.isFrozen()) {
            BFacets fThis = (BFacets)c.get();
            BEnumRange rThis = (BEnumRange)fThis.get("range");
            int[] ordsThis = rThis.getOrdinals();
            String[] tagsThis = this.getTags(rThis);
            BFacets fList = (BFacets)list.get(p);
            BEnumRange rList = (BEnumRange)fList.get("range");
            int[] ordsList = rList.getOrdinals();
            String[] tagsList = this.getTags(rList);
            if (rThis.getFrozenType() != rList.getFrozenType()) {
               throw new IllegalStateException("Mismatch between frozen types!");
            }

            ArrayList<Integer> vOrds = new ArrayList<>();
            ArrayList<String> vTags = new ArrayList<>();

            for (int i = 0; i < ordsThis.length; i++) {
               if (rThis.isDynamicOrdinal(ordsThis[i])) {
                  vOrds.add(ordsThis[i]);
                  vTags.add(tagsThis[i]);
               }
            }

            for (int ix = 0; ix < ordsList.length; ix++) {
               if (rList.isDynamicOrdinal(ordsList[ix])) {
                  vOrds.add(ordsList[ix]);
                  vTags.add(tagsList[ix]);
               }
            }

            int[] ords = new int[vOrds.size()];
            String[] tags = new String[vTags.size()];

            for (int ixx = 0; ixx < ords.length; ixx++) {
               ords[ixx] = vOrds.get(ixx);
            }

            for (int ixx = 0; ixx < tags.length; ixx++) {
               tags[ixx] = vTags.get(ixx);
            }

            this.set(p, BFacets.makeEnum(BEnumRange.make(rThis.getFrozenType(), ords, tags)));
         }
      }
   }

   static {
      niagaraEnums.setErrorCodeFacets(BFacets.makeEnum(BBacnetErrorCode.NIAGARA_ERROR_CODES_RANGE));
   }
}
