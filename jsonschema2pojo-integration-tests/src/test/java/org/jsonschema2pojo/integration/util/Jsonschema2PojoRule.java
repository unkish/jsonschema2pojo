/**
 * Copyright © 2010-2020 Nokia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jsonschema2pojo.integration.util;

import static org.apache.commons.io.FileUtils.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.jsonschema2pojo.integration.util.Compiler.*;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;

import org.apache.commons.lang.StringUtils;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A JUnit rule that executes JsonSchema2Pojo.
 *
 * @author Christian Trimble
 *
 */
public class Jsonschema2PojoRule implements TestRule, AfterAllCallback, AfterEachCallback, BeforeAllCallback, BeforeEachCallback {

    private File generateDir;
    private File compileDir;
    private boolean active = false;
    private boolean captureDiagnostics = false;
    private boolean sourceDirInitialized = false;
    private boolean classesDirInitialized = false;
    private boolean isClassRule = false;
    private List<Diagnostic<? extends JavaFileObject>> diagnostics;

    public Jsonschema2PojoRule captureDiagnostics() {
        this.captureDiagnostics = true;
        return this;
    }

    public Jsonschema2PojoRule classRule() {
        this.isClassRule = true;
        return this;
    }

    /**
     * Gets the target directory for generate calls.
     *
     * @return The target directory for generate calls.
     */
    public File getGenerateDir() {
        checkActive();
        sourceDirInitialized = ensureDirectoryInitialized(generateDir, sourceDirInitialized);
        return generateDir;
    }

    /**
     * Gets the target directory for compile calls.
     *
     * @return The target directory for compile calls.
     */
    public File getCompileDir() {
        checkActive();
        classesDirInitialized = ensureDirectoryInitialized(compileDir, classesDirInitialized);
        return compileDir;
    }

