/*
 * Copyright (C) 2019 Google LLC
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

package com.dscorp.ispadmin.util

/**
 * Represents a list of capture values from a LiveData.
 */
class LiveDataValueCapture<T> {

    val lock = Any()

    private val _values = mutableListOf<T?>()

    fun addValue(value: T?) = synchronized(lock) {
        _values += value
    }
}

