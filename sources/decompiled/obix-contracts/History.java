/*
 * Decompiled with CFR 0.152.
 */
package obix.contracts;

import obix.Abstime;
import obix.Feed;
import obix.IObj;
import obix.Int;
import obix.Op;

public interface History
extends IObj {
    public static final String countContract = "<int name='count' val='0' min='0'/>";
    public static final String startContract = "<abstime name='start' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String endContract = "<abstime name='end' val='1969-12-31T19:00:00.000-05:00' null='true'/>";
    public static final String queryContract = "<op name='query' in='obix:HistoryFilter' out='obix:HistoryQueryOut'/>";
    public static final String feedContract = "<feed name='feed' in='obix:HistoryFilter' of='obix:HistoryRecord'/>";
    public static final String rollupContract = "<op name='rollup' in='obix:HistoryRollupIn' out='obix:HistoryRollupOut'/>";

    public Int count();

    public Abstime start();

    public Abstime end();

    public Op query();

    public Feed feed();

    public Op rollup();
}

