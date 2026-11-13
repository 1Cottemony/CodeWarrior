package com.codewarrior.ui;

import java.util.ArrayList;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

/**
 * Fontes do jogo. Sem arquivos .ttf no projeto: usa fontes que já vêm no Windows
 * (Consolas Negrito para textos, Arial Black para títulos) via FreeType.
 * Se nenhuma for encontrada, cai na fonte embutida do libGDX (o jogo nunca quebra por causa de fonte).
 */
public final class Fontes {
    /** As fontes são geradas 1,5x maiores e reduzidas, para ficarem nítidas com a escala do Windows em 125%/150%. */
    public static final float SUPER = 1.5f;
    private static final String CHARS = FreeTypeFontGenerator.DEFAULT_CHARS
            + "ÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇáàâãäéèêëíìîïóòôõöúùûüç¨´`^~°ºª×•";

    public final BitmapFont codigo, pequena, media, grande, enorme, placar, titulo, tituloM;
    public final boolean usandoFonteDoWindows;
    private final ArrayList<BitmapFont> todas = new ArrayList<>();

    public Fontes() {
        String win = System.getenv("WINDIR");
        if (win == null || win.isEmpty()) win = "C:\\Windows";
        String pasta = win + "\\Fonts\\";
        String[] corpo = {pasta + "consolab.ttf", pasta + "consola.ttf", pasta + "arialbd.ttf", pasta + "segoeuib.ttf"};
        String[] tit = {pasta + "ariblk.ttf", pasta + "impact.ttf", pasta + "arialbd.ttf", pasta + "consolab.ttf"};
        boolean[] achou = new boolean[1];
        codigo = criar(corpo, 15, achou);
        pequena = criar(corpo, 20, achou);
        media = criar(corpo, 27, achou);
        grande = criar(corpo, 38, achou);
        enorme = criar(corpo, 56, achou);
        placar = criar(tit, 78, achou);
        titulo = criar(tit, 104, achou);
        tituloM = criar(tit, 52, achou);
        usandoFonteDoWindows = achou[0];
    }

    private BitmapFont criar(String[] caminhos, int px, boolean[] achou) {
        for (String c : caminhos) {
            try {
                FileHandle fh = Gdx.files.absolute(c);
                if (!fh.exists()) continue;
                FreeTypeFontGenerator g = new FreeTypeFontGenerator(fh);
                FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
                p.size = Math.round(px * SUPER);
                p.characters = CHARS;
                p.color = Color.WHITE;
                p.minFilter = Texture.TextureFilter.Linear;
                p.magFilter = Texture.TextureFilter.Linear;
                BitmapFont f = g.generateFont(p);
                g.dispose();
                f.getData().setScale(1f / SUPER);
                f.setUseIntegerPositions(false);
                todas.add(f);
                achou[0] = true;
                return f;
            } catch (Throwable t) {
                Gdx.app.error("Fontes", "Falha ao carregar " + c + ": " + t);
            }
        }
        BitmapFont f = new BitmapFont();
        f.getData().setScale(px / 15f);
        f.setUseIntegerPositions(false);
        f.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        todas.add(f);
        return f;
    }

    public void dispose() {
        for (BitmapFont f : todas) f.dispose();
    }
}
