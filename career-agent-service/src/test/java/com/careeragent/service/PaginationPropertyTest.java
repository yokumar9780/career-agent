package com.careeragent.service;

import net.jqwik.api.*;
import net.jqwik.api.constraints.IntRange;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * **Validates: Requirements 18.5, 8.7**
 */
@Label("Feature: career-agent, Property 15: Pagination Metadata Consistency")
class PaginationPropertyTest {

    @Property(tries = 500)
    @Label("totalPages equals ceil(totalElements / pageSize)")
    void totalPagesMatchesCeilDivision(
            @ForAll @IntRange(min = 0, max = 10000) int totalElements,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        int expectedTotalPages = (totalElements == 0) ? 0 : (int) Math.ceil((double) totalElements / pageSize);

        PageRequest pageable = PageRequest.of(0, pageSize);
        List<String> content = createPageContent(0, pageSize, totalElements);
        Page<String> page = new PageImpl<>(content, pageable, totalElements);

        assertThat(page.getTotalPages()).isEqualTo(expectedTotalPages);
    }

    @Property(tries = 500)
    @Label("Each page contains at most pageSize items")
    void eachPageContainsAtMostPageSizeItems(
            @ForAll @IntRange(min = 0, max = 10000) int totalElements,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        int totalPages = (totalElements == 0) ? 0 : (int) Math.ceil((double) totalElements / pageSize);
        if (totalPages == 0) {
            return; // no pages to check
        }

        for (int pageNum = 0; pageNum < totalPages; pageNum++) {
            PageRequest pageable = PageRequest.of(pageNum, pageSize);
            List<String> content = createPageContent(pageNum, pageSize, totalElements);
            Page<String> page = new PageImpl<>(content, pageable, totalElements);

            assertThat(page.getContent().size()).isLessThanOrEqualTo(pageSize);
        }
    }

    @Property(tries = 500)
    @Label("Sum of items across all pages equals totalElements")
    void sumOfItemsAcrossAllPagesEqualsTotalElements(
            @ForAll @IntRange(min = 0, max = 5000) int totalElements,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        int totalPages = (totalElements == 0) ? 0 : (int) Math.ceil((double) totalElements / pageSize);

        int totalItems = 0;
        for (int pageNum = 0; pageNum < totalPages; pageNum++) {
            List<String> content = createPageContent(pageNum, pageSize, totalElements);
            totalItems += content.size();
        }

        assertThat(totalItems).isEqualTo(totalElements);
    }

    @Property(tries = 500)
    @Label("Empty result set yields zero total pages")
    void emptyResultSetYieldsZeroTotalPages(
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        PageRequest pageable = PageRequest.of(0, pageSize);
        Page<String> page = new PageImpl<>(Collections.emptyList(), pageable, 0);

        assertThat(page.getTotalPages()).isZero();
        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Property(tries = 500)
    @Label("Last page contains remaining items (totalElements mod pageSize or pageSize)")
    void lastPageContainsRemainingItems(
            @ForAll @IntRange(min = 1, max = 10000) int totalElements,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        int totalPages = (int) Math.ceil((double) totalElements / pageSize);
        int lastPageNum = totalPages - 1;

        int expectedLastPageSize = totalElements % pageSize;
        if (expectedLastPageSize == 0) {
            expectedLastPageSize = pageSize;
        }

        List<String> content = createPageContent(lastPageNum, pageSize, totalElements);
        PageRequest pageable = PageRequest.of(lastPageNum, pageSize);
        Page<String> page = new PageImpl<>(content, pageable, totalElements);

        assertThat(page.getContent().size()).isEqualTo(expectedLastPageSize);
    }

    @Property(tries = 200)
    @Label("Page metadata reports correct pageSize regardless of content size")
    void pageMetadataReportsConfiguredPageSize(
            @ForAll @IntRange(min = 0, max = 5000) int totalElements,
            @ForAll @IntRange(min = 1, max = 100) int pageSize) {

        PageRequest pageable = PageRequest.of(0, pageSize);
        List<String> content = createPageContent(0, pageSize, totalElements);
        Page<String> page = new PageImpl<>(content, pageable, totalElements);

        assertThat(page.getSize()).isEqualTo(pageSize);
        assertThat(page.getTotalElements()).isEqualTo(totalElements);
    }

    // --- Helpers ---

    /**
     * Creates simulated page content for the given page number, page size, and total elements.
     */
    private List<String> createPageContent(int pageNum, int pageSize, int totalElements) {
        int startIndex = pageNum * pageSize;
        int endIndex = Math.min(startIndex + pageSize, totalElements);
        if (startIndex >= totalElements) {
            return Collections.emptyList();
        }
        return IntStream.range(startIndex, endIndex)
                .mapToObj(i -> "item-" + i)
                .toList();
    }
}
