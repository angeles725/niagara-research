package com.tridium.opcUaClient.point;

import com.prosysopc.ua.ServiceException;
import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.client.MonitoredDataItem;
import com.prosysopc.ua.client.MonitoredDataItemListener;
import com.prosysopc.ua.client.UaClient;
import com.prosysopc.ua.nodes.UaDataType;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.stack.builtintypes.ByteString;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.Enumeration;
import com.prosysopc.ua.stack.builtintypes.LocalizedText;
import com.prosysopc.ua.stack.builtintypes.NodeId;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.Structure;
import com.prosysopc.ua.stack.builtintypes.UnsignedByte;
import com.prosysopc.ua.stack.builtintypes.UnsignedInteger;
import com.prosysopc.ua.stack.builtintypes.UnsignedLong;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.builtintypes.Variant;
import com.prosysopc.ua.stack.core.Attributes;
import com.prosysopc.ua.stack.utils.NumericRange;
import com.prosysopc.ua.typedictionary.StructureSpecification;
import com.tridium.driver.util.DrUtil;
import com.tridium.ndriver.point.BNProxyExt;
import com.tridium.ndriver.poll.BINPollable;
import com.tridium.opcUaClient.BOpcUaDevice;
import com.tridium.opcUaClient.BOpcUaNetwork;
import com.tridium.opcUaClient.message.OpcUaWriteAsyncRequest;
import com.tridium.opcUaClient.util.MatrixUtil;
import com.tridium.opcUaClient.util.OpcUaClientUtil;
import com.tridium.opcUaCore.enums.BServerState;
import com.tridium.opcUaCore.util.OpcUaCoreUtil;
import java.lang.reflect.Array;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.baja.control.BControlPoint;
import javax.baja.driver.point.BReadWriteMode;
import javax.baja.driver.util.BPollFrequency;
import javax.baja.nre.annotations.NiagaraAction;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatusBoolean;
import javax.baja.status.BStatusEnum;
import javax.baja.status.BStatusNumeric;
import javax.baja.status.BStatusString;
import javax.baja.status.BStatusValue;
import javax.baja.sys.Action;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BBoolean;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uaDisplayName",
      type = "String",
      defaultValue = "",
      flags = 1
   ), @NiagaraProperty(
      name = "uaNodeId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "nameSpaceUri",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "pollFrequency",
      type = "BPollFrequency",
      defaultValue = "BPollFrequency.normal",
      flags = 4
   ), @NiagaraProperty(
      name = "arrayDimensions",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "arrayIndex",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "structurePath",
      type = "String",
      defaultValue = "",
      flags = 5
   ), @NiagaraProperty(
      name = "isStructureComponent",
      type = "boolean",
      defaultValue = "false",
      flags = 5
   ), @NiagaraProperty(
      name = "uaDataType",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "uaDataTypeIdentifier",
      type = "long",
      defaultValue = "-1"
   ), @NiagaraProperty(
      name = "uaStatusCode",
      type = "String",
      defaultValue = "",
      flags = 3
   ), @NiagaraProperty(
      name = "sourceTimestamp",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "serverTimestamp",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      flags = 3
   ), @NiagaraProperty(
      name = "infoLogging",
      type = "boolean",
      defaultValue = "false",
      flags = 4
   )})
@NiagaraAction(
   name = "enableInfoLog",
   parameterType = "BBoolean",
   defaultValue = "BBoolean.make(false)"
)
public class BOpcUaClientProxyExt extends BNProxyExt implements BINPollable, MonitoredDataItemListener {
   public static final Property uaDisplayName = newProperty(1, "", null);
   public static final Property uaNodeId = newProperty(0, "", null);
   public static final Property nameSpaceUri = newProperty(0, "", null);
   public static final Property pollFrequency = newProperty(4, BPollFrequency.normal, null);
   public static final Property arrayDimensions = newProperty(0, "", null);
   public static final Property arrayIndex = newProperty(0, "", null);
   public static final Property structurePath = newProperty(5, "", null);
   public static final Property isStructureComponent = newProperty(5, false, null);
   public static final Property uaDataType = newProperty(0, "", null);
   public static final Property uaDataTypeIdentifier = newProperty(0, -1, null);
   public static final Property uaStatusCode = newProperty(3, "", null);
   public static final Property sourceTimestamp = newProperty(3, BAbsTime.NULL, null);
   public static final Property serverTimestamp = newProperty(3, BAbsTime.NULL, null);
   public static final Property infoLogging = newProperty(4, false, null);
   public static final Action enableInfoLog = newAction(0, BBoolean.make(false), null);
   public static final Type TYPE = Sys.loadType(BOpcUaClientProxyExt.class);
   public static final int[] EMPTY_INT_ARRAY = new int[0];
   public static final Logger rdlogger = Logger.getLogger("opcUaClient.point.rd");
   public static final Logger wrlogger = Logger.getLogger("opcUaClient.point.wr");
   boolean configFault = false;
   MonitoredDataItem item;
   NodeId nodeId;
   DataValue[] values;
   StatusCode statusCode;
   NumericRange numericRange;
   private DataValue lastReadValue;

