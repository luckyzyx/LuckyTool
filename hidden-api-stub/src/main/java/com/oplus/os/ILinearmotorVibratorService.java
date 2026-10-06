package com.oplus.os;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;

public interface ILinearmotorVibratorService extends IInterface {
    
    abstract class Stub extends Binder implements ILinearmotorVibratorService {
        
        public static ILinearmotorVibratorService asInterface(IBinder obj) {
            throw new RuntimeException("STUB");
        }
        
    }
    
}
