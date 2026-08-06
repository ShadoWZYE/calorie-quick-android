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
        "sourceId": f"usda-fdc-cc0:curated-v2:{food_id}",
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

# This second reviewed wave prioritizes ordinary European and Romanian shopping,
# cooking, and restaurant vocabulary. Values are rounded generic references per
# 100 g; a package label or saved personal recipe should replace them when known.
FOODS += [
    # More fruit
    food("kiwi", "Kiwi", "Kiwi", "Fresh", "Proaspăt", "fruit", (61, 1.1, 14.7, .5, 3.0), aliases=("kiwifruit",), servings=(serving("kiwi", "medium", "1 medium kiwi", "1 kiwi mediu", 69, (1, 2, 3)),)),
    food("peach", "Peach", "Piersică", "Fresh", "Proaspătă", "fruit", (39, .9, 9.5, .3, 1.5), aliases=("peaches", "piersica"), servings=(serving("peach", "medium", "1 medium peach", "1 piersică medie", 150),)),
    food("plum", "Plum", "Prună", "Fresh", "Proaspătă", "fruit", (46, .7, 11.4, .3, 1.4), aliases=("plums", "prune"), servings=(serving("plum", "medium", "1 medium plum", "1 prună medie", 66, (1, 2, 3, 4)),)),
    food("apricot", "Apricot", "Caisă", "Fresh", "Proaspătă", "fruit", (48, 1.4, 11.1, .4, 2.0), aliases=("apricots", "caise"), servings=(serving("apricot", "medium", "1 apricot", "1 caisă", 35, (1, 2, 3, 4)),)),
    food("cherries", "Cherries", "Cireșe", "Fresh, sweet", "Proaspete, dulci", "fruit", (63, 1.1, 16.0, .2, 2.1), aliases=("cherry", "cirese"), servings=(serving("cherries", "cup", "1 cup", "1 cană", 138, (.5, 1, 2)),)),
    food("raspberries", "Raspberries", "Zmeură", "Fresh", "Proaspătă", "fruit", (52, 1.2, 11.9, .7, 6.5), aliases=("raspberry", "zmeura"), servings=(serving("raspberries", "cup", "1 cup", "1 cană", 123, (.5, 1, 2)),)),
    food("blackberries", "Blackberries", "Mure", "Fresh", "Proaspete", "fruit", (43, 1.4, 9.6, .5, 5.3), aliases=("blackberry",), servings=(serving("blackberries", "cup", "1 cup", "1 cană", 144, (.5, 1, 2)),)),
    food("pineapple", "Pineapple", "Ananas", "Fresh", "Proaspăt", "fruit", (50, .5, 13.1, .1, 1.4), servings=(serving("pineapple", "cup", "1 cup chunks", "1 cană bucăți", 165, (.5, 1, 2)),)),
    food("mango", "Mango", "Mango", "Fresh", "Proaspăt", "fruit", (60, .8, 15.0, .4, 1.6), servings=(serving("mango", "cup", "1 cup pieces", "1 cană bucăți", 165, (.5, 1, 2)),)),
    food("grapefruit", "Grapefruit", "Grepfrut", "Fresh", "Proaspăt", "fruit", (42, .8, 10.7, .1, 1.6), servings=(serving("grapefruit", "half", "1/2 fruit", "1/2 fruct", 123, (1, 2)),)),
    food("pomegranate", "Pomegranate", "Rodie", "Fresh seeds", "Semințe proaspete", "fruit", (83, 1.7, 18.7, 1.2, 4.0), aliases=("pomegranate seeds", "rodie seminte"), servings=(serving("pomegranate", "half-cup", "1/2 cup seeds", "1/2 cană semințe", 87, (1, 2)),)),
    food("cantaloupe", "Cantaloupe melon", "Pepene galben", "Fresh", "Proaspăt", "fruit", (34, .8, 8.2, .2, .9), aliases=("melon",), servings=(serving("cantaloupe", "cup", "1 cup diced", "1 cană cuburi", 160, (.5, 1, 2)),)),
    food("raisins", "Raisins", "Stafide", "Seedless, dried", "Fără sâmburi, uscate", "fruit", (299, 3.1, 79.2, .5, 3.7), servings=(serving("raisins", "tablespoon", "1 tablespoon", "1 lingură", 9, (1, 2, 3, 4)),)),
    food("dates", "Dates", "Curmale", "Dried", "Uscate", "fruit", (282, 2.5, 75.0, .4, 8.0), servings=(serving("dates", "date", "1 date", "1 curmală", 24, (1, 2, 3, 4)),)),
    # More vegetables
    food("cauliflower", "Cauliflower", "Conopidă", "Raw", "Crudă", "vegetables", (25, 1.9, 5.0, .3, 2.0), aliases=("conopida",)),
    food("green-beans", "Green beans", "Fasole verde", "Cooked", "Fiartă", "vegetables", (35, 1.9, 7.9, .3, 3.2), aliases=("string beans", "pastai")),
    food("green-peas", "Green peas", "Mazăre", "Cooked", "Fiartă", "vegetables", (84, 5.4, 15.6, .2, 5.5), aliases=("peas", "mazare")),
    food("sweet-corn", "Sweet corn", "Porumb dulce", "Cooked kernels", "Boabe fierte", "vegetables", (96, 3.4, 21.0, 1.5, 2.4), aliases=("corn", "porumb")),
    food("lettuce", "Lettuce", "Salată verde", "Raw", "Crudă", "vegetables", (15, 1.4, 2.9, .2, 1.3), aliases=("salata verde",)),
    food("kale", "Kale", "Kale", "Raw", "Crud", "vegetables", (49, 4.3, 8.8, .9, 3.6)),
    food("beetroot", "Beetroot", "Sfeclă roșie", "Cooked", "Fiartă", "vegetables", (44, 1.7, 10.0, .2, 2.0), aliases=("beet", "sfecla rosie")),
    food("celery", "Celery stalks", "Tije de țelină", "Raw", "Crude", "vegetables", (14, .7, 3.0, .2, 1.6), aliases=("apio", "telina apio"), allergens={"CELERY": "CONTAINS"}),
    food("celeriac", "Celeriac", "Țelină rădăcină", "Raw", "Crudă", "vegetables", (42, 1.5, 9.2, .3, 1.8), aliases=("celery root", "telina"), allergens={"CELERY": "CONTAINS"}),
    food("leek", "Leek", "Praz", "Raw", "Crud", "vegetables", (61, 1.5, 14.2, .3, 1.8), aliases=("leeks",)),
    food("asparagus", "Asparagus", "Sparanghel", "Cooked", "Gătit", "vegetables", (22, 2.4, 4.1, .2, 2.0)),
    food("pumpkin", "Pumpkin", "Dovleac", "Cooked", "Gătit", "vegetables", (20, .7, 4.9, .1, 1.1)),
    food("radish", "Radish", "Ridiche", "Raw", "Crudă", "vegetables", (16, .7, 3.4, .1, 1.6), aliases=("radishes", "ridichi")),
    food("sauerkraut", "Sauerkraut", "Varză murată", "Drained, generic", "Scursă, valoare generică", "vegetables", (19, .9, 4.3, .1, 2.9), aliases=("pickled cabbage", "varza murata")),
    food("pickled-cucumbers", "Pickled cucumbers", "Castraveți murați", "Drained, generic", "Scurși, valoare generică", "vegetables", (12, .5, 2.4, .3, 1.0), aliases=("pickles", "castraveti murati")),
    # Grains and bakery staples
    food("basmati-rice", "Basmati rice", "Orez basmati", "Cooked", "Fiert", "grains", (121, 3.5, 25.2, .4, .4), aliases=("orez basmati fiert",)),
    food("couscous", "Couscous", "Cușcuș", "Cooked", "Gătit", "grains", (112, 3.8, 23.2, .2, 1.4), aliases=("cuscus",), allergens={"GLUTEN": "CONTAINS"}),
    food("bulgur", "Bulgur", "Bulgur", "Cooked", "Fiert", "grains", (83, 3.1, 18.6, .2, 4.5), allergens={"GLUTEN": "CONTAINS"}),
    food("barley", "Pearled barley", "Orz perlat", "Cooked", "Fiert", "grains", (123, 2.3, 28.2, .4, 3.8), aliases=("arpacas",), allergens={"GLUTEN": "CONTAINS"}),
    food("wheat-flour", "Wheat flour", "Făină de grâu", "All-purpose", "Albă, universală", "grains", (364, 10.3, 76.3, 1.0, 2.7), aliases=("plain flour", "faina alba"), allergens={"GLUTEN": "CONTAINS"}),
    food("whole-wheat-flour", "Whole-wheat flour", "Făină integrală", "Whole grain", "Din bob integral", "grains", (340, 13.2, 72.0, 2.5, 10.7), aliases=("wholemeal flour", "faina integrala"), allergens={"GLUTEN": "CONTAINS"}),
    food("semolina", "Semolina", "Griș", "Dry wheat semolina", "Griș de grâu uscat", "grains", (360, 12.7, 72.8, 1.1, 3.9), aliases=("gris",), allergens={"GLUTEN": "CONTAINS"}),
    food("rye-bread", "Rye bread", "Pâine de secară", "Generic", "Valoare generică", "bakery", (259, 8.5, 48.3, 3.3, 5.8), aliases=("paine secara",), servings=(serving("rye-bread", "slice", "1 slice", "1 felie", 32, (1, 2, 3, 4)),), allergens={"GLUTEN": "CONTAINS"}),
    food("pita-bread", "Pita bread", "Lipie pita", "White wheat", "Din grâu alb", "bakery", (275, 9.1, 55.7, 1.2, 2.2), aliases=("pita", "lipie"), servings=(serving("pita-bread", "piece", "1 pita", "1 lipie", 60, (.5, 1, 2)),), allergens={"GLUTEN": "CONTAINS"}),
    food("wheat-tortilla", "Wheat tortilla", "Tortilla de grâu", "Generic", "Valoare generică", "bakery", (312, 8.3, 52.1, 7.5, 3.3), aliases=("wrap",), servings=(serving("wheat-tortilla", "piece", "1 tortilla", "1 tortilla", 50, (1, 2, 3)),), allergens={"GLUTEN": "CONTAINS"}),
    food("bagel", "Plain bagel", "Bagel simplu", "Generic", "Valoare generică", "bakery", (257, 10.0, 50.5, 1.7, 2.3), servings=(serving("bagel", "piece", "1 medium bagel", "1 bagel mediu", 98),), allergens={"GLUTEN": "CONTAINS"}),
    food("croissant", "Butter croissant", "Croasant cu unt", "Generic", "Valoare generică", "bakery", (406, 8.2, 45.8, 21.0, 2.6), aliases=("croasant",), servings=(serving("croissant", "piece", "1 medium croissant", "1 croasant mediu", 57),), allergens={"GLUTEN": "CONTAINS", "MILK": "CONTAINS", "EGGS": "MAY_CONTAIN"}),
    food("pretzel", "Soft pretzel", "Covrig", "Plain, generic estimate", "Simplu, valoare generică", "bakery", (338, 9.9, 69.4, 3.1, 2.7), aliases=("covrigi",), servings=(serving("pretzel", "piece", "1 medium pretzel", "1 covrig mediu", 80),), allergens={"GLUTEN": "CONTAINS"}),
    # Protein foods
    food("chicken-thigh", "Chicken thigh", "Pulpă superioară de pui", "Roasted, meat and skin", "Coaptă, carne și piele", "meat", (229, 25.0, 0.0, 15.0, 0.0), aliases=("pulpa pui",)),
    food("chicken-drumstick", "Chicken drumstick", "Ciocănel de pui", "Roasted, meat and skin", "Copt, carne și piele", "meat", (216, 27.0, 0.0, 11.2, 0.0), aliases=("copanel pui",)),
    food("beef-steak", "Beef steak", "Friptură de vită", "Grilled, lean and fat", "La grătar, carne și grăsime", "meat", (271, 26.0, 0.0, 18.0, 0.0), aliases=("steak", "vita gratar")),
    food("pork-chop", "Pork chop", "Cotlet de porc", "Cooked", "Gătit", "meat", (231, 25.7, 0.0, 13.9, 0.0), aliases=("cotlet porc",)),
    food("lamb", "Lamb", "Carne de miel", "Roasted, lean and fat", "Coaptă, carne și grăsime", "meat", (258, 25.6, 0.0, 16.5, 0.0), aliases=("miel",)),
    food("bacon", "Bacon", "Bacon", "Pan-fried, generic", "Prăjit, valoare generică", "meat", (541, 37.0, 1.4, 42.0, 0.0), servings=(serving("bacon", "slice", "1 cooked slice", "1 felie gătită", 8, (1, 2, 3, 4)),)),
    food("ham", "Cooked ham", "Șuncă fiartă", "Generic", "Valoare generică", "meat", (145, 21.0, 1.5, 7.0, 0.0), aliases=("sunca",), servings=(serving("ham", "slice", "1 slice", "1 felie", 28, (1, 2, 3, 4)),)),
    food("sausage", "Pork sausage", "Cârnat de porc", "Cooked, generic", "Gătit, valoare generică", "meat", (301, 12.0, 2.0, 27.0, 0.0), aliases=("sausages", "carnat", "carnati"), servings=(serving("sausage", "link", "1 sausage", "1 cârnat", 75, (1, 2, 3)),)),
    food("trout", "Trout", "Păstrăv", "Cooked, dry heat", "Gătit la căldură uscată", "fish", (168, 23.8, 0.0, 7.4, 0.0), aliases=("pastrav",), allergens={"FISH": "CONTAINS"}),
    food("mackerel", "Mackerel", "Macrou", "Cooked", "Gătit", "fish", (262, 23.9, 0.0, 17.8, 0.0), aliases=("macrou",), allergens={"FISH": "CONTAINS"}),
    food("sardines", "Sardines", "Sardine", "Canned in oil, drained", "Conservate în ulei, scurse", "fish", (208, 24.6, 0.0, 11.5, 0.0), allergens={"FISH": "CONTAINS"}),
    food("shrimp", "Shrimp", "Creveți", "Cooked", "Gătiți", "fish", (99, 24.0, .2, .3, 0.0), aliases=("prawns", "creveti"), allergens={"CRUSTACEANS": "CONTAINS"}),
    food("mussels", "Mussels", "Midii", "Cooked", "Gătite", "fish", (172, 23.8, 7.4, 4.5, 0.0), allergens={"MOLLUSCS": "CONTAINS"}),
    food("egg-white", "Egg white", "Albuș de ou", "Cooked", "Gătit", "eggs", (52, 10.9, .7, .2, 0.0), aliases=("albus",), servings=(serving("egg-white", "large", "1 large egg white", "1 albuș de ou mare", 33, (1, 2, 3, 4)),), allergens={"EGGS": "CONTAINS"}),
    food("egg-yolk", "Egg yolk", "Gălbenuș de ou", "Cooked", "Gătit", "eggs", (322, 15.9, 3.6, 26.5, 0.0), aliases=("galbenus",), servings=(serving("egg-yolk", "large", "1 large egg yolk", "1 gălbenuș de ou mare", 17, (1, 2, 3, 4)),), allergens={"EGGS": "CONTAINS"}),
    # Legumes and dairy
    food("white-beans", "White beans", "Fasole albă", "Cooked", "Fiartă", "legumes", (139, 9.7, 25.1, .4, 6.3), aliases=("cannellini", "fasole alba")),
    food("black-beans", "Black beans", "Fasole neagră", "Cooked", "Fiartă", "legumes", (132, 8.9, 23.7, .5, 8.7), aliases=("fasole neagra",)),
    food("split-peas", "Split peas", "Mazăre uscată despicată", "Cooked", "Fiartă", "legumes", (118, 8.3, 21.1, .4, 8.3), aliases=("mazare uscata",)),
    food("edamame", "Edamame", "Edamame", "Cooked, shelled", "Fiert, fără păstăi", "legumes", (121, 11.9, 8.9, 5.2, 5.2), allergens={"SOY": "CONTAINS"}),
    food("hummus", "Hummus", "Hummus", "Prepared, generic", "Preparat, valoare generică", "plant-protein", (166, 7.9, 14.3, 9.6, 6.0), servings=(serving("hummus", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3, 4)),), allergens={"SESAME": "CONTAINS"}),
    food("skim-milk", "Skim milk", "Lapte degresat", "About 0.1% fat", "Aproximativ 0,1% grăsime", "dairy", (34, 3.4, 5.0, .1, 0.0), servings=(serving("skim-milk", "cup", "1 cup", "1 cană", 244, (.5, 1, 2)),), allergens={"MILK": "CONTAINS"}),
    food("mozzarella", "Mozzarella", "Mozzarella", "Whole milk", "Din lapte integral", "dairy", (300, 22.2, 2.2, 22.4, 0.0), allergens={"MILK": "CONTAINS"}),
    food("parmesan", "Parmesan", "Parmezan", "Hard cheese", "Brânză tare", "dairy", (431, 38.0, 4.1, 29.0, 0.0), servings=(serving("parmesan", "tablespoon", "1 tablespoon grated", "1 lingură rasă", 5, (1, 2, 3, 4)),), allergens={"MILK": "CONTAINS"}),
    food("gouda", "Gouda", "Gouda", "Full fat", "Grasă", "dairy", (356, 24.9, 2.2, 27.4, 0.0), servings=(serving("gouda", "slice", "1 slice", "1 felie", 25, (1, 2, 3, 4)),), allergens={"MILK": "CONTAINS"}),
    food("telemea", "Telemea cheese", "Telemea", "Brined cow's cheese, generic", "Brânză de vacă în saramură, valoare generică", "dairy", (280, 17.0, 2.0, 23.0, 0.0), aliases=("branza telemea",), allergens={"MILK": "CONTAINS"}),
    food("ricotta", "Ricotta", "Ricotta", "Whole milk", "Din lapte integral", "dairy", (174, 11.3, 3.0, 13.0, 0.0), allergens={"MILK": "CONTAINS"}),
    food("cream-cheese", "Cream cheese", "Cremă de brânză", "Full fat", "Grasă", "dairy", (342, 6.2, 5.5, 34.4, 0.0), aliases=("crema branza",), servings=(serving("cream-cheese", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3, 4)),), allergens={"MILK": "CONTAINS"}),
    food("whipping-cream", "Whipping cream", "Smântână pentru frișcă", "About 35% fat", "Aproximativ 35% grăsime", "dairy", (340, 2.8, 2.9, 36.1, 0.0), aliases=("heavy cream", "frisca lichida"), allergens={"MILK": "CONTAINS"}),
    food("skyr", "Skyr", "Skyr", "Plain, low fat", "Simplu, slab", "dairy", (63, 11.0, 4.0, .2, 0.0), allergens={"MILK": "CONTAINS"}),
    # Nuts, seeds, spreads, and condiments
    food("cashews", "Cashews", "Caju", "Raw", "Crud", "nuts-seeds", (553, 18.2, 30.2, 43.9, 3.3), servings=(serving("cashews", "handful", "1 handful", "1 pumn", 28, (.5, 1, 2)),), allergens={"NUTS": "CONTAINS"}),
    food("pistachios", "Pistachios", "Fistic", "Dry roasted", "Copt uscat", "nuts-seeds", (562, 20.3, 27.5, 45.4, 10.3), servings=(serving("pistachios", "handful", "1 handful", "1 pumn", 28, (.5, 1, 2)),), allergens={"NUTS": "CONTAINS"}),
    food("hazelnuts", "Hazelnuts", "Alune de pădure", "Raw", "Crude", "nuts-seeds", (628, 15.0, 16.7, 60.8, 9.7), aliases=("alune padure",), allergens={"NUTS": "CONTAINS"}),
    food("sunflower-seeds", "Sunflower seeds", "Semințe de floarea-soarelui", "Kernels, dry roasted", "Miez, copt uscat", "nuts-seeds", (584, 20.8, 20.0, 51.5, 8.6), allergens={"NUTS": "MAY_CONTAIN"}),
    food("pumpkin-seeds", "Pumpkin seeds", "Semințe de dovleac", "Roasted kernels", "Miez copt", "nuts-seeds", (559, 30.2, 10.7, 49.1, 6.0)),
    food("chia-seeds", "Chia seeds", "Semințe de chia", "Dried", "Uscate", "nuts-seeds", (486, 16.5, 42.1, 30.7, 34.4), servings=(serving("chia-seeds", "tablespoon", "1 tablespoon", "1 lingură", 12, (1, 2, 3)),)),
    food("flax-seeds", "Flax seeds", "Semințe de in", "Whole", "Întregi", "nuts-seeds", (534, 18.3, 28.9, 42.2, 27.3), aliases=("linseed", "seminte in"), servings=(serving("flax-seeds", "tablespoon", "1 tablespoon", "1 lingură", 10, (1, 2, 3)),)),
    food("tahini", "Tahini", "Tahini", "Sesame paste", "Pastă de susan", "nuts-seeds", (595, 17.0, 21.2, 53.8, 9.3), servings=(serving("tahini", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3)),), allergens={"SESAME": "CONTAINS"}),
    food("maple-syrup", "Maple syrup", "Sirop de arțar", "Pure", "Pur", "pantry", (260, 0.0, 67.0, .1, 0.0), aliases=("sirop artar",), servings=(serving("maple-syrup", "tablespoon", "1 tablespoon", "1 lingură", 20, (1, 2, 3)),)),
    food("fruit-jam", "Fruit jam", "Gem de fructe", "Generic", "Valoare generică", "pantry", (250, .4, 65.0, .1, 1.0), aliases=("jelly", "gem", "dulceata"), servings=(serving("fruit-jam", "tablespoon", "1 tablespoon", "1 lingură", 20, (1, 2, 3)),)),
    food("cocoa-powder", "Cocoa powder", "Pudră de cacao", "Unsweetened", "Neîndulcită", "pantry", (228, 19.6, 57.9, 13.7, 37.0), aliases=("cacao",), servings=(serving("cocoa-powder", "tablespoon", "1 tablespoon", "1 lingură", 5, (1, 2, 3, 4)),)),
    food("dark-chocolate", "Dark chocolate", "Ciocolată neagră", "70–85% cocoa, generic", "70–85% cacao, valoare generică", "pantry", (598, 7.8, 45.9, 42.6, 10.9), aliases=("ciocolata neagra",), servings=(serving("dark-chocolate", "square", "1 square", "1 pătrățel", 10, (1, 2, 3, 4)),), allergens={"MILK": "MAY_CONTAIN", "NUTS": "MAY_CONTAIN"}),
    food("ketchup", "Ketchup", "Ketchup", "Generic", "Valoare generică", "pantry", (112, 1.3, 26.0, .2, .3), servings=(serving("ketchup", "tablespoon", "1 tablespoon", "1 lingură", 17, (1, 2, 3)),)),
    food("mayonnaise", "Mayonnaise", "Maioneză", "Regular, generic", "Clasică, valoare generică", "pantry", (680, 1.0, .6, 75.0, 0.0), aliases=("maioneza",), servings=(serving("mayonnaise", "tablespoon", "1 tablespoon", "1 lingură", 14, (1, 2, 3)),), allergens={"EGGS": "CONTAINS", "MUSTARD": "MAY_CONTAIN"}),
    food("mustard", "Mustard", "Muștar", "Prepared yellow", "Preparat", "pantry", (66, 4.4, 5.8, 4.0, 3.3), aliases=("mustar",), servings=(serving("mustard", "teaspoon", "1 teaspoon", "1 linguriță", 5, (1, 2, 3, 4)),), allergens={"MUSTARD": "CONTAINS"}),
    food("soy-sauce", "Soy sauce", "Sos de soia", "Regular", "Clasic", "pantry", (53, 8.1, 4.9, .6, .8), servings=(serving("soy-sauce", "tablespoon", "1 tablespoon", "1 lingură", 16, (1, 2, 3)),), allergens={"SOY": "CONTAINS", "GLUTEN": "MAY_CONTAIN"}),
    # Generic prepared foods: useful defaults, deliberately marked as estimates
    food("vegetable-soup", "Vegetable soup", "Ciorbă de legume", "Homestyle, generic estimate", "De casă, valoare generică", "prepared-meals", (42, 1.5, 6.5, 1.2, 1.5), aliases=("ciorba legume", "supa legume"), servings=(serving("vegetable-soup", "bowl", "1 bowl", "1 bol", 350, (.5, 1, 2)),)),
    food("chicken-soup", "Chicken soup", "Ciorbă de pui", "Homestyle, generic estimate", "De casă, valoare generică", "prepared-meals", (55, 4.5, 5.0, 2.0, .5), aliases=("ciorba pui", "supa pui"), servings=(serving("chicken-soup", "bowl", "1 bowl", "1 bol", 350, (.5, 1, 2)),)),
    food("sarmale", "Stuffed cabbage rolls", "Sarmale", "Meat and rice, generic estimate", "Cu carne și orez, valoare generică", "prepared-meals", (151, 7.0, 10.0, 9.0, 1.5), aliases=("cabbage rolls",), servings=(serving("sarmale", "piece", "1 roll", "1 sarma", 80, (1, 2, 3, 4)),)),
    food("zacusca", "Zacuscă", "Zacuscă", "Vegetable spread, generic estimate", "Pastă de legume, valoare generică", "prepared-meals", (105, 1.5, 9.0, 7.0, 2.5), aliases=("zacusca",), servings=(serving("zacusca", "tablespoon", "1 tablespoon", "1 lingură", 20, (1, 2, 3, 4)),)),
    food("eggplant-salad", "Eggplant spread", "Salată de vinete", "With oil, generic estimate", "Cu ulei, valoare generică", "prepared-meals", (145, 1.2, 6.5, 13.0, 3.0), aliases=("salata vinete", "vinete coapte"), servings=(serving("eggplant-salad", "tablespoon", "1 tablespoon", "1 lingură", 20, (1, 2, 3, 4)),)),
    food("mashed-beans", "Mashed bean spread", "Fasole bătută", "Generic estimate", "Valoare generică", "prepared-meals", (142, 7.0, 21.0, 3.5, 6.0), aliases=("fasole batuta",)),
    food("mici", "Grilled minced-meat rolls", "Mici", "Generic estimate", "Valoare generică", "prepared-meals", (280, 18.0, 2.0, 22.0, 0.0), aliases=("mititei",), servings=(serving("mici", "piece", "1 piece", "1 mic", 70, (1, 2, 3, 4)),)),
    food("cozonac", "Cozonac", "Cozonac", "Sweet bread, generic estimate", "Pâine dulce, valoare generică", "prepared-meals", (356, 8.0, 52.0, 13.0, 2.0), servings=(serving("cozonac", "slice", "1 slice", "1 felie", 60, (1, 2, 3)),), allergens={"GLUTEN": "CONTAINS", "MILK": "CONTAINS", "EGGS": "CONTAINS", "NUTS": "MAY_CONTAIN"}),
    # Drinks
    food("black-coffee", "Black coffee", "Cafea neagră", "Brewed, unsweetened", "Preparată, neîndulcită", "beverages", (1, .1, 0.0, 0.0, 0.0), aliases=("coffee", "cafea"), servings=(serving("black-coffee", "cup", "1 cup", "1 cană", 240, (1, 2, 3)),)),
    food("tea", "Tea", "Ceai", "Brewed, unsweetened", "Preparat, neîndulcit", "beverages", (1, 0.0, .3, 0.0, 0.0), servings=(serving("tea", "cup", "1 cup", "1 cană", 240, (1, 2, 3)),)),
    food("orange-juice", "Orange juice", "Suc de portocale", "100% juice", "Suc 100%", "beverages", (45, .7, 10.4, .2, .2), servings=(serving("orange-juice", "glass", "1 glass", "1 pahar", 250, (.5, 1, 2)),)),
    food("apple-juice", "Apple juice", "Suc de mere", "100% juice", "Suc 100%", "beverages", (46, .1, 11.3, .1, .2), servings=(serving("apple-juice", "glass", "1 glass", "1 pahar", 250, (.5, 1, 2)),)),
    food("cola", "Cola soft drink", "Băutură tip cola", "Sugar-sweetened, generic", "Cu zahăr, valoare generică", "beverages", (42, 0.0, 10.6, 0.0, 0.0), aliases=("soda", "suc acidulat"), servings=(serving("cola", "glass", "1 glass", "1 pahar", 250, (1, 2)),)),
    food("beer", "Beer", "Bere", "About 5% alcohol", "Aproximativ 5% alcool", "beverages", (43, .5, 3.6, 0.0, 0.0), servings=(serving("beer", "bottle", "1 bottle", "1 sticlă", 330, (1, 2)),)),
    food("red-wine", "Red wine", "Vin roșu", "About 12% alcohol", "Aproximativ 12% alcool", "beverages", (85, .1, 2.6, 0.0, 0.0), aliases=("vin rosu",), servings=(serving("red-wine", "glass", "1 glass", "1 pahar", 150, (1, 2)),), allergens={"SULPHITES": "MAY_CONTAIN"}),
    # Everyday cooking ingredients
    food("salt", "Salt", "Sare", "Table salt", "Sare de masă", "pantry", (0, 0.0, 0.0, 0.0, 0.0), aliases=("sea salt", "sare de mare"), servings=(serving("salt", "teaspoon", "1 teaspoon", "1 linguriță", 6, (.25, .5, 1, 2)),)),
    food("baking-powder", "Baking powder", "Praf de copt", "Leavening agent", "Agent de afânare", "pantry", (53, 0.0, 28.1, 0.0, .2), servings=(serving("baking-powder", "teaspoon", "1 teaspoon", "1 linguriță", 4, (.5, 1, 2, 3)),)),
    food("baking-soda", "Baking soda", "Bicarbonat de sodiu", "Leavening agent", "Agent de afânare", "pantry", (0, 0.0, 0.0, 0.0, 0.0), aliases=("sodium bicarbonate", "bicarbonat"), servings=(serving("baking-soda", "teaspoon", "1 teaspoon", "1 linguriță", 5, (.25, .5, 1, 2)),)),
    food("dry-yeast", "Dry yeast", "Drojdie uscată", "Active dry", "Activă, uscată", "pantry", (325, 40.4, 41.2, 7.6, 26.9), aliases=("yeast", "drojdie"), servings=(serving("dry-yeast", "teaspoon", "1 teaspoon", "1 linguriță", 3, (1, 2, 3)),)),
    food("cornstarch", "Cornstarch", "Amidon de porumb", "Dry", "Uscat", "pantry", (381, .3, 91.3, .1, .9), aliases=("corn flour", "amidon"), servings=(serving("cornstarch", "tablespoon", "1 tablespoon", "1 lingură", 8, (1, 2, 3, 4)),)),
    food("breadcrumbs", "Breadcrumbs", "Pesmet", "Dry, plain", "Uscat, simplu", "pantry", (395, 13.4, 71.9, 5.3, 4.5), aliases=("bread crumbs",), servings=(serving("breadcrumbs", "tablespoon", "1 tablespoon", "1 lingură", 7, (1, 2, 3, 4)),), allergens={"GLUTEN": "CONTAINS"}),
    food("rice-flour", "Rice flour", "Făină de orez", "White rice, dry", "Din orez alb, uscată", "grains", (366, 5.9, 80.1, 1.4, 2.4), aliases=("faina orez",)),
    food("almond-flour", "Almond flour", "Făină de migdale", "Ground almonds", "Migdale măcinate", "nuts-seeds", (571, 21.4, 21.4, 50.0, 10.7), aliases=("almond meal", "faina migdale"), allergens={"NUTS": "CONTAINS"}),
    food("coconut-flour", "Coconut flour", "Făină de cocos", "Defatted, generic", "Degresată, valoare generică", "grains", (400, 20.0, 60.0, 13.3, 33.3), aliases=("faina cocos",)),
    food("vanilla-extract", "Vanilla extract", "Extract de vanilie", "Pure, generic", "Pur, valoare generică", "pantry", (288, .1, 12.7, .1, 0.0), aliases=("vanilla essence", "esenta vanilie"), servings=(serving("vanilla-extract", "teaspoon", "1 teaspoon", "1 linguriță", 4, (.5, 1, 2, 3)),)),
    food("cinnamon", "Ground cinnamon", "Scorțișoară măcinată", "Dried spice", "Condiment uscat", "pantry", (247, 4.0, 80.6, 1.2, 53.1), aliases=("scortisoara",), servings=(serving("cinnamon", "teaspoon", "1 teaspoon", "1 linguriță", 3, (.5, 1, 2)),)),
    food("paprika", "Paprika", "Boia", "Ground spice", "Condiment măcinat", "pantry", (282, 14.1, 54.0, 13.0, 34.9), aliases=("boia dulce",), servings=(serving("paprika", "teaspoon", "1 teaspoon", "1 linguriță", 2, (.5, 1, 2, 3)),)),
    food("black-pepper", "Black pepper", "Piper negru", "Ground", "Măcinat", "pantry", (251, 10.4, 64.0, 3.3, 25.3), aliases=("pepper", "piper"), servings=(serving("black-pepper", "teaspoon", "1 teaspoon", "1 linguriță", 2, (.25, .5, 1, 2)),)),
    food("oregano", "Dried oregano", "Oregano uscat", "Dried herb", "Plantă aromatică uscată", "pantry", (265, 9.0, 68.9, 4.3, 42.5), servings=(serving("oregano", "teaspoon", "1 teaspoon", "1 linguriță", 1, (1, 2, 3)),)),
    food("basil", "Fresh basil", "Busuioc proaspăt", "Fresh herb", "Plantă aromatică proaspătă", "vegetables", (23, 3.2, 2.7, .6, 1.6), aliases=("busuioc",)),
    food("parsley", "Fresh parsley", "Pătrunjel proaspăt", "Fresh herb", "Plantă aromatică proaspătă", "vegetables", (36, 3.0, 6.3, .8, 3.3), aliases=("patrunjel",)),
    food("dill", "Fresh dill", "Mărar proaspăt", "Fresh herb", "Plantă aromatică proaspătă", "vegetables", (43, 3.5, 7.0, 1.1, 2.1), aliases=("marar",)),
    food("tomato-paste", "Tomato paste", "Pastă de tomate", "Concentrated, generic", "Concentrată, valoare generică", "pantry", (82, 4.3, 18.9, .5, 4.1), aliases=("tomato puree", "pasta tomate"), servings=(serving("tomato-paste", "tablespoon", "1 tablespoon", "1 lingură", 16, (1, 2, 3, 4)),)),
    food("canned-tomatoes", "Canned tomatoes", "Roșii la conservă", "With juice, generic", "În suc, valoare generică", "pantry", (24, 1.2, 5.3, .2, 1.5), aliases=("chopped tomatoes", "rosii conserva")),
    food("coconut-milk", "Coconut milk", "Lapte de cocos", "Canned, full fat", "Conservat, gras", "pantry", (230, 2.3, 5.5, 23.8, 2.2), servings=(serving("coconut-milk", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3, 4)),)),
    food("apple-cider-vinegar", "Apple cider vinegar", "Oțet de mere", "Generic", "Valoare generică", "pantry", (21, 0.0, .9, 0.0, 0.0), aliases=("otet mere",), servings=(serving("apple-cider-vinegar", "tablespoon", "1 tablespoon", "1 lingură", 15, (1, 2, 3)),)),
    food("balsamic-vinegar", "Balsamic vinegar", "Oțet balsamic", "Generic", "Valoare generică", "pantry", (88, .5, 17.0, 0.0, 0.0), aliases=("otet balsamic",), servings=(serving("balsamic-vinegar", "tablespoon", "1 tablespoon", "1 lingură", 16, (1, 2, 3)),), allergens={"SULPHITES": "MAY_CONTAIN"}),
    food("stock-cube", "Stock cube", "Cub de supă", "Prepared seasoning, generic", "Condiment preparat, valoare generică", "pantry", (198, 14.6, 23.5, 4.7, 0.0), aliases=("bouillon cube", "cub concentrat"), servings=(serving("stock-cube", "cube", "1 cube", "1 cub", 10, (.5, 1, 2)),), allergens={"CELERY": "MAY_CONTAIN"}),
    food("green-olives", "Green olives", "Măsline verzi", "Brined, drained", "În saramură, scurse", "pantry", (145, 1.0, 3.8, 15.3, 3.3), aliases=("olives", "masline verzi"), servings=(serving("green-olives", "olive", "1 olive", "1 măslină", 4, (1, 2, 3, 4)),)),
    food("black-olives", "Black olives", "Măsline negre", "Ripe, canned, drained", "Coapte, conservate, scurse", "pantry", (116, .8, 6.0, 10.9, 1.6), aliases=("masline negre",), servings=(serving("black-olives", "olive", "1 olive", "1 măslină", 4, (1, 2, 3, 4)),)),
    food("capers", "Capers", "Capere", "Canned, drained", "Conservate, scurse", "pantry", (23, 2.4, 4.9, .9, 3.2), servings=(serving("capers", "tablespoon", "1 tablespoon", "1 lingură", 9, (1, 2, 3)),)),
    food("gelatin", "Gelatin powder", "Gelatină pudră", "Unflavoured, dry", "Fără aromă, uscată", "pantry", (335, 85.6, 0.0, .1, 0.0), aliases=("gelatine", "gelatina"), servings=(serving("gelatin", "teaspoon", "1 teaspoon", "1 linguriță", 3, (1, 2, 3)),)),
]


CATALOGUE = {
    "schema": "calorie-quick-built-in-catalogue",
    "version": 2,
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
