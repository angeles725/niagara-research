package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NEventSummary;
import com.tridium.bacnet.services.BacnetComplexAck;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.io.AsnException;

public class GetEventInformationAck extends BacnetComplexAck {
   public static final int LIST_OF_EVENT_SUMMARIES_TAG = 0;
   public static final int MORE_EVENTS_TAG = 1;
   private Vector<NEventSummary> listOfEventSummaries;
   private boolean moreEvents;

   public GetEventInformationAck() {
      super(29);
      this.listOfEventSummaries = new Vector<>();
   }

   public GetEventInformationAck(Vector<NEventSummary> listOfEventSummaries, boolean moreEvents) {
      super(29);
      this.listOfEventSummaries = listOfEventSummaries;
      this.moreEvents = moreEvents;
   }

   public GetEventInformationAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.listOfEventSummaries = new Vector<>();
      this.readEncoded(inputStream);
   }

   public void addEventSummary(NEventSummary eventSummary) {
      this.listOfEventSummaries.addElement(eventSummary);
   }

   public ListIterator<NEventSummary> getEventSummaries() {
      return this.listOfEventSummaries.listIterator();
   }

   public Vector<NEventSummary> getListOfEventSummaries() {
      return this.listOfEventSummaries;
   }

   public boolean isMoreEvents() {
      return this.moreEvents;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      out.writeOpeningTag(0);

      for (NEventSummary eventSummary : this.listOfEventSummaries) {
         eventSummary.writeAsn(out);
      }

      out.writeClosingTag(0);
      out.writeBoolean(1, this.moreEvents);
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      in.skipTag();
      if (!in.isOpeningTag(0)) {
         throw new AsnException("Expected opening tag");
      } else {
         for (int tag = in.peekTag(); !in.isClosingTag(0); tag = in.peekTag()) {
            if (tag == -1) {
               throw new AsnException("No closing tag");
            }

            NEventSummary esum = new NEventSummary();
            esum.readAsn(in);
            this.listOfEventSummaries.addElement(esum);
         }

         in.skipTag();
         this.moreEvents = in.readBoolean(1);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("GetEventInformationAck: ");

      for (NEventSummary eventSummary : this.listOfEventSummaries) {
         sb.append(eventSummary.toString());
      }

      sb.append("\n  moreEvents: " + this.moreEvents);
      return sb.toString();
   }
}
