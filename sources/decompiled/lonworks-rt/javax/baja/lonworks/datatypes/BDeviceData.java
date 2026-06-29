package javax.baja.lonworks.datatypes;

import com.tridium.lonworks.BLonRouter;
import com.tridium.lonworks.NAddressManager;
import com.tridium.lonworks.device.DeviceFacets;
import com.tridium.lonworks.util.NmUtil;
import javax.baja.lonworks.AddressManager;
import javax.baja.lonworks.BLonDevice;
import javax.baja.lonworks.LonException;
import javax.baja.lonworks.enums.BLonNodeState;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BComplex;
import javax.baja.sys.BComponent;
import javax.baja.sys.BFacets;
import javax.baja.sys.BIcon;
import javax.baja.sys.BObject;
import javax.baja.sys.BajaRuntimeException;
import javax.baja.sys.BasicContext;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "neuronId",
      type = "BNeuronId",
      defaultValue = "BNeuronId.DEFAULT",
      flags = 64
   ), @NiagaraProperty(
      name = "programId",
      type = "BProgramId",
      defaultValue = "BProgramId.DEFAULT",
      flags = 8
   ), @NiagaraProperty(
      name = "nodeState",
      type = "BLonNodeState",
      defaultValue = "BLonNodeState.unknown",
      flags = 64
   ), @NiagaraProperty(
      name = "subnetNodeId",
      type = "BSubnetNode",
      defaultValue = "BSubnetNode.DEFAULT",
      flags = 65
   ), @NiagaraProperty(
      name = "location",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "authenticate",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "channelId",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "workingDomain",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "bindingII",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "hosted",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "twoDomains",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "msgTagCount",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "addressCount",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "addressTable",
      type = "BAddressTable",
      defaultValue = "new BAddressTable()",
      flags = 65
   ), @NiagaraProperty(
      name = "extAddressTable",
      type = "BExtAddressTable",
      defaultValue = "new BExtAddressTable()",
      flags = 4
   ), @NiagaraProperty(
      name = "prioritySlot",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "aliasTable",
      type = "BAliasTable",
      defaultValue = "new BAliasTable()",
      flags = 1
   ), @NiagaraProperty(
      name = "selfDoc",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "hasNodeObject",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "freezeChannelPriorities",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "lastHash",
      type = "int",
      defaultValue = "-1",
      flags = 71
   ), @NiagaraProperty(
      name = "facets",
      type = "BFacets",
      defaultValue = "BFacets.DEFAULT"
   )})
public class BDeviceData extends BComponent {
   public static final Property neuronId = newProperty(64, BNeuronId.DEFAULT, null);
   public static final Property programId = newProperty(8, BProgramId.DEFAULT, null);
   public static final Property nodeState = newProperty(64, BLonNodeState.unknown, null);
   public static final Property subnetNodeId = newProperty(65, BSubnetNode.DEFAULT, null);
   public static final Property location = newProperty(0, "", null);
   public static final Property authenticate = newProperty(0, false, null);
   public static final Property channelId = newProperty(1, 0, null);
   public static final Property workingDomain = newProperty(1, 0, null);
   public static final Property bindingII = newProperty(1, false, null);
   public static final Property hosted = newProperty(1, false, null);
   public static final Property twoDomains = newProperty(1, false, null);
   public static final Property msgTagCount = newProperty(1, 0, null);
   public static final Property addressCount = newProperty(1, 0, null);
   public static final Property addressTable = newProperty(65, new BAddressTable(), null);
   public static final Property extAddressTable = newProperty(4, new BExtAddressTable(), null);
   public static final Property prioritySlot = newProperty(1, 0, null);
   public static final Property aliasTable = newProperty(1, new BAliasTable(), null);
   public static final Property selfDoc = newProperty(0, "", null);
   public static final Property hasNodeObject = newProperty(0, false, null);
   public static final Property freezeChannelPriorities = newProperty(0, false, null);
   public static final Property lastHash = newProperty(71, -1, null);
   public static final Property facets = newProperty(0, BFacets.DEFAULT, null);
   public static final Type TYPE = Sys.loadType(BDeviceData.class);
   private Object pickle = null;
   private static final BIcon icon = BIcon.std("deviceData.png");
   public static final Context importChanges = new BasicContext();

