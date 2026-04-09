package com.example.reaccionatencion;

import android.graphics.Color;

import java.util.Random;

public class StimulusGenerator {

    private static final String[] WORDS = {
            "CASA", "ARBOL", "LUNA", "PERRO", "SOL", "MESA"
    };

    private static final String[] COLOR_NAMES = {
            "ROJO", "VERDE", "AZUL", "AMARILLO"
    };

    private static final int[] COLOR_VALUES = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW
    };

    private final Random random = new Random();

    public Stimulus next() {
        int pick = random.nextInt(3);
        StimulusType type = StimulusType.values()[pick];

        if (type == StimulusType.WORD) {
            String value = WORDS[random.nextInt(WORDS.length)];
            return new Stimulus(type, value, Color.DKGRAY);
        }

        if (type == StimulusType.NUMBER) {
            int number = 1 + random.nextInt(199);
            return new Stimulus(type, String.valueOf(number), Color.DKGRAY);
        }

        int idx = random.nextInt(COLOR_NAMES.length);
        return new Stimulus(type, COLOR_NAMES[idx], COLOR_VALUES[idx]);
    }
}
