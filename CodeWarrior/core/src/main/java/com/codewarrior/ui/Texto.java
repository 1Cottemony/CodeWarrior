package com.codewarrior.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.codewarrior.efeitos.Cores;
import com.codewarrior.efeitos.Pincel;

/** Desenho de texto com alinhamento e efeitos (sombra neon, gradiente, glitch). */
public final class Texto {
    public static final int ESQ = -1, CENTRO = 0, DIR = 1;
    private static final GlyphLayout layout = new GlyphLayout();
    private static final Color tmp = new Color();

    private Texto() {
    }

    public static float largura(BitmapFont f, String s) {
        layout.setText(f, s);
        return layout.width;
    }

    public static float altura(BitmapFont f, String s) {
        layout.setText(f, s);
        return layout.height;
    }

    /** (x, y) = ponto de alinhamento horizontal e CENTRO VERTICAL do texto. */
    public static void desenhar(Pincel p, BitmapFont f, String s, float x, float y, int alinh, Color c, float alfa) {
        f.setColor(c.r, c.g, c.b, c.a * alfa);
        layout.setText(f, s);
        float w = layout.width;
        float ax = alinh == ESQ ? x : alinh == CENTRO ? x - w / 2f : x - w;
        f.draw(p.batch, layout, ax, y + layout.height / 2f);
    }

    public static void desenhar(Pincel p, BitmapFont f, String s, float x, float y, int alinh, Color c) {
        desenhar(p, f, s, x, y, alinh, c, 1f);
    }

    /** Texto com brilho colorido atrás (neon). */
    public static void neon(Pincel p, BitmapFont f, String s, float x, float y, int alinh, Color c, float alfa) {
        p.aditivo(true);
        desenhar(p, f, s, x - 1.5f, y, alinh, c, 0.35f * alfa);
        desenhar(p, f, s, x + 1.5f, y, alinh, c, 0.35f * alfa);
        desenhar(p, f, s, x, y - 1.5f, alinh, c, 0.35f * alfa);
        desenhar(p, f, s, x, y + 1.5f, alinh, c, 0.35f * alfa);
        p.aditivo(false);
        desenhar(p, f, s, x, y, alinh, c, alfa);
    }

    public static void sombra(Pincel p, BitmapFont f, String s, float x, float y, int alinh, Color c, float alfa) {
        desenhar(p, f, s, x + 2f, y - 2f, alinh, Cores.FUNDO, 0.85f * alfa);
        desenhar(p, f, s, x, y, alinh, c, alfa);
    }

    /** Cada letra com uma cor do gradiente h0 -> h1 (matiz). Centralizado em x. */
    public static void gradiente(Pincel p, BitmapFont f, String s, float x, float y, float h0, float h1, float alfa) {
        float total = largura(f, s);
        float cx = x - total / 2f;
        for (int i = 0; i < s.length(); i++) {
            String parte = s.substring(0, i);
            float off = i == 0 ? 0f : largura(f, parte + "|") - largura(f, "|");
            float t = s.length() <= 1 ? 0f : i / (float) (s.length() - 1);
            Cores.hsv(h0 + (h1 - h0) * t, 0.75f, 1f, tmp);
            desenhar(p, f, s.substring(i, i + 1), cx + off, y, ESQ, tmp, alfa);
        }
    }

    /** Título com deslocamento de canais (aberração) e fatias de glitch. */
    public static void glitch(Pincel p, BitmapFont f, String s, float x, float y, Color c, float intensidade, float t) {
        float j = intensidade * 5f;
        p.aditivo(true);
        desenhar(p, f, s, x - j, y, CENTRO, Cores.CIANO, 0.55f);
        desenhar(p, f, s, x + j, y, CENTRO, Cores.MAGENTA, 0.55f);
        p.aditivo(false);
        float fatia = (float) Math.sin(t * 23f) * j * 1.6f * (Math.sin(t * 3.1f) > 0.85f ? 1f : 0f);
        desenhar(p, f, s, x + fatia, y, CENTRO, c, 1f);
    }

    /** Quebra o texto em linhas de no máximo "larg" pixels. */
    public static String quebrar(BitmapFont f, String s, float larg) {
        StringBuilder out = new StringBuilder();
        StringBuilder linha = new StringBuilder();
        for (String palavra : s.split(" ")) {
            String teste = linha.length() == 0 ? palavra : linha + " " + palavra;
            if (largura(f, teste) > larg && linha.length() > 0) {
                out.append(linha).append('\n');
                linha.setLength(0);
                linha.append(palavra);
            } else {
                linha.setLength(0);
                linha.append(teste);
            }
        }
        out.append(linha);
        return out.toString();
    }

    /** Desenha várias linhas (separadas por \n) centradas verticalmente em y. */
    public static void linhas(Pincel p, BitmapFont f, String s, float x, float y, int alinh, Color c, float alfa, float entre) {
        String[] ls = s.split("\n");
        float top = y + (ls.length - 1) * entre / 2f;
        for (int i = 0; i < ls.length; i++) desenhar(p, f, ls[i], x, top - i * entre, alinh, c, alfa);
    }
}