   public BNeuronId getNeuronId() {
      return (BNeuronId)this.get(neuronId);
   }

   public void setNeuronId(BNeuronId v) {
      this.set(neuronId, v, null);
   }

   public BProgramId getProgramId() {
      return (BProgramId)this.get(programId);
   }

   public void setProgramId(BProgramId v) {
      this.set(programId, v, null);
   }

   public BLonNodeState getNodeState() {
      return (BLonNodeState)this.get(nodeState);
   }

   public void setNodeState(BLonNodeState v) {
      this.set(nodeState, v, null);
   }

   public BSubnetNode getSubnetNodeId() {
      return (BSubnetNode)this.get(subnetNodeId);
   }

   public void setSubnetNodeId(BSubnetNode v) {
      this.set(subnetNodeId, v, null);
   }

   public String getLocation() {
      return this.getString(location);
   }

   public void setLocation(String v) {
      this.setString(location, v, null);
   }

   public boolean getAuthenticate() {
      return this.getBoolean(authenticate);
   }

   public void setAuthenticate(boolean v) {
      this.setBoolean(authenticate, v, null);
   }

   public int getChannelId() {
      return this.getInt(channelId);
   }

   public void setChannelId(int v) {
      this.setInt(channelId, v, null);
   }

   public int getWorkingDomain() {
      return this.getInt(workingDomain);
   }

   public void setWorkingDomain(int v) {
      this.setInt(workingDomain, v, null);
   }

   public boolean getBindingII() {
      return this.getBoolean(bindingII);
   }

   public void setBindingII(boolean v) {
      this.setBoolean(bindingII, v, null);
   }

   public boolean getHosted() {
      return this.getBoolean(hosted);
   }

   public void setHosted(boolean v) {
      this.setBoolean(hosted, v, null);
   }

   public boolean getTwoDomains() {
      return this.getBoolean(twoDomains);
   }

   public void setTwoDomains(boolean v) {
      this.setBoolean(twoDomains, v, null);
   }

   public int getMsgTagCount() {
      return this.getInt(msgTagCount);
   }

   public void setMsgTagCount(int v) {
      this.setInt(msgTagCount, v, null);
   }

   public int getAddressCount() {
      return this.getInt(addressCount);
   }

   public void setAddressCount(int v) {
      this.setInt(addressCount, v, null);
   }

   public BAddressTable getAddressTable() {
      return (BAddressTable)this.get(addressTable);
   }

   public void setAddressTable(BAddressTable v) {
      this.set(addressTable, v, null);
   }

   public BExtAddressTable getExtAddressTable() {
      return (BExtAddressTable)this.get(extAddressTable);
   }

   public void setExtAddressTable(BExtAddressTable v) {
      this.set(extAddressTable, v, null);
   }

   public int getPrioritySlot() {
      return this.getInt(prioritySlot);
   }

   public void setPrioritySlot(int v) {
      this.setInt(prioritySlot, v, null);
   }

   public BAliasTable getAliasTable() {
      return (BAliasTable)this.get(aliasTable);
   }

   public void setAliasTable(BAliasTable v) {
      this.set(aliasTable, v, null);
   }

   public String getSelfDoc() {
      return this.getString(selfDoc);
   }

   public void setSelfDoc(String v) {
      this.setString(selfDoc, v, null);
   }

   public boolean getHasNodeObject() {
      return this.getBoolean(hasNodeObject);
   }

   public void setHasNodeObject(boolean v) {
      this.setBoolean(hasNodeObject, v, null);
   }

   public boolean getFreezeChannelPriorities() {
      return this.getBoolean(freezeChannelPriorities);
   }

   public void setFreezeChannelPriorities(boolean v) {
      this.setBoolean(freezeChannelPriorities, v, null);
   }

   public int getLastHash() {
      return this.getInt(lastHash);
   }

