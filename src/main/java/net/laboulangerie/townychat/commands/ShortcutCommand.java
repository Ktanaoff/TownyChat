package net.laboulangerie.townychat.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.TownyMessaging;

import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.laboulangerie.townychat.TownyChat;
import net.laboulangerie.townychat.channels.ChannelTypes;
import net.laboulangerie.townychat.player.ChatPlayer;

public class ShortcutCommand implements CommandExecutor {
    private ChannelTypes channelType;

    public ShortcutCommand(ChannelTypes channelType) {
        this.channelType = channelType;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias,
            @NotNull String[] args) {
        if (!(sender instanceof Player)) {
            String errorMessage = TownyChat.PLUGIN.getConfig().getString("lang.err_sender_not_player");
            sender.sendMessage(MiniMessage.miniMessage().deserialize(errorMessage));
            return true;
        }

        Player player = (Player) sender;
        ChatPlayer chatPlayer = TownyChat.PLUGIN.getChatPlayerManager().getChatPlayer(player);

        if (args.length == 0 && chatPlayer.getChannel(channelType) != null) {
            chatPlayer.setCurrentChannel(channelType);

            String switchMessage = TownyChat.PLUGIN.getConfig().getString("lang.channel_switched");
            TextComponent switchMessageComponent = (TextComponent) MiniMessage.miniMessage().deserialize(switchMessage,
                    Placeholder.unparsed("channel", channelType.name()));

            sender.sendMessage("\n");
            TownyMessaging.sendMsg(sender, switchMessageComponent.content());
            sender.sendMessage("\n");

            return true;
        }

        String message = String.join(" ", args);

        if (chatPlayer.getChannels().keySet().contains(this.channelType)) {
            ChannelTypes previousChannelType = chatPlayer.getCurrentChannel().getType();
            chatPlayer.setCurrentChannel(this.channelType);
            player.chat(message);
            chatPlayer.setCurrentChannel(previousChannelType);
            return true;
        }

        String errorMessage = TownyChat.PLUGIN.getConfig().getString("lang.err_channel_not_found");
        TextComponent errorMessageComponent = (TextComponent) MiniMessage.miniMessage().deserialize(errorMessage,
                Placeholder.unparsed("channel", channelType.name().toLowerCase()));

        TownyMessaging.sendErrorMsg(sender, errorMessageComponent.content());

        return true;
    }
}
