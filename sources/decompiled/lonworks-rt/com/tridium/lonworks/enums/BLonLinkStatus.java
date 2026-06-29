package com.tridium.lonworks.enums;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("unbound"), @Range("newLink"), @Range("bound"), @Range("obsolete"), @Range("error"), @Range("comError"), @Range("deviceError"), @Range("groupError"), @Range("maxCritError"), @Range("dirtyGroup"), @Range("serviceTypeError"), @Range("authicateError"), @Range("priorityError"), @Range("nvTypeError"), @Range("pollOnly"), @Range("dirtyPoll"), @Range("aliasError"), @Range("groupExcludeError"), @Range("descriptorError"), @Range("dirtyDescriptor")}
)
public final class BLonLinkStatus extends BFrozenEnum {
   public static final int UNBOUND = 0;
   public static final int NEW_LINK = 1;
   public static final int BOUND = 2;
   public static final int OBSOLETE = 3;
   public static final int ERROR = 4;
   public static final int COM_ERROR = 5;
   public static final int DEVICE_ERROR = 6;
   public static final int GROUP_ERROR = 7;
   public static final int MAX_CRIT_ERROR = 8;
   public static final int DIRTY_GROUP = 9;
   public static final int SERVICE_TYPE_ERROR = 10;
   public static final int AUTHICATE_ERROR = 11;
   public static final int PRIORITY_ERROR = 12;
   public static final int NV_TYPE_ERROR = 13;
   public static final int POLL_ONLY = 14;
   public static final int DIRTY_POLL = 15;
   public static final int ALIAS_ERROR = 16;
   public static final int GROUP_EXCLUDE_ERROR = 17;
   public static final int DESCRIPTOR_ERROR = 18;
   public static final int DIRTY_DESCRIPTOR = 19;
   public static final BLonLinkStatus unbound = new BLonLinkStatus(0);
   public static final BLonLinkStatus newLink = new BLonLinkStatus(1);
   public static final BLonLinkStatus bound = new BLonLinkStatus(2);
   public static final BLonLinkStatus obsolete = new BLonLinkStatus(3);
   public static final BLonLinkStatus error = new BLonLinkStatus(4);
   public static final BLonLinkStatus comError = new BLonLinkStatus(5);
   public static final BLonLinkStatus deviceError = new BLonLinkStatus(6);
   public static final BLonLinkStatus groupError = new BLonLinkStatus(7);
   public static final BLonLinkStatus maxCritError = new BLonLinkStatus(8);
   public static final BLonLinkStatus dirtyGroup = new BLonLinkStatus(9);
   public static final BLonLinkStatus serviceTypeError = new BLonLinkStatus(10);
   public static final BLonLinkStatus authicateError = new BLonLinkStatus(11);
   public static final BLonLinkStatus priorityError = new BLonLinkStatus(12);
   public static final BLonLinkStatus nvTypeError = new BLonLinkStatus(13);
   public static final BLonLinkStatus pollOnly = new BLonLinkStatus(14);
   public static final BLonLinkStatus dirtyPoll = new BLonLinkStatus(15);
   public static final BLonLinkStatus aliasError = new BLonLinkStatus(16);
   public static final BLonLinkStatus groupExcludeError = new BLonLinkStatus(17);
   public static final BLonLinkStatus descriptorError = new BLonLinkStatus(18);
   public static final BLonLinkStatus dirtyDescriptor = new BLonLinkStatus(19);
   public static final BLonLinkStatus DEFAULT = unbound;
   public static final Type TYPE = Sys.loadType(BLonLinkStatus.class);

   public static BLonLinkStatus make(int ordinal) {
      return (BLonLinkStatus)unbound.getRange().get(ordinal, false);
   }

   public static BLonLinkStatus make(String tag) {
      return (BLonLinkStatus)unbound.getRange().get(tag);
   }

   private BLonLinkStatus(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }

   public boolean isError() {
      return this.getOrdinal() >= 4;
   }

   public boolean isBound() {
      return this.getOrdinal() == 2;
   }

   public boolean isNew() {
      return this.getOrdinal() == 1;
   }
}
