/*
 * Kimios - Document Management System Software
 * Copyright (C) 2008-2015  DevLib'
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 * You should have received a copy of the GNU Affero General Public License
 * aong with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.kimios.kernel.events.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import org.kimios.api.events.annotations.DmsEvent;
import org.kimios.api.events.annotations.DmsEventName;
import org.kimios.api.events.annotations.DmsEventOccur;
import org.kimios.kernel.dms.model.DMEntity;
import org.kimios.kernel.dms.model.DMEntityImpl;
import org.kimios.kernel.dms.model.Document;
import org.kimios.kernel.dms.model.Folder;
import org.kimios.kernel.dms.model.Workspace;
import org.kimios.kernel.events.GenericEventHandler;
import org.kimios.kernel.events.model.EventContext;
import org.kimios.kernel.log.ActionType;
import org.kimios.kernel.log.FactoryInstantiator;
import org.kimios.kernel.log.model.DMEntityLog;
import org.kimios.kernel.security.model.Session;
import org.kimios.kernel.ws.pojo.Share;
import org.kimios.kernel.ws.pojo.ShareSessionWrapper;
import org.kimios.kernel.ws.pojo.UpdateNoticeMessage;
import org.kimios.kernel.ws.pojo.UpdateNoticeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class ActionLogger extends GenericEventHandler
{
    private static Logger logger = LoggerFactory.getLogger(ActionLogger.class);

    Gson gson = new Gson();
    Map<UpdateNoticeType, RuleCheckFunctionalInterface> noticeSendingRules = new HashMap<>();

    public ActionLogger() {
        RuleCheckFunctionalInterface mustBeAdmin = (session) ->  isAdmin(session);

        noticeSendingRules.put(
                UpdateNoticeType.GROUP_CREATED,
                mustBeAdmin
        );
    }

    @DmsEvent(eventName = { DmsEventName.WORKSPACE_CREATE }, when = DmsEventOccur.AFTER)
    public void createWorkspace(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        Workspace workspace = (Workspace) ctx.getEntity();
        workspace.setUid((Long) returnObj);
        ctx.setEntity(workspace);
        saveLog(new DMEntityLog<Workspace>(), ActionType.CREATE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.WORKSPACE_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateWorkspace(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Workspace>(), ActionType.UPDATE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.WORKSPACE_DELETE }, when = DmsEventOccur.AFTER)
    public void deleteWorkspace(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Workspace>(), ActionType.DELETE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.FOLDER_CREATE }, when = DmsEventOccur.AFTER)
    public void createFolder(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        Folder folder = (Folder) ctx.getEntity();
        folder.setUid((Long) returnObj);
        ctx.setEntity(folder);
        saveLog(new DMEntityLog<Folder>(), ActionType.CREATE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.FOLDER_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateFolder(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Folder>(), ActionType.UPDATE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.FOLDER_DELETE }, when = DmsEventOccur.AFTER)
    public void deleteFolder(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Folder>(), ActionType.DELETE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_CREATE }, when = DmsEventOccur.AFTER)
    public void createDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        Document document = (Document) ctx.getEntity();
        document.setUid((Long) returnObj);
        ctx.setEntity(document);
        saveLog(new DMEntityLog<Document>(), ActionType.CREATE, ctx);
        sendDocumentUpdateNotice(
                ctx.getSession().getUid(),
                document
        );
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.UPDATE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_DELETE }, when = DmsEventOccur.AFTER)
    public void deleteDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.DELETE, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_CHECKOUT }, when = DmsEventOccur.AFTER)
    public void checkoutDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.CHECKOUT, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_CHECKIN }, when = DmsEventOccur.AFTER)
    public void checkinDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.CHECKIN, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_ADD_RELATED }, when = DmsEventOccur.AFTER)
    public void addRelatedDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.ADD_RELATED_DOCUMENT, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_REMOVE_RELATED }, when = DmsEventOccur.AFTER)
    public void removeRelatedDocument(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Document>(), ActionType.REMOVE_RELATED_DOCUMENT, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_VERSION_CREATE }, when = DmsEventOccur.AFTER)
    public void createDocumentVersion(Object[] paramsObj, Object returnObj, EventContext ctx) throws Exception
    {
        saveLog(new DMEntityLog<Document>(), ActionType.CREATE_DOCUMENT_VERSION, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_VERSION_CREATE_FROM_LATEST }, when = DmsEventOccur.AFTER)
    public void createDocumentVersionFromLatest(Object[] paramsObj, Object returnObj, EventContext ctx)
            throws Exception
    {
        saveLog(new DMEntityLog<Document>(), ActionType.CREATE_DOCUMENT_VERSION_FROM_LATEST, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_VERSION_UPDATE }, when = DmsEventOccur.AFTER)
    public void updateDocumentVersion(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.UPDATE_DOCUMENT_VERSION, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.FILE_UPLOAD }, when = DmsEventOccur.AFTER)
    public void uploadVersionEnd(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.UPDATE_DOCUMENT_VERSION, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_VERSION_READ }, when = DmsEventOccur.AFTER)
    public void startDownloadTransaction(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        saveLog(new DMEntityLog<Document>(), ActionType.READ, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_TRASH }, when = DmsEventOccur.AFTER)
    public void trashDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        ctx.setEntity((DMEntity)EventContext.getParameters().get("document"));
        if(ctx.getEntity() instanceof Document){
            saveLog(new DMEntityLog<Document>(), ActionType.TRASH_DOCUMENT, ctx);
        } else if(ctx.getEntity() instanceof Folder){
            saveLog(new DMEntityLog<Folder>(), ActionType.TRASH_ENTITY, ctx);
        } else if(ctx.getEntity() instanceof Workspace){
            saveLog(new DMEntityLog<Workspace>(), ActionType.TRASH_ENTITY, ctx);
        }

    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_UNTRASH }, when = DmsEventOccur.AFTER)
    public void untrashDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        ctx.setEntity((DMEntity)EventContext.getParameters().get("document"));
        if(ctx.getEntity() instanceof Document)
            saveLog(new DMEntityLog<Document>(), ActionType.UNTRASH_DOCUMENT, ctx);
        else if(ctx.getEntity() instanceof Folder){
            saveLog(new DMEntityLog<Folder>(), ActionType.UNTRASH_ENTITY, ctx);
        } else if(ctx.getEntity() instanceof Workspace){
            saveLog(new DMEntityLog<Workspace>(), ActionType.UNTRASH_ENTITY, ctx);
        }
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_COPY }, when = DmsEventOccur.AFTER)
    public void copyDocument(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        ctx.setEntity((DMEntity)EventContext.getParameters().get("document"));
        saveLog(new DMEntityLog<Document>(), ActionType.COPY_DOCUMENT, ctx);
    }

    @DmsEvent(eventName = { DmsEventName.DOCUMENT_SHARED }, when = DmsEventOccur.AFTER)
    public void shareDocuments(Object[] paramsObj, Object returnObj, EventContext ctx)
    {
        ShareSessionWrapper shareSessionWrapper = (ShareSessionWrapper) paramsObj[0];
        if (shareSessionWrapper == null) {
            logger.error("After DmsEventName.DOCUMENT_SHARED: shareSessionWrapper is null");
            return;
        }
        Share share = shareSessionWrapper.getShare();
        Session session = shareSessionWrapper.getSession();
        if (share == null) {
            logger.error("After DmsEventName.DOCUMENT_SHARED: share is null");
            return;
        }
        if (session == null) {
            logger.error("After DmsEventName.DOCUMENT_SHARED: session is null");
            return;
        }

        ctx.setEntity(
                org.kimios.kernel.dms.FactoryInstantiator
                        .getInstance().getDocumentFactory().getDocument(share.getEntity().getUid())
        );
        ctx.setSession(session);
        saveLog(new DMEntityLog<Document>(), ActionType.DOCUMENT_SHARED, ctx);
        sendShareUpdateNotice(session, share);
    }

    @DmsEvent(eventName = { DmsEventName.USER_GROUP_ADD }, when = DmsEventOccur.AFTER)
    public void userGroupAdd(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String group = (String) EventContext.getParameters().get("group");
        String user = (String) EventContext.getParameters().get("user");
        String source = (String) EventContext.getParameters().get("source");

        if (group == null || user == null || source == null) {
            return;
        }
        this.sendUserGroupChange(source, group, user, UpdateNoticeType.USER_GROUP_ADD);
    }

    @DmsEvent(eventName = { DmsEventName.USER_GROUP_REMOVE }, when = DmsEventOccur.AFTER)
    public void userGroupRemove(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String group = (String) EventContext.getParameters().get("group");
        String user = (String) EventContext.getParameters().get("user");
        String source = (String) EventContext.getParameters().get("source");

        if (group == null || user == null || source == null) {
            return;
        }
        this.sendUserGroupChange(source, group, user, UpdateNoticeType.USER_GROUP_REMOVE);
    }

    @DmsEvent(eventName = { DmsEventName.USER_CREATE }, when = DmsEventOccur.AFTER)
    public void userCreate(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String user = (String) EventContext.getParameters().get("user");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("user", user),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.USER_CREATED);
    }

    @DmsEvent(eventName = { DmsEventName.USER_DELETE }, when = DmsEventOccur.AFTER)
    public void userDelete(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String user = (String) EventContext.getParameters().get("user");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("user", user),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.USER_REMOVED);
    }

    @DmsEvent(eventName = { DmsEventName.USER_UPDATE }, when = DmsEventOccur.AFTER)
    public void userUpdate(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String user = (String) EventContext.getParameters().get("user");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("user", user),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.USER_MODIFIED);
    }

    @DmsEvent(eventName = { DmsEventName.GROUP_CREATE }, when = DmsEventOccur.AFTER)
    public void groupCreate(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String group = (String) EventContext.getParameters().get("group");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("group", group),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.GROUP_CREATED);
    }

    @DmsEvent(eventName = { DmsEventName.GROUP_UPDATE }, when = DmsEventOccur.AFTER)
    public void groupUpdate(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String group = (String) EventContext.getParameters().get("group");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("group", group),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.GROUP_MODIFIED);
    }

    @DmsEvent(eventName = { DmsEventName.GROUP_DELETE }, when = DmsEventOccur.AFTER)
    public void groupDelete(Object[] paramsObj, Object returnObj, EventContext ctx) {
        String group = (String) EventContext.getParameters().get("group");
        String source = (String) EventContext.getParameters().get("source");
        Map<String, Object> messageProperties = this.makePropertiesMap(
                new AbstractMap.SimpleEntry<String, Object>("group", group),
                new AbstractMap.SimpleEntry<String, Object>("source", source)
        );
        this.sendUpdateNoticeWithMessage(messageProperties, UpdateNoticeType.GROUP_REMOVED);
    }

    private <T extends DMEntityImpl> void saveLog(DMEntityLog<T> log, int actionType, EventContext ctx)
    {
        try {

            DMEntity entity = null;
            if(ctx.getEntity() == null){
                //try from parameters
                if(EventContext.getParameters().get("document") instanceof Document){
                    entity = (Document)EventContext.getParameters().get("document");
                } else
                if(EventContext.getParameters().get("folder") instanceof Folder){
                    entity = (Document)EventContext.getParameters().get("folder");
                } else
                if(EventContext.getParameters().get("workspace") instanceof Workspace){
                    entity = (Document)EventContext.getParameters().get("workspace");
                }
            } else {
                entity = ctx.getEntity();
            }
            if(logger.isDebugEnabled()){
                logger.debug("Save log on event: {} - entity: {} - contextParameters: {}", ctx, entity, EventContext.getParameters());
            }
            log.setDate(new Date());
            log.setUser(ctx.getSession().getUserName());
            log.setUserSource(ctx.getSession().getUserSource());
            log.setAction(actionType);
            log.setDMEntity(entity);
            //if delete action, store item data as json
            if(actionType == ActionType.DELETE){
                if(ctx.getEntity() != null){
                    // store document path
                    log.setActionData(ctx.getEntity().getPath() + " - (owner:" + ctx.getEntity().getOwner() + "@" + ctx.getEntity().getOwnerSource() + ")");
                }
            }
            FactoryInstantiator.getInstance().getEntityLogFactory().saveLog(log);
        } catch (Exception e) {
            logger.error(
                    ctx.getEvent() + " - " + (ctx.getEntity() != null ? ctx.getEntity().getUid() : " Entity is null"),
                    e);
        }
    }

    private void sendDocumentUpdateNotice(String sessionUid, Document document) {
        FactoryInstantiator.getInstance().getSessionManager().getSessions().forEach(session -> {
            if (FactoryInstantiator.getInstance().getSecurityController().canRead(session, document.getUid())) {
                FactoryInstantiator.getInstance().getWebSocketManager().sendUpdateNotice(
                        new UpdateNoticeMessage(
                                UpdateNoticeType.DOCUMENT,
                                null,
                                session.getUid(),
                                gson.toJson(document)
                        )
                );
            }
        });
    }

    private void sendShareUpdateNotice(Session session, Share share) {
        // to share creator
        FactoryInstantiator.getInstance().getWebSocketManager().sendUpdateNotice(
                new UpdateNoticeMessage(
                        UpdateNoticeType.SHARES_BY_ME,
                        null,
                        session.getUid()
                )
        );

        // to all share target user sessions
        FactoryInstantiator.getInstance().getSessionManager().getSessions().stream().filter(sess ->
                sess.getUserName().equals(share.getTargetUserId())
                && sess.getUserSource().equals(share.getTargetUserSource())
        ).forEach(sess -> {
            FactoryInstantiator.getInstance().getWebSocketManager().sendUpdateNotice(
                    new UpdateNoticeMessage(
                            UpdateNoticeType.SHARES_WITH_ME,
                            null,
                            sess.getUid()
                    )
            );
        });
    }

    private void sendUserGroupChange(String source, String group, String user, UpdateNoticeType updateNoticeType) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("source", source);
        rootNode.put("group", group);
        rootNode.put("user", user);

        String jsonString = "";
        try {
            jsonString = mapper.writer().writeValueAsString(rootNode);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return;
        }

        String finalJsonString = jsonString;
        FactoryInstantiator.getInstance().getSessionManager().getSessions().stream().filter(sess ->
                shouldReceiveNotice(sess, updateNoticeType)
        ).forEach(sess -> {
            FactoryInstantiator.getInstance().getWebSocketManager().sendUpdateNotice(
                    new UpdateNoticeMessage(
                            updateNoticeType,
                            null,
                            sess.getUid(),
                            finalJsonString
                    )
            );
        });
    }

    private Map<String, Object> makePropertiesMap(AbstractMap.SimpleEntry<String, Object>... entries) {
        return Arrays.stream(entries)
                .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));
    }

    private void sendUserChange(String source, String user, UpdateNoticeType updateNoticeType) {
        Map<String, Object> messageProperties = new HashMap<>();
        messageProperties.put("source", source);
        messageProperties.put("user", user);
        this.sendUpdateNoticeWithMessage(messageProperties, updateNoticeType);
    }

    private void sendUpdateNoticeWithMessage(Map<String, Object> messageProperties, UpdateNoticeType updateNoticeType) {
        FactoryInstantiator.getInstance().getSessionManager().getSessions().stream().filter(sess ->
                shouldReceiveNotice(sess, updateNoticeType)
        ).forEach(sess ->
            FactoryInstantiator.getInstance().getWebSocketManager().sendUpdateNotice(
                    new UpdateNoticeMessage(
                            updateNoticeType,
                            null,
                            sess.getUid(),
                            gson.toJson(messageProperties)
                    )
            )
        );
    }

    private boolean shouldReceiveNotice(Session session, UpdateNoticeType updateNoticeType) {
        RuleCheckFunctionalInterface ruleCheckFunctionalInterface =
                this.noticeSendingRules.get(updateNoticeType);

        if (ruleCheckFunctionalInterface == null) {
            return true;
        }
        return ruleCheckFunctionalInterface.check(session);
    }

    private boolean isAdmin(Session session) {
        return FactoryInstantiator.getInstance().getSecurityController().isAdmin(session);
    }

    @FunctionalInterface
    public interface RuleCheckFunctionalInterface {
        Boolean check(Session session);
    }
}

