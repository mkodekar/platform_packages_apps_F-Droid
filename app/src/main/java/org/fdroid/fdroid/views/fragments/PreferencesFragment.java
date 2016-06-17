package org.fdroid.fdroid.views.fragments;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.support.v4.preference.PreferenceFragment;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.text.TextUtils;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.PreferencesActivity;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.PreferencesCompat;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import info.guardianproject.netcipher.NetCipher;
import info.guardianproject.netcipher.proxy.OrbotHelper;

public class PreferencesFragment extends PreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String[] SUMMARIES_TO_UPDATE = {
        Preferences.PREF_UPD_INTERVAL,
        Preferences.PREF_UPD_WIFI_ONLY,
        Preferences.PREF_UPD_NOTIFY,
        Preferences.PREF_UPD_HISTORY,
        Preferences.PREF_ROOTED,
        Preferences.PREF_INCOMP_VER,
        Preferences.PREF_THEME,
        Preferences.PREF_IGN_TOUCH,
        Preferences.PREF_LOCAL_REPO_NAME,
        Preferences.PREF_LANGUAGE,
        Preferences.PREF_KEEP_CACHE_TIME,
        Preferences.PREF_EXPERT,
        //Preferences.PREF_PRIVILEGED_INSTALLER,
        Preferences.PREF_ENABLE_PROXY,
        Preferences.PREF_PROXY_HOST,
        Preferences.PREF_PROXY_PORT,
    };

    private static final int REQUEST_INSTALL_ORBOT = 0x1234;
    private CheckBoxPreference enableProxyCheckPref;
    private CheckBoxPreference useTorCheckPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        useTorCheckPref = (CheckBoxPreference) findPreference(Preferences.PREF_USE_TOR);
        enableProxyCheckPref = (CheckBoxPreference) findPreference(Preferences.PREF_ENABLE_PROXY);
    }

    private void checkSummary(String key, int resId) {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(key);
        pref.setSummary(resId);
    }

    private void entrySummary(String key) {
        ListPreference pref = (ListPreference) findPreference(key);
        pref.setSummary(pref.getEntry());
    }

    private void textSummary(String key, int resId) {
        EditTextPreference pref = (EditTextPreference) findPreference(key);
        pref.setSummary(getString(resId, pref.getText()));
    }

    private void updateSummary(String key, boolean changing) {

        int result = 0;

        switch (key) {
            case Preferences.PREF_UPD_INTERVAL:
                ListPreference listPref = (ListPreference) findPreference(
                        Preferences.PREF_UPD_INTERVAL);
                int interval = Integer.parseInt(listPref.getValue());
                Preference onlyOnWifi = findPreference(
                        Preferences.PREF_UPD_WIFI_ONLY);
                onlyOnWifi.setEnabled(interval > 0);
                if (interval == 0) {
                    listPref.setSummary(R.string.update_interval_zero);
                } else {
                    listPref.setSummary(listPref.getEntry());
                }
                break;

            case Preferences.PREF_UPD_WIFI_ONLY:
                checkSummary(key, R.string.automatic_scan_wifi_on);
                break;

            case Preferences.PREF_UPD_NOTIFY:
                checkSummary(key, R.string.notify_on);
                break;

            case Preferences.PREF_UPD_HISTORY:
                textSummary(key, R.string.update_history_summ);
                break;

            case Preferences.PREF_THEME:
                entrySummary(key);
                if (changing) {
                    result |= PreferencesActivity.RESULT_RESTART;
                    getActivity().setResult(result);
                }
                break;

            case Preferences.PREF_INCOMP_VER:
                checkSummary(key, R.string.show_incompat_versions_on);
                break;

            case Preferences.PREF_ROOTED:
                checkSummary(key, R.string.rooted_on);
                break;

            case Preferences.PREF_IGN_TOUCH:
                checkSummary(key, R.string.ignoreTouch_on);
                break;

            case Preferences.PREF_LOCAL_REPO_NAME:
                textSummary(key, R.string.local_repo_name_summary);
                break;

            case Preferences.PREF_LOCAL_REPO_HTTPS:
                checkSummary(key, R.string.local_repo_https_on);
                break;

            case Preferences.PREF_LANGUAGE:
                entrySummary(key);
                if (changing) {
                    result |= PreferencesActivity.RESULT_RESTART;
                    Activity activity = getActivity();
                    activity.setResult(result);
                    ((FDroidApp) activity.getApplication()).updateLanguage();
                }
                break;

            case Preferences.PREF_KEEP_CACHE_TIME:
                entrySummary(key);
                break;

            case Preferences.PREF_EXPERT:
                checkSummary(key, R.string.expert_on);
                break;

            case Preferences.PREF_PRIVILEGED_INSTALLER:
                checkSummary(key, R.string.system_installer_on);
                break;

            case Preferences.PREF_ENABLE_PROXY:
                CheckBoxPreference checkPref = (CheckBoxPreference) findPreference(key);
                checkPref.setSummary(R.string.enable_proxy_summary);
                break;

            case Preferences.PREF_PROXY_HOST:
                EditTextPreference textPref = (EditTextPreference) findPreference(key);
                String text = Preferences.get().getProxyHost();
                if (TextUtils.isEmpty(text) || text.equals(Preferences.DEFAULT_PROXY_HOST)) {
                    textPref.setSummary(R.string.proxy_host_summary);
                } else {
                    textPref.setSummary(text);
                }
                break;

            case Preferences.PREF_PROXY_PORT:
                EditTextPreference textPref2 = (EditTextPreference) findPreference(key);
                int port = Preferences.get().getProxyPort();
                if (port == Preferences.DEFAULT_PROXY_PORT) {
                    textPref2.setSummary(R.string.proxy_port_summary);
                } else {
                    textPref2.setSummary(String.valueOf(port));
                }
                break;

        }
    }

    /**
     * Initializes SystemInstaller preference, which can only be enabled when F-Droid is installed as a system-app
     */
    private void initPrivilegedInstallerPreference() {
        CheckBoxPreference pref = (CheckBoxPreference) findPreference(Preferences.PREF_PRIVILEGED_INSTALLER);

        // we are handling persistence ourself!
        pref.setPersistent(false);

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                final CheckBoxPreference pref = (CheckBoxPreference) preference;

                if (pref.isChecked()) {
                    int isInstalledCorrectly =
                            PrivilegedInstaller.isExtensionInstalledCorrectly(getActivity());
                    if (isInstalledCorrectly == PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES) {
                        // privileged permission are granted, i.e. the extension is installed correctly
                        SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                        editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, true);
                        PreferencesCompat.apply(editor);
                        pref.setChecked(true);
                    } else {
                        // privileged permission not available
                        SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                        editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, false);
                        PreferencesCompat.apply(editor);
                        pref.setChecked(false);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
                        alertBuilder.setTitle(R.string.system_install_denied_title);

                        String message;
                        switch (isInstalledCorrectly) {
                            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_NO:
                                message = getActivity().getString(R.string.system_install_denied_body) +
                                        "<br/><br/>" + getActivity().getString(R.string.system_install_question);
                                break;
                            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM:
                                message = getActivity().getString(R.string.system_install_denied_signature);
                                break;
                            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_PERMISSIONS_PROBLEM:
                                message = getActivity().getString(R.string.system_install_denied_permissions);
                                break;
                            default:
                                throw new RuntimeException("unhandled return");
                        }
                        alertBuilder.setMessage(Html.fromHtml(message));
                        alertBuilder.setPositiveButton(R.string.system_install_button_open, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Open details of F-Droid Privileged
                                Intent intent = new Intent(getActivity(), AppDetails.class);
                                intent.putExtra(AppDetails.EXTRA_APPID,
                                        PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
                                startActivity(intent);
                            }
                        });
                        alertBuilder.setNegativeButton(R.string.cancel, null);
                        alertBuilder.create().show();
                    }
                } else {
                    SharedPreferences.Editor editor = pref.getSharedPreferences().edit();
                    editor.putBoolean(Preferences.PREF_PRIVILEGED_INSTALLER, false);
                    PreferencesCompat.apply(editor);
                    pref.setChecked(false);
                }

                return true;
            }
        });
    }

    private void initManagePrivilegedAppPreference() {
        Preference pref = findPreference(Preferences.PREF_UNINSTALL_PRIVILEGED_APP);
        pref.setPersistent(false);

        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {

            @Override
            public boolean onPreferenceClick(Preference preference) {
                // Open details of F-Droid Privileged
                Intent intent = new Intent(getActivity(), AppDetails.class);
                intent.putExtra(AppDetails.EXTRA_APPID,
                        PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
                startActivity(intent);

                return true;
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        for (final String key : SUMMARIES_TO_UPDATE) {
            updateSummary(key, false);
        }

        /*
        initPrivilegedInstallerPreference();
        initManagePrivilegedAppPreference();
        */
        // this pref's default is dynamically set based on whether Orbot is installed
        boolean useTor = Preferences.get().isTorEnabled();
        useTorCheckPref.setDefaultValue(useTor);
        useTorCheckPref.setChecked(useTor);
        useTorCheckPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object enabled) {
                if ((Boolean) enabled) {
                    final Activity activity = getActivity();
                    enableProxyCheckPref.setEnabled(false);
                    if (OrbotHelper.isOrbotInstalled(activity)) {
                        NetCipher.useTor();
                    } else {
                        Intent intent = OrbotHelper.getOrbotInstallIntent(activity);
                        activity.startActivityForResult(intent, REQUEST_INSTALL_ORBOT);
                    }
                } else {
                    enableProxyCheckPref.setEnabled(true);
                    NetCipher.clearProxy();
                }
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        Preferences.get().configureProxy();
    }

    @Override
    public void onSharedPreferenceChanged(
            SharedPreferences sharedPreferences, String key) {
        updateSummary(key, true);
    }

}
