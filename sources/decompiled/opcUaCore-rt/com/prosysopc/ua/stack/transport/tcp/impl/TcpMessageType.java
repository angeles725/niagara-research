package com.prosysopc.ua.stack.transport.tcp.impl;

public class TcpMessageType {
   public static final int FINAL = 1174405120;
   public static final int CONTINUE = 1124073472;
   public static final int ABORT = 1090519040;
   public static final int MESSAGE_TYPE_MASK = 16777215;
   public static final int CHUNK_TYPE_MASK = -16777216;
   public static final int MESSAGE = 4674381;
   public static final int OPEN = 5132367;
   public static final int CLOSE = 5196867;
   public static final int HELLO = 4998472;
   public static final int REVERSE_HELLO = 4540498;
   public static final int RHEF = 1178945618;
   public static final int ACKNOWLEDGE = 4932417;
   public static final int ERROR = 5395013;
   public static final int MSGC = 1128747853;
   public static final int MSGF = 1179079501;
   public static final int MSGA = 1095193421;
   public static final int OPNC = 1129205839;
   public static final int OPNF = 1179537487;
   public static final int OPNA = 1095651407;
   public static final int HELF = 1179403592;
   public static final int ACKF = 1179337537;
   public static final int ERRF = 1179800133;

   public static boolean continues(int var0) {
      return (var0 & 0xFF000000) == 1124073472;
   }

   public static boolean isAbort(int var0) {
      return (var0 & 0xFF000000) == 1090519040;
   }

   public static boolean isFinal(int var0) {
      return (var0 & 0xFF000000) == 1174405120;
   }
}
