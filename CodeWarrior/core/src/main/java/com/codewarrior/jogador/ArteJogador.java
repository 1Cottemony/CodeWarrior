package com.codewarrior.jogador;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;

/** O Cursor Ninja (e o clone holográfico), desenhados por código. Em escala 1 mede cerca de 44 px de altura. */
public final class ArteJogador {
    private static final Color CORPO = Color.valueOf("151c2b");
    private static final Color CORPO2 = Color.valueOf("222c42");
    private static final Color PELE = Color.valueOf("f3cfae");
    private static final Color tmp = new Color();

    private ArteJogador() {
    }

    /**
     * @param dir  1 = olhando para a direita, -1 = esquerda
     * @param t    tempo (animação)
     * @param holo versão holográfica (clone)
     * @param rosto versão grande de menu: rosto à mostra, piscadinha e katana brilhante
     */
    public static void ninja(Pincel p, float x, float y, float s, float dir, float t, boolean holo, boolean rosto, float alfa) {
        float a = alfa;
        Color corpo = holo ? Cores.CIANO : CORPO;
        Color corpo2 = holo ? Cores.CIANO : CORPO2;
        float ac = holo ? 0.35f * a : a;
        if (!holo) p.brilho(x, y - 4f * s, 30f * s);
        else p.brilho(x, y, 36f * s);

        // faixa/rabo da bandana (atrás)
        float cabY = y + 10f * s;
        p.cor(Cores.CIANO, a * 0.9f);
        float onda = (float) Math.sin(t * 11f) * 4f * s;
        float onda2 = (float) Math.sin(t * 11f + 1.2f) * 5f * s;
        p.linha(x - dir * 10f * s, cabY + 5f * s, x - dir * 22f * s, cabY + 5f * s + onda, 3.2f * s);
        p.linha(x - dir * 10f * s, cabY + 3f * s, x - dir * 20f * s, cabY + 1f * s + onda2, 2.6f * s);

        // katana nas costas
        p.cor(holo ? Cores.CIANO : Cores.BRANCO, a * (holo ? 0.6f : 0.9f));
        p.linha(x - dir * 8f * s, y - 12f * s, x + dir * 12f * s, y + 20f * s, 2f * s);
        p.cor(Cores.AMARELO, a * 0.9f);
        p.linha(x - dir * 9f * s, y - 12.5f * s, x - dir * 5f * s, y - 7f * s, 3f * s);

        // pernas
        p.cor(corpo, ac);
        p.reta(x - 8f * s, y - 20f * s, 6f * s, 8f * s);
        p.reta(x + 2f * s, y - 20f * s, 6f * s, 8f * s);
        // corpo
        p.cor(corpo, ac);
        p.retaCortada(x - 10f * s, y - 14f * s, 20f * s, 18f * s, 3f * s);
        p.cor(corpo2, ac);
        p.reta(x - 10f * s, y - 6f * s, 20f * s, 3f * s);
        p.cor(Cores.CIANO, a * (holo ? 0.9f : 0.8f));
        p.reta(x - 10f * s, y - 3f * s, 20f * s, 1.6f * s);
        // braços
        p.cor(corpo, ac);
        p.reta(x - 14f * s, y - 10f * s, 5f * s, 10f * s);
        p.reta(x + 9f * s, y - 10f * s, 5f * s, 10f * s);

        // cabeça
        p.cor(corpo, ac);
        p.disco(x, cabY, 11f * s);
        if (rosto) {
            p.cor(PELE, a);
            p.disco(x + dir * 1f * s, cabY - 0.5f * s, 8.2f * s);
            p.cor(CORPO, a);
            // olhos (um piscando)
            p.reta(x - 5.5f * s, cabY - 1f * s, 3f * s, 4f * s);
            p.reta(x + 2.5f * s, cabY + 0.5f * s, 4f * s, 1.4f * s);
            p.cor(Cores.BRANCO, a);
            p.reta(x - 4.8f * s, cabY + 1.4f * s, 1.2f * s, 1.2f * s);
            p.cor(Color.valueOf("e9906f"), a * 0.7f);
            p.disco(x - 8f * s, cabY - 3f * s, 1.8f * s);
            p.disco(x + 8f * s, cabY - 3f * s, 1.8f * s);
        } else {
            p.cor(holo ? Cores.CIANO : Cores.BRANCO, a * (holo ? 0.5f : 0.92f));
            p.reta(x - 8f * s, cabY - 1f * s, 16f * s, 5f * s);
            p.cor(Cores.CIANO, a);
            p.reta(x - 5.5f * s, cabY, 3f * s, 3f * s);
            p.reta(x + 2.5f * s, cabY, 3f * s, 3f * s);
        }
        // bandana
        p.cor(Cores.CIANO, a);
        p.reta(x - 11f * s, cabY + 4.2f * s, 22f * s, 4.2f * s);
        p.cor(Cores.BRANCO, a * 0.8f);
        p.reta(x - 11f * s, cabY + 7.2f * s, 22f * s, 1f * s);

        if (rosto) {
            // katana brilhante na frente
            p.aditivo(true);
            p.cor(Cores.CIANO, 0.9f * a);
            p.linha(x + dir * 16f * s, y - 16f * s, x + dir * 26f * s, y + 26f * s, 2.4f * s);
            p.cor(Cores.BRANCO, 0.9f * a);
            p.linha(x + dir * 16f * s, y - 16f * s, x + dir * 26f * s, y + 26f * s, 0.9f * s);
            p.aditivo(false);
        }
        if (holo) {
            // linhas de varredura do holograma
            p.cor(Cores.FUNDO, 0.55f * a);
            for (int i = 0; i < 9; i++) {
                float yy = y - 20f * s + ((i * 5f * s + t * 18f * s) % (46f * s));
                p.reta(x - 15f * s, yy, 30f * s, 1.2f * s);
            }
        }
    }

    /** Cursor piscante "_" que aparece sob o ninja quando ele está parado (identidade do personagem). */
    public static void cursorPiscando(Pincel p, float x, float y, float t, float alfa) {
        if (((int) (t * 2.2f)) % 2 == 0) {
            p.cor(Cores.CIANO, 0.9f * alfa);
            p.reta(x - 7f, y, 14f, 3f);
        }
    }

    /** Silhueta de rastro (afterimage) do dash. */
    public static void fantasma(Pincel p, float x, float y, float dir, float alfa) {
        p.aditivo(true);
        p.cor(Cores.CIANO, 0.35f * alfa);
        p.retaCortada(x - 11f, y - 16f, 22f, 36f, 4f);
        p.aditivo(false);
        tmp.set(Cores.BRANCO);
    }
}
