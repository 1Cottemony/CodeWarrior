package com.codewarrior.efeitos;

import java.util.ArrayList;
import java.util.Iterator;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.codewarrior.ui.Fontes;
import com.codewarrior.ui.Texto;

/** Partículas (quadrados, discos, glifos de código, anéis, faíscas) e números flutuantes. */
public final class Particulas {
    private static final int MAX = 1800;
    private static final int QUADRADO = 0, DISCO = 1, GLIFO = 2, ANEL = 3, FAISCA = 4;
    private static final String GLIFOS = "01{}<>/;=()[]&|!#";

    private final float[] x = new float[MAX], y = new float[MAX], vx = new float[MAX], vy = new float[MAX];
    private final float[] vida = new float[MAX], vidaMax = new float[MAX], tam = new float[MAX];
    private final float[] r = new float[MAX], g = new float[MAX], b = new float[MAX];
    private final float[] grav = new float[MAX], atrito = new float[MAX];
    private final byte[] tipo = new byte[MAX];
    private final char[] glifo = new char[MAX];
    private int n;
    /** Multiplicador de quantidade (intensidade dos efeitos). */
    public float quantidade = 1f;

    private static final class Popup {
        String s;
        float x, y, vida, max;
        Color c = new Color();
        int tamanho;
    }

    private final ArrayList<Popup> popups = new ArrayList<>();
    private final Color tmp = new Color();

    public void limpar() {
        n = 0;
        popups.clear();
    }

    public int quantas() {
        return n;
    }

    private void add(int tp, float px, float py, float pvx, float pvy, float vd, float tm, Color c, float gr, float at, char gl) {
        if (n >= MAX) return;
        int i = n++;
        x[i] = px; y[i] = py; vx[i] = pvx; vy[i] = pvy;
        vida[i] = vd; vidaMax[i] = vd; tam[i] = tm;
        r[i] = c.r; g[i] = c.g; b[i] = c.b;
        grav[i] = gr; atrito[i] = at; tipo[i] = (byte) tp; glifo[i] = gl;
    }

    private int q(int base) {
        return Math.max(1, Math.round(base * quantidade));
    }

    // ------------------------------------------------------------ emissores
    public void explosao(float px, float py, Color c, int qtd, float vel) {
        for (int i = 0, k = q(qtd); i < k; i++) {
            float a = MathUtils.random(MathUtils.PI2), s = vel * MathUtils.random(0.25f, 1f);
            add(QUADRADO, px, py, MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(0.3f, 0.8f),
                    MathUtils.random(3f, 8f), c, -200f, 3f, ' ');
        }
    }

    public void faiscas(float px, float py, Color c, int qtd, float vel) {
        for (int i = 0, k = q(qtd); i < k; i++) {
            float a = MathUtils.random(MathUtils.PI2), s = vel * MathUtils.random(0.4f, 1f);
            add(FAISCA, px, py, MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(0.2f, 0.5f), 1f, c, 0f, 4f, ' ');
        }
    }

    public void glifos(float px, float py, Color c, int qtd, float vel) {
        for (int i = 0, k = q(qtd); i < k; i++) {
            float a = MathUtils.random(MathUtils.PI2), s = vel * MathUtils.random(0.2f, 1f);
            add(GLIFO, px, py, MathUtils.cos(a) * s, MathUtils.sin(a) * s, MathUtils.random(0.5f, 1.1f), 1f, c, -120f, 1.5f,
                    GLIFOS.charAt(MathUtils.random(GLIFOS.length() - 1)));
        }
    }

    public void anel(float px, float py, float raio, Color c, float vd) {
        add(ANEL, px, py, 0f, 0f, vd, raio, c, 0f, 0f, ' ');
    }

    public void disco(float px, float py, Color c, float raio, float vd) {
        add(DISCO, px, py, 0f, 0f, vd, raio, c, 0f, 0f, ' ');
    }

