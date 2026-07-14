package com.tiktok.iap.billing.client;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.tiktok.util.JSON;

import org.json.JSONObject;

import java.util.List;

final class V8ProductDetailCompat {
    private V8ProductDetailCompat() {
    }

    @Nullable
    static JSONObject toCompatDetailJson(@Nullable ProductDetails detail, boolean isSubs) {
        if (detail == null) {
            return null;
        }

        try {
            JSONObject json = JSON.build();
            JSON.putObject(json, "productId", detail.getProductId());
            JSON.putObject(json, "type", detail.getProductType());
            JSON.putObject(json, "title", detail.getTitle());
            JSON.putObject(json, "description", detail.getDescription());

            if (isSubs || BillingClient.ProductType.SUBS.equals(detail.getProductType())) {
                fillSubs(json, detail);
            } else {
                fillInApp(json, detail);
            }

            fillDefaults(json);
            return json;
        } catch (Throwable ignore) {
        }

        try {
            String rawJson = BillUtils.parserJsonFromProductDetail(String.valueOf(detail));
            JSONObject json = JSON.build(rawJson);
            if (json != null && json.length() > 0) {
                fillDefaults(json);
                return json;
            }
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static void fillInApp(JSONObject json, ProductDetails detail) {
        try {
            ProductDetails.OneTimePurchaseOfferDetails oneTime = detail.getOneTimePurchaseOfferDetails();
            if (oneTime == null) {
                return;
            }
            JSON.putObject(json, "price", oneTime.getFormattedPrice());
            JSON.putLong(json, "price_amount_micros", oneTime.getPriceAmountMicros());
            JSON.putObject(json, "price_currency_code", oneTime.getPriceCurrencyCode());
        } catch (Throwable ignore) {
        }
    }

    private static void fillSubs(JSONObject json, ProductDetails detail) {
        try {
            List<ProductDetails.SubscriptionOfferDetails> offers = detail.getSubscriptionOfferDetails();
            if (offers == null || offers.isEmpty()) {
                return;
            }
            ProductDetails.SubscriptionOfferDetails offer = findBestOffer(offers);
            if (offer == null) {
                return;
            }
            String offerId = offer.getOfferId();
            JSON.putObject(json, "offer_id", TextUtils.isEmpty(offerId) ? offer.getBasePlanId() : offerId);
            JSON.putObject(json, "offer_type", TextUtils.isEmpty(offerId) ? "base_plan" : "offer");

            ProductDetails.PricingPhases phases = offer.getPricingPhases();
            if (phases == null) {
                return;
            }
            List<ProductDetails.PricingPhase> phaseList = phases.getPricingPhaseList();
            if (phaseList == null || phaseList.isEmpty()) {
                return;
            }

            ProductDetails.PricingPhase paidPhase = null;
            ProductDetails.PricingPhase trialPhase = null;
            for (ProductDetails.PricingPhase phase : phaseList) {
                if (phase == null) {
                    continue;
                }
                if (phase.getPriceAmountMicros() > 0L && paidPhase == null) {
                    paidPhase = phase;
                }
                if (phase.getPriceAmountMicros() == 0L && trialPhase == null) {
                    trialPhase = phase;
                }
            }

            ProductDetails.PricingPhase targetPhase = paidPhase != null ? paidPhase : phaseList.get(0);
            if (targetPhase == null) {
                return;
            }
            JSON.putObject(json, "price", targetPhase.getFormattedPrice());
            JSON.putLong(json, "price_amount_micros", targetPhase.getPriceAmountMicros());
            JSON.putObject(json, "price_currency_code", targetPhase.getPriceCurrencyCode());
            String billingPeriod = targetPhase.getBillingPeriod();
            JSON.putObject(json, "subscriptionPeriod", billingPeriod);
            JSON.putInt(json, "subscriptionPeriodNumber", parseBillingPeriodCount(billingPeriod));
            if (trialPhase != null) {
                JSON.putObject(json, "freeTrialPeriod", trialPhase.getBillingPeriod());
            }
        } catch (Throwable ignore) {
        }
    }

    private static int parseBillingPeriodCount(String billingPeriod) {
        if (TextUtils.isEmpty(billingPeriod)) {
            return 0;
        }
        int value = 0;
        try {
            for (int i = 0; i < billingPeriod.length(); i++) {
                try {
                    char ch = billingPeriod.charAt(i);
                    if (Character.isDigit(ch)) {
                        value = (value * 10) + Character.digit(ch, 10);
                    } else if (value > 0) {
                        return value;
                    }
                } catch (Throwable ignore) {
                }
            }
        } catch (Throwable ignore) {
        }
        return value;
    }

    private static ProductDetails.SubscriptionOfferDetails findBestOffer(List<ProductDetails.SubscriptionOfferDetails> offers) {
        if (offers == null || offers.isEmpty()) {
            return null;
        }
        ProductDetails.SubscriptionOfferDetails fallbackOffer = null;
        try {
            for (ProductDetails.SubscriptionOfferDetails offer : offers) {
                if (offer == null) {
                    continue;
                }
                if (fallbackOffer == null) {
                    fallbackOffer = offer;
                }
                ProductDetails.PricingPhases phases = offer.getPricingPhases();
                List<ProductDetails.PricingPhase> phaseList = phases == null ? null : phases.getPricingPhaseList();
                if (phaseList == null || phaseList.isEmpty()) {
                    continue;
                }
                for (ProductDetails.PricingPhase phase : phaseList) {
                    if (phase != null && phase.getPriceAmountMicros() > 0L) {
                        return offer;
                    }
                }
            }
        } catch (Throwable ignore) {
        }
        return fallbackOffer;
    }

    private static void fillDefaults(JSONObject json) {
        try {
            if (json.isNull("price")) {
                JSON.putObject(json, "price", "");
            }
            if (json.isNull("price_amount_micros")) {
                JSON.putLong(json, "price_amount_micros", 0L);
            }
            if (json.isNull("price_currency_code")) {
                JSON.putObject(json, "price_currency_code", "");
            }
            if (json.isNull("subscriptionPeriod")) {
                JSON.putObject(json, "subscriptionPeriod", "");
            }
            if (json.isNull("subscriptionPeriodNumber")) {
                JSON.putInt(json, "subscriptionPeriodNumber", 0);
            }
            if (json.isNull("freeTrialPeriod")) {
                JSON.putObject(json, "freeTrialPeriod", "");
            }
            if (json.isNull("offer_id")) {
                JSON.putObject(json, "offer_id", "");
            }
            if (json.isNull("offer_type")) {
                JSON.putObject(json, "offer_type", "");
            }
        } catch (Throwable ignore) {
        }
    }
}