   public String getUaDisplayName() {
      return this.getString(uaDisplayName);
   }

   public void setUaDisplayName(String v) {
      this.setString(uaDisplayName, v, null);
   }

   public String getUaNodeId() {
      return this.getString(uaNodeId);
   }

   public void setUaNodeId(String v) {
      this.setString(uaNodeId, v, null);
   }

   public String getNameSpaceUri() {
      return this.getString(nameSpaceUri);
   }

   public void setNameSpaceUri(String v) {
      this.setString(nameSpaceUri, v, null);
   }

   public BPollFrequency getPollFrequency() {
      return (BPollFrequency)this.get(pollFrequency);
   }

   public void setPollFrequency(BPollFrequency v) {
      this.set(pollFrequency, v, null);
   }

   public String getArrayDimensions() {
      return this.getString(arrayDimensions);
   }

   public void setArrayDimensions(String v) {
      this.setString(arrayDimensions, v, null);
   }

   public String getArrayIndex() {
      return this.getString(arrayIndex);
   }

   public void setArrayIndex(String v) {
      this.setString(arrayIndex, v, null);
   }

   public String getStructurePath() {
      return this.getString(structurePath);
   }

   public void setStructurePath(String v) {
      this.setString(structurePath, v, null);
   }

   public boolean getIsStructureComponent() {
      return this.getBoolean(isStructureComponent);
   }

   public void setIsStructureComponent(boolean v) {
      this.setBoolean(isStructureComponent, v, null);
   }

   public String getUaDataType() {
      return this.getString(uaDataType);
   }

   public void setUaDataType(String v) {
      this.setString(uaDataType, v, null);
   }

   public long getUaDataTypeIdentifier() {
      return this.getLong(uaDataTypeIdentifier);
   }

   public void setUaDataTypeIdentifier(long v) {
      this.setLong(uaDataTypeIdentifier, v, null);
   }

   public String getUaStatusCode() {
      return this.getString(uaStatusCode);
   }

   public void setUaStatusCode(String v) {
      this.setString(uaStatusCode, v, null);
   }

   public BAbsTime getSourceTimestamp() {
      return (BAbsTime)this.get(sourceTimestamp);
   }

   public void setSourceTimestamp(BAbsTime v) {
      this.set(sourceTimestamp, v, null);
   }

   public BAbsTime getServerTimestamp() {
      return (BAbsTime)this.get(serverTimestamp);
   }

   public void setServerTimestamp(BAbsTime v) {
      this.set(serverTimestamp, v, null);
   }

   public boolean getInfoLogging() {
      return this.getBoolean(infoLogging);
   }

   public void setInfoLogging(boolean v) {
      this.setBoolean(infoLogging, v, null);
   }

   public void enableInfoLog(BBoolean parameter) {
      this.invoke(enableInfoLog, parameter, null);
   }

   public Type getType() {
      return TYPE;
   }

   public final BOpcUaNetwork getOpcUaClientNetwork() {
      return (BOpcUaNetwork)this.getNetwork();
   }

   public final BOpcUaDevice getBOpcUaClientDevice() {
      return (BOpcUaDevice)DrUtil.getParent(this, BOpcUaDevice.TYPE);
   }

   public final BOpcUaClientPointDeviceExt getOpcUaClientPointDeviceExt() {
      return (BOpcUaClientPointDeviceExt)this.getDeviceExt();
   }

   public void started() throws Exception {
      super.started();

      try {
         this.nodeId = NodeId.parseNodeId(this.getUaNodeId());
      } catch (Exception var2) {
         this.nodeId = null;
         this.configFail(OpcUaCoreUtil.getLocalizedMessage(var2));
      }
   }

