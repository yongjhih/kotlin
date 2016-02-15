/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.diagnostics.rendering

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.NameShortness
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.contains
import java.util.*

class SmartTypeRenderer(private val basicRenderer: DescriptorRenderer) : SmartRenderer<KotlinType> {
    override fun render(obj: KotlinType, context: RenderingContext): String {
        val classifiers = classifiersMentionedByType(context)
        val byName = classifiers.groupBy { it.name }
        val smartRenderer = basicRenderer.withOptions {
            nameShortness = object : NameShortness {
                override fun renderClassifier(classifier: ClassifierDescriptor, renderer: DescriptorRenderer): String {
                    val uniqueName = (byName[classifier.name]?.size ?: 0) > 1
                    return when {
                    //TODO_R:
                        uniqueName -> NameShortness.SHORT.renderClassifier(classifier, renderer)
                        classifier is ClassDescriptor -> NameShortness.FULLY_QUALIFIED.renderClassifier(classifier, renderer)
                        classifier is TypeParameterDescriptor -> NameShortness.FULLY_QUALIFIED.renderClassifier(classifier, renderer)
                        else -> error("Unexpected blabla")
                    }
                }

            }
        }
        return smartRenderer.renderType(obj)
    }

    private fun classifiersMentionedByType(context: RenderingContext): Set<ClassifierDescriptor> {
        return context.compute(KEY) { objects ->
            val result = LinkedHashSet<ClassifierDescriptor>()
            objects.filterIsInstance<KotlinType>().forEach { diagnosticType ->
                diagnosticType.contains {
                    innerType ->
                    innerType.constructor.declarationDescriptor?.let { result.add(it) }
                    false
                }
            }
            result
        }
    }

    companion object {
        private val KEY = RenderingContext.Key<Set<ClassifierDescriptor>>("SMART_TYPE_RENDERER")
    }
}