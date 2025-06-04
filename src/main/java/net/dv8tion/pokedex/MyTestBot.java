package net.dv8tion.pokedex;

import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.container.Container;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.mediagallery.MediaGallery;
import net.dv8tion.jda.api.components.mediagallery.MediaGalleryItem;
import net.dv8tion.jda.api.components.section.Section;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.thumbnail.Thumbnail;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.utils.FileUpload;

import java.util.*;
import java.util.stream.Collectors;

class MyTestBot extends ListenerAdapter {
    public static final String HOSTED_ASSETS_ROOT = "https://raw.githubusercontent.com/DV8FromTheWorld/discord-pokedex/refs/heads/main";

    private static final int POKEMON_PER_PAGE = 10;

    /*
        data sources: https://github.com/Purukitto/pokemon-data.json
        image sources: https://www.kaggle.com/datasets/vishalsubbiah/pokemon-images-and-types
     */
    public Pokedex pokedex = new Pokedex();

    public static void main(String[] args) {
        String token = System.getenv("DISCORD_TOKEN");
        if (token == null) {
            throw new RuntimeException("DISCORD_TOKEN not set");
        }

        JDABuilder builder = JDABuilder.createDefault(token)
            .addEventListeners(new MyTestBot());

        // Initialize the bot
        JDA jda = builder.build();

        // Create our commands
        jda.updateCommands().addCommands(
            Commands.slash("pokedex", "Shows the pokédex for the OG 151 pokemon")
                .setIntegrationTypes(IntegrationType.USER_INSTALL),

            Commands.slash("pokedex-lookup", "Lookup pokemon by their id")
                .setIntegrationTypes(IntegrationType.USER_INSTALL)
                .addOptions(
                    new OptionData(OptionType.INTEGER, "pokemon-id", "The id of the pokemon (1 - 151)")
                        .setMinValue(1)
                        .setMaxValue(Pokedex.LAST_POKEMON_ID_IN_FIRST_GEN)
                        .setAutoComplete(true)
                        .setRequired(true)
                )

        ).queue();
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "pokedex": {
                event.replyComponents(getPokedex(1))
                    .useComponentsV2()
                    .queue();

                return;
            }
            case "pokedex-lookup": {
                int pokemonId = event.getOption("pokemon-id").getAsInt();

                event.replyComponents(getPokemonCard(pokemonId))
                    .useComponentsV2()
                    .queue();
                return;
            }
        }

