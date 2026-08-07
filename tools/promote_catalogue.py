#!/usr/bin/env python3
"""Promote clean collected foods into Calorie Quick's distributable catalogue.

The local review inbox remains the evidence store. This command produces a
deterministic Android asset containing only records that pass structural,
nutrition, identity, consent, and explicit-flag checks. Rejected records are
listed in a local report instead of being silently discarded.
"""

from __future__ import annotations

import argparse
import json
import math
import re
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from review_inbox_core import default_inbox_root

SCHEMA = "calorie-quick-built-in-catalogue"
PROMOTION_SCHEMA = "calorie-quick-catalogue-promotion-report"
ALLOWED_CATEGORIES = {
    "fruit", "vegetables", "grains", "bakery", "meat", "fish", "eggs",
    "dairy", "fats", "pantry", "nuts-seeds", "legumes", "plant-protein",
    "prepared-meals", "beverages", "other",
}
CORE_NUTRIENTS = ("energy-kcal_100g", "proteins_100g", "carbohydrates_100g", "fat_100g")
QUANTITY = re.compile(r"([0-9]+(?:[.,][0-9]+)?)\s*(kg|g|ml|cl|l)\b", re.IGNORECASE)
LIQUID_UNITS = {"ml", "cl", "l"}
PACKAGING_NAME = re.compile(r"(?:^|\s)(?:PET|SICAFE|SLEEVE|TRAY)(?:\s|$)", re.IGNORECASE)
SIZE_SUFFIX = re.compile(r"\b\d+(?:[.,]\d+)?\s*(?:kg|g|ml|cl|l)?\s*$", re.IGNORECASE)


def _now() -> str:
    return datetime.now(timezone.utc).isoformat()


def _valid_ean(value: str) -> bool:
    if not value.isdigit() or len(value) not in (8, 12, 13, 14):
        return False
    digits = [int(char) for char in value]
    payload = digits[:-1]
    total = sum(digit * (3 if (len(payload) - index) % 2 else 1) for index, digit in enumerate(payload))
    return (10 - total % 10) % 10 == digits[-1]


def _restricted_barcode(value: str) -> bool:
    return len(value) >= 2 and value[:2].isdigit() and 20 <= int(value[:2]) <= 29


def _number(value: Any) -> float | None:
    try:
        result = float(value)
        return result if math.isfinite(result) else None
    except (TypeError, ValueError):
        return None


def _category(product: dict[str, Any]) -> str | None:
    tags_value = product.get("categories_tags")
    tags = " ".join(str(value).lower() for value in tags_value) if isinstance(tags_value, list) else str(tags_value or "").lower()
    text = " ".join((
        tags,
        str(product.get("product_name") or "").lower(),
        str(product.get("product_name_en") or "").lower(),
        str(product.get("generic_name") or "").lower(),
    ))
    rules = (
        ("beverages", ("beverages", "waters", "juices", "sodas", "cola", "pepsi", "schweppes", "sprite", "soft drink")),
        ("dairy", ("dairies", "milks", "cheeses", "yogurts")),
        ("fruit", ("fruits", "fruit-based")),
        ("vegetables", ("vegetables", "vegetable-based")),
        ("meat", ("meats", "poultries")),
        ("fish", ("seafood", "fishes")),
        ("eggs", ("eggs",)),
        ("bakery", ("breads", "pastries", "bakery")),
        ("grains", ("cereals", "grains", "pastas", "rices")),
        ("legumes", ("legumes", "beans", "lentils")),
        ("nuts-seeds", ("nuts", "seeds")),
        ("fats", ("oils", "fats", "butters")),
        ("prepared-meals", ("meals", "prepared-foods", "sandwiches", "pizzas")),
    )
    return next((category for category, needles in rules if any(needle in text for needle in needles)), None)


def _quantity(value: Any, unit: Any) -> tuple[float, bool] | None:
    amount = _number(value)
    normalized_unit = str(unit or "").strip().lower()
    if amount is None or amount <= 0:
        return None
    factors = {"g": 1.0, "kg": 1_000.0, "ml": 1.0, "cl": 10.0, "l": 1_000.0}
    factor = factors.get(normalized_unit)
    return (amount * factor, normalized_unit in LIQUID_UNITS) if factor else None


