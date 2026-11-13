package com.codewarrior.audio;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;

/**
 * Áudio 100% sintetizado por código (PCM em tempo real, sem arquivos de som).
 * Uma thread própria gera amostras e envia ao AudioDevice. Os efeitos entram numa fila e são misturados com a música.
 * A música é chiptune/synthwave em camadas: base (baixo + pad) -> +bateria -> +arpejo (chefão).
 */
public final class Sintetizador {
    private static final int SR = 44100;
    private static final int BUF = 512;
    private static final int SENO = 0, QUADRADA = 1, SERRA = 2, RUIDO = 3, TRIANGULO = 4;
    private static final int[] PENTA = {0, 2, 4, 7, 9};

    private static final class Voz {
        int onda;
        float f0, f1, dur, vol, atq = 0.003f, pot = 2f;
        float atraso;
        float t, fase, lp;
        float filtro = 1f;
    }

    private AudioDevice dispositivo;
    private Thread thread;
    private volatile boolean rodando;
    private final ConcurrentLinkedQueue<Voz> fila = new ConcurrentLinkedQueue<>();
    private final ArrayList<Voz> vozes = new ArrayList<>();
    private final ArrayList<Voz> vozesMusica = new ArrayList<>();
    private final Random rnd = new Random(7);

    public volatile float volSom = 0.8f;
    public volatile float volMusica = 0.6f;
    /** 0 = só base, 1 = base + bateria, 2 = base + bateria + arpejo. */
    public volatile int camadas = 0;
    public volatile boolean musicaLigada = false;
    /** 1 = andamento normal; 0.5 = câmera lenta (terminal). */
    public volatile float andamento = 1f;
    public volatile float abafar = 0f;
    private final boolean disponivel;

    // estado da música (somente na thread de áudio)
    private float gBase, gBateria, gArp;
    private double acumPasso;
    private int passo;

    public Sintetizador() {
        boolean ok = true;
        try {
            dispositivo = Gdx.audio.newAudioDevice(SR, true);
        } catch (Throwable t) {
            ok = false;
            Gdx.app.error("Audio", "Sem dispositivo de áudio: " + t);
        }
        disponivel = ok;
        if (ok) {
            rodando = true;
            thread = new Thread(this::laco, "sintetizador-codewarrior");
            thread.setDaemon(true);
            thread.start();
        }
    }

    public boolean funcionando() {
        return disponivel;
    }

    public void dispose() {
        rodando = false;
        if (thread != null) {
            try {
                thread.join(300);
            } catch (InterruptedException ignorada) {
                Thread.currentThread().interrupt();
            }
        }
        if (dispositivo != null) {
            try {
                dispositivo.dispose();
            } catch (Throwable ignorada) {
                // nada a fazer ao encerrar
            }
        }
    }

    // =====================================================================
    // thread de áudio
    // =====================================================================
    private void laco() {
        short[] saida = new short[BUF];
        try {
            while (rodando) {
                gerar(saida);
                dispositivo.writeSamples(saida, 0, BUF);
            }
        } catch (Throwable t) {
            Gdx.app.error("Audio", "Thread de áudio encerrada: " + t);
        }
    }

    private void gerar(short[] saida) {
        Voz nova;
        while ((nova = fila.poll()) != null) {
            if (vozes.size() >= 28) vozes.remove(0);
            vozes.add(nova);
        }
        float sv = volSom, mv = volMusica;
        float velMus = andamento;
        float alvoBase = musicaLigada ? 1f : 0f;
        float alvoBat = musicaLigada && camadas >= 1 ? 1f : 0f;
        float alvoArp = musicaLigada && camadas >= 2 ? 1f : 0f;
        double amostrasPorPasso = SR * 60.0 / 124.0 / 4.0;
        for (int i = 0; i < BUF; i++) {
            gBase += (alvoBase - gBase) * 0.00012f;
            gBateria += (alvoBat - gBateria) * 0.00012f;
            gArp += (alvoArp - gArp) * 0.00012f;
            acumPasso += velMus;
            if (acumPasso >= amostrasPorPasso) {
                acumPasso -= amostrasPorPasso;
                tocarPasso(passo++);
            }
            float s = 0f;
            for (int k = vozes.size() - 1; k >= 0; k--) {
                Voz v = vozes.get(k);
                float x = amostra(v);
                if (v.t >= v.dur + v.atraso) vozes.remove(k);
                s += x;
            }
            float m = 0f;
            for (int k = vozesMusica.size() - 1; k >= 0; k--) {
                Voz v = vozesMusica.get(k);
                float x = amostra(v);
                if (v.t >= v.dur + v.atraso) vozesMusica.remove(k);
                m += x;
            }
            float mix = s * sv * 0.9f + m * mv * 0.8f * (1f - abafar * 0.5f);
            mix = (float) Math.tanh(mix);
            saida[i] = (short) (mix * 30000f);
        }
    }

