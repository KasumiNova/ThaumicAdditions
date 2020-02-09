package com.zeitheron.thaumicadditions.items.seed;

import com.zeitheron.thaumicadditions.api.AspectUtil;
import com.zeitheron.thaumicadditions.init.BlocksTAR;
import com.zeitheron.thaumicadditions.init.ItemsTAR;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemSeeds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.translation.I18n;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;
import net.minecraftforge.common.util.Constants.NBT;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.api.aspects.IEssentiaContainerItem;

public class ItemVisSeeds
		extends ItemSeeds
		implements IEssentiaContainerItem
{
	public static final int ASPECT_COUNT = 2;

	public ItemVisSeeds()
	{
		super(null, Blocks.FARMLAND);
		setTranslationKey("vis_seeds");
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World worldIn, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ)
	{
		ItemStack itemstack = player.getHeldItem(hand);
		net.minecraft.block.state.IBlockState state = worldIn.getBlockState(pos);
		if(facing == EnumFacing.UP && player.canPlayerEdit(pos.offset(facing), facing, itemstack) && state.getBlock().canSustainPlant(state, worldIn, pos, EnumFacing.UP, this) && worldIn.isAirBlock(pos.up()))
		{
			Aspect asp = Aspect.getAspect(itemstack.getTagCompound().getString("Aspect"));

			worldIn.setBlockState(pos.up(), BlocksTAR.VIS_CROPS.get(asp).getDefaultState());

			if(player instanceof EntityPlayerMP)
			{
				CriteriaTriggers.PLACED_BLOCK.trigger((EntityPlayerMP) player, pos.up(), itemstack);
			}

			itemstack.shrink(1);
			return EnumActionResult.SUCCESS;
		}
		return EnumActionResult.FAIL;
	}

	@Override
	public EnumPlantType getPlantType(IBlockAccess world, BlockPos pos)
	{
		return EnumPlantType.Crop;
	}

	@Override
	public IBlockState getPlant(IBlockAccess world, BlockPos pos)
	{
		Block crop = BlocksTAR.VIS_CROPS.get(AspectUtil.cycleRandomAspect(BlocksTAR.INDEXED_ASPECTS));
		if(crop == null) crop = Blocks.AIR;
		return crop.getDefaultState();
	}

	public static int getColor(ItemStack stack, int layer)
	{
		if(layer == 1 && stack.hasTagCompound() && stack.getTagCompound().hasKey("Aspect", NBT.TAG_STRING))
		{
			Aspect a = Aspect.getAspect(stack.getTagCompound().getString("Aspect"));
			if(a != null)
				return a.getColor();
		}
		return 0xFFFFFF;
	}

	public static ItemStack create(Aspect aspect, int count)
	{
		ItemStack stack = new ItemStack(ItemsTAR.VIS_SEEDS, count);
		stack.setTagCompound(new NBTTagCompound());
		stack.getTagCompound().setString("Aspect", aspect.getTag());
		return stack;
	}

	@Override
	public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items)
	{
		if(isInCreativeTab(tab))
			for(Aspect a : BlocksTAR.VIS_CROPS.keySet())
				items.add(create(a, 1));
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack)
	{
		String an = "Unknown";
		if(stack.hasTagCompound() && stack.getTagCompound().hasKey("Aspect", NBT.TAG_STRING))
		{
			Aspect a = Aspect.getAspect(stack.getTagCompound().getString("Aspect"));
			if(a != null)
				an = a.getName();
		}
		return I18n.translateToLocalFormatted(this.getUnlocalizedNameInefficiently(stack) + ".name", an).trim();
	}

	@Override
	public AspectList getAspects(ItemStack stack)
	{
		AspectList al = new AspectList();
		if(stack.hasTagCompound())
		{
			NBTTagCompound nbt = stack.getTagCompound();
			if(nbt.hasKey("Aspect", NBT.TAG_STRING))
				al.add(Aspect.getAspect(nbt.getString("Aspect")), ASPECT_COUNT);
		}
		return al;
	}

	@Override
	public boolean ignoreContainedAspects()
	{
		return false;
	}

	@Override
	public void setAspects(ItemStack stack, AspectList list)
	{
		if(list.getAspects().length > 0)
		{
			Aspect a = list.getAspects()[0];
			int ac = list.getAmount(a) / ASPECT_COUNT;
			stack.setCount(ac);
			if(!stack.hasTagCompound())
				stack.setTagCompound(new NBTTagCompound());
			stack.getTagCompound().setString("Aspect", a.getTag());
		}
	}
}