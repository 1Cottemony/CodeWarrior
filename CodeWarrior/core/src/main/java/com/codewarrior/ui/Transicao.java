package com.codewarrior.ui;

import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;

/** Transição entre telas: faixas neon que fecham e abrem a tela. Nenhuma troca de tela é instantânea. */
public final class Transicao {
    private static final int FAIXAS = 12;
    private static final float DUR_FECHAR = 0.30f, DUR_ABRIR = 0.34f;

    private enum Estado { PARADA, FECHANDO, ABRINDO }

    private Estado estado = Estado.PARADA;
    private float t;
    private Runnable aoFechar;

    public boolean ocupada() {
        return estado != Estado.PARADA;
    }

    /** Fecha a tela, executa "aoFechar" (troca de tela) quando estiver totalmente coberta e abre de novo. */
    public void iniciar(Runnable aoFechar) {
        if (estado != Estado.PARADA) return;
        this.aoFechar = aoFechar;
        estado = Estado.FECHANDO;
        t = 0f;
    }

    public void atualizar(float dt) {
        if (estado == Estado.PARADA) return;
        t += dt;
        if (estado == Estado.FECHANDO && t >= DUR_FECHAR) {
            if (aoFechar != null) aoFechar.run();
            aoFechar = null;
            estado = Estado.ABRINDO;
            t = 0f;
        } else if (estado == Estado.ABRINDO && t >= DUR_ABRIR) {
            estado = Estado.PARADA;
        }
    }

    public void desenhar(Pincel p) {
        if (estado == Estado.PARADA) return;
        float prog = estado == Estado.FECHANDO ? Math.min(1f, t / DUR_FECHAR) : Math.min(1f, t / DUR_ABRIR);
        float largura = 1280f / FAIXAS;
        for (int i = 0; i < FAIXAS; i++) {
            float k = Math.max(0f, Math.min(1f, prog * 1.6f - (i / (float) FAIXAS) * 0.6f));
            k = k * k * (3f - 2f * k);
            float x = i * largura;
            float altura = 720f * (estado == Estado.FECHANDO ? k : 1f - k);
            if (altura <= 0.5f) continue;
            float y = estado == Estado.FECHANDO ? 720f - altura : 0f;
            p.cor(Cores.FUNDO, 1f);
            p.reta(x - 0.5f, y, largura + 1f, altura);
            p.aditivo(true);
            p.cor(i % 2 == 0 ? Cores.CIANO : Cores.MAGENTA, 0.9f);
            float borda = estado == Estado.FECHANDO ? y : y + altura;
            p.reta(x, borda - 2f, largura, 4f);
            p.aditivo(false);
        }
    }
}
