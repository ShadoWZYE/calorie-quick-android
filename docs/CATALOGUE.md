# Built-in catalogue

The offline catalogue is generated from `tools/build_catalogue.py` into
`app/src/main/assets/catalogue/built_in_catalogue_v1.json`. The JSON asset—not
Kotlin source—is the versioned product data contract.

## Source and licensing

The initial generic-food values are a reviewed, rounded snapshot based on
[USDA FoodData Central](https://fdc.nal.usda.gov/). FoodData Central publishes
its data under [CC0 1.0](https://fdc.nal.usda.gov/api-guide.html). The app keeps
the requested attribution even though CC0 does not require permission.

The `sourceId` values beginning with `usda-fdc-cc0:curated-v1:` are internal
snapshot provenance tags, not USDA FDC IDs. Before a future automated USDA
import assigns an actual FDC ID, it must verify the selected data type, food
description, nutrient basis, and household weights.

Generic entries are reference estimates. Branded foods, recipes, cultivars,
water gain/loss, drained weight, and cooking fat can materially change values.
Barcode lookup and personal foods remain the preferred paths for a known
packaged product.

## Contract

Each food has:

- a stable lowercase ID and English/Romanian names;
- a normalized category and searchable aliases;
- calories, protein, carbohydrate, fat, and known/unknown fiber per 100 g;
- optional household servings, allergen declarations, and preparation variants;
- stable serving/preparation IDs so interaction history survives updates.

The Android parser rejects duplicate or malformed IDs, invalid nutrient ranges,
bad serving weights, unknown allergen values, and missing default preparations.
Import is transactional and updates built-in rows without replacing a personal
food that happens to use the same ID.

## Updating the snapshot

1. Edit the reviewed records in `tools/build_catalogue.py`.
2. Increase the catalogue `version` for any shipped data change.
3. Run `python tools/build_catalogue.py`.
4. Run `./gradlew test lintDebug assembleDebug`.
5. Review bilingual names, aliases, allergen declarations, preparation basis,
   and serving weights before shipping.

Images are deliberately separate from nutrient provenance. Only assets with a
documented redistribution license and attribution record should be bundled.

## Open Food Facts staging

Successful barcode and text-search responses are requested without a field projection, cached locally for 30 days, and reused before another network request. The complete returned JSON is retained after it becomes stale, so it remains useful as an offline fallback and as evidence for catalogue growth. Review exports bundle the cache index and response wrappers under `open-food-facts/`; the hashed keys do not expose search phrases in the index.

Products that pass the app parser are appended automatically as non-personal `OPEN_FOOD_FACTS` rows in the device catalogue. Existing built-in and personal barcode matches are protected from replacement. Repeat API responses may refresh their matching API row, while diary entries retain the nutrition captured when they were logged.

On the review PC, bundle import retains the full archive and automatically merges unique barcodes into the private `shared-food-catalogue.json` staging database. Opted-in personal foods without barcodes use a normalized name-and-brand identity. This is the source pool for later built-in snapshots or a central service; routine records do not require manual approval. Deliberate review is reserved for products users explicitly flag as incorrect from quick add. Those reports carry structured reason codes and an optional note, and the inbox links them to every cached raw response for the same barcode.
