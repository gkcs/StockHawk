package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.TaskParams;
import com.sam_chordas.android.stockhawk.SharedPreferenceManager;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.ui.MyStocksActivity;

public class StockIntentService extends IntentService {

    public static final String TAG = "tag";
    public static final String ADD = "add";
    public static final String HISTORIC = "historic";

    public StockIntentService() {
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(StockIntentService.class.getSimpleName(), "Stock Intent Service");
        final Bundle args = new Bundle();
        if (intent.getStringExtra(TAG).equals(ADD) || intent.getStringExtra(TAG).equals(HISTORIC)) {
            args.putString(QuoteColumns.SYMBOL, intent.getStringExtra(QuoteColumns.SYMBOL));
        }
        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.
        new StockTaskService(this).onRunTask(new TaskParams(intent.getStringExtra(TAG), args));
        if (!SharedPreferenceManager.getGCMStatus()) {
            sendBroadcast(new Intent(MyStocksActivity.RESULT_FAILURE));
            SharedPreferenceManager.setGCMStatus(true);
        }
    }
}
