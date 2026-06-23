package com.tiktok.iap.billing.client;

import com.tiktok.iap.billing.GPBillVersions;

public class TTBillingFactory {

    public static IBillingProxy createBillingProxy() {
        GPBillVersions.GPBillingVer ver = GPBillVersions.getMajorVersion();
        if (ver == GPBillVersions.GPBillingVer.V8 || ver == GPBillVersions.GPBillingVer.V9) {
            return new V8BillingProxy(ver);
        }
        if (ver == GPBillVersions.GPBillingVer.V5_V7) {
            return new V5_V7BillingProxy();
        }

        return new EmptyBillingProxy();
    }

}