def _parsed_quantity(text: Any) -> tuple[float, bool] | None:
    match = QUANTITY.search(str(text or ""))
    if not match:
        return None
    return _quantity(match.group(1).replace(",", "."), match.group(2))


def _nutrition(nutriments: dict[str, Any]) -> tuple[dict[str, Any] | None, list[str]]:
    values = {key: _number(nutriments.get(key)) for key in CORE_NUTRIENTS}
    reasons = [f"missing {key}" for key, value in values.items() if value is None]
    if reasons:
        return None, reasons
    calories = values["energy-kcal_100g"]
    protein = values["proteins_100g"]
    carbs = values["carbohydrates_100g"]
    fat = values["fat_100g"]
    assert calories is not None and protein is not None and carbs is not None and fat is not None
    if not (0 <= calories <= 1_000) or any(value < 0 or value > 100 for value in (protein, carbs, fat)):
        reasons.append("nutrition values are outside valid per-100 bounds")
    macro_energy = protein * 4 + carbs * 4 + fat * 9
    if abs(calories - macro_energy) > max(30, calories * 0.25):
        reasons.append(f"macro energy estimate {macro_energy:.0f} kcal conflicts with declared {calories:.0f} kcal")
    fiber = _number(nutriments.get("fiber_100g"))
    if fiber is not None and not 0 <= fiber <= 100:
        reasons.append("fiber is outside valid per-100 bounds")
    if reasons:
        return None, reasons
    return {
        "calories": round(calories),
        "proteinGrams": protein,
        "carbsGrams": carbs,
        "fatGrams": fat,
        "fiberGrams": fiber,
    }, []


def _allergens(product: dict[str, Any]) -> dict[str, str]:
    mapping = {
        "gluten": "GLUTEN", "cereals-containing-gluten": "GLUTEN", "wheat": "GLUTEN",
        "crustaceans": "CRUSTACEANS", "eggs": "EGGS", "fish": "FISH",
        "peanuts": "PEANUTS", "soybeans": "SOY", "soy": "SOY", "milk": "MILK",
        "nuts": "NUTS", "tree-nuts": "NUTS", "celery": "CELERY", "mustard": "MUSTARD",
        "sesame-seeds": "SESAME", "sesame": "SESAME", "sulphites": "SULPHITES",
        "lupin": "LUPIN", "molluscs": "MOLLUSCS",
    }
    result: dict[str, str] = {}
    for field, declaration in (("traces_tags", "MAY_CONTAIN"), ("allergens_tags", "CONTAINS")):
        values = product.get(field) if isinstance(product.get(field), list) else []
        for value in values:
            key = mapping.get(str(value).split(":")[-1].lower())
            if key and (key not in result or declaration == "CONTAINS"):
                result[key] = declaration
    return result