        event.reply("This command has not been handled: " + event.getName()).queue();
    }

    @Override
    public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
        if (event.getName().equals("pokedex-lookup")) {
            String typedValue = event.getFocusedOption().getValue();

            // If the value typed is empty, show first 25 pokemon
            // if the value is not empty, and is an int, try to match
            // if the value is not empty and is not an int, get angry
            if (typedValue.isBlank()) {
                List<Command.Choice> choices = pokedex.getPokemonMap().values().stream().limit(25).map(pokemonData -> {
                        return new Command.Choice(pokemonData.getName(), pokemonData.getId());
                    })
                    .toList();

                event.replyChoices(choices).queue();
                return;
            }

            List<Command.Choice> choices =pokedex.getPokemonMap().values().stream()
                .filter(pokemonData -> String.valueOf(pokemonData.getId()).contains(typedValue))
                .limit(25)
                .map(pokemonData -> {
                    return new Command.Choice(pokemonData.getName(), pokemonData.getId());
                })
                .toList();

            event.replyChoices(choices).queue();
            return;
        }


        // TODO - show that there were no options
        event.replyChoices().queue();
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        // pokemon-card--133
        String componentId = event.getComponentId();
        if (componentId.startsWith("pokemon-card")) {
            int pokemonId = Integer.parseInt(componentId.split("--")[1]);

            int targetPokedexPage = (pokemonId / POKEMON_PER_PAGE) + (pokemonId % POKEMON_PER_PAGE == 0 ? 0 : 1);

            event.editComponents(
                    getPokemonCard(pokemonId),
                    ActionRow.of(
                        Button.secondary(makePokedexId(targetPokedexPage), "Return to Pokédex")
                    )
                )
                .useComponentsV2()
                .queue();

            return;
        }
        // pokedex--1
        else if (componentId.startsWith("pokedex")) {
            int page = Integer.parseInt(componentId.split("--")[1]);
            event.editComponents(getPokedex(page)).useComponentsV2().queue();
            return;
        }

        event.reply("This button has not been handled: " + event.getComponentId()).queue();
    }

    private Container getPokemonCard(int pokemonId) {
        PokemonData pokemonData = pokedex.getPokemonMap().get(pokemonId);

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(
            Section.of(
                Thumbnail.fromUrl(
                    pokemonData.getImages().getThumbnailUrl()
                ),
                TextDisplay.of(String.format(
                    "## %s\n%s",
                    pokemonData.getName(),
                    pokemonData.getDescription()
                ))
            )
        );

        // Add evolution information
        if (pokemonData.hasEvolutions()) {
            children.add(Separator.createDivider(Separator.Spacing.SMALL));

            PokemonData.Evolution prevEvolution = pokemonData.getPreviousEvolution();
            List<PokemonData.Evolution> nextEvolutions = pokemonData.getNextEvolutions();

            if (prevEvolution != null) {
                children.add(TextDisplay.of("**Previous Evolution**"));
                children.add(getPokemonRow(prevEvolution.getPokemonId()));
            }
            if (nextEvolutions != null) {
                String header = nextEvolutions.size() == 1
                    ? "**Next Evolution**"
                    : "**Next Evolutions**";

                children.add(TextDisplay.of(header));
                nextEvolutions.forEach(evolution -> {
                    String criteriaDescription = "-# Criteria: " + evolution.getCriteria();
                    children.add(getPokemonRow(evolution.getPokemonId(), criteriaDescription));
                });
            }

            children.add(Separator.createInvisible(Separator.Spacing.SMALL));
        }

        List<MediaGalleryItem> mediaItems = pokemonData.getImages().getRandomMediaImages(4)
            .stream()
            .map(path -> MediaGalleryItem.fromFile(FileUpload.fromData(path)))
            .toList();

        children.addAll(List.of(
            MediaGallery.of(mediaItems),
            Section.of(
                Button.secondary(makePokemonCardId(pokemonId), "More Images").withEmoji(Emoji.fromUnicode("\uD83D\uDD04")),
                TextDisplay.of(
                    "-# Data for this pokemon comes from [pokemon.json](https://github.com/Purukitto/pokemon-data.json) and images from [dataset](https://www.kaggle.com/datasets/vishalsubbiah/pokemon-images-and-types)."
                )
            )
        ));

        return Container.of(children);
    }

    private Container getPokedex(int currentPage) {
        int totalPokemon = pokedex.getTotalPokemon();

        // 123 / 10 -> 12 | 3 -> 1 | 13
        // 120 / 10 -> 12 | 0 -> 0 | 12
        int totalPages = (totalPokemon / POKEMON_PER_PAGE) + (totalPokemon % POKEMON_PER_PAGE == 0 ? 0 : 1);

        // 1: 1 - 10, 11- 20, 21 - 30
        int firstPokemonId = 1 + ((currentPage - 1) * POKEMON_PER_PAGE);
        int lastPokemonId = firstPokemonId + (POKEMON_PER_PAGE - 1);

        List<Section> selectedPokemon = pokedex.getPokemonMap().values()
            .stream()
            .filter(pokemonData -> pokemonData.getId() >= firstPokemonId && pokemonData.getId() <= lastPokemonId)
            .map(pokemonData -> getPokemonRow(pokemonData.getId())).toList();

        List<ContainerChildComponent> children = new ArrayList<>();
        children.add(MediaGallery.of(
            MediaGalleryItem.fromUrl(
                Pokedex.POKEDEX_HEADER_URL
            )
        ));

        children.addAll(selectedPokemon);

        int nextPage = currentPage + 1;
        int prevPage = currentPage - 1;
        String paginatorLabel = String.format("Page (%d / %d)", currentPage, totalPages);

        children.add(
            ActionRow.of(
                Button.primary(makePokedexId(prevPage), "Prev").withDisabled(prevPage <= 0),
                Button.secondary("ignore", paginatorLabel).withDisabled(true),
                Button.primary(makePokedexId(nextPage), "Next").withDisabled(nextPage > totalPages)
            )
        );

        return Container.of(children);
    }

    private Section getPokemonRow(int pokemonId) {
        return getPokemonRow(pokemonId, null);
    }

    private Section getPokemonRow(int pokemonId, String extraDescription) {
        PokemonData pokemonData = pokedex.getPokemonMap().get(pokemonId);

        // [<:a:ID>, <:a:ID>, <:a:ID>, <:a:ID>]
        // 0, 1
        // 2, 3
        List<String> emojiIds = pokemonData.getImages().getThumbnailEmojiMentions();

        String rowDescription = String.format(
            "**%s%s   %s**\n%s%s   %s - %s",
            emojiIds.get(0), emojiIds.get(1),
            pokemonData.getName(),

            emojiIds.get(2), emojiIds.get(3),
            pokemonData.getSpecies(),
            pokemonData.getTypes().stream().collect(Collectors.joining(", "))
        );

        if (extraDescription != null) {
            rowDescription += "\n" + extraDescription;
        }

        return Section.of(
            Button.secondary(makePokemonCardId(pokemonData.getId()), "View"),
            TextDisplay.of(rowDescription)
        );
    }

    private String makePokedexId(int page) {
        return "pokedex--" + page;
    }

    private String makePokemonCardId(int pokemonId) {
        return "pokemon-card--" + pokemonId;
    }
}