    private float amostra(Voz v) {
        v.t += 1f / SR;
        float tt = v.t - v.atraso;
        if (tt < 0f) return 0f;
        float prog = Math.min(1f, tt / v.dur);
        float f = v.f0 * (float) Math.pow(v.f1 / v.f0, prog);
        v.fase += f / SR;
        v.fase -= (int) v.fase;
        float s;
        switch (v.onda) {
            case SENO:
                s = (float) Math.sin(v.fase * 6.2831853f);
                break;
            case QUADRADA:
                s = v.fase < 0.5f ? 0.6f : -0.6f;
                break;
            case SERRA:
                s = (v.fase * 2f - 1f) * 0.7f;
                break;
            case TRIANGULO:
                s = (v.fase < 0.5f ? v.fase * 4f - 1f : 3f - v.fase * 4f);
                break;
            default:
                float r = rnd.nextFloat() * 2f - 1f;
                float k = v.filtro * (0.15f + 0.85f * (1f - prog));
                v.lp += (r - v.lp) * k;
                s = v.lp * 1.6f;
                break;
        }
        float env = Math.min(1f, tt / v.atq) * (float) Math.pow(1f - prog, v.pot);
        return s * env * v.vol;
    }

    // ---------------------------------------------------------------- música
    private Voz mv(int onda, float f0, float f1, float dur, float vol, float pot) {
        Voz v = new Voz();
        v.onda = onda;
        v.f0 = f0;
        v.f1 = f1;
        v.dur = dur;
        v.vol = vol;
        v.pot = pot;
        return v;
    }

    private static float hz(int midi) {
        return 440f * (float) Math.pow(2.0, (midi - 69) / 12.0);
    }

    private void addMus(Voz v) {
        if (vozesMusica.size() < 40) vozesMusica.add(v);
    }

    private static final int[] RAIZ = {33, 29, 36, 31};
    private static final int[][] ACORDE = {{57, 60, 64}, {53, 57, 60}, {60, 64, 67}, {55, 59, 62}};

    private void tocarPasso(int p) {
        int s16 = p % 16;
        int compasso = (p / 16) % 4;
        int raiz = RAIZ[compasso];
        int[] ac = ACORDE[compasso];
        // baixo: colcheias, com salto de oitava em alguns tempos
        if (gBase > 0.02f && s16 % 2 == 0) {
            int nota = raiz + ((s16 == 6 || s16 == 14) ? 12 : 0);
            addMus(mv(SERRA, hz(nota), hz(nota) * 0.98f, 0.20f, 0.20f * gBase, 1.6f));
            addMus(mv(QUADRADA, hz(nota - 12), hz(nota - 12), 0.18f, 0.10f * gBase, 1.8f));
        }
        // pad: um acorde por compasso
        if (gBase > 0.02f && s16 == 0) {
            for (int n : ac) {
                Voz v = mv(SERRA, hz(n), hz(n) * 1.002f, 1.9f, 0.045f * gBase, 0.6f);
                v.atq = 0.25f;
                addMus(v);
                Voz w = mv(TRIANGULO, hz(n + 12), hz(n + 12) * 0.999f, 1.9f, 0.04f * gBase, 0.6f);
                w.atq = 0.3f;
                addMus(w);
            }
        }
        // bateria
        if (gBateria > 0.02f) {
            if (s16 % 4 == 0) addMus(mv(SENO, 130f, 42f, 0.16f, 0.50f * gBateria, 1.4f));
            if (s16 == 4 || s16 == 12) {
                Voz sn = mv(RUIDO, 1f, 1f, 0.14f, 0.30f * gBateria, 1.5f);
                sn.filtro = 0.9f;
                addMus(sn);
                addMus(mv(TRIANGULO, 210f, 150f, 0.07f, 0.15f * gBateria, 2f));
            }
            if (s16 % 2 == 1) {
                Voz h = mv(RUIDO, 1f, 1f, 0.035f, 0.10f * gBateria, 1f);
                h.filtro = 1f;
                addMus(h);
            }
        }
        // arpejo (chefão)
        if (gArp > 0.02f) {
            int[] ordem = {0, 1, 2, 1, 0, 1, 2, 2};
            int nota = ac[ordem[s16 % 8]] + 12 + ((s16 / 8) % 2 == 1 ? 12 : 0);
            addMus(mv(QUADRADA, hz(nota), hz(nota), 0.11f, 0.09f * gArp, 1.6f));
        }
    }

