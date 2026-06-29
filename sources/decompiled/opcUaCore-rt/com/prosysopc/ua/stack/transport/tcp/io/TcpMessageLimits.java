package com.prosysopc.ua.stack.transport.tcp.io;

import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;

public class TcpMessageLimits {
   public static final int MessageTypeAndSize = 8;
   public static final int MinBufferSize = 8192;
   public static final int MaxBufferSize = 147456;
   public static final int MaxErrorReasonLength = 4096;
   public static final int MaxEndpointUrlLength = 4096;
   public static final int MaxCertificateSize = 7500;
   public static final int MaxSecurityPolicyUriSize = 256;
   public static final int BaseHeaderSize = 12;
   public static final int SymmetricHeaderSize = 16;
   public static final int SequenceHeaderSize = 8;
   public static final int CertificateThumbprintSize = 20;
   public static final int StringLengthSize = 4;
   public static final UnsignedInteger MinSequenceNumber = UnsignedInteger.valueOf(4294967295L - UnsignedShort.L_MAX_VALUE);
   public static final UnsignedInteger MaxRolloverSequenceNumber = UnsignedInteger.valueOf(UnsignedShort.L_MAX_VALUE);
   public static final int DefaultMaxBufferSize = 65535;
   public static final int DefaultMaxMessageSize = 1048560;
   public static final int DefaultChannelLifetime = 60000;
   public static final int DefaultSecurityTokenLifeTime = 3600000;
   public static final int MinSecurityTokenLifeTime = 60000;
   public static final int MinTimeBetweenReconnects = 0;
   public static final int MaxTimeBetweenReconnects = 120000;
   public static final double TokenRenewalPeriod = 0.75;
}
