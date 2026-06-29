package com.tridium.bacnet.services.confirmed;

import com.tridium.bacnet.asn.AsnInputStream;
import com.tridium.bacnet.asn.AsnOutputStream;
import com.tridium.bacnet.asn.NAlarmSummary;
import com.tridium.bacnet.services.BacnetComplexAck;
import java.util.ListIterator;
import java.util.Vector;
import javax.baja.bacnet.io.AsnException;

public class GetAlarmSummaryAck extends BacnetComplexAck {
   private Vector<NAlarmSummary> listOfAlarmSummaries;

   public GetAlarmSummaryAck() {
      super(3);
      this.listOfAlarmSummaries = new Vector<>();
   }

   public GetAlarmSummaryAck(Vector<NAlarmSummary> listOfAlarmSummaries) {
      super(3);
      this.listOfAlarmSummaries = listOfAlarmSummaries;
   }

   public GetAlarmSummaryAck(int serviceChoice, AsnInputStream inputStream) throws AsnException {
      super(serviceChoice);
      this.listOfAlarmSummaries = new Vector<>();
      this.readEncoded(inputStream);
   }

   public void addAlarmSummary(NAlarmSummary eventSummary) {
      this.listOfAlarmSummaries.addElement(eventSummary);
   }

   public ListIterator<NAlarmSummary> getAlarmSummaries() {
      return this.listOfAlarmSummaries.listIterator();
   }

   public Vector<NAlarmSummary> getListOfAlarmSummaries() {
      return this.listOfAlarmSummaries;
   }

   @Override
   public void writeEncoded(AsnOutputStream out) {
      for (NAlarmSummary eventSummary : this.listOfAlarmSummaries) {
         eventSummary.writeAsn(out);
      }
   }

   @Override
   public void readEncoded(AsnInputStream in) throws AsnException {
      while (in.peekTag() != -1) {
         NAlarmSummary asum = new NAlarmSummary();
         asum.readAsn(in);
         this.listOfAlarmSummaries.addElement(asum);
      }
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder("GetAlarmSummaryAck: ");

      for (NAlarmSummary eventSummary : this.listOfAlarmSummaries) {
         sb.append(eventSummary.toString());
      }

      return sb.toString();
   }
}
