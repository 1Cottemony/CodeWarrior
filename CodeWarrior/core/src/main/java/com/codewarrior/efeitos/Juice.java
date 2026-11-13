package com.codewarrior.efeitos;

import com.badlogic.gdx.math.MathUtils;

/** "Game feel": tremor de tela, hit-stop, câmera lenta, clarões e glitch. Tudo decai sozinho. */
public final class Juice {
    /** 0.5 baixa, 1 média, 1.4 alta (vem das configurações). */
    public float intensidade = 1f;
    public boolean glitchLigado = true;

    public float tremor;
    public float flash;
    public float flashR = 1f, flashG = 1f, flashB = 1f;
    public float glitch;
    private float hitStop;
    private float lentoT, lentoDur = 1f, lentoFator = 1f;

    public void tremer(float px) {
        tremor = Math.max(tremor, px * intensidade);
    }

    public void parar(float seg) {
        hitStop = Math.max(hitStop, seg * Math.min(1f, intensidade));
    }

    public void lento(float seg, float fator) {
        if (seg >= lentoT) {
            lentoT = seg;
            lentoDur = seg;
            lentoFator = fator;
        }
    }

    public void clarao(float a, float r, float g, float b) {
        if (a * intensidade >= flash) {
            flash = a * intensidade;
            flashR = r;
            flashG = g;
            flashB = b;
        }
    }

    public void glitchar(float a) {
        if (glitchLigado) glitch = Math.max(glitch, a);
    }

    /** Fator de tempo do jogo neste quadro (0 durante hit-stop, <1 em câmera lenta). */
    public float escalaTempo(float dtReal) {
        if (hitStop > 0f) {
            hitStop -= dtReal;
            return 0f;
        }
        if (lentoT > 0f) {
            lentoT -= dtReal;
            float k = Math.max(0f, lentoT) / lentoDur;
            return k > 0.3f ? lentoFator : MathUtils.lerp(1f, lentoFator, k / 0.3f);
        }
        return 1f;
    }

    public void atualizar(float dtReal) {
        tremor *= (float) Math.exp(-10f * dtReal);
        if (tremor < 0.05f) tremor = 0f;
        flash = Math.max(0f, flash - dtReal * 3.2f);
        glitch = Math.max(0f, glitch - dtReal * 2.2f);
    }

    public float tremorX() {
        return tremor > 0f ? MathUtils.random(-tremor, tremor) : 0f;
    }

    public float tremorY() {
        return tremor > 0f ? MathUtils.random(-tremor, tremor) : 0f;
    }
}
