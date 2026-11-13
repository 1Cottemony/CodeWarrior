package com.codewarrior.teclado;

import java.util.HashMap;
import java.util.Map;

import com.badlogic.gdx.Input.Keys;

/**
 * Liga os ids do mapa (posição física no padrão do libGDX) aos códigos de tecla do libGDX.
 * No ABNT2 algumas teclas ("\" e "/") não têm código no libGDX: ficam apagadas no mapa.
 */
public final class TeclasGdx {
    private static final Map<Integer, String> POR_CODIGO = new HashMap<>();

    static {
        POR_CODIGO.put(Keys.GRAVE, "GRAVE");
        int[] numeros = {Keys.NUM_0, Keys.NUM_1, Keys.NUM_2, Keys.NUM_3, Keys.NUM_4, Keys.NUM_5, Keys.NUM_6, Keys.NUM_7, Keys.NUM_8, Keys.NUM_9};
        for (int i = 0; i < 10; i++) POR_CODIGO.put(numeros[i], "NUM_" + i);
        POR_CODIGO.put(Keys.MINUS, "MINUS");
        POR_CODIGO.put(Keys.EQUALS, "EQUALS");
        POR_CODIGO.put(Keys.BACKSPACE, "BACKSPACE");
        POR_CODIGO.put(Keys.TAB, "TAB");
        for (char c = 'A'; c <= 'Z'; c++) POR_CODIGO.put(Keys.A + (c - 'A'), String.valueOf(c));
        POR_CODIGO.put(Keys.LEFT_BRACKET, "LEFT_BRACKET");
        POR_CODIGO.put(Keys.RIGHT_BRACKET, "RIGHT_BRACKET");
        POR_CODIGO.put(Keys.CAPS_LOCK, "CAPS_LOCK");
        POR_CODIGO.put(Keys.SEMICOLON, "SEMICOLON");
        POR_CODIGO.put(Keys.APOSTROPHE, "APOSTROPHE");
        POR_CODIGO.put(Keys.BACKSLASH, "BACKSLASH");
        POR_CODIGO.put(Keys.SHIFT_LEFT, "SHIFT_LEFT");
        POR_CODIGO.put(Keys.SHIFT_RIGHT, "SHIFT_RIGHT");
        POR_CODIGO.put(Keys.COMMA, "COMMA");
        POR_CODIGO.put(Keys.PERIOD, "PERIOD");
        POR_CODIGO.put(Keys.SLASH, "SLASH");
        POR_CODIGO.put(Keys.SPACE, "SPACE");
    }

    private TeclasGdx() {
    }

    /** Id do mapa para um código de tecla do libGDX, ou null se a tecla não é destino. */
    public static String id(int codigoGdx) {
        return POR_CODIGO.get(codigoGdx);
    }
}