def _off_record(
    product: dict[str, Any],
    flagged_barcodes: set[str],
    verified_ids: set[str],
) -> tuple[dict[str, Any] | None, list[str]]:
    code = str(product.get("code") or product.get("_id") or "").strip()
    reasons: list[str] = []
    if not _valid_ean(code):
        reasons.append("barcode is invalid")
    if _restricted_barcode(code):
        reasons.append("barcode is restricted-circulation")
    if code in flagged_barcodes:
        reasons.append("product was explicitly flagged by a tester")
    fallback = str(product.get("product_name") or "").strip()
    name_en = str(product.get("product_name_en") or fallback).strip()
    name_ro = str(product.get("product_name_ro") or fallback or name_en).strip()
    if not name_en or name_en.lower().startswith("open food facts product"):
        reasons.append("display name is missing or synthetic")
    if PACKAGING_NAME.search(name_en):
        reasons.append("display name contains packaging or logistics shorthand")
    if SIZE_SUFFIX.search(name_en) and any(character.isdigit() for character in name_en):
        reasons.append("display name appears to contain a package size")
    category = _category(product)
    if category is None:
        reasons.append("category could not be normalized")
    nutrition, nutrition_reasons = _nutrition(product.get("nutriments") if isinstance(product.get("nutriments"), dict) else {})
    reasons.extend(nutrition_reasons)
    if reasons:
        return None, sorted(set(reasons))

    brand = str(product.get("brands") or "").strip()
    declared_quantity = str(product.get("quantity") or "").strip()
    if declared_quantity and _parsed_quantity(declared_quantity) is None:
        return None, ["package quantity could not be normalized"]
    details = " · ".join(dict.fromkeys(value for value in (brand, declared_quantity) if value)) or "Open Food Facts"
    package = _quantity(product.get("product_quantity"), product.get("product_quantity_unit")) or _parsed_quantity(declared_quantity)
    serving = _quantity(product.get("serving_quantity"), product.get("serving_quantity_unit")) or _parsed_quantity(product.get("serving_size"))
    liquid = bool(package and package[1])
    servings: list[dict[str, Any]] = []
    if package:
        servings.append({
            "id": f"off-{code}-package",
            "label": {"en": "1 bottle" if liquid else "1 package", "ro": "1 sticlă" if liquid else "1 ambalaj"},
            "grams": round(package[0], 3),
            "suggestedAmounts": [0.5, 0.25],
            "isPackage": True,
        })
    if serving and (not package or abs(serving[0] - package[0]) > 0.001):
        servings.append({
            "id": f"off-{code}-serving", "label": {"en": "1 serving", "ro": "1 porție"},
            "grams": round(serving[0], 3), "suggestedAmounts": [1], "isPackage": False,
        })
    if liquid:
        servings.append({
            "id": f"off-{code}-millilitre", "label": {"en": "ml", "ro": "ml"},
            "grams": 1, "suggestedAmounts": [100, 250], "isPackage": False,
        })
    image_url = str(product.get("image_front_small_url") or product.get("image_front_url") or "").strip()
    record_id = f"off-{code}"
    verified = record_id in verified_ids
    return {
        "id": record_id,
        "name": {"en": name_en, "ro": name_ro or name_en},
        "detail": {"en": details, "ro": details},
        "category": category,
        "sourceId": f"cq-{'verified' if verified else 'promoted'}:OPEN_FOOD_FACTS:{code}",
        "catalogueOrigin": "OPEN_FOOD_FACTS",
        "verified": verified,
        "brand": brand or None,
        "barcode": code,
        "isPackaged": package is not None,
        "aliases": [],
        "nutritionPer100g": nutrition,
        "servings": servings,
        "allergens": _allergens(product),
        "preparations": [],
        "image": ({
            "remoteUrl": image_url,
            "attribution": "Open Food Facts contributors",
            "license": "Open Food Facts database/image terms",
        } if image_url.startswith("https://") else None),
    }, []


