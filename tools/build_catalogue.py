"""Build the reviewed, deterministic built-in food catalogue asset."""

import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "app" / "src" / "main" / "assets" / "catalogue" / "built_in_catalogue_v1.json"


def nutrition(kcal, protein, carbs, fat, fiber):
    return {
        "calories": kcal,
        "proteinGrams": protein,
        "carbsGrams": carbs,
        "fatGrams": fat,
        "fiberGrams": fiber,
    }


def serving(food_id, key, en, ro, grams, amounts=(1,)):
    return {
        "id": f"{food_id}-{key}",
        "label": {"en": en, "ro": ro},
        "grams": grams,
        "suggestedAmounts": list(amounts),
    }


def preparation(food_id, key, en, ro, values):
    return {
        "id": f"{food_id}|{key}",
        "name": {"en": en, "ro": ro},
        "nutritionPer100g": nutrition(*values),
    }


def food(
    food_id,
    en,
    ro,
    detail_en,
    detail_ro,
    category,
    values,
    *,
    aliases=(),
    servings=(),
    allergens=None,
    preparations=(),
    default_preparation=None,
):
    record = {
        "id": food_id,
        "name": {"en": en, "ro": ro},
        "detail": {"en": detail_en, "ro": detail_ro},
        "category": category,
        "sourceId": f"usda-fdc-cc0:curated-v1:{food_id}",
        "aliases": list(aliases),
        "nutritionPer100g": nutrition(*values),
        "servings": list(servings),
        "allergens": allergens or {},
        "preparations": list(preparations),
    }
    if default_preparation:
        record["defaultPreparationId"] = f"{food_id}|{default_preparation}"
    return record


