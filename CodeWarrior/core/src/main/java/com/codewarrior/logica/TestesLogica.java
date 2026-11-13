package com.codewarrior.logica;

import java.util.HashSet;
import java.util.Set;

/**
 * Testes automáticos da lógica (sem janela, sem libGDX, sem JUnit).
 * Rodar:  gradlew.bat core:testarLogica
 */
public final class TestesLogica {
    private static int ok = 0, falhas = 0;

    private static void check(String nome, boolean cond) {
        if (cond) {
            ok++;
            System.out.println("  OK   " + nome);
        } else {
            falhas++;
            System.out.println("  FALHA " + nome);
        }
    }

    private static Simulacao novo(boolean tutorial) {
        Regras r = new Regras();
        r.tutorial = tutorial;
        r.miraAssistida = false;
        return new Simulacao(r, 42L);
    }

    private static Inimigo mk(Simulacao s, Inimigo.Tipo t, float x, float y) {
        Inimigo in = new Inimigo();
        in.id = s.e.proxId++;
        in.tipo = t;
        in.x = x;
        in.y = y;
        in.raio = 20f;
        in.tTranca = 99f;
        s.e.inimigos.add(in);
        return in;
    }

    private static Inimigo manequim(Simulacao s, Inimigo.Tipo t, String tecla) {
        s.spawnarManequim(t, tecla);
        s.atualizar(0.5f);
        return s.e.inimigos.get(s.e.inimigos.size() - 1);
    }

    private static int idx(Simulacao s, String id) {
        return s.teclado.indice(id);
    }

    private static boolean temEvento(Simulacao s, Evento.Tipo t) {
        for (Evento ev : s.eventos) if (ev.tipo == t) return true;
        return false;
    }

    public static void main(String[] args) {
        teclado();
        regrasEDificuldade();
        dashEMira();
        inimigos();
        pares();
        combo();
        habilidades();
        ondas();
        chefao();
        determinismo();
        System.out.println();
        System.out.println(ok + " verificações OK, " + falhas + " falhas.");
        if (falhas > 0) System.exit(1);
    }

    // ------------------------------------------------------------------
    static void teclado() {
        System.out.println("[Teclado ABNT2]");
        Teclado t = new Teclado();
        Set<String> ids = new HashSet<>();
        boolean unico = true;
        float maxFim = 0;
        for (Teclado.Tecla k : t.teclas) {
            unico &= ids.add(k.id);
            maxFim = Math.max(maxFim, k.x + k.w);
        }
        check("63 teclas desenhadas, ids únicos", t.teclas.length == 63 && unico);
        check("53 teclas-destino", t.contarDestinos() == 53);
        check("nenhuma tecla passa de 15 unidades de largura", maxFim <= 15.001f);
        check("3 pares de combo", t.pares.length == 3);
        check("par 9<->0", t.parceira(t.indice("NUM_9")) == t.indice("NUM_0"));
        check("par ,<->.", t.parceira(t.indice("COMMA")) == t.indice("PERIOD"));
        check("par [<->]", t.parceira(t.indice("RIGHT_BRACKET")) == t.indice("BACKSLASH"));
        check("Ctrl e Enter são reservadas", t.teclas[t.indice("CONTROL_LEFT")].tipo == Teclado.Tipo.RESERVADA
                && t.teclas[t.indice("ENTER")].tipo == Teclado.Tipo.RESERVADA);
        check("Alt/AltGr/Win ficam indisponíveis", t.teclas[t.indice("ALT_LEFT")].tipo == Teclado.Tipo.INDISPONIVEL
                && t.teclas[t.indice("SUPER_LEFT")].tipo == Teclado.Tipo.INDISPONIVEL);
        int g = t.indice("G");
        check("teclaEm encontra a tecla pelo centro", t.teclaEm(t.teclas[g].cx, t.teclas[g].cy) == g);
        boolean cabe = true;
        for (Teclado.Tecla k : t.teclas) {
            cabe &= k.cx - k.pw / 2f >= 0 && k.cx + k.pw / 2f <= 1280 && k.cy - k.ph / 2f >= 100 && k.cy + k.ph / 2f <= 560;
        }
        check("teclado cabe na tela 1280x720 sem invadir HUD", cabe);
        check("grupos de loop existem", t.gruposLoop.length >= 5 && t.gruposLoop[0][2] > 40f);
    }

