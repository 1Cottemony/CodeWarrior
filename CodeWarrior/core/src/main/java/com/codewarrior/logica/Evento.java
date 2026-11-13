package com.codewarrior.logica;

/** Algo que aconteceu na simulação. A interface lê esses eventos para fazer efeitos visuais e sons. */
public final class Evento {
    public enum Tipo {
        DASH_INICIO, DASH_FIM, MORTE_INIMIGO, GOLPE_LOOP, DANO_JOGADOR, EXPLOSAO_PAR, ONDA_INICIO, ONDA_FIM,
        CURA, CHEFE_ENTRADA, CHEFE_DANO, CHEFE_FASE, CHEFE_MORTO, TRAVA, DESTRAVA, GOSMA, DESFAZER, COPIAR,
        CLONE, CLONE_ATAQUE, PUSH, SUDO, VITORIA, DERROTA, SURGIU, COMBO_SOBE, ZONA_AVISO, ZONA_EXPLODE,
        DESALINHO, PAR_ARMADO, NEGADO
    }

    public final Tipo tipo;
    public final float x, y;
    /** Dado extra: tipo do inimigo (ordinal), número da onda, fase, dano... depende do evento. */
    public final int n;
    public final float x2, y2;

    public Evento(Tipo tipo, float x, float y, int n) {
        this(tipo, x, y, n, 0, 0);
    }

    public Evento(Tipo tipo, float x, float y, int n, float x2, float y2) {
        this.tipo = tipo;
        this.x = x;
        this.y = y;
        this.n = n;
        this.x2 = x2;
        this.y2 = y2;
    }
}
