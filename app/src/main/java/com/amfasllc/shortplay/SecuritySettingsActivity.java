package com.amfasllc.shortplay;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.annotation.NonNull;

import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import com.afollestad.digitus.Digitus;
import com.afollestad.digitus.DigitusCallback;
import com.afollestad.digitus.DigitusErrorType;
import com.afollestad.digitus.FingerprintDialog;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.amfasllc.shortplay.helpers.PrefHelper;

import org.polaric.colorful.ColorfulActivity;

public class SecuritySettingsActivity extends ColorfulActivity implements DigitusCallback,
        FingerprintDialog.Callback {

    private static final int EXCLUDED_REQUEST = 8000;

    private CheckBoxPreference secureHidden;
    private CheckBoxPreference secureAppAccess;
    private CheckBoxPreference secureFunctions;

    private String requestChange = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preference_activity_custom);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction().replace(R.id.content_frame, new
                    SettingsFragment()).commit();
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == EXCLUDED_REQUEST) {
            setResult(resultCode);
        }
    }

    @Override
    public void onFingerprintDialogAuthenticated() {
        Toast.makeText(this, "Authenticated successfully", Toast.LENGTH_SHORT).show();

        if (requestChange.equals(getResources().getString(R.string.hide_folders)))
            secureHidden.setChecked(false);
        else if (requestChange.equals(getResources().getString(R.string.secure)))
            secureFunctions.setChecked(false);
        else if (requestChange.equals(getResources().getString(R.string.appaccess)))
            secureAppAccess.setChecked(false);

    }

    @Override
    public void onFingerprintDialogVerifyPassword(FingerprintDialog dialog, String password) {
        dialog.notifyPasswordValidation(password.equals(PrefHelper.getHiddenPasscode(this)));
    }

    @Override
    public void onFingerprintDialogStageUpdated(FingerprintDialog dialog, FingerprintDialog.Stage stage) {
    }

    @Override
    public void onFingerprintDialogCancelled() {
        Toast.makeText(this, "Authentication dialog cancelled", Toast.LENGTH_SHORT).show();

        if (requestChange.equals(getResources().getString(R.string.hide_folders)))
            secureHidden.setChecked(true);
        else if (requestChange.equals(getResources().getString(R.string.secure)))
            secureFunctions.setChecked(true);
        else if (requestChange.equals(getResources().getString(R.string.appaccess)))
            secureAppAccess.setChecked(true);
    }

    @Override
    public void onDigitusReady(Digitus digitus) {

    }

    @Override
    public void onDigitusListening(boolean newFingerprint) {

    }

    @Override
    public void onDigitusAuthenticated(Digitus digitus) {
        requestChange = "";
    }

    @Override
    public void onDigitusError(Digitus digitus, DigitusErrorType type, Exception e) {

    }

    //TODO FIX WEIRD DIALOG FOR PASSWORD/PIN

    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.security);

            ((SecuritySettingsActivity) getActivity()).secureFunctions = (CheckBoxPreference)
                    findPreference("secure");
            ((SecuritySettingsActivity) getActivity()).secureFunctions
                    .setOnPreferenceClickListener(secureListener);

            ((SecuritySettingsActivity) getActivity()).secureHidden = (CheckBoxPreference)
                    findPreference(PrefHelper.SECURE_HIDDEN);
            ((SecuritySettingsActivity) getActivity()).secureHidden
                    .setOnPreferenceClickListener(click);

            ((SecuritySettingsActivity) getActivity()).secureAppAccess = (CheckBoxPreference)
                    findPreference(PrefHelper.SECURE_APPACCESS);
            ((SecuritySettingsActivity) getActivity()).secureAppAccess
                    .setOnPreferenceClickListener(click);
        }

        private Preference.OnPreferenceClickListener click = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ((CheckBoxPreference) preference).setChecked(!((CheckBoxPreference)
                        preference).isChecked());

                if (((CheckBoxPreference)preference).isChecked()) {
                    final String method = PrefHelper.getSecureMethod(getActivity());
                    if (!method.equals(getResources().getString(R.string.fingerprint))) {
                        MaterialDialog dialog = new MaterialDialog.Builder
                                (getActivity())
                                .title("Enter " + method.toLowerCase())
                                .positiveText("Enter")
                                .input("Enter " + method.toLowerCase(), "", false, callback)
                                .inputType(getMethodInput(method))
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog,
                                                        @NonNull DialogAction which) {
                                        if (dialog.getInputEditText().getText().toString().equals(PrefHelper.getHiddenPasscode(getActivity()))) {
                                            ((CheckBoxPreference) preference).setChecked(!((CheckBoxPreference) preference).isChecked());
                                        } else {
                                            dialog.getInputEditText().setError("Wrong " + method);
                                        }
                                    }
                                })
                                .build();
                        dialog.show();
                    } else {
                        if (((CheckBoxPreference) preference).isChecked()) {
                            ((SecuritySettingsActivity) getActivity()).requestChange =
                                    preference.getTitle().toString();

                            FingerprintDialog.show(((SecuritySettingsActivity)
                                    getActivity()), getString(R.string.app_name), 69, true);
                        } else {
                            ((CheckBoxPreference) preference).setChecked(!(
                                    (CheckBoxPreference) preference).isChecked());
                        }
                    }
                    return true;
                } else {
                    ((CheckBoxPreference) preference).setChecked(!(
                            (CheckBoxPreference) preference).isChecked());
                }
                return true;
            }
        };

        private CheckBoxPreference.OnPreferenceClickListener secureListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(final Preference preference) {
                ((CheckBoxPreference) preference).setChecked(!((CheckBoxPreference)
                        preference).isChecked());

                if (!((CheckBoxPreference) preference).isChecked()) {
                    MaterialDialog.Builder dialog = new MaterialDialog.Builder(getActivity());
                    dialog.title("Security options")
                            .items(supportsFingerPrint() ? R.array.pref_security_options :
                                    R.array.pref_security_options_nofinger)
                            .itemsCallbackSingleChoice(-1, new MaterialDialog.ListCallbackSingleChoice() {
                                @Override
                                public boolean onSelection(MaterialDialog dialog, View itemView, int which, CharSequence text) {
                                    final String method = text.toString();
                                    if (method.equals(getResources().getString(R.string.fingerprint))) {
                                        MaterialDialog.Builder dialog2 = new MaterialDialog.Builder(getActivity());
                                        dialog2.title("Enter backup password")
                                                .positiveText("Enter")
                                                .negativeText("Cancel")
                                                .input("Enter backup password", "", callback)
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                                        PrefHelper.setSecureMethod(getActivity(), method);
                                                        PrefHelper.setHiddenPasscode(getActivity(), dialog.getInputEditText().getText().toString());
                                                        ((CheckBoxPreference) preference).setChecked(true);
                                                    }
                                                })
                                                .build().show();
                                    } else {
                                        MaterialDialog passwordDialog = new
                                                MaterialDialog.Builder(getActivity())
                                                .title("Enter " + method.toLowerCase())
                                                .positiveText("Enter")
                                                .input("Enter " + method.toLowerCase(), "", callback)
                                                .inputType(getMethodInput(method))
                                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                    @Override
                                                    public void onClick(@NonNull MaterialDialog dialog,
                                                                        @NonNull DialogAction which) {
                                                        String passcode = dialog.getInputEditText().getText().toString();
                                                        PrefHelper.setSecureMethod(getActivity(), method);
                                                        PrefHelper.setHiddenPasscode(getActivity(), passcode);
                                                        ((CheckBoxPreference) preference).setChecked(true);
                                                    }
                                                })
                                                .build();
                                        passwordDialog.show();
                                    }
                                    return true;
                                }
                            })
                            .dismissListener(new DialogInterface.OnDismissListener() {
                                @Override
                                public void onDismiss(DialogInterface dialog) {
                                    if (((CheckBoxPreference) preference).isChecked()) {
                                        String method = PrefHelper.getSecureMethod(getActivity());
                                        if (!method.equals(getResources().getString(R.string.fingerprint))) {
                                            MaterialDialog passwordDialog = new
                                                    MaterialDialog.Builder(getActivity())
                                                    .title("Enter " + method.toLowerCase())
                                                    .positiveText("Enter")
                                                    .input("Enter " + method.toLowerCase(), "", callback)
                                                    .inputType(getMethodInput(method))
                                                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                                                        @Override
                                                        public void onClick(@NonNull MaterialDialog dialog,
                                                                            @NonNull DialogAction which) {
                                                            String passcode = dialog.getInputEditText().getText().toString();
                                                            PrefHelper.setHiddenPasscode(getActivity().getApplicationContext(), passcode);
                                                        }
                                                    })
                                                    .build();
                                            passwordDialog.show();
                                        }
                                    }
                                }
                            })
                            .cancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    ((CheckBoxPreference) preference).setChecked(false);
                                }
                            })
                            .build().show();
                } else {
                    final String method = PrefHelper.getSecureMethod(getActivity());
                    if (!method.equals(getResources().getString(R.string.fingerprint))) {
                        MaterialDialog dialog = new MaterialDialog.Builder(getActivity())
                                .title("Enter " + method.toLowerCase())
                                .positiveText("Enter")
                                .input("Enter " + method.toLowerCase(), "", callback)
                                .inputType(getMethodInput(method))
                                .onPositive(new MaterialDialog.SingleButtonCallback() {
                                    @Override
                                    public void onClick(@NonNull MaterialDialog dialog,
                                                        @NonNull DialogAction which) {
                                        if (dialog.getInputEditText().getText().toString().equals(PrefHelper.getHiddenPasscode(getActivity()))) {
                                            PrefHelper.setSecureMethod(getActivity(), method);
                                            ((CheckBoxPreference) preference).setChecked(false);
                                        } else {
                                            dialog.getInputEditText().setError("Wrong " + method);
                                        }
                                    }
                                })
                                .build();
                        dialog.show();
                    } else {
                        ((SecuritySettingsActivity) getActivity()).requestChange = getActivity().getResources().getString(R.string.secure);

                        FingerprintDialog.show(((SecuritySettingsActivity) getActivity()), getString(R.string.app_name), 69, true);
                    }
                }
                return true;
            }
        };

        private MaterialDialog.InputCallback callback = new MaterialDialog.InputCallback() {
            @Override
            public void onInput(@NonNull MaterialDialog dialog, CharSequence input) {
            }
        };

        private int getMethodInput(String method) {
            return method.equals(getResources().getString(R.string.pin)) ? InputType.TYPE_CLASS_NUMBER : InputType.TYPE_CLASS_TEXT;
        }

        private boolean supportsFingerPrint(){
            if (Build.VERSION.SDK_INT >= 23) {
                //Fingerprint API only available on from Android 6.0 (M)
                FingerprintManager fingerprintManager = (FingerprintManager) getActivity().getSystemService(Context.FINGERPRINT_SERVICE);
                return fingerprintManager.isHardwareDetected() && fingerprintManager.hasEnrolledFingerprints();
            }
            return false;
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            super.onViewCreated(view, savedInstanceState);
            ListView list = (ListView) view.findViewById(android.R.id.list);
            list.setDivider(null);
            list.setDividerHeight(0);
            int listTopBottomPadding = 8;
            list.setPadding(0, listTopBottomPadding, 0, listTopBottomPadding);
            list.setClipToPadding(false);
        }

        @Override
        public void onPause() {
            super.onPause();

            FingerprintDialog dialog = FingerprintDialog.getVisible((SecuritySettingsActivity)
                    getActivity());

            if (dialog != null && dialog.isVisible()) {
                dialog.onDestroy();
                ((SecuritySettingsActivity) getActivity()).onFingerprintDialogCancelled();
            }
        }
    }
}
