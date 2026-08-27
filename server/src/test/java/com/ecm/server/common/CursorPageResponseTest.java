package com.ecm.server.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CursorPageResponseTest {

    record Item(String id, String name) {}
    record ItemDto(String id, String displayName) {}

    @Test
    void of_whenItemsExceedLimit_shouldSetHasNextTrueAndExtractCursor() {
        List<Item> fetchedItems = List.of(
                new Item("item-1", "Laptop"),
                new Item("item-2", "Mouse"),
                new Item("item-3", "Keyboard") // 3rd item = limit + 1
        );

        CursorPageResponse<Item> response = CursorPageResponse.of(
                fetchedItems,
                2,
                Item::id
        );

        assertTrue(response.isHasNext());
        assertEquals(2, response.getItems().size());
        assertEquals("item-2", response.getNextCursor());
        assertEquals(2, response.getSize());
    }

    @Test
    void of_whenItemsLessThanOrEqualLimit_shouldSetHasNextFalse() {
        List<Item> fetchedItems = List.of(
                new Item("item-1", "Laptop")
        );

        CursorPageResponse<Item> response = CursorPageResponse.of(
                fetchedItems,
                2,
                Item::id
        );

        assertFalse(response.isHasNext());
        assertEquals(1, response.getItems().size());
        assertNull(response.getNextCursor());
    }

    @Test
    void of_withMapper_shouldTransformEntitiesToDtos() {
        List<Item> fetchedItems = List.of(
                new Item("item-1", "Laptop"),
                new Item("item-2", "Mouse"),
                new Item("item-3", "Keyboard")
        );

        CursorPageResponse<ItemDto> response = CursorPageResponse.of(
                fetchedItems,
                2,
                Item::id,
                item -> new ItemDto(item.id(), item.name().toUpperCase())
        );

        assertTrue(response.isHasNext());
        assertEquals(2, response.getItems().size());
        assertEquals("item-2", response.getNextCursor());
        assertEquals("LAPTOP", response.getItems().get(0).displayName());
    }
}