   public void stopped() {
      BOpcUaNetwork network = (BOpcUaNetwork)this.getNetwork();
      if (network != null && this.item != null) {
         network.postAsync(new PointUnsubscribeCmd(this.getBOpcUaClientDevice(), this.getOpcUaClientPointDeviceExt(), this.getNodeId(), this));
      }
   }

   public void changed(Property p, Context cx) {
      super.changed(p, cx);
      if (this.isRunning() && cx != Context.decoding) {
         if (p.equals(BOpcUaClientProxyExt.arrayIndex)) {
            int[] arrayIndex = MatrixUtil.stringToIndexes(this.getArrayIndex());
            int[] maxIndex = MatrixUtil.stringToIndexes(this.getArrayDimensions());
            boolean indexOk = true;
            if (arrayIndex.length == maxIndex.length) {
               for (int i = 0; i < arrayIndex.length; i++) {
                  if (maxIndex[i] > 0 && arrayIndex[i] > maxIndex[i]) {
                     indexOk = false;
                     break;
                  }
               }
            } else {
               indexOk = false;
            }

            if (!indexOk) {
               this.configFail("Invalid index: " + this.getArrayIndex() + " for ArrayDimensions: " + this.getArrayDimensions());
            }
         } else if (p.equals(uaNodeId)) {
            try {
               this.nodeId = NodeId.parseNodeId(this.getUaNodeId());
            } catch (Exception var8) {
               this.nodeId = null;
               this.configFail(OpcUaCoreUtil.getLocalizedMessage(var8));
               return;
            }

            this.configOk();
            if (this.isSubscribed()) {
               try {
                  this.readSubscribed(cx);
               } catch (Exception var7) {
                  rdlogger.fine(var7.getMessage());
               }
            }
         }
      }
   }

   public void configFail(String cause) {
      this.configFault = true;
      this.readFail(cause);
   }

   public void configOk() {
      this.configFault = false;
      this.readReset();
   }

   public void readSubscribed(Context cx) throws Exception {
      BOpcUaNetwork network = (BOpcUaNetwork)this.getNetwork();
      if (network != null) {
         network.postAsync(new PointSubscribeCmd(this.getBOpcUaClientDevice(), this.getOpcUaClientPointDeviceExt(), this.getNodeId(), this));
      }
   }

   public void readUnsubscribed(Context cx) throws Exception {
   }

   public boolean write(Context cx) throws Exception {
      if (!this.configFault && this.getParentPoint().isWritablePoint()) {
         BStatusValue out = this.getWriteValue();
         if (out.getStatus().isNull()) {
            return false;
         } else {
            BOpcUaNetwork network = (BOpcUaNetwork)this.getNetwork();
            BOpcUaDevice device = (BOpcUaDevice)this.getDevice();
            if (!device.getServerState().equals(BServerState.Running)) {
               return false;
            } else {
               try {
                  network.postAsync(new OpcUaWriteAsyncRequest(this, out));
                  return true;
               } catch (Exception var6) {
                  network.log().severe("Could not post write for " + this.getParentPoint().getName());
                  this.writeFail("postWriteFail: " + OpcUaCoreUtil.getLocalizedMessage(var6));
                  return false;
               }
            }
         }
      } else {
         return false;
      }
   }

