package com.codewarrior.telas;

import com.badlogic.gdx.Input;
import com.codewarrior.CodeWarriorGame;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Mascote;
import com.codewarrior.ui.Texto;

/** O mascote pergunta se você quer o tutorial (opcional). */
public final class TelaPergunta extends Tela {
    private final Mascote mascote = new Mascote();
    private final Botao sim = new Botao("SIM", 380f, 170f, 240f, 80f, Cores.CIANO, true);
    private final Botao pular = new Botao("PULAR", 660f, 170f, 240f, 80f, Cores.MAGENTA, false);
    private int foco = 0;

    public TelaPergunta(CodeWarriorGame jogo) {
        super(jogo);
    }

    @Override
    public void mostrar() {
        mascote.falar("Quer um tutorial rápido? Prometo: zero bugs. (Quase.)");
        entrada = 0f;
    }

    private void escolher(boolean tutorial) {
        jogo.audio.uiOk();
        jogo.irPara(new TelaJogo(jogo, jogo.novasRegras(), tutorial));
    }

    @Override
    public boolean teclaDown(int k) {
        if (k == Input.Keys.LEFT || k == Input.Keys.A) {
            foco = 0;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.RIGHT || k == Input.Keys.D) {
            foco = 1;
            jogo.audio.uiMover();
        } else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) {
            escolher(foco == 0);
        } else if (k == Input.Keys.ESCAPE) {
            jogo.audio.uiVoltar();
            jogo.irPara(new TelaMenu(jogo));
        } else {
            return false;
        }
        return true;
    }

    @Override
    public void mouseMover(float x, float y) {
        if (sim.contem(x, y) && foco != 0) {
            foco = 0;
            jogo.audio.uiMover();
        } else if (pular.contem(x, y) && foco != 1) {
            foco = 1;
            jogo.audio.uiMover();
        }
    }

    @Override
    public boolean mouseDown(float x, float y, int botao) {
        if (sim.contem(x, y)) {
            escolher(true);
            return true;
        }
        if (pular.contem(x, y)) {
            escolher(false);
            return true;
        }
        return false;
    }

    @Override
    public void atualizar(float dt) {
        super.atualizar(dt);
        mascote.atualizar(dt);
        sim.foco = foco == 0;
        pular.foco = foco == 1;
        sim.atualizar(dt, false);
        pular.atualizar(dt, false);
    }

    @Override
    public void desenhar(Pincel p) {
        jogo.fundo.desenhar(p, jogo.fontes, tempo, 1f);
        float k = suave(entrada / 0.5f);
        Texto.neon(p, jogo.fontes.tituloM, "TUTORIAL?", 640f, 600f + (1f - k) * 120f, Texto.CENTRO, Cores.CIANO, k);
        Mascote.personagem(p, 220f - (1f - k) * 300f, 380f, 1.7f, tempo, k);
        mascote.balao(p, jogo.fontes, 360f, 310f, 700f, 150f, tempo, k, 300f, 390f);
        float e1 = suave((entrada - 0.3f) / 0.4f), e2 = suave((entrada - 0.42f) / 0.4f);
        sim.desenhar(p, jogo.fontes.grande, tempo, e1, (1f - e1) * -400f);
        pular.desenhar(p, jogo.fontes.grande, tempo, e2, (1f - e2) * 400f);
        Texto.desenhar(p, jogo.fontes.pequena, "Esc: voltar ao menu", 640f, 90f, Texto.CENTRO, Cores.CINZA, 0.9f);
    }
}
