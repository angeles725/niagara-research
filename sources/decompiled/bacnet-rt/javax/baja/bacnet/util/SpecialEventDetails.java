package javax.baja.bacnet.util;

import java.util.List;
import javax.baja.schedule.BAbstractSchedule;
import javax.baja.schedule.BTimeSchedule;

public class SpecialEventDetails {
   private BAbstractSchedule daysSchedule;
   private List<BTimeSchedule> timeSchedules;

   public SpecialEventDetails(BAbstractSchedule daysSchedule, List<BTimeSchedule> timeSchedules) {
      this.daysSchedule = daysSchedule;
      this.timeSchedules = timeSchedules;
   }

   public BAbstractSchedule getDaysSchedule() {
      return this.daysSchedule;
   }

   public List<BTimeSchedule> getTimeSchedules() {
      return this.timeSchedules;
   }
}
