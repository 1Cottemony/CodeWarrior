package com.codewarrior.logica;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Random;

import com.codewarrior.logica.Estado.Fase;
import com.codewarrior.logica.Evento.Tipo;

/**
 * Coração do jogo: regras sem nenhuma dependência do libGDX (por isso é testável sem abrir janela).
 * A interface chama atualizar(dt) todo quadro, os comandos (dash, desfazer...) e lê "e" e "eventos".
 * IMPORTANTE: "e" pode ser trocado (Ctrl+Z), então a interface sempre lê sim.e de novo a cada quadro.
 */
public final class Simulacao {
    public enum Resultado { JOGANDO, VITORIA, DERROTA }

    private static final int[][] COMPOSICAO = {{6, 0, 0}, {6, 3, 0}, {4, 4, 2}, {8, 4, 3}, {10, 5, 4}};
    public static final float LIMITE_X0 = 24f, LIMITE_X1 = 1256f, LIMITE_Y0 = 24f, LIMITE_Y1 = 696f;

    public final Teclado teclado;
    public final Regras regras;
    public Estado e;
    public Resultado resultado = Resultado.JOGANDO;
    public final ArrayList<Evento> eventos = new ArrayList<>();

    private final Random rnd;
    private final float[] recarga = new float[Habilidade.values().length];
    private final ArrayList<Estado> historico = new ArrayList<>();
    private float tSnap;
    private int copiaTecla = -1;
    private float copiaT;
    private int bufIdx = -1;
    private float bufT;
    private final Caminho cam = new Caminho();
    private final float[] tmp = new float[2];

    public Simulacao(Regras regras, long semente) {
        this.regras = regras;
        this.rnd = new Random(semente);
        this.teclado = new Teclado();
        e = new Estado(teclado.teclas.length);
        e.coracoesMax = regras.coracoesMax();
        e.coracoes = e.coracoesMax;
        e.jTecla = teclado.indice("G");
        e.jx = teclado.teclas[e.jTecla].cx;
        e.jy = teclado.teclas[e.jTecla].cy;
        if (regras.tutorial) {
            e.fase = Fase.ONDA;
        } else {
            iniciarOnda(1);
        }
        historico.add(e.copiar());
    }

    // =====================================================================
    // utilidades
    // =====================================================================
    public float cx(int i) {
        return teclado.teclas[i].cx + e.offX[i];
    }

    public float cy(int i) {
        return teclado.teclas[i].cy + e.offY[i];
    }

    private void emit(Tipo t, float x, float y, int n) {
        eventos.add(new Evento(t, x, y, n));
    }

    private void emit2(Tipo t, float x, float y, int n, float x2, float y2) {
        eventos.add(new Evento(t, x, y, n, x2, y2));
    }

    public void limparEventos() {
        eventos.clear();
    }

    /** Índice da tecla sob o ponto considerando o desalinhamento atual, ou -1. */
    public int teclaEmAtual(float px, float py) {
        for (Teclado.Tecla t : teclado.teclas) {
            if (Math.abs(px - cx(t.indice)) <= t.pw / 2f && Math.abs(py - cy(t.indice)) <= t.ph / 2f) return t.indice;
        }
        return -1;
    }

    static float distSeg(float px, float py, float ax, float ay, float bx, float by) {
        float dx = bx - ax, dy = by - ay;
        float l2 = dx * dx + dy * dy;
        float t = l2 < 0.0001f ? 0f : ((px - ax) * dx + (py - ay) * dy) / l2;
        t = Math.max(0f, Math.min(1f, t));
        return (float) Math.hypot(px - (ax + dx * t), py - (ay + dy * t));
    }

    private static boolean noRetangulo(float px, float py, Chefe c, float margem) {
        return Math.abs(px - c.x) <= Chefe.LARGURA / 2f + margem && Math.abs(py - c.y) <= Chefe.ALTURA / 2f + margem;
    }

    private static float distRetangulo(float px, float py, Chefe c) {
        float dx = Math.max(Math.abs(px - c.x) - Chefe.LARGURA / 2f, 0f);
        float dy = Math.max(Math.abs(py - c.y) - Chefe.ALTURA / 2f, 0f);
        return (float) Math.hypot(dx, dy);
    }

    public int multiplicador() {
        return Math.min(Regras.MULT_MAX, 1 + e.cadeia / 3);
    }

    public float recargaRestante(Habilidade h) {
        return recarga[h.ordinal()];
    }

    public float recargaTotal(Habilidade h) {
        return h.base * regras.fatorRecarga();
    }

    public boolean copiaAtiva() {
        return copiaTecla >= 0;
    }

    public float copiaRestante() {
        return copiaTecla >= 0 ? copiaT : 0f;
    }

    public int copiaTeclaIndice() {
        return copiaTecla;
    }

    /** 0..1: quanto da onda já foi limpo (ou vida do chefão durante a luta). */
    public float progressoOnda() {
        Estado s = e;
        if (s.chefe != null) return s.chefe.hp / (float) Regras.CHEFE_PV;
        if (s.fase == Fase.INTERVALO) return 1f;
        if (s.totalOnda <= 0) return 0f;
        float restam = s.fila.size() + s.inimigos.size();
        return Math.max(0f, Math.min(1f, 1f - restam / s.totalOnda));
    }

