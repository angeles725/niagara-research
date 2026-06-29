package com.tridium.fox.session;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.message.MessageReader;
import com.tridium.fox.message.MessageWriter;
import com.tridium.fox.message.ReadLimitExceededException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FoxFrame {
   public static final int SYNC = 115;
   public static final int ASYNC = 97;
   public static final int REPLY = 114;
   public static final int ERROR = 101;
   public static final int NULL = 110;
   public static final int KEEPALIVE = 107;
   public static final int CLOSE = 99;
   public final int frameType;
   public final int sequenceNumber;
   public final int replyNumber;
   public final String channel;
   public final String command;
   public final FoxMessage message;
   FoxAsyncCallbacks callbacks;
   FoxFrame next;
   private static final Logger logger = Logger.getLogger(FoxFrame.class.getName());

   public FoxFrame(int frameType, int sequenceNumber, int replyNumber, String channel, String command, FoxMessage message) {
      this.frameType = frameType;
      this.sequenceNumber = sequenceNumber;
      this.replyNumber = replyNumber;
      this.channel = channel;
      this.command = command;
      this.message = message;
   }

   public final void write(MessageWriter out) throws IOException {
      this.writeHeader(out);
      this.message.writeValue(out);
      this.writeFooter(out);
   }

   private final void writeHeader(MessageWriter out) throws IOException {
      out.write(102)
         .write(111)
         .write(120)
         .write(32)
         .write(this.frameType)
         .write(32)
         .writeInt(this.sequenceNumber)
         .write(32)
         .writeInt(this.replyNumber)
         .write(32)
         .writeName(this.channel)
         .write(32)
         .writeName(this.command)
         .write(10);
   }

   private final void writeFooter(MessageWriter out) throws IOException {
      out.write(59).write(59).write(10);
   }

   public static FoxFrame read(MessageReader in) throws IOException {
      int type = 63;
      int seqNo = -1;
      int replyNo = -1;
      String channel = "UNKNOWN";
      String command = "UNKNOWN";

      try {
         in.consume("fox ");
         type = in.read();
         in.consume(32);
         seqNo = in.readInt();
         in.consume(32);
         replyNo = in.readInt();
         in.consume(32);
         channel = in.readName().intern();
         in.consume(32);
         command = in.readName().intern();
         in.consume(10);
         FoxMessage msg;
         if (type == 97 || type == 115 || type == 99) {
            msg = new FoxRequest(channel, command);
         } else if (type == 114) {
            msg = new FoxResponse();
         } else {
            msg = new FoxMessage();
         }

         msg.readValue(in);
         in.consume(59);
         in.consume(59);
         in.consume(10);
         return new FoxFrame(type, seqNo, replyNo, channel, command, msg);
      } catch (ReadLimitExceededException var7) {
         Fox.incrementInvalidFrameCount();
         logger.log(
            Level.FINE,
            String.format(
               "Size limit exceeded while reading Fox frame [type = %c, sequence # = %d, reply # = %d, channel = %s, command = %s]",
               type,
               seqNo,
               replyNo,
               channel,
               command
            ),
            (Throwable)var7
         );
         throw var7;
      }
   }

   @Override
   public String toString() {
      StringBuilder s = new StringBuilder();
      s.append(this.frameType)
         .append(' ')
         .append(this.sequenceNumber)
         .append(' ')
         .append(this.replyNumber)
         .append(' ')
         .append(this.channel)
         .append(' ')
         .append(this.command);
      return s.toString();
   }

   public void dump(OutputStream out) {
      try {
         MessageWriter msgOut = new MessageWriter(out, true);
         this.write(msgOut);
         msgOut.flush();
      } catch (IOException var3) {
         var3.printStackTrace();
         throw new RuntimeException(var3.toString());
      }
   }

   public void dump() {
      this.dump(System.out);
   }
}
