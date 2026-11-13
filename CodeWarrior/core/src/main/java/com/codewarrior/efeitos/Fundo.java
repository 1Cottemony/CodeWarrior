package com.codewarrior.efeitos;

import com.badlogic.gdx.graphics.Color;
import com.codewarrior.ui.Fontes;
import com.codewarrior.ui.Texto;

/** Fundo animado: editor de código escuro, grade synthwave em perspectiva e linhas de código rolando. */
public final class Fundo {
    private static final String[] CODIGO = {
            "import com.codewarrior.Terminal;",
            "public class Cursor extends Ninja {",
            "  private int bugs = 0;",
            "  void debug(Code code) {",
            "    while (code.hasBugs()) {",
            "      Bug b = code.next();",
            "      if (b.isSyntaxError()) b.delete();",
            "      else if (b.leaks()) cleanup(b);",
            "      combo += b.points * multiplier;",
            "    }",
            "  }",
            "  // TODO: fix misaligned <div>",
            "  const hp = boss.health - dash.damage;",
            "  git commit -m \"fix: all the bugs\"",
            "  for (;;) { /* infinite loop */ }",
            "  catch (NullPointerException e) {",
            "    log.error(\"segfault at 0x00\");",
            "  }",
            "  return refactor(world).clean();",
            "  ctrl.z(2.0); // rewind time",
            "  sudo rm -rf /bugs",
            "  <div style=\"margin:0 auto\"></div>",
            "  let score = combo * 100;",
            "  if (heart <= 0) gameOver();",
            "  while (alive) { dash(); }",
            "  npm run build && git push",
            "  }",
            "  /* the end is a semicolon; */",
    };
    private static final String[] FLUTUA = {"{", "}", ";", "<", ">", "/", "0", "1", "()", "[]", "=>", "&&"};
    private final Color tmp = new Color();

    /** t = tempo em segundos; forca 0..1 controla a visibilidade geral. */
    public void desenhar(Pincel p, Fontes f, float t, float forca) {
        // grade do "editor" (quadrados de 64px)
        p.cor(Cores.CIANO, 0.035f * forca);
        for (int x = 0; x <= 1280; x += 64) p.linha(x, 0, x, 720, 1f);
        for (int y = 0; y <= 720; y += 64) p.linha(0, y, 1280, y, 1f);

        // chão synthwave em perspectiva (horizonte em y = 330)
        float hor = 330f;
        int n = 12;
        float fase = (t * 0.22f) % 1f;
        for (int i = 0; i < n; i++) {
            float k = (i + fase) / n;
            float y = hor - hor * k * k;
            p.cor(Cores.MAGENTA, (0.03f + 0.12f * k) * forca);
            p.linha(0, y, 1280, y, 1.5f);
        }
        for (int j = -12; j <= 12; j++) {
            p.cor(Cores.CIANO, 0.07f * forca);
            p.linha(640f, hor, 640f + j * 120f, 0f, 1.2f);
        }
        p.cor(Cores.MAGENTA, 0.20f * forca);
        p.linha(0, hor, 1280, hor, 2f);

        // linhas de código rolando (duas colunas)
        float lh = 24f;
        float rolar = (t * 12f) % lh;
        for (int i = 0; i < 31; i++) {
            float y = 700f - i * lh + rolar;
            String s = CODIGO[(i + (int) (t * 0.5f)) % CODIGO.length];
            Color c = (i % 5 == 0) ? Cores.MAGENTA : (i % 5 == 1) ? Cores.LIMA : (i % 5 == 2) ? Cores.CIANO : Cores.CINZA;
            float a = 0.10f * forca;
            Texto.desenhar(p, f.codigo, s, 18f, y, Texto.ESQ, c, a);
            Texto.desenhar(p, f.codigo, s, 1262f, y - lh * 0.5f, Texto.DIR, c, a * 0.8f);
        }

        // símbolos flutuando
        for (int i = 0; i < 26; i++) {
            float sx = (i * 197f) % 1280f;
            float velocidade = 14f + (i % 7) * 6f;
            float y = ((i * 91f) + t * velocidade) % 760f - 20f;
            tmp.set(i % 2 == 0 ? Cores.CIANO : Cores.MAGENTA);
            Texto.desenhar(p, f.media, FLUTUA[i % FLUTUA.length], sx + (float) Math.sin(t * 0.6f + i) * 14f, y, Texto.CENTRO, tmp, 0.07f * forca);
        }
    }
}
