package com.wickr.java;

/**
 * a simple component of the wickr-io library that needs to be tracked and shutdown
 *
 * @date 3/18/21.
 */
public interface WickrComponent {
    void shutdown() throws Exception;
}
