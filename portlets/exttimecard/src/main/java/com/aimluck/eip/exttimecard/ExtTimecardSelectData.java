/*
 * Aipo is a groupware program developed by Aimluck,Inc.
 * Copyright (C) 2004-2010 Aimluck,Inc.
 * http://aipostyle.com/
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

package com.aimluck.eip.exttimecard;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.portal.portlets.VelocityPortlet;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.turbine.services.TurbineServices;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

import com.aimluck.commons.field.ALDateTimeField;
import com.aimluck.commons.utils.ALDateUtil;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecard;
import com.aimluck.eip.cayenne.om.portlet.EipTExtTimecardSystem;
import com.aimluck.eip.common.ALAbstractSelectData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALData;
import com.aimluck.eip.common.ALEipGroup;
import com.aimluck.eip.common.ALEipManager;
import com.aimluck.eip.common.ALEipPost;
import com.aimluck.eip.common.ALEipUser;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.exttimecard.util.ExtTimecardUtils;
import com.aimluck.eip.modules.actions.common.ALAction;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.ResultList;
import com.aimluck.eip.orm.query.SelectQuery;
import com.aimluck.eip.services.accessctl.ALAccessControlConstants;
import com.aimluck.eip.services.accessctl.ALAccessControlFactoryService;
import com.aimluck.eip.services.accessctl.ALAccessControlHandler;
import com.aimluck.eip.util.ALEipUtils;

/**
 * タイムカード検索データを管理するクラスです。 <BR>
 * 
 */
