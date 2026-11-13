package com.codewarrior.logica;

/** Um bug em campo. Campos públicos de propósito: a interface só lê. */
public final class Inimigo {
    public enum Tipo { SYNTAX, LEAK, LOOP }

    public int id;
    public Tipo tipo;
    public float x, y;
    /** Velocidade de empurrão (git push, recuo). */
    public float vx, vy;
    public float raio;
    /** > 0 enquanto está surgindo (inofensivo e intocável). */
    public float spawn;
    public float stun;
    /** Infinite Loop com o ciclo quebrado (1 acerto levado). */
    public boolean quebrado;
    public float quebradoT;
    /** Syntax: contagem para trancar tecla / duração do pouso. */
    public float tTranca, pouso;
    /** Leak: última tecla marcada com gosma. */
    public int teclaAnterior = -1;
    /** Loop: órbita. */
    public float orbCx, orbCy, orbR, orbAng, orbVel;
    public float ox, oy;
    /** No tutorial o inimigo fica parado e não machuca. */
    public boolean parado;
    public float anim, flash, semente;

    public Inimigo copiar() {
        Inimigo c = new Inimigo();
        c.id = id;
        c.tipo = tipo;
        c.x = x;
        c.y = y;
        c.vx = vx;
        c.vy = vy;
        c.raio = raio;
        c.spawn = spawn;
        c.stun = stun;
        c.quebrado = quebrado;
        c.quebradoT = quebradoT;
        c.tTranca = tTranca;
        c.pouso = pouso;
        c.teclaAnterior = teclaAnterior;
        c.orbCx = orbCx;
        c.orbCy = orbCy;
        c.orbR = orbR;
        c.orbAng = orbAng;
        c.orbVel = orbVel;
        c.ox = ox;
        c.oy = oy;
        c.parado = parado;
        c.anim = anim;
        c.flash = flash;
        c.semente = semente;
        return c;
    }
}