    // =====================================================================
    // laço principal
    // =====================================================================
    public void atualizar(float dtTotal) {
        while (dtTotal > 1e-6f) {
            float dt = Math.min(dtTotal, 1f / 90f);
            dtTotal -= dt;
            passo(dt);
            if (resultado != Resultado.JOGANDO) break;
        }
    }

    private void passo(float dt) {
        Estado s = e;
        s.t += dt;
        for (int i = 0; i < recarga.length; i++) recarga[i] = Math.max(0f, recarga[i] - dt);
        if (copiaTecla >= 0) {
            copiaT -= dt;
            if (copiaT <= 0f) copiaTecla = -1;
        }
        if (bufIdx >= 0) {
            bufT -= dt;
            if (bufT <= 0f) bufIdx = -1;
        }
        s.tCadeia += dt;
        if (s.cadeia > 0 && s.tCadeia > Regras.COMBO_JANELA) s.cadeia = 0;
        s.invul = Math.max(0f, s.invul - dt);
        s.dashCd = Math.max(0f, s.dashCd - dt);
        if (s.parTecla >= 0) {
            s.parT -= dt;
            if (s.parT <= 0f) s.parTecla = -1;
        }
        atualizarDesalinhos(dt);
        atualizarGosmaETravas(dt);
        atualizarOnda(dt);
        atualizarJogador(dt);
        atualizarInimigos(dt);
        atualizarChefe(dt);
        atualizarClone(dt);
        atualizarZonas(dt);
        contatos();
        if (resultado == Resultado.JOGANDO) {
            tSnap += dt;
            if (tSnap >= 0.1f) {
                tSnap = 0f;
                historico.add(e.copiar());
                while (historico.size() > 26) historico.remove(0);
            }
        }
    }

    private void atualizarDesalinhos(float dt) {
        Estado s = e;
        java.util.Arrays.fill(s.offX, 0f);
        java.util.Arrays.fill(s.offY, 0f);
        Iterator<Estado.Desalinho> it = s.desalinhos.iterator();
        while (it.hasNext()) {
            Estado.Desalinho d = it.next();
            d.t += dt;
            if (d.t >= d.dur) {
                it.remove();
                continue;
            }
            float up = Math.min(1f, d.t / 0.4f);
            float down = Math.min(1f, (d.dur - d.t) / 0.5f);
            float m = Math.min(up, down);
            float env = m * m * (3f - 2f * m);
            for (Teclado.Tecla t : teclado.teclas) {
                if (t.fila == d.fila && !"ENTER".equals(t.id)) s.offX[t.indice] += d.dx * env;
            }
        }
    }

    private void atualizarGosmaETravas(float dt) {
        Estado s = e;
        for (int i = 0; i < s.gosma.length; i++) {
            if (s.gosma[i] > 0f) s.gosma[i] = Math.max(0f, s.gosma[i] - dt);
            if (s.trava[i] > 0f) {
                s.trava[i] -= dt;
                if (s.trava[i] <= 0f) {
                    s.trava[i] = 0f;
                    s.travaDono[i] = 0;
                }
            }
        }
    }

    // =====================================================================
    // ondas e chefão
    // =====================================================================
    private void iniciarOnda(int n) {
        Estado s = e;
        s.onda = n;
        s.fase = Fase.INTRO_ONDA;
        s.tFase = 0f;
        s.tOnda = 0f;
        s.fila.clear();
        int[] c = COMPOSICAO[n - 1];
        ArrayList<Inimigo.Tipo> lista = new ArrayList<>();
        for (int i = 0; i < c[0]; i++) lista.add(Inimigo.Tipo.SYNTAX);
        for (int i = 0; i < c[1]; i++) lista.add(Inimigo.Tipo.LEAK);
        for (int i = 0; i < c[2]; i++) lista.add(Inimigo.Tipo.LOOP);
        Collections.shuffle(lista, rnd);
        for (int i = 0; i < lista.size(); i++) s.adicionarSpawn(lista.get(i), (i % 3) * 4f);
        s.totalOnda = lista.size();
        emit(Tipo.ONDA_INICIO, 640f, 360f, n);
    }

    private void atualizarOnda(float dt) {
        if (regras.tutorial) return;
        Estado s = e;
        s.tFase += dt;
        switch (s.fase) {
            case INTRO_ONDA:
                if (s.tFase >= 1.6f) {
                    s.fase = Fase.ONDA;
                    s.tFase = 0f;
                    s.tOnda = 0f;
                }
                break;
            case ONDA:
                s.tOnda += dt;
                Iterator<Estado.Spawn> it = s.fila.iterator();
                while (it.hasNext()) {
                    Estado.Spawn sp = it.next();
                    if (sp.t <= s.tOnda) {
                        it.remove();
                        spawnar(sp.tipo);
                    }
                }
                if (s.fila.isEmpty() && s.inimigos.isEmpty()) fimDaOnda();
                break;
            case INTERVALO:
                if (s.tFase >= 3f) iniciarOnda(s.onda + 1);
                break;
            case CHEFE_INTRO:
                if (s.tFase >= 2.5f) {
                    s.fase = Fase.CHEFE;
                    s.tFase = 0f;
                    s.chefe.invul = 0f;
                }
                break;
            case CHEFE_MORRENDO:
                if (s.tFase >= 1.8f) {
                    s.fase = Fase.FIM;
                    resultado = Resultado.VITORIA;
                    emit(Tipo.VITORIA, 640f, 360f, 0);
                }
                break;
            default:
                break;
        }
    }

