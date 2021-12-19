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
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.codemodel.JAnnotationUse;
import com.sun.codemodel.JFieldVar;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Tests {@link MinimumMaximumRuleTest}
 */
@ExtendWith(MockitoExtension.class)
class MinimumMaximumRuleTest {

    private final ObjectNode node = JsonNodeFactory.instance.objectNode();
    private Class<? extends Annotation> decimalMaxClass;
    private Class<? extends Annotation> decimalMinClass;
    private MinimumMaximumRule rule;
    @Mock
    private GenerationConfig config;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private JFieldVar fieldVar;
    @Mock
    private JAnnotationUse annotationMax;
    @Mock
    private JAnnotationUse annotationMin;

    static Stream<Arguments> applicableTypes() {
        // After removal of javax.validation support given method should return Stream<Class<?>>
        return Stream.of(BigDecimal.class, BigInteger.class, String.class, Byte.class, Short.class, Integer.class, Long.class)
                .flatMap(o -> Stream.of(true, false).map(b -> Arguments.of(o, b)));
    }

    private void setMinMaxClasses(boolean useJakartaValidation) {
        // After removal of javax.validation support decimalMaxClass and decimalMinClass can be removed/inlined
        if (useJakartaValidation) {
            decimalMaxClass = DecimalMax.class;
            decimalMinClass = DecimalMin.class;
        } else {
            decimalMaxClass = javax.validation.constraints.DecimalMax.class;
            decimalMinClass = javax.validation.constraints.DecimalMin.class;
        }
    }

    @BeforeEach
    void setUp() {
        rule = new MinimumMaximumRule(new RuleFactory(config, new NoopAnnotator(), new SchemaStore()));
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMinimum(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setMinMaxClasses(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = RandomUtils.nextInt();
        node.put("minimum", minValue);
        when(fieldVar.annotate(decimalMinClass)).thenReturn(annotationMin);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(decimalMinClass);
        verify(annotationMin).param("value", Integer.toString(minValue));
        verify(fieldVar, never()).annotate(decimalMaxClass);
        verifyNoInteractions(annotationMax);
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMaximum(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setMinMaxClasses(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int maxValue = RandomUtils.nextInt();
        node.put("maximum", maxValue);
        when(fieldVar.annotate(decimalMaxClass)).thenReturn(annotationMax);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(decimalMaxClass);
        verify(annotationMax).param("value", Integer.toString(maxValue));
        verify(fieldVar, never()).annotate(decimalMinClass);
        verifyNoInteractions(annotationMin);
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testMaximumAndMinimum(Class<?> fieldClass, boolean useJakartaValidation) {
        when(config.isUseJakartaValidation()).thenReturn(useJakartaValidation);
        setMinMaxClasses(useJakartaValidation);
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        final int minValue = RandomUtils.nextInt();
        final int maxValue = RandomUtils.nextInt();
        when(fieldVar.annotate(decimalMinClass)).thenReturn(annotationMin);
        when(fieldVar.annotate(decimalMaxClass)).thenReturn(annotationMax);
        node.put("minimum", minValue);
        node.put("maximum", maxValue);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar).annotate(decimalMinClass);
        verify(annotationMin).param("value", Integer.toString(minValue));
        verify(fieldVar).annotate(decimalMaxClass);
        verify(annotationMax).param("value", Integer.toString(maxValue));
    }

    static Stream<Class<?>> nonApplicableTypes() {
        return Stream.of(Float.class, Double.class);
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeHasMinimum_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);

        node.put("minimum", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotationMin, annotationMax);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeHasMaximum_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);

        node.put("maximum", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotationMax, annotationMin);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("nonApplicableTypes")
    void nonApplicableType_nodeHasMinimumAndMaximum_ruleNotApplied(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);

        node.put("minimum", RandomUtils.nextInt());
        node.put("maximum", RandomUtils.nextInt());
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotationMin, annotationMax);
        verify(config, never()).isUseJakartaValidation();
    }

    @ParameterizedTest
    @MethodSource("applicableTypes")
    void testNotUsed(Class<?> fieldClass) {
        when(config.isIncludeJsr303Annotations()).thenReturn(true);
        when(fieldVar.type().boxify().fullName()).thenReturn(fieldClass.getTypeName());

        assertThat(node.get("minimum"), is(nullValue()));
        assertThat(node.get("maximum"), is(nullValue()));

        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verify(fieldVar, never()).annotate(ArgumentMatchers.<Class<? extends Annotation>>any());
        verifyNoInteractions(annotationMax, annotationMin);
        verify(config, never()).isUseJakartaValidation();
    }

    @Test
    void jsrDisable() {
        when(config.isIncludeJsr303Annotations()).thenReturn(false);
        JFieldVar result = rule.apply("node", node, null, fieldVar, null);
        assertThat(fieldVar, sameInstance(result));

        verifyNoInteractions(annotationMax, annotationMin, fieldVar);
        verify(config, never()).isUseJakartaValidation();
    }
}