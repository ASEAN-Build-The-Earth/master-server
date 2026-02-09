/*
 * This file is part of DiscordSRV, licensed under the GPLv3 License
 * Copyright (c) 2016-2025 Austin "Scarsz" Shapiro, Henri "Vankka" Schubin and DiscordSRV contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
// package com.discordsrv.common.command.combined.abstraction;
package asia.buildtheearth.asean.commands.abstraction;

import asia.buildtheearth.asean.MasterServer;
import asia.buildtheearth.asean.core.providers.PluginProvider;
import asia.buildtheearth.asean.utils.SendableDiscordMessageUtil;
import com.discordsrv.api.discord.entity.DiscordUser;
import com.discordsrv.api.discord.entity.interaction.component.impl.DiscordModal;
import com.discordsrv.api.discord.entity.message.SendableDiscordMessage;
import com.discordsrv.api.events.discord.interaction.AbstractInteractionEvent;
import com.discordsrv.api.events.discord.interaction.command.DiscordChatInputInteractionEvent;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.callbacks.IModalCallback;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.commands.CommandInteractionPayload;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DiscordCommandExecution extends PluginProvider implements CommandExecution {
    private final AbstractInteractionEvent<?> event;

    private final CommandInteractionPayload interactionPayload;
    private final IReplyCallback replyCallback;
    private final IModalCallback modalCallback;

    private final AtomicBoolean isEphemeral = new AtomicBoolean(true);
    private final AtomicReference<InteractionHook> hook = new AtomicReference<>();

    public DiscordCommandExecution(MasterServer plugin, DiscordChatInputInteractionEvent event) {
        super(plugin);
        this.event = event;
        // Fork internal executable interfaces
        this.interactionPayload = event.asJDA();
        this.replyCallback = event.asJDA();
        this.modalCallback = event.asJDA();
    }

    public DiscordUser getUser() {
        return event.getUser();
    }

    @Override
    public Locale locale() {
        return event.getUserLocale();
    }

    @Override
    public void setEphemeral(boolean ephemeral) {
        isEphemeral.set(ephemeral);
    }

    @Override
    public String getString(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsString() : null;
    }

    @Override
    public Boolean getBoolean(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsBoolean() : null;
    }

    public Message.Attachment getAttachment(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsAttachment() : null;
    }

    public Long getLong(String label) {
        OptionMapping mapping = interactionPayload.getOption(label);
        return mapping != null ? mapping.getAsLong() : null;
    }

    public String getID() {
        return interactionPayload.getId();
    }

    @Override
    public void send(Collection<Text> texts, Collection<Text> extra) {
        StringBuilder builder = new StringBuilder();
        EnumMap<Text.Formatting, Boolean> formats = new EnumMap<>(Text.Formatting.class);

        for (Text text : texts) {
            render(text, builder, formats);
        }
        verifyStyle(builder, formats, null);

        if (!extra.isEmpty()) {
            builder.append("\n\n");
            for (Text text : extra) {
                render(text, builder, formats);
            }
            verifyStyle(builder, formats, null);
        }

        sendResponse(SendableDiscordMessage.builder().setContent(builder.toString()).build());
    }

    @Override
    public void send(Component minecraftComponent, SendableDiscordMessage discord) {
        if (discord == null) {
            return;
        }
        sendResponse(discord);
    }

    private void sendResponse(SendableDiscordMessage message) {
        InteractionHook interactionHook = hook.get();
        boolean ephemeral = isEphemeral.get();
        MessageCreateData data = SendableDiscordMessageUtil.toJDASend(message);
        if (interactionHook != null) {
            interactionHook.sendMessage(data).setEphemeral(ephemeral).queue();
        } else {
            replyCallback.reply(data).setEphemeral(ephemeral).queue();
        }
    }

    private void render(Text text, StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats) {
        if (StringUtils.isEmpty(text.content())) return;

        verifyStyle(builder, formats, text);
        builder.append(text.content());
    }

    private void verifyStyle(StringBuilder builder, EnumMap<Text.Formatting, Boolean> formats, Text text) {
        for (Text.Formatting format : Text.Formatting.values()) {
            boolean is = formats.computeIfAbsent(format, key -> false);
            boolean thisIs = text != null && text.discordFormatting().contains(format);

            if (is != thisIs) {
                // should end or start
                builder.append(format.discord());
                formats.put(format, thisIs);
            }
        }
    }

    @Override
    public void runAsync(Runnable runnable) {
        replyCallback.deferReply(isEphemeral.get()).queue(ih -> {
            hook.set(ih);
            plugin.scheduler().run(runnable);
        });
    }

    public void executeModal(Supplier<DiscordModal> modalSupplier) {
        plugin.scheduler().run(() -> {
            DiscordModal modal = modalSupplier.get();
            modalCallback.replyModal(modal.asJDA()).queue();
        });
    }
}