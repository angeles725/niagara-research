package javax.baja.bacnet.config;

import javax.baja.bacnet.BBacnetObject;
import javax.baja.bacnet.datatypes.BBacnetBitString;
import javax.baja.bacnet.datatypes.BBacnetObjectIdentifier;
import javax.baja.bacnet.enums.BBacnetObjectType;
import javax.baja.bacnet.enums.BBacnetProgramRequest;
import javax.baja.bacnet.enums.BBacnetProgramState;
import javax.baja.bacnet.util.BacnetBitStringUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BDynamicEnum;
import javax.baja.sys.BEnumRange;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "BBacnetObjectIdentifier",
      defaultValue = "BBacnetObjectIdentifier.make(BBacnetObjectType.PROGRAM)",
      flags = 8,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_IDENTIFIER, ASN_OBJECT_IDENTIFIER)")},
      override = true
   ), @NiagaraProperty(
      name = "objectType",
      type = "BEnum",
      defaultValue = "BDynamicEnum.make(BBacnetObjectType.PROGRAM, BEnumRange.make(BBacnetObjectType.TYPE))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OBJECT_TYPE, ASN_ENUMERATED)")},
      override = true
   ), @NiagaraProperty(
      name = "programState",
      type = "BBacnetProgramState",
      defaultValue = "BBacnetProgramState.idle",
      flags = 3,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROGRAM_STATE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "programChange",
      type = "BBacnetProgramRequest",
      defaultValue = "BBacnetProgramRequest.ready",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.PROGRAM_CHANGE, ASN_ENUMERATED)")}
   ), @NiagaraProperty(
      name = "statusFlags",
      type = "BBacnetBitString",
      defaultValue = "BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength(\"BacnetStatusFlags\"))",
      flags = 1,
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.STATUS_FLAGS, ASN_BIT_STRING, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)")}
   ), @NiagaraProperty(
      name = "outOfService",
      type = "boolean",
      defaultValue = "false",
      facets = {@Facet("makeFacets(BBacnetPropertyIdentifier.OUT_OF_SERVICE, ASN_BOOLEAN)")}
   )})
public class BBacnetProgram extends BBacnetObject {
   public static final Property objectId = newProperty(8, BBacnetObjectIdentifier.make(16), makeFacets(75, 12));
   public static final Property objectType = newProperty(1, BDynamicEnum.make(16, BEnumRange.make(BBacnetObjectType.TYPE)), makeFacets(79, 9));
   public static final Property programState = newProperty(3, BBacnetProgramState.idle, makeFacets(92, 9));
   public static final Property programChange = newProperty(0, BBacnetProgramRequest.ready, makeFacets(90, 9));
   public static final Property statusFlags = newProperty(
      1,
      BBacnetBitString.emptyBitString(BacnetBitStringUtil.getBitStringLength("BacnetStatusFlags")),
      makeFacets(111, 8, BacnetBitStringUtil.BACNET_STATUS_FLAGS_MAP)
   );
   public static final Property outOfService = newProperty(0, false, makeFacets(81, 1));
   public static final Type TYPE = Sys.loadType(BBacnetProgram.class);

   public BBacnetProgramState getProgramState() {
      return (BBacnetProgramState)this.get(programState);
   }

   public void setProgramState(BBacnetProgramState v) {
      this.set(programState, v, null);
   }

   public BBacnetProgramRequest getProgramChange() {
      return (BBacnetProgramRequest)this.get(programChange);
   }

   public void setProgramChange(BBacnetProgramRequest v) {
      this.set(programChange, v, null);
   }

   public BBacnetBitString getStatusFlags() {
      return (BBacnetBitString)this.get(statusFlags);
   }

   public void setStatusFlags(BBacnetBitString v) {
      this.set(statusFlags, v, null);
   }

   public boolean getOutOfService() {
      return this.getBoolean(outOfService);
   }

   public void setOutOfService(boolean v) {
      this.setBoolean(outOfService, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public String toString(Context context) {
      StringBuilder sb = new StringBuilder();
      sb.append(this.getObjectId().toString(context)).append((char)(nameContext.equals(context) ? '_' : ':'));
      return sb.toString();
   }
}
