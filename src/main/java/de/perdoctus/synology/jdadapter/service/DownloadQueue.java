package de.perdoctus.synology.jdadapter.service;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

@Service
public class DownloadQueue {

    private List<URI> uriList;

    public DownloadQueue() {
        uriList = new ArrayList<URI>();
    }

    public synchronized void addURI(URI uri) {
        uriList.add(uri);
    }

    public boolean hasURIs() {
        return !uriList.isEmpty();
    }

    public synchronized URI removeURI() {
        if (uriList.isEmpty()) {
            throw new IllegalStateException("Download queue is empty");
        }
        return (URI) uriList.remove(0);
    }
}