    // =====================================================================
    // efeitos sonoros (podem ser chamados de qualquer thread)
    // =====================================================================
    private Voz v(int onda, float f0, float f1, float dur, float vol) {
        Voz v = new Voz();
        v.onda = onda;
        v.f0 = f0;
        v.f1 = f1;
        v.dur = dur;
        v.vol = vol;
        return v;
    }

    private void tocar(Voz v, float atraso) {
        if (!disponivel) return;
        v.atraso = atraso;
        fila.add(v);
    }

    private void tocar(Voz v) {
        tocar(v, 0f);
    }

    private Voz ruido(float dur, float vol, float filtro) {
        Voz r = v(RUIDO, 1f, 1f, dur, vol);
        r.filtro = filtro;
        return r;
    }

    private static float nota(int semitons) {
        return 392f * (float) Math.pow(2.0, semitons / 12.0);
    }

    /** Cada dash toca uma nota da escala pentatônica; sobe de tom conforme o combo. */
    public void dash(int combo) {
        int idx = Math.max(0, Math.min(12, combo));
        float f = nota(12 * (idx / 5) + PENTA[idx % 5]);
        tocar(v(QUADRADA, f, f * 1.5f, 0.14f, 0.20f));
        tocar(v(SENO, f * 2f, f * 2f, 0.10f, 0.08f));
        tocar(ruido(0.10f, 0.10f, 0.6f));
    }

    public void matar(int tipo) {
        if (tipo == 0) {
            tocar(v(QUADRADA, 700f, 140f, 0.12f, 0.24f));
            tocar(ruido(0.07f, 0.12f, 0.9f));
        } else if (tipo == 1) {
            tocar(v(SENO, 320f, 70f, 0.20f, 0.30f));
            tocar(ruido(0.12f, 0.14f, 0.35f));
        } else {
            tocar(v(TRIANGULO, 520f, 1040f, 0.10f, 0.22f));
            tocar(v(QUADRADA, 1040f, 260f, 0.16f, 0.18f), 0.08f);
        }
    }

    public void golpeLoop() {
        tocar(v(TRIANGULO, 900f, 500f, 0.10f, 0.25f));
        tocar(ruido(0.06f, 0.15f, 1f));
    }

    public void dano() {
        tocar(v(SERRA, 240f, 55f, 0.38f, 0.34f));
        tocar(ruido(0.28f, 0.28f, 0.5f));
    }

    public void explosaoPar() {
        tocar(ruido(0.65f, 0.40f, 0.5f));
        tocar(v(SENO, 150f, 35f, 0.55f, 0.50f));
        float[] acorde = {523f, 659f, 784f, 1047f};
        for (int i = 0; i < acorde.length; i++) tocar(v(QUADRADA, acorde[i], acorde[i], 0.22f, 0.12f), 0.04f * i);
    }

    public void parArmado() {
        tocar(v(TRIANGULO, 1200f, 1500f, 0.10f, 0.16f));
    }

    public void comboSobe(int mult) {
        float f = 600f * (float) Math.pow(2.0, mult / 6.0);
        tocar(v(QUADRADA, f, f * 1.25f, 0.12f, 0.14f));
        tocar(v(QUADRADA, f * 1.5f, f * 1.5f, 0.14f, 0.12f), 0.07f);
    }

    public void ondaInicio() {
        float[] n = {330f, 440f, 660f};
        for (int i = 0; i < n.length; i++) tocar(v(QUADRADA, n[i], n[i], 0.16f, 0.16f), 0.09f * i);
    }

    public void ondaFim() {
        float[] n = {523f, 659f, 784f};
        for (int i = 0; i < n.length; i++) tocar(v(TRIANGULO, n[i], n[i], 0.45f, 0.20f), 0.06f * i);
    }

    public void cura() {
        tocar(v(TRIANGULO, 880f, 880f, 0.14f, 0.20f));
        tocar(v(TRIANGULO, 1320f, 1320f, 0.30f, 0.20f), 0.1f);
    }

    public void chefeEntrada() {
        tocar(v(SERRA, 80f, 38f, 1.4f, 0.40f));
        tocar(ruido(1.2f, 0.25f, 0.2f));
    }

    public void chefeDano() {
        tocar(v(SERRA, 170f, 60f, 0.26f, 0.30f));
        tocar(ruido(0.14f, 0.20f, 0.8f));
    }

