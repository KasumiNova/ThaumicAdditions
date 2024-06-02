package org.zeith.thaumicadditions;

import com.zeitheron.hammercore.HammerCore;
import com.zeitheron.hammercore.internal.SimpleRegistration;
import com.zeitheron.hammercore.mod.ModuleLister;
import com.zeitheron.hammercore.utils.ReflectionUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.ModMetadata;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.oredict.OreDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeith.thaumicadditions.api.AttributesTAR;
import org.zeith.thaumicadditions.api.ShadowEnchantment;
import org.zeith.thaumicadditions.compat.ITARC;
import org.zeith.thaumicadditions.entity.EntityBlueWolf;
import org.zeith.thaumicadditions.entity.EntityChester;
import org.zeith.thaumicadditions.entity.EntityEssentiaShot;
import org.zeith.thaumicadditions.entity.EntityMithminiteScythe;
import org.zeith.thaumicadditions.init.*;
import org.zeith.thaumicadditions.misc.theorycraft.CardThaumicAdditions;
import org.zeith.thaumicadditions.proxy.CommonProxy;
import org.zeith.thaumicadditions.utils.CreativeTabTAR;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.internal.WeightedRandomLoot;
import thaumcraft.api.research.ResearchCategories;
import thaumcraft.api.research.ResearchCategory;
import thaumcraft.api.research.theorycraft.TheorycraftManager;
import thaumcraft.common.entities.monster.EntityPech;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

@Mod(
		modid = InfoTAR.MOD_ID,
		name = InfoTAR.MOD_NAME,
		version = InfoTAR.MOD_VERSION,
		certificateFingerprint = "9f5e2a811a8332a842b34f6967b7db0ac4f24856",
		dependencies = "required-after:hammercore;required-after:thaumcraft@[6.1.BETA26,);before:iceandfire;after:solarflux",
		updateJSON = "https://api.modrinth.com/updates/Fi4BbtaL/forge_updates.json"
)
public class TAReconstructed
{
	public static final Logger LOG = LogManager.getLogger(InfoTAR.MOD_ID);

	@Instance
	public static TAReconstructed instance;

	public static CreativeTabs tab;

	@SidedProxy(serverSide = "org.zeith.thaumicadditions.proxy.CommonProxy", clientSide = "org.zeith.thaumicadditions.proxy.ClientProxy")
	public static CommonProxy proxy;

	public static ResearchCategory RES_CAT;

	public static List<ITARC> arcs;
	static Field registryNameIMPL;

	public static void resetRegistryName(net.minecraftforge.registries.IForgeRegistryEntry.Impl<?> impl)
	{
		if(registryNameIMPL == null)
			registryNameIMPL = net.minecraftforge.registries.IForgeRegistryEntry.Impl.class.getDeclaredFields()[2];
		try
		{
			if(Modifier.isFinal(registryNameIMPL.getModifiers()))
				ReflectionUtil.setFinalField(registryNameIMPL, impl, null);
			else
			{
				registryNameIMPL.setAccessible(true);
				registryNameIMPL.set(impl, null);
			}
		} catch(ReflectiveOperationException e)
		{
			e.printStackTrace();
		}
	}

