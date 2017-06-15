package io.bitsensor.plugins.maven;

import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.util.Arrays;
import java.util.List;

public class ClassUtils {

    public static final List<String> DEFAULT_SEARCH_PACKAGES = Arrays.asList("java.lang", "java.util");

    public static Class<?> findClassByName(String name) throws ClassNotFoundException {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return findClassInDefaultPackage(name, DEFAULT_SEARCH_PACKAGES);
        }
    }

    public static Class<?> findClassByName(String name, Iterable<? extends String> searchPackages) throws ClassNotFoundException {
        for (String searchPackage : searchPackages) {
            try {
                return ReflectionUtils.forName(new Reflections(searchPackage).getStore()
                        .get(SubTypesScanner.class.getSimpleName()).values()
                        .stream()
                        .filter(s -> s.endsWith("." + name) || s.endsWith("$" + name))
                        .findFirst()
                        .orElseThrow(() -> new ClassNotFoundException(name.concat(" in ").concat(searchPackage))));
            } catch (ClassNotFoundException ignored) {
            }
        }
        return findClassByName(name);
    }

    private static Class<?> findClassInDefaultPackage(String name, Iterable<? extends String> searchPackages) throws ClassNotFoundException {
        for (String searchPackage : searchPackages) {
            try {
                return Class.forName(searchPackage + "." + name);
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

}