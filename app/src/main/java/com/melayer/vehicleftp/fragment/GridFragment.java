package com.melayer.vehicleftp.fragment;

import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.JsonObjectRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.melayer.vehicleftp.R;
import com.melayer.vehicleftp.activity.MainActivity;
import com.melayer.vehicleftp.database.RepoFactory;
import com.melayer.vehicleftp.database.repository.RepoImplLogin;
import com.melayer.vehicleftp.database.repository.RepoImplModule;
import com.melayer.vehicleftp.database.repository.RepoImplRegisterVehicle;
import com.melayer.vehicleftp.database.repository.RepoLogin;
import com.melayer.vehicleftp.database.repository.RepoModule;
import com.melayer.vehicleftp.database.repository.RepoRegisterVehicle;
import com.melayer.vehicleftp.domain.Module;
import com.melayer.vehicleftp.domain.VehicleRegistration;
import com.melayer.vehicleftp.prefs.Prefs;
import com.melayer.vehicleftp.web.Connectable;
import com.melayer.vehicleftp.web.JsonMan;
import com.melayer.vehicleftp.web.MultipartRequest;
import com.melayer.vehicleftp.web.NetworkUtils;
import com.melayer.vehicleftp.web.Url;
import com.melayer.vehicleftp.web.Ws;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;

/**
 * Created by root on 10/9/16.
 */
