/*
 * MIT License
 *
 * Copyright (c) 2021
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.wickr.java.util;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtils {
    private static final ObjectMapper defaultMapper = createMapper();

    public static <T> String fromEntity(final T entity) throws JsonProcessingException {
        if (null == entity) {
            return "{}";
        }
        return defaultMapper.writeValueAsString(entity);
    }

    public static <T> void toFile(final T entity, final File file) throws IOException {
        defaultMapper.writer().writeValue(file, entity);
    }

    public static TreeNode toTree(final String json) throws JsonProcessingException {
        if (null == json || json.isEmpty()) {
            return null;
        }
        return defaultMapper.readTree(json);
    }

    public static <T> T toEntity(final String json, final Class<T> clazz) throws JsonProcessingException {
        if (null == json || json.isEmpty()) {
            return null;
        }
        return defaultMapper.readValue(json, clazz);
    }

    public static <T> T toEntity(final InputStream input, final Class<T> clazz) throws IOException {
        if (null == input) {
            return null;
        }
        try (final Reader reader = new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return defaultMapper.readerFor(clazz).readValue(reader);
        }
    }

    public static <T> T toEntity(final File file, final Class<T> clazz) throws IOException {
        if (null == file) {
            return null;
        }
        return defaultMapper.readerFor(clazz).readValue(file);
    }

    public static <T> List<T> toEntityList(final String json, final Class<T> clazz) throws JsonProcessingException {
        if (null == json || json.isEmpty()) {
            return null;
        }
        return defaultMapper.readerForListOf(clazz).readValue(json);
    }

    public static String fromMap(final Map<String, ?> map) {
        if (null == map || map.isEmpty()) {
            return "{}";
        }
        try {
            return defaultMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to convert map [size=" + map.size() + "] to json.", e);
        }
    }

    public static String successMessage() {
        return statusMessage("success", null);
    }

    public static String successMessage(final String msg) {
        return statusMessage("success", msg);
    }

    public static String errorMessage(final String msg) {
        return statusMessage("error", msg);
    }

    public static String statusMessage(final String status, final String message) {
        final Map<String, String> map = new HashMap<>(2);
        map.put("status", status);
        if (message != null && !message.isBlank()) {
            map.put("message", message);
        }
        try {
            return defaultMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to convert status message to json.", e);
        }
    }

    private static ObjectMapper createMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setVisibility(mapper.getSerializationConfig().getDefaultVisibilityChecker()
                .withFieldVisibility(JsonAutoDetect.Visibility.ANY)
                .withGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withIsGetterVisibility(JsonAutoDetect.Visibility.NONE)
                .withSetterVisibility(JsonAutoDetect.Visibility.NONE));
        return mapper;
    }

    private JsonUtils() {

    }
}
