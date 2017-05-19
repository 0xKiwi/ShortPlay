package com.amfasllc.shortplay;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v14.preference.PreferenceFragment;
import android.support.v7.preference.Preference;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.amfasllc.shortplay.helpers.PrefHelper;
import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.polaric.colorful.ColorPickerDialog;
import org.polaric.colorful.Colorful;
import org.polaric.colorful.ColorfulActivity;

public class SettingsActivity extends ColorfulActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_custom);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        getFragmentManager().beginTransaction().replace(R.id.content_frame, new MyPreferenceFragment()).commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public static class MyPreferenceFragment extends PreferenceFragment implements BillingProcessor.IBillingHandler {
        BillingProcessor bp;

        @Override
        public void onCreate(final Bundle savedInstanceState) {
            Colorful.applyTheme(getActivity());
            super.onCreate(savedInstanceState);
            boolean purchased = PrefHelper.getIfAdsRemoved(getActivity());

            addPreferencesFromResource(purchased ? R.xml.paid_settings : R.xml.fragment_settings);

            bp = new BillingProcessor(getActivity(), getResources().getString(R.string.licensekey), this);
            bp.loadOwnedPurchasesFromGoogle();

            if (!purchased)
                findPreference("removeads").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        boolean isAvailable = BillingProcessor.isIabServiceAvailable(getActivity());
                        if (isAvailable) {
                            bp.purchase(getActivity(), getResources().getString(R.string.adproductid));
                        } else
                            Toast.makeText(getActivity(), "There was a problem accessing the Play Store", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

            findPreference("contact").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                            "mailto", "ivanthegreatdev@gmail.com", null));
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "ShortPlay feedback");
                    emailIntent.putExtra(Intent.EXTRA_TEXT, "");
                    startActivity(Intent.createChooser(emailIntent, "Send email with"));
                    return true;
                }
            });

            findPreference("share").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    sendIntent.putExtra(Intent.EXTRA_TEXT, "Check out ShortPlay on the Play Store! https://play.google.com/store/apps/details?id=com.amfasllc.shortplay");
                    sendIntent.setType("text/plain");
                    startActivity(Intent.createChooser(sendIntent, "Share to:"));
                    return true;
                }
            });

            findPreference("security").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    startActivity(new Intent(getActivity(), SecuritySettingsActivity.class));
                    return true;
                }
            });

            findPreference("pref_theme").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Colorful.config(getActivity()).dark(newValue.toString().equals("dark")).apply();
                    getActivity().recreate();
                    return true;
                }
            });

            findPreference("primary").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    getActivity().recreate();
                    return true;
                }
            });

            findPreference("accent").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    getActivity().recreate();
                    return true;
                }
            });
        }

        @Override
        public void onProductPurchased(String productId, TransactionDetails details) {
            if (productId.equals(getString(R.string.adproductid))) {
                PrefHelper.setIfAdsRemoved(getActivity(), true);
            }
        }

        @Override
        public void onPurchaseHistoryRestored() {
            if (bp.isPurchased(getString(R.string.adproductid))) {
                PrefHelper.setIfAdsRemoved(getActivity(), true);
            } else
                PrefHelper.setIfAdsRemoved(getActivity(), false);
        }

        @Override
        public void onBillingInitialized() {
        }

        @Override
        public void onBillingError(int errorCode, Throwable error) {
            PrefHelper.setIfAdsRemoved(getActivity(), false);
        }

        @Override
        public void onDestroy() {
            if (bp != null)
                bp.release();

            super.onDestroy();
        }

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}