   public void setLastHash(int v) {
      this.setInt(lastHash, v, null);
   }

   public BFacets getFacets() {
      return (BFacets)this.get(facets);
   }

   public void setFacets(BFacets v) {
      this.set(facets, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BDeviceData() {
   }

   public BDeviceData(BProgramId pid) {
      this.setProgramId(pid);
   }

   public BDeviceData(
      BProgramId pid, boolean bindingII, boolean hosted, boolean twoDomains, int msgTagCount, int addressCount, int aliasCount, boolean hasNodeObject
   ) {
      this.setProgramId(pid);
      this.setBindingII(bindingII);
      this.setHosted(hosted);
      this.setTwoDomains(twoDomains);
      this.setMsgTagCount(msgTagCount);
      this.setAddressCount(addressCount);
      if (aliasCount > 0) {
         this.setAliasTable(new BAliasTable(aliasCount));
      }

      this.setHasNodeObject(hasNodeObject);
   }

   public void changed(Property prop, Context context) {
      super.changed(prop, context);
      if (this.isRunning() && context != importChanges) {
         if (prop == facets) {
            this.pickle = null;
         }

         BObject p = this.getParent();
         if (p instanceof BLonDevice) {
            ((BLonDevice)p).deviceDataChanged(prop, context);
         }

         if (prop == neuronId || prop == programId || prop == nodeState || prop == subnetNodeId || prop == channelId) {
            NmUtil.getLonNetwork(this).addressManager().deviceDataChanged(this, context);
         }

         if (context != AddressManager.noDeviceChange && context != NAddressManager.localChange) {
            if (!this.getNeuronId().isZero()) {
               Runnable req = null;
               if (prop == nodeState) {
                  req = new Runnable() {
                     @Override
                     public void run() {
                        BDeviceData.this.updateNodeState();
                     }
                  };
               } else if (prop == subnetNodeId || prop == workingDomain) {
                  req = new Runnable() {
                     @Override
                     public void run() {
                        BDeviceData.this.updateSubnetNodeId();
                     }
                  };
               }

               if (req != null) {
                  NmUtil.getLonNetwork(this).postAsync(req);
               }
            }
         }
      }
   }

   private void updateSubnetNodeId() {
      BComplex o = this.getParent();
      if (o instanceof BLonDevice) {
         BLonDevice dev = (BLonDevice)o;
         dev.updateDomainTable();
      }
   }

   private void updateNodeState() {
      boolean err = false;
      BComplex o = this.getParent();
      BLonNodeState nState = this.getNodeState();
      LonException cause = null;
      if (o instanceof BLonDevice) {
         BLonDevice dev = (BLonDevice)o;
         dev.updateNodeState();
      } else if (o instanceof BLonRouter) {
         BLonRouter rtr = (BLonRouter)o;

         try {
            NmUtil.setDeviceState(rtr, nState, true);
            NmUtil.setDeviceState(rtr, nState, false);
         } catch (LonException var7) {
            err = true;
            cause = var7;
         }

         rtr.getNearDeviceData().set(nodeState, nState, AddressManager.noDeviceChange);
         rtr.getFarDeviceData().set(nodeState, nState, AddressManager.noDeviceChange);
      }

      if (err) {
         throw new BajaRuntimeException("Unable to update state change in " + o.getDisplayName(null), cause);
      }
   }

   public void setPickle(Object o) {
      if (o instanceof DeviceFacets) {
         this.pickle = o;
      }
   }

   public Object getPickle() {
      return this.pickle;
   }

   public boolean isExtended() {
      return false;
   }

   public void clearAddressTable() {
      this.getAddressTable().clearTable();
   }

   public BIAddressEntry getAddressEntry(int index) {
      return this.getAddressTable().getAddressEntry(index);
   }

   public void setAddressEntry(int index, BIAddressEntry e) {
      this.getAddressTable().setAddressEntry(index, e);
   }

   public void setAddressEntry(int index, BIAddressEntry e, Context c) {
      this.getAddressTable().setAddressEntry(index, e, c);
   }

   public BIcon getIcon() {
      return icon;
   }
}
