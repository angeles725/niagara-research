package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.enums.BLonRailAudioSensorTypeEnum;
import javax.baja.lonworks.enums.BLonRailAudioTypeEnum;
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
      name = "audioLine",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 0, 5, 3,  null)")}
   ), @NiagaraProperty(
      name = "duplexFull",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 0, 4, 1,  null)")}
   ), @NiagaraProperty(
      name = "destP2P",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.bb, 0, 3, 1,  null)")}
   ), @NiagaraProperty(
      name = "reserved",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 0, 0, 3,  null)")}
   ), @NiagaraProperty(
      name = "audioType",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonRailAudioTypeEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "addrInitUnitId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0F,true, 8, 1F, 0F, true, 2, 4, false, 0F, 4, null)")}
   ), @NiagaraProperty(
      name = "addrInitLocation",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 4,  null)")}
   ), @NiagaraProperty(
      name = "addrInitCarId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 3, 5,  null)")}
   ), @NiagaraProperty(
      name = "addrInitReserved",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 3,  null)")}
   ), @NiagaraProperty(
      name = "addrInitAudioSensorType",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "addrTalkUnitId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 5, 4, 4,  null)")}
   ), @NiagaraProperty(
      name = "addrTalkLocation",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 5, 0, 4,  null)")}
   ), @NiagaraProperty(
      name = "addrTalkCarId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 6, 3, 5,  null)")}
   ), @NiagaraProperty(
      name = "addrTalkReserved",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 6, 0, 3,  null)")}
   ), @NiagaraProperty(
      name = "addrTalkAudioSensorType",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "addrDestP2PUnitId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 8, 4, 4,  null)")}
   ), @NiagaraProperty(
      name = "addrDestP2PLocation",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 8, 0, 4,  null)")}
   ), @NiagaraProperty(
      name = "addrDestP2PCarId",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 9, 3, 5,  null)")}
   ), @NiagaraProperty(
      name = "addrDestP2PReserved",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.ub, 9, 0, 3,  null)")}
   ), @NiagaraProperty(
      name = "addrDestP2PAudioSensorType",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "addrDestP2MMaskUnit",
      type = "BLonFloat",
      defaultValue = "BLonFloat  .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 8, null)")}
   ), @NiagaraProperty(
      name = "addrDestP2MMaskCar",
      type = "BLonString",
      defaultValue = "BLonString .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.st, 4, null)")}
   ), @NiagaraProperty(
      name = "addrDestP2MMaskLocation",
      type = "BLonString",
      defaultValue = "BLonString .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.st, 2, null)")}
   ), @NiagaraProperty(
      name = "addrDestP2MMaskAudio",
      type = "BLonString",
      defaultValue = "BLonString .DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.st, 3, null)")}
   )})
