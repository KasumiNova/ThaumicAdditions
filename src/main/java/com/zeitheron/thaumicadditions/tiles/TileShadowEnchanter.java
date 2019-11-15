package com.zeitheron.thaumicadditions.tiles;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.zeitheron.hammercore.net.HCNet;
import com.zeitheron.hammercore.tile.ITileDroppable;
import com.zeitheron.hammercore.tile.TileSyncableTickable;
import com.zeitheron.hammercore.utils.SoundUtil;
import com.zeitheron.hammercore.utils.inventory.InventoryDummy;
import com.zeitheron.thaumicadditions.api.ShadowEnchantment;
import com.zeitheron.thaumicadditions.inventory.container.ContainerShadowEnchanter;
import com.zeitheron.thaumicadditions.inventory.gui.GuiShadowEnchanter;
import com.zeitheron.thaumicadditions.net.PacketShadowFX;
import com.zeitheron.thaumicadditions.utils.ListHelper;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants.NBT;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.lib.SoundsTC;
import thaumcraft.common.lib.events.EssentiaHandler;

public class TileShadowEnchanter extends TileSyncableTickable implements ITileDroppable
{
	public final InventoryDummy items = new InventoryDummy(2);
	
	public List<EnchantmentData> enchants = new ArrayList<>();
	
	public boolean infusing = false;
	public AspectList pending;
	public int craftTimer;
	
	public int brainCD;
	
	@Override
	public void tick()
	{
		enchants.removeIf(e -> !isAplicable(e.enchantment));
		
		if(infusing && items.getStackInSlot(1).isEmpty())
		{
			if(pending == null || items.getStackInSlot(0).isEmpty())
			{
				if(!world.isRemote)
					SoundUtil.playSoundEffect(world, SoundsTC.craftfail.getRegistryName().toString(), pos, 1F, 1F, SoundCategory.BLOCKS);
				infusing = false;
				craftTimer = 0;
				enchants.clear();
				pending = null;
				sendChangesToNearby();
				return;
			} else if(!world.isRemote)
			{
				++craftTimer;
				if(brainCD > 0)
					--brainCD;
				if(brainCD <= 0)
				{
					SoundUtil.playSoundEffect(world, SoundsTC.brain.getRegistryName().toString(), pos, 0.2F, 1F, SoundCategory.BLOCKS);
					brainCD += 35 + getRNG().nextInt(50);
				}
				if(pending.visSize() == 0)
				{
					ItemStack stack = items.getStackInSlot(0).copy();
					enchants.forEach(ed -> stack.addEnchantment(ed.enchantment, ed.enchantmentLevel));
					items.setInventorySlotContents(1, stack);
					items.getStackInSlot(0).shrink(1);
					SoundUtil.playSoundEffect(world, SoundsTC.poof.getRegistryName().toString(), pos, 1F, 1F, SoundCategory.BLOCKS);
					enchants.clear();
					infusing = false;
					craftTimer = 0;
					sendChangesToNearby();
					return;
				} else
				{
					Aspect a = pending.getAspectsSortedByName()[0];
					if(atTickRate(6))
					{
						if(EssentiaHandler.drainEssentia(this, a, null, 12, 1))
						{
							pending.remove(a, 1);
							HCNet.INSTANCE.sendToAllAround(PacketShadowFX.create(this, a.getColor()), getSyncPoint(100));
							sendChangesToNearby();
							brainCD = Math.min(brainCD, 50);
						} else
							brainCD = 10000;
					}
				}
			}
		}
	}
	
	public AspectList calculateAspects()
	{
		AspectList al = new AspectList();
		for(AspectList list : enchants.stream().map(ed ->
		{
			ShadowEnchantment se = ShadowEnchantment.pick(ed.enchantment);
			if(se != null)
				return se.getAspects(ed.enchantmentLevel);
			return null;
		}).collect(Collectors.toList()))
			if(list != null)
				al.add(list);
			else
				return null;
		return al;
	}
	
