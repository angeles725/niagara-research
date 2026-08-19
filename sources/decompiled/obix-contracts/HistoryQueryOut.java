/*
 * Decompiled with CFR 0.152.
 */
package obix.contracts;

import obix.Abstime;
import obix.IObj;
import obix.Int;
import obix.List;

public interface HistoryQueryOut
extends IObj {
    public static final String countContract = "<int name='count' val='0' min='0'/>";
    public static final String startContract = "<abstime name='start' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String endContract = "<abstime name='end' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String dataContract = "<list name='data' of='obix:HistoryRecord'/>";

    public Int count();

    public Abstime start();

    public Abstime end();

    public List data();
}

