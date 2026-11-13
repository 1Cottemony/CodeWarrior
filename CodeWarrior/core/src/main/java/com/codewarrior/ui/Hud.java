package com.codewarrior.ui;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Estado;
import com.codewarrior.logica.Habilidade;
import com.codewarrior.logica.Regras;
import com.codewarrior.logica.Simulacao;

/** Interface durante a partida: corações, barra da onda, combo, pontos, habilidades, chefão e terminal. */
public final class Hud {
    private String anuncio = "";
    private String subAnuncio = "";
    private float anuncioT, anuncioMax = 1f;
    private final Color anuncioCor = new Color(Cores.CIANO);
    private float comboFlash;
    private int comboAnterior;
    private float coracaoPulso;
    private int coracoesAnterior = -1;

    public void anunciar(String titulo, String sub, float dur, Color cor) {
        anuncio = titulo;
        subAnuncio = sub;
        anuncioT = dur;
        anuncioMax = dur;
        anuncioCor.set(cor);
    }

    public void atualizar(float dt) {
        anuncioT = Math.max(0f, anuncioT - dt);
        comboFlash = Math.max(0f, comboFlash - dt * 3f);
        coracaoPulso = Math.max(0f, coracaoPulso - dt * 2.5f);
    }

    public static String pontos(long v) {
        String s = Long.toString(v);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append('.');
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    public void desenhar(Pincel p, Fontes f, Simulacao sim, float t, float alfa) {
        desenhar(p, f, sim, t, alfa, false, 4);
    }

    /**
     * @param tutorial       true esconde corações, onda, pontos, combo e chefão (no tutorial não importam)
     * @param slotsVisiveis  quantas habilidades aparecem na barra de baixo (0 a 4)
     */
    public void desenhar(Pincel p, Fontes f, Simulacao sim, float t, float alfa, boolean tutorial, int slotsVisiveis) {
        if (!tutorial) desenharPartida(p, f, sim, t, alfa);
        desenharHabilidades(p, f, sim, t, alfa, slotsVisiveis);
        desenharAnuncio(p, f, alfa);
    }

    private void desenharPartida(Pincel p, Fontes f, Simulacao sim, float t, float alfa) {
        Estado e = sim.e;
        // ---------- corações ----------
        if (coracoesAnterior >= 0 && e.coracoes != coracoesAnterior) coracaoPulso = 1f;
        coracoesAnterior = e.coracoes;
        for (int i = 0; i < e.coracoesMax; i++) {
            boolean cheio = i < e.coracoes;
            float pulso = cheio && i == e.coracoes - 1 ? coracaoPulso : 0f;
            Icones.coracao(p, 40f + i * 44f, 676f, 32f, cheio, pulso, alfa);
        }
        // ---------- barra da onda ----------
        float bw = 300f;
        p.cor(0.04f, 0.09f, 0.13f, 0.9f * alfa);
        p.reta(22f, 636f, bw, 12f);
        p.cor(Cores.CIANO, alfa);
        p.reta(24f, 638f, (bw - 4f) * sim.progressoOnda(), 8f);
        p.cor(Cores.CIANO, 0.9f * alfa);
        p.borda(22f, 636f, bw, 12f, 2f);
        String rotuloOnda = e.chefe != null ? "CHEFÃO" : e.onda > 0 ? "ONDA " + e.onda + "/" + Regras.ONDAS : "TREINO";
        Texto.desenhar(p, f.pequena, rotuloOnda, 24f, 618f, Texto.ESQ, Cores.BRANCO, 0.9f * alfa);
        Texto.desenhar(p, f.pequena, "PONTOS " + pontos(e.pontos), 24f, 594f, Texto.ESQ, Cores.AMARELO, alfa);

        // ---------- combo ----------
        if (e.cadeia >= 2) {
            if (e.cadeia != comboAnterior) comboFlash = 1f;
            comboAnterior = e.cadeia;
            Color c = Cores.mistura(Cores.AMARELO, Cores.BRANCO, comboFlash * 0.7f, new Color());
            Texto.neon(p, f.grande, "COMBO ×" + e.cadeia, 1250f, 676f, Texto.DIR, c, alfa);
            Texto.desenhar(p, f.pequena, "PONTOS ×" + sim.multiplicador(), 1250f, 640f, Texto.DIR, Cores.LIMA, alfa);
            float k = Math.max(0f, 1f - e.tCadeia / Regras.COMBO_JANELA);
            p.cor(Cores.AMARELO, 0.9f * alfa);
            p.reta(1250f - 180f * k, 622f, 180f * k, 4f);
        } else {
            comboAnterior = 0;
        }

        // ---------- chefão ----------
        if (e.chefe != null && e.fase != Estado.Fase.FIM) {
            float w = 620f, x = 640f - w / 2f;
            float hp = e.chefe.hp / (float) Regras.CHEFE_PV;
            Texto.neon(p, f.media, "DIV DESALINHADA", 640f, 700f, Texto.CENTRO, Cores.VERMELHO, alfa);
            p.cor(0.08f, 0.02f, 0.05f, 0.9f * alfa);
            p.reta(x, 672f, w, 16f);
            p.cor(Cores.mistura(Cores.VERMELHO, Cores.AMARELO, 1f - hp, new Color()), alfa);
            p.reta(x + 2f, 674f, (w - 4f) * hp, 12f);
            p.cor(Cores.VERMELHO, alfa);
            p.borda(x, 672f, w, 16f, 2.5f);
            // marcas das fases (20 e 10 PV)
            p.cor(Cores.BRANCO, 0.8f * alfa);
            p.reta(x + w * (20f / 30f) - 1f, 670f, 2f, 20f);
            p.reta(x + w * (10f / 30f) - 1f, 670f, 2f, 20f);
            Texto.desenhar(p, f.pequena, "FASE " + e.chefe.fase, x + w + 12f, 680f, Texto.ESQ, Cores.CORAL, alfa);
        }

    }

    private void desenharHabilidades(Pincel p, Fontes f, Simulacao sim, float t, float alfa, int slotsVisiveis) {
        // ---------- habilidades ----------
        Habilidade[] hs = Habilidade.values();
        for (int i = 0; i < hs.length && i < slotsVisiveis; i++) {
            float x = 640f + (i - 1.5f) * 118f;
            boolean armado = hs[i] == Habilidade.COPIAR_COLAR && sim.copiaAtiva();
            Icones.slot(p, f, hs[i], x, 74f, 64f, sim.recargaRestante(hs[i]), sim.recargaTotal(hs[i]), armado, t, alfa);
        }
        if (sim.copiaAtiva()) {
            Texto.neon(p, f.pequena, "Ctrl+V em " + String.format("%.1f", sim.copiaRestante()) + " s", 640f, 128f, Texto.CENTRO, Cores.LIMA, alfa);
        }

    }

    private void desenharAnuncio(Pincel p, Fontes f, float alfa) {
        // ---------- anúncio central ----------
        if (anuncioT > 0f) {
            float k = anuncioT / anuncioMax;
            float entra = Math.min(1f, (1f - k) * 6f);
            float sai = Math.min(1f, k * 4f);
            float a = Math.min(entra, sai) * alfa;
            float dx = (1f - entra) * -120f;
            Texto.neon(p, f.enorme, anuncio, 640f + dx, 598f, Texto.CENTRO, anuncioCor, a);
            if (!subAnuncio.isEmpty()) Texto.desenhar(p, f.media, subAnuncio, 640f + dx, 552f, Texto.CENTRO, Cores.BRANCO, a);
        }
    }

    /** Janela do terminal (modo de digitar comandos). tempoReal/duracao = barra do tempo restante. */
    public static void terminal(Pincel p, Fontes f, String digitado, float tempoReal, float duracao, String aviso, float t, float anim) {
        float a = Math.min(1f, anim);
        // escurece o jogo
        p.cor(0f, 0.02f, 0.05f, 0.45f * a);
        p.reta(0f, 0f, 1280f, 720f);
        Texto.neon(p, f.pequena, "CONSOLE ATIVO — CÂMERA LENTA", 640f, 188f - (1f - a) * 40f, Texto.CENTRO, Cores.CIANO, a);
        float w = 820f, h = 130f, x = 640f - w / 2f, y = 30f - (1f - a) * 160f;
        p.cor(0.02f, 0.06f, 0.10f, 0.97f);
        p.retaCortada(x, y, w, h, 8f);
        p.cor(Cores.CIANO, 1f);
        p.bordaCortada(x, y, w, h, 8f, 3f);
        p.cor(Cores.CIANO, 0.22f);
        p.reta(x + 3f, y + h - 30f, w - 6f, 27f);
        Texto.desenhar(p, f.pequena, "TERMINAL", x + 16f, y + h - 16f, Texto.ESQ, Cores.CIANO, 1f);
        float frac = Math.max(0f, Math.min(1f, tempoReal / duracao));
        p.cor(frac > 0.3f ? Cores.CIANO : Cores.VERMELHO, 1f);
        p.reta(x + w - 190f, y + h - 21f, 170f * frac, 8f);
        p.borda(x + w - 190f, y + h - 21f, 170f, 8f, 1.5f);
        String prompt = "user@codewarrior:~$ ";
        com.badlogic.gdx.graphics.g2d.BitmapFont fo = Texto.largura(f.grande, prompt + digitado + "_") > w - 60f ? f.media : f.grande;
        Texto.desenhar(p, fo, prompt, x + 22f, y + 52f, Texto.ESQ, Cores.VERDE, 1f);
        float px = x + 22f + Texto.largura(fo, prompt);
        Texto.desenhar(p, fo, digitado, px, y + 52f, Texto.ESQ, Cores.BRANCO, 1f);
        if (((int) (t * 2.6f)) % 2 == 0) {
            p.cor(Cores.VERDE, 1f);
            p.reta(px + Texto.largura(fo, digitado) + 4f, y + 38f, 16f, 28f);
        }
        if (aviso != null && !aviso.isEmpty()) {
            Texto.desenhar(p, f.pequena, aviso, x + 22f, y + 14f, Texto.ESQ, Cores.VERMELHO, 1f);
        } else {
            Texto.desenhar(p, f.pequena, "git push    sudo rm -rf        Enter confirma · Esc cancela", x + 22f, y + 14f, Texto.ESQ, Cores.CINZA, 0.9f);
        }
    }
}
