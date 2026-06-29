package com.tridium.opcUaClient.util;

import com.prosysopc.ua.client.ConnectException;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.client.UaClientListener;
import com.prosysopc.ua.stack.application.Session;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.core.MessageSecurityMode;
import com.prosysopc.ua.stack.core.PublishRequest;
import com.prosysopc.ua.stack.core.PublishResponse;
import com.prosysopc.ua.stack.core.RepublishResponse;

public class OpcUaClientListener implements UaClientListener {
   private static final long ALLOWED_PUBLISHTIME_DIFFERENCE = 3600000L;
   private static final boolean publishTimeOverride = false;
   private static final double UNREALISTIC_LONG_TIMEOUT = 8.64E7;
   private static final double UNREALISTIC_SHORT_TIMEOUT = 10000.0;

   public void onAfterCreateSessionChannel(UaClient client, Session session) throws ConnectException {
      Session s = client.getSession();
      if (s.getSessionTimeout() <= 10000.0) {
         throw new ConnectException("The RevisedSessionTimeout is unrealistically short: " + s.getSessionTimeout(), "", null);
      } else if (s.getSessionTimeout() >= 8.64E7) {
         throw new ConnectException("The RevisedSessionTimeout is unrealistically long: " + s.getSessionTimeout(), "", null);
      } else if ((s.getServerNonce() == null || s.getServerNonce().getLength() < 32)
         && MessageSecurityMode.None != client.getSecurityMode().getMessageSecurityMode()) {
         throw new ConnectException("The serverNonce is less than 32 bytes", "", null);
      }
   }

   public void onBeforePublishRequest(UaClient client, PublishRequest publishRequest) {
   }

   public boolean validatePublishResponse(UaClient client, PublishResponse response) {
      return validatePublishTime(response.getNotificationMessage().getPublishTime());
   }

   public boolean validateRepublishResponse(UaClient client, RepublishResponse response) {
      return validatePublishTime(response.getNotificationMessage().getPublishTime());
   }

   private static void pl(String line) {
      System.out.println(line);
   }

   private static boolean validatePublishTime(DateTime publishTime) {
      long diff = Math.abs(DateTime.currentTime().getTimeInMillis() - publishTime.getTimeInMillis());
      if (diff > 3600000L && !publishTime.equals(DateTime.MIN_VALUE) && !publishTime.equals(DateTime.MAX_VALUE)) {
         pl(String.format("PublishResponse PublishTime difference to current time more than allowed, discarding data, (%sms vs %sms)", diff, 3600000L));
         return false;
      } else {
         return true;
      }
   }
}
