// Ported from the Unfair client (https://github.com/UnfairGaming/Unfair)
// Original location: cn.unfair.util.via - adapted to leader.util.via for Leader-Lite.
package leader.util.via;

/**
 * Leader-Lite port helper: Unfair adds {@code swingClientSide()} to
 * EntityLivingBase sources; here it is exposed through a mixin interface.
 */
public interface ModernSwing {
    void swingClientSide();
}
