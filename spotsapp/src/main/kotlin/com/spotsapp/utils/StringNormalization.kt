package com.spotsapp.utils

/**
 * Regulariza espacios en texto que viene del cliente antes de guardarlo o compararlo:
 * quita espacios al inicio/final y colapsa espacios internos repetidos a uno solo.
 * "  Ciudad   Grande  " -> "Ciudad Grande". "" y "   " se colapsan a "" (útil junto con
 * `.ifBlank { null }` en campos opcionales).
 *
 * Se aplica en el Service (no en el DTO ni en el Mapper) para que el mismo valor
 * normalizado se use tanto al chequear duplicados como al persistir — si se
 * normalizara solo en el Mapper, un chequeo de duplicados hecho antes con el valor
 * crudo del request podría no detectar "Ciudad" contra "  Ciudad  ".
 */
fun String.normalizeSpaces(): String = trim().replace(Regex("\\s+"), " ")
