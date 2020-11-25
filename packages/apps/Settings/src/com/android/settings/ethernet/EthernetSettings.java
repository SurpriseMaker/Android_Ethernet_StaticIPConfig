/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.ethernet;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.Preference.OnPreferenceChangeListener;
import android.support.v7.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.preference.CheckBoxPreference;

import java.util.ArrayList;

import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.dashboard.SummaryLoader;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.SettingsActivity;

import android.widget.Switch;
import android.app.Activity;
import android.app.ActivityManager;
import android.net.DhcpInfo;
import android.net.EthernetManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.widget.Toast;
import android.os.Looper;

public class EthernetSettings extends SettingsPreferenceFragment implements OnPreferenceChangeListener{
	private static final String TAG = "EthernetSettings";
	private EthernetEnabler mEthEnabler;
    private static final String KEY_CONF_ETH = "ethernet_config";
	private EthernetDialog mEthernetDialog = null;
	private Preference mEthConfigPref;
	private ConnectivityManager mCM;
	private DhcpInfo mDhcpinfo;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.ethernet_settings);
        final PreferenceScreen preferenceScreen = getPreferenceScreen();
        mEthConfigPref = preferenceScreen.findPreference(KEY_CONF_ETH);
    }

    @Override
    public void onStart() {
        super.onStart();
        // On/off switch is hidden for Setup Wizard (returns null)
        mEthEnabler = createEthernetEnabler();
		mCM = (ConnectivityManager)getActivity().getSystemService(
		        Context.CONNECTIVITY_SERVICE);
		mEthernetDialog = new EthernetDialog(getActivity(),
                (EthernetManager)getSystemService(Context.ETHERNET_SERVICE), mCM);
		mEthEnabler.setConfigDialog(mEthernetDialog);
    }

    @Override
    public void onResume() {
        super.onResume();
        final Activity activity = getActivity();
        if (mEthEnabler != null) {
            mEthEnabler.resume(activity);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mEthEnabler != null) {
            mEthEnabler.pause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (mEthEnabler != null) {
            mEthEnabler.teardownSwitchBar();
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.ETHERNET;
    }

    /**
     * @return new EthernetEnabler or null
     */
    /* package */ 
	EthernetEnabler createEthernetEnabler() {
        final SettingsActivity activity = (SettingsActivity) getActivity();
        SwitchBar bar = activity.getSwitchBar();
        return new EthernetEnabler(activity, new SwitchBarController(activity.getSwitchBar()),
                (EthernetManager)getSystemService(Context.ETHERNET_SERVICE));
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        return true;
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mEthConfigPref) {
			final SettingsActivity activity = (SettingsActivity) getActivity();
			if(activity.getSwitchBar().isChecked()) {
				if(mEthernetDialog != null)
		        	mEthernetDialog.show();
			} else {
				Toast.makeText(getActivity(), R.string.eth_open_ethernet_tip, Toast.LENGTH_LONG).show();
			}
        }
        return super.onPreferenceTreeClick(preference);
    }

    private static class SummaryProvider implements SummaryLoader.SummaryProvider {

        private final Context mContext;
        private final SummaryLoader mSummaryLoader;

        public SummaryProvider(Context context, SummaryLoader summaryLoader) {
            mContext = context;
            mSummaryLoader = summaryLoader;
        }

        @Override
        public void setListening(boolean listening) {
            if (listening) {
		        int enable = Settings.Global.getInt(mContext.getContentResolver(), Settings.Global.ETHERNET_ON, 0);
		        if(enable == EthernetManager.ETHERNET_STATE_ENABLED) {
                    mSummaryLoader.setSummary(this, mContext.getString(R.string.eth_state_on));
                } else {
                    mSummaryLoader.setSummary(this, mContext.getString(R.string.eth_state_off));
                }
            }
        }
    }

    public static final SummaryLoader.SummaryProviderFactory SUMMARY_PROVIDER_FACTORY
            = new SummaryLoader.SummaryProviderFactory() {
        @Override
        public SummaryLoader.SummaryProvider createSummaryProvider(Activity activity,
                                                                   SummaryLoader summaryLoader) {
            return new SummaryProvider(activity, summaryLoader);
        }
    };
}
