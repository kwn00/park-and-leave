# 제출용 APK

제출물은 소스와 실행 가능한 release APK를 함께 둡니다. 서명된 APK는 이 폴더에 다음 이름으로 둡니다.

```text
apk/ParkAndLeave_release.apk
```

키스토어 파일과 비밀번호는 저장소에 넣지 않습니다. `*.jks`와 `*.keystore`는 `.gitignore`에 있습니다. 서명 값은 환경 변수이거나 `~/.gradle/gradle.properties`의 Gradle 속성입니다.

| 이름 | 의미 |
|---|---|
| `PARK_LEAVE_STORE_FILE` | 키스토어 파일 경로 |
| `PARK_LEAVE_STORE_PASSWORD` | 키스토어 비밀번호 |
| `PARK_LEAVE_KEY_ALIAS` | 키 별칭 |
| `PARK_LEAVE_KEY_PASSWORD` | 키 비밀번호 |

네 값이 모두 있고 파일이 있으면 `assembleRelease`가 그 키스토어로 서명합니다. 하나라도 없으면 빌드는 실패하지 않고 디버그 키스토어로 서명합니다. 그 APK도 에뮬레이터에 설치할 수 있습니다. 이 폴더의 `ParkAndLeave_release.apk`는 키스토어 변수가 없을 때 만든 그 서명본입니다. 콘테스트 키가 있으면 같은 이름으로 바꾸면 됩니다.

```bash
./gradlew assembleRelease
cp app/build/outputs/apk/release/app-release.apk apk/ParkAndLeave_release.apk
```
