package leader.module.modules.render;

import leader.module.Module;
import leader.property.properties.ModeProperty;
import leader.ui.ClickGui;
import leader.ui.ListClickGui;
import leader.ui.clickgui.augustus.AugustusClickGui;
import net.minecraft.client.Minecraft;
import org.lwjgl.input.Keyboard;

public class GuiModule extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    public final ModeProperty style = new ModeProperty("Style", 0, new String[]{"Window", "List"});
    public final ModeProperty design = new ModeProperty("Design", 0, new String[]{"Leader", "Augustus"});
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
        if (this.design.getValue() == 1) {
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
