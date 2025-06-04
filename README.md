# discord-pokedex
A demo Pokédex application for Discord, written during a live stream community developers event in the official [Discord Developers](https://discord.com/invite/discord-developers) server. 
The application is written in Java using [JDA](https://github.com/discord-jda/jda).

The event, and this application, were to show off the new [Message Components](https://discord.com/developers/docs/components/reference) that [Discord launched for developers](https://discord.com/developers/docs/change-log#introducing-new-components-for-messages) 
to build new and more feature-full Application experiences on the platform.

The live stream event was recorded and is available here
https://www.youtube.com/watch?v=GVeIqO0pGE4

## Example Screenshots
|                                                     |                                                          |
|-----------------------------------------------------|----------------------------------------------------------|
| <img src="/.github/images/pokedex.png" width="450"> | <img src="/.github/images/pokemon-card.png" width="450"> |

## License
The code provided in this repo is [licensed under MIT](/LICENSE). Feel free to use as you wish.

On the other hand, the data, images, and IP related to Pokémon belong to GameFreak and The Pokémon Company. 
Their use in this project is based on Fair Use. The above MIT license does not provide and rights to that data.

## Acknowledgements
The data present in the [/pokemon-data](/pokemon-data) folder is sourced from the following locations

### Purukitto's [Pokemon.json](https://github.com/Purukitto/pokemon-data.json)
- [/pokemon-data/pokedex.json](/pokemon-data/pokedex.json)
- [/pokemon-data/images/hires](/pokemon-data/images/hires)
- [/pokemon-data/images/sprites](/pokemon-data/images/sprites)
- [/pokemon-data/images/thumbnails](/pokemon-data/images/thumbnails)
- [/pokemon-data/images/emojis](/pokemon-data/images/emojis)
  - Images sourced from [/pokemon-data/images/thumbnails](//pokemon-data/images/thumbnails) and processed via [imagemagick](https://imagemagick.org/index.php). 
    Script for processing and uploading to Discord available [here](/pokemon-data/emoji-split-and-upload.sh).


### Vishalsubbiah's [pokemon-images-and-types dataset](https://www.kaggle.com/datasets/vishalsubbiah/pokemon-images-and-types)
- [/pokemon-data/images/media-gallery](/pokemon-data/images/media-gallery)