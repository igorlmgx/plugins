package io.flutter.plugins.googlemaps.cozy;

public class CozyMarkerElements {
    public final CozyMarkerElement canvas;
    public final CozyMarkerElement bubble;
    public final CozyMarkerElement[] labels;
    public final CozyMarkerElement[] icons;
    public final CozyMarkerElement iconCircle;
    public final CozyMarkerElement pointer;

    public CozyMarkerElements(CozyMarkerElement canvas, CozyMarkerElement bubble, CozyMarkerElement[] labels, CozyMarkerElement[] icons, CozyMarkerElement iconCircle, CozyMarkerElement pointer) {
        this.canvas = canvas;
        this.bubble = bubble;
        this.labels = labels;
        this.icons = icons;
        this.iconCircle = iconCircle;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return canvas.toString() + bubble.toString() + labels.toString() + icons.toString() + iconCircle.toString() + pointer.toString();
    }
}