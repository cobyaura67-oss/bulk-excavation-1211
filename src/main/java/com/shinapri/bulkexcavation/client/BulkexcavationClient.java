package com.shinapri.bulkexcavation.client;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.shinapri.bulkexcavation.network.SetRegionPayload;

import com.mojang.datafixers.util.Pair;
import com.shinapri.bulkexcavation.ClientSel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

import net.minecraft.client.render.*;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.util.ActionResult;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;

public class BulkexcavationClient implements ClientModInitializer {
    private static KeyBinding KEY_ACTIVE;
    private boolean complete = false;
    private boolean prevLmb = false;
    private boolean prevRmb = false;
    private static final long CLICK_COOLDOWN_MS = 120;
    private long lastClickMs = 0;

    private BlockPos oldPos = null;

    private boolean selectionMode = false;

    @Override public void onInitializeClient() {



        KEY_ACTIVE = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.excavation.pos",
                GLFW.GLFW_KEY_LEFT_ALT,
                "key.categories.excavation"
        ));

        WorldRenderEvents.AFTER_TRANSLUCENT.register(this::renderSelectionOutline);

        // 2) Cancel right-click on blocks (placement, block use)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            return selectionMode ? ActionResult.FAIL : ActionResult.PASS;
        });

        // 3) Cancel right-click in air / item use (bucket, ender pearl, etc.)
        UseItemCallback.EVENT.register((player, world, hand) -> {
            return selectionMode ? ActionResult.FAIL : ActionResult.PASS;
        });

        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                selectionMode ? ActionResult.FAIL : ActionResult.PASS
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client == null || client.world == null || client.isPaused() || client.currentScreen != null) return;
            //BlockPos oldPos = null;
            var cfg       = com.shinapri.bulkexcavation.config.ExcavationConfigIO.get();
            boolean enablePreview = cfg.preview;
            int maxVolume = cfg.maxVolume;
            boolean enableRemote = cfg.remoteExcavation;

            long window = client.getWindow().getHandle();

            boolean altHeld = KEY_ACTIVE.isPressed() || InputUtil.isKeyPressed(window, GLFW.GLFW_KEY_LEFT_ALT);
            selectionMode = altHeld;

            boolean lmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
            boolean rmb = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS;

            boolean risingEdge = lmb && !prevLmb;
            boolean rmbClicked = rmb && !prevRmb;
            prevLmb = lmb;
            prevRmb = rmb;

            if (!(altHeld && (risingEdge || rmbClicked))) return;
            long now = net.minecraft.util.Util.getMeasuringTimeMs();
            if (now - lastClickMs < CLICK_COOLDOWN_MS) return;
            lastClickMs = now;

            if(rmbClicked) {
                if(oldPos != null){
                    ClientSel.consume();
                    client.inGameHud.setOverlayMessage(
                            Text.literal("[Excavation] canceled").formatted(Formatting.RED), false
                    );
                    oldPos = null;
                }
                return;
            }

            MinecraftClient mc = MinecraftClient.getInstance();
            ClientWorld world = mc.world;
            if (world == null) return;

            BlockPos hit = getLookedBlock(client);
            if (hit == null) return;

            BlockState state   = world.getBlockState(hit);
            /*
            if (client.options != null) {
                client.options.attackKey.setPressed(false);
            }
            if (client.interactionManager != null) {
                client.interactionManager.cancelBlockBreaking(); // Yarn 1.21.x
            }*/

            if(!state.isAir()){
                complete = ClientSel.push(hit);
            }
            if (complete) {
                BlockPos a = ClientSel.pos1, b = ClientSel.pos2;
                if (a == null || b == null) return;

                int minX = Math.min(a.getX(), b.getX());
                int minY = Math.min(a.getY(), b.getY());
                int minZ = Math.min(a.getZ(), b.getZ());
                int maxX = Math.max(a.getX(), b.getX());
                int maxY = Math.max(a.getY(), b.getY());
                int maxZ = Math.max(a.getZ(), b.getZ());

                int volume = (maxX-minX+1)*(maxY-minY+1)*(maxZ-minZ+1);

                if(!enablePreview){
                    if (volume > maxVolume) {
                        client.inGameHud.setOverlayMessage(
                                Text.literal("[Excavation] preview mode volume " + Integer.toString(volume) + ", max volume exceeded.").formatted(Formatting.RED), false
                        );
                        ClientSel.consume();
                        oldPos = null;
                    }else{
                        Pair<BlockPos, BlockPos> s = ClientSel.consume();
                        ClientPlayNetworking.send(new SetRegionPayload(s.getFirst(), s.getSecond()));
                    }
                }else{
                    if (volume > maxVolume) {
                        client.inGameHud.setOverlayMessage(
                                Text.literal("[Excavation] preview mode volume " + Integer.toString(volume) + ", max volume exceeded.").formatted(Formatting.RED), false
                        );
                        oldPos = null;
                        return;
                    }
                    if(enableRemote && state.isAir()){
                        Pair<BlockPos, BlockPos> s = ClientSel.consume();
                        ClientPlayNetworking.send(new SetRegionPayload(s.getFirst(), s.getSecond()));
                        oldPos = null;
                    }else{
                        if(!state.isAir()){
                            if(java.util.Objects.equals(oldPos, b)){
                                Pair<BlockPos, BlockPos> s = ClientSel.consume();
                                ClientPlayNetworking.send(new SetRegionPayload(s.getFirst(), s.getSecond()));
                            }else{
                                client.inGameHud.setOverlayMessage(
                                        Text.literal("[Excavation] preview mode volume " + Integer.toString(volume)).formatted(Formatting.GREEN), false
                                );
                            }
                            oldPos = b;
                        }
                    }

                }
            } else {
                if(ClientSel.pos2 != null) {
                    Pair<BlockPos, BlockPos> s = ClientSel.consume();
                }
                client.inGameHud.setOverlayMessage(
                        Text.literal("[Excavation] " + hit.toShortString()).formatted(Formatting.GREEN),
                        false
                );
            }
        });

    }

    private static BlockPos getLookedBlock(MinecraftClient client) {
        HitResult target = client.crosshairTarget;
        if (!(target instanceof BlockHitResult bhr)) return null;
        return bhr.getBlockPos();
    }

    private void renderSelectionOutline(WorldRenderContext ctx) {
        if (ctx.world() == null || ctx.camera() == null || ctx.consumers() == null) return;

        BlockPos p1 = ClientSel.pos1, p2 = ClientSel.pos2;
        if (p1 == null || p2 == null) return;

        int minX = Math.min(p1.getX(), p2.getX());
        int minY = Math.min(p1.getY(), p2.getY());
        int minZ = Math.min(p1.getZ(), p2.getZ());
        int maxX = Math.max(p1.getX(), p2.getX()) + 1;
        int maxY = Math.max(p1.getY(), p2.getY()) + 1;
        int maxZ = Math.max(p1.getZ(), p2.getZ()) + 1;

        final double eps = 1e-3;
        double x1 = minX - eps, y1 = minY - eps, z1 = minZ - eps;
        double x2 = maxX + eps, y2 = maxY + eps, z2 = maxZ + eps;

        MatrixStack ms = ctx.matrixStack();
        Vec3d cam = ctx.camera().getPos();
        ms.push();
        ms.translate(-cam.x, -cam.y, -cam.z);

        // outline (unchanged)
        VertexConsumer lineVC = ctx.consumers().getBuffer(RenderLayer.getLines());
        drawEdgesBox(ms, lineVC, x1,y1,z1, x2,y2,z2, 0.2f,0.8f,1f, 1f);

        // translucent faces — use built-in POSITION_COLOR quads
        VertexConsumer faceVC = ctx.consumers().getBuffer(RenderLayer.getDebugQuads());
        fillFaces(ms, faceVC, x1,y1,z1, x2,y2,z2, 0.2f,0.8f,1f, 0.25f);

        ms.pop();
    }

    private static void fillFaces(MatrixStack ms, VertexConsumer vc,
                                  double x1,double y1,double z1, double x2,double y2,double z2,
                                  float r,float g,float b,float a) {
        MatrixStack.Entry e = ms.peek();

        // +X (ขวา)
        quad(e, vc, x2,y1,z1,  x2,y1,z2,  x2,y2,z2,  x2,y2,z1,  1,0,0,  r,g,b,a);
        // -X (ซ้าย)
        quad(e, vc, x1,y1,z2,  x1,y1,z1,  x1,y2,z1,  x1,y2,z2, -1,0,0,  r,g,b,a);
        // +Y (บน)
        quad(e, vc, x1,y2,z1,  x2,y2,z1,  x2,y2,z2,  x1,y2,z2,  0,1,0,  r,g,b,a);
        // -Y (ล่าง)
        quad(e, vc, x1,y1,z2,  x2,y1,z2,  x2,y1,z1,  x1,y1,z1,  0,-1,0, r,g,b,a);
        // +Z (หน้า)
        quad(e, vc, x1,y1,z2,  x1,y2,z2,  x2,y2,z2,  x2,y1,z2,  0,0,1,  r,g,b,a);
        // -Z (หลัง)
        quad(e, vc, x2,y1,z1,  x2,y2,z1,  x1,y2,z1,  x1,y1,z1,  0,0,-1, r,g,b,a);
    }

    private static void quad(MatrixStack.Entry e, VertexConsumer vc,
                             double x1,double y1,double z1, double x2,double y2,double z2,
                             double x3,double y3,double z3, double x4,double y4,double z4,
                             float nx,float ny,float nz, float r,float g,float b,float a) {

        vc.vertex(e, (float)x1,(float)y1,(float)z1).color(r,g,b,a).light(0xF000F0).normal(e,nx,ny,nz);
        vc.vertex(e, (float)x2,(float)y2,(float)z2).color(r,g,b,a).light(0xF000F0).normal(e,nx,ny,nz);
        vc.vertex(e, (float)x3,(float)y3,(float)z3).color(r,g,b,a).light(0xF000F0).normal(e,nx,ny,nz);
        vc.vertex(e, (float)x4,(float)y4,(float)z4).color(r,g,b,a).light(0xF000F0).normal(e,nx,ny,nz);
    }

    private static void drawEdgesBox(MatrixStack ms, VertexConsumer vc,
                                     double minX, double minY, double minZ,
                                     double maxX, double maxY, double maxZ,
                                     float r, float g, float b, float a) {
        // ฐานล่าง
        line(ms, vc, minX, minY, minZ, maxX, minY, minZ, r,g,b,a);
        line(ms, vc, maxX, minY, minZ, maxX, minY, maxZ, r,g,b,a);
        line(ms, vc, maxX, minY, maxZ, minX, minY, maxZ, r,g,b,a);
        line(ms, vc, minX, minY, maxZ, minX, minY, minZ, r,g,b,a);

        // ฐานบน
        line(ms, vc, minX, maxY, minZ, maxX, maxY, minZ, r,g,b,a);
        line(ms, vc, maxX, maxY, minZ, maxX, maxY, maxZ, r,g,b,a);
        line(ms, vc, maxX, maxY, maxZ, minX, maxY, maxZ, r,g,b,a);
        line(ms, vc, minX, maxY, maxZ, minX, maxY, minZ, r,g,b,a);

        // เสาตั้ง
        line(ms, vc, minX, minY, minZ, minX, maxY, minZ, r,g,b,a);
        line(ms, vc, maxX, minY, minZ, maxX, maxY, minZ, r,g,b,a);
        line(ms, vc, maxX, minY, maxZ, maxX, maxY, maxZ, r,g,b,a);
        line(ms, vc, minX, minY, maxZ, minX, maxY, maxZ, r,g,b,a);
    }

    private static void line(MatrixStack ms, VertexConsumer vc,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             float r, float g, float b, float a) {
        MatrixStack.Entry e = ms.peek();
        vc.vertex(e, (float)x1, (float)y1, (float)z1).color(r, g, b, a).normal(e, 0, 1, 0);
        vc.vertex(e, (float)x2, (float)y2, (float)z2).color(r, g, b, a).normal(e, 0, 1, 0);
    }


}
