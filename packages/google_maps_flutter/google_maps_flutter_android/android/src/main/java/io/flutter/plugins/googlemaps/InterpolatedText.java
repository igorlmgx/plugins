package io.flutter.plugins.googlemaps;

public class InterpolatedText {
    String startText;
    String endText;
    float step;

    InterpolatedText(String startText, String endText, float step) {
        this.startText = startText;
        this.endText = endText;
        this.step = step;
    }

    @Override
    public String toString() {
        return startText + " " + endText + " " + step;
    }
}