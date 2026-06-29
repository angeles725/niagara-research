package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonControlRespEnum;
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
      name = "status",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonControlRespEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "senderId",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, 1, 65534, 1, 65535, 1, 0, null)")}
   ), @NiagaraProperty(
      name = "senderRangeLower",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, 1, 65534, 1, 65535, 1, 0, null)")}
   ), @NiagaraProperty(
      name = "senderRangeUpper",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, 1, 65534, 1, 65535, 3, 0, null)")}
   ), @NiagaraProperty(
      name = "controllerId",
      type = "BLonFloat",
      defaultValue = "BLonFloat.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u16, 1, 65534, 1, 65535, 5, 0, null)")}
   )})
public class BLonCtrlResp extends BLonData {
   public static final Property status = newProperty(0, BLonEnum.make(BLonControlRespEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property senderId = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, 1.0F, 65534.0F, 1.0F, 65535.0F, 1, 0, null)
   );
   public static final Property senderRangeLower = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, 1.0F, 65534.0F, 1.0F, 65535.0F, 1, 0, null)
   );
   public static final Property senderRangeUpper = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, 1.0F, 65534.0F, 1.0F, 65535.0F, 3, 0, null)
   );
   public static final Property controllerId = newProperty(
      0, BLonFloat.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u16, 1.0F, 65534.0F, 1.0F, 65535.0F, 5, 0, null)
   );
   public static final Type TYPE = Sys.loadType(BLonCtrlResp.class);

   public BLonEnum getStatus() {
      return (BLonEnum)this.get(status);
   }

   public void setStatus(BLonEnum v) {
      this.set(status, v, null);
   }

   public BLonFloat getSenderId() {
      return (BLonFloat)this.get(senderId);
   }

   public void setSenderId(BLonFloat v) {
      this.set(senderId, v, null);
   }

   public BLonFloat getSenderRangeLower() {
      return (BLonFloat)this.get(senderRangeLower);
   }

   public void setSenderRangeLower(BLonFloat v) {
      this.set(senderRangeLower, v, null);
   }

   public BLonFloat getSenderRangeUpper() {
      return (BLonFloat)this.get(senderRangeUpper);
   }

   public void setSenderRangeUpper(BLonFloat v) {
      this.set(senderRangeUpper, v, null);
   }

   public BLonFloat getControllerId() {
      return (BLonFloat)this.get(controllerId);
   }

   public void setControllerId(BLonFloat v) {
      this.set(controllerId, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(status, out);
      int st = this.getStatus().getEnum().getOrdinal();
      if (st == 4) {
         this.primitiveToOutputStream(senderRangeLower, out);
         this.primitiveToOutputStream(senderRangeUpper, out);
      } else {
         this.primitiveToOutputStream(senderId, out);
         out.writeUnsigned16(0);
      }

      this.primitiveToOutputStream(controllerId, out);
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(status, in);
      int st = this.getStatus().getEnum().getOrdinal();
      if (st == 4) {
         this.primitiveFromInputStream(senderRangeLower, in);
         this.primitiveFromInputStream(senderRangeUpper, in);
      } else {
         this.primitiveFromInputStream(senderId, in);
         in.readUnsigned16();
      }

      this.primitiveFromInputStream(controllerId, in);
   }
}
