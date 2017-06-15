package io.bitsensor.plugins.maven;

import io.protostuff.compiler.parser.LocalFileReader;
import io.protostuff.generator.GeneratorException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.*;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_SOURCES;
import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST_COMPILE;
import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

@Mojo(name = "compile",
        defaultPhase = PROCESS_SOURCES,
        threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class GeneratorMojo extends AbstractMojo {
    private static final String GENERATED_SOURCES = "/generated-sources/proto";
    private static final String GENERATED_TEST_SOURCES = "/generated-test-sources/proto";

    @Parameter(property = "project.runtimeClasspathElements", required = true)
    private List<String> runtimeClasspathElements;

    @Parameter(defaultValue = "${mojoExecution}", readonly = true)
    private MojoExecution execution;

    @Parameter
    private File source;

    @Parameter
    private File target;

    @Component
    private MavenProject project;

    @Component
    private MavenSession session;

    @Component
    private BuildPluginManager pluginManager;

    public void execute() throws MojoExecutionException, MojoFailureException {
        // Compiles generated sources
        executeMojo(plugin("org.apache.maven.plugins",
                "maven-compiler-plugin",
                "3.1"),
                goal("compile"),
                configuration(),
                executionEnvironment(project, session, pluginManager)
        );

        // Adds compiled classes to classpath for use with reflections
        addCompiledClassesToClasspath();

        final Path sourcePath = getSourcePath();
        String output = computeSourceOutputDir(target);
        List<String> protoFiles = findProtoFiles(sourcePath);

        Generator generator = new Generator(new LocalFileReader(sourcePath));

        protoFiles.forEach(s -> generator.generateMatchers(s, output));

        // Adds all new generated sources to project
//        addGeneratedSourcesToProject(output);
    }

    private void addCompiledClassesToClasspath() {
        Set<URL> urls = new HashSet<>();

        for (String element : runtimeClasspathElements) {
            try {
                urls.add(new File(element).toURI().toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Failed adding compiled classes to classpath.");
            }
        }

        ClassLoader contextClassLoader = URLClassLoader.newInstance(
                urls.toArray(new URL[0]),
                Thread.currentThread().getContextClassLoader());

        Thread.currentThread().setContextClassLoader(contextClassLoader);
    }

    private Path getSourcePath() {
        if (source != null) {
            return source.toPath();
        }
        String phase = execution.getLifecyclePhase();
        String basedir;
        basedir = getCanonicalPath(project.getBasedir());
        String sourcePath;
        if (TEST_COMPILE.id().equals(phase)) {
            sourcePath = basedir + "/src/test/proto/";
        } else {
            sourcePath = basedir + "/src/main/proto/";
        }
        return Paths.get(sourcePath);
    }

    private String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            throw new GeneratorException("Could not determine full path for %s", e, file);
        }
    }

    private List<String> findProtoFiles(final Path sourcePath) {
        List<String> protoFiles = new ArrayList<>();
        if (Files.exists(sourcePath) && Files.isDirectory(sourcePath)) {
            PathMatcher protoMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.proto");
            try {
                Files.walkFileTree(sourcePath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (protoMatcher.matches(file)) {
                            String protoFile = sourcePath.relativize(file).toString();
                            String normalizedPath = normalizeProtoPath(protoFile);
                            protoFiles.add(normalizedPath);
                            getLog().info(normalizedPath);
                        }
                        return super.visitFile(file, attrs);
                    }
                });
            } catch (IOException e) {
                getLog().error("Can not build source files list", e);
            }
        }
        return protoFiles;
    }

    private String normalizeProtoPath(String protoFilePath) {
        String normalizedPath;
        if (File.separatorChar == '\\') {
            normalizedPath = protoFilePath.replace('\\', '/');
        } else {
            normalizedPath = protoFilePath;
        }
        return normalizedPath;
    }

    private String computeSourceOutputDir(File target) {
        String output;
        if (target != null) {
            output = target.getAbsolutePath();
        } else {
            String phase = execution.getLifecyclePhase();
            String buildDirectory = project.getBuild().getDirectory();
            if (TEST_COMPILE.id().equals(phase)) {
                output = buildDirectory + GENERATED_TEST_SOURCES;
            } else {
                output = buildDirectory + GENERATED_SOURCES;
            }
        }
        getLog().debug("output = " + output);
        return output;
    }

    private void addGeneratedSourcesToProject(String output) {
        // Include generated directory to the list of compilation sources
        if (TEST_COMPILE.id().equals(execution.getLifecyclePhase())) {
            project.addTestCompileSourceRoot(output);
        } else {
            project.addCompileSourceRoot(output);
        }
    }
}
