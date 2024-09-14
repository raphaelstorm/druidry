package net.tuboi.druidry.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public class SendMessage {

    public void Send(String message){

        Player player = Minecraft.getInstance().player;

        PlayerChatMessage chatMessage = PlayerChatMessage.unsigned(player.getUUID(), message);
        player.createCommandSourceStack().sendChatMessage(new OutgoingChatMessage.Player(chatMessage), false, ChatType.bind(ChatType.CHAT, player));
    }
}