    public List<Diagnostic<? extends JavaFileObject>> getDiagnostics() {
        checkActive();
        return diagnostics;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                boolean captureDiagnosticsStart = captureDiagnostics;
                try {
                    setUp(description.getClassName(), description.getMethodName());
                    base.evaluate();
                } finally {
                    cleanUp();
                    captureDiagnostics = captureDiagnosticsStart;
                }
            }
        };
    }

    private void setUp(String className, String methodName) {
        active = true;
        diagnostics = new ArrayList<>();

        final File testRoot = methodNameDir(classNameDir(rootDirectory(), className), methodName);
        generateDir = new File(testRoot, "generate");
        compileDir = new File(testRoot, "compile");
    }

    private void cleanUp() {
        generateDir = null;
        compileDir = null;
        sourceDirInitialized = false;
        classesDirInitialized = false;
        diagnostics = null;
        active = false;
    }

    @Override
    public void afterAll(ExtensionContext context) {
        if (isClassRule) {
            cleanUp();
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        if (!isClassRule) {
            return;
        }
        setUp(context.getRequiredTestClass().getName(), null);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (!isClassRule) {
            cleanUp();
        }
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        if (isClassRule) {
            return;
        }
        final String displayName = StringUtils.removeEnd(context.getDisplayName(), "()");
        String methodName = context.getRequiredTestMethod().getName();
        if (!StringUtils.equals(displayName, methodName)) {
            methodName = methodName + "[" + displayName + "]";
        }
        setUp(context.getRequiredTestClass().getName(), methodName);
    }

    public File generate(String schema, String targetPackage) {
        return generate(schema, targetPackage, emptyConfig());
    }

    public File generate(URL schema, String targetPackage) {
        return generate(schema, targetPackage, emptyConfig());
    }

    public File generate(String schema, String targetPackage, Map<String, Object> configValues) {
        return generate(schemaUrl(schema), targetPackage, configValues);
    }

    public File generate(final URL schema, final String targetPackage, final Map<String, Object> configValues) {
        CodeGenerationHelper.generate(schema, targetPackage, configValues, getGenerateDir());
        return generateDir;
    }

    public ClassLoader compile() {
        return compile(emptyClasspath(), emptyConfig());
    }

    public ClassLoader compile(List<File> classpath) {
        return compile(classpath, emptyConfig());
    }

    public ClassLoader compile(List<File> classpath, Map<String, Object> config) {
        return compile(systemJavaCompiler(), null, classpath, config);
    }

    public ClassLoader compile(JavaCompiler compiler, Writer out, List<File> classpath, Map<String, Object> config) {
        DiagnosticListener<JavaFileObject> diagnosticListener = captureDiagnostics ? new CapturingDiagnosticListener() : null;
        return CodeGenerationHelper.compile(compiler, out, getGenerateDir(), getCompileDir(), classpath, config, diagnosticListener);
    }

    public ClassLoader generateAndCompile(String schema, String targetPackage, Map<String, Object> configValues) {
        generate(schema, targetPackage, configValues);
        return compile(emptyClasspath(), configValues);
    }

    public ClassLoader generateAndCompile(String schema, String targetPackage) {
        generate(schema, targetPackage);
        return compile();
    }

    public ClassLoader generateAndCompile(URL schema, String targetPackage) {
        generate(schema, targetPackage);
        return compile(emptyClasspath(), emptyConfig());
    }

    public ClassLoader generateAndCompile(URL schema, String targetPackage, Map<String, Object> configValues) {
        generate(schema, targetPackage, configValues);
        return compile(emptyClasspath(), configValues);
    }

    public File generated(String relativeSourcePath) {
        return new File(generateDir, relativeSourcePath);
    }

    private void checkActive() {
        if (active != true) {
            throw new IllegalStateException("cannot access Jsonschema2PojoRule state when inactive");
        }
    }

    class CapturingDiagnosticListener implements DiagnosticListener<JavaFileObject> {
        @Override
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            diagnostics.add(diagnostic);
        }
    }

    private static List<File> emptyClasspath() {
        return new ArrayList<>();
    }

    private static Map<String, Object> emptyConfig() {
        return new HashMap<>();
    }

    private static URL schemaUrl(String schema) {
        URL schemaUrl = Jsonschema2PojoRule.class.getResource(schema);
        assertThat("Unable to read schema resource from the classpath: " + schema, schemaUrl, is(notNullValue()));
        return schemaUrl;
    }

    static File rootDirectory() {
        return new File("target" + File.separator + "jsonschema2pojo");
    }

    static File classNameDir(File baseDir, String className) {
        return new File(baseDir, classNameToPath(className));
    }

    static final Pattern methodNamePattern = compilePattern("\\A([^\\[]+)(?:\\[(.*)\\])?\\Z");

    /**
     * Returns the compiled pattern, or null if the pattern could not compile.
     */
    static Pattern compilePattern(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            System.err.println("Could not compile pattern " + pattern);
            e.printStackTrace(System.err);
            return null;
        }
    }

    static File methodNameDir(File baseDir, String methodName) {
        if (methodName == null)
            methodName = "class";
        Matcher matcher = methodNamePattern.matcher(methodName);

        if (matcher.matches()) {
            if (matcher.group(2) != null) {
                baseDir = new File(baseDir, safeDirName(matcher.group(2)));
            }
            return new File(baseDir, safeDirName(matcher.group(1)));
        } else {
            throw new IllegalArgumentException("cannot transform methodName (" + methodName + ") into path");
        }
    }

    static boolean ensureDirectoryInitialized(File dir, boolean isInitialized) {
        if (!isInitialized) {
            try {
                forceMkdir(dir);
                cleanDirectory(dir);
            } catch (IOException ioe) {
                throw new RuntimeException("could not clean directory", ioe);
            }
        }
        return true;
    }

    static String safeDirName(String label) {
        return label.replaceAll("[^a-zA-Z1-9]+", "_");
    }

    static String classNameToPath(String className) {
        return className
                .replaceAll("\\A(?:.*\\.)?([^.]*)\\Z", "$1")
                .replaceAll("\\$", Pattern.quote(File.separator));
    }

}
