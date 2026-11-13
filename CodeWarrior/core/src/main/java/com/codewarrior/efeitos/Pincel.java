package com.codewarrior.efeitos;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

import space.earlygrey.shapedrawer.ShapeDrawer;

/**
 * Pincel único do jogo: um SpriteBatch + ShapeDrawer sobre um mesmo atlas gerado por código
 * (um pixel branco e um brilho radial). Toda a arte do jogo é desenhada com ele.
 * Coordenadas: tela virtual 1280x720, y para cima.
 */
public final class Pincel {
    public final SpriteBatch batch;
    public final ShapeDrawer sd;
    private final Texture atlas;
    private final TextureRegion pixel;
    private final TextureRegion brilho;
    private final Color cor = new Color(1f, 1f, 1f, 1f);
    private final float[] v = new float[20];
    private boolean aditivo;

    public Pincel() {
        batch = new SpriteBatch();
        Pixmap pm = new Pixmap(128, 64, Pixmap.Format.RGBA8888);
        pm.setColor(1f, 1f, 1f, 1f);
        pm.fillRectangle(64, 0, 64, 64);
        for (int y = 0; y < 64; y++) {
            for (int x = 0; x < 64; x++) {
                float d = (float) Math.hypot(x - 31.5f, y - 31.5f) / 32f;
                float a = d >= 1f ? 0f : (1f - d) * (1f - d);
                pm.drawPixel(x, y, Color.rgba8888(1f, 1f, 1f, a));
            }
        }
        atlas = new Texture(pm);
        atlas.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        pixel = new TextureRegion(atlas, 96, 32, 1, 1);
        brilho = new TextureRegion(atlas, 0, 0, 64, 64);
        sd = new ShapeDrawer(batch, pixel);
    }

    public void dispose() {
        batch.dispose();
        atlas.dispose();
    }

    // ---------------------------------------------------------------- cor
    public void cor(float r, float g, float b, float a) {
        cor.set(r, g, b, a);
        sd.setColor(cor);
        batch.setColor(cor);
    }

    public void cor(Color c) {
        cor(c.r, c.g, c.b, c.a);
    }

    public void cor(Color c, float alfa) {
        cor(c.r, c.g, c.b, c.a * alfa);
    }

    public Color corAtual() {
        return cor;
    }

    /** Liga/desliga a mistura aditiva (brilhos). */
    public void aditivo(boolean ligado) {
        if (ligado == aditivo) return;
        aditivo = ligado;
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, ligado ? GL20.GL_ONE : GL20.GL_ONE_MINUS_SRC_ALPHA);
    }

    // ---------------------------------------------------------------- formas
    public void reta(float x, float y, float w, float h) {
        sd.filledRectangle(x, y, w, h);
    }

    public void borda(float x, float y, float w, float h, float lw) {
        sd.rectangle(x, y, w, h, lw);
    }

    public void linha(float x1, float y1, float x2, float y2, float lw) {
        sd.line(x1, y1, x2, y2, lw);
    }

    public void disco(float x, float y, float r) {
        sd.filledCircle(x, y, r);
    }

    public void anel(float x, float y, float r, float lw) {
        arco(x, y, r, 0f, 360f, lw);
    }

    /** Arco em graus (0 = direita, anti-horário). */
    public void arco(float x, float y, float r, float graus0, float graus1, float lw) {
        int n = Math.max(6, (int) (Math.abs(graus1 - graus0) / 12f));
        float px = x + r * (float) Math.cos(Math.toRadians(graus0));
        float py = y + r * (float) Math.sin(Math.toRadians(graus0));
        for (int i = 1; i <= n; i++) {
            float a = (float) Math.toRadians(graus0 + (graus1 - graus0) * i / n);
            float nx = x + r * (float) Math.cos(a), ny = y + r * (float) Math.sin(a);
            sd.line(px, py, nx, ny, lw);
            px = nx;
            py = ny;
        }
    }

    public void tri(float x1, float y1, float x2, float y2, float x3, float y3) {
        float c = cor.toFloatBits();
        float u = (pixel.getU() + pixel.getU2()) * 0.5f, w = (pixel.getV() + pixel.getV2()) * 0.5f;
        v[0] = x1; v[1] = y1; v[2] = c; v[3] = u; v[4] = w;
        v[5] = x2; v[6] = y2; v[7] = c; v[8] = u; v[9] = w;
        v[10] = x3; v[11] = y3; v[12] = c; v[13] = u; v[14] = w;
        v[15] = x3; v[16] = y3; v[17] = c; v[18] = u; v[19] = w;
        batch.draw(pixel.getTexture(), v, 0, 20);
    }

    /** Polígono convexo preenchido (leque de triângulos). pts = x0,y0,x1,y1... */
    public void poligono(float[] pts, int nPontos) {
        for (int i = 1; i < nPontos - 1; i++) {
            tri(pts[0], pts[1], pts[i * 2], pts[i * 2 + 1], pts[i * 2 + 2], pts[i * 2 + 3]);
        }
    }

    public void contorno(float[] pts, int nPontos, float lw) {
        for (int i = 0; i < nPontos; i++) {
            int j = (i + 1) % nPontos;
            sd.line(pts[i * 2], pts[i * 2 + 1], pts[j * 2], pts[j * 2 + 1], lw);
        }
    }

    private final float[] oct = new float[16];

    private void octogono(float x, float y, float w, float h, float n) {
        oct[0] = x + n; oct[1] = y;
        oct[2] = x + w - n; oct[3] = y;
        oct[4] = x + w; oct[5] = y + n;
        oct[6] = x + w; oct[7] = y + h - n;
        oct[8] = x + w - n; oct[9] = y + h;
        oct[10] = x + n; oct[11] = y + h;
        oct[12] = x; oct[13] = y + h - n;
        oct[14] = x; oct[15] = y + n;
    }

    /** Retângulo de cantos "picotados" (estilo pixel-art das teclas). */
    public void retaCortada(float x, float y, float w, float h, float n) {
        octogono(x, y, w, h, n);
        poligono(oct, 8);
    }

    public void bordaCortada(float x, float y, float w, float h, float n, float lw) {
        octogono(x, y, w, h, n);
        contorno(oct, 8, lw);
    }

    /** Brilho radial suave (usa mistura aditiva por conta própria e restaura a anterior). */
    public void brilho(float x, float y, float raio) {
        boolean antes = aditivo;
        aditivo(true);
        batch.draw(brilho, x - raio, y - raio, raio * 2f, raio * 2f);
        aditivo(antes);
    }

    /** Brilho retangular simples (halo em volta de um botão): vários contornos de alfa decrescente. */
    public void halo(float x, float y, float w, float h, Color c, float forca) {
        boolean antes = aditivo;
        aditivo(true);
        for (int i = 1; i <= 4; i++) {
            cor(c, forca * (0.20f / i));
            sd.rectangle(x - i * 3f, y - i * 3f, w + i * 6f, h + i * 6f, 3f);
        }
        aditivo(antes);
        cor(1f, 1f, 1f, 1f);
    }
}