    // ------------------------------------------------------------------
    static void regrasEDificuldade() {
        System.out.println("[Regras e dificuldade]");
        Regras r = new Regras();
        r.dificuldade = Dificuldade.FACIL;
        check("Fácil: 7 corações", r.coracoesMax() == 7);
        r.dificuldade = Dificuldade.NORMAL;
        check("Normal: 5 corações", r.coracoesMax() == 5);
        r.dificuldade = Dificuldade.DIFICIL;
        check("Difícil: 3 corações", r.coracoesMax() == 3);
        r.facilitador = true;
        check("Facilitador soma +2 corações", r.coracoesMax() == 5);
        check("Facilitador deixa inimigos 25% mais lentos", Math.abs(r.velInimigos() - 1.2f * 0.75f) < 1e-5f);
        check("Facilitador reduz recargas em 30%", Math.abs(r.fatorRecarga() - 1.15f * 0.7f) < 1e-5f);
        Simulacao s = new Simulacao(r, 1L);
        check("Simulação começa com os corações certos", s.e.coracoes == 5 && s.e.coracoesMax == 5);
        check("Recarga do sudo no Normal = 90 s", Math.abs(novo(false).recargaTotal(Habilidade.SUDO) - 90f) < 1e-4f);
    }

    // ------------------------------------------------------------------
    static void dashEMira() {
        System.out.println("[Dash, prévia e mira assistida]");
        Simulacao s = novo(true);
        int G = idx(s, "G"), J = idx(s, "J"), H = idx(s, "H");
        check("começa em G", s.e.jTecla == G);
        check("dash em tecla reservada é recusado", !s.dash(idx(s, "CONTROL_LEFT")));
        check("dash em tecla indisponível é recusado", !s.dash(idx(s, "WORLD_1")));
        check("dash na própria tecla é recusado", !s.dash(G));
        Inimigo m = manequim(s, Inimigo.Tipo.SYNTAX, "H");
        Caminho c = s.preverDash(J);
        check("prévia mostra 1 acerto no caminho", c.valido && c.acertos == 1);
        check("dash começa", s.dash(J));
        s.atualizar(1f);
        check("dash chega ao destino", s.e.jTecla == J && !s.e.dashando);
        check("Syntax Error no caminho foi deletado (+100)", !s.e.inimigos.contains(m) && s.e.pontos == 100);

        // recarga e buffer
        Simulacao s2 = novo(true);
        s2.dash(idx(s2, "J"));
        s2.atualizar(0.1f);
        check("dash durante recarga longa é recusado", !s2.dash(idx(s2, "A")));
        s2.atualizar(0.2f); // restam ~0,1 s de recarga -> entra no buffer
        check("aperto logo antes da recarga acabar entra no buffer", s2.dash(idx(s2, "A")));
        s2.atualizar(0.5f);
        check("dash guardado no buffer é executado", s2.e.jTecla == idx(s2, "A"));

        // mira assistida
        Regras r = new Regras();
        r.tutorial = true;
        r.miraAssistida = false;
        Simulacao a = new Simulacao(r, 5L);
        manequim(a, Inimigo.Tipo.SYNTAX, "U");
        a.dash(idx(a, "J"));
        a.atualizar(1f);
        check("sem mira assistida: bug a 1 tecla do caminho não é pego", a.e.inimigos.size() == 1);
        r.miraAssistida = true;
        Simulacao b = new Simulacao(r, 5L);
        manequim(b, Inimigo.Tipo.SYNTAX, "U");
        Caminho cb = b.preverDash(idx(b, "J"));
        check("com mira assistida a prévia desvia e prevê o acerto", cb.desviou && cb.acertos == 1 && cb.n == 3);
        b.dash(idx(b, "J"));
        b.atualizar(1f);
        check("com mira assistida o bug próximo é deletado", b.e.inimigos.isEmpty() && b.e.jTecla == idx(b, "J"));

        // trava e gosma
        Simulacao t = novo(true);
        t.e.trava[idx(t, "J")] = 4f;
        check("tecla trancada não pode ser destino", !t.dash(idx(t, "J")));
        check("tecla trancada pode ser atravessada", t.dash(idx(t, "K")));
        t.atualizar(1f);
        check("atravessou a tecla trancada", t.e.jTecla == idx(t, "K"));
        t.e.trava[idx(t, "J")] = 0.05f;
        t.atualizar(0.2f);
        check("tranca expira", t.e.trava[idx(t, "J")] == 0f);

        Simulacao g1 = novo(true), g2 = novo(true);
        g2.e.gosma[H] = 6f;
        Caminho lento = g2.preverDash(J), normal = g1.preverDash(J);
        check("caminho por tecla com gosma é marcado como lento", lento.lento && !normal.lento);
        g1.dash(J);
        g2.dash(J);
        check("dash na gosma é 50% mais lento (velocidade menor)", g2.e.dashVel < g1.e.dashVel * 0.8f);
        Simulacao g3 = novo(true);
        g3.e.gosma[J] = 6f;
        check("gosma só no destino também desacelera", g3.preverDash(J).lento);
        check("gosma só na origem (tecla atual) também desacelera", novo(true).preverDash(J).lento == false);
        Simulacao g4 = novo(true);
        g4.e.gosma[G] = 6f;
        check("gosma na tecla de saída desacelera", g4.preverDash(J).lento);

        // prévia com 2 inimigos
        Simulacao p = novo(true);
        manequim(p, Inimigo.Tipo.SYNTAX, "H");
        p.spawnarManequim(Inimigo.Tipo.LEAK, "K");
        p.atualizar(0.5f);
        check("prévia conta 2 bugs", p.preverDash(idx(p, "L")).acertos == 2);
        p.dash(idx(p, "L"));
        p.atualizar(1f);
        check("um dash deleta os 2 bugs (+100 +150, combo ainda ×1 para o 2º)", p.e.inimigos.isEmpty() && p.e.pontos == 250);
    }

