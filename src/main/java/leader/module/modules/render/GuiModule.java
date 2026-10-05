package leader.module.modules.render;

import leader.module.Module;
import leader.property.properties.BooleanProperty;
import leader.property.properties.ModeProperty;
import leader.property.properties.PercentProperty;
import leader.ui.ClickGui;
import leader.ui.ListClickGui;
import leader.ui.clickgui.augustus.AugustusClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    // Style unifies the old Window/List switch with the Augustus design (which used to
    // live under a separate "Design" property, mismatching the upstream Unfair layout).
    public final ModeProperty style = new ModeProperty("Style", 0, new String[]{"Window", "List", "Augustus"});
    // Augustus-only knobs: window background opacity plus the two post-process effects.
    public final PercentProperty opacity = new PercentProperty("Opacity", 85, () -> this.style.getValue() == 2);
    public final BooleanProperty blur = new BooleanProperty("Blur", true, () -> this.style.getValue() == 2);
    public final BooleanProperty bloom = new BooleanProperty("Bloom", true, () -> this.style.getValue() == 2);
    private ClickGui clickGui;
    private ListClickGui listClickGui;
    private AugustusClickGui augustusClickGui;

    public GuiModule() {
        super("ClickGui", false);
        setKey(Keyboard.KEY_RSHIFT);
    }

    @Override
    public void onEnabled() {
        setEnabled(false);
        if (this.style.getValue() == 2) {
            if (augustusClickGui == null) augustusClickGui = new AugustusClickGui();
            mc.displayGuiScreen(augustusClickGui);
        } else if (this.style.getValue() == 1) {
            if (listClickGui == null) listClickGui = new ListClickGui();
            mc.displayGuiScreen(listClickGui);
        } else {
            if (clickGui == null) clickGui = new ClickGui();
            mc.displayGuiScreen(clickGui);
        }
    }
}