    private void fimDaOnda() {
        Estado s = e;
        emit(Tipo.ONDA_FIM, 640f, 360f, s.onda);
        if ((s.onda == 2 || s.onda == 4) && s.coracoes < s.coracoesMax) {
            s.coracoes++;
            emit(Tipo.CURA, s.jx, s.jy, s.coracoes);
        }
        if (s.onda >= Regras.ONDAS) {
            s.fase = Fase.CHEFE_INTRO;
            s.tFase = 0f;
            s.chefe = new Chefe();
            emit(Tipo.CHEFE_ENTRADA, 640f, 520f, 0);
        } else {
            s.fase = Fase.INTERVALO;
            s.tFase = 0f;
        }
    }

    private int escolherTecla(float distMin) {
        int k = -1;
        for (int tent = 0; tent < 24; tent++) {
            int c = rnd.nextInt(teclado.teclas.length);
            if (teclado.teclas[c].tipo != Teclado.Tipo.DESTINO) continue;
            k = c;
            if (Math.hypot(cx(c) - e.jx, cy(c) - e.jy) >= distMin) break;
        }
        return k < 0 ? teclado.indice("H") : k;
    }

    private Inimigo novo(Inimigo.Tipo tipo) {
        Inimigo in = new Inimigo();
        in.id = e.proxId++;
        in.tipo = tipo;
        in.semente = rnd.nextFloat() * 6.28f;
        in.spawn = 0.7f;
        in.tTranca = 1.5f + rnd.nextFloat() * 1.5f;
        in.raio = tipo == Inimigo.Tipo.SYNTAX ? 20f : tipo == Inimigo.Tipo.LEAK ? 26f : 24f;
        return in;
    }

    private void spawnar(Inimigo.Tipo tipo) {
        Inimigo in = novo(tipo);
        if (tipo == Inimigo.Tipo.LOOP) {
            float[] g = null;
            for (int tent = 0; tent < 12; tent++) {
                float[] c = teclado.gruposLoop[rnd.nextInt(teclado.gruposLoop.length)];
                g = c;
                if (Math.hypot(c[0] - e.jx, c[1] - e.jy) >= 260f) break;
            }
            in.orbCx = g[0];
            in.orbCy = g[1];
            in.orbR = g[2];
            in.orbAng = rnd.nextFloat() * 6.28f;
            in.orbVel = (rnd.nextBoolean() ? 1f : -1f) * 1.7f * regras.velInimigos();
            posicionarLoop(in);
        } else {
            int k = escolherTecla(260f);
            in.x = cx(k);
            in.y = cy(k);
        }
        e.inimigos.add(in);
        emit(Tipo.SURGIU, in.x, in.y, tipo.ordinal());
    }

    /** Usado pelo tutorial: bug parado e inofensivo sobre uma tecla. */
    public void spawnarManequim(Inimigo.Tipo tipo, String idTecla) {
        int k = teclado.indice(idTecla);
        Inimigo in = novo(tipo);
        in.x = cx(k);
        in.y = cy(k);
        in.parado = true;
        in.spawn = 0.4f;
        if (tipo == Inimigo.Tipo.LOOP) {
            in.orbCx = in.x;
            in.orbCy = in.y;
            in.orbR = 0f;
        }
        e.inimigos.add(in);
        emit(Tipo.SURGIU, in.x, in.y, tipo.ordinal());
    }

    /** Usado pelo tutorial: coloca o jogador numa tecla. */
    public void teleportar(String idTecla) {
        int k = teclado.indice(idTecla);
        e.jTecla = k;
        e.dashando = false;
        e.jx = cx(k);
        e.jy = cy(k);
        bufIdx = -1;
    }

    public void limparInimigos() {
        e.inimigos.clear();
        java.util.Arrays.fill(e.trava, 0f);
        java.util.Arrays.fill(e.gosma, 0f);
    }

    private void posicionarLoop(Inimigo in) {
        in.x = in.orbCx + (float) Math.cos(in.orbAng) * in.orbR + in.ox;
        in.y = in.orbCy + (float) Math.sin(in.orbAng) * in.orbR + in.oy;
        limitar(in);
    }

    private static void limitar(Inimigo in) {
        in.x = Math.max(LIMITE_X0, Math.min(LIMITE_X1, in.x));
        in.y = Math.max(LIMITE_Y0, Math.min(LIMITE_Y1, in.y));
    }

