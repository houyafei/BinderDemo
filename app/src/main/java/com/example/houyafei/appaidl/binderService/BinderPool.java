package com.example.houyafei.appaidl.binderService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import com.example.houyafei.appaidl.IBinderInterface;

import java.util.concurrent.CountDownLatch;

/**
 * Created by Hou Yafei on 2017/7/14.
 * yafeihou@sjtu.edu.cn
 */
public class BinderPool {

    private Context mContext ;

    private CountDownLatch mConnBinderPoolCountDownLatch ;

    private static volatile BinderPool mInstance ;

    private IBinderInterface iBinderInterface ;

    private IBinder.DeathRecipient mBinderPoolDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {

            iBinderInterface.asBinder().unlinkToDeath(mBinderPoolDeathRecipient, 0);
            iBinderInterface = null;

            //链接断了重新连接
            connectMyService();
        }
    };

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {

            iBinderInterface = IBinderInterface.Stub.asInterface(iBinder);

            try {
                iBinderInterface.asBinder().linkToDeath(mBinderPoolDeathRecipient,0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }

            mConnBinderPoolCountDownLatch.countDown();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };


    /**
     * 单例模式，构造方法
     * @param context 上下文对象
     */
    private BinderPool(Context context){

        mContext = context.getApplicationContext() ;

        connectMyService();

    }

    public static  BinderPool getInstance(Context context){

        if (mInstance==null){
            synchronized (BinderPool.class){
                mInstance = new BinderPool(context);
            }
        }

        return mInstance ;
    }

    /**
     * 绑定服务
     */
    private synchronized void connectMyService() {

        mConnBinderPoolCountDownLatch = new CountDownLatch(1);

        mContext.bindService(new Intent(mContext,MyService.class),
                mConnection,Context.BIND_AUTO_CREATE) ;

        try {
            mConnBinderPoolCountDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    /**
     * 获取IBinder对象
     * 该类提供给客户端链接使用
     */
    public IBinder queryBinder(int requestCode){

        IBinder iBinder = null;

        if (iBinderInterface != null) {
            try {
                iBinder = iBinderInterface.queryBinder(requestCode);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        return iBinder;


    }



}
