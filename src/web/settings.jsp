<%@ page contentType="text/html; charset=UTF-8" %>

<%@ page import="org.jivesoftware.util.*" %>
<%@ page import="tr.com.busoft.openfire.pusher.PusherManager" %>

<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="/WEB-INF/pushermanager.tld" prefix="pm"%>

<%
    boolean csrfCheck = true;
    Cookie csrfCookie = CookieUtils.getCookie(request, "csrf");
    String csrfParam = ParamUtils.getParameter(request, "csrf");

    if (csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals(csrfParam))
    {
        csrfCheck = false;
    }

    csrfParam = StringUtils.randomString(15);
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute("csrf", csrfParam);

    String apns = ParamUtils.getStringParameter(request, "apns", null);
    String fcm = ParamUtils.getStringParameter(request, "fcm", null);
    if (csrfCheck)
    {
        String newUrl = "settings.jsp?savesucceeded=true";
        if (apns != null)
        {
            PusherManager.writeCredentialFileContent(apns, "ios");
            response.sendRedirect(newUrl);
        }

        if (fcm != null)
        {
            PusherManager.writeCredentialFileContent(fcm, "android");
            response.sendRedirect(newUrl);
        }
    }
    else if ((apns != null || fcm != null) && !csrfCheck)
    {
        String newUrl = "settings.jsp?savesucceeded=false";
        response.sendRedirect(newUrl);
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

        <div class="jive-table">
            <form action="settings.jsp" method="post">
                <input type="hidden" name="csrf" value="${csrf}" />
                <table cellpadding="0" cellspacing="0" border="0" width="60%">
                    <tbody>
                        <thead>
                            <tr>
                                <th nowrap><fmt:message key="settings.ios" /></th>
                            </tr>
                        </thead>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.bundleid" />
                            </td>
                            <td>
                                <text readonly size="80">${pm:getProperty("pusher.apple.apns.bundleId")}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.key" />
                            </td>
                            <td>
                                <text readonly size="80">${pm:getProperty("pusher.apple.apns.key")}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.teamid" />
                            </td>
                            <td>
                                <text readonly size="80">${pm:getProperty("pusher.apple.apns.teamId")}</text>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <fmt:message key="settings.ios.sandbox" />
                            </td>
                            <td>
                                <text readonly size="80">${pm:getProperty("pusher.apple.apns.sandbox")}</text>
                            </td>
                        </tr>
                        <thead>
                            <tr>
                                <th nowrap><fmt:message key="settings.android" /></th>
                            </tr>
                        </thead>
                        <tr>
                            <td>
                                <fmt:message key="settings.android.projectid" />
                            </td>
                            <td>
                                <text readonly size="80">${pm:getProperty("pusher.google.fcm.projectId")}</text>
                            </td>
                        </tr>
                        <thead>
                            <tr>
                                <th nowrap><fmt:message key="settings.credentials" /></th>
                            </tr>
                        </thead>
                        <tr>
                            <td width="20%">
                                <fmt:message key="settings.android.file" />
                            </td>
                            <td>
                                <textarea name="fcm" cols="70" rows="10"></textarea>
                            </td>
                            <td width="35%">
                                <button type="submit">
                                    <fmt:message key="settings.save" />
                                </button>
                            </td>
                        </tr>
                        <tr>
                            <td width="20%">
                                <fmt:message key="settings.ios.file" />
                            </td>
                            <td>
                                <textarea name="apns" cols="70" rows="10"></textarea>
                            </td>
                            <td width="35%">
                                <button type="submit">
                                    <fmt:message key="settings.save" />
                                </button>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </form>
        </div>
    </body>
</html>
