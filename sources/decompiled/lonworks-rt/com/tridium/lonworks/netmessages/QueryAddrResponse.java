package com.tridium.lonworks.netmessages;

import javax.baja.lonworks.InvalidResponseException;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.LonMessage;
import javax.baja.lonworks.enums.BAddressType;
import javax.baja.lonworks.enums.BLonGroupRestrictionEnum;
import javax.baja.lonworks.enums.BLonReceiveTimer;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;

public class QueryAddrResponse extends LonMessage implements NetMessages {
   public int addrType;
   public int domainIndex;
   public int memberOrNode;
   public int repeatTimer;
   public int retryCount;
   public int receiveTimer;
   public int xmitTimer;
   public int groupOrSubnet;
   public BLonGroupRestrictionEnum restriction = BLonGroupRestrictionEnum.grpNormal;

   public QueryAddrResponse() {
   }

   public QueryAddrResponse(LonInputStream in) throws LonException {
      this.code = 39;
      this.fromInputStream(in);
   }

   @Override
   public int getDomainIndex() {
      return this.domainIndex;
   }

   public int getMemberOrNode() {
      return this.memberOrNode;
   }

   public int getNodeId() {
      return this.memberOrNode;
   }

   @Override
   public BLonRepeatTimer getRepeatTimer() {
      return BLonRepeatTimer.make(this.repeatTimer);
   }

   @Override
   public int getRetryCount() {
      return this.retryCount;
   }

   public BLonReceiveTimer getReceiveTimer() {
      return BLonReceiveTimer.make(this.receiveTimer);
   }

   public BLonRepeatTimer getXmitTimer() {
      return BLonRepeatTimer.make(this.xmitTimer);
   }

   public int getGroupOrSubnet() {
      return this.groupOrSubnet;
   }

   public int getSubnetId() {
      return this.groupOrSubnet;
   }

   public BLonGroupRestrictionEnum getRestriction() {
      return this.restriction;
   }

   public boolean isExtended() {
      return false;
   }

   public BAddressType getAddressType() {
      if ((this.addrType & 128) != 0) {
         return BAddressType.group;
      } else if (this.addrType == 1) {
         return BAddressType.subnetNode;
      } else if (this.addrType == 3) {
         return BAddressType.broadcast;
      } else {
         return this.addrType == 0 && this.memberOrNode == 1 ? BAddressType.turnaround : BAddressType.none;
      }
   }

   public int getSize() {
      return (this.addrType & 128) != 0 ? this.addrType & -129 : -1;
   }

   @Override
   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      out.write(this.addrType);
      int data = this.domainIndex << 7 | this.memberOrNode;
      out.write(data);
      data = this.repeatTimer << 4 | this.retryCount;
      out.write(data);
      data = this.receiveTimer << 4 | this.xmitTimer;
      out.write(data);
      out.write(this.groupOrSubnet);
   }

   @Override
   public void fromInputStream(LonInputStream in) throws LonException {
      int code = in.readUnsigned8();
      if (code != 39) {
         throw new InvalidResponseException(code);
      } else {
         this.addrType = in.readUnsigned8();
         int data = in.readUnsigned8();
         this.memberOrNode = data & 127;
         this.domainIndex = (data & 128) >> 7;
         data = in.readUnsigned8();
         this.retryCount = data & 15;
         this.repeatTimer = (data & 240) >> 4;
         data = in.readUnsigned8();
         this.xmitTimer = data & 15;
         this.receiveTimer = (data & 240) >> 4;
         this.groupOrSubnet = in.readUnsigned8();
      }
   }
}
