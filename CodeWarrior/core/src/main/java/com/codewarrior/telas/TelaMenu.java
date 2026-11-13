package com.codewarrior.telas;

import com.badlogic.gdx.Input;
import com.codewarrior.CodeWarriorGame;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.inimigos.ArteInimigos;
import com.codewarrior.jogador.ArteJogador;
import com.codewarrior.logica.Dificuldade;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Hud;
import com.codewarrior.ui.Texto;

/** Menu principal: poucos botões (Jogar, Configurações, Sair). */
public final class TelaMenu extends Tela {
    private static final String[] FRASES = {
            "// TODO: consertar o mundo",
            "Funciona na minha máquina.",
            "git blame: foi o estagiário.",
            "Não é bug, é feature.",
            "Ctrl+Z não resolve a vida.",
            "Compilou? Então manda pra produção.",
    };
    private final Botao[] botoes;
    private int foco = 0;

    public TelaMenu(CodeWarriorGame jogo) {
        super(jogo);
        botoes = new Botao[]{
                new Botao("JOGAR", 450f, 370f, 380f, 80f, Cores.CIANO, true),
                new Botao("CONFIGURAÇÕES", 450f, 270f, 380f, 80f, Cores.MAGENTA, false),
                new Botao("SAIR", 450f, 170f, 380f, 80f, Cores.CORAL, false),
        };
    }

    @Override
    public void mostrar() {
        jogo.audio.musicaLigada = true;
        jogo.audio.camadas = 0;
        jogo.audio.andamento = 1f;
        jogo.audio.abafar = 0f;
        entrada = 0f;
    }

    private void ativar(int i) {
        jogo.audio.uiOk();
        botoes[i].clique = 1f;
        if (i == 0) jogo.irPara(new TelaPergunta(jogo));
        else if (i == 1) jogo.irPara(new TelaConfig(jogo));
        else jogo.sair();
    }

    private void mudarFoco(int d) {
        foco = (foco + d + botoes.length) % botoes.length;
        jogo.audio.uiMover();
    }

    @Override
    public boolean teclaDown(int k) {
        if (k == Input.Keys.DOWN || k == Input.Keys.S || k == Input.Keys.TAB) mudarFoco(1);
        else if (k == Input.Keys.UP || k == Input.Keys.W) mudarFoco(-1);
        else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) ativar(foco);
        else return false;
        return true;
    }

    @Override
    public void mouseMover(float x, float y) {
        for (int i = 0; i < botoes.length; i++) {
            if (botoes[i].contem(x, y) && foco != i) {
                foco = i;
                jogo.audio.uiMover();
            }
        }
    }

    @Override
    public boolean mouseDown(float x, float y, int botao) {
        for (int i = 0; i < botoes.length; i++) {
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
        for (int i = 0; i < botoes.length; i++) {
            botoes[i].foco = i == foco;
            botoes[i].atualizar(dt, false);
        }
    }

    @Override
    public void desenhar(Pincel p) {
        jogo.fundo.desenhar(p, jogo.fontes, tempo, 1f);
        jogo.particulas.desenhar(p, jogo.fontes);

        // teclado e bugs no canto inferior esquerdo
        float[][] ts = {{70f, 200f}, {140f, 200f}, {210f, 200f}, {105f, 134f}, {175f, 134f}};
        String[] rot = {"4", "5", "T", "D", "F"};
        for (int i = 0; i < ts.length; i++) {
            p.cor(0.035f, 0.09f, 0.13f, 0.9f);
            p.retaCortada(ts[i][0] - 29f, ts[i][1] - 29f, 58f, 58f, 6f);
            p.cor(Cores.CIANO, 0.9f);
            p.bordaCortada(ts[i][0] - 29f, ts[i][1] - 29f, 58f, 58f, 6f, 2.6f);
            Texto.desenhar(p, jogo.fontes.media, rot[i], ts[i][0], ts[i][1], Texto.CENTRO, Cores.BRANCO, 0.9f);
        }
        ArteInimigos.syntax(p, 250f, 258f + (float) Math.sin(tempo * 2f) * 6f, tempo, 1f, 0f, false, false);
        ArteInimigos.leak(p, 300f, 150f + (float) Math.sin(tempo * 1.7f) * 5f, tempo, 1f, 0f, false);
        ArteInimigos.loop(p, 345f, 250f, tempo, 1f, 0f, false, false, 1f);

        // ninja grande no canto inferior direito
        ArteJogador.ninja(p, 1110f, 170f + (float) Math.sin(tempo * 2f) * 4f, 4.2f, -1f, tempo, false, true, 1f);

        // título
        float k = suave(entrada / 0.7f);
        float ty = 610f + (1f - k) * 160f;
        float glitch = jogo.config.glitch ? 1f : 0.2f;
        Texto.glitch(p, jogo.fontes.titulo, "CODE WARRIOR", 640f, ty, Cores.BRANCO, glitch * (0.6f + 0.4f * (float) Math.sin(tempo * 2f)), tempo);
        p.aditivo(true);
        Texto.gradiente(p, jogo.fontes.titulo, "CODE WARRIOR", 640f, ty, 0.88f, 0.52f + (float) Math.sin(tempo) * 0.02f, 0.55f);
        p.aditivo(false);
        Texto.neon(p, jogo.fontes.media, "O MESTRE DO DEBUG", 640f, ty - 66f, Texto.CENTRO, Cores.CIANO, k);

        // botões entrando com atraso
        for (int i = 0; i < botoes.length; i++) {
            float e = suave((entrada - 0.25f - i * 0.12f) / 0.45f);
            botoes[i].desenhar(p, jogo.fontes.grande, tempo, e, -(1f - e) * 500f);
        }

        // dificuldade e recorde
        Dificuldade d = Dificuldade.deIndice(jogo.config.dificuldade);
        Texto.desenhar(p, jogo.fontes.pequena, "DIFICULDADE: " + d.nome, 1250f, 690f, Texto.DIR, Cores.AMARELO, 0.9f);
        Texto.desenhar(p, jogo.fontes.pequena, "RECORDE: " + Hud.pontos(jogo.config.recorde(d)), 1250f, 662f, Texto.DIR, Cores.LIMA, 0.9f);
        String frase = FRASES[((int) (tempo / 5f)) % FRASES.length];
        Texto.desenhar(p, jogo.fontes.pequena, frase, 640f, 50f, Texto.CENTRO, Cores.CINZA, 0.9f);
        Texto.desenhar(p, jogo.fontes.pequena, "F11: tela cheia", 24f, 690f, Texto.ESQ, Cores.CINZA, 0.7f);
    }
}