FOODS = [
    # Fruit
    food("apple", "Apple", "Măr", "Fresh, with skin", "Proaspăt, cu coajă", "fruit", (52, .3, 13.8, .2, 2.4), aliases=("mere",), servings=(serving("apple", "medium", "1 medium apple", "1 măr mediu", 182),)),
    food("banana", "Banana", "Banană", "Fresh", "Proaspătă", "fruit", (89, 1.1, 22.8, .3, 2.6), aliases=("banane",), servings=(serving("banana", "medium", "1 medium banana", "1 banană medie", 118), serving("banana", "small", "1 small banana", "1 banană mică", 101))),
    food("orange", "Orange", "Portocală", "Fresh", "Proaspătă", "fruit", (47, .9, 11.8, .1, 2.4), aliases=("portocale",), servings=(serving("orange", "medium", "1 medium orange", "1 portocală medie", 131),)),
    food("pear", "Pear", "Pară", "Fresh, with skin", "Proaspătă, cu coajă", "fruit", (57, .4, 15.2, .1, 3.1), aliases=("pere",), servings=(serving("pear", "medium", "1 medium pear", "1 pară medie", 178),)),
    food("strawberries", "Strawberries", "Căpșuni", "Fresh", "Proaspete", "fruit", (32, .7, 7.7, .3, 2.0), aliases=("strawberry", "capsuni"), servings=(serving("strawberries", "cup", "1 cup", "1 cană", 152, (.5, 1, 2)),)),
    food("blueberries", "Blueberries", "Afine", "Fresh", "Proaspete", "fruit", (57, .7, 14.5, .3, 2.4), aliases=("blueberry",), servings=(serving("blueberries", "cup", "1 cup", "1 cană", 148, (.5, 1, 2)),)),
    food("grapes", "Grapes", "Struguri", "Fresh", "Proaspeți", "fruit", (69, .7, 18.1, .2, .9), aliases=("grape",), servings=(serving("grapes", "cup", "1 cup", "1 cană", 151, (.5, 1, 2)),)),
    food("watermelon", "Watermelon", "Pepene verde", "Fresh", "Proaspăt", "fruit", (30, .6, 7.6, .2, .4), aliases=("pepene rosu",), servings=(serving("watermelon", "cup", "1 cup diced", "1 cană cuburi", 152, (.5, 1, 2)),)),
    food("avocado", "Avocado", "Avocado", "Fresh", "Proaspăt", "fruit", (160, 2.0, 8.5, 14.7, 6.7), servings=(serving("avocado", "half", "1/2 avocado", "1/2 avocado", 100, (.5, 1, 2)),)),
    food("lemon", "Lemon", "Lămâie", "Fresh", "Proaspătă", "fruit", (29, 1.1, 9.3, .3, 2.8), aliases=("lamaie",), servings=(serving("lemon", "medium", "1 medium lemon", "1 lămâie medie", 58),)),
    # Vegetables
    food("tomato", "Tomato", "Roșie", "Raw", "Crudă", "vegetables", (18, .9, 3.9, .2, 1.2), aliases=("tomatoes", "rosie", "rosii"), servings=(serving("tomato", "medium", "1 medium tomato", "1 roșie medie", 123),)),
    food("cucumber", "Cucumber", "Castravete", "Raw, with peel", "Crud, cu coajă", "vegetables", (15, .7, 3.6, .1, .5), aliases=("cucumbers", "castraveti"), servings=(serving("cucumber", "medium", "1 medium cucumber", "1 castravete mediu", 201),)),
    food("onion", "Onion", "Ceapă", "Raw", "Crudă", "vegetables", (40, 1.1, 9.3, .1, 1.7), aliases=("onions",), servings=(serving("onion", "medium", "1 medium onion", "1 ceapă medie", 110),)),
    food("garlic", "Garlic", "Usturoi", "Raw", "Crud", "vegetables", (149, 6.4, 33.1, .5, 2.1), servings=(serving("garlic", "clove", "1 clove", "1 cățel", 3, (1, 2, 3, 4)),)),
    food("carrot", "Carrot", "Morcov", "Raw", "Crud", "vegetables", (41, .9, 9.6, .2, 2.8), aliases=("carrots", "morcovi"), servings=(serving("carrot", "medium", "1 medium carrot", "1 morcov mediu", 61),)),
    food("broccoli", "Broccoli", "Broccoli", "Raw", "Crud", "vegetables", (34, 2.8, 6.6, .4, 2.6), servings=(serving("broccoli", "cup", "1 cup chopped", "1 cană mărunțit", 91, (.5, 1, 2)),)),
    food("spinach", "Spinach", "Spanac", "Raw", "Crud", "vegetables", (23, 2.9, 3.6, .4, 2.2), servings=(serving("spinach", "cup", "1 cup", "1 cană", 30, (1, 2, 3)),)),
    food("bell-pepper", "Bell pepper", "Ardei gras", "Raw", "Crud", "vegetables", (31, 1.0, 6.0, .3, 2.1), aliases=("sweet pepper", "capsicum", "ardei"), servings=(serving("bell-pepper", "medium", "1 medium pepper", "1 ardei mediu", 119),)),
    food("cabbage", "Cabbage", "Varză", "Raw", "Crudă", "vegetables", (25, 1.3, 5.8, .1, 2.5), aliases=("varza",), servings=(serving("cabbage", "cup", "1 cup chopped", "1 cană mărunțită", 89, (.5, 1, 2)),)),
    food("mushrooms", "White mushrooms", "Ciuperci champignon", "Raw", "Crude", "vegetables", (22, 3.1, 3.3, .3, 1.0), aliases=("mushroom", "ciuperci albe"), servings=(serving("mushrooms", "cup", "1 cup sliced", "1 cană felii", 70, (1, 2, 3)),)),
    food("zucchini", "Zucchini", "Dovlecel", "Raw", "Crud", "vegetables", (17, 1.2, 3.1, .3, 1.0), aliases=("courgette", "dovlecei"), servings=(serving("zucchini", "medium", "1 medium zucchini", "1 dovlecel mediu", 196),)),
    food("eggplant", "Eggplant", "Vânătă", "Raw", "Crudă", "vegetables", (25, 1.0, 5.9, .2, 3.0), aliases=("aubergine", "vanata", "vinete")),
    food("potato", "Potato", "Cartof", "Cooked flesh and skin", "Gătit, pulpă și coajă", "vegetables", (87, 1.9, 20.1, .1, 1.8), aliases=("potatoes", "cartofi"), preparations=(preparation("potato", "boiled", "Boiled", "Fiert", (87, 1.9, 20.1, .1, 1.8)), preparation("potato", "baked", "Baked", "Copt", (93, 2.5, 21.2, .1, 2.2))), default_preparation="boiled"),
    food("sweet-potato", "Sweet potato", "Cartof dulce", "Cooked", "Gătit", "vegetables", (90, 2.0, 20.7, .2, 3.3), aliases=("sweet potatoes", "cartofi dulci")),
    # Grains and bakery
    food("oats", "Rolled oats", "Fulgi de ovăz", "Dry", "Uscați", "grains", (379, 13.2, 67.7, 6.5, 10.1), aliases=("oatmeal", "ovaz"), servings=(serving("oats", "half-cup", "1/2 cup dry", "1/2 cană uscată", 40, (.5, 1, 2)),), allergens={"GLUTEN": "MAY_CONTAIN"}),
    food("rice", "White rice", "Orez alb", "Cooked", "Gătit", "grains", (130, 2.7, 28.2, .3, .4), aliases=("cooked white rice", "orez fiert")),
    food("brown-rice", "Brown rice", "Orez brun", "Cooked", "Gătit", "grains", (123, 2.7, 25.6, 1.0, 1.6), aliases=("wholegrain rice", "orez integral")),
    food("pasta", "Pasta", "Paste", "Cooked, plain", "Fierte, simple", "grains", (158, 5.8, 30.9, .9, 1.8), aliases=("spaghetti", "macaroni", "macaroane"), allergens={"GLUTEN": "CONTAINS"}),
    food("white-bread", "White bread", "Pâine albă", "Commercially prepared", "Preparată comercial", "bakery", (266, 8.9, 49.4, 3.3, 2.7), aliases=("paine alba",), servings=(serving("white-bread", "slice", "1 slice", "1 felie", 25, (1, 2, 3, 4)),), allergens={"GLUTEN": "CONTAINS"}),
    food("whole-wheat-bread", "Whole-wheat bread", "Pâine integrală", "Commercially prepared", "Preparată comercial", "bakery", (247, 13.0, 41.0, 3.4, 6.8), aliases=("wholemeal bread", "paine integrala"), servings=(serving("whole-wheat-bread", "slice", "1 slice", "1 felie", 28, (1, 2, 3, 4)),), allergens={"GLUTEN": "CONTAINS"}),
    food("cornmeal", "Cornmeal", "Mălai", "Whole-grain, dry", "Integral, uscat", "grains", (370, 7.1, 79.5, 1.8, 3.9), aliases=("malai", "polenta flour")),
    food("polenta", "Polenta", "Mămăligă", "Cooked with water", "Fiartă cu apă", "grains", (70, 1.5, 15.6, .4, .8), aliases=("mamaliga",)),
    food("quinoa", "Quinoa", "Quinoa", "Cooked", "Fiartă", "grains", (120, 4.4, 21.3, 1.9, 2.8)),
    # Meat, fish, eggs, plant protein
    food("chicken-breast", "Chicken breast", "Piept de pui", "Cooked, skinless", "Gătit, fără piele", "meat", (165, 31.0, 0.0, 3.6, 0.0), aliases=("chicken fillet", "pui"), preparations=(preparation("chicken-breast", "roasted", "Roasted", "Copt", (165, 31.0, 0.0, 3.6, 0.0)), preparation("chicken-breast", "grilled", "Grilled", "La grătar", (165, 31.0, 0.0, 3.6, 0.0))), default_preparation="roasted"),
    food("turkey-breast", "Turkey breast", "Piept de curcan", "Roasted, skinless", "Copt, fără piele", "meat", (135, 30.1, 0.0, .7, 0.0), aliases=("turkey fillet", "curcan")),
    food("ground-beef", "Ground beef", "Carne de vită tocată", "Cooked, about 85% lean", "Gătită, aproximativ 85% slabă", "meat", (250, 26.0, 0.0, 15.0, 0.0), aliases=("minced beef", "vita tocata")),
    food("pork-loin", "Pork loin", "Mușchi de porc", "Roasted, lean only", "Copt, numai carne slabă", "meat", (242, 27.3, 0.0, 13.9, 0.0), aliases=("pork tenderloin", "muschi de porc")),
    food("salmon", "Atlantic salmon", "Somon atlantic", "Cooked, dry heat", "Gătit la căldură uscată", "fish", (206, 22.1, 0.0, 12.4, 0.0), aliases=("somon",), allergens={"FISH": "CONTAINS"}),
    food("tuna", "Tuna", "Ton", "Canned in water, drained", "Conservat în apă, scurs", "fish", (116, 25.5, 0.0, .8, 0.0), aliases=("canned tuna", "ton conserva"), allergens={"FISH": "CONTAINS"}),
    food("cod", "Cod", "Cod", "Cooked, dry heat", "Gătit la căldură uscată", "fish", (89, 19.9, 0.0, .7, 0.0), allergens={"FISH": "CONTAINS"}),
    food("eggs", "Whole egg", "Ou întreg", "Boiled", "Fiert", "eggs", (155, 12.6, 1.1, 10.6, 0.0), aliases=("egg", "oua", "ouă"), servings=(serving("eggs", "large", "1 large egg", "1 ou mare", 50, (1, 2, 3, 4)),), allergens={"EGGS": "CONTAINS"}, preparations=(preparation("eggs", "boiled", "Boiled", "Fiert", (155, 12.6, 1.1, 10.6, 0.0)), preparation("eggs", "fried", "Fried", "Prăjit", (196, 13.6, .8, 14.8, 0.0))), default_preparation="boiled"),
    food("lentils", "Lentils", "Linte", "Cooked", "Fiartă", "legumes", (116, 9.0, 20.1, .4, 7.9), aliases=("linte fiarta",)),
    food("chickpeas", "Chickpeas", "Năut", "Cooked", "Fiert", "legumes", (164, 8.9, 27.4, 2.6, 7.6), aliases=("garbanzo beans", "naut")),
    food("kidney-beans", "Kidney beans", "Fasole roșie", "Cooked", "Fiartă", "legumes", (127, 8.7, 22.8, .5, 6.4), aliases=("red beans", "fasole rosie")),
    food("tofu", "Firm tofu", "Tofu ferm", "Prepared with calcium", "Preparat cu calciu", "plant-protein", (144, 17.3, 2.8, 8.7, 2.3), aliases=("bean curd",), allergens={"SOY": "CONTAINS"}),
    # Dairy and fats
    food("whole-milk", "Whole milk", "Lapte integral", "About 3.25% fat", "Aproximativ 3,25% grăsime", "dairy", (61, 3.2, 4.8, 3.3, 0.0), aliases=("full fat milk", "lapte 3.5"), servings=(serving("whole-milk", "cup", "1 cup", "1 cană", 244, (.5, 1, 2)),), allergens={"MILK": "CONTAINS"}),
    food("semi-skimmed-milk", "Semi-skimmed milk", "Lapte semidegresat", "About 1.5% fat", "Aproximativ 1,5% grăsime", "dairy", (46, 3.4, 4.9, 1.5, 0.0), aliases=("low fat milk", "lapte 1.5"), servings=(serving("semi-skimmed-milk", "cup", "1 cup", "1 cană", 244, (.5, 1, 2)),), allergens={"MILK": "CONTAINS"}),
    food("plain-yogurt", "Plain yogurt", "Iaurt simplu", "Whole milk", "Din lapte integral", "dairy", (61, 3.5, 4.7, 3.3, 0.0), aliases=("natural yogurt", "iaurt natural"), allergens={"MILK": "CONTAINS"}),
    food("greek-yogurt", "Greek yogurt", "Iaurt grecesc", "About 2% fat", "Aproximativ 2% grăsime", "dairy", (73, 9.9, 3.9, 2.0, 0.0), aliases=("strained yogurt",), servings=(serving("greek-yogurt", "small-tub", "1 small tub", "1 pahar mic", 150),), allergens={"MILK": "CONTAINS"}),
    food("kefir", "Kefir", "Chefir", "Plain, whole milk", "Simplu, din lapte integral", "dairy", (61, 3.5, 4.5, 3.3, 0.0), aliases=("chefir",), servings=(serving("kefir", "cup", "1 cup", "1 cană", 243, (.5, 1, 2)),), allergens={"MILK": "CONTAINS"}),
    food("cheddar", "Cheddar cheese", "Brânză Cheddar", "Full fat", "Grasă", "dairy", (403, 24.9, 1.3, 33.1, 0.0), aliases=("cheddar", "branza cheddar"), servings=(serving("cheddar", "slice", "1 slice", "1 felie", 28, (1, 2, 3)),), allergens={"MILK": "CONTAINS"}),
    food("cottage-cheese", "Cottage cheese", "Brânză de vaci", "About 2% fat", "Aproximativ 2% grăsime", "dairy", (84, 11.1, 4.3, 2.3, 0.0), aliases=("branza de vaci",), allergens={"MILK": "CONTAINS"}),
    food("feta", "Feta cheese", "Brânză feta", "Brined cheese", "Brânză în saramură", "dairy", (264, 14.2, 4.1, 21.3, 0.0), aliases=("feta",), allergens={"MILK": "CONTAINS"}),
    food("sour-cream", "Sour cream", "Smântână", "About 20% fat", "Aproximativ 20% grăsime", "dairy", (206, 2.4, 4.6, 19.7, 0.0), aliases=("smantana",), servings=(serving("sour-cream", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3, 4)),), allergens={"MILK": "CONTAINS"}),
    food("butter", "Butter", "Unt", "Unsalted", "Nesărat", "fats", (717, .9, .1, 81.1, 0.0), servings=(serving("butter", "teaspoon", "1 teaspoon", "1 linguriță", 5, (1, 2, 3, 4)),), allergens={"MILK": "CONTAINS"}),
    food("olive-oil", "Olive oil", "Ulei de măsline", "Pure fat", "Grăsime pură", "fats", (884, 0.0, 0.0, 100.0, 0.0), aliases=("ulei masline",), servings=(serving("olive-oil", "teaspoon", "1 teaspoon", "1 linguriță", 4, (1, 2, 3, 4)), serving("olive-oil", "tablespoon", "1 tablespoon", "1 lingură", 14, (1, 2, 3)))),
    food("sunflower-oil", "Sunflower oil", "Ulei de floarea-soarelui", "Pure fat", "Grăsime pură", "fats", (884, 0.0, 0.0, 100.0, 0.0), aliases=("ulei floarea soarelui",), servings=(serving("sunflower-oil", "teaspoon", "1 teaspoon", "1 linguriță", 4, (1, 2, 3, 4)), serving("sunflower-oil", "tablespoon", "1 tablespoon", "1 lingură", 14, (1, 2, 3)))),
    # Pantry, nuts and seeds
    food("sugar", "White sugar", "Zahăr alb", "Granulated", "Granulat", "pantry", (387, 0.0, 100.0, 0.0, 0.0), aliases=("granulated sugar", "zahar"), servings=(serving("sugar", "teaspoon", "1 teaspoon", "1 linguriță", 4, (1, 2, 3, 4)), serving("sugar", "tablespoon", "1 tablespoon", "1 lingură", 13, (1, 2, 3)))),
    food("honey", "Honey", "Miere", "Pure", "Pură", "pantry", (304, .3, 82.4, 0.0, .2), servings=(serving("honey", "teaspoon", "1 teaspoon", "1 linguriță", 7, (1, 2, 3, 4)), serving("honey", "tablespoon", "1 tablespoon", "1 lingură", 21, (1, 2, 3)))),
    food("peanut-butter", "Peanut butter", "Unt de arahide", "Smooth", "Fin", "nuts-seeds", (588, 25.1, 20.0, 50.4, 6.0), aliases=("unt arahide",), servings=(serving("peanut-butter", "tablespoon", "1 tablespoon", "1 lingură", 16, (1, 2, 3, 4)),), allergens={"PEANUTS": "CONTAINS"}),
    food("almonds", "Almonds", "Migdale", "Raw", "Crude", "nuts-seeds", (579, 21.2, 21.6, 49.9, 12.5), aliases=("almond",), servings=(serving("almonds", "handful", "1 handful", "1 pumn", 28, (.5, 1, 2)),), allergens={"NUTS": "CONTAINS"}),
    food("walnuts", "Walnuts", "Nuci", "Raw", "Crude", "nuts-seeds", (654, 15.2, 13.7, 65.2, 6.7), aliases=("walnut",), servings=(serving("walnuts", "handful", "1 handful", "1 pumn", 28, (.5, 1, 2)),), allergens={"NUTS": "CONTAINS"}),
]


CATALOGUE = {
    "schema": "calorie-quick-built-in-catalogue",
    "version": 1,
    "source": {
        "name": "USDA FoodData Central — curated common-food snapshot",
        "url": "https://fdc.nal.usda.gov/",
        "license": "CC0-1.0",
        "note": "Rounded generic reference values; branded products and recipes vary.",
    },
    "foods": FOODS,
}


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(CATALOGUE, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Wrote {len(FOODS)} foods to {OUTPUT}")


if __name__ == "__main__":
    main()
