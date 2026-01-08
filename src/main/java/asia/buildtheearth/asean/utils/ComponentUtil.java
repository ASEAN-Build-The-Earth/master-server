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
// package com.discordsrv.common.util;
package asia.buildtheearth.asean.utils;

import com.discordsrv.api.DiscordSRV;
import com.discordsrv.api.component.MinecraftComponent;
import com.discordsrv.api.component.MinecraftComponentAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.flattener.ComponentFlattener;
import net.kyori.adventure.text.flattener.FlattenerListener;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
// import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
// import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A util class for {@link Component}s and {@link MinecraftComponent}s.
 */
public final class ComponentUtil {

    private ComponentUtil() {}

    private static MinecraftComponentAdapter<Component> ADAPTER;

    @NotNull
    private static MinecraftComponentAdapter<Component> adapter() {
        return ADAPTER != null ? ADAPTER : (Objects.requireNonNull(ADAPTER = MinecraftComponentAdapter.unrelocated()));
    }

//    public static boolean isEmpty(@NotNull Component component) {
//        return ADAPTER.serialize(component).isEmpty();
//    }

    public static boolean isEmpty(@NotNull MinecraftComponent component) {
        return component.asPlainString().isEmpty();
    }

    @Contract("null -> null")
    public static MinecraftComponent fromPlain(@Nullable String plainText) {
        if (plainText == null) {
            return null;
        }
        return toAPI(Component.text(plainText));
    }

    @Contract("null -> null")
    public static MinecraftComponent toAPI(Component component) {
        if (component == null) {
            return null;
        }

        return ADAPTER.toDiscordSRV(component);
    }

    @Contract("null -> null; _ -> _")
    public static Component fromAPI(@Nullable MinecraftComponent component) {
        if (component == null) {
            return null;
        }

        return component.asAdventure(adapter());
    }

    public static List<TextColor> extractColors(Component component) {
        List<TextColor> colors = new ArrayList<>();
        ComponentFlattener.basic().flatten(component, new FlattenerListener() {
            @Override
            public void component(@NotNull String text) {}

            @Override
            public void popStyle(@NotNull Style style) {
                TextColor textColor = style.color();
                if (textColor != null) {
                    colors.add(textColor);
                }
            }
        });
        return colors;
    }
}