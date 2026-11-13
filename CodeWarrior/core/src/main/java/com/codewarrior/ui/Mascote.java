package com.codewarrior.ui;

import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;

/** O mascote: um cursor "_" dentro de uma janelinha de terminal, de bandana, que fala com humor de programador. */
public final class Mascote {
    private String texto = "";
    private String quebrado = "";
    private float t;

    public void falar(String s) {
        texto = s;
        quebrado = null;
        t = 0f;
    }

    public void atualizar(float dt) {
        t += dt;
    }

    public boolean terminouDeDigitar() {
        return quebrado == null || (int) (t * 48f) >= quebrado.length();
    }

    /** Desenha só a janelinha do mascote, centrada em (x, y). */
    public static void personagem(Pincel p, float x, float y, float s, float tempo, float alfa) {
        float w = 96f * s, h = 80f * s;
        float bx = x - w / 2f, by = y - h / 2f;
        p.brilho(x, y, 80f * s);
        p.cor(0.03f, 0.08f, 0.12f, 0.96f * alfa);
        p.retaCortada(bx, by, w, h, 6f * s);
        p.cor(Cores.CIANO, alfa);
        p.bordaCortada(bx, by, w, h, 6f * s, 3f);
        // barra de título com 3 bolinhas
        p.cor(Cores.CIANO, 0.25f * alfa);
        p.reta(bx + 3f, by + h - 14f * s, w - 6f, 11f * s);
        p.cor(Cores.VERMELHO, alfa);
        p.disco(bx + 10f * s, by + h - 8.5f * s, 2.6f * s);
        p.cor(Cores.AMARELO, alfa);
        p.disco(bx + 18f * s, by + h - 8.5f * s, 2.6f * s);
        p.cor(Cores.VERDE, alfa);
        p.disco(bx + 26f * s, by + h - 8.5f * s, 2.6f * s);
        // bandana + olhos
        float onda = (float) Math.sin(tempo * 9f) * 4f * s;
        p.cor(Cores.CIANO, alfa);
        p.reta(bx - 4f * s, by + h * 0.50f, w + 8f * s, 8f * s);
        p.linha(bx + w + 4f * s, by + h * 0.50f + 4f * s, bx + w + 20f * s, by + h * 0.50f + 8f * s + onda, 4f * s);
        p.cor(Cores.BRANCO, alfa);
        p.reta(bx + w * 0.22f, by + h * 0.52f, w * 0.20f, 4f * s);
        p.reta(bx + w * 0.58f, by + h * 0.52f, w * 0.20f, 4f * s);
        // prompt >_
        p.cor(Cores.CIANO, alfa);
        float px = bx + w * 0.28f, py = by + h * 0.27f;
        p.linha(px, py + 8f * s, px + 10f * s, py, 3.2f * s);
        p.linha(px + 10f * s, py, px, py - 8f * s, 3.2f * s);
        if (((int) (tempo * 2.4f)) % 2 == 0) p.reta(px + 18f * s, py - 9f * s, 18f * s, 4f * s);
    }

    /** Balão de fala com efeito de máquina de escrever. (bx, by) = canto inferior esquerdo. */
    public void balao(Pincel p, Fontes f, float bx, float by, float bw, float bh, float tempo, float alfa, float pontaX, float pontaY) {
        if (quebrado == null) quebrado = Texto.quebrar(f.grande, texto, bw - 50f);
        p.cor(0.03f, 0.08f, 0.13f, 0.95f * alfa);
        p.retaCortada(bx, by, bw, bh, 10f);
        p.tri(bx + 2f, by + bh * 0.62f, bx + 2f, by + bh * 0.30f, pontaX, pontaY);
        p.cor(Cores.CIANO, alfa);
        p.bordaCortada(bx, by, bw, bh, 10f, 3f);
        p.halo(bx, by, bw, bh, Cores.CIANO, 0.5f * alfa);
        int n = Math.min(quebrado.length(), (int) (t * 48f));
        String vis = quebrado.substring(0, n);
        Texto.linhas(p, f.grande, vis, bx + 26f, by + bh / 2f + 4f, Texto.ESQ, Cores.BRANCO, alfa, 44f);
        // cursor piscando no fim do texto
        if (((int) (tempo * 2.6f)) % 2 == 0) {
            String[] ls = vis.split("\n", -1);
            String ultima = ls[ls.length - 1];
            float top = by + bh / 2f + 4f + (quebrado.split("\n").length - 1) * 22f;
            float cy = top - (ls.length - 1) * 44f;
            p.cor(Cores.CIANO, alfa);
            p.reta(bx + 26f + Texto.largura(f.grande, ultima) + 6f, cy - 15f, 18f, 5f);
        }
    }
}
