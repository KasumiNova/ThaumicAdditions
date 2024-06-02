package org.zeith.thaumicadditions.proxy;

import com.google.common.base.Predicates;
import com.zeitheron.hammercore.api.lighting.*;
import com.zeitheron.hammercore.client.render.item.ItemRenderingHandler;
import com.zeitheron.hammercore.client.utils.UtilsFX;
import com.zeitheron.hammercore.internal.blocks.base.IBlockHorizontal;
import com.zeitheron.hammercore.internal.blocks.base.IBlockOrientable;
import com.zeitheron.hammercore.proxy.RenderProxy_Client;
import com.zeitheron.hammercore.utils.color.ColorHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMap.Builder;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.gen.NoiseGeneratorSimplex;
import net.minecraftforge.client.event.RenderSpecificHandEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.event.TextureStitchEvent.Pre;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.client.model.obj.OBJLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fluids.BlockFluidBase;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.client.registry.RenderingRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import org.zeith.thaumicadditions.InfoTAR;
import org.zeith.thaumicadditions.TAReconstructed;
import org.zeith.thaumicadditions.api.AspectUtil;
import org.zeith.thaumicadditions.api.EdibleAspect;
import org.zeith.thaumicadditions.api.animator.BaseItemAnimator;
import org.zeith.thaumicadditions.api.animator.IAnimatableItem;
import org.zeith.thaumicadditions.blocks.BlockAbstractEssentiaJar.BlockAbstractJarItem;
import org.zeith.thaumicadditions.blocks.BlockCrystal;
import org.zeith.thaumicadditions.blocks.decor.BlockCrystalLamp;
import org.zeith.thaumicadditions.blocks.plants.BlockVisCrop;
import org.zeith.thaumicadditions.client.isr.ItemRenderJar;
import org.zeith.thaumicadditions.client.models.baked.BakedCropModel;
import org.zeith.thaumicadditions.client.render.block.statemap.LambdaStateMapper;
import org.zeith.thaumicadditions.client.render.entity.*;
import org.zeith.thaumicadditions.client.render.tile.*;
import org.zeith.thaumicadditions.client.texture.TextureThaumonomiconBG;
import org.zeith.thaumicadditions.compat.ITARC;
import org.zeith.thaumicadditions.entity.*;
import org.zeith.thaumicadditions.events.ClientEventReactor;
import org.zeith.thaumicadditions.init.BlocksTAR;
import org.zeith.thaumicadditions.init.ItemsTAR;
import org.zeith.thaumicadditions.inventory.gui.GuiSealGlobe;
import org.zeith.thaumicadditions.items.ItemSealSymbol;
import org.zeith.thaumicadditions.items.ItemVisPod;
import org.zeith.thaumicadditions.items.seed.ItemVisSeeds;
import org.zeith.thaumicadditions.items.weapons.ItemEssentiaPistol;
import org.zeith.thaumicadditions.items.weapons.ItemEssentiaPistol.ItemRendererEssentiaPistol;
import org.zeith.thaumicadditions.items.weapons.ItemShadowBeamStaff;
import org.zeith.thaumicadditions.proxy.fx.FXHandler;
import org.zeith.thaumicadditions.proxy.fx.FXHandlerClient;
import org.zeith.thaumicadditions.tiles.*;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.blocks.essentia.BlockJarItem;
import thaumcraft.common.tiles.crafting.TileInfusionMatrix;
import thaumcraft.common.tiles.devices.TileMirror;
import thaumcraft.common.tiles.devices.TileMirrorEssentia;
import thaumcraft.common.tiles.misc.TileHole;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class ClientProxy
		extends CommonProxy
{
	public static final NoiseGeneratorSimplex BASE_SIMPLEX = new NoiseGeneratorSimplex();
	private final List<Vec3d> shadowPositions = new ArrayList<>();

	private static void mapFluid(BlockFluidBase fluidBlock)
	{
		final Item item = Item.getItemFromBlock(fluidBlock);
		assert item != null;
		ModelBakery.registerItemVariants(item);
		ModelResourceLocation modelResourceLocation = new ModelResourceLocation(InfoTAR.MOD_ID + ":fluid", fluidBlock.getFluid().getName());
		ModelLoader.setCustomMeshDefinition(item, stack -> modelResourceLocation);
		ModelLoader.setCustomStateMapper(fluidBlock, new LambdaStateMapper(state -> modelResourceLocation));
	}

	@Nonnull
	public static TextureAtlasSprite getSprite(String path)
	{
		TextureMap m = Minecraft.getMinecraft().getTextureMapBlocks();
		TextureAtlasSprite s = m.getTextureExtry(path);
		if(s == null)
			s = m.getAtlasSprite(path);
		return s != null ? s : m.getMissingSprite();
	}

	@Override
	public void preInit()
	{
		ModelLoader.setCustomStateMapper(BlocksTAR.CRYSTAL_WATER, new Builder().ignore(BlockFluidBase.LEVEL).build());
		ModelLoader.setCustomStateMapper(BlocksTAR.ASPECT_COMBINER, new Builder().ignore(IBlockHorizontal.FACING).build());
		ModelLoader.setCustomStateMapper(BlocksTAR.CRYSTAL_BORE, new Builder().ignore(IBlockOrientable.FACING).build());

		RenderingRegistry.registerEntityRenderingHandler(EntityChester.class, RenderChester::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityEssentiaShot.class, RenderEssentiaShot::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityBlueWolf.class, RenderBlueWolf::new);
		RenderingRegistry.registerEntityRenderingHandler(EntityMithminiteScythe.class, RenderMithminiteScythe::new);

		OBJLoader.INSTANCE.addDomain(InfoTAR.MOD_ID);
	}

	@Override
	public void init()
	{
		MinecraftForge.EVENT_BUS.register(ClientEventReactor.REACTOR);

		for(ITARC a : TAReconstructed.arcs)
			a.initClient();

		// Assign custom texture
		Minecraft.getMinecraft().getTextureManager().loadTickableTexture(TEXTURE_THAUMONOMICON_BG, new TextureThaumonomiconBG());

		// Adding custom color handlers
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemsTAR.SALT_ESSENCE::getItemColor, ItemsTAR.SALT_ESSENCE);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemVisPod::getColor, ItemsTAR.VIS_POD);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemVisSeeds::getColor, ItemsTAR.VIS_SEEDS);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, layer) ->
		{
			if(layer == 1)
			{
				AspectList al = EdibleAspect.getSalt(stack);
				return al.visSize() > 0 ? AspectUtil.getColor(al, true) : 0xFF0000;
			}
			return 0xFFFFFF;
		}, BlocksTAR.CAKE);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemsTAR.ENTITY_CELL::getColor, ItemsTAR.ENTITY_CELL);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler(BlocksTAR.CRYSTAL_BLOCK::getColor, BlocksTAR.CRYSTAL_BLOCK.getItemBlock());
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, index) ->
		{
			Aspect a;
			return index == 0 && (a = ItemSealSymbol.getAspect(stack)) != null ? a.getColor() : 0xFFFFFF;
		}, ItemsTAR.SEAL_SYMBOL);
		Minecraft.getMinecraft().getItemColors().registerItemColorHandler((stack, layer) ->
		{
			if(layer == 1)
			{
				if(stack.hasTagCompound())
				{
					int[] rgb = stack.getTagCompound().getIntArray("RGB");
					if(rgb.length >= 3)
						return rgb[0] << 16 | rgb[1] << 8 | rgb[2];
				}

				return 0xFF0000;
			}

			return 0xFFFFFF;
		}, Item.getItemFromBlock(BlocksTAR.SEAL));
		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(BlockCrystal::getColor, BlocksTAR.CRYSTAL_BLOCK);
		Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(BlockCrystalLamp::getColor, BlocksTAR.CRYSTAL_LAMP);

		for(BlockVisCrop blk : BlocksTAR.VIS_CROPS.values())
		{
			Minecraft.getMinecraft().getBlockColors().registerBlockColorHandler(blk::getColor, blk);
			blk.getBlockState().getValidStates().forEach(state -> RenderProxy_Client.bakedModelStore.putConstant(state, new BakedCropModel(state)));

			Minecraft.getMinecraft().getRenderItem().getItemModelMesher().register(blk.seed, 0, new ModelResourceLocation(new ResourceLocation(ItemsTAR.VIS_SEEDS.getTranslationKey().substring(5)), "inventory"));
			Minecraft.getMinecraft().getItemColors().registerItemColorHandler(ItemVisSeeds::getColor, blk.seed);
		}

		LightingBlacklist.registerShadedTile(TileHole.class);
		LightingBlacklist.registerShadedTile(TileMirror.class);
		LightingBlacklist.registerShadedTile(TileMirrorEssentia.class);
		ColoredLightManager.addGenerator(partialTicks ->
		{
			EntityPlayer player = ColoredLightManager.getClientPlayer();
			if(player != null)
			{
				return player.world.tickableTileEntities.stream().filter(Predicates.instanceOf(TileInfusionMatrix.class)).map(te ->
				{
					TileInfusionMatrix im = (TileInfusionMatrix) te;
					if(im.active)
					{
						float mod = im.crafting ? 1F : 0.5F;
						float rad = (float) (BASE_SIMPLEX.getValue(im.count / 128F, 0) + 1.5F) * 5F;
						return ColoredLight.builder().pos(te.getPos()).color(mod * 1F, mod * 0.5F, mod * 1F).radius(rad).build();
					}
					return null;
				});
			}
			return Stream.empty();
		});

		// Add custom TESRs

		ClientRegistry.bindTileEntitySpecialRenderer(TileAuraDisperser.class, new TESRAuraDisperser());

		ItemRenderingHandler.INSTANCE.applyItemRender(new ItemRenderJar(), i -> i instanceof BlockAbstractJarItem || i instanceof BlockJarItem);
		ItemRenderingHandler.INSTANCE.applyItemRender(new ItemRendererEssentiaPistol(), i -> i instanceof ItemEssentiaPistol);

		TESRAspectCombiner acom = new TESRAspectCombiner();
		ClientRegistry.bindTileEntitySpecialRenderer(TileAspectCombiner.class, acom);
		ItemRenderingHandler.INSTANCE.setItemRender(Item.getItemFromBlock(BlocksTAR.ASPECT_COMBINER), acom);
		Minecraft.getMinecraft().getRenderItem().registerItem(Item.getItemFromBlock(BlocksTAR.ASPECT_COMBINER), 0, "chest");

		TESRAuraCharger cha = new TESRAuraCharger();
		ClientRegistry.bindTileEntitySpecialRenderer(TileAuraCharger.class, cha);
		ItemRenderingHandler.INSTANCE.setItemRender(Item.getItemFromBlock(BlocksTAR.AURA_CHARGER), cha);
		Minecraft.getMinecraft().getRenderItem().registerItem(Item.getItemFromBlock(BlocksTAR.AURA_CHARGER), 0, "chest");

		TESRCrystalCrusher crycr = new TESRCrystalCrusher();
		ClientRegistry.bindTileEntitySpecialRenderer(TileCrystalCrusher.class, crycr);
		ItemRenderingHandler.INSTANCE.setItemRender(Item.getItemFromBlock(BlocksTAR.CRYSTAL_CRUSHER), crycr);
		Minecraft.getMinecraft().getRenderItem().registerItem(Item.getItemFromBlock(BlocksTAR.CRYSTAL_CRUSHER), 0, "chest");

		TESRCrystalBore crybo = new TESRCrystalBore();
		ClientRegistry.bindTileEntitySpecialRenderer(TileCrystalBore.class, crybo);
		ItemRenderingHandler.INSTANCE.setItemRender(Item.getItemFromBlock(BlocksTAR.CRYSTAL_BORE), crybo);
		Minecraft.getMinecraft().getRenderItem().registerItem(Item.getItemFromBlock(BlocksTAR.CRYSTAL_BORE), 0, "chest");

		TESRFluxConcentrator fc = new TESRFluxConcentrator();
		ClientRegistry.bindTileEntitySpecialRenderer(TileFluxConcentrator.class, fc);
		ItemRenderingHandler.INSTANCE.setItemRender(Item.getItemFromBlock(BlocksTAR.FLUX_CONCENTRATOR), fc);
		Minecraft.getMinecraft().getRenderItem().registerItem(Item.getItemFromBlock(BlocksTAR.FLUX_CONCENTRATOR), 0, "chest");

		{
			ModelResourceLocation cryloc = new ModelResourceLocation(BlocksTAR.CRYSTAL_BLOCK.getRegistryName(), "normal");
			ModelLoader.setCustomStateMapper(BlocksTAR.CRYSTAL_BLOCK, new LambdaStateMapper(state -> cryloc));
		}

		// Fluid state mapping.
		mapFluid(BlocksTAR.CRYSTAL_WATER);
	}

	@Override
	public void postInit()
	{
	}

	@Override
	public int getItemColor(ItemStack stack, int layer)
	{
		return Minecraft.getMinecraft().getItemColors().colorMultiplier(stack, layer);
	}

	@Override
	protected FXHandler createFX()
	{
		return new FXHandlerClient();
	}

	@SubscribeEvent
	public void textureStitch(Pre e)
	{
		TextureMap txMap = e.getMap();

		for(String tx0 : BakedCropModel.textures0)
			txMap.registerSprite(new ResourceLocation(tx0));
		for(String tx1 : BakedCropModel.textures1)
			txMap.registerSprite(new ResourceLocation(tx1));
	}

	@SubscribeEvent
	public void onHandRender(RenderSpecificHandEvent e)
	{
		Minecraft mc = Minecraft.getMinecraft();

		EntityRenderer entityRenderer = mc.entityRenderer;
		EntityPlayerSP player = mc.player;

		BaseItemAnimator animator = null;

		float progress = e.getSwingProgress();

		boolean flag = e.getHand() == EnumHand.MAIN_HAND;
		EnumHandSide handSide = flag ? player.getPrimaryHand() : player.getPrimaryHand().opposite();

		ItemStack held = e.getItemStack();

		if(!held.isEmpty() && held.getItem() instanceof IAnimatableItem)
		{
			IAnimatableItem ai = ((IAnimatableItem) held.getItem());
			animator = ai.getAnimator(held);
			progress = ai.overrideSwing(progress, held, player, e.getPartialTicks());

			if(animator.rendersHand(player, e.getHand(), handSide))
			{
				e.setCanceled(true);
				return;
			}
		}

		{
			EnumHand ohand = e.getHand() == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
			ItemStack held2 = player.getHeldItem(ohand);

			if(ohand == EnumHand.MAIN_HAND && !held2.isEmpty()) return;

			if(!held2.isEmpty() && held2.getItem() instanceof IAnimatableItem)
			{
				IAnimatableItem ai = ((IAnimatableItem) held2.getItem());
				BaseItemAnimator animator2 = ai.getAnimator(held2);
				if(animator2 != null)
				{
					boolean isMain = flag && !player.getHeldItemMainhand().isEmpty();
					if(animator2.rendersHand(player, ohand, handSide) && (animator == null || e.getHand() == EnumHand.OFF_HAND) && !isMain)
					{
						e.setCanceled(true);
						return;
					}
				}
			}
		}

		if(animator == null) return;
		e.setCanceled(true);

		GlStateManager.pushMatrix();

		float partialTicks = e.getPartialTicks();

		if(mc.gameSettings.viewBobbing && mc.getRenderViewEntity() instanceof EntityPlayer)
		{
			EntityPlayer playerView = (EntityPlayer) mc.getRenderViewEntity();
			float f = playerView.distanceWalkedModified - playerView.prevDistanceWalkedModified;
			float f1 = -(playerView.distanceWalkedModified + f * partialTicks);
			float f2 = playerView.prevCameraYaw + (playerView.cameraYaw - playerView.prevCameraYaw) * partialTicks;
			float f3 = playerView.prevCameraPitch + (playerView.cameraPitch - playerView.prevCameraPitch) * partialTicks;
			GlStateManager.translate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0.05F, -Math.abs(MathHelper.cos(f1 * (float) Math.PI) * f2) * 0.1f, 0.0F);
			GlStateManager.rotate(MathHelper.sin(f1 * (float) Math.PI) * f2 * 0f, 0.0F, 0.0F, 1.0F);
			GlStateManager.rotate(Math.abs(MathHelper.cos(f1 * (float) Math.PI - 0.2F) * f2) * 1f, 1.0F, 0.0F, 0.0F);
			GlStateManager.rotate(f3, 1.0F, 0.0F, 0.0F);
		}

		setLightmap(player);
		GlStateManager.enableRescaleNormal();
		entityRenderer.enableLightmap();

		Render<AbstractClientPlayer> render = mc.getRenderManager().getEntityRenderObject(player);
		if(render instanceof RenderPlayer)
		{
			UtilsFX.bindTexture(player.getLocationSkin());
			RenderPlayer rp = (RenderPlayer) render;

			GlStateManager.pushMatrix();
			if(animator.transformHand(e, progress))
				rp.renderRightArm(player);
			GlStateManager.popMatrix();

			GlStateManager.pushMatrix();
			if(animator.transformHandItem(e, progress))
				mc.getRenderItem().renderItem(held, TransformType.FIRST_PERSON_RIGHT_HAND);
			GlStateManager.popMatrix();
		}

		GlStateManager.popMatrix();
	}

	private void setLightmap(EntityPlayerSP player)
	{
		int i = Minecraft.getMinecraft().world.getCombinedLight(new BlockPos(player.posX, player.posY + (double) player.getEyeHeight(), player.posZ), 0);
		float f = (float) (i & 65535);
		float f1 = (float) (i >> 16);
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
	}

	@SubscribeEvent
	public void renderLast(RenderWorldLastEvent e)
	{
		EntityPlayer player = ColoredLightManager.getClientPlayer();
		ItemStack mainhand;
		if(player != null && !(mainhand = player.getHeldItemMainhand()).isEmpty() && mainhand.getItem() instanceof ItemShadowBeamStaff)
		{
			shadowPositions.clear();
			double cx = 0, cy = 0, cz = 0;
			ItemShadowBeamStaff.recursiveLoop(player, e.getPartialTicks(), shadowPositions, 80);
			Vec3d v = shadowPositions.get(0);
			shadowPositions.set(0, new Vec3d(v.x, v.y - 0.5, v.z));

			if(Minecraft.getMinecraft().gameSettings.thirdPersonView == 0)
			{
				GlStateManager.pushMatrix();
				GlStateManager.translate(-TileEntityRendererDispatcher.staticPlayerX, -TileEntityRendererDispatcher.staticPlayerY, -TileEntityRendererDispatcher.staticPlayerZ);
				GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
				float w = GL11.glGetFloat(GL11.GL_LINE_WIDTH);
				short st = (short) GL11.glGetInteger(GL11.GL_LINE_STIPPLE);
				GL11.glLineWidth(4F);
				GL11.glLineStipple(1, (short) 0x2020);
				GL11.glEnable(GL11.GL_LINE_STIPPLE);
				GlStateManager.enableBlend();
				ColorHelper.glColor1ia(0x33FFFFFF);
				GlStateManager.disableTexture2D();
				GlStateManager.disableLighting();
				for(int i = 0; i < shadowPositions.size() - 1; ++i)
				{
					Vec3d pos = shadowPositions.get(i);
					Vec3d pos2 = shadowPositions.get(i + 1);

					GL11.glBegin(GL11.GL_LINES);
					GL11.glVertex3d(pos.x - cx, pos.y - cy, pos.z - cz);
					GL11.glVertex3d(pos2.x - cx, pos2.y - cy, pos2.z - cz);
					GL11.glEnd();
				}
				GL11.glLineWidth(w);
				GL11.glLineStipple(1, st);
				GlStateManager.enableLighting();
				GlStateManager.enableTexture2D();
				GlStateManager.popMatrix();
				GL11.glPopAttrib();
			}
		}
	}

	@Override
	public void viewSeal(TileSeal tile)
	{
		Minecraft.getMinecraft().addScheduledTask(() -> Minecraft.getMinecraft().displayGuiScreen(new GuiSealGlobe(tile)));
	}
}