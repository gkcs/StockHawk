package com.sam_chordas.android.stockhawk.data;

import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.Table;

@Database(version = QuoteDatabase.VERSION)
public class QuoteDatabase {
    private QuoteDatabase() {
    }

    public static final int VERSION = 8;

    @Table(QuoteColumns.class)
    public static final String QUOTES = "quotes";

    @Table(QuoteHistoricData.class)
    public static final String QUOTE_HISTORIC = "quote_historic";
}
