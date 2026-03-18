package br.com.wayvista.minimap.data;

public final class PlayerSettings {
    private boolean showShared;
    private boolean minimapEnabled;

    public PlayerSettings(boolean showShared, boolean minimapEnabled) {
        this.showShared = showShared;
        this.minimapEnabled = minimapEnabled;
    }

    public static PlayerSettings defaults() {
        return new PlayerSettings(true, true);
    }

    public boolean showShared() {
        return showShared;
    }

    public void setShowShared(boolean showShared) {
        this.showShared = showShared;
    }

    public boolean minimapEnabled() {
        return minimapEnabled;
    }

    public void setMinimapEnabled(boolean minimapEnabled) {
        this.minimapEnabled = minimapEnabled;
    }
}
