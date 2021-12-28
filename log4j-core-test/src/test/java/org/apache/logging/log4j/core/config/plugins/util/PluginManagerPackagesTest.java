/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package org.apache.logging.log4j.core.config.plugins.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.ConfigurationFactory;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.status.StatusLogger;
import org.apache.logging.log4j.core.test.appender.ListAppender;
import org.apache.logging.log4j.util.Strings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PluginManagerPackagesTest {
    private static final String TEST_SOURCE = "target/test-classes/customPlugin/FixedStringLayout.java";
    private static Configuration config;
    private static ListAppender listAppender;
    private static LoggerContext ctx;

    @AfterAll
    public static void cleanupClass() {
        System.clearProperty(ConfigurationFactory.CONFIGURATION_FILE_PROPERTY);
        if (ctx != null) {
            ctx.reconfigure();
        }
        StatusLogger.getLogger().reset();
    }

    @AfterAll
    public static void afterClass() {
        File file = new File(TEST_SOURCE);
        File parent = file.getParentFile();
        if (file.exists()) {
            file.delete();
        }
        file = new File(parent, "FixedStringLayout.class");
        if (file.exists()) {
            file.delete();
        }
    }

    @Test
    public void test() throws Exception {

        // To ensure our custom plugin is NOT included in the log4j plugin metadata file,
        // we make sure the class does not exist until after the build is finished.
        // So we don't create the custom plugin class until this test is run.
        final File orig = new File(TEST_SOURCE + ".source");
        final File f = new File(orig.getParentFile(), "FixedStringLayout.java");
        assertTrue(orig.renameTo(f), "renamed source file failed");
        compile(f);
        assertTrue(f.renameTo(orig), "reverted source file failed");

        // load the compiled class
        Class.forName("customplugin.FixedStringLayout");

        // now that the custom plugin class exists, we load the config
        // with the packages element pointing to our custom plugin
        ctx = Configurator.initialize("Test1", "customplugin/log4j2-741.xml");
        config = ctx.getConfiguration();
        listAppender = config.getAppender("List");

        final Logger logger = LogManager.getLogger(PluginManagerPackagesTest.class);
        logger.info("this message is ignored");

        final List<String> messages = listAppender.getMessages();
        assertEquals(1, messages.size(), messages.toString());
        assertEquals("abc123XYZ", messages.get(0));
    }

    static void compile(final File f) throws IOException {
        // set up compiler
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        final DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        final List<String> errors = new ArrayList<>();
        try (final StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, null)) {
            final Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjectsFromFiles(
                    Collections.singletonList(f));
            String classPath = System.getProperty("jdk.module.path");
            List<String> options = new ArrayList<>();
            if (Strings.isNotBlank(classPath)) {
                options.add("-classpath");
                options.add(classPath);
            }
            options.add("-proc:none");
            // compile generated source
            compiler.getTask(null, fileManager, diagnostics, options, null, compilationUnits).call();

            // check we don't have any compilation errors
            for (final Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                    errors.add(String.format("Compile error: %s%n", diagnostic.getMessage(Locale.getDefault())));
                }
            }
        } catch (RuntimeException ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        assertTrue(errors.isEmpty(), errors.toString());
    }
}
