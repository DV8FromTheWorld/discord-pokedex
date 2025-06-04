package net.dv8tion.pokedex;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import static net.dv8tion.pokedex.MyTestBot.HOSTED_ASSETS_ROOT;

public class Pokedex
{
    public static final int LAST_POKEMON_ID_IN_FIRST_GEN = 151;
    public static final String POKEDEX_HEADER_URL = HOSTED_ASSETS_ROOT + "pokemon-data/images/pokedex-header.webp";

    private static final String POKEDEX_DATA_FILE = "pokemon-data/pokedex.json";
    private static final String EMOJI_DATA_FILE = "pokemon-data/emoji_ids.json";

    private final HashMap<Integer, PokemonData> pokedexData = new HashMap<>();

    public Pokedex()
    {
        // Load the Pokémon data and the referenced emoji data
        try (InputStream inputStream = Files.newInputStream(Paths.get(POKEDEX_DATA_FILE));
             InputStream emojiInputStream = Files.newInputStream(Paths.get(EMOJI_DATA_FILE)))
        {
            DataArray pokemonData = DataArray.fromJson(inputStream);
            DataObject emojiData = DataObject.fromJson(emojiInputStream);

            for (int i = 0; i < pokemonData.length(); i++) {
                DataObject data = pokemonData.getObject(i);
                PokemonData pokemon = new PokemonData(data, emojiData);
                pokedexData.put(pokemon.getId(), pokemon);

                // We only support the first gen
                if (pokemon.getId() >= LAST_POKEMON_ID_IN_FIRST_GEN) {
                    break;
                }
            }
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getTotalPokemon() {
        return pokedexData.size();
    }

    public HashMap<Integer, PokemonData> getPokemonMap() {
        return pokedexData;
    }

    public PokemonData getPokemon(int id) {
        return pokedexData.get(id);
    }
}
