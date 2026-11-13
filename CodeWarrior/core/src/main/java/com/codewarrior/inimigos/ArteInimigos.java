package com.codewarrior.inimigos;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Inimigo;

/** Desenho dos 3 bugs. Cada um tem forma própria (triângulo, gota, losango), além da cor: leitura fácil. */
public final class ArteInimigos {
    private static final Color tmp = new Color();
    private static final float[] pts = new float[8];

    private ArteInimigos() {
    }

    public static void desenhar(Pincel p, Inimigo in, float t) {
        float a = 1f;
        if (in.spawn > 0f) {
            a = Math.max(0.15f, 1f - in.spawn / 0.7f);
            // portal de surgimento: anel que encolhe
            float k = in.spawn / 0.7f;
            p.aditivo(true);
            p.cor(corDe(in.tipo), 0.9f * (1f - k) + 0.1f);
            p.anel(in.x, in.y, 14f + 48f * k, 3f);
            p.cor(Cores.BRANCO, 0.5f * (1f - k));
            p.anel(in.x, in.y, 6f + 24f * k, 2f);
            p.aditivo(false);
        }
        float flash = in.flash;
        switch (in.tipo) {
            case SYNTAX:
                syntax(p, in.x, in.y, in.anim, a, flash, in.pouso > 0f, in.stun > 0f);
                break;
            case LEAK:
                leak(p, in.x, in.y, in.anim, a, flash, in.stun > 0f);
                break;
            default:
                loop(p, in.x, in.y, in.anim, a, flash, in.quebrado, in.stun > 0f, in.orbVel);
                break;
        }
        if (in.stun > 0f) {
            // estrelinhas de atordoamento
            p.cor(Cores.AMARELO, 0.9f);
            for (int i = 0; i < 3; i++) {
                float an = t * 5f + i * 2.09f;
                p.disco(in.x + (float) Math.cos(an) * 18f, in.y + in.raio + 10f + (float) Math.sin(an) * 4f, 2.5f);
            }
        }
    }

    public static Color corDe(Inimigo.Tipo tp) {
        return tp == Inimigo.Tipo.SYNTAX ? Cores.VERMELHO : tp == Inimigo.Tipo.LEAK ? Cores.MAGENTA : Cores.CORAL;
    }

    private static Color fl(Color c, float flash) {
        return Cores.mistura(c, Cores.BRANCO, Math.min(1f, flash), tmp);
    }

    /** Triângulo vermelho com antenas, olhos amarelos e perninhas. */
    public static void syntax(Pincel p, float x, float y, float t, float a, float flash, boolean pousando, boolean atordoado) {
        float tremor = pousando ? (float) Math.sin(t * 60f) * 1.6f : 0f;
        x += tremor;
        p.brilho(x, y, 34f);
        // pernas
        p.cor(Cores.VERMELHO, a);
        for (int i = -1; i <= 1; i++) {
            float w = (float) Math.sin(t * 16f + i * 2f) * 3f;
            p.linha(x + i * 9f, y - 12f, x + i * 12f + w, y - 21f, 2.4f);
        }
        // antenas
        p.linha(x - 6f, y + 12f, x - 13f, y + 25f, 2f);
        p.linha(x + 6f, y + 12f, x + 13f, y + 25f, 2f);
        p.cor(Cores.AMARELO, a);
        p.disco(x - 13f, y + 26f, 3f);
        p.disco(x + 13f, y + 26f, 3f);
        // corpo
        p.cor(fl(Color.valueOf("5a0f1c"), flash), a);
        p.tri(x - 20f, y - 14f, x + 20f, y - 14f, x, y + 19f);
        p.cor(fl(Cores.VERMELHO, flash), a);
        p.sd.line(x - 20f, y - 14f, x + 20f, y - 14f, 3f);
        p.sd.line(x + 20f, y - 14f, x, y + 19f, 3f);
        p.sd.line(x, y + 19f, x - 20f, y - 14f, 3f);
        // olhos
        p.cor(pousando ? Cores.BRANCO : Cores.AMARELO, a);
        p.reta(x - 9f, y - 5f, 6f, 8f);
        p.reta(x + 3f, y - 5f, 6f, 8f);
        p.cor(Cores.FUNDO, a);
        p.reta(x - 7f, y - 3f, 2f, 4f);
        p.reta(x + 5f, y - 3f, 2f, 4f);
    }

