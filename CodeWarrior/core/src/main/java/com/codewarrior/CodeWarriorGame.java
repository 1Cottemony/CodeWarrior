package com.codewarrior;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.codewarrior.audio.Sintetizador;
import com.codewarrior.dados.Configuracoes;
import com.codewarrior.efeitos.Fundo;
import com.codewarrior.efeitos.Juice;
import com.codewarrior.efeitos.Particulas;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.efeitos.PosProcessamento;
import com.codewarrior.logica.Dificuldade;
import com.codewarrior.logica.Regras;
import com.codewarrior.telas.Tela;
import com.codewarrior.telas.TelaMenu;
import com.codewarrior.ui.Fontes;
import com.codewarrior.ui.Transicao;

/**
 * Jogo principal: guarda os recursos compartilhados (pincel, fontes, áudio, efeitos), desenha a cena
 * num FrameBuffer 16:9 (faixas pretas, sem deformar) e repassa teclado/mouse para a tela atual.
 */
public class CodeWarriorGame extends ApplicationAdapter {
    public static final float LARGURA = 1280f, ALTURA = 720f;

    public Pincel pincel;
    public Fontes fontes;
    public Sintetizador audio;
    public Configuracoes config;
    public PosProcessamento pos;
    public final Juice juice = new Juice();
    public final Particulas particulas = new Particulas();
    public final Fundo fundo = new Fundo();
    public final Transicao transicao = new Transicao();

    private OrthographicCamera cameraHud, cameraMundo;
    private Tela tela;
    private float tempoGlobal;
    private int vx, vy, vw, vh;
    private float escalaVista = 1f;
    private float mouseX, mouseY;
    private boolean telaCheiaPendente;
    private int fboPendenteW;
    private float fboPendenteDesde;

