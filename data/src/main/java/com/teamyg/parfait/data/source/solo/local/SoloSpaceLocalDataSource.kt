package com.teamyg.parfait.data.source.solo.local

import com.teamyg.parfait.data.model.entity.SoloSpaceRecordEntity

/**
 * 혼자 공간 디렉터리(`canvas.json` + `images/`)의 파일 I/O 만 맡는다. 순서·롤백은 저장소가 정한다.
 * 모든 메서드는 블로킹이고 실패를 예외로 던진다.
 */
interface SoloSpaceLocalDataSource {
    /** 기록 파일이 없으면 `null`. 그 밖의 읽기·파싱 실패는 빈 기록으로 접지 않고 던진다 */
    fun readRecord(): SoloSpaceRecordEntity?

    /** 임시 파일에 쓰고 rename 한다. 도중에 죽어도 이전 기록이 남는다 */
    fun writeRecord(record: SoloSpaceRecordEntity)

    fun deleteRecord()

    /** [sourcePath] 를 `images/`[fileName] 으로 복사한다. 실패하면 반쯤 쓴 사본을 남기지 않는다 */
    fun copyImage(
        sourcePath: String,
        fileName: String,
    )

    fun deleteImage(fileName: String)

    fun clearImages()

    /** `images/` 안의 [fileName] 을 가리키는 `file://` 절대 URI */
    fun imageUri(fileName: String): String
}
