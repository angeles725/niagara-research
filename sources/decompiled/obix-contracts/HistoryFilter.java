/*
 * Decompiled with CFR 0.152.
 */
package obix.contracts;

import obix.Abstime;
import obix.IObj;
import obix.Int;

public interface HistoryFilter
extends IObj {
    public static final String limitContract = "<int name='limit' val='0' null='true'/>";
    public static final String startContract = "<abstime name='start' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String endContract = "<abstime name='end' val='1969-12-31T19:00:00.000-05:00' null='true'/>";

    public Int limit();

    public Abstime start();

    public Abstime end();
}

