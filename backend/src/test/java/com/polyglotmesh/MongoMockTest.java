package com.polyglotmesh;

import com.polyglotmesh.data.MongoMock;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MongoMockTest {
    MongoMock db = new MongoMock();
    @Test void readsProducts() { assertEquals(3, db.find("products").size()); }
    @Test void filtersProducts() { assertEquals("Keyboard", db.find("products", Map.of("id", "p2")).get(0).get("name")); }
    @Test void rejectsUnknownCollections() { assertThrows(IllegalArgumentException.class, () -> db.find("secrets")); }
}