public class ExtTimecardSelectData extends
    ALAbstractSelectData<EipTExtTimecard, EipTExtTimecard> implements ALData {

  /** <code>TARGET_GROUP_NAME</code> グループによる表示切り替え用変数の識別子 */
  private final String TARGET_GROUP_NAME = "target_group_name";

  /** <code>TARGET_USER_ID</code> ユーザによる表示切り替え用変数の識別子 */
  // private final String TARGET_USER_ID = "target_user_id";

  /** logger */
  private static final JetspeedLogger logger = JetspeedLogFactoryService
    .getLogger(ExtTimecardSelectData.class.getName());

  /** <code>target_group_name</code> 表示対象の部署名 */
  private String target_group_name;

  /** <code>target_user_id</code> 表示対象のユーザ ID */
  private String target_user_id;

  /** <code>myGroupList</code> グループリスト（My グループと部署） */
  private List<ALEipGroup> myGroupList = null;

  /** <code>userList</code> 表示切り替え用のユーザリスト */
  private List<ALEipUser> userList = null;

  /** <code>userid</code> ユーザーID */
  private String userid;

  /** 集計日付 */
  // private ALDateTimeField viewMonth;
  private String nowtime;

  /** 日付マップ */
  private Map<String, ExtTimecardListResultData> datemap;

  /** タイムカード設定 */
  private EipTExtTimecardSystem timecard_system;

  /** アクセス権限の機能名 */
  private String aclPortletFeature = null;

  /** 閲覧権限の有無（集計画面） */
  private boolean hasAclSummaryOther;

  /** 閲覧権限の有無（タイムカード外部出力） */
  private boolean hasAclXlsExport;

  /** <code>viewMonth</code> 現在の月 */
  private ALDateTimeField viewMonth;

  /** <code>prevMonth</code> 前の月 */
  private ALDateTimeField prevMonth;

  /** <code>nextMonth</code> 次の月 */
  private ALDateTimeField nextMonth;

  /** <code>currentMonth</code> 今月 */
  private ALDateTimeField currentMonth;

  /** <code>today</code> 今日 */
  private ALDateTimeField today;

  /** <code>viewStart</code> 表示開始日時 */
  private ALDateTimeField viewStart;

  /** <code>viewEnd</code> 表示終了日時 */
  private ALDateTimeField viewEnd;

  /** <code>viewEndCrt</code> 表示終了日時 (Criteria) */
  private ALDateTimeField viewEndCrt;

  /** <code>viewTodo</code> ToDo 表示設定 */
  protected int viewTodo;

  /**
   * 
   * @param action
   * @param rundata
   * @param context
   */
  @Override
  public void init(ALAction action, RunData rundata, Context context)
      throws ALPageNotFoundException, ALDBErrorException {

    // POST/GET から yyyy-MM の形式で受け渡される。
    // 現在の月
    viewMonth = new ALDateTimeField("yyyy-MM");
    viewMonth.setNotNull(true);
    // 前の月
    prevMonth = new ALDateTimeField("yyyy-MM");
    // 次の月
    nextMonth = new ALDateTimeField("yyyy-MM");
    // 今月
    currentMonth = new ALDateTimeField("yyyy-MM");
    // 表示開始日時
    viewStart = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時
    viewEnd = new ALDateTimeField("yyyy-MM-dd");
    // 表示終了日時 (Criteria)
    viewEndCrt = new ALDateTimeField("yyyy-MM-dd");
    // 今日
    today = new ALDateTimeField("yyyy-MM-dd");
    Calendar to = Calendar.getInstance();
    to.set(Calendar.HOUR_OF_DAY, 0);
    to.set(Calendar.MINUTE, 0);
    today.setValue(to.getTime());
    currentMonth.setValue(to.getTime());

    // 自ポートレットからのリクエストであれば、パラメータを展開しセッションに保存する。
    if (ALEipUtils.isMatch(rundata, context)) {
      // スケジュールの表示開始日時
      // e.g. 2004-3-14
      if (rundata.getParameters().containsKey("view_month")) {
        ALEipUtils.setTemp(rundata, context, "view_month", rundata
          .getParameters()
          .getString("view_month"));
      }
    }

    // 現在の月
    String tmpViewMonth = ALEipUtils.getTemp(rundata, context, "view_month");
    if (tmpViewMonth == null || tmpViewMonth.equals("")) {
      Calendar cal = Calendar.getInstance();
      cal.set(Calendar.DATE, 1);
      cal.set(Calendar.HOUR_OF_DAY, 0);
      cal.set(Calendar.MINUTE, 0);
      viewMonth.setValue(cal.getTime());
    } else {
      viewMonth.setValue(tmpViewMonth);
      if (!viewMonth.validate(new ArrayList<String>())) {
        ALEipUtils.removeTemp(rundata, context, "view_month");
        throw new ALPageNotFoundException();
      }
    }

    // 表示開始日時
    Calendar cal = Calendar.getInstance();
    Calendar tmpCal = Calendar.getInstance();
    cal.setTime(viewMonth.getValue());
    tmpCal.setTime(viewMonth.getValue());
    int dayofweek = cal.get(Calendar.DAY_OF_WEEK);
    cal.add(Calendar.DATE, -dayofweek + 1);
    viewStart.setValue(cal.getTime());

    Calendar cal4 = Calendar.getInstance();
    cal4.setTime(cal.getTime());
    Calendar tmpCal4 = Calendar.getInstance();
    tmpCal4.setTime(tmpCal.getTime());

    Calendar cal5 = Calendar.getInstance();
    cal5.setTime(cal.getTime());
    Calendar tmpCal5 = Calendar.getInstance();
    tmpCal5.setTime(tmpCal.getTime());

    // 表示終了日時
    viewEndCrt.setValue(cal.getTime());
    cal.add(Calendar.DATE, -1);
    viewEnd.setValue(cal.getTime());
    // 次の月、前の月
    Calendar cal2 = Calendar.getInstance();
    cal2.setTime(viewMonth.getValue());
    cal2.add(Calendar.MONTH, 1);
    nextMonth.setValue(cal2.getTime());
    cal2.add(Calendar.MONTH, -2);
    prevMonth.setValue(cal2.getTime());

    ALEipUtils.setTemp(rundata, context, "tmpStart", viewStart.toString()
      + "-00-00");
    ALEipUtils.setTemp(rundata, context, "tmpEnd", viewStart.toString()
      + "-00-00");

    // ログインユーザの ID を設定する．
    userid = Integer.toString(ALEipUtils.getUserId(rundata));

    // My グループの一覧を取得する．
    List<ALEipGroup> myGroups = ALEipUtils.getMyGroups(rundata);
    myGroupList = new ArrayList<ALEipGroup>();
    int length = myGroups.size();
    for (int i = 0; i < length; i++) {
      myGroupList.add(myGroups.get(i));
    }

    try {
      String groupFilter =
        ALEipUtils.getTemp(rundata, context, TARGET_GROUP_NAME);
      if (groupFilter == null || groupFilter.equals("")) {
        VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
        groupFilter = portlet.getPortletConfig().getInitParameter("p3a-group");
        if (groupFilter != null) {
          ALEipUtils.setTemp(rundata, context, TARGET_GROUP_NAME, groupFilter);
        }
      }

      // スケジュールを表示するユーザ ID をセッションに設定する．
      // String userFilter = ALEipUtils.getTemp(rundata, context,
      // TARGET_USER_ID);
      // if (userFilter == null || userFilter.equals("")) {
      // VelocityPortlet portlet = ALEipUtils.getPortlet(rundata, context);
      // userFilter = portlet.getPortletConfig().getInitParameter("p3a-user");
      // }

    } catch (Exception ex) {
      logger.error("Exception", ex);
    }
    // 指定グループや指定ユーザをセッションに設定する．
    setupLists(rundata, context);

    // アクセス権
    if (target_user_id == null
      || "".equals(target_user_id)
      || userid.equals(target_user_id)) {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_TIMECARD_TIMECARD_SELF;
    } else {
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_TIMECARD_TIMECARD_OTHER;
    }
    ALAccessControlFactoryService aclservice =
      (ALAccessControlFactoryService) ((TurbineServices) TurbineServices
        .getInstance()).getService(ALAccessControlFactoryService.SERVICE_NAME);
    ALAccessControlHandler aclhandler = aclservice.getAccessControlHandler();
    hasAclSummaryOther =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        ALAccessControlConstants.POERTLET_FEATURE_TIMECARD_TIMECARD_OTHER,
        ALAccessControlConstants.VALUE_ACL_LIST);
    if (!hasAclSummaryOther) {
      // 他ユーザーの閲覧権限がないときには、ログインユーザーのIDに変更する。
      target_user_id = userid;
      aclPortletFeature =
        ALAccessControlConstants.POERTLET_FEATURE_TIMECARD_TIMECARD_SELF;
    }

    hasAclXlsExport =
      aclhandler.hasAuthority(
        ALEipUtils.getUserId(rundata),
        aclPortletFeature,
        ALAccessControlConstants.VALUE_ACL_EXPORT);

    datemap = new LinkedHashMap<String, ExtTimecardListResultData>();

    if (target_user_id == null) {
      timecard_system =
        ExtTimecardUtils
          .getEipTExtTimecardSystemCurrentUserId(rundata, context);
    } else {
      timecard_system =
        ExtTimecardUtils.getEipTExtTimecardSystemByUserId(Integer
          .valueOf(target_user_id));
    }

    super.init(action, rundata, context);
  }

  /**
   * 検索条件を設定した SelectQuery を返します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  private SelectQuery<EipTExtTimecard> getSelectQuery(RunData rundata,
      Context context) {
    SelectQuery<EipTExtTimecard> query = Database.query(EipTExtTimecard.class);

    Expression exp1 =
      ExpressionFactory.matchExp(EipTExtTimecard.USER_ID_PROPERTY, Integer
        .valueOf(target_user_id));
    query.setQualifier(exp1);

    Calendar cal = Calendar.getInstance();
    Expression exp11 =
      ExpressionFactory.greaterOrEqualExp(
        EipTExtTimecard.PUNCH_DATE_PROPERTY,
        viewMonth.getValue());

    cal.setTime(viewMonth.getValue());
    cal.add(Calendar.MONTH, +1);

    ALDateTimeField viewMonth_add_month = new ALDateTimeField("yyyy-MM-dd");
    viewMonth_add_month.setValue(cal.getTime());

    Expression exp12 =
      ExpressionFactory.lessOrEqualExp(
        EipTExtTimecard.PUNCH_DATE_PROPERTY,
        viewMonth_add_month.getValue());
    query.andQualifier(exp11.andExp(exp12));

    return buildSelectQueryForFilter(query, rundata, context);
  }

  /**
   * 一覧データを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public ResultList<EipTExtTimecard> selectList(RunData rundata, Context context) {
    try {

      if (!"".equals(target_user_id)) {

        SelectQuery<EipTExtTimecard> query = getSelectQuery(rundata, context);
        buildSelectQueryForListView(query);
        query.orderAscending(EipTExtTimecard.PUNCH_DATE_PROPERTY);

        return query.getResultList();
      } else {
        return null;
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * ResultData に値を格納して返します。（一覧データ） <BR>
   * 
   * @param obj
   * @return
   */
  @Override
  protected Object getResultData(EipTExtTimecard record) {
    try {
      Date date = record.getPunchDate();
      String checkdate = ALDateUtil.format(date, "yyyyMMdd");
      Object value = datemap.get(checkdate);
      if (value == null) {
        ExtTimecardListResultData listrd = new ExtTimecardListResultData();
        listrd.initField();
        listrd.setDate(date);
        listrd.setTimecardSystem(timecard_system);
        listrd.setBeforeAfter();

        ExtTimecardResultData rd = new ExtTimecardResultData();
        rd.initField();
        rd.setPunchDate(record.getPunchDate());
        rd.setRefixFlag(record.getCreateDate(), record.getUpdateDate());
        rd.setTimecardId(record.getExtTimecardId().longValue());
        rd.setReason(record.getReason());
        rd.setRemarks(record.getRemarks());

        String reason = record.getReason();
        String remarks = record.getRemarks();

        if (reason == null || "".equals(reason)) {
          rd.setReasonFlg(false);
        } else {
          rd.setReasonFlg(true);
        }
        if (remarks == null || "".equals(remarks)) {
          rd.setRemarksFlg(false);
        } else {
          rd.setRemarksFlg(true);
        }
        rd.setClockInTime(record.getClockInTime());
        rd.setClockOutTime(record.getClockOutTime());

        String type = record.getType();
        rd.setType(type);

        if ("P".equals(type)) {
          rd.setIsTypeP(true);
        } else if ("A".equals(type)) {
          rd.setIsTypeA(true);
        } else if ("H".equals(type)) {
          rd.setIsTypeH(true);
        } else if ("C".equals(type)) {
          rd.setIsTypeC(true);
        } else if ("E".equals(type)) {
          rd.setIsTypeE(true);
        }
        for (int i = 1; i <= EipTExtTimecard.OUTGOING_COMEBACK_PER_DAY; i++) {
          rd.setOutgoingTime(record.getOutgoingTime(i), i);
          rd.setComebackTime(record.getComebackTime(i), i);
        }
        listrd.setRd(rd);

        datemap.put(checkdate, listrd);
      }
      /*
       * ExtTimecardListResultData listrd = (ExtTimecardListResultData) datemap
       * .get(checkdate);
       */

      return null;

    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 指定した2つの日付を比較する．
   * 
   * @param date1
   * @param date2
   * @param checkTime
   *          時間まで比較する場合，true．
   * @return 等しい場合，0. date1>date2の場合, 1. date1 <date2の場合, 2.
   */
  @SuppressWarnings("unused")
  private boolean sameDay(Date date1, Date date2) {
    Calendar cal1 = Calendar.getInstance();
    Calendar cal2 = Calendar.getInstance();
    cal1.setTime(date1);
    cal2.setTime(date2);

    int date1Year = cal1.get(Calendar.YEAR);
    int date1Month = cal1.get(Calendar.MONTH) + 1;
    int date1Day = cal1.get(Calendar.DATE);
    int date2Year = cal2.get(Calendar.YEAR);
    int date2Month = cal2.get(Calendar.MONTH) + 1;
    int date2Day = cal2.get(Calendar.DATE);

    if (date1Year == date2Year
      && date1Month == date2Month
      && date1Day == date2Day) {
      return true;
    }
    return false;
  }

  /**
   * 検索条件を設定した Criteria を返します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  private SelectQuery<EipTExtTimecard> getSelectQueryDetail(RunData rundata,
      Context context) {
    SelectQuery<EipTExtTimecard> query = Database.query(EipTExtTimecard.class);
    Expression exp =
      ExpressionFactory.matchExp(EipTExtTimecard.USER_ID_PROPERTY, Integer
        .valueOf(ALEipUtils.getUserId(rundata)));

    Calendar calendar_now = Calendar.getInstance();
    Calendar from_calendar = Calendar.getInstance();

    int hour = timecard_system.getChangeHour().intValue();
    // int hour = 4;
    from_calendar.set(Calendar.HOUR_OF_DAY, hour);
    from_calendar.set(Calendar.MINUTE, 0);
    from_calendar.set(Calendar.SECOND, 0);
    if (calendar_now.before(from_calendar)) {
      from_calendar.add(Calendar.DAY_OF_MONTH, -1);
    }
    from_calendar.set(Calendar.HOUR_OF_DAY, 0);
    Expression exp2 =
      ExpressionFactory.matchExp(
        EipTExtTimecard.PUNCH_DATE_PROPERTY,
        from_calendar.getTime());

    query.setQualifier(exp.andExp(exp2));

    return query;
  }

  /**
   * 詳細データを取得します。 <BR>
   * 
   * @param rundata
   * @param context
   * @return
   */
  @Override
  public EipTExtTimecard selectDetail(RunData rundata, Context context) {
    try {
      Calendar cal = Calendar.getInstance();
      nowtime =
        cal.get(Calendar.HOUR_OF_DAY) + "時" + cal.get(Calendar.MINUTE) + "分";

      SelectQuery<EipTExtTimecard> query =
        getSelectQueryDetail(rundata, context);
      query.orderDesending(EipTExtTimecard.PUNCH_DATE_PROPERTY);
      List<EipTExtTimecard> list = query.fetchList();
      if (list != null && list.size() > 0) {
        return list.get(0);
      } else {
        return null;
      }
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * ResultData に値を格納して返します。（詳細データ） <BR>
   * 
   * @param obj
   * @return
   */
  @Override
  protected Object getResultDataDetail(EipTExtTimecard record) {
    try {

      ExtTimecardResultData rd = new ExtTimecardResultData();
      rd.initField();
      rd.setClockInTime(record.getClockInTime());
      rd.setClockOutTime(record.getClockOutTime());
      rd.setPunchDate(record.getPunchDate());
      for (int i = 1; i <= EipTExtTimecard.OUTGOING_COMEBACK_PER_DAY; i++) {
        rd.setOutgoingTime(record.getOutgoingTime(i), i);
        rd.setComebackTime(record.getComebackTime(i), i);
      }
      return rd;
    } catch (Exception ex) {
      logger.error("Exception", ex);
      return null;
    }
  }

  /**
   * 指定グループや指定ユーザをセッションに設定する．
   * 
   * @param rundata
   * @param context
   * @throws ALDBErrorException
   */
  private void setupLists(RunData rundata, Context context) {
    target_group_name = getTargetGroupName(rundata, context);
    if ((target_group_name != null)
      && (!target_group_name.equals(""))
      && (!target_group_name.equals("all"))) {
      userList = ALEipUtils.getUsers(target_group_name);
    } else {
      userList = ALEipUtils.getUsers("LoginUser");
    }

    if (userList == null || userList.size() == 0) {
      target_user_id = "";
      ALEipUtils.removeTemp(rundata, context, ExtTimecardUtils.TARGET_USER_ID);
      return;
    }

    target_user_id = getTargetUserId(rundata, context);
  }

  /**
   * 表示切り替えで指定したグループ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  private String getTargetGroupName(RunData rundata, Context context) {
    String target_group_name = null;
    String idParam = null;
    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，グループ名を取得する．
      idParam =
        rundata.getParameters().getString(ExtTimecardUtils.TARGET_GROUP_NAME);
    }
    target_group_name =
      ALEipUtils.getTemp(rundata, context, ExtTimecardUtils.TARGET_GROUP_NAME);

    if (idParam == null && target_group_name == null) {
      ALEipUtils.setTemp(
        rundata,
        context,
        ExtTimecardUtils.TARGET_GROUP_NAME,
        "all");
      target_group_name = "all";
    } else if (idParam != null) {
      ALEipUtils.setTemp(
        rundata,
        context,
        ExtTimecardUtils.TARGET_GROUP_NAME,
        idParam);
      target_group_name = idParam;
    }
    return target_group_name;
  }

  /**
   * 表示切り替えで指定したユーザ ID を取得する．
   * 
   * @param rundata
   * @param context
   * @return
   */
  private String getTargetUserId(RunData rundata, Context context) {
    String target_user_id = null;
    String idParam = null;
    if (ALEipUtils.isMatch(rundata, context)) {
      // 自ポートレットへのリクエストの場合に，ユーザ ID を取得する．
      idParam =
        rundata.getParameters().getString(ExtTimecardUtils.TARGET_USER_ID);
    }
    target_user_id =
      ALEipUtils.getTemp(rundata, context, ExtTimecardUtils.TARGET_USER_ID);

    if (idParam == null && (target_user_id == null)) {
      // ログインユーザのスケジュールを表示するため，ログイン ID を設定する．
      ALEipUtils.setTemp(
        rundata,
        context,
        ExtTimecardUtils.TARGET_USER_ID,
        userid);
      target_user_id = userid;
    } else if (idParam != null) {
      if (idParam.equals("none")) {
        // グループで表示を切り替えた場合，
        // ログインユーザもしくはユーザリストの一番初めのユーザを
        // 表示するため，ユーザ ID を設定する．
        ALEipUser eipUser = null;
        boolean found = false;
        int length = userList.size();
        for (int i = 0; i < length; i++) {
          eipUser = userList.get(i);
          String eipUserId = eipUser.getUserId().getValueAsString();
          if (userid.equals(eipUserId)) {
            ALEipUtils.setTemp(
              rundata,
              context,
              ExtTimecardUtils.TARGET_USER_ID,
              userid);
            target_user_id = userid;
            found = true;
            break;
          }
        }
        if (!found) {
          eipUser = userList.get(0);
          String userId = eipUser.getUserId().getValueAsString();
          ALEipUtils.setTemp(
            rundata,
            context,
            ExtTimecardUtils.TARGET_USER_ID,
            userId);
          target_user_id = userId;
        }
      } else {
        // ユーザで表示を切り替えた場合，指定したユーザの ID を設定する．
        ALEipUtils.setTemp(
          rundata,
          context,
          ExtTimecardUtils.TARGET_USER_ID,
          idParam);
        target_user_id = idParam;
      }
    }
    return target_user_id;
  }

  /**
   * 表示切り替え時に指定するグループ名
   * 
   * @return
   */
  public String getTargetGroupName() {
    return target_group_name;
  }

  /**
   * 表示切り替え時に指定するユーザ ID
   * 
   * @return
   */
  public String getTargetUserId() {
    return target_user_id;
  }

  /**
   * 指定グループに属するユーザの一覧を取得する．
   * 
   * @param groupname
   * @return
   */
  public List<ALEipUser> getUsers() {
    if (hasAclSummaryOther) {
      return userList;
    } else {
      try {
        List<ALEipUser> users = new ArrayList<ALEipUser>();
        users.add(ALEipUtils.getALEipUser(Integer.parseInt(userid)));
        return users;
      } catch (Exception e) {
        return null;
      }
    }
  }

  /**
   * 部署の一覧を取得する．
   * 
   * @return
   */
  public Map<Integer, ALEipPost> getPostMap() {
    if (hasAclSummaryOther) {
      return ALEipManager.getInstance().getPostMap();
    } else {
      return null;
    }
  }

  /**
   * My グループの一覧を取得する．
   * 
   * @return
   */
  public List<ALEipGroup> getMyGroupList() {
    if (hasAclSummaryOther) {
      return myGroupList;
    } else {
      return null;
    }
  }

  /**
   * ログインユーザの ID を取得する．
   * 
   * @return
   */
  public String getUserId() {
    return userid;
  }

  /**
   * @return
   * 
   */
  @Override
  protected Attributes getColumnMap() {
    Attributes map = new Attributes();
    return map;
  }

  /**
   * 
   * @param id
   * @return
   */
  public boolean isMatch(int id1, long id2) {
    return id1 == (int) id2;
  }

  /**
   * @return
   */
  public ALDateTimeField getViewDate() {
    return viewMonth;
  }

  public String getNowTime() {
    return nowtime;
  }

  /**
   * タイムカード一覧画面で、表示すべきデータをリストにして返します。
   * 
   * @return
   */
  public List<ExtTimecardListResultData> getDateListKeys() {
    try {
      List<ExtTimecardListResultData> list =
        new ArrayList<ExtTimecardListResultData>();

      /** 現在表示中の月の日数を取得 */
      Calendar cal = Calendar.getInstance();
      cal.setTime(viewMonth.getValue());
      int month = Integer.parseInt(viewMonth.getMonth()) - 1;

      /** ExtTimecardListResultDataのインスタンスをlistに投入。 */
      while (cal.get(Calendar.MONTH) == month) {
        Date date = cal.getTime();
        String datestring = ALDateUtil.format(date, "yyyyMMdd");
        if (datemap.containsKey(datestring)) {
          list.add(datemap.get(datestring));
        } else {
          ExtTimecardListResultData rd = new ExtTimecardListResultData();
          rd.initField();
          rd.setDate(date);
          rd.setTimecardSystem(timecard_system);
          rd.setBeforeAfter();

          list.add(rd);
        }
        cal.add(Calendar.DAY_OF_MONTH, 1);
      }

      return list;
    } catch (Exception e) {
     logger.error(e);
      return null;
    }
  }

  public ExtTimecardListResultData getDateListValue(String date_str) {
    return datemap.get(date_str);
  }

  /**
   * アクセス権限チェック用メソッド。<br />
   * アクセス権限の機能名を返します。
   * 
   * @return
   */
  @Override
  public String getAclPortletFeature() {
    return aclPortletFeature;
  }

  public boolean hasAclXlsExport() {
    return hasAclXlsExport;
  }

  /**
   * 表示開始日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewStart() {
    return viewStart;
  }

  /**
   * 表示終了日時を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEnd() {
    return viewEnd;
  }

  /**
   * 表示終了日時 (Criteria) を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewEndCrt() {
    return viewEndCrt;
  }

  /**
   * 前の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getPrevMonth() {
    return prevMonth;
  }

  /**
   * 次の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getNextMonth() {
    return nextMonth;
  }

  /**
   * 今月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getCurrentMonth() {
    return currentMonth;
  }

  /**
   * 現在の月を取得します。
   * 
   * @return
   */
  public ALDateTimeField getViewMonth() {
    return viewMonth;
  }

  /**
   * 今日を取得します。
   * 
   * @return
   */
  public ALDateTimeField getToday() {
    return today;
  }

  /**
   * 勤務形態を返します。
   * 
   * @return
   */
  public EipTExtTimecardSystem getTimecardSystem() {
    return timecard_system;
  }
}
