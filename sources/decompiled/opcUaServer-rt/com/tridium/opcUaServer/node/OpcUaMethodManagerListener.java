package com.tridium.opcUaServer.node;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.server.CallableListener;
import com.prosysopc.ua.server.MethodManager;
import com.prosysopc.ua.server.ServiceContext;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.StatusCodes;

public class OpcUaMethodManagerListener implements CallableListener {
   private final UaNode myMethod;

   public OpcUaMethodManagerListener(UaNode myMethod) {
      this.myMethod = myMethod;
   }

   public boolean onCall(
      ServiceContext serviceContext,
      NodeId objectId,
      UaNode object,
      NodeId methodId,
      UaMethod method,
      Variant[] inputArguments,
      StatusCode[] inputArgumentResults,
      DiagnosticInfo[] inputArgumentDiagnosticInfos,
      Variant[] outputs
   ) throws StatusException {
      if (methodId.equals(this.myMethod.getNodeId())) {
         MethodManager.checkInputArguments(new Class[]{String.class, Double.class}, inputArguments, inputArgumentResults, inputArgumentDiagnosticInfos, false);

         String operation;
         try {
            operation = (String)inputArguments[0].getValue();
         } catch (ClassCastException var16) {
            throw this.inputError(0, var16.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
         }

         double input;
         try {
            input = inputArguments[1].intValue();
         } catch (ClassCastException var15) {
            throw this.inputError(1, var15.getMessage(), inputArgumentResults, inputArgumentDiagnosticInfos);
         }

         operation = operation.toLowerCase();
         double result;
         if (operation.equals("sin")) {
            result = Math.sin(Math.toRadians(input));
         } else if (operation.equals("cos")) {
            result = Math.cos(Math.toRadians(input));
         } else if (operation.equals("tan")) {
            result = Math.tan(Math.toRadians(input));
         } else {
            if (!operation.equals("pow")) {
               throw this.inputError(
                  0, "Unknown function '" + operation + "': valid functions are sin, cos, tan, pow", inputArgumentResults, inputArgumentDiagnosticInfos
               );
            }

            result = input * input;
         }

         outputs[0] = new Variant(result);
         return true;
      } else {
         return false;
      }
   }

   private StatusException inputError(int index, String message, StatusCode[] inputArgumentResults, DiagnosticInfo[] inputArgumentDiagnosticInfos) {
      inputArgumentResults[index] = StatusCode.valueOf(StatusCodes.Bad_InvalidArgument);
      DiagnosticInfo di = new DiagnosticInfo();
      di.setAdditionalInfo(message);
      inputArgumentDiagnosticInfos[index] = di;
      return new StatusException(StatusCodes.Bad_InvalidArgument);
   }
}
