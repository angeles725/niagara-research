package com.tridium.fox.sys;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.MulticastServer;
import java.util.HashMap;

public class Learn {
   public static BLearnStation[] learn(long wait, MulticastServer.RollcallCallback cb) throws Exception {
      MulticastServer server = null;

      try {
         server = new MulticastServer(null);
         server.start();
      } catch (Exception var9) {
         System.err.println("ERROR: Could not create Fox MulticastServer: " + var9);
         throw var9;
      }

      BLearnStation[] e;
      try {
         e = learn(server, wait, cb);
      } finally {
         server.kill();
      }

      return e;
   }

   public static BLearnStation[] learn(MulticastServer server, long wait, MulticastServer.RollcallCallback cb) throws Exception {
      if (server == null) {
         return learn(wait, cb);
      } else {
         FoxMessage[] announcements = server.rollcall(new FoxMessage(), wait, cb);
         HashMap<String, BLearnStation> map = new HashMap<>();

         for (int i = 0; i < announcements.length; i++) {
            try {
               BLearnStation[] stations = BLearnStation.make(announcements[i]);

               for (int j = 0; j < stations.length; j++) {
                  if (map.get(stations[j].getKey()) == null) {
                     map.put(stations[j].getKey(), stations[j]);
                  }
               }
            } catch (Exception var10) {
               var10.printStackTrace();

               try {
                  announcements[i].dump();
               } catch (Exception var9) {
               }
            }
         }

         return map.values().toArray(new BLearnStation[0]);
      }
   }
}
