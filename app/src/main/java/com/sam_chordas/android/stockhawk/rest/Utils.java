package com.sam_chordas.android.stockhawk.rest;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteHistoricData;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.exceptions.StockDoesNotExistException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class Utils {

    public static final String QUERY_PARAM = "query";
    public static final String COUNT = "count";
    public static final String RESULTS_PARAM = "results";
    public static final String QUOTE_PARAM = "quote";
    public static final String BID_PARAM = "Bid";
    public static final String NULL = "null";
    public static final String SYMBOL_PARAM = "symbol";
    public static final String CHANGE_IN_PERCENT = "ChangeinPercent";
    public static final String CHANGE_PARAM = "Change";
    private static String LOG_TAG = Utils.class.getSimpleName();

    public static boolean showPercent = true;

    public static ArrayList<ContentProviderOperation> quoteJsonToContentVals(String JSON, boolean isHistoric) throws StockDoesNotExistException {
        ArrayList<ContentProviderOperation> batchOperations = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(JSON);
            if (jsonObject.length() != 0) {
                jsonObject = jsonObject.getJSONObject(QUERY_PARAM);
                int count = Integer.parseInt(jsonObject.getString(COUNT));
                if (count == 1) {
                    jsonObject = jsonObject.getJSONObject(RESULTS_PARAM).getJSONObject(QUOTE_PARAM);
                    if (jsonObject.getString(BID_PARAM) != null && !NULL.equalsIgnoreCase(jsonObject.getString(BID_PARAM))) {
                        if (isHistoric) {
                            batchOperations.add(buildBatchOperationHistoric(jsonObject));
                        } else {
                            batchOperations.add(buildBatchOperation(jsonObject));
                        }
                    } else {
                        throw new StockDoesNotExistException("Stock not found on server");
                    }
                } else {
                    final JSONArray resultsArray = jsonObject.getJSONObject(RESULTS_PARAM).getJSONArray(QUOTE_PARAM);
                    if (resultsArray != null && resultsArray.length() != 0) {
                        for (int i = 0; i < resultsArray.length(); i++) {
                            if (isHistoric) {
                                batchOperations.add(buildBatchOperationHistoric(resultsArray.getJSONObject(i)));
                            } else {
                                batchOperations.add(buildBatchOperation(resultsArray.getJSONObject(i)));
                            }
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "String to JSON failed: " + e);
        }
        return batchOperations;
    }

    public static String truncateBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String truncateChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuilder builder = new StringBuilder(change);
        builder.insert(0, weight);
        builder.append(ampersand);
        change = builder.toString();
        return change;
    }

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString(CHANGE_PARAM);
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString(SYMBOL_PARAM));
            builder.withValue(QuoteColumns.BIDPRICE, truncateBidPrice(jsonObject.getString(BID_PARAM)));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, truncateChange(
                    jsonObject.getString(CHANGE_IN_PERCENT), true));
            builder.withValue(QuoteColumns.CHANGE, truncateChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static ContentProviderOperation buildBatchOperationHistoric(JSONObject jsonObject) {
        try {
            return ContentProviderOperation.newInsert(QuoteProvider.QuotesHistoric.CONTENT_URI)
                    .withValue(QuoteHistoricData.SYMBOL, jsonObject.getString("Symbol"))
                    .withValue(QuoteHistoricData.DATE, jsonObject.getString("Date"))
                    .withValue(QuoteHistoricData.OPEN, jsonObject.getString("Open"))
                    .withValue(QuoteHistoricData.HIGH, jsonObject.getString("High"))
                    .withValue(QuoteHistoricData.LOW, jsonObject.getString("Low"))
                    .withValue(QuoteHistoricData.CLOSE, jsonObject.getString("Close"))
                    .build();
        } catch (JSONException e) {
            Log.e(LOG_TAG, "buildBatchOperationHistoric: Exception while parsing JSON", e);
            return null;
        }
    }
}
