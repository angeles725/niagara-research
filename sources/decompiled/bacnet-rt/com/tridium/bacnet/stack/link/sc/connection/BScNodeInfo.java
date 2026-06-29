package com.tridium.bacnet.stack.link.sc.connection;

import com.tridium.bacnet.stack.link.sc.BScHubConnectorState;
import com.tridium.bacnet.stack.link.sc.ScLinkLayerUtil;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BFacets;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BString;
import javax.baja.sys.BStruct;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "hubConnectorStatus",
      type = "BScHubConnectorState",
      defaultValue = "BScHubConnectorState.DEFAULT",
      flags = 1
   ), @NiagaraProperty(
      name = "acceptsDirectConnections",
      type = "boolean",
      defaultValue = "false",
      flags = 1
   ), @NiagaraProperty(
      name = "directUris",
      type = "BString",
      defaultValue = "BString.DEFAULT",
      flags = 1,
      facets = {@Facet(
         name = "BFacets.MULTI_LINE",
         value = "true"
      ), @Facet(
         name = "BFacets.FIELD_WIDTH",
         value = "80"
      )}
   ), @NiagaraProperty(
      name = "maxBvlcLength",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "maxNpduLength",
      type = "int",
      defaultValue = "0",
      flags = 1
   ), @NiagaraProperty(
      name = "addressResolutionStatus",
      type = "String",
      defaultValue = "getLexiconText(\"directInitiatingConnection.sendInfoMessageStatus.notAttempted\")",
      flags = 1
   ), @NiagaraProperty(
      name = "addressResolutionTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(10)"
   ), @NiagaraProperty(
      name = "advertisementSolicitationStatus",
      type = "String",
      defaultValue = "getLexiconText(\"directInitiatingConnection.sendInfoMessageStatus.notAttempted\")",
      flags = 1
   ), @NiagaraProperty(
      name = "advertisementSolicitationTimeout",
      type = "BRelTime",
      defaultValue = "BRelTime.makeSeconds(10)"
   )})
public final class BScNodeInfo extends BStruct {
   public static final Property hubConnectorStatus = newProperty(1, BScHubConnectorState.DEFAULT, null);
   public static final Property acceptsDirectConnections = newProperty(1, false, null);
   public static final Property directUris = newProperty(1, BString.DEFAULT, BFacets.make(BFacets.make("multiLine", true), BFacets.make("fieldWidth", 80)));
   public static final Property maxBvlcLength = newProperty(1, 0, null);
   public static final Property maxNpduLength = newProperty(1, 0, null);
   public static final Property addressResolutionStatus = newProperty(
      1, ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.notAttempted"), null
   );
   public static final Property addressResolutionTimeout = newProperty(0, BRelTime.makeSeconds(10), null);
   public static final Property advertisementSolicitationStatus = newProperty(
      1, ScLinkLayerUtil.getLexiconText("directInitiatingConnection.sendInfoMessageStatus.notAttempted"), null
   );
   public static final Property advertisementSolicitationTimeout = newProperty(0, BRelTime.makeSeconds(10), null);
   public static final Type TYPE = Sys.loadType(BScNodeInfo.class);

   public BScHubConnectorState getHubConnectorStatus() {
      return (BScHubConnectorState)this.get(hubConnectorStatus);
   }

   public void setHubConnectorStatus(BScHubConnectorState v) {
      this.set(hubConnectorStatus, v, null);
   }

   public boolean getAcceptsDirectConnections() {
      return this.getBoolean(acceptsDirectConnections);
   }

   public void setAcceptsDirectConnections(boolean v) {
      this.setBoolean(acceptsDirectConnections, v, null);
   }

   public String getDirectUris() {
      return this.getString(directUris);
   }

   public void setDirectUris(String v) {
      this.setString(directUris, v, null);
   }

   public int getMaxBvlcLength() {
      return this.getInt(maxBvlcLength);
   }

   public void setMaxBvlcLength(int v) {
      this.setInt(maxBvlcLength, v, null);
   }

   public int getMaxNpduLength() {
      return this.getInt(maxNpduLength);
   }

   public void setMaxNpduLength(int v) {
      this.setInt(maxNpduLength, v, null);
   }

   public String getAddressResolutionStatus() {
      return this.getString(addressResolutionStatus);
   }

   public void setAddressResolutionStatus(String v) {
      this.setString(addressResolutionStatus, v, null);
   }

   public BRelTime getAddressResolutionTimeout() {
      return (BRelTime)this.get(addressResolutionTimeout);
   }

   public void setAddressResolutionTimeout(BRelTime v) {
      this.set(addressResolutionTimeout, v, null);
   }

   public String getAdvertisementSolicitationStatus() {
      return this.getString(advertisementSolicitationStatus);
   }

   public void setAdvertisementSolicitationStatus(String v) {
      this.setString(advertisementSolicitationStatus, v, null);
   }

   public BRelTime getAdvertisementSolicitationTimeout() {
      return (BRelTime)this.get(advertisementSolicitationTimeout);
   }

   public void setAdvertisementSolicitationTimeout(BRelTime v) {
      this.set(advertisementSolicitationTimeout, v, null);
   }

   public Type getType() {
      return TYPE;
   }
}
