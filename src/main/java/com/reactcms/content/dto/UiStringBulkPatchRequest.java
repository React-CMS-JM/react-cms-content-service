package com.reactcms.content.dto;

import java.util.ArrayList;
import java.util.List;

public class UiStringBulkPatchRequest {
    public List<Item> items = new ArrayList<>();

    public static class Item {
        public String stringKey;
        public String languageCode;
        public String stringValue;
    }
}
