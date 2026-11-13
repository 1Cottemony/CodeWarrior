package com.codewarrior.efeitos;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/**
 * A cena inteira é desenhada num FrameBuffer (proporção fixa 16:9) e depois passa por shaders GLSL:
 * brilho (bloom) suave, aberração cromática leve, linhas de varredura opcionais, vinheta e glitch.
 * Se algum shader não compilar, o jogo continua sem pós-processamento (sem travar).
 */
public final class PosProcessamento {
    private static final String VERT =
            "attribute vec4 a_position;\n"
                    + "attribute vec4 a_color;\n"
                    + "attribute vec2 a_texCoord0;\n"
                    + "uniform mat4 u_projTrans;\n"
                    + "varying vec4 v_color;\n"
                    + "varying vec2 v_texCoords;\n"
                    + "void main() {\n"
                    + "  v_color = a_color;\n"
                    + "  v_texCoords = a_texCoord0;\n"
                    + "  gl_Position = u_projTrans * a_position;\n"
                    + "}\n";

    private static final String FRAG_BRILHO =
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                    + "varying vec2 v_texCoords;\n"
                    + "uniform sampler2D u_texture;\n"
                    + "uniform float u_limiar;\n"
                    + "void main() {\n"
                    + "  vec3 c = texture2D(u_texture, v_texCoords).rgb;\n"
                    + "  float l = max(max(c.r, c.g), c.b);\n"
                    + "  float k = smoothstep(u_limiar, 1.0, l);\n"
                    + "  gl_FragColor = vec4(c * k, 1.0);\n"
                    + "}\n";

    private static final String FRAG_BLUR =
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                    + "varying vec2 v_texCoords;\n"
                    + "uniform sampler2D u_texture;\n"
                    + "uniform vec2 u_dir;\n"
                    + "void main() {\n"
                    + "  vec2 uv = v_texCoords;\n"
                    + "  vec3 s = texture2D(u_texture, uv).rgb * 0.227027;\n"
                    + "  s += (texture2D(u_texture, uv + u_dir * 1.3846).rgb + texture2D(u_texture, uv - u_dir * 1.3846).rgb) * 0.3162162;\n"
                    + "  s += (texture2D(u_texture, uv + u_dir * 3.2308).rgb + texture2D(u_texture, uv - u_dir * 3.2308).rgb) * 0.0702703;\n"
                    + "  gl_FragColor = vec4(s, 1.0);\n"
                    + "}\n";

    private static final String FRAG_FINAL =
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n"
                    + "varying vec2 v_texCoords;\n"
                    + "uniform sampler2D u_texture;\n"
                    + "uniform sampler2D u_bloom;\n"
                    + "uniform float u_bloomAmt;\n"
                    + "uniform float u_aberr;\n"
                    + "uniform float u_scan;\n"
                    + "uniform float u_vig;\n"
                    + "uniform float u_glitch;\n"
                    + "uniform float u_time;\n"
                    + "uniform float u_resY;\n"
                    + "void main() {\n"
                    + "  vec2 uv = v_texCoords;\n"
                    + "  float passo = floor(u_time * 18.0);\n"
                    + "  if (u_glitch > 0.001) {\n"
                    + "    float faixa = floor(uv.y * 26.0) + passo;\n"
                    + "    float h = fract(sin(faixa * 91.7 + passo * 13.1) * 43758.5453);\n"
                    + "    if (h > 0.8) { uv.x += (h - 0.9) * 0.10 * u_glitch; }\n"
                    + "  }\n"
                    + "  vec2 dir = uv - vec2(0.5, 0.5);\n"
                    + "  float a = u_aberr * (1.0 + u_glitch * 4.0);\n"
                    + "  float r = texture2D(u_texture, uv + dir * a * 2.0).r;\n"
                    + "  float g = texture2D(u_texture, uv).g;\n"
                    + "  float b = texture2D(u_texture, uv - dir * a * 2.0).b;\n"
                    + "  vec3 col = vec3(r, g, b);\n"
                    + "  col += texture2D(u_bloom, uv).rgb * u_bloomAmt;\n"
                    + "  float sl = 0.5 + 0.5 * sin(uv.y * u_resY * 3.14159);\n"
                    + "  col *= 1.0 - u_scan * (1.0 - sl) * 0.55;\n"
                    + "  float d = length(dir) * 1.25;\n"
                    + "  col *= 1.0 - u_vig * d * d;\n"
                    + "  gl_FragColor = vec4(col, 1.0);\n"
                    + "}\n";

    private FrameBuffer cena, bloomA, bloomB;
    private TextureRegion regCena, regA, regB;
    private ShaderProgram shBrilho, shBlur, shFinal;
    private final SpriteBatch lote = new SpriteBatch();
    private final Matrix4 proj = new Matrix4();
    private boolean shadersOk;
    private int larg, alt;

    public float bloom = 0.45f, aberracao = 0.0015f, varredura = 0f, vinheta = 0.35f, glitch = 0f, tempo = 0f;
    public boolean ativo = true;

    public PosProcessamento() {
        ShaderProgram.pedantic = false;
        try {
            shBrilho = new ShaderProgram(VERT, FRAG_BRILHO);
            shBlur = new ShaderProgram(VERT, FRAG_BLUR);
            shFinal = new ShaderProgram(VERT, FRAG_FINAL);
            shadersOk = shBrilho.isCompiled() && shBlur.isCompiled() && shFinal.isCompiled();
            if (!shadersOk) {
                Gdx.app.error("Shader", "brilho: " + shBrilho.getLog() + " blur: " + shBlur.getLog() + " final: " + shFinal.getLog());
            }
        } catch (Throwable t) {
            shadersOk = false;
            Gdx.app.error("Shader", "Falha ao criar shaders: " + t);
        }
    }

