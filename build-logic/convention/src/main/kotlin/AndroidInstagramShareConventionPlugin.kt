import com.android.build.api.dsl.LibraryExtension
import com.teamyg.parfait.buildlogic.setConfigInstagramShare
import org.gradle.kotlin.dsl.findByType

/** 인스타그램 스토리 공유 귀속용 Facebook App ID 를 BuildConfig.FACEBOOK_APP_ID 로 싣는다 */
class AndroidInstagramShareConventionPlugin : BaseConventionPlugin({
    val libraryExtension: LibraryExtension = extensions.findByType(LibraryExtension::class)
        ?: error("must be applied com.android.library")

    setConfigInstagramShare(libraryExtension)
})