    // ------------------------------------------------------------------
    static void inimigos() {
        System.out.println("[Inimigos]");
        // Infinite Loop precisa de 2 acertos
        Simulacao s = novo(true);
        Inimigo loop = manequim(s, Inimigo.Tipo.LOOP, "H");
        s.dash(idx(s, "J"));
        s.atualizar(0.6f);
        check("Loop sobrevive ao 1º acerto e fica 'quebrado'", s.e.inimigos.contains(loop) && loop.quebrado && s.e.pontos == 0);
        s.dash(idx(s, "G"));
        s.atualizar(0.6f);
        check("Loop morre ao 2º acerto em até 2 s (+300)", s.e.inimigos.isEmpty() && s.e.pontos == 300);
        Simulacao s2 = novo(true);
        Inimigo loop2 = manequim(s2, Inimigo.Tipo.LOOP, "H");
        s2.dash(idx(s2, "J"));
        s2.atualizar(2.5f);
        check("se passar de 2 s o ciclo se refaz", !loop2.quebrado && s2.e.inimigos.contains(loop2));
        s2.dash(idx(s2, "G"));
        s2.atualizar(0.6f);
        check("...e o próximo acerto conta como o primeiro de novo", s2.e.inimigos.contains(loop2) && loop2.quebrado);

        // contato e invulnerabilidade
        Simulacao c = novo(false);
        c.atualizar(0.05f);
        Inimigo bug = mk(c, Inimigo.Tipo.SYNTAX, c.e.jx + 10f, c.e.jy);
        c.atualizar(0.05f);
        check("tocar num bug tira 1 coração", c.e.coracoes == c.e.coracoesMax - 1);
        check("depois do dano há invulnerabilidade", c.e.invul > 1.3f);
        mk(c, Inimigo.Tipo.SYNTAX, c.e.jx + 5f, c.e.jy);
        c.atualizar(0.5f);
        check("durante a invulnerabilidade não leva mais dano", c.e.coracoes == c.e.coracoesMax - 1);
        check("bug que causou o dano é empurrado e atordoado", bug.stun > 0f || !c.e.inimigos.contains(bug));
        Simulacao d = novo(false);
        d.e.coracoes = 1;
        d.atualizar(0.05f);
        mk(d, Inimigo.Tipo.LEAK, d.e.jx, d.e.jy);
        d.atualizar(0.05f);
        check("acabaram os corações = derrota", d.resultado == Simulacao.Resultado.DERROTA && temEvento(d, Evento.Tipo.DERROTA));
        Simulacao dd = novo(false);
        dd.atualizar(0.05f);
        dd.dash(idx(dd, "J"));
        dd.atualizar(0.01f);
        mk(dd, Inimigo.Tipo.SYNTAX, dd.e.jx, dd.e.jy);
        dd.atualizar(0.005f);
        check("durante o dash o jogador é invulnerável", dd.e.coracoes == dd.e.coracoesMax);
        Simulacao tu = novo(true);
        mk(tu, Inimigo.Tipo.SYNTAX, tu.e.jx, tu.e.jy);
        tu.atualizar(0.2f);
        check("no tutorial não há dano", tu.e.coracoes == tu.e.coracoesMax);

        // Syntax Error tranca tecla; deletá-lo destranca
        Simulacao sy = novo(false);
        sy.e.invul = 999f;
        sy.atualizar(0.05f);
        Inimigo se = mk(sy, Inimigo.Tipo.SYNTAX, sy.cx(idx(sy, "A")), sy.cy(idx(sy, "A")));
        se.tTranca = 0.01f;
        sy.atualizar(0.7f);
        int travada = -1;
        for (int i = 0; i < sy.e.trava.length; i++) if (sy.e.trava[i] > 0f && sy.e.travaDono[i] == se.id) travada = i;
        check("Syntax Error tranca a tecla onde pousa por 4 s", travada >= 0 && sy.e.trava[travada] > 3f);
        sy.e.inimigos.remove(se);
        // simula a morte pelo dash: coloca o bug no caminho
        Inimigo se2 = mk(sy, Inimigo.Tipo.SYNTAX, sy.cx(idx(sy, "H")), sy.cy(idx(sy, "H")));
        se2.parado = true;
        sy.e.trava[idx(sy, "Q")] = 4f;
        sy.e.travaDono[idx(sy, "Q")] = se2.id;
        sy.e.jTecla = idx(sy, "G");
        sy.atualizar(0.5f);
        sy.dash(idx(sy, "J"));
        sy.atualizar(0.6f);
        check("deletar o Syntax Error destranca as teclas dele na hora", sy.e.trava[idx(sy, "Q")] == 0f);

        // Memory Leak deixa gosma
        Simulacao ml = novo(false);
        ml.e.invul = 999f;
        ml.atualizar(0.05f);
        mk(ml, Inimigo.Tipo.LEAK, ml.cx(idx(ml, "T")), ml.cy(idx(ml, "T")));
        ml.atualizar(1f);
        int gosmas = 0;
        for (float gsm : ml.e.gosma) if (gsm > 0f) gosmas++;
        check("Memory Leak deixa gosma nas teclas por onde passa", gosmas >= 1);
        ml.atualizar(7f);
        check("a gosma some depois de 6 s (sem novos rastros longe)", ml.e.gosma[idx(ml, "T")] == 0f || ml.e.inimigos.size() == 1);

        // Loop orbita
        Simulacao lo = novo(false);
        lo.e.invul = 999f;
        Inimigo l = mk(lo, Inimigo.Tipo.LOOP, 0, 0);
        float[] gr = lo.teclado.gruposLoop[0];
        l.orbCx = gr[0];
        l.orbCy = gr[1];
        l.orbR = gr[2];
        l.orbVel = 1.7f;
        lo.atualizar(0.5f);
        float x1 = l.x, y1 = l.y;
        lo.atualizar(0.5f);
        float dist = (float) Math.hypot(l.x - gr[0], l.y - gr[1]);
        check("Infinite Loop circula em volta do grupo de teclas", Math.abs(dist - gr[2]) < 2f && (x1 != l.x || y1 != l.y));
    }

