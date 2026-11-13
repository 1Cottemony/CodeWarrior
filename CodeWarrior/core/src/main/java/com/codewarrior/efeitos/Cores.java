package com.codewarrior.efeitos;

import com.badlogic.gdx.graphics.Color;

/** Paleta: fundo azul-grafite, teclas ciano, bugs vermelho/magenta, combos amarelo/verde-limão. */
public final class Cores {
    private Cores() {
    }

    public static final Color FUNDO = Color.valueOf("0d1117");
    public static final Color PAINEL = Color.valueOf("111a26");
    public static final Color CIANO = Color.valueOf("19e6ff");
    public static final Color CIANO_ESC = Color.valueOf("0a7f95");
    public static final Color MAGENTA = Color.valueOf("ff2bd6");
    public static final Color VERMELHO = Color.valueOf("ff3b4e");
    public static final Color CORAL = Color.valueOf("ff7a66");
    public static final Color AMARELO = Color.valueOf("ffe94a");
    public static final Color LIMA = Color.valueOf("b8ff3a");
    public static final Color VERDE = Color.valueOf("3dff8a");
    public static final Color LARANJA = Color.valueOf("ff9f2e");
    public static final Color ROXO = Color.valueOf("9b5cff");
    public static final Color BRANCO = Color.valueOf("f2f8ff");
    public static final Color CINZA = Color.valueOf("7d8aa0");

    /** Cor HSV (h em 0..1) para gradientes. */
    public static Color hsv(float h, float s, float v, Color out) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        float hh = h * 6f;
        int i = (int) hh;
        float f = hh - i;
        float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return out.set(r, g, b, 1f);
    }

    public static Color mistura(Color a, Color b, float t, Color out) {
        return out.set(a.r + (b.r - a.r) * t, a.g + (b.g - a.g) * t, a.b + (b.b - a.b) * t, a.a + (b.a - a.a) * t);
    }
}
