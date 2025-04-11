package net.earthcomputer.clientcommands.command.arguments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;

public class FormattedTextArgumentType implements ArgumentType<MutableComponent> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Earth", "bold{xpple}", "bold{italic{red{nwex}}}");

    private FormattedTextArgumentType() {
    }

    public static FormattedTextArgumentType formattedText() {
        return new FormattedTextArgumentType();
    }

    public static MutableComponent getFormattedText(CommandContext<FabricClientCommandSource> context, String arg) {
        return context.getArgument(arg, MutableComponent.class);
    }

    @Override
    public MutableComponent parse(StringReader reader) throws CommandSyntaxException {
        return new Parser(reader).parse();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        StringReader reader = new StringReader(builder.getInput());
        reader.setCursor(builder.getStart());

        Parser parser = new Parser(reader);

        try {
            parser.parse();
        } catch (CommandSyntaxException ignored) {
        }

        if (parser.suggestor != null) {
            parser.suggestor.accept(builder);
        }

        return builder.buildFuture();
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    private static class Parser {
        private final StringReader reader;
        private Consumer<SuggestionsBuilder> suggestor;

        public Parser(StringReader reader) {
            this.reader = reader;
        }

        public MutableComponent parse() throws CommandSyntaxException {
            int cursor = reader.getCursor();
            suggestor = builder -> {
                SuggestionsBuilder newBuilder = builder.createOffset(cursor);
                SharedSuggestionProvider.suggest(FormattedText.FORMATTING.keySet(), newBuilder);
                builder.add(newBuilder);
            };

            String word = reader.readUnquotedString();

            if (FormattedText.FORMATTING.containsKey(word.toLowerCase(Locale.ROOT))) {
                FormattedText.Styler styler = FormattedText.FORMATTING.get(word.toLowerCase(Locale.ROOT));
                suggestor = null;
                reader.skipWhitespace();

                if (!reader.canRead() || reader.peek() != '{') {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "{");
                }
                reader.skip();
                reader.skipWhitespace();
                MutableComponent literalText;
                List<String> arguments = new ArrayList<>();
                if (reader.canRead()) {
                    if (reader.peek() != '}') {
                        if (StringReader.isQuotedStringStart(reader.peek())) {
                            literalText = Component.literal(reader.readQuotedString());
                        } else {
                            literalText = parse();
                        }
                        reader.skipWhitespace();
                        while (reader.canRead() && reader.peek() != '}') {
                            if (arguments.isEmpty()) {
                                suggestor = builder -> {
                                    SuggestionsBuilder newBuilder = builder.createOffset(cursor);
                                    SharedSuggestionProvider.suggest(styler.suggestions, newBuilder);
                                    builder.add(newBuilder);
                                };
                            }
                            if (reader.peek() != ',') {
                                throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, ",");
                            }
                            reader.skip();
                            reader.skipWhitespace();
                            arguments.add(readArgument());
                            reader.skipWhitespace();
                        }
                    } else {
                        literalText = Component.literal("");
                    }
                } else {
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.readerExpectedSymbol().createWithContext(reader, "}");
                }
                reader.skip();

                if (styler.argumentCount != arguments.size()) {
                    reader.setCursor(cursor);
                    reader.readUnquotedString();
                    throw CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(reader);
                }
                return new FormattedText(styler.operator, literalText, arguments).style();
            } else {
                return Component.literal(word + readArgument());
            }
        }

        private String readArgument() {
            final int start = reader.getCursor();
            while (reader.canRead() && isAllowedInArgument(reader.peek())) {
                reader.skip();
            }
            return reader.getString().substring(start, reader.getCursor());
        }

        private static boolean isAllowedInArgument(final char c) {
            return c != ',' && c != '{' && c != '}';
        }
    }

    static class FormattedText {
        private static final Map<String, Styler> FORMATTING = ImmutableMap.<String, Styler>builder()
                .put("aqua", new Styler((s, o) -> s.applyFormat(ChatFormatting.AQUA), 0))
                .put("black", new Styler((s, o) -> s.applyFormat(ChatFormatting.BLACK), 0))
                .put("blue", new Styler((s, o) -> s.applyFormat(ChatFormatting.BLUE), 0))
                .put("bold", new Styler((s, o) -> s.applyFormat(ChatFormatting.BOLD), 0))
                .put("dark_aqua", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_AQUA), 0))
                .put("dark_blue", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_BLUE), 0))
                .put("dark_gray", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_GRAY), 0))
                .put("dark_green", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_GREEN), 0))
                .put("dark_purple", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_PURPLE), 0))
                .put("dark_red", new Styler((s, o) -> s.applyFormat(ChatFormatting.DARK_RED), 0))
                .put("gold", new Styler((s, o) -> s.applyFormat(ChatFormatting.GOLD), 0))
                .put("gray", new Styler((s, o) -> s.applyFormat(ChatFormatting.GRAY), 0))
                .put("green", new Styler((s, o) -> s.applyFormat(ChatFormatting.GREEN), 0))
                .put("italic", new Styler((s, o) -> s.applyFormat(ChatFormatting.ITALIC), 0))
                .put("light_purple", new Styler((s, o) -> s.applyFormat(ChatFormatting.LIGHT_PURPLE), 0))
                .put("obfuscated", new Styler((s, o) -> s.applyFormat(ChatFormatting.OBFUSCATED), 0))
                .put("red", new Styler((s, o) -> s.applyFormat(ChatFormatting.RED), 0))
                .put("reset", new Styler((s, o) -> s.applyFormat(ChatFormatting.RESET), 0))
                .put("strikethrough", new Styler((s, o) -> s.applyFormat(ChatFormatting.STRIKETHROUGH), 0))
                .put("underline", new Styler((s, o) -> s.applyFormat(ChatFormatting.UNDERLINE), 0))
                .put("white",  new Styler((s, o) -> s.applyFormat(ChatFormatting.WHITE), 0))
                .put("yellow", new Styler((s, o) -> s.applyFormat(ChatFormatting.YELLOW), 0))

                .put("font", new Styler((s, o) -> s.withFont(ResourceLocation.tryParse(o.get(0))), 1, "alt", "default"))
                .put("hex", new Styler((s, o) -> s.withColor(TextColor.fromRgb(Integer.parseInt(o.get(0), 16))), 1))
                .put("insert", new Styler((s, o) -> s.withInsertion(o.get(0)), 1))

                .put("click", new Styler((s, o) -> s.withClickEvent(new ClickEvent(ClickEvent.Action.getByName(o.get(0)), o.get(1))), 2, "change_page", "copy_to_clipboard", "open_file", "open_url", "run_command", "suggest_command"))
                .put("hover", new Styler((s, o) -> s.withHoverEvent(HoverEvent.Action.getByName(o.get(0)).deserializeFromLegacy(Component.nullToEmpty(o.get(1)))), 2, "show_entity", "show_item", "show_text"))

                // aliases
                .put("strike", new Styler((s, o) -> s.applyFormat(ChatFormatting.STRIKETHROUGH), 0))
                .put("magic", new Styler((s, o) -> s.applyFormat(ChatFormatting.OBFUSCATED), 0))
                .build();

        private final BiFunction<Style, List<String>, Style> styler;
        private final MutableComponent argument;
        private final List<String> optional;

        public FormattedText(BiFunction<Style, List<String>, Style> styler, MutableComponent argument, List<String> optional) {
            this.styler = styler;
            this.argument = argument;
            this.optional = optional;
        }

        public MutableComponent style() {
            return this.argument.setStyle(this.styler.apply(this.argument.getStyle(), this.optional));
        }

        private record Styler(BiFunction<Style, List<String>, Style> operator, int argumentCount, String... suggestions) {}
    }
}