    @Override
    public void create() {
        config = Configuracoes.carregar();
        pincel = new Pincel();
        fontes = new Fontes();
        audio = new Sintetizador();
        pos = new PosProcessamento();
        cameraHud = new OrthographicCamera();
        cameraHud.setToOrtho(false, LARGURA, ALTURA);
        cameraMundo = new OrthographicCamera();
        cameraMundo.setToOrtho(false, LARGURA, ALTURA);
        aplicarConfig();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override
            public boolean keyDown(int keycode) {
                if (keycode == Input.Keys.F11) {
                    config.telaCheia = !Gdx.graphics.isFullscreen();
                    config.salvar();
                    aplicarConfig();
                    return true;
                }
                return tela != null && !transicao.ocupada() && tela.teclaDown(keycode);
            }

            @Override
            public boolean keyUp(int keycode) {
                return tela != null && !transicao.ocupada() && tela.teclaUp(keycode);
            }

            @Override
            public boolean keyTyped(char character) {
                if (tela != null && !transicao.ocupada()) tela.caractere(character);
                return true;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (tela != null && !transicao.ocupada()) tela.mouseMover(mouseX, mouseY);
                return true;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (tela != null && !transicao.ocupada()) tela.mouseMover(mouseX, mouseY);
                return true;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                return tela != null && !transicao.ocupada() && tela.mouseDown(mouseX, mouseY, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (tela != null && !transicao.ocupada()) tela.mouseUp(mouseX, mouseY, button);
                return true;
            }
        });
        tela = new TelaMenu(this);
        tela.mostrar();
    }

    // ------------------------------------------------------------------ configurações
    public Regras novasRegras() {
        Regras r = new Regras();
        r.dificuldade = Dificuldade.deIndice(config.dificuldade);
        r.facilitador = config.facilitador;
        r.miraAssistida = config.miraAssistida;
        return r;
    }

    /** Aplica as configurações salvas ao áudio, efeitos e janela. */
    public void aplicarConfig() {
        audio.volSom = config.volSom;
        audio.volMusica = config.volMusica;
        float f = config.fatorEfeitos();
        juice.intensidade = f;
        juice.glitchLigado = config.glitch;
        particulas.quantidade = f;
        pos.bloom = config.intensidade == 0 ? 0.28f : config.intensidade == 1 ? 0.45f : 0.7f;
        pos.aberracao = !config.glitch ? 0.0004f : config.intensidade == 0 ? 0.0007f : config.intensidade == 1 ? 0.0015f : 0.003f;
        pos.varredura = config.varredura ? 0.5f : 0f;
        telaCheiaPendente = true; // a troca de janela é feita no começo do próximo quadro (mais seguro)
    }

    private void trocarModoDaJanela() {
        telaCheiaPendente = false;
        try {
            if (config.telaCheia != Gdx.graphics.isFullscreen()) {
                boolean ok;
                if (config.telaCheia) {
                    Graphics.DisplayMode dm = Gdx.graphics.getDisplayMode();
                    ok = Gdx.graphics.setFullscreenMode(dm);
                } else {
                    ok = Gdx.graphics.setWindowedMode(1280, 720);
                }
                if (!ok) config.telaCheia = Gdx.graphics.isFullscreen();
                fboPendenteDesde = 0f;
            }
        } catch (Throwable t) {
            Gdx.app.error("Janela", "Não foi possível trocar o modo de tela: " + t);
            config.telaCheia = Gdx.graphics.isFullscreen();
        }
    }

    // ------------------------------------------------------------------ telas
    public void irPara(final Tela nova) {
        transicao.iniciar(new Runnable() {
            @Override
            public void run() {
                if (tela != null) tela.ocultar();
                tela = nova;
                tela.mostrar();
            }
        });
    }

    public void sair() {
        transicao.iniciar(new Runnable() {
            @Override
            public void run() {
                Gdx.app.exit();
            }
        });
    }

    public float tempoGlobal() {
        return tempoGlobal;
    }

    // ------------------------------------------------------------------ câmeras
    /** Câmera do mundo (com o tremor de tela). */
    public void usarCameraMundo(Pincel p) {
        cameraMundo.position.set(LARGURA / 2f + juice.tremorX(), ALTURA / 2f + juice.tremorY(), 0f);
        cameraMundo.update();
        p.batch.setProjectionMatrix(cameraMundo.combined);
    }

    /** Câmera fixa (interface, sem tremor). */
    public void usarCameraHud(Pincel p) {
        p.batch.setProjectionMatrix(cameraHud.combined);
    }

    // ------------------------------------------------------------------ laço
    private void redimensionarSeguro(int fw) {
        try {
            pos.redimensionar(fw, Math.round(fw * 9f / 16f));
        } catch (Throwable t) {
            Gdx.app.error("Janela", "Falha ao redimensionar a cena: " + t);
            if (pos.largura() == 0) pos.redimensionar(1280, 720);
        }
    }

    private void calcularViewport(float dt) {
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        escalaVista = Math.min(bw / LARGURA, bh / ALTURA);
        vw = Math.max(2, Math.round(LARGURA * escalaVista));
        vh = Math.max(2, Math.round(ALTURA * escalaVista));
        vx = (bw - vw) / 2;
        vy = (bh - vh) / 2;
        int fw = Math.max(640, Math.min(1920, vw));
        if (pos.largura() == 0) {
            redimensionarSeguro(fw);
            fboPendenteW = fw;
        } else if (fw != pos.largura()) {
            if (fw != fboPendenteW) {
                fboPendenteW = fw;
                fboPendenteDesde = 0f;
            }
            fboPendenteDesde += dt;
            if (fboPendenteDesde > 0.25f) redimensionarSeguro(fw);
        }
        float mx = Gdx.input.getX() * (float) bw / Math.max(1, Gdx.graphics.getWidth());
        float my = Gdx.input.getY() * (float) bh / Math.max(1, Gdx.graphics.getHeight());
        mouseX = (mx - vx) / escalaVista;
        mouseY = ((bh - my) - vy) / escalaVista;
    }

    @Override
    public void render() {
        if (telaCheiaPendente) trocarModoDaJanela();
        int bw = Gdx.graphics.getBackBufferWidth(), bh = Gdx.graphics.getBackBufferHeight();
        if (bw < 2 || bh < 2) return;
        float dt = Math.min(Gdx.graphics.getDeltaTime(), 0.05f);
        tempoGlobal += dt;
        calcularViewport(dt);
        juice.atualizar(dt);
        transicao.atualizar(dt);
        tela.atualizar(dt);

        Gdx.gl.glViewport(0, 0, bw, bh);
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        pos.tempo = tempoGlobal;
        pos.glitch = juice.glitch;
        pos.comecar();
        usarCameraHud(pincel);
        pincel.batch.begin();
        pincel.aditivo(false);
        pincel.cor(1f, 1f, 1f, 1f);
        tela.desenhar(pincel);
        usarCameraHud(pincel);
        if (juice.flash > 0.01f) {
            pincel.cor(juice.flashR, juice.flashG, juice.flashB, Math.min(0.6f, juice.flash * 0.5f));
            pincel.reta(0f, 0f, LARGURA, ALTURA);
        }
        transicao.desenhar(pincel);
        pincel.aditivo(false);
        pincel.batch.end();
        pos.terminar(vx, vy, vw, vh);
    }

    @Override
    public void dispose() {
        if (tela != null) tela.ocultar();
        config.salvar();
        audio.dispose();
        pincel.dispose();
        fontes.dispose();
        pos.dispose();
    }
}
