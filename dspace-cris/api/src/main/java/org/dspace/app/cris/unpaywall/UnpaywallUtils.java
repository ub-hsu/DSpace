/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.unpaywall;

import org.apache.log4j.Logger;
import org.dspace.app.cris.unpaywall.model.Unpaywall;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import it.cilea.osd.common.core.TimeStampInfo;

public class UnpaywallUtils {

    private static final String DOI_DEFAULT_BASEURL = "https://doi.org/";
    
    /** log4j logger */
    private static Logger log = Logger.getLogger(UnpaywallUtils.class);

    public static JSONObject convertUnpaywallStringToJson(String source)
    {
    	return new JSONObject(source);
    }
    
    public static UnpaywallRecord convertStringToUnpaywallRecord(String source)
    {
    	JSONObject obj = convertUnpaywallStringToJson(source);
    	
    	UnpaywallBestOA unpBestOA = new UnpaywallBestOA();
    	UnpaywallRecord unpaywallRecord = new UnpaywallRecord();
    	
    	if (obj.isNull("best_oa_location")) return null;

    	JSONObject bestOA = obj.getJSONObject("best_oa_location");
    	String url = "";
    	if(!bestOA.isNull("url_for_pdf")){
    		url = bestOA.getString("url_for_pdf");			
		}
    	unpBestOA.setUrl_for_pdf(url);
        unpBestOA.setHost_type(bestOA.getString("host_type"));
        unpBestOA.setVersion(bestOA.getString("version"));
        unpBestOA.setEvidence(bestOA.getString("evidence"));

        JSONArray oaLocations = obj.getJSONArray("oa_locations");
        UnpaywallOA[] unpaywallOAArray = new UnpaywallOA[oaLocations
                .length()];
        UnpaywallOA unpaywallOA = new UnpaywallOA();

        for (int i = 0; i < oaLocations.length(); i++)
        {
            unpaywallOA.setEndpoint_id(
                    oaLocations.getJSONObject(i).optString("endpoint_id"));
            unpaywallOA.setEvidence(
                    oaLocations.getJSONObject(i).optString("evidence"));
            unpaywallOA.setHost_type(
                    oaLocations.getJSONObject(i).optString("host_type"));
            unpaywallOA.setIs_best(
                    oaLocations.getJSONObject(i).optBoolean("is_best"));
            unpaywallOA.setLicense(
                    oaLocations.getJSONObject(i).optString("license"));
            unpaywallOA.setPmh_id(
                    oaLocations.getJSONObject(i).optString("pmh_id"));
            unpaywallOA.setRepository_institution(oaLocations
                    .getJSONObject(i).optString("repository_institution"));
            unpaywallOA.setUpdated(
                    oaLocations.getJSONObject(i).optString("updated"));
            unpaywallOA
                    .setUrl(oaLocations.getJSONObject(i).optString("url"));
            unpaywallOA.setUrl_for_landing_page(oaLocations.getJSONObject(i)
                    .optString("url_for_landing_page"));
            unpaywallOA.setUrl_for_pdf(
                    oaLocations.getJSONObject(i).optString("url_for_pdf"));
            unpaywallOA.setVersion(
                    oaLocations.getJSONObject(i).optString("version"));
            unpaywallOAArray[i] = unpaywallOA;
        }

        unpaywallRecord.setDoi(obj.optString("doi"));
        unpaywallRecord.setPublished_date(obj.optString("published_date"));
        unpaywallRecord.setUpdated("updated");
        unpaywallRecord.setUnpaywallBestOA(unpBestOA);
        unpaywallRecord.setUnpaywallOA(unpaywallOAArray);
        
        return unpaywallRecord;
    }

    public static Unpaywall makeUnpaywall(String source, Integer id)
    {
    	Unpaywall destination = new Unpaywall();
    	JSONObject obj = convertUnpaywallStringToJson(source);
    	
    	destination.setTimeStampInfo(new TimeStampInfo());
        destination.setDoi(obj.optString("doi"));
        destination.setJsonRecord(source);
        destination.setItemId(id);
        
        return destination;
    }

    public static String resolveDoi(String doi) {
        if (doi.startsWith(DOI_DEFAULT_BASEURL)) {
            doi = doi.replace(DOI_DEFAULT_BASEURL, "");
        }
        else if (doi.startsWith("http")) {
            doi = doi.substring(doi.indexOf("10."));
        }

        return doi.toLowerCase();
    }
	
}
