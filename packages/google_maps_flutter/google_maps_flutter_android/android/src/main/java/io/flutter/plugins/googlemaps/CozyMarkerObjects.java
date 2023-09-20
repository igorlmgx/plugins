package io.flutter.plugins.googlemaps;

public class CozyMarkerObjects {
    public final CozyMarkerObject canvas;
    public final CozyMarkerObject bubble;
    public final CozyMarkerObject[] labels;
    public final CozyMarkerObject icon;
    public final CozyMarkerObject iconCircle;
    public final CozyMarkerObject pointer;

    public CozyMarkerObjects(CozyMarkerObject canvas, CozyMarkerObject bubble, CozyMarkerObject[] labels, CozyMarkerObject icon, CozyMarkerObject iconCircle, CozyMarkerObject pointer) {
        this.canvas = canvas;
        this.bubble = bubble;
        this.labels = labels;
        this.icon = icon;
        this.iconCircle = iconCircle;
        this.pointer = pointer;
    }

    @Override
    public String toString() {
        return canvas.toString() + bubble.toString() + labels.toString() + icon.toString() + iconCircle.toString() + pointer.toString();
    }
}