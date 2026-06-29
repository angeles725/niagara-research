package com.tridium.bacnet.history;

import java.io.IOException;
import javax.baja.bacnet.io.ErrorType;
import javax.baja.history.BHistoryConfig;
import javax.baja.history.BIHistory;
import javax.baja.history.BTrendRecord;
import javax.baja.history.HistoryException;
import javax.baja.history.ext.BActivePeriod;
import javax.baja.nre.annotations.NiagaraType;
import javax.baja.sys.BInterface;
import javax.baja.sys.Context;
import javax.baja.sys.Sys;
import javax.baja.sys.Type;

@NiagaraType
public interface BIBacnetTrendLogExt extends BInterface {
   Type TYPE = Sys.loadType(BIBacnetTrendLogExt.class);

   BIHistory getHistory();

   BHistoryConfig getHistoryConfig();

   BBacnetTrendRecord getRecord();

   void append(BTrendRecord var1) throws IOException, HistoryException;

   long getTotalRecordCount();

   void setTotalRecordCount(long var1);

   boolean getEnabled();

   BActivePeriod getActivePeriod();

   ErrorType setLogInterval(long var1, Context var3);

   boolean getTrigger();
}
