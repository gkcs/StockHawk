package com.sam_chordas.android.stockhawk.rest;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;
import android.widget.RemoteViewsService.RemoteViewsFactory;

import com.sam_chordas.android.stockhawk.LineGraphActivity;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.SharedPreferenceManager;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;

public class WidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {

        WidgetDataProvider dataProvider = new WidgetDataProvider(
                getApplicationContext(), intent);
        return dataProvider;
    }

}

class WidgetDataProvider implements RemoteViewsFactory {

    Cursor mCollections;
    private int mAppWidgetId;
    Context mContext;

    public WidgetDataProvider(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    @Override
    public int getCount() {
        return mCollections.getCount();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews mView = new RemoteViews(mContext.getPackageName(),
                R.layout.widget_item);
        if(mCollections.moveToPosition(position)) {
            mView.setTextViewText(R.id.stock_symbol, mCollections.getString(mCollections.getColumnIndex(LineGraphActivity.COLUMN_SYMBOL)));
            mView.setTextViewText(R.id.bid_price, mCollections.getString(mCollections.getColumnIndex(LineGraphActivity.COLUMN_BID_PRICE)));

            if (mCollections.getInt(mCollections.getColumnIndex("is_up")) == 1) {
                mView.setTextColor(R.id.change, Color.GREEN);
            } else {
                mView.setTextColor(R.id.change, Color.RED);
            }
            if (SharedPreferenceManager.getShowPercentage()) {
                mView.setTextViewText(R.id.change, mCollections.getString(mCollections.getColumnIndex(LineGraphActivity.COLUMN_PERCENT_CHANGE)));
            } else {
                mView.setTextViewText(R.id.change, mCollections.getString(mCollections.getColumnIndex(LineGraphActivity.COLUMN_CHANGE)));
            }
        }
        return mView;
    }


    @Override
    public void onCreate() {
        Thread thread = new Thread() {
            public void run() {
                mCollections = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onDataSetChanged() {
        if (mCollections != null) {
            mCollections.close();
        }

        Thread thread = new Thread() {
            public void run() {
                mCollections = mContext.getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                        new String[]{ QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                                QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                        QuoteColumns.ISCURRENT + " = ?",
                        new String[]{"1"},
                        null);
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (mCollections != null) {
            mCollections.close();
        }
    }

}