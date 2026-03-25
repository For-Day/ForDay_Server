package com.example.ForDay.domain.hobby.dto;

public record CoverChangeResult(Long hobbyId, Long recordId, String updatedCoverUrl, boolean unchanged) {

    public static CoverChangeResult unchanged(Long hobbyId, String currentUrl) {
        return new CoverChangeResult(hobbyId, null, currentUrl, true);
    }

    public static CoverChangeResult changed(Long hobbyId, String newUrl) {
        return new CoverChangeResult(hobbyId, null, newUrl, false);
    }

}