def _personal_record(
    food: dict[str, Any],
    unresolved_data_ids: set[str],
    verified_ids: set[str],
) -> tuple[dict[str, Any] | None, list[str]]:
    food_id = str(food.get("id") or "").strip()
    reasons: list[str] = []
    if str(food.get("reviewStatusAtExport") or food.get("reviewStatus") or "") not in {"READY_FOR_REVIEW", "EXPORTED"}:
        reasons.append("user did not opt in to catalogue review/distribution")
    if not food.get("selectedForThisExport", True):
        reasons.append("user excluded the item from this export")
    if f"data:{food_id}" in unresolved_data_ids:
        reasons.append("record has an unresolved data-quality review")
    names = food.get("name") if isinstance(food.get("name"), dict) else {}
    name_en = str(names.get("en") or names.get("ro") or "").strip()
    name_ro = str(names.get("ro") or name_en).strip()
    if not name_en:
        reasons.append("display name is missing")
    category = str(food.get("category") or "").strip()
    if category not in ALLOWED_CATEGORIES:
        reasons.append("category is missing or unsupported")
    barcode = str(food.get("barcode") or "").strip()
    if barcode and (not _valid_ean(barcode) or _restricted_barcode(barcode)):
        reasons.append("barcode is invalid or restricted-circulation")
    raw_nutrition = food.get("nutritionPer100g") if isinstance(food.get("nutritionPer100g"), dict) else {}
    nutrient_map = {
        "energy-kcal_100g": raw_nutrition.get("calories"),
        "proteins_100g": raw_nutrition.get("proteinGrams"),
        "carbohydrates_100g": raw_nutrition.get("carbsGrams"),
        "fat_100g": raw_nutrition.get("fatGrams"),
        "fiber_100g": raw_nutrition.get("fiberGrams"),
    }
    nutrition, nutrition_reasons = _nutrition(nutrient_map)
    reasons.extend(nutrition_reasons)
    measures = food.get("measures") if isinstance(food.get("measures"), list) else []
    if not measures:
        reasons.append("no practical measure is defined")
    if reasons:
        return None, sorted(set(reasons))

    servings = []
    for index, measure in enumerate(measures):
        if not isinstance(measure, dict) or _number(measure.get("grams")) is None:
            continue
        label = measure.get("label") if isinstance(measure.get("label"), dict) else {}
        servings.append({
            "id": f"{food_id}-measure-{index}",
            "label": {"en": str(label.get("en") or "1 serving"), "ro": str(label.get("ro") or label.get("en") or "1 porție")},
            "grams": float(measure["grams"]),
            "suggestedAmounts": measure.get("suggestedAmounts") if isinstance(measure.get("suggestedAmounts"), list) else [1],
            "isPackage": bool(measure.get("isPackage")),
        })
    verified = food_id in verified_ids
    return {
        "id": food_id,
        "name": {"en": name_en, "ro": name_ro},
        "detail": {"en": str(food.get("brand") or "Opt-in contribution"), "ro": str(food.get("brand") or "Contribuție voluntară")},
        "category": category,
        "sourceId": f"cq-{'verified' if verified else 'promoted'}:OPT_IN:{food_id}",
        "catalogueOrigin": "OPT_IN",
        "verified": verified,
        "brand": food.get("brand"),
        "barcode": barcode or None,
        "isPackaged": bool(food.get("isPackaged")),
        "aliases": food.get("aliases") if isinstance(food.get("aliases"), list) else [],
        "nutritionPer100g": nutrition,
        "servings": servings,
        "allergens": {
            str(value.get("key")): str(value.get("declaration"))
            for value in food.get("allergens", []) if isinstance(value, dict) and value.get("key") and value.get("declaration")
        },
        "preparations": food.get("preparations") if isinstance(food.get("preparations"), list) else [],
        "image": None,
    }, []