   public void doWrite(BStatusValue out) {
      try {
         BOpcUaNetwork net = (BOpcUaNetwork)this.getNetwork();
         if (net == null || net.isDisabled() || this.configFault) {
            return;
         }

         BOpcUaDevice device = (BOpcUaDevice)this.getDevice();
         UaClient uaClient = device.uaClient;
         NodeId nodeId = NodeId.parseNodeId(this.getUaNodeId());
         if (this.statusCode != null && this.statusCode.isBad()) {
            this.writeFail(this.statusCode.getName());
            return;
         }

         AccessController.doPrivileged(
            (PrivilegedExceptionAction<Void>)(() -> {
               try {
                  UaNode thisNode = uaClient.getAddressSpace().getNode(nodeId);
                  UaDataType dataType = null;
                  Class<?> javaClass = null;
                  if (thisNode instanceof UaVariable) {
                     UaVariable v = (UaVariable)thisNode;
                     if (v.getDataType() == null) {
                        v.setDataType(uaClient.getAddressSpace().getDataType(v.getDataTypeId()));
                     }

                     dataType = v.getDataType();
                     javaClass = dataType.getJavaClass();
                  }

                  BControlPoint point = this.getParentPoint();
                  if (point.isWritablePoint()) {
                     String dimensions = this.getArrayDimensions();
                     boolean isArray = !dimensions.isEmpty() && !"[]".equals(dimensions);
                     Object wrValue = this.convertForWrite(out, isArray);
                     Variant wrVariant = new Variant(wrValue);
                     StructureSpecification spec = OpcUaClientUtil.getStructureSpecification(uaClient, dataType);
                     if (spec != null) {
                        if (this.lastReadValue == null) {
                           this.logMessage(wrlogger, "Failed to write to structure without any last read data");
                           return null;
                        }

                        Structure structure = (Structure)this.lastReadValue.getValue().getValue();
                        wrVariant = new Variant(this.writeToStructure(structure, this.getStructurePath().split(">"), 0, wrValue));
                     }

                     if (isArray) {
                        this.logMessage(wrlogger, point.getName() + ": wrValue = " + ((Object[])wrValue)[0] + ", class = " + wrValue.getClass().getSimpleName());
                     } else {
                        this.logMessage(wrlogger, point.getName() + ": wrValue = " + wrValue + ", class = " + wrValue.getClass().getSimpleName());
                     }

                     Object[] convertedValue = new Object[]{wrVariant};
                     if (convertedValue[0] == null) {
                        this.writeFail("Cannot write null OpcUa variable value");
                        return null;
                     }

                     boolean written;
                     if (isArray && !this.getIsStructureComponent()) {
                        this.numericRange = MatrixUtil.indexToNumericRange(this.getArrayIndex());
                        written = uaClient.writeAttribute(nodeId, Attributes.Value, wrVariant, this.numericRange);
                     } else {
                        written = uaClient.writeAttribute(nodeId, Attributes.Value, convertedValue[0]);
                     }

                     if (written) {
                        if (wrlogger.isLoggable(Level.FINEST)) {
                           wrlogger.log(
                              Level.FINEST,
                              point.getName() + ": wrValue = " + wrValue + ", class = " + wrValue.getClass().getSimpleName() + " has already been written"
                           );
                        }
                     } else if (wrlogger.isLoggable(Level.FINEST)) {
                        wrlogger.log(
                           Level.FINEST,
                           point.getName() + ": wrValue = " + wrValue + ", class = " + wrValue.getClass().getSimpleName() + " will be written asynchronously"
                        );
                     }

                     this.writeOk(out);
                  }
               } catch (ServiceException var15) {
                  this.writeFail("ServiceException: " + OpcUaCoreUtil.getLocalizedMessage(var15));
               } catch (StatusException var16) {
                  this.writeFail("StatusException: " + OpcUaCoreUtil.getLocalizedMessage(var16));
               } catch (Exception var17) {
                  this.writeFail("Exception: " + OpcUaCoreUtil.getLocalizedMessage(var17));
               }

               return null;
            })
         );
      } catch (Exception var6) {
         this.writeFail("Exception: " + OpcUaCoreUtil.getLocalizedMessage(var6));
      }
   }

   private boolean isArray() {
      String dimensions = this.getArrayDimensions();
      return !dimensions.isEmpty() && !"[]".equals(dimensions);
   }

   private NumericRange getNumericRange() {
      return new NumericRange(new int[][]{MatrixUtil.stringToIndexes(this.getArrayIndex())});
   }

