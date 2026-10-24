package com.dandi.nyummy.admin.enum

/**
 * 어드민이 올리는 이미지의 용도. 객체 키는 `{prefix}/{id}.png`이고, 앱은 CDN 주소에 이 키를 붙여 URL을 조립한다.
 *
 * 앱과 같은 규칙을 여기 한 곳에만 둔다. 어드민 미리보기 URL과 `icons.image_url`도 이 규칙으로 만든다.
 *
 * @property prefix S3 객체 키의 최상위 경로
 * @property maxFileSizeBytes 허용되는 최대 파일 크기(byte)
 */
enum class AdminImagePurpose(val prefix: String, val maxFileSizeBytes: Long) {

    ICON("icons", 512 * 1024),

    // 마이룸 전용 경로가 아니라 아이템 공통 경로를 쓴다. items가 모든 아이템(이후 고양이 치장 포함)의 공통 부모라
    // id가 겹치지 않고, 우편은 아이템 종류를 모른 채 items/{itemId}.png로 조립하기 때문이다.
    ROOM_ITEM("items", 5 * 1024 * 1024),
    ;

    companion object {
        const val CONTENT_TYPE = "image/png"

        // CDN은 길게 들고 있다가 이미지를 바꿀 때만 무효화한다. 무효화가 닿지 않는 기기 캐시는 1시간으로 짧게 둔다.
        const val CACHE_CONTROL = "public, max-age=3600, s-maxage=31536000"
    }

    /** @param id 음식 아이콘은 iconId, 마이룸 아이템은 itemId */
    fun createObjectKey(id: Long): String = "$prefix/$id.png"

    fun createImageUrl(cdnBaseUrl: String, id: Long): String = "$cdnBaseUrl/${createObjectKey(id)}"
}
