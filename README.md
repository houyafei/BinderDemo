# Binder线程池实现AIDL

## 1 说明

对于多模块AIDL的应用，由于业务复杂，如果为每个AIDL分别创建一个Service，必然浪费系统资源。
因此我们采用Binder线程池来进行管理每一个Binder链接。因为对应每一个AIDL文件接口，会产生一个Binder链接。
采用线程管理的方式，只需要一个Service进行处理，同时增强其扩展性。

## 2 例子

该demo实现了两个AIDL接口通过线程池获取Binder，然后根据Binder获取相对应的服务。
图示如下：

![Binder连接池原理]()

## 3 首先创建两个独立业务的aidl接口文件

这两个文件分别如下，内容比较简单，一个是计算两数的积，另外一个设置密码。

IComputeInterface.aidl接口

    // IComputeInterface.aidl
    package com.example.houyafei.appaidl;
    
    // Declare any non-default types here with import statements
    
    interface IComputeInterface {
    
        int compute(in int a, in int b);
    
    }
    
ISecurityInterface.aidl接口
    
    // ISecurityInterface.aidl
    package com.example.houyafei.appaidl;
    
    // Declare any non-default types here with import statements
    
    interface ISecurityInterface {
    
        String setPassword(in String password);
    
    }
    
以上这两个接口对应两个不同的业务逻辑，我们再添加一个aidl接口用于实现对不同连接的Binder的查找。

    // IBinderInterface.aidl
    package com.example.houyafei.appaidl;
    
    // Declare any non-default types here with import statements
    
    interface IBinderInterface {
    
        IBinder queryBinder(in int requestCode);
    
    }
    
## 4 创建Binder的连接池

注：由于Android四大组件分别运行在不同的进程中，在同一个应用中实现不同进程的通信。
这里通过单例模式创建一个BinderPool类。在该类中运行过程如下：

> 1: 通过单例模式创建该对象；
> 2: 创建ServiceConnection属性；
> 3: 对象创建时绑定服务；
> 4: 在ServiceConnection中，获取IBinderInterface对象；
> 5: 提供一个方法供用户调用获取IBinderInterface对象中的方法；
> 6: 至此需要的部分基本完成，但为了线程的安全，将异步转化为同步，做了如下操作：

>>  1: 实例化时，进行线程同步设置；
    2: 绑定服务时，设置Binder断开的监听；
    3: 创建Binder监听器，当Binder断开时，重新连接服务。

    //BinderPool.java文件
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


## 5 创建服务

在该服务中，我将所有的接口实现部分单独放在一个文件中，所有的aidl接口文件都要继承相对应的一个Stub
从而实现其内部方法。在Service中只要实现其OnBind方法即可，当然需要获取一个IBinder属性。

    //MyService.java
    package com.example.houyafei.appaidl.binderService;
    
    import android.app.Service;
    import android.content.Intent;
    import android.os.IBinder;
    import android.util.Log;
    
    import com.example.houyafei.appaidl.binderService.InterfaceStubUtils.IBinderInterfaceImpl;
    
    public class MyService extends Service {
    
        private static final String TAG = "main";
        
        //这一句很重要
        private IBinder iBinder = new IBinderInterfaceImpl();

        public MyService() {
        }
    
        @Override
        public IBinder onBind(Intent intent) {
            Log.e(TAG,"myservice onBind()");
            return iBinder ;
        }
    }
    
所有的接口实现放在一个类中，项目扩展只需要将aidl的实现添加到这个类中即可。

    package com.example.houyafei.appaidl.binderService;
    
    import android.os.IBinder;
    import android.os.RemoteException;
    import android.util.Log;
    
    import com.example.houyafei.appaidl.IBinderInterface;
    import com.example.houyafei.appaidl.IComputeInterface;
    import com.example.houyafei.appaidl.ISecurityInterface;
    
    /**
     * Created by Hou Yafei on 2017/7/14.
     */
    public class InterfaceStubUtils {
    
        private static final String TAG = "main";
        /**
         * 每个aidl接口对应一个requestCode
         */
        public static final int REQUEST_COMPUTE = 0x909010;
        public static final int REQUEST_SECRITY = 0x909011;
    
        /**
         * 对应的每个aidl接口对应一个requestCode都要在这里被分配到不同的Binder对象
         * 该类提供给Myservice.java的，
         */
        public static class IBinderInterfaceImpl extends IBinderInterface.Stub{
    
            public IBinderInterfaceImpl() {
            }
    
            @Override
            public IBinder queryBinder(int requestCode) throws RemoteException {
                IBinder iBinder = null ;
    
                switch(requestCode){
                    case REQUEST_COMPUTE:
                        Log.e(TAG,"queryBinder( )");
                        iBinder = new InterfaceStubUtils.IComputeInterfaceImpl();
    
                        break;
                    case REQUEST_SECRITY:
    
                        iBinder = new InterfaceStubUtils.ISecurityInterfaceImpl();
    
                        break ;
                }
    
                return iBinder;
            }
        }
    
        /**
         * IComputeInterface接口实现
         */
        private  static class IComputeInterfaceImpl extends IComputeInterface.Stub{
            
            @Override
            public int compute(int a, int b) throws RemoteException {
                Log.e(TAG,"compute(int a, int b)");
                return a * b;
            }
        }
    
        /**
         * ISecurityInterface接口实现
         */
        private static class ISecurityInterfaceImpl extends ISecurityInterface.Stub{
    
            @Override
            public String setPassword(String password) throws RemoteException {
                return "Password : " + password;
            }
        }
    
    
    }

    
 
 

