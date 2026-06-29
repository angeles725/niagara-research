package com.tridium.opcUaClient.point;

import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.BUaArgumentVector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BStringPoint;
import javax.baja.control.ext.BAbstractProxyExt;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BNumber;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraAction(
   name = "callMethod",
   parameterType = "BVector",
   defaultValue = "new BVector()",
   returnType = "BVector",
   flags = 256
)
public class BOpcUaMethod extends BStringPoint {
   public static final Action callMethod = newAction(256, new BVector(), null);
   public static final Type TYPE = Sys.loadType(BOpcUaMethod.class);
   private static final String PROP_RESULT = "result";
   private static final String PROP_LAST_ARG = "lastArg";
   public static final Logger logger = Logger.getLogger("opcUaClient.point");

   public BVector callMethod(BVector parameter) {
      return (BVector)this.invoke(callMethod, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public void started() throws Exception {
      super.started();
   }

   public BVector doCallMethod(BVector arg) {
      logger.info("BOpcUaMethod.doInvoke(" + arg + ")");
      arg.loadSlots();
      BAbstractProxyExt proxyExt = this.getProxyExt();
      if (proxyExt instanceof BOpcUaMethodProxyExt) {
         BOpcUaMethodProxyExt proxy = (BOpcUaMethodProxyExt)proxyExt;
         BOpcUaDevice device = proxy.getBOpcUaClientDevice();
         NodeId objectNodeId = NodeId.parseNodeId(proxy.getObjectId());
         NodeId methodNodeId = NodeId.parseNodeId(proxy.getUaNodeId());
         Variant[] methodArg = proxy.getInputArgs().makeUserArguments(arg);

         try {
            Variant[] results = OpcUaClientUtil.call(device.uaClient, objectNodeId, methodNodeId, methodArg);
            BUaArgumentVector resultDefs = ((BOpcUaMethodProxyExt)this.getProxyExt()).getResults();
            BVector resultsVector = resultDefs.makeResultVector(results);
            BValue bResult = resultsVector.get("result");
            if (bResult != null) {
               proxy.readOk(new BStatusString(bResult.toString()));
               this.updateResults(bResult);
            }

            Property lastArg = this.getProperty("lastArg");
            if (lastArg == null) {
               this.add("lastArg", arg, 7);
            } else {
               this.set(lastArg, arg);
            }

            return resultsVector;
         } catch (Exception var13) {
            if (logger.isLoggable(Level.FINE)) {
               logger.log(Level.WARNING, "Error in CallMethod.", (Throwable)var13);
            } else {
               logger.log(Level.WARNING, "Error in CallMethod: " + var13.getMessage());
            }
         }
      }

      return null;
   }

   private void updateResults(BValue bResult) {
      BStatusValue sValue = null;
      if (bResult instanceof BNumber) {
         sValue = new BStatusNumeric(((BNumber)bResult).getDouble());
      } else if (bResult instanceof BBoolean) {
         sValue = new BStatusBoolean(((BBoolean)bResult).getBoolean());
      } else if (bResult instanceof BString) {
         sValue = new BStatusString(((BString)bResult).getString());
      }

      BValue resultPropValue = this.get("result");
      if (resultPropValue == null) {
         this.add("result", sValue, 267);
      } else if (sValue != null && resultPropValue.getType().is(sValue.getType())) {
         resultPropValue.asComplex().copyFrom(sValue);
      } else {
         this.set(this.getProperty("result"), sValue);
      }
   }

   public BValue getActionParameterDefault(Action action) {
      if (action.equals(callMethod)) {
         BValue value = this.get("lastArg");
         if (value != null) {
            return value.newCopy(true);
         } else {
            BOpcUaMethodProxyExt proxyExt = (BOpcUaMethodProxyExt)this.getProxyExt();
            return proxyExt.makeDefaultInputArgs();
         }
      } else {
         return action.getParameterDefault();
      }
   }
}
