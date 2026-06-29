package javax.baja.bacnet.datatypes;

import javax.baja.bacnet.io.AsnException;
import javax.baja.bacnet.io.AsnInput;
import javax.baja.bacnet.io.AsnOutput;
import javax.baja.bacnet.io.PropertyReference;
import javax.baja.bacnet.io.PropertyValue;
import javax.baja.naming.SlotPath;
import javax.baja.nre.annotations.Facet;
import javax.baja.nre.annotations.NiagaraProperties;
import javax.baja.nre.annotations.NiagaraProperty;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.status.BStatus;
import javax.baja.status.BStatusValue;
import javax.baja.sys.BAbsTime;
import javax.baja.sys.BFacets;
import javax.baja.sys.BFloat;
import javax.baja.sys.BNumber;
import javax.baja.sys.BRelTime;
import javax.baja.sys.BStruct;
import javax.baja.sys.BValue;
import javax.baja.sys.Context;
import javax.baja.sys.Property;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;
import javax.baja.sys.Clock.Ticket;

@NiagaraType
@NiagaraProperties({@NiagaraProperty(
      name = "recipient",
      type = "BBacnetRecipientProcess",
      defaultValue = "new BBacnetRecipientProcess()"
   ), @NiagaraProperty(
      name = "monitoredPropertyReference",
      type = "BBacnetObjectPropertyReference",
      defaultValue = "new BBacnetObjectPropertyReference()"
   ), @NiagaraProperty(
      name = "issueConfirmedNotifications",
      type = "boolean",
      defaultValue = "false"
   ), @NiagaraProperty(
      name = "subscriptionEndTime",
      type = "BAbsTime",
      defaultValue = "BAbsTime.NULL",
      facets = {@Facet(
         name = "BFacets.SHOW_SECONDS",
         value = "true"
      ), @Facet(
         name = "BFacets.SHOW_MILLISECONDS",
         value = "true"
      )}
   ), @NiagaraProperty(
      name = "covIncrement",
      type = "float",
      defaultValue = "BFloat.NaN"
   )})
public final class BBacnetCovSubscription extends BStruct implements BIBacnetDataType {
   public static final Property recipient = newProperty(0, new BBacnetRecipientProcess(), null);
   public static final Property monitoredPropertyReference = newProperty(0, new BBacnetObjectPropertyReference(), null);
   public static final Property issueConfirmedNotifications = newProperty(0, false, null);
   public static final Property subscriptionEndTime = newProperty(
      0, BAbsTime.NULL, BFacets.make(BFacets.make("showSeconds", true), BFacets.make("showMilliseconds", true))
   );
   public static final Property covIncrement = newProperty(0, BFloat.NaN, null);
   public static final Type TYPE = Sys.loadType(BBacnetCovSubscription.class);
   public static final int RECIPIENT_TAG = 0;
   public static final int MONITORED_PROPERTY_REFERENCE_TAG = 1;
   public static final int ISSUE_CONFIRMED_NOTIFICATIONS_TAG = 2;
   public static final int TIME_REMAINING_TAG = 3;
   public static final int COV_INCREMENT_TAG = 4;
   public static final int MAX_ENCODED_SIZE = 44;
   private Ticket ticket;
   private boolean covProperty = false;
   private BValue lastPropValue = null;
   private PropertyValue lastPropertyValue = null;
   private BStatus lastStatusFlags = null;

   public BBacnetRecipientProcess getRecipient() {
      return (BBacnetRecipientProcess)this.get(recipient);
   }

   public void setRecipient(BBacnetRecipientProcess v) {
      this.set(recipient, v, null);
   }

   public BBacnetObjectPropertyReference getMonitoredPropertyReference() {
      return (BBacnetObjectPropertyReference)this.get(monitoredPropertyReference);
   }

   public void setMonitoredPropertyReference(BBacnetObjectPropertyReference v) {
      this.set(monitoredPropertyReference, v, null);
   }

   public boolean getIssueConfirmedNotifications() {
      return this.getBoolean(issueConfirmedNotifications);
   }

   public void setIssueConfirmedNotifications(boolean v) {
      this.setBoolean(issueConfirmedNotifications, v, null);
   }

   public BAbsTime getSubscriptionEndTime() {
      return (BAbsTime)this.get(subscriptionEndTime);
   }

   public void setSubscriptionEndTime(BAbsTime v) {
      this.set(subscriptionEndTime, v, null);
   }

   public float getCovIncrement() {
      return this.getFloat(covIncrement);
   }

   public void setCovIncrement(float v) {
      this.setFloat(covIncrement, v, null);
   }

   public Type getType() {
      return TYPE;
   }

   public BBacnetCovSubscription() {
   }

   public BBacnetCovSubscription(
      BBacnetAddress subscriberAddress, long subscriberProcessId, BBacnetObjectIdentifier monitoredObjectId, boolean issueConfirmedNotifications
   ) {
      this.getRecipient().getRecipient().setRecipient(subscriberAddress);
      this.getRecipient().setProcessIdentifier(BBacnetUnsigned.make(subscriberProcessId));
      this.getMonitoredPropertyReference().setObjectId(monitoredObjectId);
      this.setIssueConfirmedNotifications(issueConfirmedNotifications);
   }

