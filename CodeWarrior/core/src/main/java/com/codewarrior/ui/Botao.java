package com.codewarrior.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;

/** Botão neon (preenchido ou só contorno), com destaque ao passar o mouse ou receber o foco do teclado. */
public final class Botao {
    public String texto;
    public float x, y, w, h;
    public Color cor;
    public boolean preenchido;
    public boolean foco;
    public float hover;
    public float clique;

    public Botao(String texto, float x, float y, float w, float h, Color cor, boolean preenchido) {
        this.texto = texto;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.cor = cor;
        this.preenchido = preenchido;
    }

    public boolean contem(float mx, float my) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    public void atualizar(float dt, boolean sobre) {
        float alvo = (sobre || foco) ? 1f : 0f;
        hover += (alvo - hover) * Math.min(1f, dt * 14f);
        clique = Math.max(0f, clique - dt * 4f);
    }

    public void desenhar(Pincel p, BitmapFont fonte, float tempo, float alfa, float dx) {
        float esc = 1f + 0.05f * hover - 0.04f * clique;
        float ww = w * esc, hh = h * esc;
        float bx = x + dx - (ww - w) / 2f, by = y - (hh - h) / 2f;
        float corte = Math.min(12f, hh * 0.22f);
        if (preenchido) {
            p.cor(cor, alfa * (0.88f + 0.12f * hover));
            p.retaCortada(bx, by, ww, hh, corte);
            p.aditivo(true);
            p.cor(Cores.BRANCO, 0.18f * hover * alfa);
            p.retaCortada(bx, by, ww, hh, corte);
            p.aditivo(false);
            p.cor(cor, alfa);
            p.bordaCortada(bx, by, ww, hh, corte, 3f);
        } else {
            p.cor(0.04f, 0.07f, 0.11f, 0.88f * alfa);
            p.retaCortada(bx, by, ww, hh, corte);
            p.cor(cor, (0.10f + 0.12f * hover) * alfa);
            p.retaCortada(bx, by, ww, hh, corte);
            p.cor(cor, alfa);
            p.bordaCortada(bx, by, ww, hh, corte, 3f);
        }
        p.halo(bx, by, ww, hh, cor, (0.5f + hover) * alfa);
        Color txt = preenchido ? Cores.FUNDO : cor;
        Texto.desenhar(p, fonte, texto, bx + ww / 2f, by + hh / 2f, Texto.CENTRO, preenchido ? txt : Cores.BRANCO, alfa);
        if (foco) {
            // mira de seleção
            float cx = bx + ww - 18f, cy = by + hh / 2f;
            float pulso = 1f + 0.12f * (float) Math.sin(tempo * 8f);
            p.cor(preenchido ? Cores.FUNDO : cor, alfa);
            p.anel(cx, cy, 9f * pulso, 2f);
            p.linha(cx - 14f * pulso, cy, cx - 5f, cy, 2f);
            p.linha(cx + 5f, cy, cx + 14f * pulso, cy, 2f);
        }
    }
}
