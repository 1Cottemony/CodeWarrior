package com.codewarrior.telas;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.codewarrior.CodeWarriorGame;
import com.codewarrior.dados.Configuracoes;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Dificuldade;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Icones;
import com.codewarrior.ui.Mascote;
import com.codewarrior.ui.Texto;

/** Configurações em tela própria: volumes, efeitos, acessibilidade e dificuldade. */
public final class TelaConfig extends Tela {
    private static final String[] ROTULOS = {"SOM", "MÚSICA", "EFEITOS", "TELA CHEIA", "EFEITOS GLITCH",
            "LINHAS DE VARREDURA", "MIRA ASSISTIDA", "MODO FACILITADOR"};
    private static final String[] NIVEIS = {"BAIXA", "MÉDIA", "ALTA"};
    private static final float X_CTRL = 450f, LARG_SLIDER = 240f;
    private static final int LINHAS = 8, FOCO_DIF = 8, FOCO_APLICAR = 11, FOCO_VOLTAR = 12;

    private final Configuracoes edit = new Configuracoes();
    private final Botao[] dif = new Botao[3];
    private final Botao aplicar = new Botao("APLICAR", 380f, 30f, 250f, 66f, Cores.CIANO, false);
    private final Botao voltar = new Botao("VOLTAR", 660f, 30f, 250f, 66f, Cores.MAGENTA, false);
    private int foco = 0;
    private int arrastando = -1;
    private float toast;

    public TelaConfig(CodeWarriorGame jogo) {
        super(jogo);
        edit.copiarDe(jogo.config);
        Color[] cs = {Cores.VERDE, Cores.AMARELO, Cores.VERMELHO};
        String[] nomes = {"FÁCIL", "NORMAL", "DIFÍCIL"};
        for (int i = 0; i < 3; i++) dif[i] = new Botao(nomes[i], 810f, 470f - i * 100f, 380f, 80f, cs[i], false);
    }

    @Override
    public void mostrar() {
        jogo.audio.musicaLigada = true;
        jogo.audio.camadas = 0;
        entrada = 0f;
    }

    private static float yLinha(int i) {
        return 566f - i * 56f;
    }

    private float valorSlider(int i) {
        return i == 0 ? edit.volSom : edit.volMusica;
    }

    private void setSlider(int i, float v) {
        v = Math.max(0f, Math.min(1f, v));
        if (i == 0) edit.volSom = v;
        else edit.volMusica = v;
        jogo.audio.volSom = edit.volSom;
        jogo.audio.volMusica = edit.volMusica;
    }

    private boolean valorToggle(int i) {
        switch (i) {
            case 3: return edit.telaCheia;
            case 4: return edit.glitch;
            case 5: return edit.varredura;
            case 6: return edit.miraAssistida;
            default: return edit.facilitador;
        }
    }

    private void alternar(int i) {
        switch (i) {
            case 3: edit.telaCheia = !edit.telaCheia; break;
            case 4: edit.glitch = !edit.glitch; break;
            case 5: edit.varredura = !edit.varredura; break;
            case 6: edit.miraAssistida = !edit.miraAssistida; break;
            default: edit.facilitador = !edit.facilitador; break;
        }
        jogo.audio.uiOk();
    }

    private void ajustar(int i, int dir) {
        if (i < 2) {
            setSlider(i, valorSlider(i) + dir * 0.05f);
            jogo.audio.uiMover();
            if (i == 0) jogo.audio.uiOk();
        } else if (i == 2) {
            edit.intensidade = (edit.intensidade + dir + 3) % 3;
            jogo.audio.uiMover();
        } else if (i < LINHAS) {
            alternar(i);
        }
    }

    private void acao(int i) {
        if (i >= FOCO_DIF && i < FOCO_DIF + 3) {
            edit.dificuldade = i - FOCO_DIF;
            jogo.audio.uiOk();
        } else if (i == FOCO_APLICAR) {
            jogo.config.copiarDe(edit);
            jogo.config.salvar();
            jogo.aplicarConfig();
            toast = 1.6f;
            jogo.audio.uiOk();
        } else if (i == FOCO_VOLTAR) {
            sair();
        } else if (i >= 2) {
            if (i == 2) ajustar(2, 1);
            else alternar(i);
        } else {
            ajustar(i, 1);
        }
    }

    private void sair() {
        jogo.audio.uiVoltar();
        jogo.aplicarConfig(); // volta aos valores salvos (descarta o que não foi aplicado)
        jogo.irPara(new TelaMenu(jogo));
    }

