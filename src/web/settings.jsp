<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="tr.com.busoft.openfire.pusher.PusherManager" %>
<%@ page import="tr.com.busoft.openfire.pusher.PusherProperty" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/pushermanager.tld" prefix="pm"%>

<%
    boolean androidUpdate = request.getParameter("androidUpdate") != null;
    boolean iosUpdate = request.getParameter("iosUpdate") != null;
    boolean androidCredentialUpdate = request.getParameter("androidCredentialUpdate") != null;
    boolean iosCredentialUpdate = request.getParameter("iosCredentialUpdate") != null;

    boolean csrfCheck = false;
    if (androidUpdate || iosUpdate || androidCredentialUpdate || iosCredentialUpdate)
    {
        Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
        String csrfParam = ParamUtils.getParameter(request, "csrf");
        if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam))
        {
            String newUrl = "settings.jsp?savesucceeded=false";
            response.sendRedirect(newUrl);
        }
        else
        {
            csrfCheck = true;
        }
    }

    String csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);
    pageContext.setAttribute("fcmcredentialpath", PusherProperty.FCM_CREDENTIAL_FILE_PATH);
    pageContext.setAttribute("apnscredentialpath", PusherProperty.APNS_PKCS8_FILE_PATH);

    if (csrfCheck)
    {
        String newUrl = "settings.jsp?savesucceeded=true";
        if (iosUpdate)
        {
            String bundleId = ParamUtils.getStringParameter(request, "bundleId", null);
            String key = ParamUtils.getStringParameter(request, "key", null);
            String teamId = ParamUtils.getStringParameter(request, "teamId", null);
            String sandbox = ParamUtils.getStringParameter(request, "sandbox", null);

            PusherManager.setIosSettings(bundleId, key, teamId, sandbox);
            response.sendRedirect(newUrl);
        }

        if (androidUpdate)
        {
            String projectId = ParamUtils.getStringParameter(request, "projectId", null);
            PusherManager.setAndroidSettings(projectId);
            response.sendRedirect(newUrl);
        }

        if (iosCredentialUpdate)
        {
            String apns = ParamUtils.getStringParameter(request, "apns", null);
            PusherManager.writeCredentialFileContent(apns, "ios");
            response.sendRedirect(newUrl);
        }

        if (androidCredentialUpdate)
        {
            String fcm = ParamUtils.getStringParameter(request, "fcm", null);
            PusherManager.writeCredentialFileContent(fcm, "android");
            response.sendRedirect(newUrl);
        }
    }
%>

<html>
    <head>
        <title>
            <fmt:message key="settings.title"/>
        </title>
        <meta name="pageID" content="settings"/>
    </head>
    <body>
        <c:choose>
            <c:when test="${param.savesucceeded eq 'true'}">
                <div class="jive-success">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key='settings.savesuccess' />"/></td>
                                <td class="jive-icon-label">
                                    <fmt:message key='settings.savesuccess' />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
            <c:when test="${param.savesucceeded eq 'false'}">
                <div class="jive-error">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key='settings.savefail' />"/></td>
                                <td class="jive-icon-label">
                                    <fmt:message key='settings.savefail' />
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
        </c:choose>

        <form action="settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="settings.ios" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.bundleid" />
                            </td>
                            <td>
                                <input type="text" name="bundleId" size="80" value='${pm:getProperty("pusher.apple.apns.bundleId")}' />
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.key" />
                            </td>
                            <td>
                                <input type="text" name="key" size="80" value='${pm:getProperty("pusher.apple.apns.key")}' />
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.teamid" />
                            </td>
                            <td>
                                <input type="text" name="teamId" size="80" value='${pm:getProperty("pusher.apple.apns.teamId")}' />
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.sandbox" />
                            </td>
                            <td>
                                <input type="text" name="sandbox" size="80" value='${pm:getProperty("pusher.apple.apns.sandbox")}' />
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="iosUpdate">
                <fmt:message key="settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="settings.android" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="settings.android.projectid" />
                            </td>
                            <td>
                                <input type="text" name="projectId" size="80" value='${pm:getProperty("pusher.google.fcm.projectId")}' />
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="androidUpdate">
                <fmt:message key="settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="settings.ios.file" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="settings.path"/>: <text readonly size="80">${apnscredentialpath}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <textarea name="ios" cols="70" rows="10"></textarea>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="iosCredentialUpdate">
                <fmt:message key="settings.save" />
            </button>
        </form>
        <br />
        <br />

        <form action="settings.jsp" method="post">
            <input type="hidden" name="csrf" value="${csrf}" />
            <div class="jive-contentBoxHeader">
                <fmt:message key="settings.android.file" />
            </div>
            <div class="jive-contentBox">
                <table cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td>
                                <fmt:message key="settings.path" />: <text readonly size="80">${fcmcredentialpath}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <textarea name="fcm" cols="70" rows="10"></textarea>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <button type="submit" name="androidCredentialUpdate">
                <fmt:message key="settings.save" />
            </button>
        </form>
    </body>
</html>
