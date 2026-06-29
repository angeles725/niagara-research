package com.tridium.opcUaServer.history;

import com.prosysopc.ua.StatusException;
import com.prosysopc.ua.nodes.DataChangeListener;
import com.prosysopc.ua.nodes.UaNode;
import com.prosysopc.ua.nodes.UaVariable;
import com.prosysopc.ua.server.nodes.UaVariableNode;
import com.prosysopc.ua.stack.builtintypes.DataValue;
import com.prosysopc.ua.stack.builtintypes.DateTime;
import com.prosysopc.ua.stack.builtintypes.DiagnosticInfo;
import com.prosysopc.ua.stack.builtintypes.StatusCode;
import com.prosysopc.ua.stack.builtintypes.UnsignedShort;
import com.prosysopc.ua.stack.core.StatusCodes;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

class ValueHistory {
   private int capacity = 1;
   private final List<DataValue> values = new CopyOnWriteArrayList<>();
   private final UaVariable variable;
   private final DataChangeListener listener = new DataChangeListener() {
      public void onDataChange(UaNode uaNode, DataValue prevValue, DataValue value) {
         ValueHistory.this.values.add(value);

         while (ValueHistory.this.values.size() > ValueHistory.this.capacity) {
            ValueHistory.this.values.remove(0);
         }
      }
   };

   public ValueHistory(UaVariableNode variable) {
      this.variable = variable;
      variable.addDataChangeListener(this.listener);
   }

   public void deleteAtTimes(DateTime[] reqTimes, StatusCode[] operationResults, DiagnosticInfo[] operationDiagnostics) {
      for (int i = 0; i < reqTimes.length; i++) {
         try {
            this.deleteAtTime(reqTimes[i]);
            operationResults[i] = StatusCode.GOOD;
         } catch (StatusException var6) {
            operationResults[i] = var6.getStatusCode();
            operationDiagnostics[i] = var6.getDiagnosticInfo();
         }
      }
   }

   public void deleteRaw(DateTime startTime, DateTime endTime) throws StatusException {
      int i = 0;
      boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
      if (!endTimeDefined) {
         throw new StatusException(StatusCodes.Bad_InvalidArgument);
      } else {
         while (this.values.size() > i) {
            DataValue value = this.values.get(i);
            DateTime t = value.getSourceTimestamp() == null ? value.getServerTimestamp() : value.getSourceTimestamp();
            if (t == null) {
               this.values.remove(i);
            } else if (t.compareTo(startTime) >= 0) {
               this.values.remove(i);
            } else {
               if (t.compareTo(endTime) >= 0) {
                  break;
               }

               i++;
            }
         }
      }
   }

   public int getCapacity() {
      return this.capacity;
   }

   public UaVariable getVariable() {
      return this.variable;
   }

   public DataValue[] readAtTimes(DateTime[] reqTimes) {
      if (reqTimes == null) {
         return null;
      } else {
         DataValue[] values = new DataValue[reqTimes.length];

         for (int i = 0; i < reqTimes.length; i++) {
            DateTime t = reqTimes[i];
            DataValue v = this.getValue(t);
            values[i] = new DataValue(
               v == null ? null : v.getValue(), v == null ? StatusCode.valueOf(StatusCodes.Bad_NoData) : v.getStatusCode(), t, UnsignedShort.ZERO, null, null
            );
         }

         return values;
      }
   }

   public Integer readRaw(DateTime startTime, DateTime endTime, int maxValues, boolean returnBounds, int firstIndex, List<DataValue> history) {
      int i = 0;
      boolean startTimeDefined = startTime.compareTo(DateTime.MIN_VALUE) > 0;
      boolean endTimeDefined = endTime.compareTo(DateTime.MIN_VALUE) > 0;
      if (!startTimeDefined && endTimeDefined) {
         for (int j = this.values.size() - 1; j >= 0; j--) {
            DataValue value = this.values.get(j);
            DateTime t = value.getSourceTimestamp() == null ? value.getServerTimestamp() : value.getSourceTimestamp();
            if (t != null) {
               int compareToEnd = t.compareTo(endTime);
               if (compareToEnd <= 0 && (returnBounds || compareToEnd != 0)) {
                  if (i >= firstIndex) {
                     history.add(value);
                  }

                  i++;
                  if (history.size() == maxValues) {
                     return i;
                  }
               }
            }
         }
      } else {
         for (DataValue value : this.values) {
            DateTime t = value.getSourceTimestamp() == null ? value.getServerTimestamp() : value.getSourceTimestamp();
            if (t != null) {
               int compareToEnd = endTimeDefined ? t.compareTo(endTime) : -1;
               if (compareToEnd > 0 || !returnBounds && compareToEnd == 0) {
                  break;
               }

               int compareToStart = t.compareTo(startTime);
               if (compareToStart > 0 || returnBounds && compareToStart == 0) {
                  if (i >= firstIndex) {
                     history.add(value);
                  }

                  i++;
                  if (history.size() == maxValues) {
                     return i;
                  }
               }
            }
         }
      }

      return null;
   }

   public void setCapacity(int capacity) {
      if (capacity < 0) {
         throw new IllegalArgumentException("capacity must be a positive value");
      } else {
         this.capacity = capacity;
      }
   }

   private void deleteAtTime(DateTime timestamp) throws StatusException {
      boolean found = false;

      for (int i = this.values.size() - 1; i >= 0; i--) {
         int compareTo = timestamp.compareTo(this.values.get(i).getSourceTimestamp());
         if (compareTo == 0) {
            this.values.remove(i);
            found = true;
         } else if (compareTo < 0) {
            break;
         }
      }

      if (!found) {
         throw new StatusException(StatusCodes.Bad_NoData);
      }
   }

   private DataValue getValue(DateTime requestedTime) {
      int i = this.values.size() - 1;

      while (i >= 0 && this.values.get(i).getSourceTimestamp().compareTo(requestedTime) > 0) {
         i--;
      }

      return i < 0 ? null : this.values.get(i);
   }
}
