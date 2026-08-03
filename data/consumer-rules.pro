# Retrofit 서비스 메서드의 @NoAuth를 런타임에 읽으므로 어노테이션이 유지돼야 한다.
# 이 어노테이션을 소비하는 모듈(앱)이 항상 이 규칙을 적용받도록 consumer-rules.pro 에 둔다.
-keep @interface com.teamyg.parfait.data.network.NoAuth