    public boolean shadersFuncionando() {
        return shadersOk;
    }

    public int largura() {
        return larg;
    }

    public int altura() {
        return alt;
    }

    /** Recria os FrameBuffers para o tamanho (em pixels) da área 16:9 da janela. */
    public void redimensionar(int w, int h) {
        w = Math.max(320, w);
        h = Math.max(180, h);
        if (cena != null && w == larg && h == alt) return;
        FrameBuffer novaCena = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        FrameBuffer novoA = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(2, w / 4), Math.max(2, h / 4), false);
        FrameBuffer novoB = new FrameBuffer(Pixmap.Format.RGBA8888, Math.max(2, w / 4), Math.max(2, h / 4), false);
        if (cena != null) {
            cena.dispose();
            bloomA.dispose();
            bloomB.dispose();
        }
        larg = w;
        alt = h;
        cena = novaCena;
        bloomA = novoA;
        bloomB = novoB;
        for (FrameBuffer fb : new FrameBuffer[]{cena, bloomA, bloomB}) {
            fb.getColorBufferTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            fb.getColorBufferTexture().setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge);
        }
        regCena = new TextureRegion(cena.getColorBufferTexture());
        regCena.flip(false, true);
        regA = new TextureRegion(bloomA.getColorBufferTexture());
        regA.flip(false, true);
        regB = new TextureRegion(bloomB.getColorBufferTexture());
        regB.flip(false, true);
    }

    /** Começa a desenhar a cena (tudo que o jogo desenhar a seguir vai para o FrameBuffer). */
    public void comecar() {
        cena.begin();
        Gdx.gl.glClearColor(Cores.FUNDO.r, Cores.FUNDO.g, Cores.FUNDO.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    /** Termina a cena, aplica os shaders e mostra o resultado na área (vx, vy, vw, vh) da janela. */
    public void terminar(int vx, int vy, int vw, int vh) {
        cena.end();
        Gdx.gl.glViewport(vx, vy, vw, vh);
        proj.setToOrtho2D(0f, 0f, vw, vh);
        lote.setProjectionMatrix(proj);
        lote.disableBlending();
        if (!ativo || !shadersOk) {
            lote.setShader(null);
            lote.begin();
            lote.setColor(1f, 1f, 1f, 1f);
            lote.draw(regCena, 0f, 0f, vw, vh);
            lote.end();
            lote.enableBlending();
            return;
        }
        int bw = bloomA.getWidth(), bh = bloomA.getHeight();
        // 1) pixels brilhantes
        bloomA.begin();
        Gdx.gl.glViewport(0, 0, bw, bh);
        proj.setToOrtho2D(0f, 0f, bw, bh);
        lote.setProjectionMatrix(proj);
        lote.setShader(shBrilho);
        lote.begin();
        shBrilho.setUniformf("u_limiar", 0.5f);
        lote.draw(regCena, 0f, 0f, bw, bh);
        lote.end();
        bloomA.end();
        // 2) desfoque horizontal e vertical
        bloomB.begin();
        Gdx.gl.glViewport(0, 0, bw, bh);
        proj.setToOrtho2D(0f, 0f, bw, bh);
        lote.setProjectionMatrix(proj);
        lote.setShader(shBlur);
        lote.begin();
        shBlur.setUniformf("u_dir", 1f / bw, 0f);
        lote.draw(regA, 0f, 0f, bw, bh);
        lote.end();
        bloomB.end();
        bloomA.begin();
        Gdx.gl.glViewport(0, 0, bw, bh);
        proj.setToOrtho2D(0f, 0f, bw, bh);
        lote.setProjectionMatrix(proj);
        lote.begin();
        shBlur.setUniformf("u_dir", 0f, 1f / bh);
        lote.draw(regB, 0f, 0f, bw, bh);
        lote.end();
        bloomA.end();
        // 3) composição final na tela
        Gdx.gl.glViewport(vx, vy, vw, vh);
        proj.setToOrtho2D(0f, 0f, vw, vh);
        lote.setProjectionMatrix(proj);
        lote.setShader(shFinal);
        lote.begin();
        shFinal.setUniformf("u_bloomAmt", bloom);
        shFinal.setUniformf("u_aberr", aberracao);
        shFinal.setUniformf("u_scan", varredura);
        shFinal.setUniformf("u_vig", vinheta);
        shFinal.setUniformf("u_glitch", glitch);
        shFinal.setUniformf("u_time", tempo);
        shFinal.setUniformf("u_resY", alt);
        bloomA.getColorBufferTexture().bind(1);
        shFinal.setUniformi("u_bloom", 1);
        Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
        lote.draw(regCena, 0f, 0f, vw, vh);
        lote.end();
        lote.setShader(null);
        lote.enableBlending();
    }

    public void dispose() {
        lote.dispose();
        if (cena != null) {
            cena.dispose();
            bloomA.dispose();
            bloomB.dispose();
        }
        if (shBrilho != null) shBrilho.dispose();
        if (shBlur != null) shBlur.dispose();
        if (shFinal != null) shFinal.dispose();
    }
}