    // ----- chefão -----
    private void atualizarChefe(float dt) {
        Estado s = e;
        Chefe c = s.chefe;
        if (c == null) return;
        c.anim += dt;
        c.hitCd = Math.max(0f, c.hitCd - dt);
        c.parCd = Math.max(0f, c.parCd - dt);
        c.cloneCd = Math.max(0f, c.cloneCd - dt);
        c.flash = Math.max(0f, c.flash - dt * 3f);
        c.invul = Math.max(0f, c.invul - dt);
        if (s.fase == Fase.CHEFE_INTRO) {
            c.entrada = Math.min(1f, c.entrada + dt / 2.5f);
            float k = 1f - (1f - c.entrada) * (1f - c.entrada);
            c.x = 640f;
            c.y = 820f + (520f - 820f) * k;
            c.invul = 999f;
            return;
        }
        if (s.fase == Fase.CHEFE_MORRENDO) {
            c.x += (rnd.nextFloat() - 0.5f) * 8f;
            c.y -= dt * 20f;
            return;
        }
        if (s.fase != Fase.CHEFE) return;
        c.x = 640f + (float) Math.sin(c.anim * 0.6f) * 200f;
        c.y = 520f + (float) Math.sin(c.anim * 1.1f) * 12f;
        int nf = c.hp > 20 ? 1 : (c.hp > 10 ? 2 : 3);
        if (nf > c.fase) {
            c.fase = nf;
            c.invul = 1.2f;
            c.tDesalinho = 2.5f;
            c.tInvoca = 2f;
            c.tZona = 2f;
            emit(Tipo.CHEFE_FASE, c.x, c.y, nf);
        }
        if (c.invul > 0f) return;
        if (c.fase == 1) {
            c.tDesalinho -= dt;
            if (c.tDesalinho <= 0f) {
                c.tDesalinho = 5f;
                criarDesalinho();
            }
        } else if (c.fase == 2) {
            c.tInvoca -= dt;
            if (c.tInvoca <= 0f) {
                c.tInvoca = 6f;
                invocar(3);
            }
        } else {
            c.tZona -= dt;
            if (c.tZona <= 0f) {
                c.tZona = 3.5f;
                criarZonas();
            }
            c.tInvoca -= dt;
            if (c.tInvoca <= 0f) {
                c.tInvoca = 9f;
                invocar(2);
            }
        }
    }

    private void criarDesalinho() {
        Estado.Desalinho d = new Estado.Desalinho();
        d.fila = rnd.nextInt(Teclado.FILAS);
        d.dx = (rnd.nextBoolean() ? 1f : -1f) * (45f + rnd.nextFloat() * 30f);
        d.dur = 4.5f;
        e.desalinhos.add(d);
        emit(Tipo.DESALINHO, 640f, Teclado.OY + (d.fila + 0.5f) * Teclado.U, d.fila);
    }

    private void invocar(int n) {
        int vivos = 0;
        for (Inimigo in : e.inimigos) if (in.tipo == Inimigo.Tipo.SYNTAX) vivos++;
        for (int i = 0; i < n && vivos < 7; i++, vivos++) spawnar(Inimigo.Tipo.SYNTAX);
    }

    private void criarZonas() {
        Estado s = e;
        for (int i = 0; i < 2; i++) {
            Estado.Zona z = new Estado.Zona();
            if (i == 0) {
                z.x = s.jx + (rnd.nextFloat() - 0.5f) * 60f;
                z.y = s.jy + (rnd.nextFloat() - 0.5f) * 60f;
            } else {
                int k = escolherTecla(0f);
                z.x = cx(k);
                z.y = cy(k);
            }
            z.r = 110f;
            z.t = 1.2f;
            s.zonas.add(z);
            emit(Tipo.ZONA_AVISO, z.x, z.y, 0);
        }
    }

    private void atualizarZonas(float dt) {
        Estado s = e;
        Iterator<Estado.Zona> it = s.zonas.iterator();
        while (it.hasNext()) {
            Estado.Zona z = it.next();
            if (!z.explodiu) {
                z.t -= dt;
                if (z.t <= 0f) {
                    z.explodiu = true;
                    z.vis = 0.35f;
                    emit(Tipo.ZONA_EXPLODE, z.x, z.y, 0);
                    if (!s.dashando && Math.hypot(s.jx - z.x, s.jy - z.y) <= z.r) levarDano(z.x, z.y);
                }
            } else {
                z.vis -= dt;
                if (z.vis <= 0f) it.remove();
            }
        }
    }

    private boolean danoChefe(int n) {
        Chefe c = e.chefe;
        if (c == null || e.fase != Fase.CHEFE || c.invul > 0f) return false;
        c.hp = Math.max(0, c.hp - n);
        c.flash = 1f;
        emit(Tipo.CHEFE_DANO, c.x, c.y, n);
        if (c.hp <= 0) chefeMorreu();
        return true;
    }

    private void chefeMorreu() {
        Estado s = e;
        s.fase = Fase.CHEFE_MORRENDO;
        s.tFase = 0f;
        s.pontos += Regras.PONTOS_CHEFE * multiplicador();
        for (int i = s.inimigos.size() - 1; i >= 0; i--) matarInimigo(s.inimigos.get(i), false);
        s.zonas.clear();
        s.desalinhos.clear();
        emit(Tipo.CHEFE_MORTO, s.chefe.x, s.chefe.y, 0);
    }

