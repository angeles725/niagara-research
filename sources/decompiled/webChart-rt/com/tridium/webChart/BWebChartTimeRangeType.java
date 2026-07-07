package com.tridium.webChart;

import javax.baja.nre.annotations.NiagaraEnum;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.nre.annotations.Range;
import javax.baja.sys.BFrozenEnum;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
@NiagaraEnum(
   range = {@Range("auto"), @Range("timeRange"), @Range("today"), @Range("last24Hours"), @Range("yesterday"), @Range("weekToDate"), @Range("lastWeek"), @Range("last7Days"), @Range("monthToDate"), @Range("lastMonth"), @Range("yearToDate"), @Range("lastYear")},
   defaultValue = "today"
)
public final class BWebChartTimeRangeType extends BFrozenEnum {
   public static final int AUTO = 0;
   public static final int TIME_RANGE = 1;
   public static final int TODAY = 2;
   public static final int LAST_24HOURS = 3;
   public static final int YESTERDAY = 4;
   public static final int WEEK_TO_DATE = 5;
   public static final int LAST_WEEK = 6;
   public static final int LAST_7DAYS = 7;
   public static final int MONTH_TO_DATE = 8;
   public static final int LAST_MONTH = 9;
   public static final int YEAR_TO_DATE = 10;
   public static final int LAST_YEAR = 11;
   public static final BWebChartTimeRangeType auto = new BWebChartTimeRangeType(0);
   public static final BWebChartTimeRangeType timeRange = new BWebChartTimeRangeType(1);
   public static final BWebChartTimeRangeType today = new BWebChartTimeRangeType(2);
   public static final BWebChartTimeRangeType last24Hours = new BWebChartTimeRangeType(3);
   public static final BWebChartTimeRangeType yesterday = new BWebChartTimeRangeType(4);
   public static final BWebChartTimeRangeType weekToDate = new BWebChartTimeRangeType(5);
   public static final BWebChartTimeRangeType lastWeek = new BWebChartTimeRangeType(6);
   public static final BWebChartTimeRangeType last7Days = new BWebChartTimeRangeType(7);
   public static final BWebChartTimeRangeType monthToDate = new BWebChartTimeRangeType(8);
   public static final BWebChartTimeRangeType lastMonth = new BWebChartTimeRangeType(9);
   public static final BWebChartTimeRangeType yearToDate = new BWebChartTimeRangeType(10);
   public static final BWebChartTimeRangeType lastYear = new BWebChartTimeRangeType(11);
   public static final BWebChartTimeRangeType DEFAULT = today;
   public static final Type TYPE = Sys.loadType(BWebChartTimeRangeType.class);

   public static BWebChartTimeRangeType make(int ordinal) {
      return (BWebChartTimeRangeType)auto.getRange().get(ordinal, false);
   }

   public static BWebChartTimeRangeType make(String tag) {
      return (BWebChartTimeRangeType)auto.getRange().get(tag);
   }

   private BWebChartTimeRangeType(int ordinal) {
      super(ordinal);
   }

   public Type getType() {
      return TYPE;
   }
}
