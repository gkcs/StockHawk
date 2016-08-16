package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.Utils;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.service.StockIntentService;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import static android.preference.PreferenceManager.getDefaultSharedPreferences;

public class LineGraphActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String TAG = Utils.class.getSimpleName();

    public static final String COLUMN_CLOSE = "close";
    public static final String COLUMN_DATE = "date";
    public static final String COLUMN_SYMBOL = "symbol";
    public static final String COLUMN_BID_PRICE = "bid_price";
    public static final String COLUMN_CHANGE = "change";
    public static final String COLUMN_PERCENT_CHANGE = "percent_change";
    public static final String TABLE_HISTORIC = "historic";

    private LineChart mChart;
    private String mSymbol;
    private static final int CURSOR_LOADER_ID = 1;
    private TextView mChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_line_graph);
        mChart = (LineChart) findViewById(R.id.line_chart);
        TextView mStockSymbol = (TextView) findViewById(R.id.stock_symbol);
        TextView mBidPrice = (TextView) findViewById(R.id.bid_price);
        mChange = (TextView) findViewById(R.id.change);

        mChart.setContentDescription(getResources().getString(R.string.historic_data_description));
        mChart.setNoDataText(getResources().getString(R.string.historic_data_empty));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mChart.setPinchZoom(false);

        mChart.setDrawGridBackground(false);

        XAxis xAxis = mChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setAxisLineColor(Color.WHITE);
        xAxis.setTextColor(Color.WHITE);

        YAxis leftAxis = mChart.getAxisLeft();
        leftAxis.setLabelCount(10, false);
        leftAxis.setAxisLineColor(Color.WHITE);
        leftAxis.setTextColor(Color.WHITE);

        YAxis rightAxis = mChart.getAxisRight();
        rightAxis.setEnabled(false);

        mChart.getLegend().setEnabled(false);
        mChart.resetTracking();

        Intent mServiceIntent = new Intent(this, StockIntentService.class);

        mSymbol = getIntent().getStringExtra(COLUMN_SYMBOL);
        setTitle(mSymbol + " " + getResources().getString(R.string.historic_data));
        mStockSymbol.setText(mSymbol);
        mBidPrice.setText(getIntent().getStringExtra(COLUMN_BID_PRICE));
        setChange();
        Cursor c = getContentResolver().query(QuoteProvider.QuotesHistoric.withSymbol(mSymbol), null, null, null, null);
        if (c.getCount() == 0 || checkLastDate(c)) {
            getContentResolver().delete(QuoteProvider.QuotesHistoric.withSymbol(mSymbol), null, null);
            mServiceIntent.putExtra("tag", "historic");
            mServiceIntent.putExtra("symbol", mSymbol);
            startService(mServiceIntent);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    private boolean checkLastDate(Cursor c) {
        c.moveToLast();
        Date today = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(getString(R.string.date_format), Locale.ENGLISH);
        return !c.getString(c.getColumnIndex(COLUMN_DATE)).equals(simpleDateFormat.format(today));
    }

    public LineGraphActivity() {
        super();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this, QuoteProvider.QuotesHistoric.withSymbol(mSymbol),
                null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor.getCount() > 0) {
            resetChart(cursor);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public void resetChart(Cursor cursor) {
        mChart.clear();
        if (cursor.getCount() <= 0) return;
        ArrayList<Entry> entries = new ArrayList<>();
        ArrayList<String> labels = new ArrayList<>();
        int i = 0;
        while (cursor.moveToNext()) {
            float close = Float.parseFloat(cursor.getString(cursor.getColumnIndex(COLUMN_CLOSE)));
            entries.add(new Entry(close, i));
            labels.add(i, cursor.getString(cursor.getColumnIndex(COLUMN_DATE)));
            i++;
        }
        LineDataSet dataset = new LineDataSet(entries, getResources().getString(R.string.data_set));
        dataset.setAxisDependency(YAxis.AxisDependency.LEFT);

        LineData data = new LineData(labels, dataset);

        mChart.setData(data);
        mChart.invalidate();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.stock_detail, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean getPercentagePreference() {
        return getDefaultSharedPreferences(getApplicationContext())
                .getString(getString(R.string.show_percentage_key),
                        getString(R.string.show_percentage_value))
                .equals(getString(R.string.show_percentage_value));
    }

    private void setChange() {
        if (getPercentagePreference()) {
            String change = getIntent().getStringExtra(COLUMN_PERCENT_CHANGE);
            mChange.setText(change);
            setColor(change);
        } else {
            String change = getIntent().getStringExtra(COLUMN_CHANGE);
            mChange.setText(change);
            setColor(change);
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(R.string.line_graph_activity_name);
    }

    public void setColor(String change) {
        if (change.startsWith("+")) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mChange.setBackground(
                        getResources().getDrawable(R.drawable.percent_change_pill_green, null));
            } else {
                mChange.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.percent_change_pill_green));
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                mChange.setBackground(
                        getResources().getDrawable(R.drawable.percent_change_pill_red, null));
            } else {
                mChange.setBackgroundDrawable(
                        getResources().getDrawable(R.drawable.percent_change_pill_red));
            }
        }
    }
}