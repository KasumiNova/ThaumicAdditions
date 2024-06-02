package org.zeith.thaumicadditions.net.fxh;

import com.zeitheron.hammercore.HammerCore;
import com.zeitheron.hammercore.net.IPacket;
import com.zeitheron.hammercore.net.PacketContext;
import com.zeitheron.hammercore.utils.base.Cast;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.zeith.thaumicadditions.TAReconstructed;
import org.zeith.thaumicadditions.tiles.TileAuraDisperser;

public class FXHPacket
		implements IPacket
{
	static
	{
		IPacket.handle(FXHPacket.class, FXHPacket::new);
	}

	BlockPos pos;
	int sub;

	public FXHPacket(BlockPos pos, int sub)
	{
		this.pos = pos;
		this.sub = sub;
	}

	public FXHPacket()
	{
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt)
	{
		nbt.setLong("p", pos.toLong());
		nbt.setInteger("s", sub);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt)
	{
		pos = BlockPos.fromLong(nbt.getLong("p"));
		sub = nbt.getInteger("s");
	}

	@Override
	public IPacket executeOnClient(PacketContext net)
	{
		EntityPlayer ep = HammerCore.renderProxy.getClientPlayer();
		
		if(sub == 0)
		{
			TileAuraDisperser tad = Cast.cast(ep.world.getTileEntity(pos), TileAuraDisperser.class);
			if(tad != null)
				TAReconstructed.proxy.getFX().spawnAuraDisperserFX(tad);
		}
		
		return null;
	}
}