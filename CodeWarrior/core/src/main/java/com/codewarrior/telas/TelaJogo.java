package com.codewarrior.telas;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.codewarrior.CodeWarriorGame;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;
import com.codewarrior.inimigos.ArteChefe;
import com.codewarrior.inimigos.ArteInimigos;
import com.codewarrior.jogador.ArteJogador;
import com.codewarrior.logica.Caminho;
import com.codewarrior.logica.Estado;
import com.codewarrior.logica.Evento;
import com.codewarrior.logica.Habilidade;
import com.codewarrior.logica.Inimigo;
import com.codewarrior.logica.Regras;
import com.codewarrior.logica.Simulacao;
import com.codewarrior.logica.Teclado;
import com.codewarrior.teclado.TeclasGdx;
import com.codewarrior.ui.ArteTeclado;
import com.codewarrior.ui.Botao;
import com.codewarrior.ui.Hud;
import com.codewarrior.ui.Texto;

/** A partida: lê o teclado, roda a Simulacao, transforma eventos em efeitos/sons e desenha tudo. */
public final class TelaJogo extends Tela {
    private static final float HOLD_PREVIA = 0.15f;
    private static final float TERM_LIMITE = 6f, TERM_LIMITE_TUTORIAL = 15f;

    private static final class Corte {
        final float[] x = new float[3], y = new float[3];
        int n;
        float vida;
    }

    private Simulacao sim;
    private Tutorial tut;
    private final Hud hud = new Hud();

    private int segurando = -1;
    private float tSegurando;
    private float dir = 1f, ultimoX;

    private boolean terminalAberto;
    private final StringBuilder termTexto = new StringBuilder();
    private float termRestante;
    private String termAviso = "";
    private float termAnim;

    private boolean pausado;
    private int focoPausa;
    private final Botao[] botoesPausa = {
            new Botao("RETOMAR", 450f, 320f, 380f, 76f, Cores.CIANO, true),
            new Botao("MENU PRINCIPAL", 450f, 220f, 380f, 76f, Cores.MAGENTA, false)};

    private float[] brilho;
    private final ArrayList<Corte> cortes = new ArrayList<>();
    private float fimT;
    private boolean saindo;

    public TelaJogo(CodeWarriorGame jogo, Regras regras, boolean tutorial) {
        super(jogo);
        if (tutorial) {
            tut = new Tutorial(regras);
            sim = tut.comecar();
        } else {
            sim = new Simulacao(regras, System.nanoTime());
        }
        brilho = new float[sim.teclado.teclas.length];
        ultimoX = sim.e.jx;
    }

    @Override
    public void mostrar() {
        jogo.particulas.limpar();
        jogo.audio.musicaLigada = true;
        jogo.audio.camadas = 0;
        jogo.audio.andamento = 1f;
        jogo.audio.abafar = 0f;
        entrada = 0f;
    }

    @Override
    public void ocultar() {
        jogo.audio.andamento = 1f;
        jogo.audio.abafar = 0f;
    }

    // ------------------------------------------------------------------ fluxo
    private void iniciarPartidaReal() {
        tut = null;
        sim = new Simulacao(jogo.novasRegras(), System.nanoTime());
        segurando = -1;
        terminalAberto = false;
        cortes.clear();
        jogo.particulas.limpar();
        jogo.audio.uiOk();
    }

    private void voltarAoMenu() {
        saindo = true;
        jogo.irPara(new TelaMenu(jogo));
    }

    private void abrirTerminal() {
        terminalAberto = true;
        termTexto.setLength(0);
        termAviso = "";
        termRestante = tut == null ? TERM_LIMITE : TERM_LIMITE_TUTORIAL;
        segurando = -1;
        jogo.audio.uiMover();
    }

    private void fecharTerminal() {
        terminalAberto = false;
        jogo.audio.uiVoltar();
    }

    private void enviarComando() {
        Habilidade h = Simulacao.habilidadeDoComando(termTexto.toString());
        if (h == null) {
            termAviso = "comando desconhecido: " + termTexto;
            termTexto.setLength(0);
            jogo.audio.negado();
        } else if (!sim.habilidadePronta(h)) {
            termAviso = h.nome + " em recarga: " + String.format("%.0f", sim.recargaRestante(h)) + " s";
            jogo.audio.negado();
        } else {
            sim.executarComando(h);
            terminalAberto = false;
        }
    }