   private Object convertForWrite(BStatusValue wrStatusValue, boolean isArray) {
      String uaDataType = this.getUaDataType();
      int i = uaDataType.indexOf(91);
      if (i >= 0) {
         uaDataType = uaDataType.substring(0, i);
      }

      if (wrStatusValue instanceof BStatusNumeric) {
         double dValue = ((BStatusNumeric)wrStatusValue).getValue();
         switch (uaDataType) {
            case "SByte":
            case "Byte":
               Byte bvalue = (byte)dValue;
               if (isArray) {
                  return new Byte[]{bvalue};
               }

               return bvalue;
            case "UByte":
            case "UnsignedByte":
               UnsignedByte ubvalue = UnsignedByte.valueOf((int)dValue);
               if (isArray) {
                  return new UnsignedByte[]{ubvalue};
               }

               return ubvalue;
            case "Int16":
            case "Short":
               Short svalue = (short)dValue;
               if (isArray) {
                  return new Short[]{svalue};
               }

               return svalue;
            case "UInt16":
            case "UnsignedShort":
               UnsignedShort usvalue = UnsignedShort.valueOf((int)dValue);
               if (isArray) {
                  return new UnsignedShort[]{usvalue};
               }

               return usvalue;
            case "Int32":
            case "Integer":
               Integer ivalue = (int)dValue;
               if (isArray) {
                  return new Integer[]{ivalue};
               }

               return ivalue;
            case "UInt32":
            case "UInteger":
            case "UnsignedInteger":
               UnsignedInteger uivalue = UnsignedInteger.valueOf((long)dValue);
               if (isArray) {
                  return new UnsignedInteger[]{uivalue};
               }

               return uivalue;
            case "Int64":
            case "Long":
               Long value = (long)dValue;
               if (isArray) {
                  return new Long[]{value};
               }

               return value;
            case "UInt64":
            case "ULong":
            case "UnsignedLong":
               UnsignedLong ulvalue = UnsignedLong.valueOf((long)dValue);
               if (isArray) {
                  return new UnsignedLong[]{ulvalue};
               }

               return ulvalue;
            case "Float":
               Float fvalue = (float)dValue;
               if (isArray) {
                  return new Float[]{fvalue};
               }

               return fvalue;
            case "Number":
            case "Double":
               Double dvalue = dValue;
               if (isArray) {
                  return new Double[]{dvalue};
               }

               return dvalue;
            default:
               return Double.toString(dValue);
         }
      } else if (wrStatusValue instanceof BStatusBoolean) {
         boolean bValue = ((BStatusBoolean)wrStatusValue).getValue();
         if ("Boolean".equals(uaDataType)) {
            Boolean value = bValue;
            return isArray ? new Boolean[]{value} : value;
         } else {
            return Boolean.toString(bValue);
         }
      } else if (wrStatusValue instanceof BStatusEnum) {
         int iValue = ((BStatusEnum)wrStatusValue).getValue().getOrdinal();
         switch (uaDataType) {
            case "SByte":
            case "Byte":
               return (byte)iValue;
            case "UByte":
            case "UnsignedByte":
               return UnsignedByte.valueOf(iValue);
            case "Int16":
            case "Short":
               return (short)iValue;
            case "UInt16":
            case "UnsignedShort":
               return UnsignedShort.valueOf(iValue);
            case "Int32":
            case "Integer":
               return iValue;
            case "UInt32":
            case "UInteger":
            case "UnsignedInteger":
               return UnsignedInteger.valueOf(iValue);
            case "Int64":
            case "Long":
               return (long)iValue;
            case "UInt64":
            case "ULong":
            case "UnsignedLong":
               return UnsignedLong.valueOf(iValue);
            default:
               return (long)iValue;
         }
      } else {
         String sValue = ((BStatusString)wrStatusValue).getValue();
         String dimensions = this.getArrayDimensions();
         boolean isByteStringArray = dimensions.startsWith("[][");
         switch (uaDataType) {
            case "String":
               if (isArray) {
                  return new String[]{sValue};
               }

               return sValue;
            case "LocalizedText":
               if (isArray) {
                  return new LocalizedText[]{new LocalizedText(sValue)};
               }

               return new LocalizedText(sValue);
            case "ByteString":
               ByteString byteString = ByteString.fromHex(sValue);
               if (isArray) {
                  return new ByteString[]{byteString};
               }

               return byteString;
            default:
               throw new IllegalArgumentException("Invalid String type: " + uaDataType);
         }
      }
   }

   public Type getDeviceExtType() {
      return BOpcUaClientPointDeviceExt.TYPE;
   }

   public BReadWriteMode getMode() {
      return this.getParentPoint().isWritablePoint() ? BReadWriteMode.readWrite : BReadWriteMode.readonly;
   }

