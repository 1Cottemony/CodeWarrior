package com.codewarrior.telas;

import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.logica.Evento;
import com.codewarrior.logica.Inimigo;
import com.codewarrior.logica.Regras;
import com.codewarrior.logica.Simulacao;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Fontes;
import com.codewarrior.ui.Mascote;
import com.codewarrior.ui.Texto;

/**
 * Tutorial opcional em 7 passos. Cada passo tem 3 fases: DEMO (o mascote mostra sozinho), ACAO (sua vez) e OK
 * (parabéns; só avança quando você apertar Enter ou clicar em CONTINUAR). Nada avança sozinho para o próximo passo.
 * Cada passo usa uma Simulacao nova, assim as recargas das habilidades começam zeradas.
 */
public final class Tutorial {
    public static final int PASSOS = 7;

    public enum Fase { DEMO, ACAO, OK }

    private static final int TP = 0, PREVIA = 1, DASH = 2, SEM_PREVIA = 3, COPIAR = 4, COLAR = 5, DESFAZER = 6, PUSH = 7,
            TERM_ABRE = 8, TERM_DIGITA = 9, TERM_FECHA = 10;

    private static final class Acao {
        final float t;
        final int tipo;
        final String tecla;

        Acao(float t, int tipo, String tecla) {
            this.t = t;
            this.tipo = tipo;
            this.tecla = tecla;
        }
    }

    private static final String[] INICIO = {"G", "G", "M", "G", "S", "G", "G"};

    private static final String[] FALA_DEMO = {
            "Eu sou o seu cursor! Segure uma tecla para ver a linha-fantasma e solte para dar o dash.",
            "Bugs andam pelo teclado. Atravesse eles com o dash para deletar!",
            "Pares de teclas explodem! Dash em , e depois em . em até 3 segundos.",
            "Errou feio? Ctrl+Z volta 2 segundos no tempo (recarga de 20 s).",
            "Ctrl+C marca onde você está. Em até 5 s, Ctrl+V cola um clone que ataca bugs por 8 s.",
            "Aperte Enter para abrir o terminal em câmera lenta e digite um comando: git push.",
            "",
    };
    private static final String[] FALA_ACAO = {
            "Sua vez: segure uma tecla qualquer e solte para dar o dash.",
            "Sua vez: destrua os 3 bugs parados no teclado.",
            "Sua vez: dash em , (vírgula) e depois em . (ponto) para explodir os bugs.",
            "Sua vez: dê um ou dois dashes e aperte Ctrl+Z.",
            "Sua vez: Ctrl+C, dash para bem longe e Ctrl+V.",
            "Sua vez: Enter, digite git push e Enter. Esc fecha o terminal.",
            "",
    };
    private static final String[] FALA_OK = {
            "Isso! Quanto mais bugs na linha, mais pontos.",
            "Deletados! Dashes em sequência rápida fazem o combo subir até ×5.",
            "BOOM! Os pares são ( ), < > e [ ]. Use perto de vários bugs.",
            "Voltou no tempo! Bugs, pontos e corações voltam. A recarga não.",
            "O clone luta no ponto copiado. Ótimo para proteger uma área!",
            "git push empurra e atordoa os bugs. sudo rm -rf apaga todos (recarga de 90 s).",
            "Pronto! Cuidado com o chefão Div Desalinhada. Boa sorte, mestre do debug!",
    };

    private final Regras regras;
    public final Mascote mascote = new Mascote();
    private final Botao proximo = new Botao("CONTINUAR", 1040f, 545f, 220f, 62f, Cores.CIANO, true);

    public int passo;
    public Fase fase = Fase.DEMO;
    /** Tecla mostrada na linha-fantasma da demonstração (-1 = nenhuma). */
    public int fantasma = -1;
    /** Texto digitado na demonstração do terminal (null = terminal fechado). */
    public String terminalDemo;
    /** True quando o jogador confirmou o último passo. */
    public boolean encerrado;

    private float t;
    private int prox;
    private Acao[] roteiro = new Acao[0];
    private float duracaoDemo;

    public Tutorial(Regras base) {
        regras = base.copia();
        regras.tutorial = true;
    }

    // ------------------------------------------------------------------ cenas
    public Simulacao comecar() {
        passo = 0;
        return entrarNoPasso();
    }

