package com.codewarrior.logica;

/** Trajeto de um dash (reto, ou com um desvio da mira assistida) e prévia dos acertos. */
public final class Caminho {
    public final float[] x = new float[3];
    public final float[] y = new float[3];
    public int n;
    public float total;
    /** Passa por gosma (dash 50% mais lento). */
    public boolean lento;
    /** Quantos bugs (e o chefão) seriam atingidos. */
    public int acertos;
    public boolean valido;
    public int destino = -1;
    /** Houve desvio da mira assistida. */
    public boolean desviou;

    public void recalcularTotal() {
        total = 0;
        for (int i = 1; i < n; i++) total += (float) Math.hypot(x[i] - x[i - 1], y[i] - y[i - 1]);
    }

    /** Ponto a uma distância d do início; devolve em out[0], out[1]. */
    public void pontoEm(float d, float[] out) {
        if (d <= 0) {
            out[0] = x[0];
            out[1] = y[0];
            return;
        }
        float acc = 0;
        for (int i = 1; i < n; i++) {
            float seg = (float) Math.hypot(x[i] - x[i - 1], y[i] - y[i - 1]);
            if (d <= acc + seg || i == n - 1) {
                float f = seg <= 0.0001f ? 1f : Math.min(1f, (d - acc) / seg);
                out[0] = x[i - 1] + (x[i] - x[i - 1]) * f;
                out[1] = y[i - 1] + (y[i] - y[i - 1]) * f;
                return;
            }
            acc += seg;
        }
        out[0] = x[n - 1];
        out[1] = y[n - 1];
    }
}
