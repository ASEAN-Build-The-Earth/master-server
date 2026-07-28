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
/*
 * Modified by Tin <tintinkung.lemonade@gmail.com> on 2026-07-29
 * - Added some static utility methods
 */
// package com.discordsrv.common.command.combined.abstraction;
package asia.buildtheearth.asean.discord;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.apache.commons.text.CaseUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.EnumSet;

public class Text {

    private final String content;
    private TextColor gameColor;
    private EnumSet<TextDecoration> gameFormatting;
    private EnumSet<Formatting> discordFormatting;

    public Text(String content) {
        this.content = content;
        this.gameFormatting = EnumSet.noneOf(TextDecoration.class);
        this.discordFormatting = EnumSet.noneOf(Formatting.class);
    }

    public String content() {
        return content;
    }

    public Text withGameColor(TextColor color) {
        this.gameColor = color;
        return this;
    }

    public TextColor gameColor() {
        return gameColor;
    }

    public Text withFormatting(Formatting... formatting) {
        EnumSet<TextDecoration> game = EnumSet.noneOf(TextDecoration.class);
        for (Formatting format : formatting) {
            game.add(format.game);
        }

        this.gameFormatting = game;
        this.discordFormatting = EnumSet.copyOf(Arrays.asList(formatting));
        return this;
    }

    public Text withGameFormatting(Formatting... formatting) {
        EnumSet<TextDecoration> game = EnumSet.noneOf(TextDecoration.class);
        for (Formatting format : formatting) {
            game.add(format.game);
        }

        this.gameFormatting = game;
        return this;
    }

    public EnumSet<TextDecoration> gameFormatting() {
        return gameFormatting;
    }

    public Text withDiscordFormatting(Formatting... formatting) {
        this.discordFormatting = EnumSet.copyOf(Arrays.asList(formatting));
        return this;
    }

    public EnumSet<Formatting> discordFormatting() {
        return discordFormatting;
    }

    public enum Formatting {
        BOLD(TextDecoration.BOLD, "**"),
        ITALICS(TextDecoration.ITALIC, "*"),
        UNDERLINED(TextDecoration.UNDERLINED, "__"),
        STRIKETHROUGH(TextDecoration.STRIKETHROUGH, "~~");

        private final TextDecoration game;
        private final String discord;

        Formatting(TextDecoration game, String discord) {
            this.game = game;
            this.discord = discord;
        }

        public String discord() {
            return discord;
        }
    }

    @Contract(pure = true)
    public static @NotNull String stripExtension(@NotNull String base) {
        return base.replaceAll("\\.[^.]+$", "");
    }

    /**
     * Normalizes a base string into lower-hyphen (kebab-case) to be conventional with BlueMap marker naming.
     * <p>If {@code preserveNaming} is true, the original string is returned unmodified.</p>
     *
     * @see <a href="https://stackoverflow.com/a/70956516">Normalization Process</a>
     *
     * @param base The input base name (may include extension, Unicode, or CamelCase)
     * @return A normalized, lower-hyphen-case string
     */
    public static String toLowerHyphen(@NotNull String name) {
        // Normalize latin/unicode characters
        name = Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("_", "-");

        // Split obvious non-word characters
        String[] words = name.split("\\W+");

        // Cannot determine words, returning as non-white-space
        if(words.length == 0) return name.replaceAll("\\s+", "");

        StringBuilder lowerHyphen = new StringBuilder();

        for (String word : words) {
            CaseUtils.toCamelCase(word, true, ' ');

            String hyphen = word.replaceAll("[a-z]+[0-9]*|[A-Z][a-z]+[0-9]*", "-$0-")
                    .replaceFirst("^-+", "")
                    .replaceFirst("-+$", "")
                    .replaceAll("--+", "-")
                    .toLowerCase();

            if(!lowerHyphen.isEmpty()) lowerHyphen.append("-");

            lowerHyphen.append(hyphen);
        }

        return lowerHyphen.toString();
    }

    @Nullable
    public static String getFileExtension(@NotNull String fileName) {
        int index = fileName.lastIndexOf('.') + 1;
        return index == 0 || index == fileName.length() ? null : fileName.substring(index);
    }
}
