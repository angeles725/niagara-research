package com.tridium.bacnet.stack.link.sc.message;

import com.tridium.bacnet.stack.link.sc.connection.ScDataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.baja.bacnet.enums.BBacnetErrorCode;
import javax.baja.nre.util.ByteBuffer;

public final class AddressResolutionAck extends AddressedMessage {
   private List<String> webSocketUris;

   AddressResolutionAck() {
   }

   private AddressResolutionAck(int messageId) {
      super(messageId);
   }

   public static AddressResolutionAck make(int messageId, List<String> webSocketUris) {
      ScMessageUtil.checkUnsignedShort(messageId, "messageId");
      if (webSocketUris == null) {
         webSocketUris = Collections.emptyList();
      }

      AddressResolutionAck message = new AddressResolutionAck(messageId);
      message.webSocketUris = webSocketUris;
      return message;
   }

   @Override
   public void encode(ScDataOutputStream out) throws IOException {
      super.encode(out);
      byte[] webSocketUrisEncoded = String.join(" ", this.webSocketUris).getBytes(StandardCharsets.UTF_8);
      out.write(webSocketUrisEncoded);
   }

   @Override
   protected void decodePayload(ByteBuffer in) throws ScReadMessageException {
      try {
         String uris = ScMessageUtil.readString(in).trim();
         if (uris.isEmpty()) {
            this.webSocketUris = Collections.emptyList();
         } else {
            String[] urisSplit = uris.split(" ");

            for (String uri : urisSplit) {
               String error = ScMessageUtil.checkWebSocketUri(uri);
               if (error != null) {
                  throw new ScReadMessageException("Web Socket URI \"" + uri + "\" is invalid: " + error, BBacnetErrorCode.inconsistentParameters);
               }
            }

            this.webSocketUris = Arrays.asList(urisSplit);
         }
      } catch (IOException var9) {
         throw new ScReadMessageException("Message payload is incomplete", BBacnetErrorCode.messageIncomplete);
      }
   }

   public List<String> getWebSocketUris() {
      return this.webSocketUris;
   }

   @Override
   public int getFunction() {
      return 3;
   }

   @Override
   public String toString() {
      return super.toString() + "; web socket URIs: " + String.join(", ", this.webSocketUris);
   }
}
