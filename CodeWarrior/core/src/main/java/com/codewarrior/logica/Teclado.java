package com.codewarrior.logica;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Mapa do jogo: o bloco principal de um teclado ABNT2 (sem numérico e sem F1-F12).
 * Cada tecla é identificada por um id fixo (posição física no padrão do libGDX) e leva o rótulo ABNT2.
 * Coordenadas em "unidades de tecla"; use U/OX/OY para converter para a tela virtual 1280x720.
 */
public final class Teclado {
    public enum Tipo { DESTINO, RESERVADA, INDISPONIVEL }

    public static final float U = 66f;
    public static final float OX = 145f;
    public static final float OY = 175f;
    public static final int FILAS = 5;

    public static final class Tecla {
        public final int indice;
        public final String id;
        public final String rotulo;
        /** Caractere do Shift (ou null). */
        public final String rotuloShift;
        public final float x, y, w, h;
        public final Tipo tipo;
        public final int fila;
        /** Centro e tamanho em pixels da tela virtual (sem desalinhamento). */
        public final float cx, cy, pw, ph;

        Tecla(int indice, String id, String rotulo, String shift, float x, int fila, float w, float h, Tipo tipo) {
            this.indice = indice;
            this.id = id;
            this.rotulo = rotulo;
            this.rotuloShift = shift;
            this.x = x;
            this.y = fila;
            this.w = w;
            this.h = h;
            this.tipo = tipo;
            this.fila = fila;
            this.pw = w * U;
            this.ph = h * U;
            this.cx = OX + (x + w / 2f) * U;
            this.cy = OY + (fila + h / 2f) * U;
        }
    }

    public final Tecla[] teclas;
    /** Pares de teclas: cada item = {indiceAbertura, indiceFechamento}. */
    public final int[][] pares;
    /** Rótulos dos pares para a interface. */
    public final String[] paresNome = {"( )", "< >", "[ ]"};
    /** Grupos para o Infinite Loop circular: {centroX, centroY, raio}. */
    public final float[][] gruposLoop;

    private final Map<String, Integer> porId = new HashMap<>();
    private final ArrayList<Tecla> lista = new ArrayList<>();
    private float cur;