    @Override
    public boolean teclaDown(int k) {
        if (k == Input.Keys.ESCAPE) {
            sair();
        } else if (k == Input.Keys.DOWN || k == Input.Keys.S) {
            foco = (foco + 1) % 13;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.UP || k == Input.Keys.W) {
            foco = (foco + 12) % 13;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.LEFT || k == Input.Keys.A) {
            if (foco < LINHAS) ajustar(foco, -1);
        } else if (k == Input.Keys.RIGHT || k == Input.Keys.D) {
            if (foco < LINHAS) ajustar(foco, 1);
        } else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) {
            acao(foco);
        } else {
            return false;
        }
        return true;
    }

    private int focoEm(float x, float y) {
        for (int i = 0; i < LINHAS; i++) if (x > 70f && x < 740f && Math.abs(y - yLinha(i)) < 26f) return i;
        for (int i = 0; i < 3; i++) if (dif[i].contem(x, y)) return FOCO_DIF + i;
        if (aplicar.contem(x, y)) return FOCO_APLICAR;
        if (voltar.contem(x, y)) return FOCO_VOLTAR;
        return -1;
    }

    @Override
    public void mouseMover(float x, float y) {
        if (arrastando >= 0) {
            setSlider(arrastando, (x - X_CTRL) / LARG_SLIDER);
            return;
        }
        int f = focoEm(x, y);
        if (f >= 0 && f != foco) {
            foco = f;
            jogo.audio.uiMover();
        }
    }

    @Override
    public boolean mouseDown(float x, float y, int botao) {
        int f = focoEm(x, y);
        if (f < 0) return false;
        foco = f;
        if (f < 2 && x >= X_CTRL - 14f && x <= X_CTRL + LARG_SLIDER + 14f) {
            arrastando = f;
            setSlider(f, (x - X_CTRL) / LARG_SLIDER);
        } else if (f == 2) {
            for (int j = 0; j < 3; j++) {
                float bx = X_CTRL + j * 86f;
                if (x >= bx && x <= bx + 80f) {
                    edit.intensidade = j;
                    jogo.audio.uiMover();
                }
            }
        } else if (f >= 3) {
            if (f < LINHAS) {
                if (x >= X_CTRL - 10f && x <= X_CTRL + 110f) alternar(f);
            } else {
                acao(f);
            }
        }
        return true;
    }

    @Override
    public void mouseUp(float x, float y, int botao) {
        if (arrastando == 0) jogo.audio.uiOk();
        arrastando = -1;
    }

    @Override
    public void atualizar(float dt) {
        super.atualizar(dt);
        toast = Math.max(0f, toast - dt);
        for (int i = 0; i < 3; i++) {
            dif[i].foco = foco == FOCO_DIF + i;
            dif[i].preenchido = edit.dificuldade == i;
            dif[i].atualizar(dt, false);
        }
        aplicar.foco = foco == FOCO_APLICAR;
        voltar.foco = foco == FOCO_VOLTAR;
        aplicar.atualizar(dt, false);
        voltar.atualizar(dt, false);
    }

    @Override
    public void desenhar(Pincel p) {
        jogo.fundo.desenhar(p, jogo.fontes, tempo, 0.8f);
        float k = suave(entrada / 0.5f);
        Texto.neon(p, jogo.fontes.tituloM, "CONFIGURAÇÕES", 640f, 670f + (1f - k) * 100f, Texto.CENTRO, Cores.CIANO, k);

        // painel esquerdo
        float ex = suave(entrada / 0.55f), ed = suave((entrada - 0.1f) / 0.55f);
        float dxE = (1f - ex) * -700f, dxD = (1f - ed) * 700f;
        p.cor(0.03f, 0.07f, 0.11f, 0.9f * ex);
        p.retaCortada(60f + dxE, 128f, 690f, 500f, 12f);
        p.cor(Cores.CIANO, ex);
        p.bordaCortada(60f + dxE, 128f, 690f, 500f, 12f, 3f);
        p.halo(60f + dxE, 128f, 690f, 500f, Cores.CIANO, 0.5f * ex);
        for (int i = 0; i < LINHAS; i++) {
            float y = yLinha(i);
            if (foco == i) {
                p.cor(Cores.CIANO, 0.10f * ex);
                p.reta(72f + dxE, y - 26f, 666f, 52f);
                p.cor(Cores.CIANO, ex);
                p.tri(78f + dxE, y + 9f, 78f + dxE, y - 9f, 92f + dxE, y);
            }
            Texto.desenhar(p, jogo.fontes.media, ROTULOS[i], 104f + dxE, y, Texto.ESQ, Cores.BRANCO, ex);
            desenharControle(p, i, y, dxE, ex);
        }

        // painel direito
        p.cor(0.03f, 0.07f, 0.11f, 0.9f * ed);
        p.retaCortada(780f + dxD, 128f, 440f, 500f, 12f);
        p.cor(Cores.MAGENTA, ed);
        p.bordaCortada(780f + dxD, 128f, 440f, 500f, 12f, 3f);
        p.halo(780f + dxD, 128f, 440f, 500f, Cores.MAGENTA, 0.5f * ed);
        p.cor(Cores.FUNDO, ed);
        p.reta(900f + dxD, 612f, 200f, 32f);
        Texto.neon(p, jogo.fontes.media, "DIFICULDADE", 1000f + dxD, 628f, Texto.CENTRO, Cores.MAGENTA, ed);
        for (int i = 0; i < 3; i++) {
            Botao b = dif[i];
            b.x = 810f;
            b.desenhar(p, jogo.fontes.grande, tempo, ed, dxD);
            int cor = Dificuldade.deIndice(i).coracoes;
            Icones.coracao(p, 810f + 36f + dxD, b.y + 40f, 26f, true, 0f, ed);
            Texto.desenhar(p, jogo.fontes.media, "×" + cor, 810f + 62f + dxD, b.y + 40f, Texto.ESQ, b.preenchido ? Cores.FUNDO : Cores.BRANCO, ed);
        }
        Texto.desenhar(p, jogo.fontes.pequena, "Corações no Normal; facilitador soma +2", 1000f + dxD, 160f, Texto.CENTRO, Cores.CINZA, ed);

        float eb = suave((entrada - 0.3f) / 0.4f);
        aplicar.desenhar(p, jogo.fontes.grande, tempo, eb, 0f);
        voltar.desenhar(p, jogo.fontes.grande, tempo, eb, 0f);
        if (toast > 0f) Texto.neon(p, jogo.fontes.media, "APLICADO!", 640f, 124f, Texto.CENTRO, Cores.LIMA, Math.min(1f, toast * 2f));
        Mascote.personagem(p, 1190f, 36f, 0.9f, tempo, eb);
        jogo.particulas.desenhar(p, jogo.fontes);
    }

    private void desenharControle(Pincel p, int i, float y, float dx, float a) {
        float x0 = X_CTRL + dx;
        if (i < 2) {
            float v = valorSlider(i);
            Color c = i == 0 ? Cores.CIANO : Cores.AMARELO;
            p.cor(0.05f, 0.09f, 0.14f, a);
            p.reta(x0, y - 5f, LARG_SLIDER, 10f);
            p.cor(c, a);
            p.reta(x0, y - 5f, LARG_SLIDER * v, 10f);
            p.borda(x0, y - 5f, LARG_SLIDER, 10f, 2f);
            p.brilho(x0 + LARG_SLIDER * v, y, 26f);
            p.cor(c, a);
            p.disco(x0 + LARG_SLIDER * v, y, 13f);
            p.cor(Cores.FUNDO, a);
            p.disco(x0 + LARG_SLIDER * v, y, 5f);
            Texto.desenhar(p, jogo.fontes.pequena, Math.round(v * 100f) + "%", x0 + LARG_SLIDER + 28f, y, Texto.ESQ, Cores.BRANCO, a);
        } else if (i == 2) {
            for (int j = 0; j < 3; j++) {
                boolean sel = edit.intensidade == j;
                float bx = x0 + j * 86f;
                p.cor(sel ? Cores.AMARELO : Cores.CINZA, (sel ? 0.9f : 0.35f) * a);
                if (sel) p.retaCortada(bx, y - 17f, 80f, 34f, 6f);
                else p.bordaCortada(bx, y - 17f, 80f, 34f, 6f, 2f);
                Texto.desenhar(p, jogo.fontes.pequena, NIVEIS[j], bx + 40f, y, Texto.CENTRO, sel ? Cores.FUNDO : Cores.BRANCO, a);
            }
        } else {
            boolean on = valorToggle(i);
            Color c = on ? Cores.VERDE : Cores.MAGENTA;
            p.cor(0.05f, 0.09f, 0.14f, a);
            p.retaCortada(x0, y - 18f, 100f, 36f, 8f);
            p.cor(c, a);
            p.bordaCortada(x0, y - 18f, 100f, 36f, 8f, 2.5f);
            p.cor(c, 0.9f * a);
            p.retaCortada(on ? x0 + 52f : x0 + 4f, y - 14f, 44f, 28f, 6f);
            Texto.desenhar(p, jogo.fontes.pequena, on ? "ON" : "OFF", on ? x0 + 28f : x0 + 74f, y, Texto.CENTRO, on ? Cores.FUNDO : Cores.BRANCO, a);
        }
    }
}
