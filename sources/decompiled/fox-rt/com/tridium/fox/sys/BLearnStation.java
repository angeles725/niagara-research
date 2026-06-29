package com.tridium.fox.sys;

import com.tridium.fox.message.FoxMessage;
import com.tridium.fox.session.MulticastUtil;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.util.Array;
import javax.baja.sys.BStruct;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "stationName",
      type = "String",
      defaultValue = "",
      flags = 8
   ), @NiagaraProperty(
      name = "scheme",
      type = "String",
      defaultValue = "fox",
      flags = 8
   ), @NiagaraProperty(
      name = "address",
      type = "String",
      defaultValue = "",
      flags = 8
   ), @NiagaraProperty(
      name = "hostName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "hostAddress",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "foxPort",
      type = "int",
      defaultValue = "0"
   ), @NiagaraProperty(
      name = "version",
      type = "String",
      defaultValue = "",
      flags = 8
   ), @NiagaraProperty(
      name = "hostModel",
      type = "String",
      defaultValue = "",
      flags = 8
   ), @NiagaraProperty(
      name = "hostModelVersion",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "hostId",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "vmName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "vmVersion",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "osName",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "osVersion",
      type = "String",
      defaultValue = ""
   ), @NiagaraProperty(
      name = "niagaraPlatformType",
      type = "String",
      defaultValue = "",
      flags = 4
   )})
public class BLearnStation extends BStruct {
   public static final Property stationName = newProperty(8, "", null);
   public static final Property scheme = newProperty(8, "fox", null);
   public static final Property address = newProperty(8, "", null);
   public static final Property hostName = newProperty(0, "", null);
   public static final Property hostAddress = newProperty(0, "", null);
   public static final Property foxPort = newProperty(0, 0, null);
   public static final Property version = newProperty(8, "", null);
   public static final Property hostModel = newProperty(8, "", null);
   public static final Property hostModelVersion = newProperty(0, "", null);
   public static final Property hostId = newProperty(0, "", null);
   public static final Property vmName = newProperty(0, "", null);
   public static final Property vmVersion = newProperty(0, "", null);
   public static final Property osName = newProperty(0, "", null);
   public static final Property osVersion = newProperty(0, "", null);
   public static final Property niagaraPlatformType = newProperty(4, "", null);
   public static final Type TYPE = Sys.loadType(BLearnStation.class);
   private String key;

   public String getStationName() {
      return this.getString(stationName);
   }

   public void setStationName(String v) {
      this.setString(stationName, v, null);
   }

   public String getScheme() {
      return this.getString(scheme);
   }

   public void setScheme(String v) {
      this.setString(scheme, v, null);
   }

   public String getAddress() {
      return this.getString(address);
   }

   public void setAddress(String v) {
      this.setString(address, v, null);
   }

   public String getHostName() {
      return this.getString(hostName);
   }

   public void setHostName(String v) {
      this.setString(hostName, v, null);
   }

   public String getHostAddress() {
      return this.getString(hostAddress);
   }

   public void setHostAddress(String v) {
      this.setString(hostAddress, v, null);
   }

   public int getFoxPort() {
      return this.getInt(foxPort);
   }

   public void setFoxPort(int v) {
      this.setInt(foxPort, v, null);
   }

   public String getVersion() {
      return this.getString(version);
   }

   public void setVersion(String v) {
      this.setString(version, v, null);
   }

   public String getHostModel() {
      return this.getString(hostModel);
   }

   public void setHostModel(String v) {
      this.setString(hostModel, v, null);
   }

   public String getHostModelVersion() {
      return this.getString(hostModelVersion);
   }

   public void setHostModelVersion(String v) {
      this.setString(hostModelVersion, v, null);
   }

   public String getHostId() {
      return this.getString(hostId);
   }

   public void setHostId(String v) {
      this.setString(hostId, v, null);
   }

   public String getVmName() {
      return this.getString(vmName);
   }

   public void setVmName(String v) {
      this.setString(vmName, v, null);
   }

   public String getVmVersion() {
      return this.getString(vmVersion);
   }

   public void setVmVersion(String v) {
      this.setString(vmVersion, v, null);
   }

   public String getOsName() {
      return this.getString(osName);
   }