    public Teclado() {
        // ---- fila 5 (topo) ----
        ini(0);
        k(4, "GRAVE", "'", "\"", 1, Tipo.DESTINO);
        String[] sh = {"!", "@", "#", "$", "%", "¨", "&", "*", "(", ")"};
        String[] dig = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0"};
        for (int i = 0; i < 10; i++) k(4, "NUM_" + dig[i], dig[i], sh[i], 1, Tipo.DESTINO);
        k(4, "MINUS", "-", "_", 1, Tipo.DESTINO);
        k(4, "EQUALS", "=", "+", 1, Tipo.DESTINO);
        k(4, "BACKSPACE", "BKSP", null, 2, Tipo.DESTINO);
        // ---- fila 4 ----
        ini(0);
        k(3, "TAB", "TAB", null, 1.5f, Tipo.DESTINO);
        for (char c : "QWERTYUIOP".toCharArray()) k(3, "" + c, "" + c, null, 1, Tipo.DESTINO);
        k(3, "LEFT_BRACKET", "´", "`", 1, Tipo.DESTINO);
        k(3, "RIGHT_BRACKET", "[", "{", 1, Tipo.DESTINO);
        lista.add(new Tecla(lista.size(), "ENTER", "ENTER", null, 13.5f, 2, 1.5f, 2f, Tipo.RESERVADA));
        // ---- fila 3 ----
        ini(0);
        k(2, "CAPS_LOCK", "CAPS", null, 1.5f, Tipo.DESTINO);
        for (char c : "ASDFGHJKL".toCharArray()) k(2, "" + c, "" + c, null, 1, Tipo.DESTINO);
        k(2, "SEMICOLON", "Ç", null, 1, Tipo.DESTINO);
        k(2, "APOSTROPHE", "~", "^", 1, Tipo.DESTINO);
        k(2, "BACKSLASH", "]", "}", 1, Tipo.DESTINO);
        // ---- fila 2 ----
        ini(0);
        k(1, "SHIFT_LEFT", "SHIFT", null, 1.25f, Tipo.DESTINO);
        k(1, "WORLD_1", "\\", "|", 1, Tipo.INDISPONIVEL);
        for (char c : "ZXCVBNM".toCharArray()) k(1, "" + c, "" + c, null, 1, Tipo.DESTINO);
        k(1, "COMMA", ",", "<", 1, Tipo.DESTINO);
        k(1, "PERIOD", ".", ">", 1, Tipo.DESTINO);
        k(1, "SLASH", ";", ":", 1, Tipo.DESTINO);
        k(1, "ABNT_SLASH", "/", "?", 1, Tipo.INDISPONIVEL);
        k(1, "SHIFT_RIGHT", "SHIFT", null, 1.75f, Tipo.DESTINO);
        // ---- fila 1 (base) ----
        ini(0);
        k(0, "CONTROL_LEFT", "CTRL", null, 1.25f, Tipo.RESERVADA);
        k(0, "SUPER_LEFT", "WIN", null, 1.25f, Tipo.INDISPONIVEL);
        k(0, "ALT_LEFT", "ALT", null, 1.25f, Tipo.INDISPONIVEL);
        k(0, "SPACE", "ESPAÇO", null, 6.25f, Tipo.DESTINO);
        k(0, "ALT_RIGHT", "ALTGR", null, 1.25f, Tipo.INDISPONIVEL);
        k(0, "SUPER_RIGHT", "WIN", null, 1.25f, Tipo.INDISPONIVEL);
        k(0, "MENU", "MENU", null, 1.25f, Tipo.INDISPONIVEL);
        k(0, "CONTROL_RIGHT", "CTRL", null, 1.25f, Tipo.RESERVADA);

        teclas = lista.toArray(new Tecla[0]);
        for (Tecla t : teclas) porId.put(t.id, t.indice);

        pares = new int[][]{
                {indice("NUM_9"), indice("NUM_0")},
                {indice("COMMA"), indice("PERIOD")},
                {indice("RIGHT_BRACKET"), indice("BACKSLASH")}
        };

        String[][] grupos = {
                {"W", "A", "S", "D"}, {"I", "J", "K", "L"}, {"R", "T", "F", "G"}, {"Z", "X", "C", "V"},
                {"NUM_1", "NUM_2", "Q", "W"}, {"B", "N", "M", "COMMA"}, {"NUM_7", "NUM_8", "U", "I"}
        };
        gruposLoop = new float[grupos.length][3];
        for (int g = 0; g < grupos.length; g++) {
            float sx = 0, sy = 0;
            for (String id : grupos[g]) {
                Tecla t = teclas[indice(id)];
                sx += t.cx;
                sy += t.cy;
            }
            float cx = sx / grupos[g].length, cy = sy / grupos[g].length, r = 0;
            for (String id : grupos[g]) {
                Tecla t = teclas[indice(id)];
                r = Math.max(r, (float) Math.hypot(t.cx - cx, t.cy - cy));
            }
            gruposLoop[g][0] = cx;
            gruposLoop[g][1] = cy;
            gruposLoop[g][2] = r + 22f;
        }
    }

    private void ini(float x) {
        cur = x;
    }

    private void k(int fila, String id, String rot, String shift, float w, Tipo tipo) {
        lista.add(new Tecla(lista.size(), id, rot, shift, cur, fila, w, 1f, tipo));
        cur += w;
    }

    public int indice(String id) {
        Integer i = porId.get(id);
        if (i == null) throw new IllegalArgumentException("Tecla desconhecida: " + id);
        return i;
    }

    public boolean existe(String id) {
        return porId.containsKey(id);
    }

    /** Índice da tecla sob o ponto (sem desalinhamento), ou -1. */
    public int teclaEm(float px, float py) {
        for (Tecla t : teclas) {
            if (Math.abs(px - t.cx) <= t.pw / 2f && Math.abs(py - t.cy) <= t.ph / 2f) return t.indice;
        }
        return -1;
    }

    /** Se a tecla faz parte de um par, retorna o índice do par (0..2); senão -1. */
    public int parDe(int tecla) {
        for (int p = 0; p < pares.length; p++) {
            if (pares[p][0] == tecla || pares[p][1] == tecla) return p;
        }
        return -1;
    }

    public int parceira(int tecla) {
        int p = parDe(tecla);
        if (p < 0) return -1;
        return pares[p][0] == tecla ? pares[p][1] : pares[p][0];
    }

    public int contarDestinos() {
        int n = 0;
        for (Tecla t : teclas) if (t.tipo == Tipo.DESTINO) n++;
        return n;
    }
}
