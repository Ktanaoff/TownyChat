package net.laboulangerie.townychat.core;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.laboulangerie.townychat.TownyChat;

public class ComponentRenderer {

    public Component parse(OfflinePlayer player, String text) {
        return getMiniMessage(player).deserialize(text, parseTags(player));
    }

    public Component parse(OfflinePlayer player, String text, TagResolver additionalResolver) {
        TagResolver tagResolver = TagResolver.resolver(parseTags(player), additionalResolver);
        return getMiniMessage(player).deserialize(text, tagResolver);
    }

    private TagResolver parseTags(OfflinePlayer player) {
        List<TagResolver> resolvers = new ArrayList<>();
        ConfigurationSection tagSection = TownyChat.PLUGIN.getConfig().getConfigurationSection("tags");

        if (tagSection != null) {
            for (String key : tagSection.getKeys(false)) {
                String raw = tagSection.getString(key, "");
                Component resolved = getMiniMessage(player).deserialize(raw);
                resolvers.add(Placeholder.component(key, resolved));
            }
        }

        return TagResolver.resolver(resolvers);
    }

    private MiniMessage getMiniMessage(OfflinePlayer player) {
        return MiniMessage.builder()
                .tags(
                    TagResolver.builder()
                        .resolver(StandardTags.defaults())    
                        .resolver(papiTagResolver(player))      
                        .build()
                )
                .build();
    }

    private TagResolver papiTagResolver(OfflinePlayer player) {
        return TagResolver.resolver("papi", (argumentQueue, context) -> {
            String placeholder = argumentQueue
                .popOr("The <papi> tag requires exactly one argument, the PAPI placeholder").value();

            String parsed = PlaceholderAPI.setPlaceholders(player, "%" + placeholder + "%");

            if (parsed.contains("§")) {
                return Tag.selfClosingInserting(LegacyComponentSerializer.legacySection().deserialize(parsed));
            } else {
                return Tag.selfClosingInserting(MiniMessage.miniMessage().deserialize(parsed));
            }
        });
    }
}
