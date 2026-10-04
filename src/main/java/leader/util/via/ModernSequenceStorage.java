// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

import com.viaversion.viaversion.api.connection.StorableObject;

public final class ModernSequenceStorage implements StorableObject {

    private int sequence;

    public synchronized int next() {
        if (sequence == Integer.MAX_VALUE) {
            sequence = 0;
        }
        return ++sequence;
    }

    public synchronized void reset() {
        sequence = 0;
    }
}
