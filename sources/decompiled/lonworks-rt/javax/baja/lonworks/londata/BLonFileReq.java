package javax.baja.lonworks.londata;

import javax.baja.lonworks.datatypes.BAddressEntry;
import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.enums.BLonFileRequestEnum;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "request",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonFileRequestEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "index",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null)")}
   ), @NiagaraProperty(
      name = "recvTimeout",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null)")}
   ), @NiagaraProperty(
      name = "address",
      type = "BLonSimple",
      defaultValue = "BLonSimple.make(BAddressEntry.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.na, 4, null)")}
   ), @NiagaraProperty(
      name = "authenticate",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.FALSE",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.BOOLEAN, null)")}
   ), @NiagaraProperty(
      name = "priority",
      type = "BLonBoolean",
      defaultValue = "BLonBoolean.FALSE",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.BOOLEAN, null)")}
   )})
public class BLonFileReq extends BLonData {
   public static final Property request = newProperty(0, BLonEnum.make(BLonFileRequestEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property index = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null));
   public static final Property recvTimeout = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null));
   public static final Property address = newProperty(0, BLonSimple.make(BAddressEntry.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.na, 4, null));
   public static final Property authenticate = newProperty(0, BLonBoolean.FALSE, LonFacetsUtil.makeFacets(BLonElementQualifiers.BOOLEAN, null));
   public static final Property priority = newProperty(0, BLonBoolean.FALSE, LonFacetsUtil.makeFacets(BLonElementQualifiers.BOOLEAN, null));
   public static final Type TYPE = Sys.loadType(BLonFileReq.class);

   public BLonEnum getRequest() {
      return (BLonEnum)this.get(request);
   }

   public void setRequest(BLonEnum v) {
      this.set(request, v, null);
   }

   public BLonInteger getIndex() {
      return (BLonInteger)this.get(index);
   }

   public void setIndex(BLonInteger v) {
      this.set(index, v, null);
   }

   public BLonInteger getRecvTimeout() {
      return (BLonInteger)this.get(recvTimeout);
   }

   public void setRecvTimeout(BLonInteger v) {
      this.set(recvTimeout, v, null);
   }

   public BLonSimple getAddress() {
      return (BLonSimple)this.get(address);
   }

   public void setAddress(BLonSimple v) {
      this.set(address, v, null);
   }

   public BLonBoolean getAuthenticate() {
      return (BLonBoolean)this.get(authenticate);
   }

   public void setAuthenticate(BLonBoolean v) {
      this.set(authenticate, v, null);
   }

   public BLonBoolean getPriority() {
      return (BLonBoolean)this.get(priority);
   }

   public void setPriority(BLonBoolean v) {
      this.set(priority, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }
}