public class BLonRacCtrl extends BLonData {
   public static final Property audioLine = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 0, 5, 3, null));
   public static final Property duplexFull = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 0, 4, 1, null));
   public static final Property destP2P = newProperty(0, BLonBoolean.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.bb, 0, 3, 1, null));
   public static final Property reserved = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 0, 0, 3, null));
   public static final Property audioType = newProperty(0, BLonEnum.make(BLonRailAudioTypeEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property addrInitUnitId = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, false, 0.0F, true, 8.0F, 1.0F, 0.0F, true, 2, 4, false, 0.0F, 4, null)
   );
   public static final Property addrInitLocation = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 2, 0, 4, null));
   public static final Property addrInitCarId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 3, 5, null));
   public static final Property addrInitReserved = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 3, 0, 3, null));
   public static final Property addrInitAudioSensorType = newProperty(
      0, BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null)
   );
   public static final Property addrTalkUnitId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 5, 4, 4, null));
   public static final Property addrTalkLocation = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 5, 0, 4, null));
   public static final Property addrTalkCarId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 6, 3, 5, null));
   public static final Property addrTalkReserved = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 6, 0, 3, null));
   public static final Property addrTalkAudioSensorType = newProperty(
      0, BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null)
   );
   public static final Property addrDestP2PUnitId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 8, 4, 4, null));
   public static final Property addrDestP2PLocation = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 8, 0, 4, null));
   public static final Property addrDestP2PCarId = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 9, 3, 5, null));
   public static final Property addrDestP2PReserved = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.ub, 9, 0, 3, null));
   public static final Property addrDestP2PAudioSensorType = newProperty(
      0, BLonEnum.make(BLonRailAudioSensorTypeEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null)
   );
   public static final Property addrDestP2MMaskUnit = newProperty(0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 8, null));
   public static final Property addrDestP2MMaskCar = newProperty(0, BLonString.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.st, 4, null));
   public static final Property addrDestP2MMaskLocation = newProperty(0, BLonString.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.st, 2, null));
   public static final Property addrDestP2MMaskAudio = newProperty(0, BLonString.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.st, 3, null));
   public static final Type TYPE = Sys.loadType(BLonRacCtrl.class);

   public BLonFloat getAudioLine() {
      return (BLonFloat)this.get(audioLine);
   }

   public void setAudioLine(BLonFloat v) {
      this.set(audioLine, v, null);
   }

   public BLonBoolean getDuplexFull() {
      return (BLonBoolean)this.get(duplexFull);
   }

   public void setDuplexFull(BLonBoolean v) {
      this.set(duplexFull, v, null);
   }

   public BLonBoolean getDestP2P() {
      return (BLonBoolean)this.get(destP2P);
   }

   public void setDestP2P(BLonBoolean v) {
      this.set(destP2P, v, null);
   }

   public BLonFloat getReserved() {
      return (BLonFloat)this.get(reserved);
   }

   public void setReserved(BLonFloat v) {
      this.set(reserved, v, null);
   }

   public BLonEnum getAudioType() {
      return (BLonEnum)this.get(audioType);
   }

   public void setAudioType(BLonEnum v) {
      this.set(audioType, v, null);
   }

   public BLonFloat getAddrInitUnitId() {
      return (BLonFloat)this.get(addrInitUnitId);
   }

   public void setAddrInitUnitId(BLonFloat v) {
      this.set(addrInitUnitId, v, null);
   }

   public BLonFloat getAddrInitLocation() {
      return (BLonFloat)this.get(addrInitLocation);
   }

   public void setAddrInitLocation(BLonFloat v) {
      this.set(addrInitLocation, v, null);
   }

   public BLonFloat getAddrInitCarId() {
      return (BLonFloat)this.get(addrInitCarId);
   }

   public void setAddrInitCarId(BLonFloat v) {
      this.set(addrInitCarId, v, null);
   }

   public BLonFloat getAddrInitReserved() {
      return (BLonFloat)this.get(addrInitReserved);
   }

   public void setAddrInitReserved(BLonFloat v) {
      this.set(addrInitReserved, v, null);
   }

   public BLonEnum getAddrInitAudioSensorType() {
      return (BLonEnum)this.get(addrInitAudioSensorType);
   }

   public void setAddrInitAudioSensorType(BLonEnum v) {
      this.set(addrInitAudioSensorType, v, null);
   }

   public BLonFloat getAddrTalkUnitId() {
      return (BLonFloat)this.get(addrTalkUnitId);
   }

   public void setAddrTalkUnitId(BLonFloat v) {
      this.set(addrTalkUnitId, v, null);
   }

   public BLonFloat getAddrTalkLocation() {
      return (BLonFloat)this.get(addrTalkLocation);
   }

   public void setAddrTalkLocation(BLonFloat v) {
      this.set(addrTalkLocation, v, null);
   }

   public BLonFloat getAddrTalkCarId() {
      return (BLonFloat)this.get(addrTalkCarId);
   }

   public void setAddrTalkCarId(BLonFloat v) {
      this.set(addrTalkCarId, v, null);
   }

   public BLonFloat getAddrTalkReserved() {
      return (BLonFloat)this.get(addrTalkReserved);
   }

   public void setAddrTalkReserved(BLonFloat v) {
      this.set(addrTalkReserved, v, null);
   }

   public BLonEnum getAddrTalkAudioSensorType() {
      return (BLonEnum)this.get(addrTalkAudioSensorType);
   }

   public void setAddrTalkAudioSensorType(BLonEnum v) {
      this.set(addrTalkAudioSensorType, v, null);
   }

   public BLonFloat getAddrDestP2PUnitId() {
      return (BLonFloat)this.get(addrDestP2PUnitId);
   }

   public void setAddrDestP2PUnitId(BLonFloat v) {
      this.set(addrDestP2PUnitId, v, null);
   }

   public BLonFloat getAddrDestP2PLocation() {
      return (BLonFloat)this.get(addrDestP2PLocation);
   }

   public void setAddrDestP2PLocation(BLonFloat v) {
      this.set(addrDestP2PLocation, v, null);
   }

   public BLonFloat getAddrDestP2PCarId() {
      return (BLonFloat)this.get(addrDestP2PCarId);
   }

   public void setAddrDestP2PCarId(BLonFloat v) {
      this.set(addrDestP2PCarId, v, null);
   }

   public BLonFloat getAddrDestP2PReserved() {
      return (BLonFloat)this.get(addrDestP2PReserved);
   }

   public void setAddrDestP2PReserved(BLonFloat v) {
      this.set(addrDestP2PReserved, v, null);
   }

   public BLonEnum getAddrDestP2PAudioSensorType() {
      return (BLonEnum)this.get(addrDestP2PAudioSensorType);
   }

   public void setAddrDestP2PAudioSensorType(BLonEnum v) {
      this.set(addrDestP2PAudioSensorType, v, null);
   }

   public BLonFloat getAddrDestP2MMaskUnit() {
      return (BLonFloat)this.get(addrDestP2MMaskUnit);
   }

   public void setAddrDestP2MMaskUnit(BLonFloat v) {
      this.set(addrDestP2MMaskUnit, v, null);
   }

   public BLonString getAddrDestP2MMaskCar() {
      return (BLonString)this.get(addrDestP2MMaskCar);
   }

   public void setAddrDestP2MMaskCar(BLonString v) {
      this.set(addrDestP2MMaskCar, v, null);
   }

   public BLonString getAddrDestP2MMaskLocation() {
      return (BLonString)this.get(addrDestP2MMaskLocation);
   }

   public void setAddrDestP2MMaskLocation(BLonString v) {
      this.set(addrDestP2MMaskLocation, v, null);
   }

   public BLonString getAddrDestP2MMaskAudio() {
      return (BLonString)this.get(addrDestP2MMaskAudio);
   }

   public void setAddrDestP2MMaskAudio(BLonString v) {
      this.set(addrDestP2MMaskAudio, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(audioLine, out);
      this.primitiveToOutputStream(duplexFull, out);
      this.primitiveToOutputStream(destP2P, out);
      this.primitiveToOutputStream(reserved, out);
      this.primitiveToOutputStream(audioType, out);
      this.primitiveToOutputStream(addrInitUnitId, out);
      this.primitiveToOutputStream(addrInitLocation, out);
      this.primitiveToOutputStream(addrInitCarId, out);
      this.primitiveToOutputStream(addrInitReserved, out);
      this.primitiveToOutputStream(addrInitAudioSensorType, out);
      this.primitiveToOutputStream(addrTalkUnitId, out);
      this.primitiveToOutputStream(addrTalkLocation, out);
      this.primitiveToOutputStream(addrTalkCarId, out);
      this.primitiveToOutputStream(addrTalkReserved, out);
      this.primitiveToOutputStream(addrTalkAudioSensorType, out);
      if (this.getDestP2P().getBoolean()) {
         this.primitiveToOutputStream(addrDestP2PUnitId, out);
         this.primitiveToOutputStream(addrDestP2PLocation, out);
         this.primitiveToOutputStream(addrDestP2PCarId, out);
         this.primitiveToOutputStream(addrDestP2PReserved, out);
         this.primitiveToOutputStream(addrDestP2PAudioSensorType, out);
      } else {
         this.primitiveToOutputStream(addrDestP2MMaskUnit, out);
         this.primitiveToOutputStream(addrDestP2MMaskCar, out);
         this.primitiveToOutputStream(addrDestP2MMaskLocation, out);
         this.primitiveToOutputStream(addrDestP2MMaskAudio, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(audioLine, in);
      this.primitiveFromInputStream(duplexFull, in);
      this.primitiveFromInputStream(destP2P, in);
      this.primitiveFromInputStream(reserved, in);
      this.primitiveFromInputStream(audioType, in);
      this.primitiveFromInputStream(addrInitUnitId, in);
      this.primitiveFromInputStream(addrInitLocation, in);
      this.primitiveFromInputStream(addrInitCarId, in);
      this.primitiveFromInputStream(addrInitReserved, in);
      this.primitiveFromInputStream(addrInitAudioSensorType, in);
      this.primitiveFromInputStream(addrTalkUnitId, in);
      this.primitiveFromInputStream(addrTalkLocation, in);
      this.primitiveFromInputStream(addrTalkCarId, in);
      this.primitiveFromInputStream(addrTalkReserved, in);
      this.primitiveFromInputStream(addrTalkAudioSensorType, in);
      if (this.getDestP2P().getBoolean()) {
         this.primitiveFromInputStream(addrDestP2PUnitId, in);
         this.primitiveFromInputStream(addrDestP2PLocation, in);
         this.primitiveFromInputStream(addrDestP2PCarId, in);
         this.primitiveFromInputStream(addrDestP2PReserved, in);
         this.primitiveFromInputStream(addrDestP2PAudioSensorType, in);
      } else {
         this.primitiveFromInputStream(addrDestP2MMaskUnit, in);
         this.primitiveFromInputStream(addrDestP2MMaskCar, in);
         this.primitiveFromInputStream(addrDestP2MMaskLocation, in);
         this.primitiveFromInputStream(addrDestP2MMaskAudio, in);
      }
   }
}
