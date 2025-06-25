package net.laboulangerie.townychat.core;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import io.papermc.paper.chat.ChatRenderer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.laboulangerie.townychat.TownyChat;
import net.laboulangerie.townychat.player.ChatPlayer;
import net.laboulangerie.townychat.player.ChatPlayerManager;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class TownyChatRenderer implements ChatRenderer.ViewerUnaware {

    private final ChatPlayerManager chatPlayerManager;
    private final ComponentRenderer componentRenderer;

    public TownyChatRenderer() {
        this.chatPlayerManager = TownyChat.PLUGIN.getChatPlayerManager();
        this.componentRenderer = TownyChat.PLUGIN.getComponentRenderer();
    }

    private Component buildMessage(@NotNull Player source, Component message, String channelFormat) {
        String raw = PlainTextComponentSerializer.plainText().serialize(message);
        Component legacyParsed = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);

        String cleanRaw = censorString(PlainTextComponentSerializer.plainText().serialize(legacyParsed));

        TagResolver.Builder tagBuilder = TagResolver.builder();
        if (source.hasPermission("townychat.format.all")) {
            tagBuilder.resolver(StandardTags.defaults());
        } else {
            if (source.hasPermission("townychat.format.colors"))
                tagBuilder.resolver(StandardTags.color());
            if (source.hasPermission("townychat.format.decorations"))
                tagBuilder.resolver(StandardTags.decorations());
            if (source.hasPermission("townychat.format.misc")) {
                tagBuilder.resolver(StandardTags.clickEvent());
                tagBuilder.resolver(StandardTags.hoverEvent());
                tagBuilder.resolver(StandardTags.insertion());
                tagBuilder.resolver(StandardTags.keybind());
                tagBuilder.resolver(StandardTags.translatable());
                tagBuilder.resolver(StandardTags.rainbow());
                tagBuilder.resolver(StandardTags.gradient());
                tagBuilder.resolver(StandardTags.transition());
                tagBuilder.resolver(StandardTags.font());
                tagBuilder.resolver(StandardTags.newline());
            }
        }
        Component parsedMessage = MiniMessage.builder()
                .tags(tagBuilder.build())
                .build()
                .deserialize(cleanRaw);

        tagBuilder.resolver(Placeholder.component("message", parsedMessage));
        tagBuilder.resolver(Placeholder.component("username", source.name()));

        String townFormat = TownyChat.PLUGIN.getConfig().getString("papi_chat_formatting.town", "");
        String nationFormat = TownyChat.PLUGIN.getConfig().getString("papi_chat_formatting.nation", "");
        String bothFormat = TownyChat.PLUGIN.getConfig().getString("papi_chat_formatting.both", "");

        Component formattedTown = Component.empty();
        Component formattedNation = Component.empty();
        Component formattedBoth = Component.empty();

        Resident resident = TownyAPI.getInstance().getResident(source);
        if (resident != null && resident.hasTown()) {
            Town town = resident.getTownOrNull();
            if (town != null) {
                formattedTown = MiniMessage.miniMessage().deserialize(townFormat.replace("%s", town.getName()));

                if (town.hasNation()) {
                    Nation nation = town.getNationOrNull();
                    if (nation != null) {
                        formattedNation = MiniMessage.miniMessage().deserialize(nationFormat.replace("%s", nation.getName()));
                        formattedBoth = MiniMessage.miniMessage().deserialize(
                                bothFormat.replace("%t", town.getName()).replace("%n", nation.getName()));
                    } else {
                        formattedBoth = formattedTown;
                    }
                } else {
                    formattedBoth = formattedTown;
                }
            }
        }

        tagBuilder.resolver(Placeholder.component("town", formattedTown));
        tagBuilder.resolver(Placeholder.component("nation", formattedNation));
        tagBuilder.resolver(Placeholder.component("nation_town", formattedBoth));

        return componentRenderer.parse(source, channelFormat, tagBuilder.build());
    }

    @Override
    public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message) {
        ChatPlayer chatPlayer = chatPlayerManager.getChatPlayer(source);
        String channelFormat = chatPlayer.getCurrentChannel().getFormat();
        return buildMessage(source, message, channelFormat);
    }

    public @NotNull Component spyRender(@NotNull Player source, @NotNull Component message) {
        ChatPlayer chatPlayer = chatPlayerManager.getChatPlayer(source);
        String channelFormat = chatPlayer.getCurrentChannel().getSpyFormat();
        return buildMessage(source, message, channelFormat);
    }

    private String censorString(String string) {
        List<String> words = TownyChat.PLUGIN.getConfig().getStringList("blacklist");
        String[] censorChars = { "#", "@", "!", "*" };

        for (String word : words) {
            string = string.replaceAll("(?i)" + Pattern.quote(word),
                    new Random().ints(word.length(), 0, censorChars.length)
                            .mapToObj(i -> censorChars[i])
                            .collect(Collectors.joining()));
        }

        return string;
    }
}
