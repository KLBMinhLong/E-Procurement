package com.eprocure.approval.application.service;

public record PageMeta(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean isFirst,
        boolean isLast) {

    public static PageMeta of(long totalElements, int page, int size) {
        int totalPages = size <= 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        boolean isFirst = page <= 1;
        boolean isLast = totalPages == 0 || page >= totalPages;
        return new PageMeta(page, size, totalElements, totalPages, isFirst, isLast);
    }
}
