package com.codewarrior.telas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.codewarrior.CodeWarriorGame;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.jogador.ArteJogador;
import com.codewarrior.logica.Dificuldade;
import com.codewarrior.logica.Regras;
import com.codewarrior.logica.Simulacao;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Hud;
import com.codewarrior.ui.Texto;

/** Fim da partida: "game over" (sistema corrompido) ou vitória (sistema restaurado). */
public final class TelaFim extends Tela {
    private static final String[] ERROS = {
            "$ ./vida.exe",
            "ERRO 0x00F4: corações não encontrados",
            "NullPointerException em Jogador.java:5",
            "Segmentation fault (core dumped)",
            "StackOverflow: bugs demais na fila",
            "$ _",
    };
    private static final String[] CONQUISTAS = {
            "Div Desalinhada derrotada",
            "5 ondas de bugs limpas",
            "Layout realinhado com sucesso",
    };

    private final boolean vitoria;
    private final long pontos;
    private final int onda;
    private final Dificuldade dificuldade;
    private final boolean facilitador;
    private final boolean novoRecorde;
    private final long recorde;
    private final Botao[] botoes = new Botao[2];
    private int foco = 0;
    private float confete;

    public TelaFim(CodeWarriorGame jogo, Simulacao sim) {
        super(jogo);
        this.vitoria = sim.resultado == Simulacao.Resultado.VITORIA;
        this.pontos = sim.e.pontos;
        this.onda = sim.e.onda;
        this.dificuldade = sim.regras.dificuldade;
        this.facilitador = sim.regras.facilitador;
        this.novoRecorde = jogo.config.registrarPontos(dificuldade, pontos);
        this.recorde = jogo.config.recorde(dificuldade);
        String t1 = vitoria ? "JOGAR DE NOVO" : "TENTAR DE NOVO";
        botoes[0] = new Botao(t1, 300f, 60f, 330f, 76f, vitoria ? Cores.LIMA : Cores.CIANO, true);
        botoes[1] = new Botao("MENU PRINCIPAL", 660f, 60f, 330f, 76f, Cores.MAGENTA, false);
    }

    @Override
    public void mostrar() {
        jogo.particulas.limpar();
        jogo.audio.musicaLigada = true;
        jogo.audio.andamento = vitoria ? 1f : 0.6f;
        jogo.audio.abafar = vitoria ? 0f : 0.5f;
        jogo.audio.camadas = vitoria ? 2 : 0;
        entrada = 0f;
        if (vitoria) {
            for (int i = 0; i < 40; i++) soltarConfete(MathUtils.random(40f, 1240f), MathUtils.random(500f, 740f));
        }
    }

    private void soltarConfete(float x, float y) {
        Color[] cs = {Cores.CIANO, Cores.MAGENTA, Cores.AMARELO, Cores.LIMA, Cores.CORAL};
        jogo.particulas.confeteGlifo(x, y, cs[MathUtils.random(cs.length - 1)]);
    }

    private void ativar(int i) {
        jogo.audio.uiOk();
        botoes[i].clique = 1f;
        if (i == 0) {
            Regras r = jogo.novasRegras();
            jogo.irPara(new TelaJogo(jogo, r, false));
        } else {
            jogo.irPara(new TelaMenu(jogo));
        }
    }

