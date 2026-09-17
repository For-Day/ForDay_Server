package com.example.ForDay.global.port;

/**
 * @param uploadUrl 클라이언트가 PUT 할 임시 URL
 * @param fileUrl   업로드 완료 후 접근할 최종 URL
 */
public record UploadTarget(String uploadUrl, String fileUrl) {
}