    // ------------------------------------------------------------------
    static void pares() {
        System.out.println("[Combo de pares]");
        Regras r = new Regras();
        r.tutorial = true;
        r.miraAssistida = false;
        Simulacao s = new Simulacao(r, 9L);
        manequim(s, Inimigo.Tipo.SYNTAX, "P");
        s.dash(idx(s, "NUM_9"));
        s.atualizar(0.6f);
        check("pousar na 1ª tecla do par arma o combo", s.e.parTecla == idx(s, "NUM_9"));
        s.limparEventos();
        s.dash(idx(s, "NUM_0"));
        s.atualizar(0.6f);
        check("pousar na outra tecla causa explosão em área", temEvento(s, Evento.Tipo.EXPLOSAO_PAR));
        check("a explosão deleta o bug próximo", s.e.inimigos.isEmpty());

        Simulacao s2 = new Simulacao(r, 9L);
        s2.dash(idx(s2, "NUM_0"));
        s2.atualizar(0.6f);
        s2.limparEventos();
        s2.dash(idx(s2, "NUM_9"));
        s2.atualizar(0.6f);
        check("o par vale em qualquer ordem", temEvento(s2, Evento.Tipo.EXPLOSAO_PAR));

        Simulacao s3 = new Simulacao(r, 9L);
        s3.dash(idx(s3, "COMMA"));
        s3.atualizar(4f);
        s3.limparEventos();
        s3.dash(idx(s3, "PERIOD"));
        s3.atualizar(0.6f);
        check("passou de 3 s: não explode (só arma de novo)", !temEvento(s3, Evento.Tipo.EXPLOSAO_PAR) && s3.e.parTecla == idx(s3, "PERIOD"));

        Simulacao s4 = new Simulacao(r, 9L);
        s4.dash(idx(s4, "RIGHT_BRACKET"));
        s4.atualizar(0.6f);
        s4.dash(idx(s4, "BACKSLASH"));
        s4.atualizar(0.6f);
        check("par [ ] funciona", temEvento(s4, Evento.Tipo.EXPLOSAO_PAR));

        Simulacao s5 = new Simulacao(r, 9L);
        Inimigo lp = manequim(s5, Inimigo.Tipo.LOOP, "P");
        s5.dash(idx(s5, "NUM_9"));
        s5.atualizar(0.6f);
        s5.dash(idx(s5, "NUM_0"));
        s5.atualizar(0.6f);
        check("explosão conta como 1 golpe no Loop", s5.e.inimigos.contains(lp) && lp.quebrado);
    }

