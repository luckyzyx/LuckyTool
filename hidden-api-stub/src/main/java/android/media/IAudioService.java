package android.media;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

public interface IAudioService extends IInterface {
    
    abstract class Stub extends Binder implements IAudioService {
        
        public static IAudioService asInterface(IBinder iBinder) {
            throw new RuntimeException("STUB");
        }
        
    }
    
    int getRingerModeInternal() throws RemoteException;
    
    void setRingerModeInternal(int i, String str) throws RemoteException;
    
}
