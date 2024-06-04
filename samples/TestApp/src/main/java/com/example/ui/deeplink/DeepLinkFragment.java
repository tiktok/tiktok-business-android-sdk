/*******************************************************************************
 * Copyright (c) 2024. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.deeplink;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.R;

public class DeepLinkFragment extends Fragment {

    private DeepLinkViewModel deepLinkViewModel;
    private EditText deeplink;
    private Button openDeeplink;
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        deepLinkViewModel = new ViewModelProvider(this).get(DeepLinkViewModel.class);
        View root = inflater.inflate(R.layout.fragment_deeplink, container, false);
        deeplink = root.findViewById(R.id.deepLink);
        openDeeplink = root.findViewById(R.id.open_deep_link);
        openDeeplink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (deeplink.getText() != null && !TextUtils.isEmpty(deeplink.getText().toString())) {
                    deepLinkViewModel.openDeepLink(deeplink.getText().toString());
                }
            }
        });
        return root;
    }

}