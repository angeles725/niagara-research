package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;

public class FoxRequest extends FoxMessage {
   public final String channel;
   public final String command;

   public FoxRequest(String channel, String command) {
      super(channel + "." + command);
      this.channel = channel;
      this.command = command;
   }
}