    // =====================================================================
    // inimigos
    // =====================================================================
    private void atualizarInimigos(float dt) {
        Estado s = e;
        float vm = regras.velInimigos();
        for (int i = 0; i < s.inimigos.size(); i++) {
            Inimigo in = s.inimigos.get(i);
            in.anim += dt;
            in.flash = Math.max(0f, in.flash - dt * 4f);
            if (in.spawn > 0f) {
                in.spawn -= dt;
                if (in.tipo == Inimigo.Tipo.LOOP) posicionarLoop(in);
                continue;
            }
            if (in.quebradoT > 0f) {
                in.quebradoT -= dt;
                if (in.quebradoT <= 0f) in.quebrado = false;
            }
            float dec = (float) Math.exp(-3.2f * dt);
            if (in.tipo == Inimigo.Tipo.LOOP) {
                in.ox += in.vx * dt;
                in.oy += in.vy * dt;
            } else {
                in.x += in.vx * dt;
                in.y += in.vy * dt;
            }
            in.vx *= dec;
            in.vy *= dec;
            boolean atordoado = in.stun > 0f;
            if (atordoado) in.stun -= dt;
            if (in.tipo == Inimigo.Tipo.LOOP) {
                if (!atordoado && !in.parado) {
                    in.orbAng += in.orbVel * dt;
                    float d2 = (float) Math.exp(-3f * dt);
                    in.ox *= d2;
                    in.oy *= d2;
                }
                posicionarLoop(in);
                continue;
            }
            if (atordoado || in.parado) {
                limitar(in);
                continue;
            }
            if (in.tipo == Inimigo.Tipo.SYNTAX) {
                if (in.pouso > 0f) {
                    in.pouso -= dt;
                    if (in.pouso <= 0f) {
                        travar(in);
                        in.tTranca = 2.5f + rnd.nextFloat();
                    }
                } else {
                    in.tTranca -= dt;
                    if (in.tTranca <= 0f) in.pouso = 0.5f;
                    else perseguir(in, 150f * vm, dt);
                }
            } else {
                perseguir(in, 70f * vm, dt);
                int k = teclaEmAtual(in.x, in.y);
                if (k >= 0 && k != in.teclaAnterior) {
                    in.teclaAnterior = k;
                    s.gosma[k] = 6f;
                    emit(Tipo.GOSMA, cx(k), cy(k), k);
                }
            }
            limitar(in);
        }
    }

    private void perseguir(Inimigo in, float vel, float dt) {
        float dx = e.jx - in.x, dy = e.jy - in.y;
        float d = (float) Math.hypot(dx, dy);
        if (d < 8f) return;
        float nx = dx / d, ny = dy / d;
        float wob = (float) Math.sin(in.anim * 6f + in.semente) * 0.35f;
        in.x += (nx - ny * wob) * vel * dt;
        in.y += (ny + nx * wob) * vel * dt;
    }

    private void travar(Inimigo in) {
        int k = teclaEmAtual(in.x, in.y);
        if (k < 0 || teclado.teclas[k].tipo != Teclado.Tipo.DESTINO || e.trava[k] > 0f) return;
        e.trava[k] = 4f;
        e.travaDono[k] = in.id;
        emit(Tipo.TRAVA, cx(k), cy(k), k);
    }

    private boolean golpear(Inimigo in) {
        if (in.spawn > 0f) return false;
        if (in.tipo == Inimigo.Tipo.LOOP && !in.quebrado) {
            in.quebrado = true;
            in.quebradoT = 2f;
            in.flash = 1f;
            emit(Tipo.GOLPE_LOOP, in.x, in.y, 0);
            return false;
        }
        matarInimigo(in, true);
        return true;
    }

    private void matarInimigo(Inimigo in, boolean pontuar) {
        Estado s = e;
        s.inimigos.remove(in);
        for (int k = 0; k < s.trava.length; k++) {
            if (s.trava[k] > 0f && s.travaDono[k] == in.id) {
                s.trava[k] = 0f;
                s.travaDono[k] = 0;
                emit(Tipo.DESTRAVA, cx(k), cy(k), k);
            }
        }
        if (pontuar) {
            int antes = multiplicador();
            s.cadeia++;
            s.tCadeia = 0f;
            int m = multiplicador();
            long base = in.tipo == Inimigo.Tipo.SYNTAX ? Regras.PONTOS_SYNTAX
                    : in.tipo == Inimigo.Tipo.LEAK ? Regras.PONTOS_LEAK : Regras.PONTOS_LOOP;
            s.pontos += base * m;
            if (m > antes) emit(Tipo.COMBO_SOBE, in.x, in.y, m);
        }
        if (s.dashando) s.dashKills++;
        emit2(Tipo.MORTE_INIMIGO, in.x, in.y, in.tipo.ordinal(), pontuar ? 1f : 0f, 0f);
    }

    private void levarDano(float x, float y) {
        Estado s = e;
        if (regras.tutorial || s.invul > 0f || s.dashando || resultado != Resultado.JOGANDO) return;
        s.coracoes--;
        s.invul = Regras.INVUL_DANO;
        s.cadeia = 0;
        emit(Tipo.DANO_JOGADOR, s.jx, s.jy, s.coracoes);
        if (s.coracoes <= 0) {
            s.coracoes = 0;
            resultado = Resultado.DERROTA;
            emit(Tipo.DERROTA, s.jx, s.jy, 0);
        }
    }