    // ------------------------------------------------------------------
    static void combo() {
        System.out.println("[Combo e pontuação]");
        Simulacao s = novo(true);
        check("multiplicador começa em ×1", s.multiplicador() == 1);
        s.e.cadeia = 2;
        manequim(s, Inimigo.Tipo.SYNTAX, "H");
        s.dash(idx(s, "J"));
        s.atualizar(0.6f);
        check("3ª deleção seguida sobe o multiplicador para ×2 (100×2)", s.multiplicador() == 2 && s.e.pontos == 200);
        s.atualizar(2.5f);
        check("2 s sem deletar zera o combo", s.e.cadeia == 0 && s.multiplicador() == 1);
        s.e.cadeia = 100;
        check("multiplicador limitado a ×5", s.multiplicador() == 5);
        Simulacao k = novo(true);
        k.e.cadeia = 11;
        manequim(k, Inimigo.Tipo.LOOP, "H");
        k.dash(idx(k, "J"));
        k.atualizar(0.6f);
        k.dash(idx(k, "G"));
        k.atualizar(0.6f);
        check("pontos: Loop vale 300 × multiplicador", k.e.pontos == 300L * 5);
    }

    // ------------------------------------------------------------------
    static void habilidades() {
        System.out.println("[Habilidades]");
        // Ctrl+Z
        Simulacao s = novo(true);
        s.spawnarManequim(Inimigo.Tipo.SYNTAX, "H");
        s.atualizar(1.5f);
        s.dash(idx(s, "J"));
        s.atualizar(1f);
        check("(preparo) bug deletado e pontuação > 0", s.e.inimigos.isEmpty() && s.e.pontos == 100 && s.e.jTecla == idx(s, "J"));
        Estado antes = s.e;
        check("Ctrl+Z funciona", s.desfazer());
        check("Ctrl+Z traz o bug de volta", s.e.inimigos.size() == 1);
        check("Ctrl+Z restaura pontuação e posição", s.e.pontos == 0 && s.e.jTecla == idx(s, "G"));
        check("Ctrl+Z troca o estado (interface relê sim.e)", s.e != antes);
        check("a recarga NÃO volta no tempo (20 s)", Math.abs(s.recargaRestante(Habilidade.DESFAZER) - 20f) < 0.01f);
        check("segundo Ctrl+Z durante a recarga é negado", !s.desfazer());
        s.atualizar(10f);
        check("recarga do Ctrl+Z diminui com o tempo", s.recargaRestante(Habilidade.DESFAZER) < 11f);
        // Ctrl+Z tira da armadilha: corações voltam
        Simulacao z = novo(false);
        z.atualizar(3f);
        z.e.invul = 0f;
        mk(z, Inimigo.Tipo.SYNTAX, z.e.jx, z.e.jy);
        z.atualizar(0.1f);
        check("(preparo) levou dano", z.e.coracoes == z.e.coracoesMax - 1);
        z.desfazer();
        check("Ctrl+Z devolve o coração", z.e.coracoes == z.e.coracoesMax);
        check("Ctrl+Z dá invulnerabilidade curta para escapar", z.e.invul > 0.5f);

        // Ctrl+C / Ctrl+V
        Simulacao c = novo(true);
        check("Ctrl+V sem Ctrl+C é negado", !c.colar());
        c.copiar();
        c.dash(idx(c, "Q"));
        c.atualizar(0.6f);
        manequim(c, Inimigo.Tipo.SYNTAX, "H");
        check("Ctrl+V cola o clone na tecla COPIADA (não onde você está)", c.colar() && c.e.cloneAtivo
                && Math.abs(c.e.cloneX - c.cx(idx(c, "G"))) < 0.01f);
        c.atualizar(1.2f);
        check("clone deleta bug por perto sozinho", c.e.inimigos.isEmpty() && c.e.pontos >= 100);
        check("Ctrl+C/V recarrega 25 s", Math.abs(c.recargaRestante(Habilidade.COPIAR_COLAR) - (25f - 1.2f)) < 0.2f);
        c.atualizar(8.5f);
        check("clone some depois de 8 s", !c.e.cloneAtivo);
        Simulacao c2 = novo(true);
        c2.copiar();
        c2.atualizar(5.5f);
        check("passou de 5 s: Ctrl+V não vale", !c2.colar());

        // git push
        Simulacao g = novo(true);
        Inimigo b = manequim(g, Inimigo.Tipo.SYNTAX, "H");
        float x0 = b.x;
        check("git push funciona", g.gitPush());
        g.atualizar(0.5f);
        check("git push empurra os bugs para longe de você", b.x > x0 + 100f);
        check("...e os atordoa por 1,5 s", b.stun > 0.5f);
        check("git push recarrega 30 s", Math.abs(g.recargaRestante(Habilidade.GIT_PUSH) - (30f - 0.5f)) < 0.1f);
        check("git push em recarga é negado", !g.gitPush());

        // sudo
        Simulacao u = novo(false);
        u.e.invul = 999f;
        u.atualizar(0.05f);
        for (int i = 0; i < 5; i++) mk(u, Inimigo.Tipo.values()[i % 3], 100 + i * 90, 600);
        check("sudo rm -rf funciona", u.sudoRmRf());
        check("sudo deleta TODOS os bugs (inclusive Loop)", u.e.inimigos.isEmpty() && u.e.pontos > 0);
        check("sudo recarrega 90 s", Math.abs(u.recargaRestante(Habilidade.SUDO) - 90f) < 0.01f);
        check("sudo em recarga é negado", !u.sudoRmRf());

        // comandos digitados
        check("comando 'git push'", Simulacao.habilidadeDoComando("git push") == Habilidade.GIT_PUSH);
        check("comando com espaços e maiúsculas", Simulacao.habilidadeDoComando("  GIT   Push ") == Habilidade.GIT_PUSH);
        check("comando 'sudo rm -rf'", Simulacao.habilidadeDoComando("sudo rm -rf") == Habilidade.SUDO);
        check("comando 'sudo rm -rf /'", Simulacao.habilidadeDoComando("sudo rm -rf /") == Habilidade.SUDO);
        check("comando errado não vale", Simulacao.habilidadeDoComando("git pus") == null && Simulacao.habilidadeDoComando("") == null);
    }

