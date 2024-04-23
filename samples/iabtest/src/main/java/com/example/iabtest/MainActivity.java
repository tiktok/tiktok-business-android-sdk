/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.iabtest;

import android.app.PendingIntent;
import android.content.*;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.android.vending.billing.IInAppBillingService;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private Context context;
    private String tag;

    private IInAppBillingService mService;
    private final ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
        }
    };

    // ProductID
    private String productID = null;

    // getSkuDetails() DETAILS_LIST
    private ArrayList<String> skuDetailsList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // TikTokBusinessSdk init start
        if (!TikTokBusinessSdk.isInitialized()) {
            // !!!!!!!!!!!!!!!!!!!!!!!!!
            // in order for this app to be runnable, plz create a resource file containing the relevant string resources
            String appId = this.getResources().getString(R.string.tiktok_business_app_id);
            String accessToken = this.getResources().getString(R.string.tiktok_business_app_access_token);
            TikTokBusinessSdk.TTConfig ttConfig = new TikTokBusinessSdk.TTConfig(getApplicationContext())
                    .setAppId(appId)
                    .setAccessToken(accessToken)
                    .disableInstallLogging()
                    .disableLaunchLogging()
                    .disableRetentionLogging()
                    .setLogLevel(TikTokBusinessSdk.LogLevel.INFO);
            TikTokBusinessSdk.initializeSdk(ttConfig);
        }
        // TikTokBusinessSdk init end

        // Context
        context = getApplicationContext();

        // log tag
        tag = "com.example.iabtest";

        // Bind Service
        final boolean blnBind = bindService(new Intent(
                        "com.android.vending.billing.InAppBillingService.BIND").setPackage("com.android.vending"),
                mServiceConn, Context.BIND_AUTO_CREATE);

        Toast.makeText(context, "bindService - return " + blnBind, Toast.LENGTH_SHORT).show();
        Log.i(tag, "bindService - return " + blnBind);

        // Assign View
        // View
        Button btnTest = findViewById(R.id.btnTest);
        Button btnCheck = findViewById(R.id.btnCheck);
        Button btnBuy = findViewById(R.id.btnBuy);
        Button btnConsume = findViewById(R.id.btnConsume);

        String[] skus = {
                "android.test.purchased",
                "android.test.canceled",
                "android.test.refunded",
                "android.test.item_unavailable"
        };
        Spinner spin = findViewById(R.id.spinner);
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, skus);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spin.setAdapter(aa);

        spin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                productID = skus[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                productID = null;
            }
        });

        btnTest.setOnClickListener(arg0 -> {
            if (!blnBind) return;
            if (mService == null) return;

            int result;
            try {
                result = mService.isBillingSupported(3, getPackageName(), "inapp");

                Toast.makeText(context, "isBillingSupported() - success : return " + result, Toast.LENGTH_SHORT).show();
                Log.i(tag, "isBillingSupported() - success : return " + result);
            } catch (RemoteException e) {
                e.printStackTrace();

                Toast.makeText(context, "isBillingSupported() - fail!", Toast.LENGTH_SHORT).show();
                Log.w(tag, "isBillingSupported() - fail!");
            }
        });

        btnCheck.setOnClickListener(arg0 -> {
            if (!blnBind) return;
            if (mService == null) return;

            Bundle ownedItems;
            try {
                ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);

//                Toast.makeText(context, "getPurchases() - success return Bundle", Toast.LENGTH_SHORT).show();
                Log.i(tag, "getPurchases() - success return Bundle");
            } catch (RemoteException e) {
                e.printStackTrace();

                Toast.makeText(context, "getPurchases - fail!", Toast.LENGTH_SHORT).show();
                Log.w(tag, "getPurchases() - fail!");
                return;
            }

            int response = ownedItems.getInt("RESPONSE_CODE");
//            Toast.makeText(context, "getPurchases() - \"RESPONSE_CODE\" return " + response, Toast.LENGTH_SHORT).show();
            Log.i(tag, "getPurchases() - \"RESPONSE_CODE\" return " + response);

            if (response != 0) return;

            ArrayList<String> ownedSkus = ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String> purchaseDataList = ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String> signatureList = ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
            String continuationToken = ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            Log.i(tag, "getPurchases() - \"INAPP_PURCHASE_ITEM_LIST\" return " + ownedSkus.toString());
            Log.i(tag, "getPurchases() - \"INAPP_PURCHASE_DATA_LIST\" return " + purchaseDataList.toString());
            Log.i(tag, "getPurchases() - \"INAPP_DATA_SIGNATURE\" return " + (signatureList != null ? signatureList.toString() : "null"));
            Log.i(tag, "getPurchases() - \"INAPP_CONTINUATION_TOKEN\" return " + (continuationToken != null ? continuationToken : "null"));
        });

        btnBuy.setOnClickListener(arg0 -> {
            if (!blnBind) return;
            if (mService == null) return;

            ArrayList<String> skuList = new ArrayList<>();
            skuList.add(productID);
            Bundle querySkus = new Bundle();
            querySkus.putStringArrayList("ITEM_ID_LIST", skuList);

            Bundle skuDetails;
            try {
                skuDetails = mService.getSkuDetails(3, getPackageName(), "inapp", querySkus);

//                Toast.makeText(context, "getSkuDetails() - success return Bundle", Toast.LENGTH_SHORT).show();
                Log.i(tag, "getSkuDetails() - success return Bundle");
            } catch (RemoteException e) {
                e.printStackTrace();

                Toast.makeText(context, "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
                Log.w(tag, "getSkuDetails() - fail!");
                return;
            }

            int response = skuDetails.getInt("RESPONSE_CODE");
//            Toast.makeText(context, "getSkuDetails() - \"RESPONSE_CODE\" return " + response, Toast.LENGTH_SHORT).show();
            Log.i(tag, "getSkuDetails() - \"RESPONSE_CODE\" return " + response);

            if (response != 0) return;

            skuDetailsList = skuDetails.getStringArrayList("DETAILS_LIST");
            Log.i(tag, "getSkuDetails() - \"DETAILS_LIST\" return " + skuDetailsList.toString());

            if (skuDetails.size() == 0) return;

            for (String thisResponse : skuDetailsList) {
                try {
                    JSONObject object = new JSONObject(thisResponse);

                    String sku = object.getString("productId");
                    String title = object.getString("title");
                    String price = object.getString("price");

                    Log.i(tag, "getSkuDetails() - \"DETAILS_LIST\":\"productId\" return " + sku);
                    Log.i(tag, "getSkuDetails() - \"DETAILS_LIST\":\"title\" return " + title);
                    Log.i(tag, "getSkuDetails() - \"DETAILS_LIST\":\"price\" return " + price);

                    if (!sku.equals(productID)) continue;

                    Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), sku, "inapp", "bGoa+V7g/yqDXvKRqq+JTFn4uQZbPiQJo4pf9RzJ");

//                    Toast.makeText(context, "getBuyIntent() - success return Bundle", Toast.LENGTH_SHORT).show();
                    Log.i(tag, "getBuyIntent() - success return Bundle");

                    response = buyIntentBundle.getInt("RESPONSE_CODE");
//                    Toast.makeText(context, "getBuyIntent() - \"RESPONSE_CODE\" return " + response, Toast.LENGTH_SHORT).show();
                    Log.i(tag, "getBuyIntent() - \"RESPONSE_CODE\" return " + response);

                    if (response != 0) continue;

                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (RemoteException e) {
                    e.printStackTrace();

                    Toast.makeText(context, "getSkuDetails() - fail!", Toast.LENGTH_SHORT).show();
                    Log.w(tag, "getBuyIntent() - fail!");
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
            }
        });

        btnConsume.setOnClickListener(arg0 -> {
            if (!blnBind) return;
            if (mService == null) return;

            int response;
            try {
                response = mService.consumePurchase(3, getPackageName(), String.format("inapp:com.example.iabtest:%s", productID));

//                Toast.makeText(context, "consumePurchase() - success : return " + response, Toast.LENGTH_SHORT).show();
                Log.i(tag, "consumePurchase() - success : return " + response);
            } catch (RemoteException e) {
                e.printStackTrace();

                Toast.makeText(context, "consumePurchase() - fail!", Toast.LENGTH_SHORT).show();
                Log.w(tag, "consumePurchase() - fail!");
                return;
            }

            if (response != 0) return;

        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001) {
            if (resultCode != RESULT_OK) return;

            int responseCode = data.getIntExtra("RESPONSE_CODE", 1);
//            Toast.makeText(context, "onActivityResult() - \"RESPONSE_CODE\" return " + responseCode, Toast.LENGTH_SHORT).show();
            Log.i(tag, "onActivityResult() - \"RESPONSE_CODE\" return " + responseCode);

            if (responseCode == 1) {
                Toast.makeText(this, "Purchase is cancelled", Toast.LENGTH_SHORT).show();
            }
            if (responseCode != 0) return;

            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            Log.i(tag, "onActivityResult() - \"INAPP_PURCHASE_DATA\" return " + purchaseData);
            Log.i(tag, "onActivityResult() - \"INAPP_DATA_SIGNATURE\" return " + dataSignature);

            // Tiktok purchase track
            try {
                TikTokBusinessSdk.trackGooglePlayPurchase(new TTPurchaseInfo(new JSONObject(purchaseData), new JSONObject(skuDetailsList.get(0))));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Unbind Service
        if (mService != null)
            unbindService(mServiceConn);

        super.onDestroy();
    }
}
