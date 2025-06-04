package net.dv8tion.pokedex;

import net.dv8tion.jda.api.utils.data.DataArray;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static net.dv8tion.pokedex.MyTestBot.HOSTED_ASSETS_ROOT;

public class PokemonData
{
    public static final Random rand = new Random();

    private final DataObject data;
    private final Stats stats;
    private final Images images;

    // We are using Optional to simulate a 3 state lazy-load system:
    // 1. null           -> Evolutions never accessed. Requires a load.
    // 2. Optional(null) -> Evolutions loaded. None present
    // 3. Optional(data) -> Evolutions loaded. Pokemon has evolution
    private Optional<Evolution> prevEvolution;
    private Optional<List<Evolution>> nextEvolution;

    public PokemonData(DataObject data, DataObject emojiData) {
        this.data = data;
        this.stats = new Stats(this.data.getObject("base"));
        this.images = new Images(this.data.getObject("image"), emojiData);
    }

    public int getId() {
        return this.data.getInt("id");
    }

    public String getName() {
        return this.data.getObject("name").getString("english");
    }

    private String getMediaFolderName() {
        // These Pokémon specifically have names that don't map well to our folder structure
        // that contains the additional images
        switch (this.getName()) {
            case "Nidoran♀":
            case "Nidorina": return "Nidorina";
            case "Nidoran♂":
            case "Nidorino": return "Nidorino";
            case "Mr. Mime": return "MrMime";
            case "Farfetch'd": return "Farfetchd";
        }

        String first = getName().substring(0, 1).toUpperCase();
        return first + getName().substring(1).toLowerCase();
    }

    public String getDescription() {
        return this.data.getString("description");
    }

    public String getSpecies() {
        return this.data.getString("species");
    }

    public List<String> getTypes() {
        return this.data.getArray("type")
                .stream(DataArray::getString)
                .collect(Collectors.toList());
    }

    public Stats getStats() {
        return stats;
    }

    public Images getImages() {
        return images;
    }

    public DataObject getRawData() {
        return data;
    }

    public boolean hasEvolutions() {
        return this.getPreviousEvolution() != null || this.getNextEvolutions() != null;
    }

    public Evolution getPreviousEvolution() {
        if (prevEvolution == null) {
            DataArray array = this.data.getObject("evolution")
                .optArray("prev")
                .orElse(null);

            prevEvolution = Optional.ofNullable(makeEvolution(array));
        }

        return prevEvolution.orElse(null);
    }

    public List<Evolution> getNextEvolutions() {
        if (nextEvolution == null) {
            DataArray parentArray = this.data.getObject("evolution")
                .optArray("next")
                .orElse(null);

            if (parentArray == null) {
                nextEvolution = Optional.empty();
                return null;
            }

            nextEvolution = Optional.of(parentArray
                .stream(DataArray::getArray)
                .map(this::makeEvolution)
                .filter(Objects::nonNull)
                .collect(Collectors.toList())
            );
        }

        return nextEvolution.orElse(null);
    }

    private Evolution makeEvolution(DataArray evolutionData) {
        if (evolutionData == null) {
            return null;
        }

        int pokemonId = evolutionData.getInt(0);
        if (pokemonId > Pokedex.LAST_POKEMON_ID_IN_FIRST_GEN) {
            return null;
        }

        String criteria = evolutionData.getString(1);

        return new Evolution(
            pokemonId,
            criteria
        );
    }

    public class Images {
        private static final String LOCAL_MEDIA_IMAGES_FOLDER = "pokemon-data/images/media-gallery";
        private static final String HOSTED_MEDIA_IMAGES_FOLDER = HOSTED_ASSETS_ROOT + "/pokemon-data/images/media-gallery";

        private final DataObject data;
        private final List<Path> mediaImages = new ArrayList<>();
        private final List<String> mediaImagesUrls = new ArrayList<>();
        private final List<String> emojiThumbnailIds = new ArrayList<>();

        public Images(DataObject data, DataObject emojiData) {
            this.data = data;

            Path expandedImagesPath = Paths.get(LOCAL_MEDIA_IMAGES_FOLDER, getMediaFolderName());
            if (!Files.exists(expandedImagesPath)) {
                throw new RuntimeException(String.format("Failed to find expanded images directory: %s", expandedImagesPath.toAbsolutePath()));
            }

            // iterate all files
            try (Stream<Path> stream = Files.walk(expandedImagesPath))
            {
                // Store images for upload
                stream
                    .filter(path -> path.toString().contains("--compressed"))
                    .forEach(mediaImages::add);

                // However, these images are also on the "cdn" (e.g: Github), so we can directly reference them if desired.
                mediaImages
                    .stream()
                    .map(path -> path.toString().replace(LOCAL_MEDIA_IMAGES_FOLDER, HOSTED_MEDIA_IMAGES_FOLDER))
                    .forEach(mediaImagesUrls::add);
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            for (int i = 0; i < 4; i++) {
                String key = getId() + "_part" + i;
                emojiThumbnailIds.add("<:a:" + emojiData.getString(key) + ">");
            }
        }

        public String getThumbnailUrl() {
            return this.data.getString("thumbnail");
        }

        public List<String> getThumbnailEmojiMentions() {
            return emojiThumbnailIds;
        }

        public String getHighResUrl() {
            return this.data.getString("hires");
        }

        /**
         * Selects a random set of media images for this pokemon.
         *
         * @param imageCount
         *
         * @return
         */
        public Set<Path> getRandomMediaImages(int imageCount) {
            return selectRandomItems(mediaImages, imageCount);
        }

        public Set<String> getRandomMediaImageUrls(int imageCount) {
            return selectRandomItems(mediaImagesUrls, imageCount);
        }

        private <T> Set<T> selectRandomItems(List<T> source, int itemCount) {
            Set<T> selectedItems = new HashSet<>(itemCount);
            while (selectedItems.size() < itemCount) {
                selectedItems.add(source.get(rand.nextInt(source.size())));
            }

            return selectedItems;
        }
    }

    public static class Evolution {
        private final int pokemonId;
        private final String criteria;

        public Evolution(int pokemonId, String criteria) {
            this.pokemonId = pokemonId;
            this.criteria = criteria;
        }

        public int getPokemonId() {
            return pokemonId;
        }

        public String getCriteria() {
            return criteria;
        }
    }

    public static class Stats {
        private final DataObject data;

        public Stats(DataObject data) {
            this.data = data;
        }

        public int getHP() {
            return this.data.getInt("HP");
        }

        public int getAttack() {
            return this.data.getInt("Attack");
        }

        public int getDefense() {
            return this.data.getInt("Defense");
        }

        public int getSpeed() {
            return this.data.getInt("Speed");
        }
    }
}
