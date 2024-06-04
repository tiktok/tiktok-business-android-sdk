package com.example.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

public class DeepLinkUtil {
    public static void openDeepLink(Context context, String deepLink){
        try {
            Uri uri = Uri.parse(deepLink);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(uri);
            if (!(context instanceof Activity)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            context.startActivity(intent);
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }
}
