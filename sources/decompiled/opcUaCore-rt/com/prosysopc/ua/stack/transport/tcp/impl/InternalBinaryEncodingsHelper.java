package com.prosysopc.ua.stack.transport.tcp.impl;

import com.prosysopc.ua.UaIds;
import com.prosysopc.ua.UaNodeId;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.ServiceRequest;
import com.prosysopc.ua.stack.builtintypes.ServiceResponse;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.common.ServiceResultException;
import com.prosysopc.ua.stack.encoding.DecodingException;
import com.prosysopc.ua.stack.encoding.EncodingException;
import com.prosysopc.ua.stack.encoding.IEncodeable;
import com.prosysopc.ua.stack.encoding.binary.BinaryDecoder;
import com.prosysopc.ua.stack.encoding.binary.BinaryEncoder;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.prosysopc.ua.typedictionary.UaDataTypeSpecification;
import com.prosysopc.ua.types.opcua.CommonInformationModel;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InternalBinaryEncodingsHelper {
   private static final Map<UaNodeId, StructureSpecification> xX;

   public static IEncodeable getMessage(BinaryDecoder var0) throws DecodingException {
      NodeId var1 = var0.getNodeId(null);
      if (var1 == null) {
         throw new DecodingException("Cannot decode " + var1);
      } else if (var1.getNamespaceIndex() != 0) {
         throw new DecodingException("Not valid 'Message' type: " + var1);
      } else {
         UaNodeId var2 = UaNodeId.fromStandard(var1);
         if (!xX.containsKey(var2)) {
            throw new DecodingException("Not valid 'Message' type: " + var1);
         } else {
            return var0.get(null, var2, 0);
         }
      }
   }

   public static ServiceRequest<?> getServiceRequest(BinaryDecoder var0) throws DecodingException {
      try {
         return ServiceRequest.class.cast(getMessage(var0));
      } catch (ClassCastException var2) {
         throw new DecodingException("Expected ServiceRequest, but wasn't", var2);
      }
   }

   public static ServiceResponse getServiceResponse(BinaryDecoder var0) throws DecodingException {
      try {
         return ServiceResponse.class.cast(getMessage(var0));
      } catch (ClassCastException var2) {
         throw new DecodingException("Expected ServiceResponse, but wasn't", var2);
      }
   }

   public static <T extends AbstractUaTcpCommMessage> T getUaTcpCommMessage(BinaryDecoder var0, Class<T> var1) throws DecodingException {
      if (ErrorMessage.class.equals(var1)) {
         return (T)var1.cast(b(var0));
      } else if (Hello.class.equals(var1)) {
         return (T)var1.cast(c(var0));
      } else if (Acknowledge.class.equals(var1)) {
         return (T)var1.cast(a(var0));
      } else if (ReverseHello.class.equals(var1)) {
         return (T)var1.cast(d(var0));
      } else {
         throw new DecodingException("Encountered unknown opc.tcp comm structure: " + var1);
      }
   }

   public static void putServiceRequest(BinaryEncoder var0, ServiceRequest<?> var1) throws EncodingException {
      if (var1 == null) {
         throw new IllegalStateException("Cannot encode null ServiceRequest");
      } else {
         a(var0, var1);
      }
   }

   public static void putServiceResponse(BinaryEncoder var0, ServiceResponse var1) throws EncodingException {
      if (var1 == null) {
         throw new IllegalStateException("Cannot encode null ServiceResponse");
      } else {
         a(var0, var1);
      }
   }

   public static void putUaTcpCommMessage(BinaryEncoder var0, AbstractUaTcpCommMessage var1) throws EncodingException {
      if (var1 == null) {
         throw new EncodingException("Cannot encode null opc.tcp comm structure");
      } else {
         if (var1 instanceof ErrorMessage) {
            a(var0, (ErrorMessage)var1);
         } else if (var1 instanceof Hello) {
            a(var0, (Hello)var1);
         } else if (var1 instanceof Acknowledge) {
            a(var0, (Acknowledge)var1);
         } else {
            if (!(var1 instanceof ReverseHello)) {
               throw new EncodingException("Encountered unknown opc.tcp comm structure: " + var1.getClass());
            }

            a(var0, (ReverseHello)var1);
         }
      }
   }

   private static void c(StructureSpecification var0) {
   }

   private static Acknowledge a(BinaryDecoder var0) throws DecodingException {
      Acknowledge var1 = new Acknowledge();
      var1.setProtocolVersion(var0.getUInt32("ProtocolVersion"));
      var1.setReceiveBufferSize(var0.getUInt32("ReceiveBufferSize"));
      var1.setSendBufferSize(var0.getUInt32("SendBufferSize"));
      var1.setMaxMessageSize(var0.getUInt32("MaxMessageSize"));
      var1.setMaxChunkCount(var0.getUInt32("MaxChunkCount"));
      return var1;
   }

   private static ErrorMessage b(BinaryDecoder var0) throws DecodingException {
      ErrorMessage var1 = new ErrorMessage();
      var1.setError(var0.getUInt32("Error"));
      var1.setReason(var0.getString("Reason"));
      return var1;
   }

   private static Hello c(BinaryDecoder var0) throws DecodingException {
      Hello var1 = new Hello();
      var1.setProtocolVersion(var0.getUInt32("ProtocolVersion"));
      var1.setReceiveBufferSize(var0.getUInt32("ReceiveBufferSize"));
      var1.setSendBufferSize(var0.getUInt32("SendBufferSize"));
      var1.setMaxMessageSize(var0.getUInt32("MaxMessageSize"));
      var1.setMaxChunkCount(var0.getUInt32("MaxChunkCount"));
      var1.setEndpointUrl(var0.getString("EndpointUrl"));
      return var1;
   }

   private static ReverseHello d(BinaryDecoder var0) throws DecodingException {
      ReverseHello var1 = new ReverseHello();
      var1.setServerUri(var0.getString("ServerUri"));
      var1.setEndpointUrl(var0.getString("EndpointUrl"));
      return var1;
   }

   private static void a(BinaryEncoder var0, Acknowledge var1) throws EncodingException {
      var0.putUInt32(null, var1.getProtocolVersion());
      var0.putUInt32(null, var1.getReceiveBufferSize());
      var0.putUInt32(null, var1.getSendBufferSize());
      var0.putUInt32(null, var1.getMaxMessageSize());
      var0.putUInt32(null, var1.getMaxChunkCount());
   }

   private static void a(BinaryEncoder var0, ErrorMessage var1) throws EncodingException {
      var0.putUInt32(null, var1.getError());
      var0.putString(null, var1.getReason());
   }

   private static void a(BinaryEncoder var0, Hello var1) throws EncodingException {
      var0.putUInt32(null, var1.getProtocolVersion());
      var0.putUInt32(null, var1.getReceiveBufferSize());
      var0.putUInt32(null, var1.getSendBufferSize());
      var0.putUInt32(null, var1.getMaxMessageSize());
      var0.putUInt32(null, var1.getMaxChunkCount());
      var0.putString(null, var1.getEndpointUrl());
   }

   private static void a(BinaryEncoder var0, ReverseHello var1) throws EncodingException {
      var0.putString(null, var1.getServerUri());
      var0.putString(null, var1.getEndpointUrl());
   }

   private static void a(BinaryEncoder var0, Structure var1) throws EncodingException {
      StructureSpecification var2 = var1.specification();

      try {
         NodeId var3 = var2.getBinaryEncodeId().asNodeId(var0.getEncoderContext().getNamespaceTable());
         if (NodeId.isNull(var3)) {
            throw new EncodingException("Cannot determine Message TypeId: " + var3);
         }

         var0.put(null, var3, UaIds.NodeId, 0);
      } catch (ServiceResultException var4) {
         throw new EncodingException(var4);
      }

      var0.put(null, var1, var2.getTypeId(), 0);
   }

   private InternalBinaryEncodingsHelper() {
   }

   static {
      HashMap var0 = new HashMap();

      for (UaDataTypeSpecification var2 : CommonInformationModel.MODEL.getSpecifications().values()) {
         if (var2 instanceof StructureSpecification) {
            StructureSpecification var3 = (StructureSpecification)var2;
            if (ServiceRequest.class.isAssignableFrom(var3.getJavaClass()) || ServiceResponse.class.isAssignableFrom(var3.getJavaClass())) {
               var0.put(var3.getTypeId(), var3);
               var0.put(var3.getBinaryEncodeId(), var3);
            }
         }
      }

      xX = Collections.unmodifiableMap(var0);
   }
}
