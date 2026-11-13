package com.codewarrior.dados;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.codewarrior.logica.Dificuldade;

/** Configurações e recordes, salvos no PC do jogador (pasta de preferências do libGDX). */
public final class Configuracoes {
    public float volSom = 0.8f;
    public float volMusica = 0.6f;
    /** 0 baixa, 1 média, 2 alta. */
    public int intensidade = 1;
    public boolean telaCheia = false;
    public boolean glitch = true;
    public boolean varredura = false;
    public boolean miraAssistida = true;
    public boolean facilitador = false;
    public int dificuldade = 1;
    private final long[] recordes = new long[3];
    private Preferences prefs;

    public static Configuracoes carregar() {
        Configuracoes c = new Configuracoes();
        try {
            c.prefs = Gdx.app.getPreferences("codewarrior-mestre-do-debug");
            Preferences p = c.prefs;
            c.volSom = clamp(p.getFloat("volSom", 0.8f));
            c.volMusica = clamp(p.getFloat("volMusica", 0.6f));
            c.intensidade = Math.max(0, Math.min(2, p.getInteger("intensidade", 1)));
            c.telaCheia = false; // sempre abre em janela (o valor salvo antes podia prender o jogo em tela cheia)
            c.glitch = p.getBoolean("glitch", true);
            c.varredura = p.getBoolean("varredura", false);
            c.miraAssistida = p.getBoolean("mira", true);
            c.facilitador = p.getBoolean("facilitador", false);
            c.dificuldade = Math.max(0, Math.min(2, p.getInteger("dificuldade", 1)));
            for (int i = 0; i < 3; i++) c.recordes[i] = p.getLong("recorde" + i, 0L);
        } catch (Throwable t) {
            Gdx.app.error("Configuracoes", "Não foi possível ler as preferências: " + t);
        }
        return c;
    }

    public void salvar() {
        if (prefs == null) return;
        try {
            prefs.putFloat("volSom", volSom);
            prefs.putFloat("volMusica", volMusica);
            prefs.putInteger("intensidade", intensidade);
            prefs.putBoolean("telaCheia", false);
            prefs.putBoolean("glitch", glitch);
            prefs.putBoolean("varredura", varredura);
            prefs.putBoolean("mira", miraAssistida);
            prefs.putBoolean("facilitador", facilitador);
            prefs.putInteger("dificuldade", dificuldade);
            for (int i = 0; i < 3; i++) prefs.putLong("recorde" + i, recordes[i]);
            prefs.flush();
        } catch (Throwable t) {
            Gdx.app.error("Configuracoes", "Não foi possível salvar: " + t);
        }
    }

    public void copiarDe(Configuracoes o) {
        volSom = o.volSom;
        volMusica = o.volMusica;
        intensidade = o.intensidade;
        telaCheia = o.telaCheia;
        glitch = o.glitch;
        varredura = o.varredura;
        miraAssistida = o.miraAssistida;
        facilitador = o.facilitador;
        dificuldade = o.dificuldade;
    }

    public long recorde(Dificuldade d) {
        return recordes[d.ordinal()];
    }

    /** Devolve true se bateu o recorde (e já salva). */
    public boolean registrarPontos(Dificuldade d, long pontos) {
        if (pontos > recordes[d.ordinal()]) {
            recordes[d.ordinal()] = pontos;
            salvar();
            return true;
        }
        return false;
    }

    public float fatorEfeitos() {
        return intensidade == 0 ? 0.5f : intensidade == 1 ? 1f : 1.4f;
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
