package com.melayer.vehicleftp.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.melayer.vehicleftp.Main2Activity;
import com.melayer.vehicleftp.R;
import com.melayer.vehicleftp.activity.MainActivity;
import com.melayer.vehicleftp.database.repository.RepoImplLogin;
import com.melayer.vehicleftp.database.repository.RepoLogin;
import com.melayer.vehicleftp.web.Url;
import com.melayer.vehicleftp.web.Ws;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by root on 22/8/16.
 */
public class LoginFragment extends Fragment{
    public static final String KEY_FRAGMENT_NAME = "key_fragment_name";
    public static final int PERMISSION_READ_PHONE_STATE = 5;
Button button;

    public static LoginFragment newInstance() {
        LoginFragment loginFragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(KEY_FRAGMENT_NAME, "FragmentLogin");
        loginFragment.setArguments(args);
        return loginFragment;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_login,container,false);
        initLoginButton(rootView);
        button=(Button)rootView.findViewById(R.id.btreg);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(getActivity(),Main2Activity.class);
                getActivity().startActivity(intent);

            }
        });
        initRegisterButton(rootView);
        return rootView;
    }

    private void initRegisterButton(View rootView) {

        rootView.findViewById(R.id.btreg);
        rootView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent intent;
                intent = new Intent(getActivity(), Main2Activity.class);
                getActivity().startActivity(intent);
            }
        });


    }

    private MainActivity getParent(){

        return (MainActivity) getActivity();
    }

    private void initLoginButton(final View rootView) {
        rootView.findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                    if (isUserNameValid(rootView) && isPasswordValid(rootView) ) {
                        if (getParent().isNetworkAvailable()) {
                            registerUser();

                        }
                        else {
                            getParent().snack(rootView, "Unable to connect to the server:(");
                    }
                }

            }
        });
    }





    private final Boolean isUserNameValid(final View rootView) {
        Boolean isValid = false;
        final Drawable drawableError = ContextCompat.getDrawable(getContext(), R.drawable.ic_error);
        drawableError.setBounds(0, 0, 50, 50);
        if (getUserName(rootView).getText().length() < 1) {
            getUserName(rootView).setError("Username required", drawableError);
            isValid = false;
        } else {
            getUserName(rootView).setError(null);
            getUserName(rootView).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done, 0);
            isValid = true;
        }
        return isValid;
    }

    private final Boolean isPasswordValid(final View rootView) {
        Boolean isValid = false;
        final Drawable drawableError = ContextCompat.getDrawable(getContext(), R.drawable.ic_error);
        drawableError.setBounds(0, 0, 50, 50);
        if (getPassword(rootView).getText().length() < 1) {
            getPassword(rootView).setError("Password required", drawableError);
            isValid = false;
        } else {
            getPassword(rootView).setError(null);
            getPassword(rootView).setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done, 0);
            isValid = true;
        }
        return isValid;
    }
    private EditText getUserName(View rootView){
        return (EditText) rootView.findViewById(R.id.edtUserName);
    }
    private EditText getPassword(View rootView){
        return (EditText) rootView.findViewById(R.id.edtPassword);
    }
    private String getImei() {
        TelephonyManager manager = (TelephonyManager) getActivity().getSystemService(Context.TELEPHONY_SERVICE);
        String imei = manager.getDeviceId();
        return (imei != null & imei.length() > 0) ? imei : "none";
    }


    public void loginUser() {

        final ProgressDialog progressDialog = ProgressDialog.show(getParent(),"Login", "Checking credentials");
        //Log.i(MainActivity.TAG,"Imei in login : " +getImei());
        final JSONObject obj = new JSONObject();
        try {
            obj.put("userName",getUserName(getView()).getText().toString());
            obj.put("password", getPassword(getView()).getText().toString());
            obj.put("imei",getImei());
            obj.put("currentDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
            Log.i(MainActivity.TAG, "obj to send- " + obj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }

        String urlToLogin =Url.URL_LOGIN +"?userName="+getUserName(getView()).getText().toString()
                +"&password="+getPassword(getView()).getText().toString()+"&imei="+getImei()+"&currentDate="+new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        Log.i(MainActivity.TAG, "Login url - " + urlToLogin);
        Ws.getQueue(getContext()).add(new JsonObjectRequest(com.android.volley.Request.Method.POST, urlToLogin,
                new com.android.volley.Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i(MainActivity.TAG, "response b4 if - " + response);
                        if (progressDialog !=  null && progressDialog.isShowing())
                            progressDialog.dismiss();
                        if (response != null) {
                            Log.i(MainActivity.TAG, "response after if - " + response);
                            try {
                                if (response.getString("status").equals("success")) {
                                    Log.i(MainActivity.TAG, "response - " + response.getString("status"));
                                    storeUser(response);
                                    getParent().runFragmentTransaction(R.id.frameMainContainer, GridFragment.newInstance());
                                }
                                if (response.getString("status").equals("fail")) {
                                    getParent().snack(getView(),"Invalid Credentials");
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                }, new com.android.volley.Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
               /* Log.i(MainActivity.TAG, "Error : "+error.toString());
                getParent().snack(getView(),"Server Not Responding...");*/
               progressDialog.dismiss();
                    getParent().snack(getView(),"Invalid Credentials");

            }
        }) {
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String> headers = new HashMap<>();
               headers.put("Content-Type", "application/json");
                headers.put("Accept", "application/json");
                return headers;
            }
            {
                setRetryPolicy(new RetryPolicy() {
                    @Override
                    public int getCurrentTimeout() {
                        return DefaultRetryPolicy.DEFAULT_TIMEOUT_MS * 48;
                    }
                    @Override
                    public int getCurrentRetryCount() {
                        return 0;
                    }
                    @Override
                    public void retry(VolleyError error) throws VolleyError {
                        if (getView()!=null)
                            getParent().snack(getView(),"Internet not available!");
                    }
                });
            }
        });
    }

    private void storeUser(JSONObject response) {
        RepoLogin repoLogin = new RepoImplLogin(getParent().getDbHelper());
        try {
            JSONObject jsonObjectUser = response.getJSONObject("user");
            repoLogin.saveUserCredentials(jsonObjectUser.getString("userName"),jsonObjectUser.getInt("userId"));

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_READ_PHONE_STATE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) registerUser();
            }
        }
    }

    private void registerUser() {
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.READ_PHONE_STATE}, PERMISSION_READ_PHONE_STATE);
            Log.i(MainActivity.TAG, "In permission");
            return;
        }
        if (getParent().isNetworkAvailable()) {
            loginUser();
        }else getParent().snack(getView(),"Internate Not Available..");

    }
}
