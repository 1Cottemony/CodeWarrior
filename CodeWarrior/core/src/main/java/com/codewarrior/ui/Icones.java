package com.codewarrior.ui;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Habilidade;

/** Corações e ícones das habilidades, desenhados por código. */
public final class Icones {
    private Icones() {
    }

    /** Coração centrado em (x, y) com largura ~s. */
    public static void coracao(Pincel p, float x, float y, float s, boolean cheio, float pulso, float alfa) {
        float k = s * (1f + 0.12f * pulso);
        if (cheio) {
            p.brilho(x, y, k * 0.9f);
            p.cor(Cores.VERMELHO, alfa);
        } else {
            p.cor(Cores.CINZA, 0.35f * alfa);
        }
        p.disco(x - 0.23f * k, y + 0.12f * k, 0.28f * k);
        p.disco(x + 0.23f * k, y + 0.12f * k, 0.28f * k);
        p.tri(x - 0.49f * k, y + 0.04f * k, x + 0.49f * k, y + 0.04f * k, x, y - 0.46f * k);
        if (cheio) {
            p.cor(Cores.BRANCO, 0.55f * alfa);
            p.disco(x - 0.28f * k, y + 0.2f * k, 0.07f * k);
        } else {
            p.cor(Cores.FUNDO, 0.8f * alfa);
            p.disco(x - 0.23f * k, y + 0.12f * k, 0.17f * k);
            p.disco(x + 0.23f * k, y + 0.12f * k, 0.17f * k);
            p.tri(x - 0.33f * k, y + 0.03f * k, x + 0.33f * k, y + 0.03f * k, x, y - 0.3f * k);
        }
    }

    public static Color corDe(Habilidade h) {
        switch (h) {
            case DESFAZER: return Cores.CIANO;
            case COPIAR_COLAR: return Cores.LIMA;
            case GIT_PUSH: return Cores.LARANJA;
            default: return Cores.VERMELHO;
        }
    }

    /** Ícone da habilidade dentro de um quadrado de lado s, centrado em (x, y). */
    public static void habilidade(Pincel p, Habilidade h, float x, float y, float s, float alfa) {
        p.cor(corDe(h), alfa);
        switch (h) {
            case DESFAZER:
                p.arco(x, y - 0.02f * s, 0.24f * s, 30f, 300f, 0.07f * s);
                float ang = (float) Math.toRadians(300f);
                float ax = x + 0.24f * s * (float) Math.cos(ang), ay = y - 0.02f * s + 0.24f * s * (float) Math.sin(ang);
                p.tri(ax - 0.13f * s, ay + 0.02f * s, ax + 0.07f * s, ay + 0.1f * s, ax + 0.06f * s, ay - 0.12f * s);
                break;
            case COPIAR_COLAR:
                p.borda(x - 0.25f * s, y - 0.17f * s, 0.28f * s, 0.34f * s, 0.06f * s);
                p.cor(corDe(h), alfa * 0.55f);
                p.reta(x - 0.06f * s, y - 0.25f * s, 0.28f * s, 0.34f * s);
                p.cor(corDe(h), alfa);
                p.borda(x - 0.06f * s, y - 0.25f * s, 0.28f * s, 0.34f * s, 0.06f * s);
                break;
            case GIT_PUSH:
                p.linha(x, y - 0.24f * s, x, y + 0.14f * s, 0.07f * s);
                p.tri(x - 0.16f * s, y + 0.1f * s, x + 0.16f * s, y + 0.1f * s, x, y + 0.3f * s);
                p.disco(x, y - 0.26f * s, 0.07f * s);
                p.linha(x - 0.2f * s, y - 0.32f * s, x + 0.2f * s, y - 0.32f * s, 0.05f * s);
                break;
            default:
                p.borda(x - 0.17f * s, y - 0.25f * s, 0.34f * s, 0.34f * s, 0.06f * s);
                p.reta(x - 0.25f * s, y + 0.12f * s, 0.5f * s, 0.06f * s);
                p.reta(x - 0.07f * s, y + 0.18f * s, 0.14f * s, 0.05f * s);
                p.linha(x - 0.07f * s, y - 0.18f * s, x - 0.07f * s, y + 0.04f * s, 0.04f * s);
                p.linha(x + 0.07f * s, y - 0.18f * s, x + 0.07f * s, y + 0.04f * s, 0.04f * s);
                break;
        }
    }

    /** Casinha da habilidade na barra inferior: ícone, nome, recarga em segundos. */
    public static void slot(Pincel p, Fontes f, Habilidade h, float x, float y, float s, float restante, float total, boolean armado,
                            float tempo, float alfa) {
        boolean pronto = restante <= 0f;
        Color cor = corDe(h);
        p.cor(0.04f, 0.09f, 0.14f, 0.92f * alfa);
        p.retaCortada(x - s / 2f, y - s / 2f, s, s, 7f);
        if (pronto) {
            p.aditivo(true);
            p.cor(cor, (0.10f + 0.07f * (float) Math.sin(tempo * 4f)) * alfa);
            p.retaCortada(x - s / 2f - 2f, y - s / 2f - 2f, s + 4f, s + 4f, 8f);
            p.aditivo(false);
        }
        p.cor(cor, (pronto ? 1f : 0.45f) * alfa);
        p.bordaCortada(x - s / 2f, y - s / 2f, s, s, 7f, 2.6f);
        habilidade(p, h, x, y, s, (pronto ? 1f : 0.55f) * alfa);
        if (!pronto) {
            float frac = Math.min(1f, restante / Math.max(0.01f, total));
            p.cor(Cores.FUNDO, 0.72f * alfa);
            p.reta(x - s / 2f + 3f, y - s / 2f + 3f, s - 6f, (s - 6f) * frac);
            Texto.sombra(p, f.media, String.valueOf((int) Math.ceil(restante)), x, y, Texto.CENTRO, Cores.BRANCO, alfa);
        }
        if (armado) {
            p.aditivo(true);
            p.cor(Cores.LIMA, 0.6f + 0.4f * (float) Math.sin(tempo * 12f));
            p.bordaCortada(x - s / 2f - 4f, y - s / 2f - 4f, s + 8f, s + 8f, 9f, 2.5f);
            p.aditivo(false);
        }
        Texto.desenhar(p, f.pequena, h.nome, x, y - s / 2f - 15f, Texto.CENTRO, pronto ? Cores.BRANCO : Cores.CINZA, 0.9f * alfa);
    }
}
