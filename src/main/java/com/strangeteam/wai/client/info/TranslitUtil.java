/*
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Copyright (c) 2026 Fl1z (https://strangehillco.github.io/StrangeHill.github.io/#member-Fl1z)
 */

package com.strangeteam.wai.client.info;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

/** Транслитерация кириллицы в латиницу + нормализация ID */
@SideOnly(Side.CLIENT)
public final class TranslitUtil {

    private static final Map<Character, String> MAP = new HashMap<>();

    static {
        // Русские буквы (нижний регистр)
        String[] rus = {
                "а","б","в","г","д","е","ё","ж","з","и","й","к","л","м","н","о",
                "п","р","с","т","у","ф","х","ц","ч","ш","щ","ъ","ы","ь","э","ю","я"
        };
        String[] lat = {
                "a","b","v","g","d","e","e","zh","z","i","y","k","l","m","n","o",
                "p","r","s","t","u","f","h","ts","ch","sh","sch","","y","","e","yu","ya"
        };
        for (int i = 0; i < rus.length; i++) {
            MAP.put(rus[i].charAt(0), lat[i]);
        }
        // Украинские/белорусские
        MAP.put('і', "i");
        MAP.put('ї', "yi");
        MAP.put('є', "ye");
        MAP.put('ґ', "g");
        MAP.put('ў', "u");
    }

    /** Транслитерация строки с сохранением регистра */
    public static String translit(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            char lower = Character.toLowerCase(c);
            String rep = MAP.get(lower);
            if (rep != null) {
                if (Character.isUpperCase(c) && !rep.isEmpty()) {
                    sb.append(Character.toUpperCase(rep.charAt(0)));
                    if (rep.length() > 1) sb.append(rep.substring(1));
                } else {
                    sb.append(rep);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Делает безопасный ID из произвольной строки:
     *  - транслитерирует кириллицу
     *  - заменяет всё кроме [a-zA-Z0-9_-] на _
     *  - схлопывает повторные _
     *  - обрезает по краям
     *  - ограничивает длину
     */
    public static String makeId(String source, int maxLen) {
        if (source == null || source.trim().isEmpty()) return "track";
        String t = translit(source.trim());
        t = t.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        t = t.replaceAll("_+", "_");
        t = t.replaceAll("^_+|_+$", "");
        if (t.isEmpty()) t = "track";
        if (t.length() > maxLen) t = t.substring(0, maxLen);
        return t;
    }

    public static String makeId(String source) {
        return makeId(source, 64);
    }

    private TranslitUtil() {}
}