	public static void setupShadowEnchanting()
	{
		ShadowEnchantment.registerEnchantment(Enchantments.UNBREAKING, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.TOOL, 16).multiplyByLvl(Aspect.PROTECT, 4), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/unbreaking.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.THORNS, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.EXCHANGE, 20).multiplyByLvl(Aspect.PROTECT, 6), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/thorns.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.SHARPNESS, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.AVERSION, 20).constant(KnowledgeTAR.EXITIUM, 20), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/sharpness.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.PUNCH, ShadowEnchantment.aspectBuilder().constant(KnowledgeTAR.IMPERIUM, 5).multiplyByLvl(Aspect.EXCHANGE, 12), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/punch.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.PROTECTION, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.PROTECT, 20), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/protection.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.LURE, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.DESIRE, 20), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/lure.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.LOOTING, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.DESIRE, 20).constant(Aspect.EXCHANGE, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/looting.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.KNOCKBACK, ShadowEnchantment.aspectBuilder().multiplyByLvl(KnowledgeTAR.IMPERIUM, 5).multiplyByLvl(Aspect.EXCHANGE, 12), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/knockback.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.INFINITY, ShadowEnchantment.aspectBuilder().constant(KnowledgeTAR.CAELES, 5), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/infinity.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.FORTUNE, ShadowEnchantment.aspectBuilder().constant(KnowledgeTAR.VISUM, 5).multiplyByLvl(Aspect.DESIRE, 20).multiplyByLvl(Aspect.ELDRITCH, 10).multiplyByLvl(Aspect.MIND, 5), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/fortune.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.FIRE_PROTECTION, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.PROTECT, 20).multiplyByLvl(Aspect.FIRE, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/fire_protection.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.FEATHER_FALLING, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.FLIGHT, 20).multiplyByLvl(Aspect.AIR, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/feather_falling.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.BLAST_PROTECTION, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.PROTECT, 20).multiplyByLvl(KnowledgeTAR.EXITIUM, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/blast_protection.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.SILK_TOUCH, ShadowEnchantment.aspectBuilder().constant(Aspect.ORDER, 100).constant(Aspect.DESIRE, 10).constant(KnowledgeTAR.FLUCTUS, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/silk_touch.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.SMITE, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.AVERSION, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/smite.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.FIRE_ASPECT, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.AVERSION, 10).multiplyByLvl(Aspect.FIRE, 20), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/fire_aspect.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.LUCK_OF_THE_SEA, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.WATER, 20).multiplyByLvl(Aspect.EXCHANGE, 15), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/luck_of_the_sea.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.POWER, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.AVERSION, 15).multiplyByLvl(Aspect.FLIGHT, 10).multiplyByLvl(Aspect.AIR, 2), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/power.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.EFFICIENCY, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.ORDER, 5).multiplyByLvl(Aspect.EXCHANGE, 10).multiplyByLvl(Aspect.TOOL, 10), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/efficiency.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.FROST_WALKER, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.COLD, 20), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/frost_walker.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.RESPIRATION, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.WATER, 20).multiplyByLvl(KnowledgeTAR.VENTUS, 20).multiplyByLvl(Aspect.EXCHANGE, 5), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/respiration.png"), null);
		ShadowEnchantment.registerEnchantment(Enchantments.MENDING, ShadowEnchantment.aspectBuilder().multiplyByLvl(Aspect.DESIRE, 200).multiplyByLvl(KnowledgeTAR.IMPERIUM, 300).multiplyByLvl(Aspect.EXCHANGE, 500), new ResourceLocation(InfoTAR.MOD_ID, "textures/enchantments/mending.png"), null);
	}

	public static NBTTagCompound getPlayerTag(EntityPlayer player)
	{
		NBTTagCompound nbt = player.getEntityData();
		NBTTagCompound tag = nbt.getCompoundTag(InfoTAR.MOD_ID);
		if(!nbt.hasKey(InfoTAR.MOD_ID, Constants.NBT.TAG_COMPOUND)) nbt.setTag(InfoTAR.MOD_ID, tag);
		return tag;
	}

	public static void setPlayerTag(EntityPlayer player, NBTTagCompound tag)
	{
		NBTTagCompound nbt = player.getEntityData();
		nbt.setTag(InfoTAR.MOD_ID, tag);
	}

	@EventHandler
	public void certificateViolation(FMLFingerprintViolationEvent e)
	{
		LOG.warn("*****************************");
		LOG.warn("WARNING: Somebody has been tampering with Thaumic Additions (Reconstructed) jar!");
		LOG.warn("It is highly recommended that you redownload mod from https://www.curseforge.com/projects/232564 !");
		LOG.warn("*****************************");
		HammerCore.invalidCertificates.put(InfoTAR.MOD_ID, "https://www.curseforge.com/projects/232564");
	}

	@EventHandler
	public void construct(FMLConstructionEvent e)
	{
		MinecraftForge.EVENT_BUS.register(proxy);
		proxy.construct();
	}

	@EventHandler
	public void preInit(FMLPreInitializationEvent e)
	{
		tab = new CreativeTabTAR(InfoTAR.MOD_ID);
		arcs = ModuleLister.createModules(ITARC.class, null, e.getAsmData());

		FluidsTAR.init.call();
		MinecraftForge.EVENT_BUS.register(this);
		MinecraftForge.EVENT_BUS.register(proxy);

		ModMetadata meta = e.getModMetadata();
		meta.autogenerated = false;
		meta.version = InfoTAR.MOD_VERSION;
		meta.modId = InfoTAR.MOD_ID;
		meta.name = InfoTAR.MOD_NAME;
		meta.authorList = HammerCore.AUTHORS;

		KnowledgeTAR.clInit.call();
		GuisTAR.register();
		SoundsTAR.register();

		SimpleRegistration.registerFieldItemsFrom(ItemsTAR.class, InfoTAR.MOD_ID, tab);
		SimpleRegistration.registerFieldBlocksFrom(BlocksTAR.class, InfoTAR.MOD_ID, tab);
		BlocksTAR.loadAspectBlocks();

		arcs.forEach(ITARC::preInit);

		EntityRegistry.registerModEntity(new ResourceLocation(InfoTAR.MOD_ID, "chester"), EntityChester.class, InfoTAR.MOD_ID + ".chester", 0, instance, 256, 1, true);
		EntityRegistry.registerModEntity(new ResourceLocation(InfoTAR.MOD_ID, "essentia_shot"), EntityEssentiaShot.class, InfoTAR.MOD_ID + ".essentia_shot", 1, instance, 64, 1, true);
		EntityRegistry.registerModEntity(new ResourceLocation(InfoTAR.MOD_ID, "blue_wolf"), EntityBlueWolf.class, InfoTAR.MOD_ID + ".blue_wolf", 2, instance, 64, 1, true, 0x0172C0, 0x01FFE5);
		EntityRegistry.registerModEntity(new ResourceLocation(InfoTAR.MOD_ID, "mithminite_scythe"), EntityMithminiteScythe.class, InfoTAR.MOD_ID + ".mithminite_scythe", 3, instance, 64, 1, true);

		proxy.preInit();

		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 0));
		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 1));
		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 2));
		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 3));
		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 4));
		OreDictionary.registerOre("head", new ItemStack(Items.SKULL, 1, 5));
	}

	@EventHandler
	public void init(FMLInitializationEvent e)
	{
		for(ITARC a : arcs)
			a.init();
		proxy.init();
		RES_CAT = ResearchCategories.registerCategory("THAUMADDITIONS", "UNLOCKINFUSION", new AspectList().add(Aspect.ALCHEMY, 30).add(Aspect.FLUX, 10).add(Aspect.MAGIC, 10).add(Aspect.LIFE, 5).add(Aspect.AVERSION, 5).add(Aspect.DESIRE, 5).add(Aspect.WATER, 5), new ResourceLocation(InfoTAR.MOD_ID, "textures/gui/thaumonomicon_icon.png"), CommonProxy.TEXTURE_THAUMONOMICON_BG, new ResourceLocation(InfoTAR.MOD_ID, "textures/gui/gui_research_back_over.png"));
		RecipesTAR.init.call();
		PotionsTAR.register.call();
		SealsTAR.init();

		WeightedRandomLoot.lootBagRare.add(new WeightedRandomLoot(new ItemStack(ItemsTAR.ZEITH_FUR), 1));
		EntityPech.tradeInventory.get(2).add(Arrays.asList(5, new ItemStack(ItemsTAR.ZEITH_FUR)));

		TheorycraftManager.registerCard(CardThaumicAdditions.class);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent e)
	{
		proxy.postInit();
		KnowledgeTAR.init.call();
		KnowledgeTAR.insertAspects.call();
		RecipesTAR.postInit.call();
		setupShadowEnchanting();
	}

	@SubscribeEvent
	public void entityInit(EntityEvent.EntityConstructing e)
	{
		if(e.getEntity() instanceof EntityPlayer)
		{
			EntityPlayer p = (EntityPlayer) e.getEntity();
			p.getAttributeMap().registerAttribute(AttributesTAR.SOUND_SENSIVITY);
		}
	}
}