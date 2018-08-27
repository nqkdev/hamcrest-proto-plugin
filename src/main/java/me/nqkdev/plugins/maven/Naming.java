package me.nqkdev.plugins.maven;

import java.util.Arrays;
import java.util.stream.Collectors;

public class Naming {
    static String normalize(CharSequence name) {
        return capitalize(
                Arrays.stream(name.toString().split("_"))
                        .map(Naming::capitalize)
                        .collect(Collectors.joining()))
                .replaceFirst("^Class$", "Class_");
    }

    private static String capitalize(String name) {
        if (name != null && name.length() != 0) {
            char[] chars = name.toCharArray();
            chars[0] = Character.toUpperCase(chars[0]);
            return new String(chars);
        } else {
            return name;
        }
    }
}
