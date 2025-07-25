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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jsonschema2pojo.GenerationConfig;
import org.jsonschema2pojo.NoopAnnotator;
import org.jsonschema2pojo.SchemaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.Size;

/**
 * Tests {@link MinLengthMaxLengthRuleTest}
 */
@ParameterizedClass
@MethodSource("data")
public class MinLengthMaxLengthRuleTest {

    private final boolean isApplicable;
    private MinLengthMaxLengthRule rule;
    private final Class<?> fieldClass;
    private final boolean useJakartaValidation;
    private final Class<? extends Annotation> sizeClass;
    @Mock
    private GenerationConfig config;
    @Mock
    private JsonNode node;
    @Mock
    private JsonNode subNode;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotation;

    public static Collection<Object[]> data() {
        return asList(new Object[][] {
                { true, String.class },
                { true, Collection.class },
                { true, Map.class },
                { true, Array.class },
                { false, Byte.class },
                { false, Short.class },
                { false, Integer.class },
                { false, Long.class },
                { false, Float.class },
                { false, Double.class },
        }).stream()
                .flatMap(o -> Stream.of(true, false).map(b -> Stream.concat(stream(o), Stream.of(b)).toArray()))
                .collect(Collectors.toList());
    }

    public MinLengthMaxLengthRuleTest(boolean isApplicable, Class<?> fieldClass, boolean useJakartaValidation) {
        this.isApplicable = isApplicable;
        this.fieldClass = fieldClass;
        this.useJakartaValidation = useJakartaValidation;
        this.sizeClass = useJakartaValidation ? Size.class : javax.validation.constraints.Size.class;
    }

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        rule = new MinLengthMaxLengthRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
    }

    @Test
    public void testMinLength() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = new Random().nextInt();
        when(subNode.asInt()).thenReturn(minValue);
        when(node.get("minLength")).thenReturn(subNode);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(node.has("minLength")).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, times(isApplicable ? 1 : 0)).annotate(sizeClass);
        verify(annotation, times(isApplicable ? 1 : 0)).param("min", minValue);
        verify(annotation, never()).param(eq("max"), anyString());
    }

    @Test
    public void testMaxLength() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int maxValue = new Random().nextInt();
        when(subNode.asInt()).thenReturn(maxValue);
        when(node.get("maxLength")).thenReturn(subNode);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(node.has("maxLength")).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, times(isApplicable ? 1 : 0)).annotate(sizeClass);
        verify(annotation, times(isApplicable ? 1 : 0)).param("max", maxValue);
        verify(annotation, never()).param(eq("min"), anyInt());
    }

    @Test
    public void testMaxAndMinLength() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = new Random().nextInt();
        final int maxValue = new Random().nextInt();
        JsonNode maxSubNode = Mockito.mock(JsonNode.class);
        when(subNode.asInt()).thenReturn(minValue);
        when(maxSubNode.asInt()).thenReturn(maxValue);
        when(node.get("minLength")).thenReturn(subNode);
        when(node.get("maxLength")).thenReturn(maxSubNode);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(node.has("minLength")).thenReturn(true);
        when(node.has("maxLength")).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, times(isApplicable ? 1 : 0)).annotate(sizeClass);
        verify(annotation, times(isApplicable ? 1 : 0)).param("min", minValue);
        verify(annotation, times(isApplicable ? 1 : 0)).param("max", maxValue);
    }

    @Test
    public void testMaxAndMinLengthGenericsOnType() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = new Random().nextInt();
        final int maxValue = new Random().nextInt();
        JsonNode maxSubNode = Mockito.mock(JsonNode.class);
        when(subNode.asInt()).thenReturn(minValue);
        when(maxSubNode.asInt()).thenReturn(maxValue);
        when(node.get("minLength")).thenReturn(subNode);
        when(node.get("maxLength")).thenReturn(maxSubNode);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(node.has("minLength")).thenReturn(true);
        when(node.has("maxLength")).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName() + "<String>");

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertSame(fieldVar, result);

        verify(fieldVar, times(isApplicable ? 1 : 0)).annotate(sizeClass);
        verify(annotation, times(isApplicable ? 1 : 0)).param("min", minValue);
        verify(annotation, times(isApplicable ? 1 : 0)).param("max", maxValue);
    }

    @Test
    public void testNotUsed() {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        when(node.has("minLength")).thenReturn(false);
        when(node.has("maxLength")).thenReturn(false);
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

        verify(fieldVar, never()).annotate(sizeClass);
        verify(annotation, never()).param(anyString(), anyInt());
    }
}