    private void contatos() {
        Estado s = e;
        if (regras.tutorial || s.dashando || s.invul > 0f || resultado != Resultado.JOGANDO) return;
        for (Inimigo in : s.inimigos) {
            if (in.spawn > 0f || in.stun > 0f || in.parado) continue;
            if (Math.hypot(in.x - s.jx, in.y - s.jy) < in.raio + Regras.JOGADOR_RAIO) {
                levarDano(in.x, in.y);
                float dx = in.x - s.jx, dy = in.y - s.jy;
                float d = Math.max(1f, (float) Math.hypot(dx, dy));
                in.vx = dx / d * 600f;
                in.vy = dy / d * 600f;
                in.stun = 0.6f;
                return;
            }
        }
        Chefe c = s.chefe;
        if (c != null && s.fase == Fase.CHEFE && c.invul <= 0f && noRetangulo(s.jx, s.jy, c, Regras.JOGADOR_RAIO)) {
            levarDano(c.x, c.y);
        }
    }

    // =====================================================================
    // dash
    // =====================================================================
    /** Calcula o trajeto e a prévia (linha-fantasma). Não altera nada. */
    public Caminho preverDash(int idx) {
        Caminho c = new Caminho();
        c.destino = idx;
        if (resultado != Resultado.JOGANDO || idx < 0 || idx >= teclado.teclas.length) return c;
        Teclado.Tecla t = teclado.teclas[idx];
        if (t.tipo != Teclado.Tipo.DESTINO || e.trava[idx] > 0f) return c;
        if (!e.dashando && idx == e.jTecla) return c;
        if (e.fase == Fase.CHEFE_MORRENDO || e.fase == Fase.FIM) return c;
        c.valido = true;
        float sx = e.jx, sy = e.jy, tx = cx(idx), ty = cy(idx);
        c.x[0] = sx;
        c.y[0] = sy;
        c.x[1] = tx;
        c.y[1] = ty;
        c.n = 2;
        c.recalcularTotal();
        if (regras.miraAssistida) {
            Inimigo melhor = null;
            float melhorD = 1e9f;
            for (Inimigo in : e.inimigos) {
                if (in.spawn > 0f) continue;
                float d = distSeg(in.x, in.y, sx, sy, tx, ty);
                float lim = in.raio + Regras.DASH_RAIO;
                if (d > lim && d <= lim + Teclado.U && d < melhorD) {
                    melhorD = d;
                    melhor = in;
                }
            }
            if (melhor != null) {
                float l = (float) (Math.hypot(melhor.x - sx, melhor.y - sy) + Math.hypot(tx - melhor.x, ty - melhor.y));
                if (l <= c.total + 180f) {
                    c.x[1] = melhor.x;
                    c.y[1] = melhor.y;
                    c.x[2] = tx;
                    c.y[2] = ty;
                    c.n = 3;
                    c.desviou = true;
                    c.recalcularTotal();
                }
            }
        }
        // gosma no caminho
        for (float d = 0f; d <= c.total + 0.01f && !c.lento; d += 20f) {
            c.pontoEm(d, tmp);
            int k = teclaEmAtual(tmp[0], tmp[1]);
            if (k >= 0 && e.gosma[k] > 0f) c.lento = true;
        }
        // acertos previstos
        for (Inimigo in : e.inimigos) {
            if (in.spawn > 0f) continue;
            float dmin = 1e9f;
            for (int i = 1; i < c.n; i++) dmin = Math.min(dmin, distSeg(in.x, in.y, c.x[i - 1], c.y[i - 1], c.x[i], c.y[i]));
            if (dmin <= in.raio + Regras.DASH_RAIO) c.acertos++;
        }
        Chefe ch = e.chefe;
        if (ch != null && e.fase == Fase.CHEFE && ch.invul <= 0f) {
            for (float d = 0f; d <= c.total + 0.01f; d += 12f) {
                c.pontoEm(d, tmp);
                if (noRetangulo(tmp[0], tmp[1], ch, Regras.DASH_RAIO)) {
                    c.acertos++;
                    break;
                }
            }
        }
        return c;
    }

    /** Pede um dash. Devolve true se começou ou ficou guardado no buffer (aperto logo antes da recarga acabar). */
    public boolean dash(int idx) {
        if (resultado != Resultado.JOGANDO) return false;
        Caminho c = preverDash(idx);
        if (!c.valido) return false;
        Estado s = e;
        if (s.dashando || s.dashCd > 0f) {
            float espera = s.dashando ? Math.max((s.dashTotal - s.dashPos) / Math.max(1f, s.dashVel), s.dashCd) : s.dashCd;
            if (espera <= 0.2f) {
                bufIdx = idx;
                bufT = espera + 0.05f;
                return true;
            }
            return false;
        }
        iniciarDash(idx);
        return true;
    }

    public boolean podeDash() {
        return resultado == Resultado.JOGANDO && !e.dashando && e.dashCd <= 0f;
    }

    private void iniciarDash(int idx) {
        Caminho c = preverDash(idx);
        if (!c.valido) return;
        Estado s = e;
        s.dashando = true;
        for (int i = 0; i < 3; i++) {
            s.cx[i] = c.x[i];
            s.cy[i] = c.y[i];
        }
        s.nPontos = c.n;
        s.dashTotal = c.total;
        s.dashPos = 0f;
        float vel = Regras.DASH_VEL * (c.lento ? 0.5f : 1f);
        if (c.total / vel < 0.07f) vel = Math.max(1f, c.total / 0.07f);
        s.dashVel = vel;
        s.dashDestino = idx;
        s.dashKills = 0;
        s.nAcertados = 0;
        s.dashCd = Regras.DASH_RECARGA;
        emit2(Tipo.DASH_INICIO, c.x[0], c.y[0], c.lento ? 1 : 0, c.x[c.n - 1], c.y[c.n - 1]);
    }

