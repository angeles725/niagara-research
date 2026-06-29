package com.tridium.opcUaClient.point;

import com.prosysopc.ua.client.AddressSpace;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.MethodArgumentException;
import com.prosysopc.ua.nodes.UaMethod;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.core.Argument;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.BUaArgument;
import com.tridium.opcUaCore.BUaArgumentVector;
import java.util.logging.Logger;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BFacets;
import javax.baja.sys.BInteger;
import javax.baja.sys.BObject;
import javax.baja.sys.BString;
import javax.baja.sys.BValue;
import javax.baja.sys.BVector;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.util.IFuture;
import javax.baja.util.Invocation;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "objectId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "inputArgs",
      type = "BUaArgumentVector",
      defaultValue = "new BUaArgumentVector()"
   ), @NiagaraProperty(
      name = "results",
      type = "BUaArgumentVector",
      defaultValue = "new BUaArgumentVector()"
   )})
@NiagaraAction(
   name = "learnDetail",
   flags = 16
)
public class BOpcUaMethodProxyExt extends BOpcUaClientProxyExt {
   public static final Property objectId = newProperty(0, "", null);
   public static final Property inputArgs = newProperty(0, new BUaArgumentVector(), null);
   public static final Property results = newProperty(0, new BUaArgumentVector(), null);
   public static final Action learnDetail = newAction(16, null);
   public static final Type TYPE = Sys.loadType(BOpcUaMethodProxyExt.class);
   public static final String LEARNED = "learned";
   public static final Logger logger = Logger.getLogger("opcUaClient.point");

   public String getObjectId() {
      return this.getString(objectId);
   }

   public void setObjectId(String v) {
      this.setString(objectId, v, null);
   }

   public BUaArgumentVector getInputArgs() {
      return (BUaArgumentVector)this.get(inputArgs);
   }

   public void setInputArgs(BUaArgumentVector v) {
      this.set(inputArgs, v, null);
   }

   public BUaArgumentVector getResults() {
      return (BUaArgumentVector)this.get(results);
   }

   public void setResults(BUaArgumentVector v) {
      this.set(results, v, null);
   }

   public void learnDetail() {
      this.invoke(learnDetail, null, null);
   }

   @Override
   public Type getType() {
      return TYPE;
   }

   @Override
   public void started() throws Exception {
      super.started();
   }

   @Override
   public void changed(Property p, Context cx) {
      super.changed(p, cx);
   }

   public IFuture post(Action action, BValue arg, Context cx) {
      BOpcUaDevice device = this.getBOpcUaClientDevice();
      return device != null ? device.postAsync(new Invocation(this, action, arg, cx)) : null;
   }

   public void doLearnDetail() {
      BOpcUaDevice device = this.getBOpcUaClientDevice();
      if (device != null) {
         UaClient uaClient = device.uaClient;
         if (uaClient.isConnected()) {
            AddressSpace addressSpace = uaClient.getAddressSpace();

            try {
               UaNode node = OpcUaClientUtil.getAddressSpaceNode(addressSpace, NodeId.parseNodeId(this.getUaNodeId()));
               if (node instanceof UaMethod) {
                  UaMethod method = (UaMethod)node;
                  Boolean executable = method.getExecutable();
                  Argument[] inputArguments = method.getInputArguments();
                  Argument[] outputArguments = method.getOutputArguments();
                  this.getInputArgs().removeAll();
                  this.getResults().removeAll();
                  this.getInputArgs().add("learned", BBoolean.make(true), 5);

                  for (Argument input : inputArguments) {
                     BUaArgument argument = OpcUaClientUtil.makeUaArgument(input);
                     this.getInputArgs().add("arg?", argument, 1);
                  }

                  for (Argument output : outputArguments) {
                     BUaArgument argument = OpcUaClientUtil.makeUaArgument(output);
                     this.getResults().add("result?", argument, 1);
                  }
               }
            } catch (MethodArgumentException var14) {
               logger.info("Error in the action Learn Details: " + var14);
            }
         }
      }
   }

   public BVector makeDefaultInputArgs() {
      BVector vector = new BVector();
      this.lease(2);
      int index = 0;

      for (BUaArgument argument : (BUaArgument[])this.getInputArgs().getChildren(BUaArgument.class)) {
         String description = argument.getDescription();
         int length = description.length();
         BFacets editFacet = BFacets.make("fieldWidth", BInteger.make(Math.min(length, 64)));
         BObject instance = argument.getArgType().getInstance();
         vector.add("desc" + index, BString.make(description), 1, editFacet, null);
         vector.add("value" + index, instance.asValue());
         index++;
      }

      return vector;
   }

   @Override
   public void readSubscribed(Context cx) throws Exception {
      if (this.getInputArgs().get("learned") == null) {
         this.learnDetail();
      }

      this.readOk(this.getReadValue());
   }

   @Override
   public void readUnsubscribed(Context cx) throws Exception {
   }

   @Override
   public void doPoll() {
   }

   protected void convertDeviceToProxy(BStatusValue deviceValue, BStatusValue proxyValue) {
      proxyValue.copyFrom(deviceValue);
   }

   @Override
   public BReadWriteMode getMode() {
      return BReadWriteMode.readonly;
   }
}
