/*
 * Copyright 2013 Christoph Giesche
 *
 * This file is part of jdadapter.
 *
 * jdadapter is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * jdadapter is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with jdadapter.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.perdoctus.synology.jdadapter.controller;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.FormParam;
import javax.ws.rs.PathParam;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import de.perdoctus.synolib.DownloadRedirectorClient;
import de.perdoctus.synolib.exceptions.LoginException;
import de.perdoctus.synolib.exceptions.SynoException;
import de.perdoctus.synolib.responses.AddUrlResponse;
import de.perdoctus.synology.jdadapter.utils.Decrypter;

/**
 * @author Christoph Giesche
 */
@Controller
public class JdAdapter {

    private static final Logger LOG = LogManager.getLogger(JdAdapter.class);
    private static final Map<String, String> URI_REPLACEMENT_LIST = new HashMap<String, String>();

    static {
        URI_REPLACEMENT_LIST.put("^http://share-online.biz/dl/", "http://www.share-online.biz/dl/");
        URI_REPLACEMENT_LIST.put("^http://share-online.biz/download.php\\?id=", "http://www.share-online.biz/dl/");
        URI_REPLACEMENT_LIST.put("^http://www.share-online.biz/download.php\\?id=", "http://www.share-online.biz/dl/");
    }

    @Autowired
    private DownloadRedirectorClient drClient;

    @Autowired
    private String appVersion;

    @RequestMapping(value = "/jdcheck.js", method = RequestMethod.GET)
    public void returnJdScript(final HttpServletResponse resp) throws IOException {
        LOG.info("Got Request from 'classic' click'n'load button.");
        resp.setStatus(200);
        resp.getWriter().println("jdownloader=true");
    }

    @RequestMapping(value = "/crossdomain.xml", method = RequestMethod.GET)
    public void allowCrossdomain(final HttpServletResponse resp) throws IOException {
        final OutputStream response = resp.getOutputStream();
        final InputStream input = getClass().getResourceAsStream("/crossdomain.xml");
        IOUtils.copy(input, response);
        IOUtils.closeQuietly(input);
        IOUtils.closeQuietly(response);
    }

    @RequestMapping(value = "/flash", method = RequestMethod.GET)
    public void enableFlashButton(final HttpServletResponse resp) throws IOException {
        LOG.info("Got Request from flash click'n'load button.");
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().print("JDownloader");
    }

    @RequestMapping(value = "/flash/addcrypted2", method = RequestMethod.POST)
    public void addDownloads(@FormParam("jk") final String jk, @FormParam("crypted") final String crypted,
        @PathParam("source") final String source, @PathParam("jd") final String jd, final HttpServletResponse resp)
        throws IOException {
        LOG.info("Got download request! (Assuming NON-DLC.)");
        handleClassicRequest(jk, crypted, resp);
        LOG.info("Finished.");
    }

    public void handleClassicRequest(final String jk, final String crypted, final HttpServletResponse resp)
        throws IOException {
        LOG.debug("Configuration: " + drClient.toString());

        try {
            final String key = extractKey(jk);
            final List<URI> targets = Decrypter.decryptDownloadUri(crypted, key);
            final List<URI> fixedTargets = fixURIs(targets);

            LOG.debug("Sending download URLs to Synology NAS. Number of URIs: " + targets.size());
            int addedTargets = 0;
            for (URI target : fixedTargets) {
                AddUrlResponse response = drClient.addDownloadUrl(target);
                if (response.isSuccess()) {
                    resp.getWriter().append("Added URL: " + target + "<br />");
                    resp.getWriter().flush();
                    addedTargets++;
                } else {
                    resp.getWriter().append("Failed to add URL: " + target + "<br />");
                    resp.getWriter().flush();
                }
            }
            resp.getWriter().append("Added " + addedTargets + " URL(s) to Downloadstation<br />");
            resp.setStatus(HttpServletResponse.SC_OK);

        } catch (ScriptException ex) {
            LOG.error(ex.getMessage(), ex);
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Failed to evaluate script:\n" + ex.getMessage());

        } catch (SynoException ex) {
            LOG.error(ex.getMessage(), ex);
            if (ex instanceof LoginException) {
                resp.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            } else {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
            }

        } catch (URISyntaxException ex) {
            LOG.error("Decrypted URL seems to be corrupt.", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private String extractKey(String jk) throws ScriptException {
        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine scriptEngine = scriptEngineManager.getEngineByMimeType("text/javascript");
        scriptEngine.eval(jk + "\nvar result = f();");
        return scriptEngine.get("result").toString();
    }

    private List<URI> fixURIs(final List<URI> srcURIs) throws URISyntaxException {
        final List<URI> resultURIs = new ArrayList<URI>(srcURIs.size());
        for (final URI srcURI : srcURIs) {
            String srcURIStr = srcURI.toASCIIString();
            for (final String findPattern : URI_REPLACEMENT_LIST.keySet()) {
                srcURIStr = srcURIStr.replaceAll(findPattern, URI_REPLACEMENT_LIST.get(findPattern));
            }
            resultURIs.add(new URI(srcURIStr));
        }
        return resultURIs;
    }
}
