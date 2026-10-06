package com.android.internal.telephony;

import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.DeprecatedSinceApi;
import androidx.annotation.RequiresApi;

public interface ITelephony {
    
    abstract class Stub extends Binder implements ITelephony {
        
        public static ITelephony asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
        
    }
    
    long getAllowedNetworkTypesForReason(int i, int i2) throws RemoteException;
    
    @DeprecatedSinceApi(api = Build.VERSION_CODES.CINNAMON_BUN)
    boolean setAllowedNetworkTypesForReason(int i, int i2, long j) throws RemoteException;
    
    @RequiresApi(api = Build.VERSION_CODES.CINNAMON_BUN)
    boolean setAllowedNetworkTypesForReason(int i, int i2, long j, String str) throws RemoteException;
    
    @DeprecatedSinceApi(api = Build.VERSION_CODES.S, message = "该方法仅在Android12之前存在!")
    int getPreferredNetworkType(int i) throws RemoteException;
    
    @DeprecatedSinceApi(api = Build.VERSION_CODES.S, message = "该方法仅在Android12之前存在!")
    boolean setPreferredNetworkType(int i, int i2) throws RemoteException;
    
}