   public boolean isBoolean() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusBoolean;
   }

   public boolean isNumeric() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusNumeric;
   }

   public boolean isString() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusString;
   }

   public boolean isEnum() {
      return this.getParentPoint().getOutStatusValue() instanceof BStatusEnum;
   }

   public BValue getActionParameterDefault(Action action) {
      return (BValue)(action.equals(enableInfoLog) ? BBoolean.make(!this.getInfoLogging()) : super.getActionParameterDefault(action));
   }

   public void doEnableInfoLog(BBoolean enable) {
      this.setInfoLogging(enable.getBoolean());
      rdlogger.info("enableInfoLog:" + this.getParentPoint().getName() + ' ' + enable.getBoolean());
   }

   public void doPoll() {
   }

   private int[] decodeMaxArrayIndex() {
      String dimensions = this.getArrayDimensions();
      if (dimensions.isEmpty()) {
         return EMPTY_INT_ARRAY;
      } else {
         String[] ss = dimensions.split("]");
         int[] maxIndex = new int[ss.length];

         for (int i = 0; i < maxIndex.length; i++) {
            maxIndex[i] = -1;
            if (ss[i].length() > 1 || ss[i].startsWith("[")) {
               String sIndex = ss[i].substring(1);

               try {
                  maxIndex[i] = Integer.parseInt(sIndex);
               } catch (NumberFormatException var7) {
               }
            }
         }

         return maxIndex;
      }
   }

   private int[] decodeArrayIndex() {
      int[] ret = new int[]{0};
      String s = this.getArrayIndex();

      try {
         if (!s.contains(",")) {
            ret[0] = Integer.parseInt(s);
         } else {
            String[] strings = s.split(",");
            ret = new int[strings.length];

            for (int i = 0; i < strings.length; i++) {
               ret[i] = Integer.parseInt(strings[i]);
            }
         }
      } catch (NumberFormatException var5) {
      }

      return ret;
   }

   public void onDataChange(MonitoredDataItem monitoredDataItem, DataValue oldValue, DataValue newValue) {
      if (newValue != null && monitoredDataItem.equals(this.item)) {
         int[] arrayIndex = MatrixUtil.stringToIndexes(this.getArrayIndex());
         this.statusCode = newValue.getStatusCode();
         this.setUaStatusCode(this.statusCode.toString());
         if (this.statusCode.isGood()) {
            BStatusValue desiredType = (BStatusValue)this.getParentPoint().getOutStatusValue().newCopy();
            BStatusValue setValue = OpcUaClientUtil.makeStatusValue(newValue, desiredType, arrayIndex, this.getStructurePath());
            this.logMessage(rdlogger, "onDataChange():" + this.getParentPoint().getName() + " setValue = " + setValue);
            this.readOk(setValue);
            this.lastReadValue = newValue;
            this.getDevice().pingOk();
         } else {
            this.readFail(OpcUaCoreUtil.getLocalizedDescription(this.statusCode));
         }

         DateTime sourceTimestamp = newValue.getSourceTimestamp();
         if (null != sourceTimestamp) {
            this.setSourceTimestamp(BAbsTime.make(sourceTimestamp.getTimeInMillis()));
         }

         this.setServerTimestamp(BAbsTime.make());
      }
   }

   private void logMessage(Logger logger, String message) {
      if (this.getInfoLogging()) {
         logger.info(message);
      } else if (logger.isLoggable(Level.FINE)) {
         logger.fine(message);
      }
   }

   protected void setMonitoredItem(MonitoredDataItem item) {
      this.item = item;
   }

   protected MonitoredDataItem getMonitoredItem() {
      return this.item;
   }

   protected void setNodeId(NodeId nodeId) {
      this.nodeId = nodeId;
   }

   protected NodeId getNodeId() {
      return this.nodeId;
   }

   private Structure writeToStructure(Structure structure, String[] structurePath, int level, Object wrValue) {
      Object tempValue = structure.get(structurePath[level]);
      if (OpcUaClientUtil.isStructure(tempValue) && level < structurePath.length) {
         structure.set(structurePath[level], this.writeToStructure((Structure)tempValue, structurePath, level + 1, wrValue));
      } else if (this.isArray()) {
         Object[] tempArray = (Object[])tempValue;
         Array.set(tempArray, Integer.parseInt(this.getArrayIndex()), Array.get(wrValue, 0));
         structure.set(structurePath[level], tempArray);
      } else if (this.isEnum()) {
         Enumeration enumeration = ((Enumeration)tempValue).specification().toEnumerationBuilder().setValue(((Long)wrValue).intValue()).build();
         structure.set(structurePath[level], enumeration);
      } else {
         structure.set(structurePath[level], wrValue);
      }

      return structure;
   }
}