    private void carregarCaminho() {
        Estado s = e;
        cam.n = s.nPontos;
        for (int i = 0; i < 3; i++) {
            cam.x[i] = s.cx[i];
            cam.y[i] = s.cy[i];
        }
        cam.total = s.dashTotal;
    }

    private void atualizarJogador(float dt) {
        Estado s = e;
        if (!s.dashando) {
            float k = 1f - (float) Math.exp(-25f * dt);
            s.jx += (cx(s.jTecla) - s.jx) * k;
            s.jy += (cy(s.jTecla) - s.jy) * k;
            if (bufIdx >= 0 && s.dashCd <= 0f) {
                int b = bufIdx;
                bufIdx = -1;
                iniciarDash(b);
            }
            return;
        }
        carregarCaminho();
        float antes = s.dashPos;
        s.dashPos = Math.min(s.dashTotal, s.dashPos + s.dashVel * dt);
        for (float d = antes; d < s.dashPos; d += 12f) {
            cam.pontoEm(d, tmp);
            amostrarDash(tmp[0], tmp[1]);
        }
        cam.pontoEm(s.dashPos, tmp);
        s.jx = tmp[0];
        s.jy = tmp[1];
        amostrarDash(tmp[0], tmp[1]);
        if (s.dashPos >= s.dashTotal - 0.001f) terminarDash();
    }

    private boolean jaAcertado(int id) {
        for (int i = 0; i < e.nAcertados; i++) if (e.dashAcertados[i] == id) return true;
        return false;
    }

    private void amostrarDash(float px, float py) {
        Estado s = e;
        for (int i = s.inimigos.size() - 1; i >= 0; i--) {
            if (i >= s.inimigos.size()) continue;
            Inimigo in = s.inimigos.get(i);
            if (in.spawn > 0f || jaAcertado(in.id)) continue;
            if (Math.hypot(in.x - px, in.y - py) <= in.raio + Regras.DASH_RAIO) {
                if (s.nAcertados < s.dashAcertados.length) s.dashAcertados[s.nAcertados++] = in.id;
                golpear(in);
            }
        }
        Chefe c = s.chefe;
        if (c != null && s.fase == Fase.CHEFE && c.invul <= 0f && c.hitCd <= 0f && noRetangulo(px, py, c, Regras.DASH_RAIO)) {
            c.hitCd = 0.9f;
            danoChefe(1);
        }
    }

    private void terminarDash() {
        Estado s = e;
        s.dashando = false;
        s.jTecla = s.dashDestino;
        s.jx = cx(s.jTecla);
        s.jy = cy(s.jTecla);
        emit(Tipo.DASH_FIM, s.jx, s.jy, s.dashKills);
        verificarPar(s.jTecla);
    }

    private void verificarPar(int k) {
        int p = teclado.parDe(k);
        if (p < 0) return;
        if (e.parTecla >= 0 && teclado.parceira(e.parTecla) == k) {
            explosaoPar(e.jx, e.jy);
            e.parTecla = -1;
            e.parT = 0f;
        } else {
            e.parTecla = k;
            e.parT = Regras.PAR_JANELA;
            emit(Tipo.PAR_ARMADO, e.jx, e.jy, p);
        }
    }

    private void explosaoPar(float x, float y) {
        Estado s = e;
        emit(Tipo.EXPLOSAO_PAR, x, y, 0);
        for (int i = s.inimigos.size() - 1; i >= 0; i--) {
            if (i >= s.inimigos.size()) continue;
            Inimigo in = s.inimigos.get(i);
            if (in.spawn > 0f) continue;
            if (Math.hypot(in.x - x, in.y - y) <= Regras.PAR_RAIO + in.raio) golpear(in);
        }
        Chefe c = s.chefe;
        if (c != null && s.fase == Fase.CHEFE && c.parCd <= 0f && distRetangulo(x, y, c) <= Regras.PAR_RAIO) {
            if (danoChefe(3)) c.parCd = 5f;
        }
    }

    // =====================================================================
    // clone
    // =====================================================================
    private void atualizarClone(float dt) {
        Estado s = e;
        if (!s.cloneAtivo) return;
        s.cloneT -= dt;
        if (s.cloneT <= 0f) {
            s.cloneAtivo = false;
            return;
        }
        s.cloneAtk -= dt;
        if (s.cloneAtk > 0f) return;
        Inimigo alvo = null;
        float melhor = Regras.CLONE_ALCANCE;
        for (Inimigo in : s.inimigos) {
            if (in.spawn > 0f) continue;
            float d = (float) Math.hypot(in.x - s.cloneX, in.y - s.cloneY) - in.raio;
            if (d <= melhor) {
                melhor = d;
                alvo = in;
            }
        }
        Chefe c = s.chefe;
        if (alvo != null) {
            emit2(Tipo.CLONE_ATAQUE, s.cloneX, s.cloneY, 0, alvo.x, alvo.y);
            s.cloneAtk = Regras.CLONE_INTERVALO;
            golpear(alvo);
        } else if (c != null && s.fase == Fase.CHEFE && c.invul <= 0f && c.cloneCd <= 0f
                && distRetangulo(s.cloneX, s.cloneY, c) <= Regras.CLONE_ALCANCE) {
            emit2(Tipo.CLONE_ATAQUE, s.cloneX, s.cloneY, 0, c.x, c.y);
            s.cloneAtk = Regras.CLONE_INTERVALO;
            c.cloneCd = 1.5f;
            danoChefe(1);
        } else {
            s.cloneAtk = 0.1f;
        }
    }

