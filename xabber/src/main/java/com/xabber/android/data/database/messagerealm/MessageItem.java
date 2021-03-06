/**
 * Copyright (c) 2013, Redsolution LTD. All rights reserved.
 *
 * This file is part of Xabber project; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License, Version 3.
 *
 * Xabber is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.xabber.android.data.database.messagerealm;

import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;

import com.xabber.android.data.entity.AccountJid;
import com.xabber.android.data.entity.UserJid;
import com.xabber.android.data.log.LogManager;
import com.xabber.android.data.message.ChatAction;

import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.Index;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.Required;

public class MessageItem extends RealmObject {

    public static class Fields {
        public static final String UNIQUE_ID = "uniqueId";
        public static final String ACCOUNT = "account";
        public static final String USER = "user";
        public static final String RESOURCE = "resource";
        public static final String TEXT = "text";
        public static final String ACTION = "action";
        public static final String INCOMING = "incoming";
        public static final String ENCRYPTED = "encrypted";
        public static final String UNENCRYPTED = "unencrypted"; // deprecated
        public static final String OFFLINE = "offline";
        public static final String TIMESTAMP = "timestamp";
        public static final String DELAY_TIMESTAMP = "delayTimestamp";
        public static final String ERROR = "error";
        public static final String ERROR_DESCR = "errorDescription";
        public static final String DELIVERED = "delivered";
        public static final String SENT = "sent";
        public static final String READ = "read";
        public static final String STANZA_ID = "stanzaId";
        public static final String IS_RECEIVED_FROM_MAM = "isReceivedFromMessageArchive";
        public static final String FORWARDED = "forwarded";
        public static final String FILE_PATH = "filePath";
        public static final String FILE_URL = "fileUrl";
        public static final String FILE_SIZE = "fileSize";
        public static final String IS_IMAGE = "isImage";
        public static final String IMAGE_WIDTH = "imageWidth";
        public static final String IMAGE_HEIGHT = "imageHeight";
        public static final String ACKNOWLEDGED = "acknowledged";
        public static final String IS_IN_PROGRESS = "isInProgress";

    }

    /**
     * UUID
     */

    @PrimaryKey
    @Required
    private String uniqueId;

    @Index
    private String account;
    @Index
    private String user;

    /**
     * Contact's resource.
     */
    private String resource;
    /**
     * Text representation.
     */
    private String text;
    /**
     * Optional action. If set message represent not an actual message but some
     * action in the chat.
     */
    private String action;

    private boolean incoming;

    private boolean encrypted;

    /**
     * Message was received from server side offline storage.
     */
    private boolean offline;

    /**
     * Time when message was received or sent by Xabber.
     * Realm truncated Date type to seconds, using long for accuracy
     */
    @Index
    private Long timestamp;
    /**
     * Time when message was created.
     * Realm truncated Date type to seconds, using long for accuracy
     */
    private Long delayTimestamp;
    /**
     * Error response received on send request.
     */
    private boolean error;
    private String errorDescription;
    /**
     * Receipt was received for sent message.
     */
    private boolean delivered;
    /**
     * Message was sent.
     */
    @Index
    private boolean sent;
    /**
     * Message was shown to the user.
     */
    private boolean read;
    /**
     * Outgoing packet id - usual message stanza (packet) id
     */
    private String stanzaId;

    /**
     * If message was received from server message archive (XEP-0313)
     */
    private boolean isReceivedFromMessageArchive;

    /**
     * If message was forwarded (e.g. message carbons (XEP-0280))
     */
    private boolean forwarded;

    /**
     * If message text contains url to file
     */
    private String fileUrl;

    /**
     * If message "contains" file with local file path
     */
    private String filePath;

    /**
     * If message contains URL to image (and may be drawn as image)
     */
    private boolean isImage;

    @Nullable
    private Integer imageWidth;

    @Nullable
    private Integer imageHeight;

    private Long fileSize;

    /**
     * If message was acknowledged by server (XEP-0198: Stream Management)
     */
    private boolean acknowledged;

    /**
     * If message is currently in progress (i.e. file is uploading/downloading)
     */
    private boolean isInProgress;


    public MessageItem(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public MessageItem() {
        this.uniqueId = UUID.randomUUID().toString();
    }

    public String getUniqueId() {
        return uniqueId;
    }

    public void setUniqueId(String uniqueId) {
        this.uniqueId = uniqueId;
    }

    public AccountJid getAccount() {
        try {
            return AccountJid.from(account);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setAccount(AccountJid account) {
        this.account = account.toString();
    }

    public UserJid getUser() {
        try {
            return UserJid.from(user);
        } catch (UserJid.UserJidCreateException e) {
            LogManager.exception(this, e);
            throw new IllegalStateException();
        }
    }

    public void setUser(UserJid user) {
        this.user = user.toString();
    }

    public Resourcepart getResource() {
        if (TextUtils.isEmpty(resource)) {
            return Resourcepart.EMPTY;
        }

        try {
            return Resourcepart.from(resource);
        } catch (XmppStringprepException e) {
            LogManager.exception(this, e);
            return Resourcepart.EMPTY;
        }
    }

    public void setResource(Resourcepart resource) {
        if (resource != null) {
            this.resource = resource.toString();
        } else {
            this.resource = Resourcepart.EMPTY.toString();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public boolean isIncoming() {
        return incoming;
    }

    public void setIncoming(boolean incoming) {
        this.incoming = incoming;
    }

    public boolean isOffline() {
        return offline;
    }

    public void setOffline(boolean offline) {
        this.offline = offline;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getDelayTimestamp() {
        return delayTimestamp;
    }

    public void setDelayTimestamp(Long delayTimestamp) {
        this.delayTimestamp = delayTimestamp;
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }

    public boolean isSent() {
        return sent;
    }

    public void setSent(boolean sent) {
        this.sent = sent;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getStanzaId() {
        return stanzaId;
    }

    public void setStanzaId(String stanzaId) {
        this.stanzaId = stanzaId;
    }

    public boolean isReceivedFromMessageArchive() {
        return isReceivedFromMessageArchive;
    }

    public void setReceivedFromMessageArchive(boolean receivedFromMessageArchive) {
        isReceivedFromMessageArchive = receivedFromMessageArchive;
    }

    public boolean isForwarded() {
        return forwarded;
    }

    public void setForwarded(boolean forwarded) {
        this.forwarded = forwarded;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public boolean isImage() {
        return isImage;
    }

    public void setIsImage(boolean isImage) {
        this.isImage = isImage;
    }

    @Nullable
    public Integer getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(@Nullable Integer imageWidth) {
        this.imageWidth = imageWidth;
    }

    @Nullable
    public Integer getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(@Nullable Integer imageHeight) {
        this.imageHeight = imageHeight;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public static ChatAction getChatAction(MessageItem messageItem) {
        return ChatAction.valueOf(messageItem.getAction());
    }

    public static Spannable getSpannable(MessageItem messageItem) {
        return new SpannableString(messageItem.getText());
    }

    public static boolean isUploadFileMessage(MessageItem messageItem) {
        return messageItem.getFilePath() != null && !messageItem.isIncoming() && !messageItem.isSent();
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }

    public boolean isInProgress() {
        return isInProgress;
    }

    public void setInProgress(boolean inProgress) {
        isInProgress = inProgress;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }
}
