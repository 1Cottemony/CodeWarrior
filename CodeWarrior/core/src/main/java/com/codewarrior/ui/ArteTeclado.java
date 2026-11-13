package com.codewarrior.ui;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Teclado;

/** Teclas como "blocos de memória" neon (cantos picotados, contorno duplo), e as trepadeiras de corrupção. */
public final class ArteTeclado {
    private static final Color tmp = new Color();

    private ArteTeclado() {
    }

    /**
     * @param press   0..1 brilho de tecla recém-usada
     * @param trava   segundos restantes da tranca (0 = livre)
     * @param gosma   segundos restantes de gosma (0 = limpa)
     * @param par     índice do par (0..2) ou -1
     * @param pulso   0..1 pulsação do destaque do par (1 = armado)
     * @param alvo    tecla-alvo da prévia do dash
     * @param alfa    visibilidade geral (escurece no modo terminal)
     */
    public static void tecla(Pincel p, Fontes f, Teclado.Tecla t, float cx, float cy, float tempo, float press, float trava,
                             float gosma, int par, float pulso, boolean alvo, float alfa) {
        float w = t.pw - 7f, h = t.ph - 7f, x = cx - w / 2f, y = cy - h / 2f;
        boolean dest = t.tipo == Teclado.Tipo.DESTINO;
        boolean indisp = t.tipo == Teclado.Tipo.INDISPONIVEL;
        Color borda = dest ? Cores.CIANO : t.tipo == Teclado.Tipo.RESERVADA ? Cores.ROXO : Cores.CINZA;
        float a = alfa * (indisp ? 0.28f : t.tipo == Teclado.Tipo.RESERVADA ? 0.7f : 1f);

        p.cor(0.035f, 0.09f, 0.13f, 0.93f * alfa * (indisp ? 0.6f : 1f));
        p.retaCortada(x, y, w, h, 6f);
        if (press > 0f) {
            p.aditivo(true);
            p.cor(Cores.CIANO, 0.38f * press * alfa);
            p.retaCortada(x, y, w, h, 6f);
            p.aditivo(false);
        }
        // brilho suave nas teclas de destino
        if (dest) {
            p.aditivo(true);
            p.cor(Cores.CIANO, 0.07f * alfa);
            p.retaCortada(x - 2f, y - 2f, w + 4f, h + 4f, 7f);
            p.aditivo(false);
        }
        p.cor(borda, a);
        p.bordaCortada(x, y, w, h, 6f, 2.6f);
        p.cor(borda, a * 0.35f);
        p.bordaCortada(x + 5f, y + 5f, w - 10f, h - 10f, 4f, 1.2f);

        // pares de combo
        if (par >= 0 && dest) {
            float k = 0.45f + 0.55f * pulso;
            p.aditivo(true);
            p.cor(Cores.AMARELO, 0.55f * k * alfa);
            p.bordaCortada(x - 3f, y - 3f, w + 6f, h + 6f, 7f, 2.4f);
            p.aditivo(false);
            p.cor(Cores.LIMA, 0.9f * k * alfa);
            p.reta(x + 4f, y + h - 8f, 8f, 3f);
            p.reta(x + 4f, y + h - 8f, 3f, 8f > 0 ? 6f : 0f);
        }

        // rótulos
        Color rot = dest ? Cores.BRANCO : Cores.CINZA;
        boolean longo = t.rotulo.length() > 1;
        if (t.rotuloShift != null) {
            Texto.desenhar(p, f.pequena, t.rotuloShift, cx, cy + 14f, Texto.CENTRO, rot, a * 0.65f);
            Texto.desenhar(p, f.media, t.rotulo, cx, cy - 9f, Texto.CENTRO, rot, a);
        } else if (longo) {
            Texto.desenhar(p, f.pequena, t.rotulo, cx, cy, Texto.CENTRO, rot, a * 0.85f);
        } else {
            Texto.desenhar(p, f.media, t.rotulo, cx, cy, Texto.CENTRO, rot, a);
        }

        // gosma
        if (gosma > 0f) {
            float g = Math.min(1f, gosma / 1.2f);
            p.cor(Cores.MAGENTA, 0.42f * g * alfa);
            p.retaCortada(x, y, w, h, 6f);
            p.cor(Cores.MAGENTA, 0.9f * g * alfa);
            for (int i = 0; i < 3; i++) {
                float bx = x + w * (0.2f + 0.3f * i);
                float len = 6f + (float) Math.abs(Math.sin(tempo * 2.2f + i * 2f)) * 10f;
                p.reta(bx - 2f, y - len + 2f, 4f, len);
                p.disco(bx, y - len + 2f, 3.2f);
            }
            p.cor(Cores.BRANCO, 0.5f * g * alfa);
            p.disco(cx + 12f, cy + 10f + (float) Math.sin(tempo * 3f) * 3f, 3f);
        }
        // tranca
        if (trava > 0f) {
            float g = Math.min(1f, trava / 0.8f);
            p.cor(Cores.VERMELHO, 0.35f * g * alfa);
            p.retaCortada(x, y, w, h, 6f);
            p.cor(Cores.VERMELHO, 0.9f * g * alfa);
            p.bordaCortada(x, y, w, h, 6f, 3f);
            p.cor(Cores.BRANCO, 0.95f * g * alfa);
            p.reta(cx - 9f, cy - 10f, 18f, 13f);
            p.arco(cx, cy + 3f, 6.5f, 0f, 180f, 2.6f);
            p.cor(Cores.VERMELHO, g * alfa);
            p.reta(cx - 1.5f, cy - 6f, 3f, 6f);
            // contagem regressiva da tranca (anel)
            p.cor(Cores.VERMELHO, 0.9f * g * alfa);
            p.arco(cx, cy, 24f, 90f, 90f + 360f * Math.min(1f, trava / 4f), 2.2f);
        }
        if (alvo) {
            p.aditivo(true);
            p.cor(Cores.BRANCO, 0.5f + 0.4f * (float) Math.sin(tempo * 14f));
            p.bordaCortada(x - 4f, y - 4f, w + 8f, h + 8f, 8f, 2.4f);
            p.aditivo(false);
        }
    }

    /** Trepadeiras e rachaduras verdes que crescem nas bordas conforme as ondas avançam (só visual). */
    public static void trepadeiras(Pincel p, Teclado tc, int nivel, float tempo, float alfa) {
        int qtd = Math.min(16, nivel * 3);
        for (int i = 0; i < qtd; i++) {
            Teclado.Tecla t = tc.teclas[(i * 17 + 5) % tc.teclas.length];
            float sx = t.cx + ((i % 3) - 1) * t.pw * 0.35f;
            float sy = t.cy + (((i / 3) % 3) - 1) * t.ph * 0.35f;
            float dir = (i % 2 == 0 ? 1f : -1f);
            float px = sx, py = sy;
            p.cor(Cores.VERDE, 0.30f * alfa);
            for (int s = 0; s < 7; s++) {
                float nx = px + dir * (7f + (float) Math.sin(i * 3.1f + s) * 4f);
                float ny = py + 8f * (float) Math.sin(tempo * 0.8f + i + s * 0.9f) + (s % 2 == 0 ? 5f : -3f);
                p.linha(px, py, nx, ny, 2f);
                if (s % 2 == 1) p.disco(nx, ny + 3f, 2.5f);
                px = nx;
                py = ny;
            }
        }
    }
}