def _recipe_record(
    recipe: dict[str, Any],
    unresolved_data_ids: set[str],
    verified_ids: set[str],
) -> tuple[dict[str, Any] | None, dict[str, Any] | None, list[str]]:
    recipe_id = str(recipe.get("foodId") or "").strip()
    name = str(recipe.get("name") or "").strip()
    reasons: list[str] = []
    if str(recipe.get("reviewStatusAtExport") or recipe.get("reviewStatus") or "") not in {"READY_FOR_REVIEW", "EXPORTED"}:
        reasons.append("user did not opt in to recipe review/distribution")
    if not recipe.get("selectedForThisExport", True):
        reasons.append("user excluded the recipe from this export")
    if f"data:{recipe_id}" in unresolved_data_ids:
        reasons.append("recipe has an unresolved data-quality review")
    if not recipe_id or re.fullmatch(r"[A-Za-z0-9-]+", recipe_id) is None:
        reasons.append("recipe id is missing or unsupported")
    if not name:
        reasons.append("recipe name is missing")

    template = recipe.get("template") if isinstance(recipe.get("template"), dict) else None
    batches = [item for item in recipe.get("batches", []) if isinstance(item, dict)]
    source = template if template and template.get("ingredients") else (
        max(batches, key=lambda item: _number(item.get("cookedAtEpochMillis")) or 0) if batches else None
    )
    if source is None:
        reasons.append("recipe has no reusable ingredient definition")
        return None, None, sorted(set(reasons))
    yield_grams = _number(source.get("cookedYieldGrams"))
    portion_count = _number(source.get("portionCount"))
    if yield_grams is None or not 1 <= yield_grams <= 100_000:
        reasons.append("finished recipe weight is missing or invalid")
    if portion_count is None or not 1 <= portion_count <= 1_000:
        reasons.append("portion count is missing or invalid")

    ingredients: list[dict[str, Any]] = []
    total = {"calories": 0.0, "proteinGrams": 0.0, "carbsGrams": 0.0, "fatGrams": 0.0, "fiberGrams": 0.0}
    allergens: dict[str, str] = {}
    raw_ingredients = source.get("ingredients") if isinstance(source.get("ingredients"), list) else []
    if not raw_ingredients:
        reasons.append("recipe has no ingredients")
    for index, ingredient in enumerate(raw_ingredients):
        if not isinstance(ingredient, dict):
            reasons.append(f"ingredient {index + 1} is malformed")
            continue
        grams = _number(ingredient.get("grams"))
        names = ingredient.get("name") if isinstance(ingredient.get("name"), dict) else {}
        ingredient_name = str(names.get("en") or names.get("ro") or "").strip()
        if grams is None or not 0 < grams <= 50_000:
            reasons.append(f"ingredient {index + 1} has an invalid amount")
            continue
        if not ingredient_name:
            reasons.append(f"ingredient {index + 1} has no name")
            continue
        raw_nutrition = ingredient.get("nutritionPer100g") if isinstance(ingredient.get("nutritionPer100g"), dict) else {}
        nutrition, nutrition_reasons = _nutrition({
            "energy-kcal_100g": raw_nutrition.get("calories"),
            "proteins_100g": raw_nutrition.get("proteinGrams"),
            "carbohydrates_100g": raw_nutrition.get("carbsGrams"),
            "fat_100g": raw_nutrition.get("fatGrams"),
            "fiber_100g": raw_nutrition.get("fiberGrams"),
        })
        reasons.extend(f"ingredient {index + 1}: {reason}" for reason in nutrition_reasons)
        if nutrition is None:
            continue
        for key in total:
            value = nutrition.get(key)
            if value is not None:
                total[key] += float(value) * grams / 100
        declarations: dict[str, str] = {}
        for declaration in ingredient.get("allergens", []):
            if not isinstance(declaration, dict) or not declaration.get("key") or not declaration.get("declaration"):
                continue
            key = str(declaration["key"])
            value = str(declaration["declaration"])
            declarations[key] = value
            if key not in allergens or value == "CONTAINS":
                allergens[key] = value
        preparation_names = ingredient.get("preparationName") if isinstance(ingredient.get("preparationName"), dict) else {}
        ingredients.append({
            "id": f"{recipe_id}-ingredient-{index}",
            "foodId": str(ingredient.get("foodId") or f"shared-ingredient-{index}"),
            "name": {"en": ingredient_name, "ro": str(names.get("ro") or ingredient_name)},
            "preparationId": ingredient.get("preparationId"),
            "preparationName": ({
                "en": str(preparation_names.get("en") or ""),
                "ro": str(preparation_names.get("ro") or preparation_names.get("en") or ""),
            } if preparation_names else None),
            "grams": round(grams),
            "nutritionPer100g": nutrition,
            "allergens": declarations,
        })
    if reasons:
        return None, None, sorted(set(reasons))
    assert yield_grams is not None and portion_count is not None
    nutrition = {
        key: (round(value / yield_grams * 100) if key == "calories" else value / yield_grams * 100)
        for key, value in total.items()
    }
    shared_name = re.sub(r"^(?:my|home[- ]?made)\s+", "", name, flags=re.IGNORECASE).strip()
    if shared_name != name and len(ingredients) > 1:
        matching = next(
            (item for item in ingredients if shared_name.lower() in item["name"]["en"].lower()),
            ingredients[0],
        )
        additions = [
            item["name"]["en"][:1].lower() + item["name"]["en"][1:]
            for item in ingredients if item is not matching
        ]
        shared_name = matching["name"]["en"] + (f" with {', '.join(additions)}" if additions else "")
    if not shared_name:
        shared_name = ingredients[0]["name"]["en"]
    aliases = list(dict.fromkeys([name] + [item["name"]["en"] for item in ingredients]))
    verified = recipe_id in verified_ids
    source_id = f"cq-{'verified' if verified else 'promoted'}:OPT_IN:{recipe_id}"
    food = {
        "id": recipe_id,
        "name": {"en": shared_name, "ro": shared_name},
        "detail": {
            "en": f"{len(ingredients)} ingredients · {round(yield_grams)} g batch",
            "ro": f"{len(ingredients)} ingrediente · preparat de {round(yield_grams)} g",
        },
        "category": "prepared-meals",
        "sourceId": source_id,
        "catalogueOrigin": "OPT_IN",
        "verified": verified,
        "brand": None,
        "barcode": None,
        "isPackaged": False,
        "aliases": aliases,
        "nutritionPer100g": nutrition,
        "servings": [{
            "id": f"{recipe_id}-portion",
            "label": {"en": "1 portion", "ro": "1 porție"},
            "grams": round(yield_grams / portion_count, 3),
            "suggestedAmounts": [1],
            "isPackage": False,
        }],
        "allergens": allergens,
        "preparations": [],
        "image": None,
    }
    normalized = {
        "foodId": recipe_id,
        "name": shared_name,
        "originalName": name,
        "sourceId": source_id,
        "catalogueOrigin": "OPT_IN",
        "verified": verified,
        "cookedYieldGrams": round(yield_grams),
        "portionCount": round(portion_count),
        "ingredients": ingredients,
    }
    return food, normalized, []


