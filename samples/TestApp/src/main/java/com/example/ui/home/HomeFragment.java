/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.home;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.*;
import com.example.R;
import com.tiktok.TikTokBusinessSdk;
import com.tiktok.appevents.TTPurchaseInfo;

import org.json.JSONObject;

import java.util.*;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getCanonicalName();

    private HomeViewModel homeViewModel;

    private BillingClient billingClient;
    private SkuDetails skuDetails = null;
    private Purchase purchase = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        homeViewModel.getText().observe(getViewLifecycleOwner(), s -> textView.setText(s));

        textView.setOnClickListener(v -> {
            if (purchase != null) {
                consumePurchase();
            } else if (skuDetails != null) {
                Activity activity = requireActivity();
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setSkuDetails(skuDetails)
                        .build();

                int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
                homeViewModel.setText("BillingResponseCode: " + responseCode);
            } else {
                queryPurchase();
            }
        });

        PurchasesUpdatedListener purchaseUpdateListener = (billingResult, purchases) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {

                /** tiktok.monitor track purchase */
                List<TTPurchaseInfo> purchaseInfos = new ArrayList<>();

                try {
                    for (Purchase purchase : purchases) {
                        purchaseInfos.add(new TTPurchaseInfo(new JSONObject(purchase.getOriginalJson()), new JSONObject(skuDetails.getOriginalJson())));
                    }
                    TikTokBusinessSdk.trackGooglePlayPurchase(purchaseInfos);
                } catch (Exception e) {
                    Toast.makeText(HomeFragment.this.getActivity(), "Failed to track purchase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                purchase = purchases.get(0);
                homeViewModel.setText("purchase success, sku: " + purchase.getSku() + ". click to consume");
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                homeViewModel.setText("USER_CANCELED");
            } else {
                homeViewModel.setText("otherErr : " + billingResult.getResponseCode());
            }
        };

        billingClient = BillingClient.newBuilder(requireContext())
                .setListener(purchaseUpdateListener)
                .enablePendingPurchases()
                .build();

        startBilling();

        return root;
    }

    private void newPurchase() {
        List<String> skuList = new ArrayList<>();
        skuList.add("android.test.purchased");
        SkuDetailsParams.Builder params = SkuDetailsParams.newBuilder();
        params.setSkusList(skuList).setType(BillingClient.SkuType.INAPP);
        billingClient.querySkuDetailsAsync(params.build(), (billingResult1, skuDetailsList) -> {
            if (billingResult1.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && skuDetailsList != null) {

                if (skuDetailsList.size() > 0) {
                    skuDetails = skuDetailsList.get(0);
                    homeViewModel.setText("launchBillingFlow: " + skuDetails.getSku());
                }
            }
        });
    }

    private void consumePurchase() {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                purchase = null;
                skuDetails = null;
                homeViewModel.setText("Consume success. click to start sku fetch");
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    private void queryPurchase() {
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (purchasesResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            if (Objects.requireNonNull(purchasesResult.getPurchasesList()).size() > 0) {
                // purchase exist
                purchase = purchasesResult.getPurchasesList().get(0);
                homeViewModel.setText("purchase exist, sku: " + purchase.getSku() + ". click to consume");
            } else {
                // new purchase
                newPurchase();
            }
        }
    }

    private void startBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    homeViewModel.setText("onBillingSetupFinished");
                    // query existing purchases
                    queryPurchase();
                } else {
                    homeViewModel.setText("Failed to set up billing");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                homeViewModel.setText("onBillingServiceDisconnected");
                new Handler(Looper.getMainLooper()).postDelayed(() -> startBilling(), 5000);
            }
        });
    }
}