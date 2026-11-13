package com.codewarrior.lwjgl3;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowAdapter;
import com.badlogic.gdx.graphics.Pixmap;
import com.codewarrior.CodeWarriorGame;

/** Inicia o jogo no desktop (LWJGL3). */
public class Lwjgl3Launcher {
    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // necessário em alguns sistemas (macOS) e ajuda no Windows
        new Lwjgl3Application(new CodeWarriorGame(), configuracao());
    }

    private static Lwjgl3ApplicationConfiguration configuracao() {
        Lwjgl3ApplicationConfiguration c = new Lwjgl3ApplicationConfiguration();
        c.setTitle("Code Warrior: O Mestre do Debug");
        c.useVsync(true);
        c.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate + 1);
        c.setWindowedMode(1280, 720);
        c.setWindowSizeLimits(640, 360, -1, -1);
        c.setResizable(true);
        // buffer de áudio menor = menos atraso nos efeitos sonoros (se estalar, aumente 512 para 1024)
        c.setAudioConfig(16, 512, 3);
        c.setWindowListener(new Lwjgl3WindowAdapter() {
            @Override
            public void created(Lwjgl3Window window) {
                Pixmap icone = new Pixmap(32, 32, Pixmap.Format.RGBA8888);
                icone.setColor(0.05f, 0.07f, 0.1f, 1f);
                icone.fill();
                icone.setColor(0f, 0.9f, 1f, 1f);
                icone.drawRectangle(1, 1, 30, 30);
                icone.fillRectangle(8, 10, 4, 4);
                icone.fillRectangle(12, 14, 4, 4);
                icone.fillRectangle(8, 18, 4, 4);
                icone.fillRectangle(18, 20, 8, 3);
                window.setIcon(icone);
                icone.dispose();
            }
        });
        return c;
    }
}
