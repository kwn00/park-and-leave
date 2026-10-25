# Park & Leave

기어를 P로 바꾸는 순간 창문, 도어, 조명, 뒷좌석, 공조, 타이어, 워셔액, 경고등, 잔여 에너지를 한 화면으로 모아 "모두 정상" 또는 "확인 n건"으로 보여 주는 정차 전용 앱입니다. 항목이 해결되면 카드가 바로 갱신됩니다.

## 빌드

- JDK 17 이상
- Android SDK Platform 35, Build-Tools 35
- `ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`

```bash
./gradlew assembleDebug assembleRelease
./gradlew test
```

산출물:

- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`

release 빌드는 아직 디버그 키스토어로 서명합니다. `app/build.gradle.kts`의 `release.signingConfig`가 `debug`입니다. 배포용 서명은 나중에 바꿉니다. 난독화는 꺼져 있습니다.

앱 ID는 `io.github.kwn00.parkandleave`입니다. minSdk 28, targetSdk 34, compileSdk 35. 대상 기기는 Android 14 / API 34 자동차 에뮬레이터입니다.

## 에뮬레이터에 설치

Pleos Connect Emulator 2.1.0을 Cold boot로 띄운 뒤:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n io.github.kwn00.parkandleave/.MainActivity
```

release APK도 같은 방식으로 설치할 수 있습니다.

```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```

## Virtual Console에서 상태 바꾸기

에뮬레이터가 뜬 터미널에서:

```bash
adb forward tcp:8088 tcp:8088
```

브라우저에서 `http://127.0.0.1:8088/` 을 엽니다. 콘솔에서 Gear를 P/R/N/D로 바꾸고, 도어, 잠금, 트렁크, 창문, 실내등, 독서등, 하향등, 미등, 공조, 배터리 SOC, 연료, 타이어, 경고등을 조작합니다.

## 신호 디버그 화면

요약 화면의 **신호 디버그**로 들어갑니다. 화면 위쪽에 `GEAR_SELECTION`의 프로퍼티 ID(`289408000`)와 raw 정수, 해석된 기어 이름이 크게 나옵니다. P는 `4`, R은 `2`, N은 `1`, D는 `8`입니다. 그 아래는 앱이 읽은 창문 위치, 도어 위치, 잠금 Boolean, 조명, 좌석 점유, 공조, 타이어 float, 워셔액, 경고등, SOC, 연료, 트립 거리입니다.

**실측**은 에뮬레이터 값입니다. **모의**는 앱 안의 가짜 데이터로, 에뮬레이터 없이 요약 카드를 볼 때 씁니다. 모의 모드에서는 시나리오 버튼과 해소 버튼으로 카드가 줄거나 "모두 정상"이 되는 것을 확인할 수 있습니다. 모의 모드는 주행 제한을 적용하지 않습니다. 실측 모드는 제한이 켜지면 점검 화면을 멈춥니다.

## 기어 P가 실제로 들어오는지 확인

1. 에뮬레이터를 Cold boot로 실행합니다. Quick boot에서는 차량 신호가 동작하지 않을 수 있습니다.
2. `adb install`로 디버그 APK를 설치하고 앱을 엽니다.
3. `adb forward tcp:8088 tcp:8088` 후 Virtual Console을 엽니다.
4. 앱에서 **신호 디버그**를 열고 **실측**이 선택돼 있는지 확인합니다.
5. Virtual Console에서 Gear를 D로 바꿉니다. 디버그 화면의 raw 값이 `8`, 이름이 `DRIVE`로 바뀌는지 봅니다. 요약 화면으로 돌아오면 점검 카드는 나오지 않아야 합니다.
6. Gear를 P(`4`, `PARK`)로 바꿉니다. 요약 화면이 "점검 중"을 거쳐 "모두 정상" 또는 "확인 n건"으로 바뀝니다.
7. 창문을 연 뒤 다시 닫습니다. 디버그 화면의 `window.*` 값과 요약 카드가 같이 바뀌는지 확인합니다.
8. raw가 비어 있고 오류 문구가 보이면 `CAR_POWERTRAIN`으로 `GEAR_SELECTION`을 읽지 못한 것입니다. 그 경우 요약 화면의 **지금 점검**으로 차량 신호만 조회할 수 있습니다.

