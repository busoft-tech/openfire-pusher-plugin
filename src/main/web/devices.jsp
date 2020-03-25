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

    String username = ParamUtils.getStringParameter(request, "username", null);
    String resource = ParamUtils.getStringParameter(request, "resource", null);
    int pageIndex = ParamUtils.getIntParameter(request, "pageIndex", 1);
    int pageSize = ParamUtils.getIntParameter(request, "pageSize", 25);
    if (csrfCheck && username != null && resource != null)
    {
        PusherManager.deleteDevice(username, resource);
        String newUrl = String.format("devices.jsp?deletesuccess=true&pageIndex=%s&pageSize=%s", pageIndex, pageSize);
        response.sendRedirect(newUrl);
    }
    else if (username != null && resource != null && !csrfCheck)
    {
        String newUrl = String.format("devices.jsp?deletesuccess=false&pageIndex=%s&pageSize=%s", pageIndex, pageSize);
        response.sendRedirect(newUrl);
    }
%>

<html>
    <head>
        <title>
            <fmt:message key="Device List"/>
        </title>
        <meta name="pageID" content="devices"/>
        <script type="text/javascript" >
            function handleDelete(username, resource, pageIndex, pageSize)
            {
                if (confirm('<fmt:message key="Delete device" /> username: ' + username + ' resource: ' + resource + '?'))
                {
                    var uri = encodeURI('devices.jsp?csrf=${csrf}&username=' + username + '&resource=' + resource + '&pageIndex=' + pageIndex + "&pageSize=" + pageSize);
                    location.assign(uri);
                }
            }

            function onPageSizeChange(search, pageSize)
            {
                var uri = 'devices.jsp?search=' + search + '&pageIndex=1&pageSize=' + pageSize;
                location.assign(uri);
            }
        </script>
    </head>
    <body>
        <c:set var="PAGE_SIZE_LIST" value="${[25, 50, 75, 100, 250, 500]}" />

        <c:set var="pageIndex" value="${not empty param['pageIndex'] ? param['pageIndex'] : 1}" />
        <c:set var="pageSize" value="${not empty param['pageSize'] ? param['pageSize'] : 25}" />
        <c:choose>
            <c:when test="${empty param.search}">
                <c:set var="deviceCount" value="${pm:getTotalDeviceCount()}" />
            </c:when>
            <c:otherwise>
                <c:set var="deviceCount" value="${pm:getSearchTotalDeviceCount(param.search)}" />
            </c:otherwise>
        </c:choose>

        <c:set var="dividedValue" value="${deviceCount / pageSize}" />

        <fmt:formatNumber value="${dividedValue+(1-(dividedValue%1))%1}" var="numberOfPages" type="number" pattern="#" />

        <c:set var="lastPageCount" value="${deviceCount - (pageSize * (numberOfPages - 1))}" />

        <c:choose>
            <c:when test="${empty param.search}">
                <c:set var="devices" value="${pm:getDeviceList(pageIndex, pageSize)}" />
            </c:when>
            <c:otherwise>
                <c:set var="devices" value="${pm:searchDeviceList(param.search, pageIndex, pageSize)}" />
            </c:otherwise>
        </c:choose>

        <c:if test="${param.deletesuccess eq 'true'}">
            <div class="jive-success">
                <table cellpadding="0" cellspacing="0" border="0">
                    <tbody>
                        <tr>
                            <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt="Delete Succeeded"/></td>
                            <td class="jive-icon-label">
                                Delete succeeded
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
            <br />
        </c:if>

        <c:choose>
            <c:when test="${param.deletesuccess eq 'false'}">
                <div class="jive-success">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/success-16x16.gif" width="16" height="16" border="0" alt="Delete Succeeded"/></td>
                                <td class="jive-icon-label">
                                    Delete succeeded
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
            <c:when test="${param.deletesuccess eq 'false'}">
                <div class="jive-error">
                    <table cellpadding="0" cellspacing="0" border="0">
                        <tbody>
                            <tr>
                                <td class="jive-icon"><img src="images/error-16x16.gif" width="16" height="16" border="0" alt="Delete Failed"/></td>
                                <td class="jive-icon-label">
                                    Delete failed
                                </td>
                            </tr>
                        </tbody>
                    </table>
                </div>
                <br />
            </c:when>
        </c:choose>

        <fmt:message key="Total devices" />: ${deviceCount}, <fmt:message key="Showing" />
        <c:choose>
            <c:when test="${numberOfPages != pageIndex}" >
                ${(pageIndex - 1) * pageSize + 1} - ${(pageIndex - 1) * pageSize + pageSize} --
            </c:when>
            <c:otherwise>
                ${(pageIndex - 1) * pageSize + 1} - ${(pageIndex - 1) * pageSize + lastPageCount} --
            </c:otherwise>
        </c:choose>

        <fmt:message key="Devices per page" />
        <select size="1" onchange="onPageSizeChange('${param.search}', this.options[this.selectedIndex].value)">
            <c:forEach var="range" items="${PAGE_SIZE_LIST}">
                <option value="${range}" ${range == pageSize ? "selected" : ""} >
                    ${range}
                </option>
            </c:forEach>
        </select>

        <br/>

        <fmt:message key="Pages: "/>
        <c:forEach var="index" begin="1" end="${numberOfPages}">
            <a class="${index == pageIndex ? 'jive-current' : ''}" href="devices.jsp?search=${param.search}&pageIndex=${index}&pageSize=${pageSize}">${index}</a>
        </c:forEach>
        <div class="jive-table">
            <form action="devices.jsp" method="get">
                <input type="text" name="search" value="${param.search}" />
                <input type="hidden" name="pageIndex" value="1" />
                <input type="hidden" name="pageSize" value="${pageSize}" />
                <button type="submit"><fmt:message key="Search" /></button>
            </form>
            <table cellpadding="0" cellspacing="0" border="0" width="100%">
                <thead>
                    <tr>
                        <th>&nbsp;</th>
                        <th nowrap><fmt:message key="Username" /></th>
                        <th nowrap><fmt:message key="Resource" /></th>
                        <th nowrap><fmt:message key="Token" /></th>
                        <th nowrap><fmt:message key="Type" /></th>
                        <th nowrap><fmt:message key="Delete" /></th>
                    </tr>
                </thead>
                <tbody>
                    <c:forEach var="device" items="${devices}" varStatus="loop" >
                        <tr class="jive-${loop.index % 2 == 0 ? 'even' : 'odd'}" >
                            <td width="2%" >
                                ${(pageIndex - 1) * pageSize + loop.count}
                            </td>
                            <td width="24%" >
                                ${device.username}
                            </td>
                            <td width="24%" >
                                ${device.resource}
                            </td>
                            <td width="24%" >
                                ${device.token}
                            </td>
                            <td width="24%" >
                                ${device.type}
                            </td>
                            <td width="2%" >
                                <a href="#" onclick="handleDelete('${device.username}', '${device.resource}', '${pageIndex}', '${pageSize}')" title="<fmt:message key='Delete device?' />" >
                                    <img src="images/delete-16x16.gif" width="16" height="16" border="0" alt="<fmt:message key='Confirm' /> asd" >
                                </a>
                            </td>
                        </tr>
                    </c:forEach>
                </tbody>
            </table>
        </div>

        <fmt:message key="Pages: "/>
        <c:forEach var="index" begin="1" end="${numberOfPages}">
            <a class="${index == pageIndex ? 'jive-current' : ''}" href="devices.jsp?pageIndex=${index}&pageSize=${pageSize}">${index}</a>
        </c:forEach>
    </body>
</html>