    // ------------------------------------------------------------------ entrada
    private boolean ctrl() {
        return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) || Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT);
    }

    private int indiceDe(int codigo) {
        String id = TeclasGdx.id(codigo);
        if (id == null || !sim.teclado.existe(id)) return -1;
        int i = sim.teclado.indice(id);
        return sim.teclado.teclas[i].tipo == Teclado.Tipo.DESTINO ? i : -1;
    }

    @Override
    public boolean teclaDown(int k) {
        if (pausado) {
            if (k == Input.Keys.ESCAPE) pausado = false;
            else if (k == Input.Keys.UP || k == Input.Keys.DOWN || k == Input.Keys.W || k == Input.Keys.S || k == Input.Keys.TAB) {
                focoPausa = 1 - focoPausa;
                jogo.audio.uiMover();
            } else if (k == Input.Keys.ENTER || k == Input.Keys.SPACE) acaoPausa(focoPausa);
            return true;
        }
        if (terminalAberto) {
            if (k == Input.Keys.ESCAPE) fecharTerminal();
            else if (k == Input.Keys.ENTER) enviarComando();
            else if (k == Input.Keys.BACKSPACE && termTexto.length() > 0) termTexto.setLength(termTexto.length() - 1);
            return true;
        }
        if (k == Input.Keys.ESCAPE) {
            if (tut != null) iniciarPartidaReal();
            else {
                pausado = true;
                focoPausa = 0;
                segurando = -1;
            }
            return true;
        }
        if (k == Input.Keys.ENTER) {
            if (tut != null) {
                if (tut.fase == Tutorial.Fase.OK) {
                    Simulacao n = tut.avancar();
                    if (tut.encerrado) iniciarPartidaReal();
                    else if (n != null) trocarSim(n);
                    return true;
                }
                if (tut.emDemo()) {
                    Simulacao n = tut.pularDemo();
                    if (n != null) trocarSim(n);
                    return true;
                }
                if (!tut.terminalLiberado()) return true;
            }
            abrirTerminal();
            return true;
        }
        if (tut != null && tut.emDemo()) return true;
        if (k == Input.Keys.CONTROL_LEFT || k == Input.Keys.CONTROL_RIGHT) return true;
        if (ctrl()) {
            if (k == Input.Keys.Z) sim.desfazer();
            else if (k == Input.Keys.C) sim.copiar();
            else if (k == Input.Keys.V) sim.colar();
            return true;
        }
        int idx = indiceDe(k);
        if (idx < 0) return false;
        brilho[idx] = 1f;
        if (sim.e.trava[idx] > 0f) {
            jogo.audio.negado();
            return true;
        }
        segurando = idx;
        tSegurando = 0f;
        return true;
    }

    @Override
    public boolean teclaUp(int k) {
        int idx = indiceDe(k);
        if (idx >= 0 && idx == segurando) {
            segurando = -1;
            if (!pausado && !terminalAberto && (tut == null || !tut.emDemo())) sim.dash(idx);
            return true;
        }
        return false;
    }

    @Override
    public void caractere(char c) {
        if (terminalAberto && c >= 32 && c != 127 && termTexto.length() < 24) {
            termTexto.append(c);
            jogo.audio.tecla();
        }
    }

    private void trocarSim(Simulacao n) {
        sim = n;
        segurando = -1;
        ultimoX = n.e.jx;
        cortes.clear();
    }

    private void acaoPausa(int i) {
        jogo.audio.uiOk();
        if (i == 0) pausado = false;
        else voltarAoMenu();
    }

    @Override
    public void mouseMover(float x, float y) {
        if (pausado) {
            for (int i = 0; i < 2; i++) if (botoesPausa[i].contem(x, y) && focoPausa != i) {
                focoPausa = i;
                jogo.audio.uiMover();
            }
        } else if (tut != null) {
            tut.mouseMover(x, y);
        }
    }

    @Override
    public boolean mouseDown(float x, float y, int botao) {
        if (pausado) {
            for (int i = 0; i < 2; i++) if (botoesPausa[i].contem(x, y)) {
                acaoPausa(i);
                return true;
            }
        } else if (tut != null && tut.cliqueNoBotao(x, y)) {
            Simulacao n = tut.avancar();
            if (tut.encerrado) iniciarPartidaReal();
            else if (n != null) trocarSim(n);
            jogo.audio.uiOk();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ atualização
    @Override
    public void atualizar(float dt) {
        super.atualizar(dt);
        hud.atualizar(dt);
        for (int i = 0; i < botoesPausa.length; i++) {
            botoesPausa[i].foco = focoPausa == i;
            botoesPausa[i].atualizar(dt, false);
        }
        for (int i = 0; i < brilho.length; i++) brilho[i] = Math.max(0f, brilho[i] - dt * 4f);
        for (int i = cortes.size() - 1; i >= 0; i--) {
            cortes.get(i).vida -= dt;
            if (cortes.get(i).vida <= 0f) cortes.remove(i);
        }
        termAnim += ((terminalAberto ? 1f : 0f) - termAnim) * Math.min(1f, dt * 12f);

        if (!pausado) {
            float dtJogo = dt * jogo.juice.escalaTempo(dt);
            if (terminalAberto) {
                dtJogo *= 0.2f;
                termRestante -= dt;
                if (termRestante <= 0f) {
                    terminalAberto = false;
                    jogo.audio.negado();
                }
            }
            if (segurando >= 0) tSegurando += dt;
            if (tut != null) {
                Simulacao n = tut.atualizar(dt, sim);
                if (n != null) trocarSim(n);
            }
            if (dtJogo > 0f) sim.atualizar(dtJogo);
            Estado e = sim.e;
            if (Math.abs(e.jx - ultimoX) > 1f) dir = e.jx > ultimoX ? 1f : -1f;
            ultimoX = e.jx;
            if (e.dashando) jogo.particulas.rastro(e.jx, e.jy, Cores.CIANO, 4f);
            processarEventos();
            if (sim.resultado != Simulacao.Resultado.JOGANDO && !saindo) {
                fimT += dt;
                if (fimT >= (sim.resultado == Simulacao.Resultado.VITORIA ? 2.2f : 1.6f)) {
                    saindo = true;
                    jogo.irPara(new TelaFim(jogo, sim));
                }
            }
        }
        jogo.audio.andamento = terminalAberto ? 0.5f : 1f;
        jogo.audio.abafar = terminalAberto || pausado ? 0.7f : 0f;
        jogo.audio.camadas = tut != null ? 0 : sim.e.chefe != null || sim.e.onda >= 5 ? 2 : sim.e.onda >= 3 ? 1 : 0;
    }

    private void processarEventos() {
        for (int i = 0; i < sim.eventos.size(); i++) {
            Evento ev = sim.eventos.get(i);
            if (tut != null) tut.evento(ev);
            efeito(ev);
        }
        sim.limparEventos();
    }

    private void corte(float x1, float y1, float x2, float y2) {
        Corte c = new Corte();
        c.x[0] = x1;
        c.y[0] = y1;
        c.x[1] = x2;
        c.y[1] = y2;
        c.n = 2;
        c.vida = 0.3f;
        cortes.add(c);
    }

    private void efeito(Evento ev) {
        com.codewarrior.efeitos.Particulas pa = jogo.particulas;
        com.codewarrior.efeitos.Juice ju = jogo.juice;
        com.codewarrior.audio.Sintetizador au = jogo.audio;
        switch (ev.tipo) {
            case DASH_INICIO: {
                au.dash(sim.multiplicador());
                Estado e = sim.e;
                Corte c = new Corte();
                c.n = Math.max(2, Math.min(3, e.nPontos));
                for (int i = 0; i < c.n; i++) {
                    c.x[i] = e.cx[i];
                    c.y[i] = e.cy[i];
                }
                c.vida = 0.3f;
                cortes.add(c);
                break;
            }
            case DASH_FIM:
                pa.anel(ev.x, ev.y, 34f, Cores.CIANO, 0.3f);
                if (ev.n >= 3) ju.tremer(5f);
                break;
            case SURGIU:
                au.surgiu();
                pa.anel(ev.x, ev.y, 44f, ArteInimigos.corDe(Inimigo.Tipo.values()[ev.n]), 0.4f);
                break;
            case MORTE_INIMIGO: {
                Color c = ArteInimigos.corDe(Inimigo.Tipo.values()[ev.n]);
                pa.explosao(ev.x, ev.y, c, 26, 380f);
                pa.faiscas(ev.x, ev.y, Cores.BRANCO, 10, 420f);
                pa.glifos(ev.x, ev.y, c, 6, 220f);
                au.matar(ev.n);
                ju.parar(0.04f);
                ju.tremer(4f);
                if (ev.x2 > 0f) {
                    long base = ev.n == 0 ? Regras.PONTOS_SYNTAX : ev.n == 1 ? Regras.PONTOS_LEAK : Regras.PONTOS_LOOP;
                    if (tut == null) pa.numero("+" + base * sim.multiplicador(), ev.x, ev.y + 24f, Cores.AMARELO, 1);
                }
                break;
            }
            case GOLPE_LOOP:
                au.golpeLoop();
                pa.faiscas(ev.x, ev.y, Cores.ROXO, 14, 380f);
                ju.tremer(3f);
                break;
            case DANO_JOGADOR:
                au.dano();
                ju.tremer(14f);
                ju.clarao(0.8f, 1f, 0.2f, 0.2f);
                ju.glitchar(0.8f);
                ju.parar(0.08f);
                pa.explosao(ev.x, ev.y, Cores.VERMELHO, 24, 360f);
                break;
            case EXPLOSAO_PAR:
                au.explosaoPar();
                pa.anel(ev.x, ev.y, Regras.PAR_RAIO, Cores.AMARELO, 0.5f);
                pa.explosao(ev.x, ev.y, Cores.AMARELO, 40, 520f);
                ju.tremer(10f);
                ju.clarao(0.5f, 1f, 0.9f, 0.3f);
                ju.parar(0.06f);
                break;
            case PAR_ARMADO:
                au.parArmado();
                pa.anel(ev.x, ev.y, 54f, Cores.AMARELO, 0.4f);
                break;
            case ONDA_INICIO:
                au.ondaInicio();
                if (tut == null) hud.anunciar("ONDA " + ev.n, "Bugs a caminho!", 1.6f, Cores.CIANO);
                break;
            case ONDA_FIM:
                au.ondaFim();
                hud.anunciar("ONDA LIMPA!", "", 1.6f, Cores.LIMA);
                break;
            case CURA:
                au.cura();
                pa.explosao(ev.x, ev.y, Cores.VERDE, 18, 260f);
                pa.numero("+1 VIDA", ev.x, ev.y + 30f, Cores.LIMA, 1);
                break;
            case CHEFE_ENTRADA:
                au.chefeEntrada();
                hud.anunciar("CHEFÃO", "Div Desalinhada", 2.4f, Cores.VERMELHO);
                ju.tremer(12f);
                ju.glitchar(1f);
                break;
            case CHEFE_DANO:
                au.chefeDano();
                ju.tremer(8f);
                ju.parar(0.05f);
                pa.explosao(ev.x, ev.y, Cores.VERMELHO, 22, 420f);
                break;
            case CHEFE_FASE:
                au.chefeFase();
                hud.anunciar("FASE " + ev.n, "Ela está ficando brava!", 1.6f, Cores.CORAL);
                ju.clarao(0.6f, 1f, 0.3f, 0.3f);
                ju.glitchar(1f);
                break;
            case CHEFE_MORTO:
                au.chefeMorto();
                pa.explosao(ev.x, ev.y, Cores.AMARELO, 80, 700f);
                pa.glifos(ev.x, ev.y, Cores.LIMA, 30, 500f);
                ju.tremer(25f);
                ju.lento(1.5f, 0.3f);
                ju.clarao(1f, 1f, 1f, 1f);
                ju.glitchar(1f);
                break;
            case VITORIA:
                au.vitoria();
                break;
            case DERROTA:
                au.derrota();
                ju.lento(1.2f, 0.25f);
                ju.clarao(0.8f, 1f, 0.1f, 0.1f);
                ju.glitchar(1f);
                break;
            case TRAVA:
                au.trava();
                pa.faiscas(ev.x, ev.y, Cores.CORAL, 10, 260f);
                break;
            case DESTRAVA:
                au.destrava();
                pa.faiscas(ev.x, ev.y, Cores.LIMA, 10, 260f);
                break;
            case GOSMA:
                au.gosma();
                pa.disco(ev.x, ev.y, Cores.VERDE, 30f, 0.5f);
                break;
            case DESFAZER:
                au.desfazer();
                ju.glitchar(1f);
                ju.clarao(0.5f, 0.5f, 0.6f, 1f);
                pa.anel(ev.x, ev.y, 120f, Cores.ROXO, 0.5f);
                break;
            case COPIAR:
                au.copiar();
                pa.anel(ev.x, ev.y, 50f, Cores.LIMA, 0.4f);
                break;
            case CLONE:
                au.clonar();
                pa.anel(ev.x, ev.y, 70f, Cores.CIANO, 0.5f);
                pa.explosao(ev.x, ev.y, Cores.CIANO, 20, 300f);
                break;
            case CLONE_ATAQUE:
                au.cloneAtaque();
                corte(ev.x, ev.y, ev.x2, ev.y2);
                break;
            case PUSH:
                au.empurrar();
                pa.anel(ev.x, ev.y, 300f, Cores.CIANO, 0.5f);
                ju.tremer(8f);
                break;
            case SUDO:
                au.sudo();
                ju.clarao(1f, 1f, 1f, 1f);
                ju.tremer(18f);
                ju.glitchar(1f);
                break;
            case ZONA_AVISO:
                au.zonaAviso();
                break;
            case ZONA_EXPLODE:
                au.zonaExplode();
                pa.explosao(ev.x, ev.y, Cores.VERMELHO, 30, 450f);
                ju.tremer(8f);
                break;
            case DESALINHO:
                ju.glitchar(0.6f);
                ju.tremer(4f);
                break;
            case COMBO_SOBE:
                au.comboSobe(ev.n);
                pa.numero("COMBO ×" + ev.n, ev.x, ev.y + 40f, Cores.AMARELO, 2);
                break;
            case NEGADO:
                au.negado();
                break;
            default:
                break;
        }
    }

    // ------------------------------------------------------------------ desenho
    @Override
    public void desenhar(Pincel p) {
        Estado e = sim.e;
        jogo.fundo.desenhar(p, jogo.fontes, tempo, 0.7f);
        jogo.usarCameraMundo(p);
        float alfaTec = 1f - 0.45f * termAnim;
        Teclado tc = sim.teclado;
        int alvo = -1;
        Caminho previa = null;
        if (tut != null && tut.fantasma >= 0) alvo = tut.fantasma;
        else if (segurando >= 0 && tSegurando >= HOLD_PREVIA) alvo = segurando;
        if (alvo >= 0) {
            previa = sim.preverDash(alvo);
            if (!previa.valido) previa = null;
        }
        if (tut == null) ArteTeclado.trepadeiras(p, tc, e.onda, tempo, alfaTec);
        for (Teclado.Tecla t : tc.teclas) {
            int i = t.indice;
            int par = t.tipo == Teclado.Tipo.DESTINO ? tc.parDe(i) : -1;
            float pulso = e.parTecla >= 0 && (i == e.parTecla || i == tc.parceira(e.parTecla)) ? 1f : 0f;
            ArteTeclado.tecla(p, jogo.fontes, t, sim.cx(i), sim.cy(i), tempo, brilho[i], e.trava[i], e.gosma[i], par, pulso,
                    previa != null && i == alvo, alfaTec);
        }
        // zonas de perigo do chefão
        for (Estado.Zona z : e.zonas) {
            float k = 0.18f + 0.12f * (float) Math.sin(tempo * 12f);
            p.cor(Cores.VERMELHO, k);
            p.disco(z.x, z.y, z.r);
            p.cor(Cores.VERMELHO, 0.9f);
            p.anel(z.x, z.y, z.r, 3f);
        }
        // prévia (linha-fantasma)
        if (previa != null) {
            p.aditivo(true);
            for (int i = 1; i < previa.n; i++) {
                p.cor(previa.acertos > 0 ? Cores.AMARELO : Cores.BRANCO, 0.55f);
                p.linha(previa.x[i - 1], previa.y[i - 1], previa.x[i], previa.y[i], 3f);
            }
            p.aditivo(false);
            ArteJogador.fantasma(p, previa.x[previa.n - 1], previa.y[previa.n - 1], dir, 1f);
            if (previa.acertos > 0) {
                Texto.neon(p, jogo.fontes.media, "×" + previa.acertos, previa.x[previa.n - 1], previa.y[previa.n - 1] + 40f,
                        Texto.CENTRO, Cores.AMARELO, 1f);
            }
        }
        if (e.chefe != null) ArteChefe.desenhar(p, jogo.fontes, e.chefe, tempo, e.fase == Estado.Fase.CHEFE_MORRENDO);
        for (Inimigo in : e.inimigos) ArteInimigos.desenhar(p, in, tempo);
        if (e.cloneAtivo) ArteJogador.ninja(p, e.cloneX, e.cloneY + 4f, 1f, 1f, tempo, true, false, Math.min(1f, e.cloneT));
        if (sim.copiaAtiva()) {
            int k = sim.copiaTeclaIndice();
            p.aditivo(true);
            p.cor(Cores.LIMA, 0.6f + 0.3f * (float) Math.sin(tempo * 10f));
            p.anel(sim.cx(k), sim.cy(k), 30f, 2.5f);
            p.aditivo(false);
        }
        float a = e.invul > 0f && ((int) (tempo * 16f)) % 2 == 0 ? 0.35f : 1f;
        if (sim.resultado != Simulacao.Resultado.DERROTA) ArteJogador.ninja(p, e.jx, e.jy + 4f, 1f, dir, tempo, false, false, a);
        // cortes do dash
        p.aditivo(true);
        for (Corte c : cortes) {
            float k = Math.max(0f, c.vida / 0.3f);
            for (int i = 1; i < c.n; i++) {
                p.cor(Cores.CIANO, 0.5f * k);
                p.linha(c.x[i - 1], c.y[i - 1], c.x[i], c.y[i], 10f * k + 2f);
                p.cor(Cores.BRANCO, k);
                p.linha(c.x[i - 1], c.y[i - 1], c.x[i], c.y[i], 3f * k + 1f);
            }
        }
        p.aditivo(false);
        jogo.particulas.desenhar(p, jogo.fontes);

        // ---- interface (sem tremor)
        jogo.usarCameraHud(p);
        hud.desenhar(p, jogo.fontes, sim, tempo, 1f, tut != null, tut != null ? tut.slotsVisiveis() : 4);
        if (terminalAberto) {
            Hud.terminal(p, jogo.fontes, termTexto.toString(), termRestante, tut == null ? TERM_LIMITE : TERM_LIMITE_TUTORIAL,
                    termAviso, tempo, termAnim);
        } else if (tut != null && tut.terminalDemo != null) {
            Hud.terminal(p, jogo.fontes, tut.terminalDemo, 1f, 1f, "", tempo, 1f);
        }
        if (tut != null) tut.desenhar(p, jogo.fontes, tempo);
        if (pausado) {
            p.cor(0f, 0.02f, 0.05f, 0.75f);
            p.reta(0f, 0f, 1280f, 720f);
            Texto.neon(p, jogo.fontes.tituloM, "PAUSADO", 640f, 520f, Texto.CENTRO, Cores.CIANO, 1f);
            for (Botao b : botoesPausa) b.desenhar(p, jogo.fontes.grande, tempo, 1f, 0f);
            Texto.desenhar(p, jogo.fontes.pequena, "Esc: retomar", 640f, 160f, Texto.CENTRO, Cores.CINZA, 1f);
        }
    }
}
