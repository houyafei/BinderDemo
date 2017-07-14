package com.example.houyafei.appaidl.binderService;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.example.houyafei.appaidl.binderService.InterfaceStubUtils.IBinderInterfaceImpl;


public class MyService extends Service {

    private static final String TAG = "main";
    private IBinder iBinder = new IBinderInterfaceImpl();


    @Override
    public void onCreate() {
        super.onCreate();

        Log.e(TAG,"myservice onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    public MyService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG,"myservice onBind()");
        return iBinder ;
    }
}
