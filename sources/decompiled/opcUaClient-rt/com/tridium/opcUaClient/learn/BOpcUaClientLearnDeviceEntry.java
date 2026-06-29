package com.tridium.opcUaClient.learn;

import com.prosysopc.ua.stack.core.EndpointDescription;
import com.tridium.ndriver.discover.BINDiscoveryLeaf;
import com.tridium.ndriver.util.SfUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.registry.TypeInfo;
import javax.baja.sys.BComponent;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "uri",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "securityMode",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "securityPolicy",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   ), @NiagaraProperty(
      name = "transportProfile",
      type = "String",
      defaultValue = "",
      facets = {@Facet("SfUtil.incl()")}
   )})
public class BOpcUaClientLearnDeviceEntry extends BComponent implements BINDiscoveryLeaf {
   public static final Property uri = newProperty(0, "", SfUtil.incl());
   public static final Property securityMode = newProperty(0, "", SfUtil.incl());
   public static final Property securityPolicy = newProperty(0, "", SfUtil.incl());
   public static final Property transportProfile = newProperty(0, "", SfUtil.incl());
   public static final Type TYPE = Sys.loadType(BOpcUaClientLearnDeviceEntry.class);

   public String getUri() {
      return this.getString(uri);
   }

   public void setUri(String v) {
      this.setString(uri, v, null);
   }

   public String getSecurityMode() {
      return this.getString(securityMode);
   }

   public void setSecurityMode(String v) {
      this.setString(securityMode, v, null);
   }

   public String getSecurityPolicy() {
      return this.getString(securityPolicy);
   }

   public void setSecurityPolicy(String v) {
      this.setString(securityPolicy, v, null);
   }

   public String getTransportProfile() {
      return this.getString(transportProfile);
   }

   public void setTransportProfile(String v) {
      this.setString(transportProfile, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BOpcUaClientLearnDeviceEntry() {
   }

   public BOpcUaClientLearnDeviceEntry(EndpointDescription ep) {
      this.setUri(ep.getEndpointUrl());
      this.setSecurityMode(ep.getSecurityMode().toString());
      this.setSecurityPolicy(ep.getSecurityPolicyUri().replaceFirst("http://opcfoundation\\.org/UA/SecurityPolicy#", ""));
      this.setTransportProfile(ep.getTransportProfileUri().replaceFirst("http://opcfoundation\\.org/UA-Profile/Transport/", ""));
   }

   public String getDiscoveryName() {
      return this.getUri();
   }

   public TypeInfo[] getValidDatabaseTypes() {
      return new TypeInfo[0];
   }

   public void updateTarget(BComponent target) {
   }

   public boolean isExisting(BComponent component) {
      return false;
   }

   public void defaultTargetUpdate(BComponent target) {
   }
}