    // ------------------------------------------------------------------
    static void ondas() {
        System.out.println("[Ondas]");
        int[][] esperado = {{6, 0, 0}, {6, 3, 0}, {4, 4, 2}, {8, 4, 3}, {10, 5, 4}};
        Simulacao s = novo(false);
        boolean tudoCerto = true;
        boolean curaOk = true;
        int onda = 1;
        int[] cont = new int[3];
        float t = 0;
        while (onda <= 5 && t < 400f) {
            s.e.invul = 999f;
            s.atualizar(1f / 60f);
            t += 1f / 60f;
            for (Evento ev : s.eventos) if (ev.tipo == Evento.Tipo.SURGIU) cont[ev.n]++;
            s.limparEventos();
            if (s.e.fase == Estado.Fase.ONDA && s.e.fila.isEmpty() && s.e.onda == onda) {
                for (int i = 0; i < 3; i++) tudoCerto &= cont[i] == esperado[onda - 1][i];
                if (onda == 2 || onda == 4) s.e.coracoes = 2;
                int antes = s.e.coracoes;
                s.e.inimigos.clear();
                s.atualizar(1f / 60f);
                if (onda == 2 || onda == 4) curaOk &= s.e.coracoes == antes + 1;
                else curaOk &= s.e.coracoes == antes;
                cont = new int[3];
                onda++;
            }
        }
        check("composição das 5 ondas confere (Syntax/Leak/Loop)", tudoCerto && onda == 6);
        check("recupera 1 coração após as ondas 2 e 4 (e só nelas)", curaOk);
        check("depois da onda 5 entra o chefão", s.e.fase == Estado.Fase.CHEFE_INTRO && s.e.chefe != null);
        check("chefão começa com 30 PV e invulnerável na entrada", s.e.chefe.hp == 30 && s.e.chefe.invul > 0f);
        s.e.invul = 999f;
        s.atualizar(3f);
        check("após a entrada a luta começa", s.e.fase == Estado.Fase.CHEFE && s.e.chefe.invul < 0.01f);

        Simulacao w = novo(false);
        w.e.invul = 999f;
        w.atualizar(1.7f);
        check("onda 1: 1,6 s de introdução e depois os bugs surgem", w.e.fase == Estado.Fase.ONDA);
        w.atualizar(0.1f);
        check("1º lote surge de imediato (2 de 6)", w.e.inimigos.size() == 2);
        w.atualizar(4f);
        check("2º lote depois de ~4 s", w.e.inimigos.size() == 4);
        float pr = w.progressoOnda();
        check("progresso da onda entre 0 e 1", pr >= 0f && pr <= 1f);
    }

