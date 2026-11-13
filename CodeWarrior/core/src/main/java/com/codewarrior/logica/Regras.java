package com.codewarrior.logica;

/** Opções de uma partida e constantes de balanceamento (todas em um só lugar). */
public final class Regras {
    // ----- constantes -----
    public static final float DASH_RECARGA = 0.4f;
    public static final float DASH_VEL = 2600f;
    public static final float DASH_RAIO = 30f;
    public static final float JOGADOR_RAIO = 16f;
    public static final float INVUL_DANO = 1.5f;
    public static final float DESFAZER_SEG = 2f;
    public static final float PAR_JANELA = 3f;
    public static final float PAR_RAIO = 200f;
    public static final float COPIA_JANELA = 5f;
    public static final float CLONE_DURACAO = 8f;
    public static final float CLONE_INTERVALO = 0.6f;
    public static final float CLONE_ALCANCE = 132f;
    public static final float COMBO_JANELA = 2f;
    public static final int MULT_MAX = 5;
    public static final int CHEFE_PV = 30;
    public static final long PONTOS_SYNTAX = 100, PONTOS_LEAK = 150, PONTOS_LOOP = 300, PONTOS_CHEFE = 5000;
    public static final int ONDAS = 5;

    // ----- opções da partida -----
    public Dificuldade dificuldade = Dificuldade.NORMAL;
    public boolean facilitador = false;
    public boolean miraAssistida = true;
    /** No tutorial não há ondas, e o jogador não leva dano. */
    public boolean tutorial = false;

    public int coracoesMax() {
        return dificuldade.coracoes + (facilitador ? 2 : 0);
    }

    public float velInimigos() {
        return dificuldade.velInimigos * (facilitador ? 0.75f : 1f);
    }

    public float fatorRecarga() {
        return dificuldade.fatorRecarga * (facilitador ? 0.7f : 1f);
    }

    public Regras copia() {
        Regras r = new Regras();
        r.dificuldade = dificuldade;
        r.facilitador = facilitador;
        r.miraAssistida = miraAssistida;
        r.tutorial = tutorial;
        return r;
    }
}
