package android.view;

public abstract class DisplayAddress {
    
    public static final class Physical extends DisplayAddress {
        
        public long getPhysicalDisplayId() {
            throw new RuntimeException("STUB");
        }
        
    }
    
    public static final class StablePhysical extends DisplayAddress {
        
        public long getPhysicalDisplayId() {
            throw new RuntimeException("STUB");
        }
        
    }
    
}
