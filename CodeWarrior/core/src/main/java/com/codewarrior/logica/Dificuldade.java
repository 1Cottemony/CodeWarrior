package com.codewarrior.logica;

/** Os 3 níveis de dificuldade. Números definidos no resumo de regras aprovado. */
public enum Dificuldade {
    FACIL("FÁCIL", 7, 0.8f, 0.8f),
    NORMAL("NORMAL", 5, 1f, 1f),
    DIFICIL("DIFÍCIL", 3, 1.2f, 1.15f);

    public final String nome;
    public final int coracoes;
    public final float velInimigos;
    public final float fatorRecarga;

    Dificuldade(String nome, int coracoes, float velInimigos, float fatorRecarga) {
        this.nome = nome;
        this.coracoes = coracoes;
        this.velInimigos = velInimigos;
        this.fatorRecarga = fatorRecarga;
    }

    public static Dificuldade deIndice(int i) {
        Dificuldade[] v = values();
        return v[Math.max(0, Math.min(v.length - 1, i))];
    }
}