def _review_flags(inbox: Path) -> tuple[set[str], set[str]]:
    flagged_barcodes: set[str] = set()
    unresolved_data_ids: set[str] = set()
    for path in (inbox / "bundles").glob("*/review.json"):
        try:
            review = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError):
            continue
        for item in review.get("items", []):
            if not isinstance(item, dict) or item.get("decision") == "RESOLVED":
                continue
            item_id = str(item.get("id") or "")
            if item.get("sourceType") == "PRODUCT_DATA_ISSUE":
                match = re.search(r"barcode=([0-9]+)", str(item.get("description") or ""))
                if match:
                    flagged_barcodes.add(match.group(1))
            if item.get("sourceType") == "CATALOGUE_DATA":
                unresolved_data_ids.add(item_id)
    return flagged_barcodes, unresolved_data_ids


def _verified_ids(inbox: Path) -> set[str]:
    path = inbox / "catalogue-verification.json"
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError):
        return set()
    records = data.get("verified") if isinstance(data, dict) else []
    required_checks = {"nutrition", "representativeName", "imageRightsAndFit"}
    return {
        str(record.get("id") or "").strip()
        for record in records
        if isinstance(record, dict)
        and record.get("id")
        and record.get("reviewedAt")
        and record.get("reviewedBy")
        and isinstance(record.get("checks"), dict)
        and all(record["checks"].get(check) is True for check in required_checks)
    }


