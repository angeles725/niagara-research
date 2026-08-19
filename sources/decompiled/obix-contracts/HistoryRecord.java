/*
 * Decompiled with CFR 0.152.
 */
package obix.contracts;

import obix.Abstime;
import obix.IObj;
import obix.Obj;

public interface HistoryRecord
extends IObj {
    public static final String timestampContract = "<abstime name='timestamp' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String valueContract = "<obj name='value' null='true'/>";

    public Abstime timestamp();

    public Obj value();
}

