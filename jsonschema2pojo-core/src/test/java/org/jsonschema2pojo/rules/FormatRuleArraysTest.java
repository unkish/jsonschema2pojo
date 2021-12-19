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

package org.jsonschema2pojo.rules;

import static java.util.Arrays.*;
import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Collections;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaStore;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.databind.node.TextNode;
import com.sun.codemodel.JCodeModel;
import com.sun.codemodel.JType;

class FormatRuleArraysTest {

    private final GenerationConfig config = mock(GenerationConfig.class);

    static Collection<Object[]> data() {
        return asList(new Object[][] {
                { "byte[]", byte[].class },
                { "java.lang.String[]", String[].class }});
    }

    @ParameterizedTest
    @MethodSource("data")
    void useArraysWithCustomTypeMapping(String formatValue, Class<?> expectedType) {
        when(config.getFormatTypeMapping()).thenReturn(Collections.singletonMap("test", formatValue));
        final FormatRule rule = new FormatRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
        JType result = rule.apply("fooBar", TextNode.valueOf("test"), null, new JCodeModel().ref(Object.class), null);

        assertThat(result.isArray(), is(true));

        JType expectedJType = new JCodeModel().ref(expectedType);

        assertThat(result.fullName(), equalTo(expectedJType.fullName()));
    }

}