def promote(shared: dict[str, Any], inbox: Path) -> tuple[list[dict[str, Any]], list[dict[str, Any]], list[dict[str, Any]]]:
    flagged_barcodes, unresolved_data_ids = _review_flags(inbox)
    verified_ids = _verified_ids(inbox)
    promoted: dict[str, dict[str, Any]] = {}
    promoted_recipes: dict[str, dict[str, Any]] = {}
    quarantined: list[dict[str, Any]] = []
    records = shared.get("foods") if isinstance(shared.get("foods"), dict) else {}
    for key in sorted(records):
        record = records[key]
        if not isinstance(record, dict):
            continue
        candidate = None
        reasons: list[str] = []
        if isinstance(record.get("openFoodFactsProduct"), dict):
            candidate, reasons = _off_record(record["openFoodFactsProduct"], flagged_barcodes, verified_ids)
        elif isinstance(record.get("normalizedFood"), dict):
            candidate, reasons = _personal_record(record["normalizedFood"], unresolved_data_ids, verified_ids)
        else:
            reasons = ["record has neither normalized food nor Open Food Facts payload"]
        if candidate:
            promoted[candidate["id"]] = candidate
        else:
            quarantined.append({
                "kind": "food",
                "id": key,
                "name": record.get("displayName"),
                "barcode": record.get("barcode"),
                "reasons": reasons,
            })
    recipe_records = shared.get("recipes") if isinstance(shared.get("recipes"), dict) else {}
    for key in sorted(recipe_records):
        wrapper = recipe_records[key]
        recipe = wrapper.get("normalizedRecipe") if isinstance(wrapper, dict) else None
        if not isinstance(recipe, dict):
            quarantined.append({"kind": "recipe", "id": key, "name": None, "barcode": None, "reasons": ["recipe payload is missing"]})
            continue
        food, normalized, reasons = _recipe_record(recipe, unresolved_data_ids, verified_ids)
        if food and normalized:
            promoted[food["id"]] = food
            promoted_recipes[normalized["foodId"]] = normalized
        else:
            quarantined.append({
                "kind": "recipe", "id": key, "name": wrapper.get("displayName"), "barcode": None, "reasons": reasons,
            })
    return (
        [promoted[key] for key in sorted(promoted)],
        [promoted_recipes[key] for key in sorted(promoted_recipes)],
        quarantined,
    )


def run(inbox: Path, output: Path, report_path: Path) -> dict[str, Any]:
    shared_path = inbox / "shared-food-catalogue.json"
    shared = json.loads(shared_path.read_text(encoding="utf-8"))
    promoted, promoted_recipes, quarantined = promote(shared, inbox)
    catalogue = {
        "schema": SCHEMA,
        "version": 1,
        "source": {
            "name": "Calorie Quick promoted catalogue",
            "url": "https://github.com/ShadoWZYE/calorie-quick-android",
            "license": "MIXED-PER-RECORD",
            "note": "Automatically validated records; provenance and attribution are retained per item.",
        },
        "foods": promoted,
        "recipes": promoted_recipes,
    }
    report = {
        "schema": PROMOTION_SCHEMA,
        "schemaVersion": 1,
        "generatedAt": _now(),
        "source": str(shared_path.resolve()),
        "output": str(output.resolve()),
        "promotedCount": len(promoted),
        "promotedRecipeCount": len(promoted_recipes),
        "quarantinedCount": len(quarantined),
        "verificationCriteria": ["nutrition", "representativeName", "imageRightsAndFit"],
        "promoted": [{
            "id": item["id"],
            "name": item["name"]["en"],
            "origin": item["catalogueOrigin"],
            "verified": item["verified"],
        } for item in promoted],
        "promotedRecipes": [{
            "id": item["foodId"], "name": item["name"], "origin": item["catalogueOrigin"], "verified": item["verified"],
        } for item in promoted_recipes],
        "quarantined": quarantined,
    }
    output.parent.mkdir(parents=True, exist_ok=True)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    temporary = output.with_suffix(".tmp")
    temporary.write_text(json.dumps(catalogue, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temporary.replace(output)
    report_path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return report


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--inbox", type=Path, default=default_inbox_root())
    parser.add_argument(
        "--output",
        type=Path,
        default=Path(__file__).parents[1] / "app/src/main/assets/catalogue/promoted_catalogue_v1.json",
    )
    parser.add_argument("--report", type=Path, default=None)
    args = parser.parse_args(argv)
    report_path = args.report or args.inbox / "promotion-report.json"
    report = run(args.inbox.resolve(), args.output.resolve(), report_path.resolve())
    print(
        f"Promoted {report['promotedCount']} foods including {report['promotedRecipeCount']} recipes; "
        f"quarantined {report['quarantinedCount']}."
    )
    print(f"Catalogue: {report['output']}")
    print(f"Report: {report_path.resolve()}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