## 실측 API와 목업

| 기능 | 구현 |
|---|---|
| 기어 | AAOS `CarPropertyManager` `VehiclePropertyIds.GEAR_SELECTION`. Vehicle SDK의 `Gear` API는 "deprecated, use the Google GEAR_SELECTION property"라서 쓰지 않습니다. |
| 창문, 도어, 잠금, 조명, 좌석 점유, 공조, 타이어 저압, 워셔액, SOC, 연료, 트립 거리 | `ai.pleos.playground:Vehicle:2.3.2`. capability를 보고 지원하지 않는 항목은 숨깁니다. 변화는 `register*`로 구독합니다. |
| 경고등 | `Safety.getTelltalesInfo`를 P 진입과 새로고침 때 한 번 호출합니다. SDK 2.3.2 바이트코드에서 `getTelltalesInfo`, `checkTelltalesCapabilities`, `registerTelltales`가 모두 `PRIVATE_CAR_SAFETY`입니다. 일반 앱에서는 실패할 수 있고, 그 오류는 디버그 화면에 남습니다. 실패해도 다른 항목 조회는 계속합니다. 타이어 압력 `registerTirePressure`도 private이라 저압은 조회만 합니다. |
| 음성 | `OnDeviceSpeech` 인터페이스. 구현체는 연결하지 않았고, 문제가 있을 때 짧은 알림음만 냅니다. 모두 정상이면 소리를 내지 않습니다. |
| 창문·조명·잠금 제어 | `VehicleControlGateway`. 항상 사용 불가이고, 카드에는 직접 조작하라는 안내만 있습니다. |
| 장소 프로필 | `PlaceProfileSource`. 항상 기본 프로필입니다. |
| 모의 데이터 | 디버그 화면의 **모의**. 단위 테스트도 이 스냅샷 모델로 점검 규칙을 검증합니다. |

조회 권한은 `android.car.permission.CAR_POWERTRAIN`과 `pleos.car.permission.CAR_WINDOWS`, `CAR_DOORS`, `CAR_LIGHTS`, `CAR_SEATS`, `CAR_SAFETY`, `CAR_ENERGY`, `CAR_DRIVE`, `CAR_CLIMATE`입니다. `PRIVATE_*`와 `CONTROL_*`는 선언하지 않습니다.

주행 중 사용(Distraction Optimization)은 선언하지 않습니다. 정차가 아니면 점검 세션을 열지 않고, `CarUxRestrictions`가 운전자 주의 최적화를 요구하면 실측 세션과 소리를 멈춥니다. 화면에는 애니메이션을 넣지 않았습니다. 카드는 제목, 상태, 안내 세 줄입니다.

`getDoorLockState`의 Boolean은 잠김=`true`로 해석합니다. 디버그 화면의 `locked=`가 반대라면 그 원본 값을 기준으로 보면 됩니다. `getLowTirePressure`는 0이면 정상, 0 초과 1 이하면 저압으로 보고, 더 큰 값은 압력 단위일 수 있어 카드로 올리지 않고 원본만 보여 줍니다. 트립 거리 float의 단위는 SDK 시그니처만으로는 확정하지 않아 숫자 그대로 표시합니다. SOC 20% 미만, 연료 15% 미만을 확인 항목으로 보고, SOC 30% 미만이면서 이번 트립 거리가 0보다 크면 "내일 같은 거리라면 충전 권장"을 한 줄 추가합니다.

## 모듈

- `domain`: 점검 규칙, 세션, 모의 시나리오, 디버그 행. JVM 단위 테스트.
- `designsystem`: 주간/야간 색과 글자 크기.
- `app`: Compose 화면, AAOS 기어, Vehicle SDK, 모의 보드.

## APK 폴더

제출용 APK 이름 규칙은 `apk/README.md`에 있습니다. 이 저장소에는 APK 파일을 넣지 않습니다.
