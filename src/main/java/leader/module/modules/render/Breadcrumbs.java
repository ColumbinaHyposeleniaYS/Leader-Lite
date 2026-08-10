package leader.module.modules.render;

import leader.Leader;
import leader.event.EventTarget;
import leader.events.Render3DEvent;
import leader.mixin.IAccessorRenderManager;
import leader.module.Module;
import leader.property.properties.IntProperty;
import leader.property.properties.ModeProperty;
import leader.util.ColorUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import org.lwjgl.opengl.GL11;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Breadcrumbs extends Module {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private final ModeProperty mode = new ModeProperty("Mode", 1, new String[]{"FDP", "Novoline"});
    private final IntProperty size = new IntProperty("Size", 100, 10, 500);
    private final List<Vec3> path = new ArrayList<>();
    private final Map<EntityPlayer, List<Point>> playerPoints = new HashMap<>();

    public Breadcrumbs() {
        super("Breadcrumbs", false);
    }

    @EventTarget
    public void onRender3D(Render3DEvent event) {
        if (!isEnabled())return;

        if (mode.getValue() == 0) {
            if (mc.thePlayer.lastTickPosX != mc.thePlayer.posX || mc.thePlayer.lastTickPosY != mc.thePlayer.posY || mc.thePlayer.lastTickPosZ != mc.thePlayer.posZ) {
                path.add(new Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ));
            }
            while (path.size() > size.getValue()) {
                path.remove(0);
            }
            HUD hud = (HUD) Leader.moduleManager.modules.get(HUD.class);
            Color c1 = hud.getColor(System.currentTimeMillis());
            Color c2 = hud.getColor(System.currentTimeMillis() + 500);
            renderBreadCrumbs(path, c1, c2);
        } else {
            playerPoints.entrySet().removeIf(entry -> entry.getKey().isDead || !mc.theWorld.playerEntities.contains(entry.getKey()));
            for (EntityPlayer entityPlayer : mc.theWorld.playerEntities) {
                List<Point> points = playerPoints.computeIfAbsent(entityPlayer, k -> new ArrayList<>());
                boolean render = entityPlayer != mc.thePlayer || mc.gameSettings.thirdPersonView != 0;
                points.removeIf(p -> p.age >= size.getValue());
                double x = entityPlayer.lastTickPosX + (entityPlayer.posX - entityPlayer.lastTickPosX) * (double) event.getPartialTicks();
                double y = entityPlayer.lastTickPosY + (entityPlayer.posY - entityPlayer.lastTickPosY) * (double) event.getPartialTicks();
                double z = entityPlayer.lastTickPosZ + (entityPlayer.posZ - entityPlayer.lastTickPosZ) * (double) event.getPartialTicks();
                points.add(new Point(x, y, z));
                if (render) {
                    GL11.glPushMatrix();
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_LINE_SMOOTH);
                    GL11.glDisable(GL11.GL_TEXTURE_2D);
                    GL11.glBlendFunc(770, 771);
                    GL11.glDisable(GL11.GL_CULL_FACE);
                }
                HUD hud = (HUD) Leader.moduleManager.modules.get(HUD.class);
                Color c1 = hud.getColor(System.currentTimeMillis());
                Color c2 = hud.getColor(System.currentTimeMillis() + 500);
                for (int i = 0; i < points.size(); i++) {
                    if (i >= points.size() - 1) continue;
                    Point t = points.get(i);
                    Point temp = points.get(i + 1);
                    int a = (int) (200.0F * i / (float) points.size());
                    if (render) {
                        Color base = ColorUtil.interpolate((float) (i % 10) / 10.0F, c1, c2);
                        int r = base.getRed(), gb = base.getGreen(), b = base.getBlue();
                        double x2 = t.x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                        double y2 = t.y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
                        double z2 = t.z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                        double x1 = temp.x - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
                        double y1 = temp.y - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
                        double z1 = temp.z - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
                        GL11.glBegin(GL11.GL_QUAD_STRIP);
                        GL11.glColor4f(r / 255.0F, gb / 255.0F, b / 255.0F, 0.0F);
                        GL11.glVertex3d(x2, y2 + entityPlayer.height - 0.1, z2);
                        GL11.glColor4f(r / 255.0F, gb / 255.0F, b / 255.0F, a / 255.0F);
                        GL11.glVertex3d(x2, y2 + 0.2, z2);
                        GL11.glVertex3d(x1, y1 + entityPlayer.height - 0.1, z1);
                        GL11.glVertex3d(x1, y1 + 0.2, z1);
                        GL11.glEnd();
                    }
                    t.age++;
                }
                if (render) {
                    GlStateManager.resetColor();
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                    GL11.glEnable(GL11.GL_TEXTURE_2D);
                    GL11.glEnable(GL11.GL_CULL_FACE);
                    GL11.glDisable(GL11.GL_LINE_SMOOTH);
                    GL11.glDisable(GL11.GL_BLEND);
                    GL11.glPopMatrix();
                }
            }
        }
    }

    private void renderBreadCrumbs(List<Vec3> vec3s, Color color, Color color2) {
        GlStateManager.disableDepth();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glLineWidth(2.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        for (int i = 0; i < vec3s.size(); i++) {
            Vec3 v = vec3s.get(i);
            double rx = v.xCoord - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosX();
            double ry = v.yCoord - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosY();
            double rz = v.zCoord - ((IAccessorRenderManager) mc.getRenderManager()).getRenderPosZ();
            Color c = ColorUtil.interpolate((float) i / (float) vec3s.size(), color, color2);
            int a = (int) (200.0F * i / (float) vec3s.size());
            GL11.glColor4f(c.getRed() / 255.0F, c.getGreen() / 255.0F, c.getBlue() / 255.0F, a / 255.0F);
            GL11.glVertex3d(rx, ry, rz);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.enableDepth();
        GlStateManager.resetColor();
    }

    private static class Point {
        public final double x, y, z;
        public float age;

        public Point(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }
}
