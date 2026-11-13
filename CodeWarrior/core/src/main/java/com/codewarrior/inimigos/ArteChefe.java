package com.codewarrior.inimigos;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Chefe;
import com.codewarrior.ui.Fontes;
import com.codewarrior.ui.Texto;

/** A Div Desalinhada: uma caixa HTML quebrada, com fatias deslocadas, cara brava e tags voando em volta. */
public final class ArteChefe {
    private static final Color tmp = new Color();
    private static final String[] TAGS = {"<div>", "</div>", "<div id=\"err\">", "margin:auto?", "float:left;", "<br/>"};

    private ArteChefe() {
    }

    public static void desenhar(Pincel p, Fontes f, Chefe c, float t, boolean morrendo) {
        float w = Chefe.LARGURA, h = Chefe.ALTURA;
        float x = c.x - w / 2f, y = c.y - h / 2f;
        float intenso = c.fase == 1 ? 0.6f : c.fase == 2 ? 1f : 1.5f;
        boolean piscar = c.invul > 0f && c.invul < 900f && ((int) (t * 14f)) % 2 == 0;
        float a = piscar ? 0.45f : 1f;
        float flash = c.flash;

        // aura
        p.brilho(c.x, c.y, 260f);
        p.aditivo(true);
        p.cor(Cores.VERMELHO, 0.10f * intenso);
        p.disco(c.x, c.y, 190f);
        p.aditivo(false);

        // sombra projetada nas teclas
        p.cor(0f, 0f, 0f, 0.28f);
        p.retaCortada(x + 10f, y - 14f, w, h, 10f);

        // fatias desalinhadas (atrás)
        float passo = (float) Math.floor(t * 9f);
        for (int i = 0; i < 6; i++) {
            float seed = i * 12.9898f + passo * 0.37f;
            float jx = (float) Math.sin(seed * 78.233f) * 14f * intenso;
            float jy = (float) Math.sin(seed * 39.425f) * 8f * intenso;
            float sw = w * (0.30f + 0.12f * (i % 3));
            float sh = 20f + (i % 3) * 10f;
            float sx = x + (i * 53f) % (w - sw) + jx;
            float sy = y + (i * 37f) % (h - sh) + jy;
            tmp.set(i % 3 == 0 ? Cores.MAGENTA : i % 3 == 1 ? Cores.CORAL : Cores.LARANJA);
            p.cor(tmp, 0.55f * a);
            p.reta(sx, sy, sw, sh);
        }

        // caixa principal
        p.cor(Cores.mistura(Color.valueOf("6a1230"), Cores.BRANCO, flash * 0.8f, tmp), a);
        p.retaCortada(x, y, w, h, 12f);
        p.cor(Cores.mistura(Cores.VERMELHO, Cores.BRANCO, flash, tmp), a);
        p.bordaCortada(x, y, w, h, 12f, 5f);
        p.cor(Cores.CORAL, 0.8f * a);
        p.bordaCortada(x + 12f, y + 12f, w - 24f, h - 24f, 8f, 2f);

        // painel do rosto
        p.cor(Color.valueOf("2a0614"), a);
        p.retaCortada(x + 30f, y + 26f, w - 60f, h - 52f, 8f);

        // olhos bravos (inclinados)
        float ox = c.x, oy = c.y + 16f;
        float pisca = ((int) (t * 1.7f)) % 9 == 0 ? 0.3f : 1f;
        p.cor(c.fase == 3 ? Cores.BRANCO : Cores.AMARELO, a);
        p.tri(ox - 88f, oy + 6f * pisca, ox - 30f, oy - 8f * pisca, ox - 30f, oy + 24f * pisca);
        p.tri(ox + 88f, oy + 6f * pisca, ox + 30f, oy - 8f * pisca, ox + 30f, oy + 24f * pisca);
        p.cor(Cores.VERMELHO, a);
        p.disco(ox - 44f, oy + 8f, 7f);
        p.disco(ox + 44f, oy + 8f, 7f);
        // sobrancelhas
        p.cor(Cores.VERMELHO, a);
        p.linha(ox - 96f, oy + 34f, ox - 24f, oy + 20f, 7f);
        p.linha(ox + 96f, oy + 34f, ox + 24f, oy + 20f, 7f);

        // boca em zigue-zague
        p.cor(Cores.BRANCO, a);
        float by = c.y - 38f;
        for (int i = 0; i < 9; i++) {
            float x0 = ox - 72f + i * 18f;
            p.linha(x0, by + (i % 2 == 0 ? 10f : -10f), x0 + 18f, by + (i % 2 == 0 ? -10f : 10f), 4f);
        }

        // tags voando
        for (int i = 0; i < 5; i++) {
            float ang = t * (0.5f + i * 0.07f) + i * 1.26f;
            float px = c.x + (float) Math.cos(ang) * (w * 0.62f + i * 8f);
            float py = c.y + (float) Math.sin(ang * 1.3f) * (h * 0.7f) + 30f;
            tmp.set(i % 2 == 0 ? Cores.LARANJA : Cores.CIANO);
            Texto.desenhar(p, f.pequena, TAGS[i % TAGS.length], px, py, Texto.CENTRO, tmp, 0.75f * a);
        }

        if (morrendo) {
            p.aditivo(true);
            p.cor(Cores.BRANCO, 0.25f + 0.25f * (float) Math.sin(t * 40f));
            p.retaCortada(x, y, w, h, 12f);
            p.aditivo(false);
        }
        if (flash > 0f) {
            p.aditivo(true);
            p.cor(Cores.BRANCO, flash * 0.5f);
            p.retaCortada(x, y, w, h, 12f);
            p.aditivo(false);
        }
    }
}
