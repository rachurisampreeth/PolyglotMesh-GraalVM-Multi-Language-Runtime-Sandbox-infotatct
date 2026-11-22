package com.polyglotmesh;

import com.polyglotmesh.service.TemplateCatalog;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TemplateCatalogTest {
    @Test
    void hasExpectedModesAndCode() {
        var list = new TemplateCatalog().getTemplates();
        assertEquals(4, list.size());
        assertTrue(list.stream().anyMatch(t -> "pipeline".equals(t.mode())
            && t.pythonCode().contains("result") && t.javascriptCode().contains("result")));
    }
}
