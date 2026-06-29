package javax.baja.lonworks.londata;

import javax.baja.lonworks.enums.BLonElementType;
import javax.baja.lonworks.enums.BLonFileStatusEnum;
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
      name = "fileStatus",
      type = "BLonEnum",
      defaultValue = "BLonEnum.make(BLonFileStatusEnum.DEFAULT)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.e8, null)")}
   ), @NiagaraProperty(
      name = "numberOfFiles",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null)")}
   ), @NiagaraProperty(
      name = "selectedFile",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null)")}
   ), @NiagaraProperty(
      name = "fileInfo",
      type = "BLonString",
      defaultValue = "BLonString.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.st,16,null)")}
   ), @NiagaraProperty(
      name = "size",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.s32, null)")}
   ), @NiagaraProperty(
      name = "fileType",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null)")}
   ), @NiagaraProperty(
      name = "domainId",
      type = "BLonByteArray",
      defaultValue = "BLonByteArray.make(6)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.na,6,null)")}
   ), @NiagaraProperty(
      name = "domainLength",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 6, 1, null)")}
   ), @NiagaraProperty(
      name = "subnet",
      type = "BLonInteger",
      defaultValue = "BLonInteger.make(1)",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 255, 1, null)")}
   ), @NiagaraProperty(
      name = "node",
      type = "BLonInteger",
      defaultValue = "BLonInteger.DEFAULT",
      facets = {@Facet("LonFacetsUtil.makeFacets(BLonElementType.u8, 1, 127, 1, null)")}
   )})
public class BLonFileStatus extends BLonData {
   public static final Property fileStatus = newProperty(0, BLonEnum.make(BLonFileStatusEnum.DEFAULT), LonFacetsUtil.makeFacets(BLonElementType.e8, null));
   public static final Property numberOfFiles = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null));
   public static final Property selectedFile = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null));
   public static final Property fileInfo = newProperty(0, BLonString.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.st, 16, null));
   public static final Property size = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.s32, null));
   public static final Property fileType = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementQualifiers.UNSIGNED_LONG1, null));
   public static final Property domainId = newProperty(0, BLonByteArray.make(6), LonFacetsUtil.makeFacets(BLonElementType.na, 6, null));
   public static final Property domainLength = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 6, 1, null));
   public static final Property subnet = newProperty(0, BLonInteger.make(1), LonFacetsUtil.makeFacets(BLonElementType.u8, 0, 255, 1, null));
   public static final Property node = newProperty(0, BLonInteger.DEFAULT, LonFacetsUtil.makeFacets(BLonElementType.u8, 1, 127, 1, null));
   public static final Type TYPE = Sys.loadType(BLonFileStatus.class);

   public BLonEnum getFileStatus() {
      return (BLonEnum)this.get(fileStatus);
   }

   public void setFileStatus(BLonEnum v) {
      this.set(fileStatus, v, null);
   }

   public BLonInteger getNumberOfFiles() {
      return (BLonInteger)this.get(numberOfFiles);
   }

   public void setNumberOfFiles(BLonInteger v) {
      this.set(numberOfFiles, v, null);
   }

   public BLonInteger getSelectedFile() {
      return (BLonInteger)this.get(selectedFile);
   }

   public void setSelectedFile(BLonInteger v) {
      this.set(selectedFile, v, null);
   }

   public BLonString getFileInfo() {
      return (BLonString)this.get(fileInfo);
   }

   public void setFileInfo(BLonString v) {
      this.set(fileInfo, v, null);
   }

   public BLonInteger getSize() {
      return (BLonInteger)this.get(size);
   }

   public void setSize(BLonInteger v) {
      this.set(size, v, null);
   }

   public BLonInteger getFileType() {
      return (BLonInteger)this.get(fileType);
   }

   public void setFileType(BLonInteger v) {
      this.set(fileType, v, null);
   }

   public BLonByteArray getDomainId() {
      return (BLonByteArray)this.get(domainId);
   }

   public void setDomainId(BLonByteArray v) {
      this.set(domainId, v, null);
   }

   public BLonInteger getDomainLength() {
      return (BLonInteger)this.get(domainLength);
   }

   public void setDomainLength(BLonInteger v) {
      this.set(domainLength, v, null);
   }

   public BLonInteger getSubnet() {
      return (BLonInteger)this.get(subnet);
   }

   public void setSubnet(BLonInteger v) {
      this.set(subnet, v, null);
   }

   public BLonInteger getNode() {
      return (BLonInteger)this.get(node);
   }

   public void setNode(BLonInteger v) {
      this.set(node, v, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      this.primitiveToOutputStream(fileStatus, out);
      this.primitiveToOutputStream(numberOfFiles, out);
      this.primitiveToOutputStream(selectedFile, out);
      int st = this.getFileStatus().getEnum().getOrdinal();
      if (st == 1) {
         this.primitiveToOutputStream(fileInfo, out);
         this.primitiveToOutputStream(size, out);
         this.primitiveToOutputStream(fileType, out);
      } else {
         this.primitiveToOutputStream(domainId, out);
         this.primitiveToOutputStream(domainLength, out);
         this.primitiveToOutputStream(subnet, out);
         this.primitiveToOutputStream(node, out);
      }
   }

   @Override
   public void fromInputStream(LonInputStream in) {
      this.primitiveFromInputStream(fileStatus, in);
      this.primitiveFromInputStream(numberOfFiles, in);
      this.primitiveFromInputStream(selectedFile, in);
      int st = this.getFileStatus().getEnum().getOrdinal();
      if (st == 1) {
         this.primitiveFromInputStream(fileInfo, in);
         this.primitiveFromInputStream(size, in);
         this.primitiveFromInputStream(fileType, in);
      } else {
         this.primitiveFromInputStream(domainId, in);
         this.primitiveFromInputStream(domainLength, in);
         this.primitiveFromInputStream(subnet, in);
         this.primitiveFromInputStream(node, in);
      }
   }
}
