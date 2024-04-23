/*******************************************************************************
 * Copyright (c) 2021. Tiktok Inc.
 *
 * This source code is licensed under the MIT license found in the LICENSE file in the root directory of this source tree.
 ******************************************************************************/

package com.example.ui.identify;

import android.widget.EditText;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.R;
import com.example.ui.home.HomeViewModel;
import com.tiktok.TikTokBusinessSdk;

public class IdentifyFragment extends Fragment {

    private HomeViewModel mViewModel;

    public static IdentifyFragment newInstance() {
        return new IdentifyFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        mViewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        View root = inflater.inflate(R.layout.identify_fragment, container, false);

        EditText externalID = root.findViewById(R.id.externalID);
        EditText externalUsername = root.findViewById(R.id.externalUsername);
        EditText externalPhone = root.findViewById(R.id.externalPhone);
        EditText externalEmail = root.findViewById(R.id.externalEmail);
        if (mViewModel.checkAMCacheExist()) {
            externalID.setText(mViewModel.getExternalID());
            externalUsername.setText(mViewModel.getExternalUsername());
            externalPhone.setText(mViewModel.getExternalPhoneNumber());
            externalEmail.setText(mViewModel.getExternalEmail());
        }

        root.findViewById(R.id.resetBtn).setOnClickListener(view -> {
            TikTokBusinessSdk.logout();
            mViewModel.resetCache();
        });

        root.findViewById(R.id.identifyBtn).setOnClickListener(view -> {
            String externalIDStr = externalID.getText().toString();
            String externalUsernameStr = externalUsername.getText().toString();
            String externalPhoneStr = externalPhone.getText().toString();
            String externalEmailStr = externalEmail.getText().toString();
            if (externalUsernameStr.equals("")) externalUsernameStr = null;
            if (externalPhoneStr.equals("")) externalPhoneStr = null;
            if (externalEmailStr.equals("")) externalEmailStr = null;
            TikTokBusinessSdk.identify(externalIDStr, externalUsernameStr, externalPhoneStr, externalEmailStr);
            mViewModel.setNewCache(externalIDStr, externalUsernameStr, externalPhoneStr, externalEmailStr);
        });

        root.findViewById(R.id.logoutBtn).setOnClickListener(view -> TikTokBusinessSdk.logout());
        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

}