/*******************************************************************************
 * Copyright (c) 2020. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.testdata;

import static com.tiktok.appevents.base.EventName.ACHIEVE_LEVEL;
import static com.tiktok.appevents.base.EventName.ADD_PAYMENT_INFO;
import static com.tiktok.appevents.base.EventName.COMPLETE_TUTORIAL;
import static com.tiktok.appevents.base.EventName.CREATE_GROUP;
import static com.tiktok.appevents.base.EventName.CREATE_ROLE;
import static com.tiktok.appevents.base.EventName.GENERATE_LEAD;
import static com.tiktok.appevents.base.EventName.INSTALL_APP;
import static com.tiktok.appevents.base.EventName.IN_APP_AD_CLICK;
import static com.tiktok.appevents.base.EventName.IN_APP_AD_IMPR;
import static com.tiktok.appevents.base.EventName.JOIN_GROUP;
import static com.tiktok.appevents.base.EventName.LAUNCH_APP;
import static com.tiktok.appevents.base.EventName.LOAN_APPLICATION;
import static com.tiktok.appevents.base.EventName.LOAN_APPROVAL;
import static com.tiktok.appevents.base.EventName.LOAN_DISBURSAL;
import static com.tiktok.appevents.base.EventName.LOGIN;
import static com.tiktok.appevents.base.EventName.RATE;
import static com.tiktok.appevents.base.EventName.REGISTRATION;
import static com.tiktok.appevents.base.EventName.SEARCH;
import static com.tiktok.appevents.base.EventName.SPEND_CREDITS;
import static com.tiktok.appevents.base.EventName.START_TRIAL;
import static com.tiktok.appevents.base.EventName.SUBSCRIBE;
import static com.tiktok.appevents.base.EventName.UNLOCK_ACHIEVEMENT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_ADD_TO_CARD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_ADD_TO_WISHLIST;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_CHECK_OUT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_PURCHASE;
import static com.tiktok.appevents.contents.TTContentsEventConstants.ContentsEventName.EVENT_NAME_VIEW_CONTENT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.AED;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.ARS;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.AUD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.BDT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.BHD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.BIF;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.BOB;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.BRL;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CAD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CHF;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CLP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CNY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.COP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CRC;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.CZK;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.DKK;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.DZD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.EGP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.EUR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.GBP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.GTQ;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.HKD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.HNL;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.HUF;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.IDR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.ILS;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.INR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.ISK;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.JPY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.KES;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.KHR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.KRW;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.KWD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.KZT;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.MAD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.MOP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.MXN;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.MYR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.NGN;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.NIO;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.NOK;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.NZD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.OMR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.PEN;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.PHP;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.PKR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.PLN;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.PYG;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.QAR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.RON;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.RUB;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.SAR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.SEK;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.SGD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.THB;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.TRY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.TWD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.UAH;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.USD;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.VES;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.VND;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Currency.ZAR;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_ID;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CONTENT_TYPE;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_CURRENCY;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_DESCRIPTION;
import static com.tiktok.appevents.contents.TTContentsEventConstants.Params.EVENT_PROPERTY_VALUE;

import com.tiktok.appevents.base.EventName;
import com.tiktok.appevents.contents.TTContentsEventConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public class TestEvents {

    public static String[] getAllEvents() {
        ArrayList<String> events = new ArrayList<>();
        for(String evnt : TTEventProperties.keySet()) {
            events.add(evnt);
        }
        return events.toArray(new String[events.size()]);
    }

    public static HashMap<String, String[]> TTEventProperties = new LinkedHashMap<>();
    public static HashMap<String, EventName> TTBaseEvents = new HashMap<>();
    public static HashSet<String> TTContentsEvent = new HashSet<>();
    public static String[] TTContentParams;

    public static HashMap<String, TTContentsEventConstants.Currency> TTCurrency = new HashMap<>();

    static {
        TTEventProperties.put(ACHIEVE_LEVEL.toString(), new String[]{});
        TTEventProperties.put(ADD_PAYMENT_INFO.toString(), new String[]{});
        TTEventProperties.put(EVENT_NAME_ADD_TO_CARD, new String[]{
                EVENT_PROPERTY_CONTENT_TYPE,EVENT_PROPERTY_CONTENT_ID,EVENT_PROPERTY_DESCRIPTION,
                EVENT_PROPERTY_CURRENCY,EVENT_PROPERTY_VALUE
        });
        TTEventProperties.put(EVENT_NAME_ADD_TO_WISHLIST, new String[]{
                EVENT_PROPERTY_CONTENT_TYPE,EVENT_PROPERTY_CONTENT_ID,EVENT_PROPERTY_DESCRIPTION,
                EVENT_PROPERTY_CURRENCY,EVENT_PROPERTY_VALUE
        });
        TTEventProperties.put(EVENT_NAME_CHECK_OUT, new String[]{
                EVENT_PROPERTY_CONTENT_TYPE,EVENT_PROPERTY_CONTENT_ID,EVENT_PROPERTY_DESCRIPTION,
                EVENT_PROPERTY_CURRENCY,EVENT_PROPERTY_VALUE
        });
        TTEventProperties.put(COMPLETE_TUTORIAL.toString(), new String[]{});
        TTEventProperties.put(CREATE_GROUP.toString(), new String[]{});
        TTEventProperties.put(CREATE_ROLE.toString(), new String[]{});
        TTEventProperties.put(GENERATE_LEAD.toString(), new String[]{});
        TTEventProperties.put(IN_APP_AD_CLICK.toString(), new String[]{});
        TTEventProperties.put(IN_APP_AD_IMPR.toString(), new String[]{});
        TTEventProperties.put(INSTALL_APP.toString(), new String[]{});
        TTEventProperties.put(JOIN_GROUP.toString(), new String[]{});
        TTEventProperties.put(LAUNCH_APP.toString(), new String[]{});
        TTEventProperties.put(LOAN_APPLICATION.toString(), new String[]{});
        TTEventProperties.put(LOAN_APPROVAL.toString(), new String[]{});
        TTEventProperties.put(LOAN_DISBURSAL.toString(), new String[]{});
        TTEventProperties.put(LOGIN.toString(), new String[]{});
        TTEventProperties.put(EVENT_NAME_PURCHASE, new String[]{
                EVENT_PROPERTY_CONTENT_TYPE,EVENT_PROPERTY_CONTENT_ID,EVENT_PROPERTY_DESCRIPTION,
                EVENT_PROPERTY_CURRENCY,EVENT_PROPERTY_VALUE
        });
        TTEventProperties.put(RATE.toString(), new String[]{});
        TTEventProperties.put(REGISTRATION.toString(), new String[]{});
        TTEventProperties.put(SEARCH.toString(), new String[]{});
        TTEventProperties.put(SPEND_CREDITS.toString(), new String[]{});
        TTEventProperties.put(START_TRIAL.toString(), new String[]{});
        TTEventProperties.put(SUBSCRIBE.toString(), new String[]{});
        TTEventProperties.put(UNLOCK_ACHIEVEMENT.toString(), new String[]{});
        TTEventProperties.put(EVENT_NAME_VIEW_CONTENT, new String[]{
                EVENT_PROPERTY_CONTENT_TYPE,EVENT_PROPERTY_CONTENT_ID,EVENT_PROPERTY_DESCRIPTION,
                EVENT_PROPERTY_CURRENCY,EVENT_PROPERTY_VALUE
        });
        TTEventProperties.put("Test", new String[]{});

        TTBaseEvents.put(ACHIEVE_LEVEL.toString(), ACHIEVE_LEVEL);
        TTBaseEvents.put(ADD_PAYMENT_INFO.toString(), ADD_PAYMENT_INFO);
        TTBaseEvents.put(COMPLETE_TUTORIAL.toString(), COMPLETE_TUTORIAL);
        TTBaseEvents.put(CREATE_GROUP.toString(), CREATE_GROUP);
        TTBaseEvents.put(CREATE_ROLE.toString(), CREATE_ROLE);
        TTBaseEvents.put(GENERATE_LEAD.toString(), GENERATE_LEAD);
        TTBaseEvents.put(IN_APP_AD_CLICK.toString(), IN_APP_AD_CLICK);
        TTBaseEvents.put(IN_APP_AD_IMPR.toString(), IN_APP_AD_IMPR);
        TTBaseEvents.put(INSTALL_APP.toString(), INSTALL_APP);
        TTBaseEvents.put(JOIN_GROUP.toString(), JOIN_GROUP);
        TTBaseEvents.put(LAUNCH_APP.toString(), LAUNCH_APP);
        TTBaseEvents.put(LOAN_APPLICATION.toString(), LOAN_APPLICATION);
        TTBaseEvents.put(LOAN_APPROVAL.toString(), LOAN_APPROVAL);
        TTBaseEvents.put(LOAN_DISBURSAL.toString(), LOAN_DISBURSAL);
        TTBaseEvents.put(LOGIN.toString(), LOGIN);
        TTBaseEvents.put(RATE.toString(), RATE);
        TTBaseEvents.put(REGISTRATION.toString(), REGISTRATION);
        TTBaseEvents.put(SEARCH.toString(), SEARCH);
        TTBaseEvents.put(SPEND_CREDITS.toString(), SPEND_CREDITS);
        TTBaseEvents.put(START_TRIAL.toString(), START_TRIAL);
        TTBaseEvents.put(SUBSCRIBE.toString(), SUBSCRIBE);
        TTBaseEvents.put(UNLOCK_ACHIEVEMENT.toString(), UNLOCK_ACHIEVEMENT);

        TTContentsEvent.add(EVENT_NAME_ADD_TO_CARD);
        TTContentsEvent.add(EVENT_NAME_ADD_TO_WISHLIST);
        TTContentsEvent.add(EVENT_NAME_CHECK_OUT);
        TTContentsEvent.add(EVENT_NAME_PURCHASE);
        TTContentsEvent.add(EVENT_NAME_VIEW_CONTENT);
        TTContentParams = new String[]{"price", "quantity", "content_id", "content_category",
                "content_name", "brand"};

        TTCurrency.put(AED.toString(), AED);
        TTCurrency.put(ARS.toString(),ARS);
        TTCurrency.put(AUD.toString(),AUD);
        TTCurrency.put(BDT.toString(),BDT);
        TTCurrency.put(BHD.toString(),BHD);
        TTCurrency.put(BIF.toString(),BIF);
        TTCurrency.put(BOB.toString(),BOB);
        TTCurrency.put(BRL.toString(),BRL);
        TTCurrency.put(CAD.toString(),CAD);
        TTCurrency.put(CHF.toString(),CHF);
        TTCurrency.put(CLP.toString(),CLP);
        TTCurrency.put(CNY.toString(),CNY);
        TTCurrency.put(EGP.toString(),EGP);
        TTCurrency.put(IDR.toString(),IDR);
        TTCurrency.put(ILS.toString(),ILS);
        TTCurrency.put(OMR.toString(),OMR);
        TTCurrency.put(VND.toString(),VND);
        TTCurrency.put(INR.toString(),INR);
        TTCurrency.put(PEN.toString(),PEN);
        TTCurrency.put(ZAR.toString(),ZAR);
        TTCurrency.put(ISK.toString(),ISK);
        TTCurrency.put(JPY.toString(),JPY);
        TTCurrency.put(KES.toString(),KES);
        TTCurrency.put(KHR.toString(),KHR);
        TTCurrency.put(KRW.toString(),KRW);
        TTCurrency.put(KWD.toString(),KWD);
        TTCurrency.put(KZT.toString(),KZT);
        TTCurrency.put(MAD.toString(),MAD);
        TTCurrency.put(MOP.toString(),MOP);
        TTCurrency.put(EUR.toString(),EUR);
        TTCurrency.put(PHP.toString(),PHP);
        TTCurrency.put(PKR.toString(),PKR);
        TTCurrency.put(PLN.toString(),PLN);
        TTCurrency.put(QAR.toString(),QAR);
        TTCurrency.put(PYG.toString(),PYG);
        TTCurrency.put(RON.toString(),RON);
        TTCurrency.put(MXN.toString(),MXN);
        TTCurrency.put(RUB.toString(),RUB);
        TTCurrency.put(SAR.toString(),SAR);
        TTCurrency.put(SEK.toString(),SEK);
        TTCurrency.put(SGD.toString(),SGD);
        TTCurrency.put(THB.toString(),THB);
        TTCurrency.put(COP.toString(),COP);
        TTCurrency.put(GBP.toString(),GBP);
        TTCurrency.put(MYR.toString(),MYR);
        TTCurrency.put(TRY.toString(),TRY);
        TTCurrency.put(CRC.toString(),CRC);
        TTCurrency.put(GTQ.toString(),GTQ);
        TTCurrency.put(NGN.toString(),NGN);
        TTCurrency.put(TWD.toString(),TWD);
        TTCurrency.put(CZK.toString(),CZK);
        TTCurrency.put(HKD.toString(),HKD);
        TTCurrency.put(NIO.toString(),NIO);
        TTCurrency.put(UAH.toString(),UAH);
        TTCurrency.put(DKK.toString(),DKK);
        TTCurrency.put(HNL.toString(),HNL);
        TTCurrency.put(NOK.toString(),NOK);
        TTCurrency.put(USD.toString(),USD);
        TTCurrency.put(DZD.toString(),DZD);
        TTCurrency.put(HUF.toString(),HUF);
        TTCurrency.put(NZD.toString(),NZD);
        TTCurrency.put(VES.toString(),VES);
    }
}

