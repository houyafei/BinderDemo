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