	public void startCraft()
	{
		if(!infusing && !enchants.isEmpty() && items.getStackInSlot(1).isEmpty())
		{
			AspectList al = calculateAspects();
			if(al == null || al.size() > 32)
				return;
			
			infusing = true;
			craftTimer = 0;
			pending = al;
			
			SoundUtil.playSoundEffect(world, SoundsTC.craftstart.getRegistryName().toString(), pos, 0.2F, 1F, SoundCategory.BLOCKS);
			
			sendChangesToNearby();
		}
	}
	
	public boolean isAplicable(Enchantment ench)
	{
		if(enchants.stream().filter(ed -> ed.enchantment != ench && !ed.enchantment.isCompatibleWith(ench)).findAny().isPresent())
			return false;
		ItemStack s = items.getStackInSlot(0);
		return !s.isEmpty() && s.isItemEnchantable() && ench.canApply(s);
	}
	
	public boolean isAplicableBy(Enchantment ench, EntityPlayer player)
	{
		if(!isAplicable(ench))
			return false;
		ShadowEnchantment e = ShadowEnchantment.pick(ench);
		if(e == null || !e.canBeAppliedBy(player))
			return false;
		return true;
	}
	
	public void upLvl(Enchantment ench, EntityPlayer player)
	{
		if(!isAplicableBy(ench, player) || infusing)
			return;
		if(ListHelper.replace(enchants, e -> e.enchantment == ench, k -> new EnchantmentData(ench, Math.max(ench.getMinLevel(), Math.min(k.enchantmentLevel + 1, ench.getMaxLevel())))) == 0)
			enchants.add(new EnchantmentData(ench, ench.getMinLevel()));
		sendChangesToNearby();
	}
	
	public void downLvl(Enchantment ench, EntityPlayer player)
	{
		if(!infusing && ListHelper.replace(enchants, e -> e.enchantment == ench, k ->
		{
			if(k.enchantmentLevel == ench.getMinLevel())
				return null;
			return new EnchantmentData(ench, Math.max(ench.getMinLevel(), Math.min(k.enchantmentLevel - 1, ench.getMaxLevel())));
		}) > 0)
			sendChangesToNearby();
	}
	
	@Override
	public boolean hasGui()
	{
		return true;
	}
	
	@Override
	public Object getClientGuiElement(EntityPlayer player)
	{
		return new GuiShadowEnchanter(new ContainerShadowEnchanter(player, this));
	}
	
	@Override
	public Object getServerGuiElement(EntityPlayer player)
	{
		return new ContainerShadowEnchanter(player, this);
	}
	
	@Override
	public void writeNBT(NBTTagCompound nbt)
	{
		nbt.setTag("Items", items.writeToNBT(new NBTTagCompound()));
		
		NBTTagList ench = new NBTTagList();
		enchants.forEach(d ->
		{
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("id", d.enchantment.getRegistryName().toString());
			tag.setInteger("lvl", d.enchantmentLevel);
			ench.appendTag(tag);
		});
		nbt.setTag("Enchantments", ench);
		nbt.setBoolean("Infusing", infusing);
		nbt.setInteger("Timer", craftTimer);
		if(pending != null)
			pending.writeToNBT(nbt, "Aspects");
	}
	
	@Override
	public void readNBT(NBTTagCompound nbt)
	{
		items.readFromNBT(nbt.getCompoundTag("Items"));
		infusing = nbt.getBoolean("Infusing");
		craftTimer = nbt.getInteger("Timer");
		NBTTagList ench = nbt.getTagList("Enchantments", NBT.TAG_COMPOUND);
		enchants.clear();
		for(int i = 0; i < ench.tagCount(); ++i)
		{
			NBTTagCompound tag = ench.getCompoundTagAt(i);
			Enchantment e = ForgeRegistries.ENCHANTMENTS.getValue(new ResourceLocation(tag.getString("id")));
			int lvl = tag.getInteger("lvl");
			enchants.add(new EnchantmentData(e, lvl));
		}
		if(nbt.hasKey("Aspects"))
		{
			pending = new AspectList();
			pending.readFromNBT(nbt, "Aspects");
		} else
			pending = null;
	}
	
	@Override
	public void createDrop(EntityPlayer player, World world, BlockPos pos)
	{
		items.drop(world, pos);
		items.clear();
	}
}