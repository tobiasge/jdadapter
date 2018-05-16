package de.perdoctus.synology.jdadapter.service;

import java.net.URI;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import de.perdoctus.synolib.DownloadRedirectorClient;
import de.perdoctus.synolib.exceptions.SynoException;
import de.perdoctus.synolib.responses.AddUrlResponse;

@Service
@EnableScheduling
public class DownloadService {

    private static final Logger LOG = LogManager.getLogger(DownloadService.class);

    @Autowired
    private DownloadRedirectorClient drClient;

    @Autowired
    private DownloadQueue downloadQueue;

    public DownloadService() {
    }

    @Scheduled(initialDelay = 10000L, fixedDelay = 5000L)
    public void addDownloads() {
        if (!downloadQueue.hasURIs()) {
            LOG.info("No URIs in queue; exiting...");
            return;
        }

        LOG.debug("Configuration: " + drClient.toString());
        int addedTargets = 0;
        while (downloadQueue.hasURIs()) {
            URI target = downloadQueue.removeURI();
            try {
                AddUrlResponse response = drClient.addDownloadUrl(target);
                if (response.isSuccess()) {
                    addedTargets++;
                } else {
                    LOG.error("Failed to add URL: " + target);
                    downloadQueue.addURI(target);
                }
            } catch (SynoException ex) {
                LOG.error(ex.getMessage(), ex);
                downloadQueue.addURI(target);
            }
        }
        LOG.debug("Added " + addedTargets + " URL(s) to Download Station");
    }
}