package cn.unfair.util.via;

/**
 * Leader-Lite port helper: Unfair adds {@code swingClientSide()} to
 * EntityLivingBase sources; here it is exposed through a mixin interface.
 */
public interface ModernSwing {
    void swingClientSide();
}
