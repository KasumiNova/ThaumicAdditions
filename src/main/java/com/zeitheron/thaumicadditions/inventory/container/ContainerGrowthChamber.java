package com.zeitheron.thaumicadditions.inventory.container;

import com.zeitheron.hammercore.client.gui.impl.container.ItemTransferHelper.TransferableContainer;
import com.zeitheron.thaumicadditions.tiles.TileGrowthChamber;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ContainerGrowthChamber extends TransferableContainer<TileGrowthChamber>
{
	public ContainerGrowthChamber(EntityPlayer player, TileGrowthChamber t)
	{
		super(player, t, 8, 84);
	}
	
	@Override
	protected void addCustomSlots()
	{
		addSlotToContainer(new Slot(t.growthInventory, 0, 21, 30)
		{
			@Override
			public boolean isItemValid(ItemStack stack)
			{
				return t.growthInventory.validSlots.test(0, stack);
			}
			
			@Override
			public int getSlotStackLimit()
			{
				return 1;
			}
		});
		
		addSlotToContainer(new Slot(t.growthInventory, 1, 135, 30));
	}
	
	@Override
	protected void addTransfer()
	{
		transfer.addInTransferRule(0, s -> t.growthInventory.validSlots.test(0, s));
		transfer.addOutTransferRule(0, i -> i > 1);
		
		transfer.addOutTransferRule(1, i -> i > 1);
	}
}