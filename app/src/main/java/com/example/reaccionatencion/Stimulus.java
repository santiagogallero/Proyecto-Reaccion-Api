package com.example.reaccionatencion;

public class Stimulus {

    public final StimulusType type;
    public final String text;
    public final int colorValue;

    public Stimulus(StimulusType type, String text, int colorValue) {
        this.type = type;
        this.text = text;
        this.colorValue = colorValue;
    }
}
