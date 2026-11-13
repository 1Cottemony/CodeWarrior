package com.codewarrior.logica;

import java.util.ArrayList;

/**
 * Tudo que o Ctrl+Z consegue rebobinar. As recargas, a janela de cópia e a fila de eventos NÃO ficam aqui
 * (ficam na Simulacao), por isso não voltam no tempo.
 */
public final class Estado {
    public enum Fase { INTRO_ONDA, ONDA, INTERVALO, CHEFE_INTRO, CHEFE, CHEFE_MORRENDO, FIM }

    public static final class Spawn {
        public Inimigo.Tipo tipo;
        public float t;

        Spawn(Inimigo.Tipo tipo, float t) {
            this.tipo = tipo;
            this.t = t;
        }
    }

    public static final class Zona {
        public float x, y, r, t, vis;
        public boolean explodiu;

        Zona copiar() {
            Zona z = new Zona();
            z.x = x;
            z.y = y;
            z.r = r;
            z.t = t;
            z.vis = vis;
            z.explodiu = explodiu;
            return z;
        }
    }

    public static final class Desalinho {
        public int fila;
        public float dx, t, dur;

        Desalinho copiar() {
            Desalinho d = new Desalinho();
            d.fila = fila;
            d.dx = dx;
            d.t = t;
            d.dur = dur;
            return d;
        }
    }

    public float t;
    // jogador
    public float jx, jy;
    public int jTecla;
    public boolean dashando;
    public final float[] cx = new float[3], cy = new float[3];
    public int nPontos;
    public float dashPos, dashTotal, dashVel;
    public int dashDestino;
    public int dashKills;
    public int[] dashAcertados = new int[96];
    public int nAcertados;
    public float dashCd;
    public int coracoes, coracoesMax;
    public float invul;
    // pontuação
    public long pontos;
    public int cadeia;
    public float tCadeia;
    // pares
    public int parTecla = -1;
    public float parT;
    // mundo
    public ArrayList<Inimigo> inimigos = new ArrayList<>();
    public int proxId = 1;
    public final float[] gosma, trava, offX, offY;
    public final int[] travaDono;
    public Chefe chefe;
    public Fase fase = Fase.INTRO_ONDA;
    public int onda;
    public float tFase, tOnda;
    public ArrayList<Spawn> fila = new ArrayList<>();
    public int totalOnda;
    // clone
    public boolean cloneAtivo;
    public float cloneX, cloneY, cloneT, cloneAtk;
    // chefão
    public ArrayList<Zona> zonas = new ArrayList<>();
    public ArrayList<Desalinho> desalinhos = new ArrayList<>();

    public Estado(int nTeclas) {
        gosma = new float[nTeclas];
        trava = new float[nTeclas];
        offX = new float[nTeclas];
        offY = new float[nTeclas];
        travaDono = new int[nTeclas];
    }

    void adicionarSpawn(Inimigo.Tipo tipo, float t) {
        fila.add(new Spawn(tipo, t));
    }

    public Estado copiar() {
        Estado c = new Estado(gosma.length);
        c.t = t;
        c.jx = jx;
        c.jy = jy;
        c.jTecla = jTecla;
        c.dashando = dashando;
        System.arraycopy(cx, 0, c.cx, 0, 3);
        System.arraycopy(cy, 0, c.cy, 0, 3);
        c.nPontos = nPontos;
        c.dashPos = dashPos;
        c.dashTotal = dashTotal;
        c.dashVel = dashVel;
        c.dashDestino = dashDestino;
        c.dashKills = dashKills;
        c.dashAcertados = dashAcertados.clone();
        c.nAcertados = nAcertados;
        c.dashCd = dashCd;
        c.coracoes = coracoes;
        c.coracoesMax = coracoesMax;
        c.invul = invul;
        c.pontos = pontos;
        c.cadeia = cadeia;
        c.tCadeia = tCadeia;
        c.parTecla = parTecla;
        c.parT = parT;
        for (Inimigo i : inimigos) c.inimigos.add(i.copiar());
        c.proxId = proxId;
        System.arraycopy(gosma, 0, c.gosma, 0, gosma.length);
        System.arraycopy(trava, 0, c.trava, 0, trava.length);
        System.arraycopy(offX, 0, c.offX, 0, offX.length);
        System.arraycopy(offY, 0, c.offY, 0, offY.length);
        System.arraycopy(travaDono, 0, c.travaDono, 0, travaDono.length);
        c.chefe = chefe == null ? null : chefe.copiar();
        c.fase = fase;
        c.onda = onda;
        c.tFase = tFase;
        c.tOnda = tOnda;
        for (Spawn s : fila) c.fila.add(new Spawn(s.tipo, s.t));
        c.totalOnda = totalOnda;
        c.cloneAtivo = cloneAtivo;
        c.cloneX = cloneX;
        c.cloneY = cloneY;
        c.cloneT = cloneT;
        c.cloneAtk = cloneAtk;
        for (Zona z : zonas) c.zonas.add(z.copiar());
        for (Desalinho d : desalinhos) c.desalinhos.add(d.copiar());
        return c;
    }
}
