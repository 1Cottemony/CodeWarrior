package com.codewarrior.logica;

/** A Div Desalinhada: 30 PV, 3 fases. (x, y) é o centro da caixa de colisão. */
public final class Chefe {
    public static final float LARGURA = 340f;
    public static final float ALTURA = 180f;

    public float x = 640f, y = 800f;
    public int hp = Regras.CHEFE_PV;
    public int fase = 1;
    /** Entrada 0..1 (descendo para a arena). */
    public float entrada;
    /** > 0: não leva dano (entrada ou troca de fase). */
    public float invul = 999f;
    public float hitCd;
    /** Recarga do dano de explosão de par / clone no chefão (impede ficar repetindo). */
    public float parCd, cloneCd;
    public float anim, flash;
    public float tDesalinho = 1.5f, tInvoca = 2f, tZona = 2f;

    public Chefe copiar() {
        Chefe c = new Chefe();
        c.x = x;
        c.y = y;
        c.hp = hp;
        c.fase = fase;
        c.entrada = entrada;
        c.invul = invul;
        c.hitCd = hitCd;
        c.parCd = parCd;
        c.cloneCd = cloneCd;
        c.anim = anim;
        c.flash = flash;
        c.tDesalinho = tDesalinho;
        c.tInvoca = tInvoca;
        c.tZona = tZona;
        return c;
    }
}
