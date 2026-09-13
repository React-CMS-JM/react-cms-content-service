package com.reactcms.content.dto;

import java.util.ArrayList;
import java.util.List;

public class MetadataPutRequest {
    public List<MetadataEntry> entries = new ArrayList<>();

    public static class MetadataEntry {
        public String metaKey;
        public String metaValue;
    }
}
