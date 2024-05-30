/*******************************************************************************
 * Copyright (c) 2023. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.tiktok.appevents.contents;

public interface TTContentsEventConstants {
    interface ContentsEventName {
        String EVENT_NAME_ADD_TO_CARD = "AddToCart";
        String EVENT_NAME_ADD_TO_WISHLIST = "AddToWishlist";
        String EVENT_NAME_CHECK_OUT = "Checkout";
        String EVENT_NAME_PURCHASE = "Purchase";
        String EVENT_NAME_VIEW_CONTENT = "ViewContent";

    }

    interface Params {
        String EVENT_PROPERTY_CONTENT_TYPE = "content_type";
        String EVENT_PROPERTY_CONTENT_ID = "content_id";
        String EVENT_PROPERTY_DESCRIPTION = "description";
        String EVENT_PROPERTY_CURRENCY = "currency";
        String EVENT_PROPERTY_VALUE = "value";
        String EVENT_PROPERTY_CONTENTS = "contents";
    }

    enum Currency {
        AED, ARS, AUD, BDT, BHD, BIF, BOB, BRL, CAD, CHF, CLP, CNY, COP, CRC, CZK, DKK, DZD, EGP,
        EUR, GBP, GTQ, HKD, HNL, HUF, IDR, ILS, INR, ISK, JPY, KES, KHR, KRW, KWD, KZT, MAD, MOP,
        MXN, MYR, NGN, NIO, NOK, NZD, OMR, PEN, PHP, PKR, PLN, PYG, QAR, RON, RUB, SAR, SEK, SGD,
        THB, TRY, TWD, UAH, USD, VES, VND, ZAR, BGN, IQD,JOD, LBP, TZS
    }
}