   public BBacnetCovSubscription(
      BBacnetAddress subscriberAddress,
      long subscriberProcessId,
      BBacnetObjectIdentifier monitoredObjectId,
      PropertyReference monitoredPropertyId,
      boolean issueConfirmedNotifications,
      BNumber covIncr
   ) {
      this.getRecipient().getRecipient().setRecipient(subscriberAddress);
      this.getRecipient().setProcessIdentifier(BBacnetUnsigned.make(subscriberProcessId));
      this.getMonitoredPropertyReference().setObjectId(monitoredObjectId);
      this.getMonitoredPropertyReference().setPropertyId(monitoredPropertyId.getPropertyId());
      this.getMonitoredPropertyReference().setPropertyArrayIndex(monitoredPropertyId.getPropertyArrayIndex());
      this.setIssueConfirmedNotifications(issueConfirmedNotifications);
      this.setCovIncrement(covIncr != null ? covIncr.getFloat() : Float.NaN);
   }

   public BStatusValue getLastValue() {
      return this.covProperty ? null : (BStatusValue)this.lastPropValue;
   }

   public void setLastValue(BStatusValue newValue) {
      this.lastPropValue = newValue.newCopy();
   }

   public BValue getLastPropValue() {
      return this.lastPropValue;
   }

   public void setLastPropValue(BValue newValue) {
      this.lastPropValue = newValue.newCopy();
   }

   public PropertyValue getLastPropertyValue() {
      return this.lastPropertyValue;
   }

   public void setLastPropertyValue(PropertyValue lastPropertyValue) {
      this.lastPropertyValue = lastPropertyValue;
   }

   public Ticket getTicket() {
      return this.ticket;
   }

   public void setTicket(Ticket ticket) {
      this.ticket = ticket;
   }

   public int getTimeRemaining() {
      if (this.getSubscriptionEndTime().equals(BAbsTime.NULL)) {
         return 0;
      } else {
         long curTime = BAbsTime.make().getMillis();
         int timeRemaining = (int)((this.getSubscriptionEndTime().getMillis() - curTime) / 1000L);
         return timeRemaining > 0 ? timeRemaining : -1;
      }
   }

   public String toString(Context cx) {
      if (cx != null && cx.equals(nameContext)) {
         return this.getNameString();
      } else {
         StringBuilder sb = new StringBuilder();
         sb.append(this.covProperty ? "CovPSub " : "CovSub ")
            .append(this.getRecipient().toString(cx))
            .append('{')
            .append(this.getMonitoredPropertyReference().toString(cx));
         if (this.covProperty) {
            sb.append(':').append(this.getCovIncrement());
         }

         sb.append(this.getIssueConfirmedNotifications() ? "} C until " : "} U until ").append(this.getSubscriptionEndTime());
         return sb.toString();
      }
   }

   public boolean isCovIncrementUsed() {
      return !Float.isNaN(this.getCovIncrement());
   }

   public boolean isCovProperty() {
      return this.covProperty;
   }

   public void setCovProperty(boolean v) {
      this.covProperty = v;
   }

   public int getLastStatusBits() {
      BStatus status = this.getLastStatusFlags();
      return status != null ? status.getBits() : 0;
   }

   public void setLastStatusBits(int lastStatusBits) {
      this.lastStatusFlags = BStatus.make(lastStatusBits & 43);
   }

   public void setLastStatusFlags(BStatus lastStatusFlags) {
      this.lastStatusFlags = BStatus.make(lastStatusFlags.getBits() & 43);
   }

   public BStatus getLastStatusFlags() {
      return this.lastStatusFlags;
   }

   private void setTimeRemaining(long timeRemaining) {
      this.set(subscriptionEndTime, BAbsTime.make().add(BRelTime.make(timeRemaining * 1000L)), noWrite);
   }

   private String getNameString() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.covProperty ? "covP_" : "cov_")
         .append(SlotPath.unescape(this.getRecipient().toString(nameContext)))
         .append("_")
         .append(this.getMonitoredPropertyReference().toString(nameContext));
      if (this.covProperty) {
         sb.append('_').append(this.getCovIncrement());
      }

      return SlotPath.escape(sb.toString());
   }

   @Override
   public void writeAsn(AsnOutput out) {
      out.writeOpeningTag(0);
      this.getRecipient().writeAsn(out);
      out.writeClosingTag(0);
      out.writeOpeningTag(1);
      this.getMonitoredPropertyReference().writeAsn(out);
      out.writeClosingTag(1);
      out.writeBoolean(2, this.getIssueConfirmedNotifications());
      out.writeUnsignedInteger(3, this.getTimeRemaining());
      if (this.isCovIncrementUsed()) {
         out.writeReal(4, this.getCovIncrement());
      }
   }

   @Override
   public void readAsn(AsnInput in) throws AsnException {
      in.skipOpeningTag(0);
      BBacnetRecipientProcess recipient = new BBacnetRecipientProcess();
      recipient.readAsn(in);
      in.skipClosingTag(0);
      in.skipOpeningTag(1);
      BBacnetObjectPropertyReference monitoredPropertyReference = new BBacnetObjectPropertyReference();
      monitoredPropertyReference.readAsn(in);
      in.skipClosingTag(1);
      boolean issueConfirmedNotifications = in.readBoolean(2);
      long timeRemaining = in.readUnsignedInteger(3);
      in.peekTag();
      float covIncrement = in.isValueTag(4) ? in.readReal(4) : Float.NaN;
      this.set(BBacnetCovSubscription.recipient, recipient, noWrite);
      this.set(BBacnetCovSubscription.monitoredPropertyReference, monitoredPropertyReference, noWrite);
      this.setBoolean(BBacnetCovSubscription.issueConfirmedNotifications, issueConfirmedNotifications, noWrite);
      this.setTimeRemaining(timeRemaining);
      this.setFloat(BBacnetCovSubscription.covIncrement, covIncrement, noWrite);
   }
}
