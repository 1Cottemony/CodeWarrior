package com.codewarrior.telas;

import com.codewarrior.CodeWarriorGame;
import com.codewarrior.efeitos.Pincel;

/** Base de todas as telas. Coordenadas sempre na tela virtual 1280x720 (y para cima). */
public abstract class Tela {
    protected final CodeWarriorGame jogo;
    protected float tempo;
    /** Segundos desde que a tela abriu (para animações de entrada). */
    protected float entrada;

    protected Tela(CodeWarriorGame jogo) {
        this.jogo = jogo;
    }

    public void mostrar() {
    }

    public void ocultar() {
    }

    public void atualizar(float dt) {
        tempo += dt;
        entrada += dt;
        jogo.particulas.atualizar(dt);
    }

    public abstract void desenhar(Pincel p);

    public boolean teclaDown(int k) {
        return false;
    }

    public boolean teclaUp(int k) {
        return false;
    }

    public void caractere(char c) {
    }

    public void mouseMover(float x, float y) {
    }

    public boolean mouseDown(float x, float y, int botao) {
        return false;
    }

    public void mouseUp(float x, float y, int botao) {
    }

    /** Suavização 0..1 para animações de entrada de painéis. */
    protected static float suave(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return t * t * (3f - 2f * t);
    }
}
