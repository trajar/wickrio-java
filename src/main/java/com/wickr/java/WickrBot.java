package com.wickr.java;

import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.wickr.java.http.WickrAuthentication;
import com.wickr.java.model.*;
import com.wickr.java.util.HttpUtils;
import com.wickr.java.util.JsonUtils;
import com.wickr.java.util.StringUtils;
import org.apache.hc.client5.http.auth.AuthScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * the configuration for a wickr user bot
 *
 * @date 3/5/21.
 */
public class WickrBot implements Comparable<WickrBot> {

    public static WickrBot createForProvisioning(
            final String user, final String pwd,
            final String apiKey, final String apiToken) {
        return new WickrBot(user, pwd, apiKey, apiToken, null, -1, false);
    }

    public static WickrBot createForProvisioning(
            final String user, final String pwd) {
        return new WickrBot(user, pwd, null, null, null, -1, false);
    }

    public static WickrBot createForExisting(
            final String user,
            final String apiKey, final String apiToken,
            final String host, final int port, final boolean ssl) {
        return new WickrBot(user, null, apiKey, apiToken, host, port, ssl);
    }

    public static WickrBot createForExisting(final String user) {
        return new WickrBot(user, null, null, null, null, -1, false);
    }

    private static final Logger logger = LoggerFactory.getLogger(WickrBot.class);

    private final String user;

    private String password;

    private String apiKey;

    private String apiToken;

    private AuthScheme authentication;

    private int containerPort = -1;

    private String containerHost;

    private boolean useSSL = false;

    public WickrBot(
            final String user, final String pwd,
            final String apiKey, final String apiToken,
            final String host, final int port, final boolean ssl) {
        if (null == user || user.isBlank()) {
            throw new IllegalArgumentException("User cannot be null/empty.");
        }
        this.user = user;
        if (pwd != null) {
            this.withPassword(pwd);
        }
        if (apiKey != null || apiToken != null) {
            this.withApi(apiKey, apiToken);
        }
        if (host != null) {
            this.withContainer(host, port, ssl);
        }
    }

    public String getUser() {
        return user;
    }

    public boolean hasPassword() {
        if (null == this.password) {
            return false;
        }
        return !this.password.isBlank();
    }

