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

package com.wickr.java.impl;

import com.wickr.java.WickrBot;
import com.wickr.java.WickrComponent;
import com.wickr.java.WickrListener;
import com.wickr.java.model.Message;

/**
 * a thread which delegates to existing publishing worker
 *
 * @date 3/18/21.
 */
public class WickrEventDelegateThread extends Thread implements WickrListener, WickrComponent {
    private final WickrEventPublishingWorker worker;

    WickrEventDelegateThread(final WickrEventPublishingWorker worker) {
        this.worker = worker;
    }

    @Override
    public void run() {
        this.worker.run();
    }

    @Override
    public void shutdown() throws Exception {
        this.worker.shutdown();
        this.interrupt();
    }

    @Override
    public void messageReceived(WickrBot bot, Message message) {
        this.worker.messageReceived(bot, message);
    }
}
