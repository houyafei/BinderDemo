package com.example.houyafei.appaidl;

import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.example.houyafei.appaidl.binderService.BinderPool;
import com.example.houyafei.appaidl.binderService.InterfaceStubUtils;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main";
    private TextView textView ;

    private BinderPool mBinderPool ;

    int result = 0 ;

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            if (textView!=null){
                String text = msg.getData().getString("value");
                if (text!=null){
                    textView.append(text);
                }

            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = (TextView) findViewById(R.id.msg);


        new Thread(new Runnable() {
            @Override
            public void run() {
                //创建连接池
                mBinderPool = BinderPool.getInstance(MainActivity.this);
            }
        }).start();

    }

    public void doWork(View view){

        switch (view.getId()){

            case R.id.btn_do_work:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        usingIComputeInter();
                        usingISecurityInter();
                    }
                }).start();

                break;

        }

    }

    private void usingISecurityInter() {
        if (mBinderPool==null){
            //textView.setText("Myservice 未打开");
        }

        IBinder iBinder = mBinderPool.queryBinder(InterfaceStubUtils.REQUEST_SECRITY);
        ISecurityInterface iSecurityInterface = ISecurityInterface.Stub.asInterface(iBinder);
        try {
            String password = iSecurityInterface.setPassword(" "+(Math.random()*1000));
            String text = "\n\nISecurityInterface的AIDL "+iBinder.toString()
                    +"\npassword = "+password ;
            handleMsg2TextView(text);

            Log.e(TAG,"\nISecurityInterface的AIDL "+iBinder.toString()
                    +"\npassword = "+password);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

    }

    private void handleMsg2TextView(String text) {
        Bundle bundle = new Bundle();
        bundle.putString("value",text);
        Message message = new Message();
        message.setData(bundle);
        handler.sendMessage(message);
    }

    private void usingIComputeInter() {

        if (mBinderPool == null) {
            //textView.setText("Myservice 未打开");
            Log.e("Main","Myservice 未打开") ;
        }


        IBinder iBinder = mBinderPool.queryBinder(InterfaceStubUtils.REQUEST_COMPUTE);

        IComputeInterface iComputeInterface = IComputeInterface.Stub.asInterface(iBinder);

        int a = (int) (Math.random() * 10000);
        int b = (int) (Math.random() * 10000);

        if (iComputeInterface == null) {
            Log.e("Main","IComputeInterface为空") ;
            return ;
        }

        try {
            result = iComputeInterface.compute(a, b);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        String text = "\n\nIComputeInterface的AIDL：" + iBinder.toString()
                + "\n计算("+a +","+ b+") = " + result ;
        handleMsg2TextView(text);

       Log.e(TAG,"\nIComputeInterface的AIDL：" + iBinder.toString()
                + "\n计算(\"+a +\",\"+ b+\") = " + result);

    }


}
