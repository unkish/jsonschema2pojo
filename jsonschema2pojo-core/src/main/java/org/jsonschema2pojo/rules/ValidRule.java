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

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;

import org.jsonschema2pojo.Schema;
import org.jsonschema2pojo.model.JAnnotatedClass;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JType;

import jakarta.validation.Valid;

/**
 * Applies the {@code @Valid} annotation to non-container types that require cascading validation.
 * <p>
 * Container types (Collections, Maps) are not annotated — only their element/value types are,
 * via the recursive rule pipeline.
 */
public class ValidRule implements Rule<JType, JType> {

    private final RuleFactory ruleFactory;

    public ValidRule(RuleFactory ruleFactory) {
        this.ruleFactory = ruleFactory;
    }

    @Override
    public JType apply(String nodeName, JsonNode node, JsonNode parent, JType type, Schema currentSchema) {

        if (ruleFactory.getGenerationConfig().isIncludeJsr303Annotations()
            && type instanceof JClass jclass
            && !isContainer(jclass)) {
            return JAnnotatedClass.of(jclass).annotated(getValidClass());
        } else {
            return type;
        }
    }

    private boolean isContainer(JClass jclass) {
        JClass erasure = jclass.erasure();
        return jclass.owner().ref(Collection.class).isAssignableFrom(erasure)
            || jclass.owner().ref(Map.class).isAssignableFrom(erasure);
    }

    private Class<? extends Annotation> getValidClass() {
        return ruleFactory.getGenerationConfig().isUseJakartaValidation()
                ? Valid.class
                : javax.validation.Valid.class;
    }

}
