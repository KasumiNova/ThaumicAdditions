package com.endie.thaumicadditions.client.tesr;

import com.endie.thaumicadditions.InfoTAR;
import com.endie.thaumicadditions.TAReconstructed;
import com.endie.thaumicadditions.client.util.tesseract.Tesseract;
import com.endie.thaumicadditions.tiles.TileEntitySummoner;
import com.pengu.hammercore.annotations.AtTESR;
import com.pengu.hammercore.client.render.tesr.TESR;
import com.pengu.hammercore.client.utils.UtilsFX;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

@AtTESR(TileEntitySummoner.class)
public class TESRMobSummoner extends TESR<TileEntitySummoner>
{
	@Override
	public void renderTileEntityAt(TileEntitySummoner te, double x, double y, double z, float partialTicks, ResourceLocation destroyStage, float alpha)
	{
		GlStateManager.pushMatrix();
		GlStateManager.translate((float) x + .5F, (float) y, (float) z + .5F);
		TAReconstructed.proxy.getFX().renderMob(te.getCachedEntity(), te.rotator, x, y, z, partialTicks);
		GlStateManager.translate(0, .5F, 0);
		double scale = Math.toRadians(te.rotator.getActualRotation(partialTicks) / 2);
		GlStateManager.scale(.3, .3, .3);
		GlStateManager.disableLighting();
		UtilsFX.bindTexture("thaumcraft", "textures/blocks/metal_thaumium.png");
		Tesseract.INSTANCE.draw(0xFFFFFFFF, scale);
		GlStateManager.enableLighting();
		GlStateManager.popMatrix();
	}
}