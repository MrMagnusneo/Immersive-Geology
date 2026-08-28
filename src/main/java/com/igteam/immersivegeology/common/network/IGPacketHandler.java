package com.igteam.immersivegeology.common.network;

import com.igteam.immersivegeology.common.network.msg.MessageSCRFail;
import com.igteam.immersivegeology.core.lib.IGLib;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class IGPacketHandler
{
	public static final String NET_VERSION = "1";

	public static void initialize(IEventBus modEventBus)
	{
		modEventBus.addListener(IGPacketHandler::register);
	}

	private static void register(RegisterPayloadHandlersEvent event)
	{
		PayloadRegistrar registrar = event.registrar(NET_VERSION);
		registrar.playToServer(
				MessageSCRFail.TYPE,
				MessageSCRFail.STREAM_CODEC,
				MessageSCRFail::process
		);
	}

	/**
	 * Sends a server message directly to the player. Will not do anything if the provided instance is not a {@link ServerPlayer} instance
	 *
	 * @param player  The {@link Player} to send to
	 * @param message The message to send
	 */
	public static void sendToPlayer(Player player, CustomPacketPayload message){
		if(message != null && player instanceof ServerPlayer serverPlayer){
			PacketDistributor.sendToPlayer(serverPlayer, message);
		}
	}

	/** Client -> Server */
	public static void sendToServer(CustomPacketPayload message){
		if(message == null)
			return;

		PacketDistributor.sendToServer(message);
	}

	/**
	 * Sends a packet to everyone in the specified dimension.
	 *
	 * <pre>
	 * Server -> Client
	 * </pre>
	 */
	public static void sendToDimension(ResourceKey<Level> dim, CustomPacketPayload message){
		if(dim == null || message == null)
			return;

		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if(server == null)
			return;
		ServerLevel level = server.getLevel(dim);
		if(level != null)
			PacketDistributor.sendToPlayersInDimension(level, message);
	}

	public static void sendAll(CustomPacketPayload message){
		if(message == null)
			return;

		PacketDistributor.sendToAllPlayers(message);
	}
}
