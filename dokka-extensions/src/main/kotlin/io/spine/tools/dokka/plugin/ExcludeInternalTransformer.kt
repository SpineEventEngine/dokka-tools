/*
 * Copyright 2022, TeamDev. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Redistribution and use in source and/or binary forms, with or without
 * modification, must retain the above copyright notice and the following
 * disclaimer.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package io.spine.tools.dokka.plugin

import io.spine.annotation.Internal
import org.jetbrains.dokka.base.transformers.documentables.SuppressedByConditionDocumentableFilterTransformer
import org.jetbrains.dokka.model.Annotations
import org.jetbrains.dokka.model.Annotations.Annotation
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.properties.WithExtraProperties
import org.jetbrains.dokka.plugability.DokkaContext

/**
 * Excludes everything annotated with [Internal] from the resulting documentation.
 *
 * Works for packages, types, fields and methods.
 */
@Suppress("RedundantVisibilityModifier") // It is required by explicit API mode.
public class ExcludeInternalTransformer(dokkaContext: DokkaContext) :
    SuppressedByConditionDocumentableFilterTransformer(dokkaContext) {

    /**
     * Tells whether the passed [Documentable] should be excluded from the documentation.
     *
     * Returns `true` if the passed piece is annotated with the [Internal] annotation. Otherwise,
     * returns `false`.
     *
     * [WithExtraProperties] is a Dokka-interface that adds a map-field for extra properties.
     * The information about applied annotations is present in this map.
     *
     * Not every [Documentable] subclass implements this interface.
     */
    override fun shouldBeSuppressed(d: Documentable): Boolean {
        return (d is WithExtraProperties<*>) && hasInternalAnnotation(d)
    }

    private fun hasInternalAnnotation(annotated: WithExtraProperties<*>): Boolean =
        annotated.annotations().any(InternalAnnotationCheck::test)

    /**
     * Extracts annotations from a [WithExtraProperties] object.
     *
     * Information about annotations is found in the map-field `extra`, added when implementing
     * [WithExtraProperties].
     *
     * All values with the type of [Annotations], a container-class for all applied annotations,
     * are extracted from the map-field. Then plain lists of [Annotation] are extracted from
     * every [Annotations] object and merged into the result list.
    */
    private fun WithExtraProperties<*>.annotations(): List<Annotation> {

        return this.extra.allOfType<Annotations>().flatMap {
            it.directAnnotations.values.flatten()
        }
    }

    /**
     * Provides the method to check if an [Annotation] object represents [Internal] annotation.
     */
    private object InternalAnnotationCheck {
        private val c = Internal::class.java

        /**
         * Checks if provided object represents [Internal] annotation.
         *
         * Compares the package and the class extracted from the object with the package and the
         * class of the [Internal] annotation.
         */
        fun test(a: Annotation): Boolean {
            return a.dri.packageName == c.`package`.name && a.dri.classNames == c.simpleName
        }
    }
}