public class GridFragment extends Fragment {
    public static final String KEY_FRAGMENT_NAME = "key_fragment_name";
    private UploadDataServer uploadDataServer;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String status = NetworkUtils.getConnectivityStatusResult(context);
            Toast.makeText(context,status,Toast.LENGTH_SHORT);
            if (getParent().isNetworkAvailable())
                uploadData();
        }
    };

    public static GridFragment newInstance() {
        GridFragment fragment = new GridFragment();
        Bundle args = new Bundle();
        args.putString(KEY_FRAGMENT_NAME, "GridFragment");
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        uploadDataServer = new UploadDataServer();
    }

    @Override
    public void onResume() {
        super.onResume();
        getParent().registerReceiver(broadcastReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    public void onPause() {
        getParent().unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_grid, container, false);
        initLinearLayout(rootView);
        getParent().hideKeyboard(rootView);
        uploadData();
        return rootView;
    }

    private MainActivity getParent() {
        return (MainActivity) getActivity();
    }

    private void uploadData() {
        RepoModule repoModule = new RepoImplModule(getContext(),getParent().getDbHelper());
        try {
            Integer count = repoModule.checkDataInspections();
            RepoRegisterVehicle repoRegisterVehicle = new RepoImplRegisterVehicle(getParent().getDbHelper());

            Log.i(MainActivity.TAG, "count - " + count +"Prefs.getKeyVehicleNo(getContext()) : "+Prefs.getKeyVehicleNo(getContext()));
            if (getParent()!=null) {
                if (getParent().isNetworkAvailable()) {

                    uploadDataServer.registerVehicle();
                    if (count > 0) {//needs to check
                        MeTaskUploadVehicleData taskUploader = new MeTaskUploadVehicleData(getContext());
                        taskUploader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }

                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initLinearLayout(final View rootView) {
        rootView.findViewById(R.id.layoutInspection).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getParent().runFragmentTransaction(R.id.frameMainContainer, VehicleRegisterFragment.newInstance());
            }
        });
        rootView.findViewById(R.id.layoutUpload).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(MainActivity.TAG, "Uploading Starts");
                RepoModule repoModule = new RepoImplModule(getContext(),getParent().getDbHelper());
                try {
                    Integer count = repoModule.checkDataInspections();
                    Log.i(MainActivity.TAG, "count - " + count + "Prefs.getKeyVehicleNo(getContext()) : "+Prefs.getKeyVehicleNo(getContext()));
                    if (getParent().isNetworkAvailable()) {
                        if (count > 0) {
                            uploadDataServer.registerVehicle();
                        } else {
                            getParent().snack(rootView, "No Data available to Upload:(");
                        }
                    } else {
                        getParent().snack(rootView, "Unable to connect to the server:(");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private class UploadDataServer implements Connectable {
        private void registerVehicle() {
            final JSONObject obj = new JSONObject();
            RepoLogin repoLogin = new RepoImplLogin(getParent().getDbHelper());
            RepoRegisterVehicle repoRegisterVehicle = new RepoImplRegisterVehicle(getParent().getDbHelper());
            String vehicleNo = new String();
            List<VehicleRegistration> registrationList = new ArrayList<>();
            try {
                registrationList = repoRegisterVehicle.uploadVehicleDataToServer(repoLogin.getUserId());//Change from upload method to selectAll
                Log.i(MainActivity.TAG, "registrationList);" + registrationList);
            } catch (Exception e) {
                e.printStackTrace();
            }
            for (VehicleRegistration registration : registrationList) {
                if (registration != null) {
                    vehicleNo = registration.getVehicleNo();
                    String  urlToUploadVehicle = Url.URL_REGISTER_VEHICLE
                            +"?vehicleId="+registration.getVehicleId().trim().replace(" ","%20")
                            +"&vehicleNo="+ registration.getVehicleNo().trim().replace(" ","%20")
                            +"&psrName="+registration.getPsrName().trim().replace(" ","%20")
                            +"&cellNumber="+registration.getCellNumber().trim().replace(" ","%20")
                            +"&startPlace="+registration.getStartPlace().trim().replace(" ","%20")
                            +"&endPlace="+registration.getEndPlace().trim().replace(" ","%20")
                            +"&licenceNo="+registration.getLicenceNo().trim().replace(" ","%20")
                            +"&licenceValidity="+registration.getLicenceValidity().trim().replace(" ","%20")
                            +"&tdmAsmName="+registration.getTdmAsmName().trim().replace(" ","%20")
                            +"&tdmAsm="+registration.getTdmAsm().trim().replace(" ","%20")
                            +"&entryDate="+registration.getEntryDate().trim().replace(" ","%20")
                            +"&distributor="+registration.getDistributor().trim().replace(" ","%20")
                            +"&userId="+registration.getUserId();
                    Log.i(MainActivity.TAG, "registration url - " + urlToUploadVehicle);
                    final String finalVehicleNo = vehicleNo;
                    Ws.getQueue(getContext()).add(new JsonObjectRequest(com.android.volley.Request.Method.POST, urlToUploadVehicle,
                            new com.android.volley.Response.Listener<JSONObject>() {
                                @Override
                                public void onResponse(JSONObject response) {
                                    if (response != null) {
                                        if (getParent() != null) {
                                            RepoRegisterVehicle repoRegisterVehicle = new RepoImplRegisterVehicle(getParent().getDbHelper());
                                            try {
                                                repoRegisterVehicle.updateFlag(finalVehicleNo);
                                                repoRegisterVehicle.deleteTableData();
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            //uploadModuleDataToServer();
                                            //second web service
                                            // getParent().runFragmentTransaction(R.id.frameMainContainer, CheckListFragment.newInstance());
                                        }
                                    }
                                }
                            },
                            new com.android.volley.Response.ErrorListener() {
                                @Override
                                public void onErrorResponse(VolleyError error) {
                                    Log.i(MainActivity.TAG, error.toString());
                                    if (getView() != null)
                                        getParent().snack(getView(), "Internet not available!!");
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
                                    //if (getView() != null)
                                        //getParent().snack(getView(), "Internet not available!");
                                }
                            });
                        }
                    });
                }


            }
        }
    }

    public class MeTaskUploadVehicleData extends AsyncTask<Void, Void, Void> {
        private Context context;
        private ProgressDialog progressDialog;
        private NotificationManager mNotifyManager;
        private NotificationCompat.Builder mBuilder;
        int id = 1;
        private UploadAllModule uploadAllModule;

        public MeTaskUploadVehicleData(Context context) {
            this.context = context;
            uploadAllModule = new UploadAllModule();
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //  progressDialog = ProgressDialog.show(parentActivity, "Inspection Data", "saving inspection data");
            if (getView() != null)
                getView().findViewById(R.id.layoutUpload).setClickable(false);
            mNotifyManager = (NotificationManager) getParent().getSystemService(Context.NOTIFICATION_SERVICE);
            mBuilder = new NotificationCompat.Builder(getParent());
            mBuilder.setContentTitle("Upload Transworld Data")
                    .setContentText("Upload in progress")
                    .setSmallIcon(R.drawable.ic_file_upload);
            // new MeTaskUploader().execute();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                // uploadModule();
                uploadAllModule.saveVehicleDataToServer();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //progressDialog.dismiss();

            if (getView() != null)
                getView().findViewById(R.id.layoutUpload).setClickable(true);
            mBuilder.setContentText("Upload complete");
            // Removes the progress bar
            mBuilder.setProgress(0, 0, false);
            mNotifyManager.notify(id, mBuilder.build());
        }

        private final class UploadAllModule implements Connectable {
            public Map<String, Object> saveVehicleDataToServer() throws Exception {
                Map<String, Object> mapEntity = new HashMap<>();
                // RequestBody body = RequestBody.create(MediaType.parse("application/json; charset=utf-8"), JsonMan.fromObject(uploadContactToServer()));
                String vehicleNo = null;
                String chkPtName = null;
                Set<Module> moduleList = uploadModuleDataToServer();
                for (Module module : moduleList) {
                    Log.i(MainActivity.TAG, "Uploading module Data : " + module.toString());
                    MultipartBody.Builder builder = new MultipartBody.Builder();
                    vehicleNo = module.getVehicleNo();
                    builder.addFormDataPart("vehicleId", "" + module.getVehicleId());
                    builder.addFormDataPart("vehicleNo", module.getVehicleNo());
                    builder.addFormDataPart("unit", "" + module.getUnit());
                    builder.addFormDataPart("checkPointId", "" + module.getCheckPointId());
                    builder.addFormDataPart("checkPointName", module.getCheckPointName());
                    builder.addFormDataPart("checkPointStatus", module.getCheckPointStatus());
                    builder.addFormDataPart("finalStatus", module.getFinalInspectionStatus());
                    builder.addFormDataPart("remarks", module.getRemarks()!=null ? module.getRemarks() : "");
                    List<File> imgList = new ArrayList<>();
                    Log.i(MainActivity.TAG, "ImagePath in builder - " + module.getImage().toString());
                    if (module.getImage() != null) {
                        String[] str = module.getImage().toString().split("/");
                        String imagePath = str[str.length - 1];
                        imgList.add(module.getImage());
                        builder.addFormDataPart("imagePath", imagePath);
                        Log.i(MainActivity.TAG, "ImagePath" + imagePath);
                    } else {
                        builder.addFormDataPart("imagePath", "");
                    }
                    for (File file : imgList) {
                        if (file.exists()) {
                            builder.addFormDataPart("multipartFile", "" + (file.getName()), RequestBody.create(MediaType.parse("image"), file));
                        }
                    }
                    builder.addFormDataPart("entryDate", module.getEntryDate());
                    RepoLogin repoLogin = new RepoImplLogin(getParent().getDbHelper());
                    builder.addFormDataPart("userId", "" + repoLogin.getUserId());
                    builder.setType(MultipartBody.FORM);
                    RequestBody body = builder.build();
                    OkHttpClient client = new OkHttpClient();
                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(Url.URL_UPLOAD_MODULE)
                            .post(body)
                            .addHeader("Accept", "application/json")
                            .build();
                    okhttp3.Response response = client.newCall(request).execute();
                    String responseJson = response.body().string();
                    RepoModule repoModule = new RepoImplModule(getContext(),getParent().getDbHelper());
                    Log.i(MainActivity.TAG, "Response Json - " + responseJson);
                    mapEntity = JsonMan.parseAnything(responseJson, new TypeReference<Map<String, Object>>() {
                    });
                    Log.i(MainActivity.TAG, "Response Entity - " + mapEntity);
                    String status = mapEntity.get("status").toString();
                    if (status.equals("success")) {
                        RepoRegisterVehicle repoRegisterVehicle = new RepoImplRegisterVehicle(getParent().getDbHelper());
                        try {
                            repoModule.updateFlag(vehicleNo,module.getCheckPointName());

                            repoModule.deleteTableData();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                return mapEntity;
            }
        }

        /*   //Volley Call showing exception MultipartRequest.getBody: IOException writing to ByteArrayOutputStream
           private void uploadModule() {
               List<Module> moduleList = uploadModuleDataToServer();
               for (Module module : moduleList) {
                   final String vehicleNo = module.getVehicleNo();
                   Ws.getQueue(getParent()).add(new MultipartRequest(Url.URL_UPLOAD_MODULE, module, new com.android.volley.Response.Listener<String>() {
                       @Override
                       public void onResponse(String response) {
                           //    pd.dismiss();
                           RepoModule repoModule = new RepoImplModule(getContext(),getParent().getDbHelper());
                           RepoRegisterVehicle repoRegisterVehicle = new RepoImplRegisterVehicle(getParent().getDbHelper());
                           try {
                               repoModule.updateFlag(vehicleNo, module.getCheckPointName());
                               repoRegisterVehicle.deleteTableData();
                               repoModule.deleteTableData();
                           } catch (Exception e) {
                               e.printStackTrace();
                           }
                           Log.i(MainActivity.TAG, "Response Json - " + response);
                       }
                   }, new com.android.volley.Response.ErrorListener() {
                       @Override
                       public void onErrorResponse(VolleyError error) {
                           VolleyLog.v(error.getMessage());
                       }
                   }));
               }
           }
   */
        private Set<Module> uploadModuleDataToServer() {
            Set<Module> moduleList = new HashSet<>();
            RepoModule repoModule = new RepoImplModule(getContext(),getParent().getDbHelper());
            RepoLogin repoLogin = new RepoImplLogin(getParent().getDbHelper());
            try {
                moduleList = repoModule.uploadModuleData(repoLogin.getUserId());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return moduleList;
        }
    }
}