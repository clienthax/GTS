package com.nickimpact.gts.api.discord;

import com.nickimpact.gts.discord.DiscordOption;
import com.nickimpact.gts.discord.Message;

import java.awt.*;
import java.util.concurrent.CompletableFuture;

public interface IDiscordNotifier {

	Message forgeMessage(DiscordOption option, String content);

	CompletableFuture<Void> sendMessage(Message message);
}
