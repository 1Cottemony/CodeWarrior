package com.codewarrior.logica;

/** As 4 habilidades ("scripts"). base = recarga em segundos antes dos fatores de dificuldade. */
public enum Habilidade {
    DESFAZER("Ctrl+Z", 20f),
    COPIAR_COLAR("Ctrl+C/V", 25f),
    GIT_PUSH("git push", 30f),
    SUDO("sudo rm -rf", 90f);

    public final String nome;
    public final float base;

    Habilidade(String nome, float base) {
        this.nome = nome;
        this.base = base;
    }
}
