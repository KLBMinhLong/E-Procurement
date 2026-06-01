package com.eprocure.notification.application.service;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast,
        String sort) {

    public static PageMeta of(long totalElements, int page, int size, String sort) {
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        int pages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / normalizedSize);
        return new PageMeta(
                normalizedPage,
                normalizedSize,
                totalElements,
                pages,
                normalizedPage == 1,
                pages == 0 || normalizedPage >= pages,
                sort);
    }
}
