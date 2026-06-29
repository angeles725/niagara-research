package com.tridium.opcUaClient.util;

import com.prosysopc.ua.client.Subscription;
import com.prosysopc.ua.client.SubscriptionAliveListener;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OpcUaSubscriptionAliveListener implements SubscriptionAliveListener {
   public static final Logger LOGGER = Logger.getLogger("opcUaClient.subscriptionAliveListener");

   public void onAfterCreate(Subscription subscription) {
      if (LOGGER.isLoggable(Level.FINER)) {
         LOGGER.log(Level.FINER, "Subscription onAfterCreate. ID: " + subscription.getSubscriptionId().getValue());
      }
   }

   public void onAlive(Subscription subscription) {
      if (LOGGER.isLoggable(Level.FINER)) {
         LOGGER.log(Level.FINER, "Subscription onAlive. ID: " + subscription.getSubscriptionId().getValue() + " Last Alive: " + subscription.getLastAlive());
      }
   }

   public void onLifetimeTimeout(Subscription subscription) {
      if (LOGGER.isLoggable(Level.FINER)) {
         LOGGER.log(
            Level.FINER, "Subscription onLifetimeTimeout. ID: " + subscription.getSubscriptionId().getValue() + " Last Alive: " + subscription.getLastAlive()
         );
      }
   }

   public void onTimeout(Subscription subscription) {
      if (LOGGER.isLoggable(Level.FINER)) {
         LOGGER.log(Level.FINER, "Subscription onTimeout. ID: " + subscription.getSubscriptionId().getValue() + " Last Alive: " + subscription.getLastAlive());
      }
   }
}
