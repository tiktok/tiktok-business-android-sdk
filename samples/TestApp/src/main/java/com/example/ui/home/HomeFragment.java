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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.android.billingclient.api.*;
import com.example.R;

import java.util.*;

public class HomeFragment extends Fragment {
    private static final String TAG = HomeFragment.class.getCanonicalName();

    private HomeViewModel homeViewModel;

    private BillingClient billingClient;
    private ProductDetails skuDetails = null;
    private Purchase purchase = null;
    private ProductDetails subsSkuDetails = null;
    private Purchase subscribe = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        homeViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        final TextView textView = root.findViewById(R.id.text_home);
        final TextView subsTextView = root.findViewById(R.id.text_subs);
        final TextView textLog = root.findViewById(R.id.text_log);
        homeViewModel.getText().observe(getViewLifecycleOwner(), s -> textView.setText(s));
        homeViewModel.getSubscribeText().observe(getViewLifecycleOwner(), s -> subsTextView.setText(s));
        homeViewModel.getLogText().observe(getViewLifecycleOwner(), s -> textLog.setText(s));

        textView.setOnClickListener(v -> {
            if (purchase != null) {
                consumePurchase(true);
            } else if (skuDetails != null) {
                Activity activity = requireActivity();
                BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(skuDetails)
                        .build();
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Arrays.asList(pdp))
                        .build();

                int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
                homeViewModel.setLogText("BillingResponseCode: " + responseCode);
            } else {
                queryPurchase(true);
            }
        });

        subsTextView.setOnClickListener(v -> {
            if (subscribe != null) {
                consumePurchase(false);
            } else if (subsSkuDetails != null) {
                Activity activity = requireActivity();
                BillingFlowParams.ProductDetailsParams pdp = BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(subsSkuDetails)
                        .build();
                BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                        .setProductDetailsParamsList(Arrays.asList(pdp))
                        .build();


                int responseCode = billingClient.launchBillingFlow(activity, billingFlowParams).getResponseCode();
                homeViewModel.setLogText("BillingResponseCode: " + responseCode);
            } else {
                queryPurchase(false);
            }
        });

        PurchasesUpdatedListener purchaseUpdateListener = (billingResult, purchases) -> {

            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                    && purchases != null) {
            } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
                homeViewModel.setLogText("USER_CANCELED");
            } else {
                homeViewModel.setLogText("otherErr : " + billingResult.getResponseCode());
            }
        };

        billingClient = BillingClient.newBuilder(requireContext())
                .setListener(purchaseUpdateListener)
                .enableAutoServiceReconnection()
                .enablePendingPurchases(PendingPurchasesParams.newBuilder()
                        .enableOneTimeProducts()
                        .enablePrepaidPlans()
                        .build())
                .build();

        startBilling();

        return root;
    }

    private void newPurchase(boolean isInApp) {
        QueryProductDetailsParams.Product product;
        if (isInApp) {
            product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("iab_test_abc")
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build();
        } else {
            product = QueryProductDetailsParams.Product.newBuilder()
                    .setProductId("iab_test_abc_sub")
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build();
        }

        List<QueryProductDetailsParams.Product> list = new ArrayList<>();
        list.add(product);
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(list)
                .build();

        billingClient.queryProductDetailsAsync(params, new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull QueryProductDetailsResult queryProductDetailsResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    List<ProductDetails> pd = queryProductDetailsResult.getProductDetailsList();
                    if (!pd.isEmpty()) {
                        if (isInApp) {
                            skuDetails = pd.get(0);
                        } else {
                            subsSkuDetails = pd.get(0);
                        }

                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                homeViewModel.setLogText("launchBillingFlow: " + pd.get(0));
                            }
                        });
                    }
                }
            }
        });
    }

    private void consumePurchase(boolean isInApp) {
        ConsumeParams consumeParams = ConsumeParams.newBuilder()
                .setPurchaseToken(isInApp ? purchase.getPurchaseToken() : subscribe.getPurchaseToken())
                .build();
        ConsumeResponseListener listener = (billingResult, purchaseToken) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                if (isInApp) {
                    purchase = null;
                    skuDetails = null;
                } else {
                    subscribe = null;
                    subsSkuDetails = null;
                }
                homeViewModel.setLogText("Consume success. click to start sku fetch");
            }
        };

        billingClient.consumeAsync(consumeParams, listener);
    }

    private void queryPurchase(boolean isInApp) {
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(isInApp ? BillingClient.ProductType.INAPP : BillingClient.ProductType.SUBS)
                .build();

        billingClient.queryPurchasesAsync(params, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (!list.isEmpty()) {
                        // purchase exist
                        if (isInApp) {
                            purchase = list.get(0);
                        } else {
                            subscribe = list.get(0);
                        }
                        homeViewModel.setLogText("purchase exist, sku: " + list.get(0).getProducts().get(0) + ". click to consume");
                    } else {
                        // new purchase
                        newPurchase(isInApp);
                    }
                }
            }
        });

    }

    private void startBilling() {
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    homeViewModel.setLogText("onBillingSetupFinished");
                    // query existing purchases
                    queryPurchase(true);
                } else {
                    homeViewModel.setLogText("Failed to set up billing");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                homeViewModel.setLogText("onBillingServiceDisconnected");
                new Handler(Looper.getMainLooper()).postDelayed(() -> startBilling(), 5000);
            }
        });
    }
}