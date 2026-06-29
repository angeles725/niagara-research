package com.tridium.bacnet.services.unconfirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.services.BacnetUnconfirmedRequest;
import java.util.ArrayList;
import java.util.List;
import javax.baja.bacnet.datatypes.BBacnetGroupChannelValue;
import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.RejectException;

public class WriteGroupRequest extends BacnetUnconfirmedRequest {
   private long groupNumber;
   private int writePriority;
   private BBacnetGroupChannelValue[] changeList;
   private Boolean inhibitDelay = null;
   private static int GROUP_NUMBER_TAG = 0;
   private static int WRITE_PRIORITY_TAG = 1;
   private static int CHANGE_LIST_TAG = 2;
   private static int INHIBIT_DELAY_TAG = 3;

   public WriteGroupRequest() {
      super(10);
   }

   public WriteGroupRequest(long groupNumber, int writePriority, BBacnetGroupChannelValue[] changeList, Boolean inhibitDelay) {
      super(10);
      this.groupNumber = groupNumber;
      this.writePriority = writePriority;
      this.changeList = changeList;
      this.inhibitDelay = inhibitDelay;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeUnsignedInteger(GROUP_NUMBER_TAG, this.groupNumber);
      out.writeUnsignedInteger(WRITE_PRIORITY_TAG, this.writePriority);
      out.writeOpeningTag(CHANGE_LIST_TAG);

      for (BBacnetGroupChannelValue change : this.changeList) {
         change.writeAsn(out);
      }

      out.writeClosingTag(CHANGE_LIST_TAG);
      if (this.inhibitDelay != null) {
         out.writeBoolean(INHIBIT_DELAY_TAG, this.inhibitDelay);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException, RejectException {
      this.groupNumber = in.readUnsignedInteger(GROUP_NUMBER_TAG);
      this.writePriority = (int)in.readUnsignedInteger(WRITE_PRIORITY_TAG);
      in.skipOpeningTag(CHANGE_LIST_TAG);
      List<BBacnetGroupChannelValue> changeList = new ArrayList<>();

      do {
         BBacnetGroupChannelValue change = new BBacnetGroupChannelValue();
         change.readAsn(in);
         changeList.add(change);
      } while (!in.isClosingTag(CHANGE_LIST_TAG));

      in.skipClosingTag(CHANGE_LIST_TAG);
      this.changeList = changeList.toArray(new BBacnetGroupChannelValue[0]);
      if (in.peekTag() == INHIBIT_DELAY_TAG) {
         this.inhibitDelay = in.readBoolean(INHIBIT_DELAY_TAG);
      }
   }

   public long getGroupNumber() {
      return this.groupNumber;
   }

   public int getWritePriority() {
      return this.writePriority;
   }

   public BBacnetGroupChannelValue[] getChangeList() {
      return this.changeList;
   }

   public Boolean getInhibitDelay() {
      return this.inhibitDelay;
   }
}
