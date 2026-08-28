/*
 * Muddykat
 * Copyright (c) 2025
 *
 * This code is licensed under "GNU LESSER GENERAL PUBLIC LICENSE"
 * Details can be found in the license file in the root folder of this project
 */

package com.igteam.immersivegeology.common.network.msg;

import com.igteam.immersivegeology.common.block.multiblocks.logic.SmallChemicalReactorLogic;
import com.igteam.immersivegeology.common.network.INetMessage;
import com.igteam.immersivegeology.core.lib.IGLib;
import com.igteam.immersivegeology.core.material.data.enums.MiscEnum;
import com.igteam.immersivegeology.core.material.helper.flags.BlockCategoryFlags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public class MessageSCRFail implements INetMessage
{
	public static final CustomPacketPayload.Type<MessageSCRFail> TYPE =
			new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(IGLib.MODID, "scr_fail"));
	public static final StreamCodec<FriendlyByteBuf, MessageSCRFail> STREAM_CODEC =
			StreamCodec.ofMember(MessageSCRFail::toBytes, MessageSCRFail::new);

	private final BlockPos pos;
	private final float damage;

	public MessageSCRFail(BlockPos pos, float damage)
	{
		this.pos = pos;
		this.damage = damage;
	}

	public MessageSCRFail(FriendlyByteBuf buf)
	{
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		this.damage = buf.readFloat();
	}

	@Override
	public void toBytes(FriendlyByteBuf buf)
	{
		buf.writeInt(this.pos.getX()).writeInt(this.pos.getY()).writeInt(this.pos.getZ());
		buf.writeFloat(this.damage);
	}

	@Override
	public CustomPacketPayload.@NotNull Type<MessageSCRFail> type()
	{
		return TYPE;
	}

	@Override
	public void process(IPayloadContext context)
	{
		context.enqueueWork(() -> {
			if (context.player() instanceof ServerPlayer player) {
				ServerLevel world = player.serverLevel();
				if (world.isAreaLoaded(this.pos, 1)) {
					BlockState blockState = world.getBlockState(this.pos);
					MutableBlockPos b = new MutableBlockPos();
					b.set(this.pos);
					RandomSource random = world.getRandom();
					for(int x = 0; x<=1; x++)
					{
						for(int z = 0; z<=1; z++)
						{
							for(int y = 0; y<4; y++)
							{
								if(random.nextInt(1,95) < damage){
									BlockPos pos = b.offset(x,y,z);
									BlockState newState = MiscEnum.RustyMetal.getBlock(BlockCategoryFlags.SHEETMETAL_BLOCK).defaultBlockState();
									world.setBlock(pos, newState, 1 | 2);
								}
							}
						}
					}
				}
			}
		});
	}
}
