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

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.Pattern;

/**
 * Tests {@link PatternRuleTest}
 */
@ExtendWith({ MockitoExtension.class })
class PatternRuleTest {

    private PatternRule rule;
    @Mock
    private GenerationConfig config;
    @Mock
    private JsonNode node;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotation;

    static Stream<Arguments> applicableTypes() {
        // After removal of javax.validation support given method should be replaced with @ValueSource(classes = {String.class})
        return Stream.of(String.class).flatMap(o -> Stream.of(true, false).map(b -> Arguments.of(b, o)));
    }

    @BeforeEach
    void setUp() {
        rule = new PatternRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testRegex(boolean isUseJakartaValidation, Class<?> fieldClass) {
        when(config.isUseJakartaValidation()).thenReturn(isUseJakartaValidation);
        Class<? extends Annotation> patternClass = isUseJakartaValidation
                ? Pattern.class
                : javax.validation.constraints.Pattern.class;
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final String patternValue = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";

        when(node.asText()).thenReturn(patternValue);
        when(fieldVar.annotate(patternClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(patternClass);
        verify(annotation).param("regexp", patternValue);
    }

    @ParameterizedTest
    @ValueSource(classes = { UUID.class, Collection.class, Map.class, Array.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class })
    void nonApplicableType_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotation);
        verify(config, never()).isUseJakartaValidation();
    }

    @Test
    void jsrDisable() {
        when(config.isIncludeJsr303Annotations()).thenReturn(false);
        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verifyNoInteractions(annotation, fieldVar);
        verify(config, never()).isUseJakartaValidation();
    }

}