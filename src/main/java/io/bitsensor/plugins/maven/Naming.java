package io.bitsensor.plugins.maven;

public class Naming {
    static String normalize(CharSequence name) {
        return capitalize(name.toString().replaceFirst("^_", ""))
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