    @Override
    public boolean teclaDown(int k) {
        if (k == Input.Keys.LEFT || k == Input.Keys.A || k == Input.Keys.UP || k == Input.Keys.W) {
            foco = 0;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.RIGHT || k == Input.Keys.D || k == Input.Keys.DOWN || k == Input.Keys.S || k == Input.Keys.TAB) {
            foco = (k == Input.Keys.TAB) ? 1 - foco : 1;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) {
            ativar(foco);
        } else if (k == Input.Keys.ESCAPE) {
            ativar(1);
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void mouseMover(float x, float y) {
        for (int i = 0; i < 2; i++) {
            if (botoes[i].contem(x, y) && foco != i) {
                foco = i;
                jogo.audio.uiMover();
            }
        }
    }

    @Override
    public boolean mouseDown(float x, float y, int botao) {
        for (int i = 0; i < 2; i++) {
            if (botoes[i].contem(x, y)) {
                foco = i;
                ativar(i);
                return true;
            }
        }
        return false;
    }

    @Override
    public void atualizar(float dt) {
        super.atualizar(dt);
        if (vitoria) {
            confete -= dt;
            if (confete <= 0f) {
                confete = 0.09f;
                soltarConfete(MathUtils.random(40f, 1240f), 740f);
            }
        }
        for (int i = 0; i < 2; i++) {
            botoes[i].foco = foco == i;
            botoes[i].atualizar(dt, false);
        }
    }

    @Override
    public void desenhar(Pincel p) {
        jogo.fundo.desenhar(p, jogo.fontes, tempo, vitoria ? 1f : 0.55f);
        if (vitoria) desenharVitoria(p);
        else desenharDerrota(p);
        float eb = suave((entrada - 1.4f) / 0.4f);
        for (int i = 0; i < 2; i++) botoes[i].desenhar(p, jogo.fontes.grande, tempo, eb, (1f - eb) * (i == 0 ? -300f : 300f));
        jogo.particulas.desenhar(p, jogo.fontes);
    }

    private long pontosAnimados() {
        float k = suave((entrada - 0.7f) / 1.4f);
        return Math.round(pontos * (double) k);
    }

    private void desenharPainelPontos(Pincel p, float x, float y, float w, float h, Color cor, float a) {
        p.cor(0.03f, 0.07f, 0.11f, 0.92f * a);
        p.retaCortada(x, y, w, h, 12f);
        p.cor(cor, a);
        p.bordaCortada(x, y, w, h, 12f, 3f);
        p.halo(x, y, w, h, cor, 0.5f * a);
        float cx = x + w / 2f;
        Texto.desenhar(p, jogo.fontes.pequena, "PONTUAÇÃO", cx, y + h - 34f, Texto.CENTRO, Cores.CINZA, a);
        Texto.neon(p, jogo.fontes.placar, Hud.pontos(pontosAnimados()), cx, y + h - 92f, Texto.CENTRO, Cores.AMARELO, a);
        Texto.desenhar(p, jogo.fontes.pequena, "RECORDE (" + dificuldade.nome + "): " + Hud.pontos(recorde), cx, y + h - 148f, Texto.CENTRO, Cores.LIMA, a);
        if (novoRecorde && entrada > 2.0f) {
            float pulso = 0.75f + 0.25f * (float) Math.sin(tempo * 8f);
            Texto.neon(p, jogo.fontes.media, "NOVO RECORDE!", cx, y + h - 190f, Texto.CENTRO, Cores.AMARELO, pulso * a);
        } else if (facilitador) {
            Texto.desenhar(p, jogo.fontes.pequena, "modo facilitador ligado", cx, y + h - 190f, Texto.CENTRO, Cores.CINZA, a);
        }
    }

    // ------------------------------------------------------------------ derrota
    private void desenharDerrota(Pincel p) {
        float k = suave(entrada / 0.6f);
        float glitch = 0.5f + 0.5f * (float) Math.sin(tempo * 3f);
        Texto.neon(p, jogo.fontes.tituloM, "SISTEMA CORROMPIDO", 640f, 650f + (1f - k) * 100f, Texto.CENTRO, Cores.VERMELHO, k);
        Texto.glitch(p, jogo.fontes.titulo, "GAME OVER", 640f, 560f + (1f - k) * 100f, Cores.BRANCO, 0.5f + glitch * 0.5f, tempo);

        // terminal com erros falsos
        float ex = suave((entrada - 0.2f) / 0.6f);
        float x = 70f + (1f - ex) * -600f, y = 190f, w = 560f, h = 290f;
        p.cor(0.02f, 0.04f, 0.07f, 0.95f * ex);
        p.retaCortada(x, y, w, h, 10f);
        p.cor(Cores.VERMELHO, ex);
        p.bordaCortada(x, y, w, h, 10f, 3f);
        p.cor(Cores.VERMELHO, 0.22f * ex);
        p.reta(x + 3f, y + h - 30f, w - 6f, 27f);
        Texto.desenhar(p, jogo.fontes.pequena, "TERMINAL — ERRO FATAL", x + 16f, y + h - 16f, Texto.ESQ, Cores.VERMELHO, ex);
        for (int i = 0; i < ERROS.length; i++) {
            float aparece = suave((entrada - 0.6f - i * 0.25f) / 0.25f);
            if (aparece <= 0f) continue;
            Color c = i == 0 || i == ERROS.length - 1 ? Cores.VERDE : i % 2 == 1 ? Cores.CORAL : Cores.BRANCO;
            Texto.desenhar(p, jogo.fontes.codigo, ERROS[i], x + 20f, y + h - 62f - i * 34f, Texto.ESQ, c, aparece * ex);
        }
        Texto.desenhar(p, jogo.fontes.pequena, "Onda alcançada: " + Math.max(1, onda) + "/" + Regras.ONDAS, x + 20f, y + 22f, Texto.ESQ, Cores.CINZA, ex);

        float ed = suave((entrada - 0.35f) / 0.6f);
        desenharPainelPontos(p, 700f + (1f - ed) * 600f, 190f, 500f, 290f, Cores.MAGENTA, ed);
    }

    // ------------------------------------------------------------------ vitória
    private void desenharVitoria(Pincel p) {
        float k = suave(entrada / 0.6f);
        Texto.neon(p, jogo.fontes.tituloM, "SISTEMA RESTAURADO", 640f, 650f + (1f - k) * 100f, Texto.CENTRO, Cores.LIMA, k);
        p.aditivo(true);
        Texto.gradiente(p, jogo.fontes.titulo, "VITÓRIA!", 640f, 560f + (1f - k) * 100f, 0.25f, 0.62f + (float) Math.sin(tempo) * 0.04f, k);
        p.aditivo(false);

        float ex = suave((entrada - 0.2f) / 0.6f);
        float x = 70f + (1f - ex) * -600f, y = 190f, w = 560f, h = 290f;
        p.cor(0.02f, 0.06f, 0.06f, 0.95f * ex);
        p.retaCortada(x, y, w, h, 10f);
        p.cor(Cores.LIMA, ex);
        p.bordaCortada(x, y, w, h, 10f, 3f);
        p.halo(x, y, w, h, Cores.LIMA, 0.4f * ex);
        Texto.neon(p, jogo.fontes.media, "RELATÓRIO DE DEBUG", x + w / 2f, y + h - 34f, Texto.CENTRO, Cores.LIMA, ex);
        for (int i = 0; i < CONQUISTAS.length; i++) {
            float aparece = suave((entrada - 0.6f - i * 0.35f) / 0.3f);
            if (aparece <= 0f) continue;
            float ly = y + h - 100f - i * 56f;
            float cx = x + 44f;
            p.cor(Cores.LIMA, aparece * ex);
            p.linha(cx - 12f, ly, cx - 3f, ly - 11f, 5f);
            p.linha(cx - 3f, ly - 11f, cx + 15f, ly + 12f, 5f);
            Texto.desenhar(p, jogo.fontes.media, CONQUISTAS[i], x + 80f, ly, Texto.ESQ, Cores.BRANCO, aparece * ex);
        }
        ArteJogador.ninja(p, x + w - 80f, y + 70f + (float) Math.sin(tempo * 3f) * 5f, 2.4f, -1f, tempo, false, true, ex);

        float ed = suave((entrada - 0.35f) / 0.6f);
        desenharPainelPontos(p, 700f + (1f - ed) * 600f, 190f, 500f, 290f, Cores.AMARELO, ed);
    }
}
