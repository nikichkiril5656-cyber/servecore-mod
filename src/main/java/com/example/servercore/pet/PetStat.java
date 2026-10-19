package com.example.servercore.pet;

/**
 * Бонус питомца. {@code max} — значение у обычного питомца 100 уровня;
 * на 1 уровне действует 20% от него, дальше растёт линейно.
 */
public record PetStat(StatKind kind, double max) {
    public double value(Pet pet) {
        double levelFactor = 0.2 + 0.8 * (pet.level - 1) / (double) (Pet.MAX_LEVEL - 1);
        return max * pet.rarity.multiplier * levelFactor;
    }
}
