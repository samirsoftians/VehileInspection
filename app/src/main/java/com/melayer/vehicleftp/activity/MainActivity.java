package com.melayer.vehicleftp.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.melayer.vehicleftp.R;
import com.melayer.vehicleftp.database.Database;
import com.melayer.vehicleftp.database.Helper;
import com.melayer.vehicleftp.database.repository.RepoImplLogin;
import com.melayer.vehicleftp.database.repository.RepoLogin;
import com.melayer.vehicleftp.fragment.GridFragment;
import com.melayer.vehicleftp.fragment.LoginFragment;
import com.melayer.vehicleftp.prefs.Prefs;

public class MainActivity extends AppCompatActivity

{//*****
    public static final String KEY_FRAGMENT_NAME = "key_fragment_name";
    public static final String TAG = "@vehicleftp";
    public static final int MY_PERMISSIONS_REQUESTS = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        Prefs.clearKeyLicenseValidity(getApplicationContext());
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        RepoLogin repoLogin = new RepoImplLogin(getDbHelper());
        try {
            Log.i(MainActivity.TAG, "User Id" + repoLogin.getUserId());
            if (repoLogin.getUserId().equals(""))
                runFragmentTransaction(R.id.frameMainContainer, LoginFragment.newInstance());
            else
                runFragmentTransaction(R.id.frameMainContainer, GridFragment.newInstance());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void snack(View rootView, String message) {
        Snackbar snackBar = Snackbar.make(rootView, (Html.fromHtml("<font color='#FFFFFF'>" + message + "</font>")), Snackbar.LENGTH_SHORT);
        snackBar.getView().setBackgroundColor(ContextCompat.getColor(this, R.color.snack));
        snackBar.show();
    }

    @Override
    public void onBackPressed()
    {
        removeFragmentFromBackStack();
    }

    public final void removeFragmentFromBackStack()
    {
      Log.i(MainActivity.TAG,"Fragment Name "+getSupportFragmentManager().getBackStackEntryAt
              (getSupportFragmentManager().getBackStackEntryCount() -1).getName());
        if (getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).getName() != null) {
            if ((getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).
                    getName().equals("GridFragment"))
                    || (getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1).
                    getName().equals("FragmentLogin")))
            {
                dialogExitApp();
                //finish();
            } else {
                super.onBackPressed();
            }
        } else {
            dialogExitApp();
            //runFragmentTransaction(R.id.frameMainContainer, GridFragment.newInstance());
            //finish();
        }
        Log.i(MainActivity.TAG, "getSupportFragmentManager().getBackStackEntryCount()" + getSupportFragmentManager().getBackStackEntryCount());
    }

    private void dialogExitApp() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setMessage("Sure to Exit?")
                .setCancelable(false)
                .setPositiveButton("Exit", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.dismiss();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public final void popBackStack(Integer count) {
        for (Integer i = 0; i < count; i++) {
            getSupportFragmentManager().popBackStackImmediate();
        }
    }

    public final Fragment runFragmentTransaction(Integer containerId, Fragment fragment) {
        final String backStateName = fragment.getArguments().getString(KEY_FRAGMENT_NAME);
        FragmentManager manager = getSupportFragmentManager();
        boolean fragmentPopped = manager.popBackStackImmediate(backStateName, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        if (!fragmentPopped) {
            FragmentTransaction txn = manager.beginTransaction();
            txn.replace(containerId, fragment, backStateName);
            txn.addToBackStack(backStateName);
            txn.commitAllowingStateLoss();
        }

        else

            {
            //not Popped
            FragmentTransaction txn = manager.beginTransaction();
            txn.replace(containerId, fragment, backStateName);
            txn.addToBackStack(null);
            txn.commit();
        }
        return fragment;
    }

    public final boolean isNetworkAvailable()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public void hideKeyboard(View view)
    {
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }

    public final Helper getDbHelper() {
        return new Helper(this, Database.DB_NAME, null, Database.DB_VERSION);
    }

    public final String getDeviceUniqueId() {
        TelephonyManager telephonyManager;
        WifiManager wifiManager;
        WifiInfo wifiInfo = null;
        String imei = null;
        String macId = null;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_DENIED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE,
                            Manifest.permission.ACCESS_WIFI_STATE,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.CALL_PHONE,
                            Manifest.permission.CAMERA
                    },MY_PERMISSIONS_REQUESTS);
        } else {
            telephonyManager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
            imei = telephonyManager.getDeviceId();
            wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//            wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
            wifiInfo = wifiManager.getConnectionInfo();
            macId = wifiInfo.getMacAddress();
        }
        Log.i(TAG, "Imei - " + imei);
        Log.i(TAG, "MacId - " + macId);
        return (((imei != null) && !imei.equals("")) ? imei : macId);
    }
}
