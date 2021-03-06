package org.dspace.app.webui.cris.servlet;

import java.io.IOException;
import java.sql.SQLException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dspace.app.webui.json.JSONRequest;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.utils.DSpace;

public class TreeViewJsonRest extends DSpaceServlet
{
    private static Logger log = Logger.getLogger(TreeViewJsonRest.class);

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws IOException,
            AuthorizeException
    {
        String pluginName = request.getPathInfo();

        if (pluginName == null)
        {
            pluginName = "";
        }

        if (pluginName.startsWith("/"))
        {
            pluginName = pluginName.substring(1);
            pluginName = pluginName.split("/")[0];
        }
                
        DSpace dspace = new DSpace();
        JSONRequest jsonReq = dspace.getServiceManager().getServiceByName(pluginName, JSONRequest.class);
        
        if (jsonReq == null)
        {
            log.error(LogManager.getHeader(context, "jsonrequest",
                    "No plugin found for manage the path " + pluginName));
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        jsonReq.setSubPath(pluginName);
        try
        {
            response.setContentType("application/json; charset=UTF-8");
            jsonReq.doJSONRequest(context, request, response);
        }
        catch (Exception e)
        {
            // no need to log, logging is already done by the
            // InternalErrorServlet
            // log.error(LogManager.getHeader(context, "jsonrequest",
            // pluginName),
            // e);
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
