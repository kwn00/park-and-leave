# APK 제출 위치

실행 가능한 release APK는 이 폴더에 다음 이름으로 둡니다.

```text
apk/<팀명>_ParkAndLeave_release.apk
```

팀명은 접수 팀 이름을 그대로 쓰고, 앱 이름은 `ParkAndLeave`로 고정합니다. 예시는 패턴만 보여 주기 위한 것이며, 이 저장소에는 아직 그 이름의 APK를 커밋하지 않습니다.

로컬 release 빌드 파일은 다음 경로에 생성됩니다.

```text
app/build/outputs/apk/release/app-release.apk
```

이 빌드는 현재 디버그 키스토어로 서명되어 있습니다. 제출 전에 위 파일명의 복사본을 이 폴더에 넣으면 됩니다.