    // ------------------------------------------------------------------
    static Simulacao arenaChefe() {
        Simulacao s = novo(false);
        s.e.invul = 999f;
        s.e.fase = Estado.Fase.CHEFE;
        s.e.onda = 5;
        s.e.chefe = new Chefe();
        s.e.chefe.invul = 0f;
        s.e.chefe.x = 640f;
        s.e.chefe.y = 520f;
        s.e.chefe.tDesalinho = 99f;
        s.e.chefe.tInvoca = 99f;
        s.e.chefe.tZona = 99f;
        s.e.jTecla = s.teclado.indice("NUM_1");
        s.atualizar(0.1f);
        return s;
    }

    static void chefao() {
        System.out.println("[Chefão: Div Desalinhada]");
        Simulacao s = arenaChefe();
        s.dash(idx(s, "NUM_0"));
        s.atualizar(1f);
        check("dash que atravessa o chefão causa 1 PV (e só 1 por golpe)", s.e.chefe.hp == 29);
        s.atualizar(0.6f);
        s.dash(idx(s, "NUM_1"));
        s.atualizar(1f);
        check("novo dash causa mais 1 PV", s.e.chefe.hp == 28);

        Simulacao p = arenaChefe();
        p.dash(idx(p, "NUM_9"));
        p.atualizar(1.2f);
        int antes = p.e.chefe.hp;
        p.dash(idx(p, "NUM_0"));
        p.atualizar(0.6f);
        check("explosão de par causa 3 PV no chefão (além do dash)", antes - p.e.chefe.hp >= 3);

        // anti-abuso: repetir o par seguido não funciona
        p.dash(idx(p, "NUM_9"));
        p.atualizar(0.6f);
        int hp2 = p.e.chefe.hp;
        p.dash(idx(p, "NUM_0"));
        p.atualizar(0.6f);
        check("2ª explosão de par logo em seguida não machuca o chefão (recarga de 5 s)", p.e.chefe.hp >= hp2 - 1);

        // anti-abuso: encostar no corpo do chefão machuca
        Simulacao ct = arenaChefe();
        ct.e.chefe.invul = 0f;
        ct.e.invul = 0f;
        ct.teleportar("NUM_5");
        int cor = ct.e.coracoes;
        ct.atualizar(0.1f);
        check("ficar parado dentro do corpo do chefão causa dano", ct.e.coracoes == cor - 1);

        // anti-abuso: dash de volta ao chefão tem intervalo
        Simulacao dd = arenaChefe();
        dd.dash(idx(dd, "NUM_0"));
        dd.atualizar(0.3f);
        int h0 = dd.e.chefe.hp;
        dd.dash(idx(dd, "NUM_1"));
        dd.atualizar(0.3f);
        check("dashes colados no chefão não acertam duas vezes em 0,9 s", dd.e.chefe.hp == h0);

        Simulacao u = arenaChefe();
        u.e.chefe.hp = 30;
        u.sudoRmRf();
        check("sudo rm -rf tira 6 PV (não mata de uma vez)", u.e.chefe.hp == 24 && u.e.fase == Estado.Fase.CHEFE);

        Simulacao f = arenaChefe();
        f.e.chefe.hp = 21;
        f.sudoRmRf();
        f.atualizar(0.05f);
        check("com ≤20 PV entra na fase 2", f.e.chefe.fase == 2 && f.e.chefe.invul > 1f);
        int hp = f.e.chefe.hp;
        f.copiar();
        f.dash(idx(f, "NUM_0"));
        f.atualizar(0.5f);
        check("na troca de fase o chefão fica intocável por ~1,2 s", f.e.chefe.hp == hp);
        f.e.chefe.tInvoca = 0.01f;
        f.atualizar(2f);
        int syntax = 0;
        for (Inimigo in : f.e.inimigos) if (in.tipo == Inimigo.Tipo.SYNTAX) syntax++;
        check("fase 2 invoca levas de Syntax Errors", syntax >= 3);

        Simulacao d = arenaChefe();
        d.e.chefe.tDesalinho = 0.01f;
        d.atualizar(1f);
        boolean mexeu = false;
        for (float o : d.e.offX) mexeu |= Math.abs(o) > 10f;
        check("fase 1 desalinha o mapa (teclas deslocadas)", mexeu && !d.e.desalinhos.isEmpty());
        d.e.chefe.tDesalinho = 99f;
        d.atualizar(6f);
        boolean voltou = true;
        for (float o : d.e.offX) voltou &= Math.abs(o) < 0.01f;
        check("...e o mapa realinha depois", voltou);

        Simulacao z = arenaChefe();
        z.e.chefe.hp = 10;
        z.e.chefe.fase = 3;
        z.e.chefe.tZona = 0.01f;
        z.limparEventos();
        z.atualizar(0.1f);
        check("fase 3 avisa zonas vermelhas", temEvento(z, Evento.Tipo.ZONA_AVISO) && z.e.zonas.size() == 2);
        z.limparEventos();
        z.atualizar(1.0f);
        check("zonas ainda não explodiram antes de 1,2 s", !temEvento(z, Evento.Tipo.ZONA_EXPLODE));
        z.atualizar(0.4f);
        check("zonas explodem depois do aviso de 1,2 s", temEvento(z, Evento.Tipo.ZONA_EXPLODE));

        Simulacao v = arenaChefe();
        v.e.chefe.hp = 1;
        v.e.chefe.fase = 3;
        v.dash(idx(v, "NUM_0"));
        v.atualizar(0.6f);
        check("PV zerado: chefão morre e começa a animação final", v.e.fase == Estado.Fase.CHEFE_MORRENDO && v.e.pontos >= 5000);
        check("ainda não é vitória antes da animação acabar", v.resultado == Simulacao.Resultado.JOGANDO);
        v.atualizar(2f);
        check("vitória ao fim da animação", v.resultado == Simulacao.Resultado.VITORIA && temEvento(v, Evento.Tipo.VITORIA));
    }

    // ------------------------------------------------------------------
    static void determinismo() {
        System.out.println("[Determinismo]");
        long[] r = new long[2];
        for (int k = 0; k < 2; k++) {
            Simulacao s = new Simulacao(new Regras(), 777L);
            s.e.invul = 999f;
            for (int i = 0; i < 600; i++) {
                s.atualizar(1f / 60f);
                if (i % 40 == 0) s.dash(s.teclado.indice(i % 80 == 0 ? "Q" : "M"));
                if (i == 500) s.sudoRmRf();
            }
            r[k] = s.e.pontos * 1000L + s.e.inimigos.size();
        }
        check("mesma semente + mesmos comandos = mesmo resultado", r[0] == r[1]);
        Simulacao s = new Simulacao(new Regras(), 3L);
        s.atualizar(10f);
        check("simulação roda 10 s de partida sem erro", s.e.t > 9.9f);
    }
}
