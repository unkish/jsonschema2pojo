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

package org.jsonschema2pojo.integration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;

import org.jsonschema2pojo.integration.util.Jsonschema2PojoRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaClass;

/*
  Enums are treated differently to schemas of type object and we want to ensure that a description
  added to root-level enums is added to the javadoc.
 */
public class DescriptionEnumIT {

    @RegisterExtension public static Jsonschema2PojoRule schemaRule = new Jsonschema2PojoRule();

    private static JavaClass classWithDescription;

    @BeforeAll
    public static void generateClasses() throws IOException {

        schemaRule.generateAndCompile("/schema/description/descriptionEnum.json", "com.example");
        File generatedJavaFile = schemaRule.generated("com/example/DescriptionEnum.java");

        JavaProjectBuilder javaDocBuilder = new JavaProjectBuilder();
        javaDocBuilder.addSource(generatedJavaFile);

        classWithDescription = javaDocBuilder.getClassByName("com.example.DescriptionEnum");
    }

    @Test
    public void descriptionAppearsInEnumJavadoc() {

        String javaDocComment = classWithDescription.getComment();

        assertThat(javaDocComment, containsString("A description for this enum"));

    }

}
