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
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.Size;

/**
 * Tests {@link MinLengthMaxLengthRuleTest}
 */
@ExtendWith(MockitoExtension.class)
class MinLengthMaxLengthRuleTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private MinLengthMaxLengthRule rule;
    private Class<? extends Annotation> sizeClass;
    @Mock
    private GenerationConfig config;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotation;

    static Stream<Arguments> applicableTypes() {
        // After removal of javax.validation support given method should return Stream<Class<?>>
        return Stream.of(String.class, Collection.class, Map.class, Array.class)
                .flatMap(o -> Stream.of(true, false).map(b -> Arguments.of(o, b)));
    }

    static Stream<Class<?>> nonApplicableTypes() {
        return Stream.of(Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class);
    }

    @BeforeEach
    void setUp() {
        rule = new MinLengthMaxLengthRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
    }

    private void setSizeClass(boolean useJakartaValidation) {
        // After removal of javax.validation support sizeClass can be removed/inlined
        sizeClass = useJakartaValidation ? Size.class : javax.validation.constraints.Size.class;
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMinLength(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setSizeClass(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = RandomUtils.nextInt();
        node.put("minLength", minValue);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(sizeClass);
        verify(annotation).param("min", minValue);
        verify(annotation, never()).param(eq("max"), anyString());
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeWithMinLength_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        node.put("minLength", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotation);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMaxLength(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setSizeClass(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int maxValue = RandomUtils.nextInt();
        node.put("maxLength", maxValue);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(sizeClass);
        verify(annotation).param("max", maxValue);
        verify(annotation, never()).param(eq("min"), anyInt());
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeWithMaxLength_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        node.put("maxLength", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotation);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMaxAndMinLength(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setSizeClass(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = RandomUtils.nextInt();
        final int maxValue = RandomUtils.nextInt();
        node.put("minLength", minValue);
        node.put("maxLength", maxValue);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(sizeClass);
        verify(annotation).param("min", minValue);
        verify(annotation).param("max", maxValue);
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeWithMaxLengthAndMinLength_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        node.put("minLength", RandomUtils.nextInt());
        node.put("maxLength", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotation);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMaxAndMinLengthGenericsOnType(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setSizeClass(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = RandomUtils.nextInt();
        final int maxValue = RandomUtils.nextInt();
        node.put("minLength", minValue);
        node.put("maxLength", maxValue);
        when(fieldVar.annotate(sizeClass)).thenReturn(annotation);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName() + "<String>");

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(sizeClass);
        verify(annotation).param("min", minValue);
        verify(annotation).param("max", maxValue);
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

        verifyNoInteractions(fieldVar, annotation);
        verify(config, never()).isUseJakartaValidation();
    }
}