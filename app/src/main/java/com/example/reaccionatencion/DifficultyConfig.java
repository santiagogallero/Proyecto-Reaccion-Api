package com.example.reaccionatencion;

public final class DifficultyConfig {

    private DifficultyConfig() {
    }

    public static int defaultMaxTimeFor(String difficulty) {
        switch (difficulty) {
            case "Medio":
                return 15;
            case "Dificil":
                return 10;
            case "Entrenamiento":
            case "Facil":
            default:
                return 20;
        }
    }

    public static int scoreMultiplierFor(String difficulty) {
        switch (difficulty) {
            case "Medio":
                return 2;
            case "Dificil":
                return 3;
            case "Entrenamiento":
            case "Facil":
            default:
                return 1;
        }
    }

    public static boolean isTraining(String difficulty) {
        return "Entrenamiento".equals(difficulty);
    }
}
