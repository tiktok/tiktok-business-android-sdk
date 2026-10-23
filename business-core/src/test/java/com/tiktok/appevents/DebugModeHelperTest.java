package com.tiktok.appevents;

import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class DebugModeHelperTest {
    @Test
    public void usesNestedBusinessSdkConfigForDebugMode() throws Exception {
        JSONObject businessSdkConfig = new JSONObject()
                .put("enable_debug_mode", true)
                .put("enable_sdk", true);
        JSONObject response = new JSONObject()
                .put("enable_debug_mode", false)
                .put("business_sdk_config", businessSdkConfig);

        JSONObject config = DebugModeHelper.getConfigForDebugMode(response);

        assertTrue(config.optBoolean("enable_debug_mode"));
    }

    @Test
    public void keepsTopLevelDebugModeConfigAsFallback() throws Exception {
        JSONObject response = new JSONObject()
                .put("enable_debug_mode", true)
                .put("business_sdk_config", new JSONObject().put("enable_sdk", true));

        assertSame(response, DebugModeHelper.getConfigForDebugMode(response));
    }
}