    public String getPassword() {
        return password;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getApiToken() {
        return apiToken;
    }

    public int getContainerPort() {
        return containerPort;
    }

    public String getContainerHost() {
        return containerHost;
    }

    public boolean useSSL() {
        return useSSL;
    }

    public boolean isProvisioned() {
        if (null == this.apiKey || this.apiKey.isBlank()) {
            return false;
        }
        if (null == this.apiToken || this.apiToken.isBlank()) {
            return false;
        }
        if (null == this.containerHost || this.containerHost.isBlank()) {
            return false;
        }
        if (this.containerPort <= 0) {
            return false;
        }
        return true;
    }

    public WickrBot withPassword(final String pwd) {
        this.password = pwd;
        return this;
    }

    public WickrBot withContainer(final String host, final int port, final boolean ssl) {
        this.containerHost = host;
        this.containerPort = port;
        this.useSSL = ssl;
        return this;
    }

    public WickrBot withApi(final String apiKey, final String apiToken) {
        this.apiKey = apiKey;
        this.apiToken = apiToken;
        if (this.apiKey != null && this.apiToken != null) {
            this.authentication = new WickrAuthentication(apiToken);
        } else {
            this.authentication = null;
        }
        return this;
    }

    public boolean isAvailable() {
        try {
            final Statistics status = this.getStatistics();
            return status != null && !status.isEmpty();
        } catch (final Exception e) {
            return false;
        }
    }

    public void waitUntilAvailable() {
        this.waitUntilAvailable(-1);
    }

    public boolean waitUntilAvailable(final long timeoutMsec) {
        final long start = System.currentTimeMillis();
        final long sleepMsec = 2000;
        while (timeoutMsec <= 0 || (System.currentTimeMillis() - start) <= timeoutMsec) {
            try {
                final Statistics status = this.getStatistics();
                if (status != null && !status.isEmpty()) {
                    return true;
                }
            } catch (final IOException e) {
//              e.printStackTrace();
            }
            try {
                Thread.sleep(sleepMsec);
            } catch (final InterruptedException e) {
                return false;
            }
        }
        return false;
    }

    public Statistics getStatistics() throws IOException {
        try {
            final URI endpoint = this.getContainerUrlFor("/Statistics");
            final StatisticsResponse statusResponse = HttpUtils.getJson(endpoint, StatisticsResponse.class, this.authentication);
            return statusResponse != null ? statusResponse.getStatistics() : Statistics.createEmpty();
        } catch (final Exception e) {
            throw new IOException("Unable to query statistics for bot [" + this.user + "].", e);
        }
    }

    public void clearStatistics() throws IOException {
        try {
            final URI endpoint = this.getContainerUrlFor("/Statistics");
            final String response = HttpUtils.delete(endpoint, this.authentication);
            logger.debug("Statistics for [" + this + "] cleared, response was [" + response + "].");
        } catch (final Exception e) {
            throw new IOException("Unable to clear statistics for bot [" + this.user + "].", e);
        }
    }

    public List<Message> getUnreadMessages() throws IOException {
        return this.getUnreadMessages(-1, -1);
    }

    public List<Message> getUnreadMessages(final int start, final int count) throws IOException {
        final StringBuilder request = new StringBuilder("/Messages");
        boolean firstParam = true;
        if (start > 0) {
            request.append("?").append("start=").append(start);
            firstParam = false;
        }
        if (count > 0) {
            if (firstParam) {
                request.append("?");
            }
            request.append("count=").append(count);
        }
        try {
            // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/Messages?start=<index>&count=<number>
            final String json = HttpUtils.get(this.getContainerUrlFor(request.toString()), this.authentication);
            return JsonUtils.toEntityList(json, Message.class);
        } catch (final Exception e) {
            throw new IOException("Unable to get unread messages for bot [" + this.user + "].", e);
        }
    }

    public boolean sendMessage(final Message message) throws IOException {
        if (null == message) {
            return false;
        }
        try {
            final String response = HttpUtils.postJson(this.getContainerUrlFor("/Messages"), message, this.authentication);
            return response != null && !response.isBlank();
        } catch (final Exception e) {
            throw new IOException("Unable to send message for bot [" + this.user + "].", e);
        }
    }

    public boolean sendMessageToUser(final String message, final String... recipients) throws IOException {
        if (null == message || message.isBlank()) {
            return false;
        }
        return this.sendMessage(Message.createDirectMessage(message, recipients));
    }

    public boolean sendMessageToGroup(final String message, final String groupId) throws IOException {
        if (null == message || message.isBlank()) {
            return false;
        }
        return this.sendMessage(Message.createGroupMessage(message, groupId));
    }

    public boolean sendFileToUser(final File file, final String... recipients) throws IOException {
        if (null == file) {
            return false;
        }
        return this.sendFileToUsers(file, Arrays.asList(recipients));
    }

    public boolean sendFileToUsers(final File file, final Collection<String> users) throws IOException {
        return sendFileToUsersOrGroup(file, users, null);
    }

    public boolean sendFileToGroup(final File file, final String groupId) throws IOException {
        return this.sendFileToUsersOrGroup(file, Collections.emptyList(), groupId);
    }

    private boolean sendFileToUsersOrGroup(final File file, final Collection<String> users, final String groupId) throws IOException {
        return this.sendFileToUsersOrGroup(file, users, groupId, -1, -1);
    }

    private boolean sendFileToUsersOrGroup(final File file, final Collection<String> users, final String groupId, final int bor, final int ttl) throws IOException {
        if (null == file) {
            return false;
        }
        try {
            // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/File
            final Map<String, Object> request = new LinkedHashMap<>();
            if (groupId != null && !groupId.isBlank()) {
                request.put("vgroupid", groupId);
            } else {
                final Set<String> quotedNames = users.stream()
                        .filter((x) -> x != null && !x.isEmpty())
                        .map((x) -> "\"" + x + "\"")
                        .collect(Collectors.toSet());
                if (quotedNames.isEmpty()) {
                    return false;
                }
                request.put("users", "[" + StringUtils.join(",", quotedNames) + "]");
            }
            if (bor > 0) {
                request.put("bor", bor);
            }
            if (ttl > 0) {
                request.put("ttl", ttl);
            }
            request.put("attachment", file);
            final String response = HttpUtils.postForm(this.getContainerUrlFor("/File"), request, this.authentication);
            logger.debug("File [" + file + "] sent by [" + this + "], response was [" + response + "].");
            return response != null && !response.isBlank();
        } catch (final Exception e) {
            throw new IOException("Unable to create group conversation for bot [" + this.user + "].", e);
        }
    }

    public String createGroup(final String... members) throws IOException {
        return createGroup(Arrays.asList(members));
    }

    public String createGroup(final List<String> members) throws IOException {
        if (null == members || members.isEmpty()) {
            return null;
        }
        try {
            final List<User> users = members.stream().map(User::new).collect(Collectors.toList());
            if (users.isEmpty()) {
                throw new IllegalStateException("User list is empty, unable to create group.");
            }
            users.add(new User(this.user));
            final Map<String, Object> membersElement = new HashMap<>(1);
            final Map<String, Object> request = new HashMap<>(1);
            membersElement.put("members", new LinkedHashSet<>(users));
            request.put("groupconvo", membersElement);
            final String response = HttpUtils.postJson(this.getContainerUrlFor("/GroupConvo"), request, this.authentication);
            if (null == response || response.isBlank()) {
                return null;
            }
            final Map roomInfo = JsonUtils.toEntity(response, Map.class);
            final Object vgroupid = roomInfo.get("vgroupid");
            return vgroupid != null ? vgroupid.toString() : null;
        } catch (final Exception e) {
            throw new IOException("Unable to create group conversation for bot [" + this.user + "].", e);
        }
    }

    public List<Group> getGroups() throws IOException {
        try {
            final GroupList groupList = HttpUtils.getJson(this.getContainerUrlFor("/GroupConvo"), GroupList.class, this.authentication);
            if (null == groupList || groupList.getGroups().isEmpty()) {
                return Collections.emptyList();
            }
            return groupList.getGroups();
        } catch (final Exception e) {
            throw new IOException("Unable to query group conversations visible to bot [" + this.user + "].", e);
        }
    }

    public Group getGroup(final String vGroupID) throws IOException {
        try {
            return HttpUtils.getJson(this.getContainerUrlFor("/GroupConvo/" + vGroupID), Group.class, this.authentication);
        } catch (final Exception e) {
            throw new IOException("Unable to get group conversation [" + vGroupID + "] visible to bot [" + this.user + "].", e);
        }
    }

    public void deleteGroup(final String vGroupID) throws IOException {
        try {
            // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/GroupConvo/<vGroupID>
            final String response = HttpUtils.delete(this.getContainerUrlFor("/GroupConvo/" + vGroupID), this.authentication);
            logger.debug("Group conversation [" + vGroupID + "] deleted by [" + this + "], response was [" + response + "].");
        } catch (final Exception e) {
            throw new IOException("Unable to delete group conversation for bot [" + this.user + "].", e);
        }
    }

    public String createRoom(final Room room) throws IOException {
        if (null == room) {
            return null;
        }
        try {
            final Map<String, Object> request = new HashMap<>(1);
            request.put("room", room);
            final String response = HttpUtils.postJson(this.getContainerUrlFor("/Rooms"), request, this.authentication);
            if (null == response || response.isBlank()) {
                return null;
            }
            final Map roomInfo = JsonUtils.toEntity(response, Map.class);
            final Object vgroupid = roomInfo.get("vgroupid");
            return vgroupid != null ? vgroupid.toString() : null;
        } catch (final Exception e) {
            throw new IOException("Unable to create secure room for bot [" + this.user + "].", e);
        }
    }

    public List<Room> getRooms() throws IOException {
        try {
            final RoomList roomList = HttpUtils.getJson(this.getContainerUrlFor("/Rooms"), RoomList.class, this.authentication);
            if (null == roomList || roomList.getRooms().isEmpty()) {
                return Collections.emptyList();
            }
            return roomList.getRooms();
        } catch (final Exception e) {
            throw new IOException("Unable to query secure rooms visible to bot [" + this.user + "].", e);
        }
    }

    public Room getRoom(final String vGroupID) throws IOException {
        try {
            return HttpUtils.getJson(this.getContainerUrlFor("/Rooms/" + vGroupID), Room.class, this.authentication);
        } catch (final Exception e) {
            throw new IOException("Unable to get secure room [" + vGroupID + "] visible to bot [" + this.user + "].", e);
        }
    }

    public void leaveRoom(final String vGroupID) throws IOException {
        try {
            // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/Rooms/<vGroupID>?reason=leave
            final String response = HttpUtils.delete(this.getContainerUrlFor("/Rooms/" + vGroupID + "?reason=leave"), this.authentication);
            logger.debug("Room [" + vGroupID + "] left by [" + this + "], response was [" + response + "].");
        } catch (final Exception e) {
            throw new IOException("Unable to leave secure room for bot [" + this.user + "].", e);
        }
    }

    public void deleteRoom(final String vGroupID) throws IOException {
        try {
            // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/Rooms/<vGroupID>
            final String response = HttpUtils.delete(this.getContainerUrlFor("/Rooms/" + vGroupID), this.authentication);
            logger.debug("Room [" + vGroupID + "] deleted by [" + this + "], response was [" + response + "].");
        } catch (final Exception e) {
            throw new IOException("Unable to delete secure room for bot [" + this.user + "].", e);
        }
    }

    public String getEventCallback() throws Exception {
        final URI endpoint = this.getContainerUrlFor("/MsgRecvCallback");
        String json = HttpUtils.get(endpoint, this.authentication);
        if (null == json || json.isEmpty()) {
            throw new IllegalStateException("Found empty response from rest api.");
        } else if (json.endsWith(")")) {
            // fix bug with bad encoding of json response
            json = json.substring(0, json.length() - 1) + "}";
        }
        final TreeNode response = JsonUtils.toTree(json);
        final TreeNode node = response.get("callback");
        if (node instanceof TextNode) {
            final TextNode textNode = (TextNode) node;
            return textNode.textValue();
        } else {
            throw new IOException("Unexpected json response, could not find callback value.");
        }
    }

    public void clearEventCallback() throws Exception {
        final URI endpoint = this.getContainerUrlFor("/MsgRecvCallback");
        final String response = HttpUtils.delete(endpoint, this.authentication);
        logger.debug("Event callback for [" + this + "] cleared, response was [" + response + "].");
    }

    public void setEventCallback(final String url) throws Exception {
        // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/MsgRecvCallback?callbackurl=<url>
        final URI endpoint = this.getContainerUrlFor("/MsgRecvCallback?callbackurl=" + URLEncoder.encode(url, StandardCharsets.US_ASCII));
        final String response = HttpUtils.post(endpoint, this.authentication);
        logger.debug("Event callback for [" + this + "] set to [" + url + "], response was [" + response + "].");
    }

    private URI getContainerUrlFor(final String endpoint) {
        if (null == this.containerHost) {
            return null;
        }
        // https://<host>:<port>/WickrIO/V1/Apps/<API Key>/<endpoint>
        String url = (this.useSSL ? "https://" : "http://") + this.containerHost + (this.containerPort > 0 ? (":" + this.containerPort) : "") + "/WickrIO/V1/Apps/" + this.apiKey;
        if (!endpoint.startsWith("/")) {
            url += "/";
        }
        try {
            return new URI(url + endpoint);
        } catch (final URISyntaxException e) {
            throw new IllegalArgumentException("Unable to build container URL for endpoint.", e);
        }
    }

    @Override
    public String toString() {
        return this.user;
    }

    @Override
    public int compareTo(final WickrBot other) {
        if (null == other) {
            return 1;
        }
        return this.user.compareToIgnoreCase(other.user);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WickrBot wickrBot = (WickrBot) o;
        return user.equals(wickrBot.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user);
    }
}
