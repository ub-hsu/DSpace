package org.dspace.app.webui.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.dspace.app.webui.util.DOIQueryConfigurator;
import org.dspace.app.webui.util.DoiFactoryUtils;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.event.Event;
import org.dspace.sort.SortException;
import org.dspace.sort.SortOption;
import org.dspace.utils.DSpace;
import org.hibernate.Session;

public class DoiPendingServlet extends DSpaceServlet
{
    /** log4j category */
    private static Logger log = Logger.getLogger(DoiPendingServlet.class);

    public static int EXCLUDE_ALL = 0;

    public static int EXCLUDE_ANY = 1;

    DSpace dspace = new DSpace();

    SearchService searcher = dspace.getServiceManager().getServiceByName(
            SearchService.class.getName(), SearchService.class);

    IndexingService indexer = dspace.getServiceManager().getServiceByName(
            IndexingService.class.getName(), IndexingService.class);

    @Override
    protected void doDSPost(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        doDSGet(context, request, response);
    }

    @Override
    protected void doDSGet(Context context, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {

        int submit = UIUtil.getIntParameter(request, "submit");
        DOIQueryConfigurator doiConfigurator = new DOIQueryConfigurator();
        int sortBy = doiConfigurator.getSortBy(request, "pending");
        String order = doiConfigurator.getOrder(request, "pending");
        String orderfield = sortBy != -1 ? "bi_sort_" + sortBy + "_sort" : null;
        boolean ascending = SortOption.ASCENDING.equalsIgnoreCase(order);
        int rpp = doiConfigurator.getRPP(request, "pending");
        SortOption sortOption = null;
        if (sortBy > 0)
        {
            try
            {
                sortOption = SortOption.getSortOption(sortBy);
            }
            catch (SortException e)
            {
                log.error(e.getMessage(), e);
            }
        }
        request.setAttribute("order", order);
        request.setAttribute("sort_by", sortOption);

        if (submit != -1)
        {
            if (submit == EXCLUDE_ALL)
            {
                QueryResponse rsp = searchPending(orderfield, ascending);
                List<Item> items = DoiFactoryUtils.getItemsFromSolrResult(
                        rsp.getResults(), context);
                try
                {
                    deletePendings(context, items);
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }
                response.sendRedirect(request.getContextPath()
                        + "/dspace-admin/doi");
                return;
            }
            else if (submit == EXCLUDE_ANY)
            {
                List<UUID> items = UIUtil.getUUIDParameters(request, "pendingdoi");
                try
                {
                    deletePendingsUUID(context, items);
                }
                catch (SearchServiceException e)
                {
                    log.error(e.getMessage(), new RuntimeException(e));
                }
            }
        }

        try
        {
            indexer.commit();
        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), new RuntimeException(e));
        }

        QueryResponse rsp = searchPending(orderfield, ascending);

        List<Item> results = DoiFactoryUtils.getItemsFromSolrResult(
                rsp.getResults(), context);
        Map<UUID, List<String>> doi2items = new HashMap<UUID, List<String>>();
        if (results != null && !results.isEmpty())
        {

            for (Item real : results)
            {
                List<Object[]> rows = getHibernateSession(context).createSQLQuery(
                        "SELECT identifier_doi, " + getColumnNote() + " FROM "
                                + DoiFactoryUtils.TABLE_NAME
                                + " where item_id = :item_id").addScalar("identifier_doi").addScalar("note").setParameter("item_id", real.getID()).list();

                if (rows == null)
                {
                    real.getItemService().clearMetadata(context, real, "dc", "utils", "processdoi", Item.ANY);
                    real.getItemService().update(context, real);
                }
                else
                {
                    List<String> rr = new ArrayList<String>();
                    
                    for(Object[] row : rows) {
                    	rr.add((String)row[0]);
                    	String note = (String)row[1];
                    	rr.add(note == null || note.isEmpty() ? "" : note);
                    	doi2items.put(real.getID(), rr);
                    }
                }
            }
        }

        // total number of pages
        int start = UIUtil.getIntParameter(request, "start") != -1 ? UIUtil
                .getIntParameter(request, "start") : 0;
        int pageTotal = 0;

        if (doi2items != null)
        {
            pageTotal = (int) (1 + ((doi2items.size() - 1) / rpp));
        }
        // current page being displayed
        int pageCurrent = 1 + (start / rpp);

        // pageLast = min(pageCurrent+9,pageTotal)
        int pageLast = ((pageCurrent + 9) > pageTotal) ? pageTotal
                : (pageCurrent + 9);

        // pageFirst = max(1,pageCurrent-9)
        int pageFirst = ((pageCurrent - 9) > 1) ? (pageCurrent - 9) : 1;
        request.setAttribute("doi2items", doi2items);
        request.setAttribute("results", results);
        request.setAttribute("pagetotal", new Integer(pageTotal));
        request.setAttribute("pagecurrent", new Integer(pageCurrent));
        request.setAttribute("pagelast", new Integer(pageLast));
        request.setAttribute("pagefirst", new Integer(pageFirst));
        request.setAttribute("start", start);
        request.setAttribute("total", results.size());
        request.setAttribute("rpp", rpp);
        JSPManager.showJSP(request, response, "/doi/checkerDoiPendings.jsp");

    }

    private String getColumnNote()
    {
        if ("oracle".equals(dspace.getConfigurationService().getProperty(
                "dbname")))
        {
            return "DBMS_LOB.SUBSTR(note)";
        }
        return "note";
    }

    private void deletePendingsUUID(Context context, List<UUID> items)
            throws SQLException, AuthorizeException, SearchServiceException
    {
        List<Item> ii = DoiFactoryUtils.getItems(context, items);
        deletePendings(context, ii);
    }

    private void deletePendings(Context context, List<Item> items)
            throws SQLException, AuthorizeException, SearchServiceException
    {
        for (Item i : items)
        {

            getHibernateSession(context).createSQLQuery("delete from "+ DoiFactoryUtils.TABLE_NAME + " where item_id = :item_id").setParameter("item_id", i.getID()).executeUpdate();
            i.getItemService().clearMetadata(context, i, "dc", "utils", "processdoi", Item.ANY);
            i.getItemService().update(context, i);
            context.addEvent(new Event(Event.UPDATE_FORCE, Constants.ITEM, i
                    .getID(), i.getHandle()));
        }

        context.commit();
        indexer.commit();
    }

    private QueryResponse searchPending(String orderfield, boolean ascending)
    {
        String query = ConfigurationManager.getProperty("doi.pending.query");

        QueryResponse rsp = null;
        try
        {
            SolrQuery solrQuery = new SolrQuery();
            solrQuery.setQuery(query);
            DoiFactoryUtils.prepareDefaultSolrQuery(solrQuery);
            solrQuery.setRows(Integer.MAX_VALUE);
            if (orderfield == null)
            {
                orderfield = "score";
            }
            solrQuery.addSortField(orderfield, ascending ? SolrQuery.ORDER.asc
                    : SolrQuery.ORDER.desc);
            rsp = searcher.search(solrQuery);

        }
        catch (SearchServiceException e)
        {
            log.error(e.getMessage(), e);
        }
        return rsp;
    }

    protected static Session getHibernateSession(Context context) throws SQLException {
        return ((Session) context.getDBConnection().getSession());
    }
}
