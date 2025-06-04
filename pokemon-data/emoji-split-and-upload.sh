#!/bin/bash

DISCORD_TOKEN="your_token_here"
APPLICATION_ID="your_application_id_here"
API_URL="https://discord.com/api/v10"
OUTPUT_FILE="emoji_ids.json"
BASE_URL="https://raw.githubusercontent.com/DV8FromTheWorld/discord-pokedex/main/images/thumbnails"

# Clear or create JSON output
echo "{" > "$OUTPUT_FILE"
first_entry=true

# Loop through 001 to 151
for i in $(seq -w 001 151); do
  int=$((10#$i))               # Converts 001 -> 1
  dir="emoji-processing/$int"
  original="$dir/${int}.png"
  resized="$dir/${int}_resized.png"
  split_prefix="$dir/${int}_part"

  mkdir -p "$dir"

  echo "🌐 Downloading image for #$i..."
  curl -s -L -o "$original" "$BASE_URL/$i.png"

  if [ ! -f "$original" ]; then
    echo "❌ Failed to download $BASE_URL/$i.png"
    continue
  fi

  echo "🖼 Resizing $original to 256x256..."
  magick convert "$original" -resize 256x256! "$resized"

  echo "🔪 Splitting into 4 quadrants..."
  magick "$resized" -crop 2x2@ +repage +adjoin "${split_prefix}_%d.png"

  # Upload each part
  for part in {0..3}; do
    split_img="${split_prefix}_${part}.png"
    name="${int}_part${part}"

    echo "⬆️  Uploading $name..."

    if [[ "$OSTYPE" == "darwin"* ]]; then
      b64image=$(base64 -i "$split_img")
    else
      b64image=$(base64 -w 0 "$split_img")
    fi

    json_payload=$(jq -n \
      --arg name "$name" \
      --arg image "data:image/png;base64,$b64image" \
      '{name: $name, image: $image}')

    response=$(curl -s -X POST "$API_URL/applications/$APPLICATION_ID/emojis" \
      -H "Authorization: Bot $DISCORD_TOKEN" \
      -H "Content-Type: application/json" \
      -d "$json_payload")

    emoji_id=$(echo "$response" | jq -r '.id // empty')

    if [ -n "$emoji_id" ]; then
      echo "✅ Uploaded $name with ID $emoji_id"
      if [ "$first_entry" = true ]; then
        first_entry=false
      else
        echo "," >> "$OUTPUT_FILE"
      fi
      echo "  \"$name\": \"$emoji_id\"" >> "$OUTPUT_FILE"
    else
      echo "❌ Failed to upload $name"
      echo "Response: $response"
    fi
  done
done

echo "" >> "$OUTPUT_FILE"
echo "}" >> "$OUTPUT_FILE"

echo "🎉 Finished uploading all emojis. Mapping saved to $OUTPUT_FILE"