    /** Gota magenta que escorre, com olhos aflitos. */
    public static void leak(Pincel p, float x, float y, float t, float a, float flash, boolean atordoado) {
        p.brilho(x, y, 40f);
        float respira = (float) Math.sin(t * 4f) * 1.5f;
        p.cor(fl(Color.valueOf("6a0f5c"), flash), a);
        p.disco(x, y + 2f, 22f + respira);
        p.reta(x - 22f, y - 14f, 44f, 14f);
        p.cor(fl(Cores.MAGENTA, flash), a);
        p.anel(x, y + 2f, 22f + respira, 3f);
        // pingos
        for (int i = -1; i <= 1; i++) {
            float len = 8f + (float) Math.abs(Math.sin(t * 3f + i * 1.7f)) * 10f;
            p.cor(fl(Cores.MAGENTA, flash), a);
            p.reta(x + i * 12f - 3f, y - 14f - len, 6f, len);
            p.disco(x + i * 12f, y - 14f - len, 4f);
        }
        // olhos
        p.cor(Cores.BRANCO, a);
        p.disco(x - 8f, y + 6f, 6.5f);
        p.disco(x + 8f, y + 6f, 6.5f);
        p.cor(Cores.FUNDO, a);
        p.disco(x - 7f + (float) Math.sin(t * 2f) * 1.5f, y + 5f, 3f);
        p.disco(x + 9f + (float) Math.sin(t * 2f) * 1.5f, y + 5f, 3f);
        // boquinha triste
        p.cor(Cores.FUNDO, a);
        p.arco(x, y - 6f, 5f, 30f, 150f, 2f);
    }

    /** Losango coral com carinha debochada e setas de órbita. */
    public static void loop(Pincel p, float x, float y, float t, float a, float flash, boolean quebrado, boolean atordoado, float orbVel) {
        p.brilho(x, y, 38f);
        Color base = quebrado ? Cores.LIMA : Cores.CORAL;
        // anel de órbita com setas
        float giro = (orbVel >= 0f ? 1f : -1f) * t * 140f;
        p.cor(base, a * 0.85f);
        for (int i = 0; i < 2; i++) {
            float ini = giro + i * 180f + 10f;
            if (quebrado && i == 1) continue;
            p.arco(x, y, 30f, ini, ini + 110f, 2.6f);
            float ang = (float) Math.toRadians(ini + 110f);
            float hx = x + 30f * (float) Math.cos(ang), hy = y + 30f * (float) Math.sin(ang);
            float tx = -(float) Math.sin(ang), ty = (float) Math.cos(ang);
            p.tri(hx + tx * 8f, hy + ty * 8f, hx - ty * 5f, hy + tx * 5f, hx + ty * 5f, hy - tx * 5f);
        }
        // losango
        float r = 21f;
        pts[0] = x; pts[1] = y + r + 3f;
        pts[2] = x + r; pts[3] = y;
        pts[4] = x; pts[5] = y - r - 3f;
        pts[6] = x - r; pts[7] = y;
        p.cor(fl(Color.valueOf("6b2a22"), flash), a);
        p.poligono(pts, 4);
        p.cor(fl(base, flash), a);
        p.contorno(pts, 4, 3f);
        // carinha
        p.cor(Cores.FUNDO, a);
        p.reta(x - 9f, y + 3f, 7f, 3f);
        p.reta(x + 2f, y + 3f, 7f, 3f);
        p.arco(x, y - 5f, 6f, 200f, 340f, 2f);
        if (quebrado) {
            p.cor(Cores.BRANCO, a);
            p.linha(x - 3f, y + 20f, x + 2f, y + 8f, 2f);
            p.linha(x + 2f, y + 8f, x - 2f, y - 2f, 2f);
            p.linha(x - 2f, y - 2f, x + 5f, y - 14f, 2f);
        }
    }
}