    private Simulacao entrarNoPasso() {
        t = 0f;
        prox = 0;
        fantasma = -1;
        terminalDemo = null;
        roteiro = roteiro(passo);
        duracaoDemo = roteiro.length == 0 ? 0f : roteiro[roteiro.length - 1].t + 1.3f;
        if (passo == PASSOS - 1) {
            fase = Fase.OK;
            mascote.falar(FALA_OK[passo]);
            proximo.texto = "COMEÇAR";
        } else {
            fase = Fase.DEMO;
            mascote.falar(FALA_DEMO[passo]);
            proximo.texto = "CONTINUAR";
        }
        return montar(passo);
    }

    private Simulacao montar(int p) {
        Simulacao s = new Simulacao(regras, 4242L + p);
        s.teleportar(INICIO[p]);
        switch (p) {
            case 1:
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "E");
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "U");
                s.spawnarManequim(Inimigo.Tipo.LEAK, "V");
                break;
            case 2:
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "K");
                s.spawnarManequim(Inimigo.Tipo.LEAK, "L");
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "SEMICOLON");
                break;
            case 4:
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "W");
                s.spawnarManequim(Inimigo.Tipo.LEAK, "E");
                break;
            case 5:
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "F");
                s.spawnarManequim(Inimigo.Tipo.SYNTAX, "H");
                s.spawnarManequim(Inimigo.Tipo.LEAK, "T");
                break;
            default:
                break;
        }
        return s;
    }

    private static Acao[] roteiro(int p) {
        switch (p) {
            case 0:
                return new Acao[]{new Acao(0.8f, PREVIA, "T"), new Acao(2.2f, DASH, "T"), new Acao(3.0f, PREVIA, "L"),
                        new Acao(4.2f, DASH, "L")};
            case 1:
                return new Acao[]{new Acao(0.8f, PREVIA, "E"), new Acao(2.0f, DASH, "E")};
            case 2:
                return new Acao[]{new Acao(0.8f, PREVIA, "COMMA"), new Acao(2.0f, DASH, "COMMA"),
                        new Acao(2.8f, PREVIA, "PERIOD"), new Acao(3.8f, DASH, "PERIOD")};
            case 3:
                return new Acao[]{new Acao(0.8f, DASH, "T"), new Acao(1.5f, DASH, "P"), new Acao(2.2f, DASH, "L"),
                        new Acao(3.2f, DESFAZER, null)};
            case 4:
                return new Acao[]{new Acao(0.8f, COPIAR, null), new Acao(1.6f, DASH, "L"), new Acao(2.8f, COLAR, null)};
            case 5:
                String g = "git push";
                Acao[] a = new Acao[g.length() + 3];
                a[0] = new Acao(0.8f, TERM_ABRE, null);
                for (int i = 0; i < g.length(); i++) a[i + 1] = new Acao(1.3f + i * 0.15f, TERM_DIGITA, String.valueOf(g.charAt(i)));
                a[g.length() + 1] = new Acao(3.0f, PUSH, null);
                a[g.length() + 2] = new Acao(3.0f, TERM_FECHA, null);
                return a;
            default:
                return new Acao[0];
        }
    }

    // ------------------------------------------------------------------ andamento
    /** Avança os relógios do tutorial. Devolve uma Simulacao nova quando a demonstração acaba (cena reiniciada). */
    public Simulacao atualizar(float dt, Simulacao sim) {
        mascote.atualizar(dt);
        proximo.atualizar(dt, false);
        t += dt;
        if (fase == Fase.DEMO) {
            while (prox < roteiro.length && t >= roteiro[prox].t) executar(roteiro[prox++], sim);
            if (prox >= roteiro.length && t >= duracaoDemo) return comecarAcao();
        } else if (fase == Fase.ACAO) {
            if (passo == 1 && t > 0.4f && sim.e.inimigos.isEmpty()) cumprir();
        }
        return null;
    }

    private void executar(Acao a, Simulacao sim) {
        switch (a.tipo) {
            case PREVIA:
                fantasma = sim.teclado.indice(a.tecla);
                break;
            case SEM_PREVIA:
                fantasma = -1;
                break;
            case DASH:
                fantasma = -1;
                sim.dash(sim.teclado.indice(a.tecla));
                break;
            case COPIAR:
                sim.copiar();
                break;
            case COLAR:
                sim.colar();
                break;
            case DESFAZER:
                sim.desfazer();
                break;
            case PUSH:
                sim.gitPush();
                break;
            case TERM_ABRE:
                terminalDemo = "";
                break;
            case TERM_DIGITA:
                terminalDemo = terminalDemo + a.tecla;
                break;
            case TERM_FECHA:
                terminalDemo = null;
                break;
            default:
                break;
        }
    }

    private Simulacao comecarAcao() {
        fase = Fase.ACAO;
        t = 0f;
        fantasma = -1;
        terminalDemo = null;
        mascote.falar(FALA_ACAO[passo]);
        return montar(passo);
    }

    /** Pula só a demonstração do passo atual (Enter durante a demo). */
    public Simulacao pularDemo() {
        return fase == Fase.DEMO ? comecarAcao() : null;
    }

    private void cumprir() {
        if (fase != Fase.ACAO) return;
        fase = Fase.OK;
        t = 0f;
        mascote.falar(FALA_OK[passo]);
    }

    /** O jogo avisa cada evento da simulação; assim o tutorial sabe quando você fez o que foi pedido. */
    public void evento(Evento ev) {
        if (fase != Fase.ACAO) return;
        switch (passo) {
            case 0:
                if (ev.tipo == Evento.Tipo.DASH_INICIO) cumprir();
                break;
            case 2:
                if (ev.tipo == Evento.Tipo.EXPLOSAO_PAR) cumprir();
                break;
            case 3:
                if (ev.tipo == Evento.Tipo.DESFAZER) cumprir();
                break;
            case 4:
                if (ev.tipo == Evento.Tipo.CLONE) cumprir();
                break;
            case 5:
                if (ev.tipo == Evento.Tipo.PUSH) cumprir();
                break;
            default:
                break;
        }
    }

    /** Enter ou clique em CONTINUAR no fim de um passo. Devolve a cena do próximo passo (ou null no fim). */
    public Simulacao avancar() {
        if (fase != Fase.OK) return null;
        if (passo >= PASSOS - 1) {
            encerrado = true;
            return null;
        }
        passo++;
        return entrarNoPasso();
    }

    public boolean emDemo() {
        return fase == Fase.DEMO;
    }

    /** O terminal (Enter) só funciona a partir do passo 6 e fora da demonstração. */
    public boolean terminalLiberado() {
        return passo >= 5 && fase != Fase.DEMO;
    }

    /** Quantos botões de habilidade aparecem na barra de baixo. */
    public int slotsVisiveis() {
        return passo < 3 ? 0 : passo - 2;
    }

    public boolean cliqueNoBotao(float x, float y) {
        return fase == Fase.OK && proximo.contem(x, y);
    }

    public void mouseMover(float x, float y) {
        proximo.foco = fase == Fase.OK && proximo.contem(x, y);
    }

    // ------------------------------------------------------------------ desenho
    public void desenhar(Pincel p, Fontes f, float tempo) {
        Mascote.personagem(p, 100f, 622f, 1.05f, tempo, 1f);
        mascote.balao(p, f, 170f, 535f, 860f, 170f, tempo, 1f, 142f, 620f);
        String tag = fase == Fase.DEMO ? "DEMONSTRAÇÃO" : fase == Fase.ACAO ? "SUA VEZ!" : "BOA!";
        com.badlogic.gdx.graphics.Color cor = fase == Fase.DEMO ? Cores.AMARELO : fase == Fase.ACAO ? Cores.MAGENTA : Cores.LIMA;
        float pulso = fase == Fase.ACAO ? 0.7f + 0.3f * (float) Math.sin(tempo * 8f) : 1f;
        Texto.neon(p, f.pequena, tag, 1010f, 688f, Texto.DIR, cor, pulso);
        Texto.desenhar(p, f.pequena, "PASSO " + (passo + 1) + "/" + PASSOS, 1250f, 700f, Texto.DIR, Cores.BRANCO, 0.9f);
        Texto.desenhar(p, f.pequena, "Esc: pular tutorial", 1250f, 668f, Texto.DIR, Cores.CINZA, 0.9f);
        if (fase == Fase.OK) {
            proximo.desenhar(p, f.grande, tempo, 1f, 0f);
            Texto.desenhar(p, f.pequena, "ou aperte Enter", 1150f, 525f, Texto.CENTRO, Cores.CINZA, 0.9f);
        } else if (fase == Fase.DEMO) {
            Texto.desenhar(p, f.pequena, "Enter: pular demonstração", 1250f, 636f, Texto.DIR, Cores.CINZA, 0.9f);
        }
    }
}