    /** Pontinho de rastro (dash). */
    public void rastro(float px, float py, Color c, float tm) {
        add(QUADRADO, px + MathUtils.random(-4f, 4f), py + MathUtils.random(-4f, 4f), MathUtils.random(-30f, 30f),
                MathUtils.random(-30f, 30f), MathUtils.random(0.2f, 0.45f), tm, c, 0f, 2f, ' ');
    }

    public void confeteGlifo(float px, float py, Color c) {
        add(GLIFO, px, py, MathUtils.random(-120f, 120f), MathUtils.random(-40f, 260f), MathUtils.random(1.5f, 2.6f), 1f, c, -260f, 0.4f,
                GLIFOS.charAt(MathUtils.random(GLIFOS.length() - 1)));
    }

    public void numero(String s, float px, float py, Color c, int tamanho) {
        if (popups.size() > 40) popups.remove(0);
        Popup p = new Popup();
        p.s = s;
        p.x = px;
        p.y = py;
        p.vida = p.max = 0.9f;
        p.c.set(c);
        p.tamanho = tamanho;
        popups.add(p);
    }

    // ------------------------------------------------------------ ciclo
    public void atualizar(float dt) {
        for (int i = n - 1; i >= 0; i--) {
            vida[i] -= dt;
            if (vida[i] <= 0f) {
                int u = --n;
                x[i] = x[u]; y[i] = y[u]; vx[i] = vx[u]; vy[i] = vy[u]; vida[i] = vida[u]; vidaMax[i] = vidaMax[u];
                tam[i] = tam[u]; r[i] = r[u]; g[i] = g[u]; b[i] = b[u]; grav[i] = grav[u]; atrito[i] = atrito[u];
                tipo[i] = tipo[u]; glifo[i] = glifo[u];
                continue;
            }
            vy[i] += grav[i] * dt;
            float k = Math.max(0f, 1f - atrito[i] * dt);
            vx[i] *= k;
            vy[i] *= k;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
        }
        Iterator<Popup> it = popups.iterator();
        while (it.hasNext()) {
            Popup p = it.next();
            p.vida -= dt;
            p.y += 55f * dt;
            if (p.vida <= 0f) it.remove();
        }
    }

    public void desenhar(Pincel p, Fontes f) {
        p.aditivo(true);
        for (int i = 0; i < n; i++) {
            if (tipo[i] == GLIFO) continue;
            float a = Math.max(0f, vida[i] / vidaMax[i]);
            p.cor(r[i], g[i], b[i], a);
            switch (tipo[i]) {
                case QUADRADO:
                    float s = tam[i] * (0.4f + 0.6f * a);
                    p.reta(x[i] - s / 2f, y[i] - s / 2f, s, s);
                    break;
                case DISCO:
                    p.disco(x[i], y[i], tam[i] * (1f - a * 0.5f));
                    break;
                case ANEL:
                    p.anel(x[i], y[i], tam[i] * (1f - a * a), 2f + 4f * a);
                    break;
                default:
                    p.linha(x[i], y[i], x[i] - vx[i] * 0.035f, y[i] - vy[i] * 0.035f, 2f);
                    break;
            }
        }
        p.aditivo(false);
        for (int i = 0; i < n; i++) {
            if (tipo[i] != GLIFO) continue;
            float a = Math.min(1f, vida[i] / vidaMax[i] * 1.6f);
            tmp.set(r[i], g[i], b[i], 1f);
            Texto.desenhar(p, f.pequena, String.valueOf(glifo[i]), x[i], y[i], Texto.CENTRO, tmp, a);
        }
        for (Popup po : popups) {
            float a = Math.min(1f, po.vida / po.max * 1.8f);
            com.badlogic.gdx.graphics.g2d.BitmapFont fo = po.tamanho >= 2 ? f.grande : po.tamanho == 1 ? f.media : f.pequena;
            Texto.sombra(p, fo, po.s, po.x, po.y, Texto.CENTRO, po.c, a);
        }
    }
}
