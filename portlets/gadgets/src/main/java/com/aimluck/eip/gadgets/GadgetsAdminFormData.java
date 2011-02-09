/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2011 Aimluck,Inc.
 * http://www.aipo.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.aimluck.eip.gadgets;

import java.util.ArrayList;
import java.util.List;

import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALStringField;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALApplication;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALOAuthConsumer;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.services.social.ALApplicationService;
import com.aimluck.eip.services.social.ALOAuthConsumerService;
import com.aimluck.eip.services.social.gadgets.ALGadgetSpec;
import com.aimluck.eip.services.social.model.ALApplicationGetRequest;
import com.aimluck.eip.services.social.model.ALApplicationGetRequest.Status;
import com.aimluck.eip.services.social.model.ALApplicationPutRequest;
import com.aimluck.eip.services.social.model.ALOAuthConsumerPutRequest;
import com.aimluck.eip.services.social.model.ALOAuthConsumerPutRequest.Type;
import com.aimluck.eip.util.ALEipUtils;

/**
 * 
 */
public class GadgetsAdminFormData extends ALAbstractFormData {

  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(GadgetsAdminFormData.class.getName());

  private ALStringField url;

  private List<ALOAuthConsumer> oAuthConsumers;

  /**
   * 
   */
  @Override
  public void initField() {
    url = new ALStringField();
    url.setFieldName("ガジェットXML URL");
    url.setTrim(true);

    oAuthConsumers = new ArrayList<ALOAuthConsumer>();

  }

  @Override
  protected boolean setFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    boolean res = super.setFormData(rundata, context, msgList);
    if (this.getMode().equals(ALEipConstants.MODE_UPDATE)) {
      String appId =
        ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
      ALApplication app =
        ALApplicationService.get(new ALApplicationGetRequest()
          .withAppId(appId)
          .withStatus(Status.ALL)
          .withIsDetail(true));
      if (app == null) {
        return false;
      }
      url.setValue(app.getUrl().getValue());
      oAuthConsumers = app.getOAuthConsumers();
      int size = oAuthConsumers.size();
      for (int i = 0; i < size; i++) {
        ALOAuthConsumer service = oAuthConsumers.get(i);

        String key = "consumerKey" + i;
        String keyString = rundata.getParameters().getString(key);
        service.getConsumerKey().setValue(keyString);

        key = "consumerSecret" + i;
        keyString = rundata.getParameters().getString(key);
        service.getConsumerSecret().setValue(keyString);

        key = "type" + i;
        keyString = rundata.getParameters().getString(key);
        service.getType().setValue(keyString);
      }
    }
    return res;
  }

  /**
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected void setValidator() throws ALPageNotFoundException,
      ALDBErrorException {
    url.setNotNull(true);
    url.limitMaxLength(255);
  }

  /**
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean validate(List<String> msgList)
      throws ALPageNotFoundException, ALDBErrorException {
    if (!ALEipConstants.MODE_UPDATE.equals(getMode())) {
      url.validate(msgList);
    }
    if (msgList.size() == 0) {
      if (!ALEipConstants.MODE_UPDATE.equals(getMode())) {
        ALApplication app =
          ALApplicationService.get(new ALApplicationGetRequest().withAppId(
            url.getValue()).withStatus(Status.ALL));
        if (app != null) {
          msgList.add("既に登録済みの 『 ガジェットXML URL 』 です。");
        } else {
          ALGadgetSpec metaData =
            ALApplicationService.getMetaData(url.getValue());
          if (metaData == null) {
            msgList.add("正しい  『 ガジェットXML URL 』 を指定してください。");
          }
        }
      }
    }
    return (msgList.size() == 0);
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean loadFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
    String appId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);
    ALApplication app =
      ALApplicationService.get(new ALApplicationGetRequest()
        .withAppId(appId)
        .withStatus(Status.ALL)
        .withIsDetail(true));
    if (app == null) {
      return false;
    }
    url.setValue(app.getUrl().getValue());
    oAuthConsumers = app.getOAuthConsumers();
    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean insertFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    try {

      ALApplicationService.create(new ALApplicationPutRequest().withUrl(url
        .getValue()));

    } catch (Throwable t) {
      logger.error(t, t);
      throw new ALDBErrorException();
    }
    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean updateFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    String appId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    try {

      ALApplicationService.update(appId, new ALApplicationPutRequest());

      for (ALOAuthConsumer service : oAuthConsumers) {
        ALOAuthConsumerService.put(new ALOAuthConsumerPutRequest().withAppId(
          appId).withName(service.getName().getValue()).withConsumerKey(
          service.getConsumerKey().getValue()).withConsumerSecret(
          service.getConsumerSecret().getValue()).withType(
          "RSA-SHA1".equalsIgnoreCase(service.getType().getValue())
            ? Type.RSASHA1
            : Type.HMACSHA1));
      }

    } catch (Throwable t) {
      logger.error(t, t);
      throw new ALDBErrorException();
    }

    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param msgList
   * @return
   * @throws ALPageNotFoundException
   * @throws ALDBErrorException
   */
  @Override
  protected boolean deleteFormData(RunData rundata, Context context,
      List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {

    String appId =
      ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

    try {

      ALApplicationService.delete(appId);

    } catch (Throwable t) {
      logger.error(t, t);
      throw new ALDBErrorException();
    }

    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param object
   */
  public boolean enableFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }

      String appId =
        ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

      if (appId == null || "".equals(appId)) {
        return false;
      }

      ALApplicationService.enable(appId);

    } catch (Exception e) {
      Database.rollback();
      logger.error("Exception", e);
      return false;
    }
    return true;
  }

  /**
   * @param rundata
   * @param context
   * @param object
   */
  public boolean disableFormData(RunData rundata, Context context,
      List<String> msgList) {
    try {
      if (!doCheckSecurity(rundata, context)) {
        return false;
      }

      String appId =
        ALEipUtils.getTemp(rundata, context, ALEipConstants.ENTITY_ID);

      if (appId == null || "".equals(appId)) {
        return false;
      }

      ALApplicationService.disable(appId);

    } catch (Exception e) {
      logger.error("Exception", e);
      return false;
    }
    return true;
  }

  public ALStringField getUrl() {
    return url;
  }

  /**
   * 
   * @return
   */
  public List<ALOAuthConsumer> getOAuthConsumers() {
    return oAuthConsumers;
  }
}