    public void chefeFase() {
        tocar(v(SERRA, 90f, 40f, 0.95f, 0.42f));
        tocar(ruido(0.8f, 0.30f, 0.3f));
        tocar(v(QUADRADA, 330f, 110f, 0.6f, 0.15f), 0.1f);
    }

    public void chefeMorto() {
        tocar(ruido(1.6f, 0.50f, 0.35f));
        tocar(v(SERRA, 400f, 30f, 1.6f, 0.40f));
        for (int i = 0; i < 5; i++) tocar(ruido(0.22f, 0.30f, 0.8f), 0.18f * i);
    }

    public void trava() {
        tocar(v(QUADRADA, 900f, 300f, 0.07f, 0.20f));
        tocar(v(QUADRADA, 200f, 120f, 0.12f, 0.20f), 0.06f);
    }

    public void destrava() {
        tocar(v(QUADRADA, 400f, 1000f, 0.09f, 0.15f));
    }

    public void gosma() {
        tocar(ruido(0.10f, 0.10f, 0.25f));
        tocar(v(SENO, 160f, 90f, 0.10f, 0.12f));
    }

    public void desfazer() {
        tocar(v(SENO, 200f, 1700f, 0.50f, 0.30f));
        tocar(v(SERRA, 100f, 800f, 0.50f, 0.14f));
    }

    public void copiar() {
        tocar(v(TRIANGULO, 880f, 880f, 0.09f, 0.20f));
    }

    public void clonar() {
        float[] n = {660f, 880f, 1320f};
        for (int i = 0; i < n.length; i++) tocar(v(TRIANGULO, n[i], n[i] * 1.01f, 0.25f, 0.18f), 0.05f * i);
    }

    public void cloneAtaque() {
        tocar(v(QUADRADA, 1500f, 700f, 0.06f, 0.11f));
    }

    public void empurrar() {
        tocar(ruido(0.55f, 0.32f, 0.5f));
        tocar(v(SENO, 95f, 40f, 0.45f, 0.35f));
    }

    public void sudo() {
        tocar(v(SENO, 60f, 900f, 0.70f, 0.32f));
        tocar(v(SERRA, 50f, 500f, 0.70f, 0.15f));
        tocar(ruido(1.1f, 0.55f, 0.45f), 0.65f);
        tocar(v(SENO, 90f, 25f, 1.0f, 0.55f), 0.65f);
    }

    public void vitoria() {
        int[] midi = {60, 64, 67, 72, 76, 79};
        for (int i = 0; i < midi.length; i++) tocar(v(QUADRADA, hz(midi[i]), hz(midi[i]), 0.35f, 0.16f), 0.11f * i);
        for (int n : new int[]{60, 64, 67, 72}) tocar(v(TRIANGULO, hz(n), hz(n), 1.4f, 0.12f), 0.7f);
    }

    public void derrota() {
        tocar(v(SERRA, 220f, 55f, 1.3f, 0.34f));
        tocar(v(QUADRADA, 165f, 41f, 1.3f, 0.18f), 0.15f);
        tocar(ruido(0.6f, 0.20f, 0.3f), 0.1f);
    }

    public void zonaAviso() {
        tocar(v(QUADRADA, 440f, 440f, 0.11f, 0.14f));
        tocar(v(QUADRADA, 440f, 440f, 0.11f, 0.14f), 0.17f);
    }

    public void zonaExplode() {
        tocar(ruido(0.4f, 0.35f, 0.5f));
        tocar(v(SENO, 120f, 40f, 0.35f, 0.40f));
    }

    public void uiMover() {
        tocar(v(QUADRADA, 660f, 700f, 0.045f, 0.12f));
    }

    public void uiOk() {
        tocar(v(QUADRADA, 660f, 660f, 0.07f, 0.16f));
        tocar(v(QUADRADA, 990f, 990f, 0.10f, 0.16f), 0.06f);
    }

    public void uiVoltar() {
        tocar(v(QUADRADA, 700f, 700f, 0.06f, 0.14f));
        tocar(v(QUADRADA, 440f, 440f, 0.10f, 0.14f), 0.05f);
    }

    public void tecla() {
        tocar(ruido(0.025f, 0.12f, 1f));
        tocar(v(SENO, 1300f, 800f, 0.03f, 0.10f));
    }

    public void negado() {
        tocar(v(QUADRADA, 130f, 100f, 0.18f, 0.22f));
    }

    public void surgiu() {
        tocar(v(TRIANGULO, 300f, 620f, 0.12f, 0.08f));
    }
}
