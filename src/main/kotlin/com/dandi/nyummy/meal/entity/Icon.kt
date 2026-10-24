package com.dandi.nyummy.meal.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 음식 아이콘. 앱은 id로 `https://cdn.nyummy.co.kr/icons/{id}.png`를 조립해 이미지를 띄운다.
 *
 * [imageUrl]은 같은 URL을 기존 절차대로 함께 저장해 두는 값이다.
 */
@Entity
@Table(name = "icons")
class Icon(
    @Column(name = "name", nullable = false)
    var name: String,

    @Column(name = "image_url", nullable = false)
    var imageUrl: String,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    val id: Long = 0L

    /**
     * 이미지 URL을 저장한다. URL에 id가 들어가므로 행을 저장해 id를 받은 직후에 채운다.
     */
    fun updateImageUrl(imageUrl: String) {
        this.imageUrl = imageUrl
    }
}
