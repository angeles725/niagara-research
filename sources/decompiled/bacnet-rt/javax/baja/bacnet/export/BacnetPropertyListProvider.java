package javax.baja.bacnet.export;

import com.tridium.bacnet.asn.AsnUtil;
import com.tridium.bacnet.asn.NReadPropertyResult;
import javax.baja.bacnet.BacnetConst;
import javax.baja.bacnet.io.PropertyValue;

public interface BacnetPropertyListProvider extends BacnetConst {
   int[] getPropertyList();

   default PropertyValue readPropertyList(int ndx) {
      int[] propertyList = this.getPropertyList();
      if (ndx == -1) {
         return new NReadPropertyResult(371, ndx, BacnetPropertyList.readAll(propertyList));
      } else if (ndx == 0) {
         byte[] length = AsnUtil.toAsnUnsigned(BacnetPropertyList.size(propertyList));
         return new NReadPropertyResult(371, ndx, length);
      } else if (ndx > BacnetPropertyList.size(propertyList)) {
         return BacnetPropertyList.getInvalidIdx(371, ndx);
      } else {
         try {
            int propId = BacnetPropertyList.read(ndx, propertyList);
            return new NReadPropertyResult(371, ndx, AsnUtil.toAsnEnumerated(propId));
         } catch (Exception var4) {
            return BacnetPropertyList.getInvalidIdx(371, ndx);
         }
      }
   }
}
