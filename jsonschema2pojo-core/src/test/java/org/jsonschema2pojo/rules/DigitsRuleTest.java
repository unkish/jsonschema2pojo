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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.stream.Stream;

import org.apache.commons.lang3.RandomUtils;
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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.Digits;

/**
 * Tests {@link DigitsRuleTest}
 */
@ExtendWith(MockitoExtension.class)
class DigitsRuleTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private DigitsRule rule;
    @Mock
    private GenerationConfig config;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotation;

    static Stream<Arguments> applicableTypes() {
        // After removal of javax.validation support given method should either return Stream<Class<?>> or be replaced with
        // @ValueSource(classes = {BigDecimal.class, BigInteger.class, String.class, Byte.class, Short.class, Integer.class, Long.class})
        return Stream.of(BigDecimal.class, BigInteger.class, String.class, Byte.class, Short.class, Integer.class, Long.class)
                .flatMap(o -> Stream.of(true, false).map(b -> Arguments.of(o, b)));
    }

    @BeforeEach
    void setUp() {
        rule = new DigitsRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testHasIntegerAndFractionalDigits(Class<?> fieldClass, boolean useJakartaValidation) {
        final Class<? extends Annotation> digitsClass = useJakartaValidation
                ? Digits.class
                : javax.validation.constraints.Digits.class;
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int intValue = RandomUtils.nextInt();
        final int fractionalValue = RandomUtils.nextInt();

        node.put("integerDigits", intValue);
        node.put("fractionalDigits", fractionalValue);
        when(fieldVar.annotate(digitsClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(digitsClass);
        verify(annotation).param("integer", intValue);
        verify(annotation).param("fraction", fractionalValue);
    }

    @ParameterizedTest
    @ValueSource(classes = { Float.class, Double.class })
    void nonApplicableType_nodeHasIntegerAndFractionalDigits_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);

        node.put("integerDigits", RandomUtils.nextInt());
        node.put("fractionalDigits", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotation);
        verify(config, never()).isUseJakartaValidation();
    }

    @Test
    void testNotUsed() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verifyNoInteractions(annotation, fieldVar);
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