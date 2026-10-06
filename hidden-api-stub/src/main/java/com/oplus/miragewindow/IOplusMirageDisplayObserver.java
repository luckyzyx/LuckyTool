package com.oplus.miragewindow;

import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IOplusMirageDisplayObserver extends IInterface {
    
    abstract class Stub extends Binder implements IOplusMirageDisplayObserver {
        
        public static IOplusMirageDisplayObserver asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
        
    }
    
    void onMirageDisplayCastFailed(int i) throws RemoteException;
    
    void onMirageDisplayCastSuccess(OplusMirageDisplayCastInfo oplusMirageDisplayCastInfo, int i) throws RemoteException;
    
    void onMirageDisplayConfigChanged(OplusMirageDisplayCastInfo oplusMirageDisplayCastInfo, int i) throws RemoteException;
    
    void onMirageDisplayExit(int i) throws RemoteException;
    
    void onMirageDisplayToastEvent(int i, int i2, Bundle bundle) throws RemoteException;
    
    void onMirageDisplayTopActivityUidChanged(int i, int i2) throws RemoteException;
    
}
