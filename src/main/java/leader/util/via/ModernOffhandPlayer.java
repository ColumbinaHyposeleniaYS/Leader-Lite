// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

/**
 * Client-side animation state for the modern offhand.
 */
public interface ModernOffhandPlayer {

    void swingOffhand();

    float getOffhandSwingProgress(float partialTicks);
}
