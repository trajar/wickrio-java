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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * common utilities for managing cert and pk12 files
 *
 * @date 3/21/21.
 */
public class CertUtils {
    private static final Logger logger = LoggerFactory.getLogger(CertUtils.class);

    public static File convertPEMToPKCS12(final File certFile, final File keyFile, final String keyPassword) throws IOException, InterruptedException {
        final String basename = FilenameUtils.getBaseName(certFile.getName());
        final File destFile = new File(certFile.getParent(), basename + ".p12");
        convertPEMToPKCS12(certFile, keyFile, keyPassword, destFile, "");
        return destFile;
    }

    public static void convertPEMToPKCS12(
            final File certFile, final File keyFile, final String keyPassword,
            final File destFile, final String destPassword) throws IOException, InterruptedException {
        if (destFile.exists()) {
            FileUtils.deleteQuietly(destFile);
        }
        // openssl pkcs12 -export -out aps.p12 -inkey wickr_bot.key -in wickr_bot.cert -password pass:asdf
        final int result = executeShell(
                "openssl", "pkcs12", "-export",
                "-in", certFile.getCanonicalPath(),
                "-inkey", keyFile.getCanonicalPath(),
                "-out", destFile.getCanonicalPath(),
                "-passin", "pass:" + keyPassword,
                "-passout", "pass:" + destPassword);
        if (!destFile.exists()) {
            throw new IOException("Unable to covert file to pkcs12 format (file not created), result from openssl command [" + result + "].");
        } else if (result != 0) {
            throw new IOException("Unable to covert file to pkcs12 format (bad exit code), result from openssl command [" + result + "].");
        }
    }

    private static int executeShell(final String... args) throws IOException, InterruptedException {
        return executeShell(Arrays.asList(args));
    }

    private static int executeShell(final List<String> args) throws IOException, InterruptedException {
        final Process proc = new ProcessBuilder()
                .command(args)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start();
        if (!proc.isAlive() || proc.waitFor(30, TimeUnit.SECONDS)) {
            return proc.exitValue();
        } else {
            logger.warn("Unable to wait for executed command " + args + ".");
            return -1;
        }
    }

    private CertUtils() {

    }
}
