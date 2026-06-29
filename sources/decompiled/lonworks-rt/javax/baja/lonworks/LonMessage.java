package javax.baja.lonworks;

import javax.baja.lonworks.datatypes.LonAddress;
import javax.baja.lonworks.enums.BLonRepeatTimer;
import javax.baja.lonworks.io.LonInputStream;
import javax.baja.lonworks.io.LonOutputStream;
import javax.baja.sys.BajaRuntimeException;

public class LonMessage {
   protected int code;
   private boolean farSide = false;
   private boolean request = true;
   private boolean priority = false;
   private boolean authenticate = false;
   private int domainIndex = 0;
   private LonAddress adr;
   private int tag;
   private int retryCount = -1;
   private BLonRepeatTimer transmitTimer = null;
   private BLonRepeatTimer repeatTimer = null;
   private byte[] messageData = null;
   public static final int MAX_NETMSG_DATA = 228;
   public static final int MAX_MSG_DATA = 228;
   public static final int FAR_SIDE_ESCAPE_CODE = 126;

   public LonMessage() {
   }

   public LonMessage(LonInputStream in) {
      try {
         this.fromInputStream(in);
      } catch (LonException var3) {
      }
   }

   public int getMessageCode() {
      return this.code;
   }

   public void setMessageCode(int code) {
      this.code = code;
   }

   public void setFarSide() {
      this.farSide = true;
   }

   public boolean isFarSide() {
      return this.farSide;
   }

   public void setRequest(boolean req) {
      this.request = req;
   }

   public boolean isRequest() {
      return this.request;
   }

   public void setPriority(boolean priority) {
      this.priority = priority;
   }

   public boolean isPriority() {
      return this.priority;
   }

   public void setAuthenticate(boolean authenticate) {
      this.authenticate = authenticate;
   }

   public boolean isAuthenticate() {
      return this.authenticate;
   }

   public void setSourceAddress(LonAddress a) {
      this.adr = a;
   }

   public LonAddress getSourceAddress() {
      return this.adr;
   }

   public void setTag(int t) {
      this.tag = t;
   }

   public int getTag() {
      return this.tag;
   }

   public void setMessageData(byte[] m) {
      this.messageData = m;
   }

   public byte[] getMessageData() {
      return this.messageData;
   }

   public void setRetryCount(int retryCount) {
      this.retryCount = retryCount > 15 ? 15 : retryCount;
   }

   public int getRetryCount() {
      return this.retryCount;
   }

   public void setTransmitTimer(BLonRepeatTimer t) {
      this.transmitTimer = t;
   }

   public BLonRepeatTimer getTransmitTimer() {
      return this.transmitTimer;
   }

   public void setRepeatTimer(BLonRepeatTimer t) {
      this.repeatTimer = t;
   }

   public BLonRepeatTimer getRepeatTimer() {
      return this.repeatTimer;
   }

   public void setDomainIndex(int ndx) {
      this.domainIndex = ndx;
   }

   public int getDomainIndex() {
      return this.domainIndex;
   }

   public void fromInputStream(LonInputStream in) throws LonException {
      this.code = in.readUnsigned8();
      this.messageData = in.readByteArray();
   }

   public void toOutputStream(LonOutputStream out) {
      out.writeUnsigned8(this.code);
      if (this.messageData != null) {
         out.writeByteArray(this.messageData);
      }
   }

   public LonMessage toResponse(LonInputStream in) throws LonException {
      throw new BajaRuntimeException("Not a request message.");
   }

   protected final void invalidMsgCodeException(int code) throws LonException {
      throw new InvalidResponseException(code);
   }
}
