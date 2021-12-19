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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.jsonschema2pojo.integration.util.CodeGenerationHelper.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;

import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.rules.RuleFactory;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.experimental.runners.Enclosed;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JPackage;
import com.sun.codemodel.JType;

import edu.emory.mathcs.backport.java.util.Arrays;

public class Jsonschema2PojoRuleJunit5Test {

    public static class BrokenRuleFactory extends RuleFactory {
        @Override
        public org.jsonschema2pojo.rules.Rule<JPackage, JType> getObjectRule() {
            final org.jsonschema2pojo.rules.Rule<JPackage, JType> workingRule = super.getObjectRule();

            return (nodeName, node, parent, generatableType, currentSchema) -> {
                JType objectType = workingRule.apply(nodeName, node, null, generatableType, currentSchema);
                if( objectType instanceof JDefinedClass ) {
                    JDefinedClass jclass = (JDefinedClass)objectType;
                    jclass.method(JMod.PUBLIC, jclass.owner().BOOLEAN, "brokenMethod").body();
                }
                return objectType;
            };
        }
    }

    @Nested
    class MethodTests {

        @RegisterExtension
        public Jsonschema2PojoRule rule = new Jsonschema2PojoRule();

        @Test
        void sourcesWillCompile() {
            ClassLoader resultsClassLoader = rule.generateAndCompile("/schema/default/default.json", "com.example");
            assertDoesNotThrow(() -> resultsClassLoader.loadClass("com.example.Default"));
        }

        @Test
        void compilationProblemsStdErr() {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final PrintStream err = System.err;
            try(PrintStream errStream = new PrintStream(baos, true)) {
                System.setErr(errStream);
                rule.generateAndCompile("/schema/default/default.json", "com.example", config("customRuleFactory", BrokenRuleFactory.class.getName()));
            } catch( Throwable t ) {
            } finally {
                System.setErr(err);
            }
            assertThat(baos.toString(), containsString("return"));
        }

    }

    @Nested
    class ParameterizedTest {
        @RegisterExtension
        public Jsonschema2PojoRule rule = new Jsonschema2PojoRule();

        @org.junit.jupiter.params.ParameterizedTest
        @ValueSource(strings = {"label1", "label2", "../../../"})
        public void sourcesForLabelsWillCompile() throws ClassNotFoundException {
            ClassLoader resultsClassLoader = rule.generateAndCompile("/schema/default/default.json", "com.example");
            resultsClassLoader.loadClass("com.example.Default");
        }
    }
}
