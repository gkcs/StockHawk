package com.sam_chordas.android.stockhawk.service;

import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.os.RemoteException;
import android.util.Log;

import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.GcmTaskService;
import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.LineGraphActivity;
import com.sam_chordas.android.stockhawk.SharedPreferenceManager;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.exceptions.StockDoesNotExistException;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static com.sam_chordas.android.stockhawk.data.QuoteProvider.Quotes.CONTENT_URI;
import static java.net.URLEncoder.encode;

/**
 * Created by sam_chordas on 9/30/15.
 * The GCMTask service is primarily for periodic tasks. However, OnRunTask can be called directly
 * and is used for the initialization and adding task as well.
 */
public class StockTaskService extends GcmTaskService {
    public static final String INIT = "init";
    public static final String PERIODIC = "periodic";
    public static final String UTF_8 = "UTF-8";
    public static final String ADD = "add";
    private String LOG_TAG = StockTaskService.class.getSimpleName();
    private OkHttpClient client = new OkHttpClient();
    private Context mContext;
    private StringBuilder mStoredSymbols = new StringBuilder();
    private boolean isUpdate;

    public StockTaskService(final Context context) {
        mContext = context;
    }

    String fetchData(final String url) throws IOException {
        return client.newCall(new Request.Builder().url(url).build()).execute().body().string();
    }

    @Override
    public int onRunTask(final TaskParams params) {
        boolean isHistoric = false;
        if (mContext == null) {
            mContext = this;
        }
        final StringBuilder urlStringBuilder = new StringBuilder();
        try {
            // Base URL for the Yahoo query
            urlStringBuilder.append("https://query.yahooapis.com/v1/public/yql?q=");
            if (!params.getTag().equals("historic")) {
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.quotes where symbol in (", UTF_8));
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        if (params.getTag().equals(INIT) || params.getTag().equals(PERIODIC)) {
            isUpdate = true;
            Cursor initQueryCursor = mContext.getContentResolver().query(CONTENT_URI,
                    new String[]{"Distinct " + QuoteColumns.SYMBOL}, null, null, null);
            if (initQueryCursor == null || initQueryCursor.getCount() == 0) {
                // Init task. Populates DB with quotes for the symbols seen below
                try {
                    urlStringBuilder.append(encode("\"YHOO\",\"AAPL\",\"GOOG\",\"MSFT\")", UTF_8));
                } catch (UnsupportedEncodingException e) {
                    Log.e(LOG_TAG, "onRunTask: Error while encoding url for stock:", e);
                }
            } else {
                DatabaseUtils.dumpCursor(initQueryCursor);
                initQueryCursor.moveToFirst();
                for (int i = 0; i < initQueryCursor.getCount(); i++) {
                    mStoredSymbols.append("\"")
                            .append(initQueryCursor.getString(initQueryCursor.getColumnIndex(QuoteColumns.SYMBOL)))
                            .append("\",");
                    initQueryCursor.moveToNext();
                }
                mStoredSymbols.replace(mStoredSymbols.length() - 1, mStoredSymbols.length(), ")");
                try {
                    urlStringBuilder.append(encode(mStoredSymbols.toString(), UTF_8));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
        } else if (params.getTag().equals(ADD)) {
            isUpdate = false;
            // get symbol from params.getExtra and build query
            try {
                urlStringBuilder.append(encode("\"" + params.getExtras().getString(QuoteColumns.SYMBOL) + "\")", UTF_8));
            } catch (final UnsupportedEncodingException e) {
                Log.e(LOG_TAG, "Unsupported encoding for url: ", e);
            }
        } else if (params.getTag().equals(LineGraphActivity.TABLE_HISTORIC)) {
            isHistoric = true;
            //Getting historic data for past 7 days.
            String stockInput = params.getExtras().getString(QuoteColumns.SYMBOL);
            Date startDate = new Date();
            Date endDate = new Date();
            startDate.setTime(startDate.getTime() - 10 * 24 * 60 * 60 * 1000);
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            try {
                urlStringBuilder.append(URLEncoder.encode("select * from yahoo.finance.historicaldata where symbol = \"" +
                        stockInput + "\" and startDate = \"" +
                        simpleDateFormat.format(startDate) + "\" and endDate = \"" +
                        simpleDateFormat.format(endDate) + "\"", UTF_8));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        // finalize the URL for the API query.
        urlStringBuilder.append("&format=json&diagnostics=true&env=store%3A%2F%2Fdatatables."
                + "org%2Falltableswithkeys&callback=");
        try {
            try {
                // update ISCURRENT to 0 (false) so new data is current
                if (isUpdate) {
                    final ContentValues contentValues = new ContentValues();
                    contentValues.put(QuoteColumns.ISCURRENT, 0);
                    mContext.getContentResolver().update(CONTENT_URI, contentValues, null, null);
                }
                String json = fetchData(urlStringBuilder.toString());
                Log.i(LOG_TAG, "onRunTask: Result from server: " + json);
                try {
                    mContext.getContentResolver().applyBatch(QuoteProvider.AUTHORITY,
                            Utils.quoteJsonToContentVals(json, isHistoric));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                    SharedPreferenceManager.setGCMStatus(false);
                    Log.d("status_task", SharedPreferenceManager.getGCMStatus() + "");
                    return GcmNetworkManager.RESULT_FAILURE;
                }
            } catch (NumberFormatException e) {
                Log.i(LOG_TAG, e.getMessage());
                return GcmNetworkManager.RESULT_FAILURE;
            } catch (final RemoteException | OperationApplicationException e) {
                Log.e(LOG_TAG, "Error applying batch insert", e);
            } catch (final StockDoesNotExistException e) {
                Log.i(LOG_TAG, e.getMessage());
            }
            return GcmNetworkManager.RESULT_SUCCESS;
        } catch (final IOException e) {
            Log.e(LOG_TAG, "onRunTask: Exception on running fetch data task: ", e);
            e.printStackTrace();
            return GcmNetworkManager.RESULT_FAILURE;
        }
    }
}
