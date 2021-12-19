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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

/**
 * Tests {@link DigitsRuleTest}
 */
public class DigitsRuleTest {

    private DigitsRule rule;
    private Class<? extends Annotation> digitsClass;
    private Class<? extends Annotation> sizeClass;
    @Mock
    private GenerationConfig config;
    @Mock
    private JsonNode node;
    @Mock
    private JsonNode subNodeInteger;
    @Mock
    private JsonNode subNodeFractional;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotation;

    public static Collection<Object[]> data() {
        return asList(new Object[][] {
                { true, BigDecimal.class },
                { true, BigInteger.class },
                { true, String.class },
                { true, Byte.class },
                { true, Short.class },
                { true, Integer.class },
                { true, Long.class },
                { false, Float.class },
                { false, Double.class },
        }).stream()
                .flatMap(o -> Stream.of(true, false).map(b -> Stream.concat(stream(o), Stream.of(b)).toArray()))
                .collect(Collectors.toList());
    }

    private void setUpJakartaValidationAndAnnotationClasses(boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        if (useJakartaValidation) {
            digitsClass = Digits.class;
            sizeClass = Size.class;
        } else {
            digitsClass = javax.validation.constraints.Digits.class;
            sizeClass = javax.validation.constraints.Size.class;
        }
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        rule = new DigitsRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testHasIntegerAndFractionalDigits(boolean isApplicable, Class<?> fieldClass, boolean useJakartaValidation) {
        setUpJakartaValidationAndAnnotationClasses(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int intValue = new Random().nextInt();
        final int fractionalValue = new Random().nextInt();

        when(subNodeInteger.asInt()).thenReturn(intValue);
        when(subNodeFractional.asInt()).thenReturn(fractionalValue);
        when(node.get("integerDigits")).thenReturn(subNodeInteger);
        when(node.get("fractionalDigits")).thenReturn(subNodeFractional);
        when(fieldVar.annotate(digitsClass)).thenReturn(annotation);
        when(node.has("integerDigits")).thenReturn(true);
        when(node.has("fractionalDigits")).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, times(isApplicable ? 1 : 0)).annotate(digitsClass);
        verify(annotation, times(isApplicable ? 1 : 0)).param("integer", intValue);
        verify(annotation, times(isApplicable ? 1 : 0)).param("fraction", fractionalValue);
    }

    @ParameterizedTest
    @MethodSource("data")
    public void testNotUsed(boolean isApplicable, Class<?> fieldClass, boolean useJakartaValidation) {
        setUpJakartaValidationAndAnnotationClasses(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        when(node.has("integerDigits")).thenReturn(false);
        when(node.has("fractionalDigits")).thenReturn(false);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, never()).annotate(sizeClass);
        verify(annotation, never()).param(anyString(), anyInt());
    }

    @Test
    public void jsrDisable() {
        when(config.isIncludeJsr303Annotations()).thenReturn(false);
        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verify(annotation, never()).param(anyString(), anyInt());
    }

}