   public void setOsName(String v) {
      this.setString(osName, v, null);
   }

   public String getOsVersion() {
      return this.getString(osVersion);
   }

   public void setOsVersion(String v) {
      this.setString(osVersion, v, null);
   }

   public String getNiagaraPlatformType() {
      return this.getString(niagaraPlatformType);
   }

   public void setNiagaraPlatformType(String v) {
      this.setString(niagaraPlatformType, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public static BLearnStation[] make(FoxMessage msg) throws Exception {
      Array<BLearnStation> stations = new Array(BLearnStation.class);
      BLearnStation station = new BLearnStation();
      station.setStationName(msg.getString("station"));
      if (MulticastUtil.isIpv6Message(msg)) {
         station.setHostName(msg.getString("hostNameIPv6", msg.getString("hostName", "unknown")));
         station.setHostAddress(msg.getString("hostAddressIPv6", msg.getString("hostAddress", "unknown")));
      } else {
         station.setHostName(msg.getString("hostName"));
         station.setHostAddress(msg.getString("hostAddress"));
      }

      station.setVersion(msg.getString("version", "unknown"));
      station.setHostModel(msg.getString("hostModel", "unknown"));
      station.setHostModelVersion(msg.getString("hostModelVersion", "unknown"));
      station.setHostId(msg.getString("hostId", "unknown"));
      station.setVmName(msg.getString("vmName", "unknown"));
      station.setVmVersion(msg.getString("vmVersion", "unknown"));
      station.setOsName(msg.getString("osName", "unknown"));
      station.setOsVersion(msg.getString("osVersion", "unknown"));
      station.setNiagaraPlatformType(msg.getString("niagaraPlatformType", ""));
      station.invariant(false);
      if (msg.getInt("foxPort", 0) != 0) {
         BLearnStation foxStation = (BLearnStation)station.newCopy();
         foxStation.setScheme("fox");
         foxStation.setFoxPort(msg.getInt("foxPort", 0));
         foxStation.invariant(false);
         stations.add(foxStation);
      }

      if (msg.getInt("foxsPort", 0) != 0) {
         BLearnStation foxsStation = (BLearnStation)station.newCopy();
         foxsStation.setScheme("foxs");
         foxsStation.setFoxPort(msg.getInt("foxsPort", 0));
         foxsStation.invariant(false);
         stations.add(foxsStation);
      }

      return (BLearnStation[])stations.trim();
   }

   public BLearnStation() {
   }

   public BLearnStation(String stationName) {
      this.setStationName(stationName);
   }

   public String getKey() {
      return this.key;
   }

   public void invariant(boolean useHostName) {
      String preferredAddr = useHostName ? this.getHostName() : this.getHostAddress();
      if (preferredAddr.indexOf(58) != -1) {
         preferredAddr = "[" + preferredAddr + "]";
      }

      if (this.getScheme().equalsIgnoreCase("fox") && this.getFoxPort() != 1911) {
         preferredAddr = preferredAddr + ":" + this.getFoxPort();
      } else if (this.getScheme().equalsIgnoreCase("foxs") && this.getFoxPort() != 4911) {
         preferredAddr = preferredAddr + ":" + this.getFoxPort();
      }

      this.setAddress(preferredAddr);
      this.key = this.getStationName() + ":" + this.getScheme() + "@" + this.getHostAddress() + ":" + this.getFoxPort();
   }

   public String toString(Context cx) {
      String hostAddr = this.getHostAddress();
      if (hostAddr.indexOf(58) != -1) {
         hostAddr = "[" + hostAddr + "]";
      }

      return this.getStationName()
         + ":"
         + this.getScheme()
         + " @ "
         + this.getHostName()
         + "/"
         + hostAddr
         + ":"
         + this.getFoxPort()
         + " ["
         + this.getVersion()
         + "; "
         + this.getHostId()
         + "; "
         + this.getHostModel()
         + "; "
         + this.getHostModelVersion()
         + "; "
         + this.getOsName()
         + " "
         + this.getOsVersion()
         + "; "
         + this.getVmName()
         + " "
         + this.getVmVersion()
         + (this.getNiagaraPlatformType().isEmpty() ? "]" : "; " + this.getNiagaraPlatformType() + "]");
   }
}