    // =====================================================================
    // habilidades
    // =====================================================================
    public boolean habilidadePronta(Habilidade h) {
        return resultado == Resultado.JOGANDO && recarga[h.ordinal()] <= 0f;
    }

    private void usar(Habilidade h) {
        recarga[h.ordinal()] = recargaTotal(h);
    }

    private boolean negar() {
        emit(Tipo.NEGADO, e.jx, e.jy, 0);
        return false;
    }

    /** Ctrl+Z: volta 2 s no tempo (posição, corações, inimigos, pontuação). As recargas não voltam. */
    public boolean desfazer() {
        if (!habilidadePronta(Habilidade.DESFAZER) || historico.isEmpty()) return negar();
        float alvo = e.t - Regras.DESFAZER_SEG;
        Estado esc = null;
        for (Estado h : historico) {
            if (h.t <= alvo) esc = h;
            else break;
        }
        if (esc == null) esc = historico.get(0);
        while (historico.size() > 1 && historico.get(historico.size() - 1).t > esc.t) historico.remove(historico.size() - 1);
        e = esc.copiar();
        e.invul = Math.max(e.invul, 1f);
        tSnap = 0f;
        bufIdx = -1;
        usar(Habilidade.DESFAZER);
        emit(Tipo.DESFAZER, e.jx, e.jy, 0);
        return true;
    }

    /** Ctrl+C: marca a tecla onde você está. */
    public boolean copiar() {
        if (resultado != Resultado.JOGANDO) return false;
        copiaTecla = e.dashando ? e.dashDestino : e.jTecla;
        copiaT = Regras.COPIA_JANELA;
        emit(Tipo.COPIAR, cx(copiaTecla), cy(copiaTecla), copiaTecla);
        return true;
    }

    /** Ctrl+V (até 5 s depois do Ctrl+C): cola um clone holográfico na tecla copiada. */
    public boolean colar() {
        if (copiaTecla < 0 || !habilidadePronta(Habilidade.COPIAR_COLAR)) return negar();
        Estado s = e;
        s.cloneAtivo = true;
        s.cloneX = cx(copiaTecla);
        s.cloneY = cy(copiaTecla);
        s.cloneT = Regras.CLONE_DURACAO;
        s.cloneAtk = 0.3f;
        copiaTecla = -1;
        usar(Habilidade.COPIAR_COLAR);
        emit(Tipo.CLONE, s.cloneX, s.cloneY, 0);
        return true;
    }

    /** git push: empurra todos os bugs para longe e os atordoa por 1,5 s. */
    public boolean gitPush() {
        if (!habilidadePronta(Habilidade.GIT_PUSH)) return negar();
        Estado s = e;
        for (Inimigo in : s.inimigos) {
            if (in.spawn > 0f) continue;
            float dx = in.x - s.jx, dy = in.y - s.jy;
            float d = (float) Math.hypot(dx, dy);
            if (d < 1f) {
                dx = rnd.nextFloat() - 0.5f;
                dy = rnd.nextFloat() - 0.5f;
                d = Math.max(0.1f, (float) Math.hypot(dx, dy));
            }
            in.vx = dx / d * 1300f;
            in.vy = dy / d * 1300f;
            in.stun = 1.5f;
        }
        usar(Habilidade.GIT_PUSH);
        emit(Tipo.PUSH, s.jx, s.jy, 0);
        return true;
    }

    /** sudo rm -rf: apaga todos os bugs da tela e tira 6 PV do chefão. */
    public boolean sudoRmRf() {
        if (!habilidadePronta(Habilidade.SUDO)) return negar();
        Estado s = e;
        usar(Habilidade.SUDO);
        emit(Tipo.SUDO, s.jx, s.jy, s.inimigos.size());
        for (int i = s.inimigos.size() - 1; i >= 0; i--) {
            if (i < s.inimigos.size()) matarInimigo(s.inimigos.get(i), true);
        }
        danoChefe(6);
        return true;
    }

    /** Ajuda para a interface: o comando digitado no terminal corresponde a qual habilidade (ou null). */
    public static Habilidade habilidadeDoComando(String texto) {
        if (texto == null) return null;
        String t = texto.trim().toLowerCase().replaceAll("\\s+", " ");
        if (t.equals("git push")) return Habilidade.GIT_PUSH;
        if (t.equals("sudo rm -rf") || t.equals("sudo rm -rf /") || t.equals("sudo rm -rf *")) return Habilidade.SUDO;
        return null;
    }

    public boolean executarComando(Habilidade h) {
        if (h == Habilidade.GIT_PUSH) return gitPush();
        if (h == Habilidade.SUDO) return sudoRmRf();
        return false;
    }
}
