# Ziti Tunnel Android - 하이브리드 빌드 가이드

## 개요

Windows 환경에서 네이티브 C/C++ 라이브러리 빌드는 매우 어렵습니다. 이 가이드는 **GitHub Actions에서 네이티브 라이브러리만 빌드**하고, **Windows Android Studio에서 앱 개발/디버깅**하는 하이브리드 방식을 설명합니다.

## 장점

✅ **GitHub Actions (Ubuntu Linux):**
- 네이티브 라이브러리 빌드 (OpenSSL, protobuf 등)
- 복잡한 크로스 컴파일 환경 자동 구성
- 무료 (월 2,000분)

✅ **Windows Android Studio:**
- 빠른 앱 빌드 (네이티브 빌드 스킵)
- UI/UX 실시간 수정
- 에뮬레이터/실기기 디버깅
- Kotlin/Java 코드 즉시 반영

## 사용 방법

### 1단계: GitHub에 코드 푸시

```bash
cd d:\ituz-tunnel-android\ziti-tunnel-android

# Git 초기화 (처음만)
git init
git add .
git commit -m "Initial commit"

# GitHub 원격 저장소 연결
git remote add origin https://github.com/[your-username]/ziti-tunnel-android.git
git push -u origin main
```

### 2단계: GitHub Actions에서 네이티브 라이브러리 빌드

1. **자동 빌드 (코드 푸시 시):**
   - `tunnel/src/main/cpp/` 폴더의 C/C++ 코드 수정 후 푸시
   - GitHub Actions가 자동으로 빌드 시작

2. **수동 빌드:**
   - GitHub 저장소 → "Actions" 탭
   - "Build Tunnel Native Libraries Only" 선택
   - "Run workflow" 버튼 클릭

3. **실시간 로그 확인:**
   - 실행 중인 workflow 클릭
   - 각 step의 로그를 실시간으로 확인 가능

### 3단계: 빌드된 .so 파일 다운로드

1. **완료된 workflow 확인:**
   - Actions 탭에서 완료된 빌드 확인 (초록색 체크)

2. **Artifacts 다운로드:**
   - 완료된 workflow 클릭
   - 하단 "Artifacts" 섹션
   - `ziti-tunnel-native-libs` 다운로드 (ZIP 파일)

3. **압축 해제:**
   ```bash
   # 다운로드 폴더에서
   unzip ziti-tunnel-native-libs.zip -d native-libs
   ```

### 4단계: Windows 프로젝트에 복사

```bash
# 네이티브 라이브러리를 jniLibs 폴더로 복사
cd d:\ituz-tunnel-android\ziti-tunnel-android

# 기존 jniLibs 폴더 백업 (선택사항)
mv tunnel/src/main/jniLibs tunnel/src/main/jniLibs.backup

# 다운로드한 파일 복사
cp -r ~/Downloads/native-libs tunnel/src/main/jniLibs
```

복사 후 구조:
```
tunnel/src/main/jniLibs/
├── armeabi-v7a/
│   └── libziti-tunnel.so
├── arm64-v8a/
│   └── libziti-tunnel.so
├── x86/
│   └── libziti-tunnel.so
└── x86_64/
    └── libziti-tunnel.so
```

### 5단계: Android Studio에서 빠른 빌드

```bash
# 터미널에서
cd d:\ituz-tunnel-android\ziti-tunnel-android
./gradlew assembleDebug -PskipDependentBuild

# 또는 Android Studio에서
# Run → Run 'app' (Shift+F10)
```

**빌드 시간:**
- 네이티브 빌드 포함: 30분+ (Windows는 실패 가능)
- 네이티브 빌드 스킵: **30초 이내** ✅

### 6단계: UI 개발 및 디버깅

Android Studio에서 자유롭게 작업:

1. **Kotlin/Java 코드 수정**
   - 즉시 반영 (핫 리로드)

2. **XML 레이아웃 수정**
   - Visual Editor 사용 가능

3. **에뮬레이터 디버깅**
   - 브레이크포인트 설정
   - 변수 값 확인
   - Step-by-step 실행

4. **실제 기기 테스트**
   - USB 연결 후 즉시 설치

## 파일 구조

```
ziti-tunnel-android/
├── .github/
│   └── workflows/
│       ├── android.yml                    # 전체 APK 빌드 (기존)
│       ├── build-dependencies.yml         # 네이티브 의존성 빌드
│       └── build-tunnel-only.yml          # 🆕 Tunnel 라이브러리만 빌드
├── tunnel/
│   ├── src/main/
│   │   ├── cpp/                          # C/C++ 네이티브 코드
│   │   ├── kotlin/                       # Kotlin 앱 코드
│   │   └── jniLibs/                      # 🆕 여기에 .so 파일 복사
│   ├── build.gradle.kts
│   └── CMakeLists.txt
└── app/
    └── src/main/
        ├── kotlin/                        # 앱 UI 코드
        └── res/                           # 리소스 (레이아웃, 이미지 등)
```

## 워크플로우

### 일반적인 개발 사이클

1. **앱 UI/로직 수정 (자주):**
   ```
   Windows Android Studio → 코드 수정 → 빌드 (30초) → 테스트
   ```

2. **네이티브 코드 수정 (가끔):**
   ```
   C/C++ 코드 수정 → GitHub 푸시 → Actions 빌드 (10분) →
   .so 다운로드 → jniLibs에 복사 → Android Studio 빌드
   ```

### 네이티브 코드 수정 시

```bash
# 1. 네이티브 코드 수정
vim tunnel/src/main/cpp/ziti-tunnel.c

# 2. GitHub에 푸시
git add tunnel/src/main/cpp/
git commit -m "Update native tunnel code"
git push

# 3. GitHub Actions 자동 빌드 (10-15분 대기)

# 4. Artifacts 다운로드 및 복사

# 5. Android Studio 빌드
./gradlew assembleDebug -PskipDependentBuild
```

## Gradle 옵션 설명

### `-PskipDependentBuild`
네이티브 의존성 빌드를 스킵합니다. `.so` 파일이 이미 `jniLibs/`에 있을 때 사용.

```bash
# 네이티브 빌드 스킵 (빠름)
./gradlew assembleDebug -PskipDependentBuild

# 전체 빌드 (느림, Windows에서 실패 가능)
./gradlew assembleDebug
```

### Build Variants

```bash
# Debug 빌드 (개발용)
./gradlew assembleDebug -PskipDependentBuild

# Release 빌드 (배포용)
./gradlew assembleRelease -PskipDependentBuild
```

## GitHub Actions 워크플로우 상세

### `build-tunnel-only.yml`

이 워크플로우는:

1. **트리거 조건:**
   - `tunnel/src/main/cpp/` 폴더 변경 시 자동 실행
   - 수동 실행 가능 (`workflow_dispatch`)

2. **빌드 아키텍처:**
   - armeabi-v7a (32-bit ARM)
   - arm64-v8a (64-bit ARM)
   - x86 (32-bit Intel 에뮬레이터)
   - x86_64 (64-bit Intel 에뮬레이터)

3. **결과물:**
   - Artifact 이름: `ziti-tunnel-native-libs`
   - 보관 기간: 30일
   - 포함 내용: 모든 아키텍처의 `.so` 파일

## 트러블슈팅

### Q: jniLibs에 .so 파일을 복사했는데 빌드 실패

```bash
# CMake 캐시 삭제
rm -rf tunnel/.cxx
rm -rf tunnel/build

# 다시 빌드
./gradlew clean
./gradlew assembleDebug -PskipDependentBuild
```

### Q: GitHub Actions 빌드 실패

1. **Actions 탭에서 로그 확인**
2. **일반적인 원인:**
   - CMakeLists.txt 문법 오류
   - 의존성 버전 문제
   - vcpkg.json 설정 오류

### Q: 실제 기기에서 앱 실행 안됨

```bash
# 모든 아키텍처가 빌드되었는지 확인
ls -la tunnel/src/main/jniLibs/*/

# arm64-v8a가 있어야 최신 안드로이드 기기 지원
```

### Q: 네이티브 라이브러리 버전 불일치

```bash
# 새로 다운로드한 .so 파일인지 확인
stat tunnel/src/main/jniLibs/arm64-v8a/libziti-tunnel.so

# 오래된 캐시 제거
./gradlew clean
rm -rf ~/.gradle/caches/
```

## 비용

### GitHub Actions
- **무료:** 월 2,000분 (Public 저장소: 무제한)
- **이 프로젝트:** 빌드 1회당 약 10-15분
- **예상:** 한 달에 100회+ 빌드 가능

### Windows 환경
- Android Studio: 무료
- JDK: 무료
- Android SDK/NDK: 무료

## 추가 정보

### Android Studio 설정

**local.properties 확인:**
```properties
sdk.dir=C\:\\Users\\DELL\\AppData\\Local\\Android\\Sdk
```

**gradle.properties 확인:**
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
```

### GitHub Actions 수동 실행

```bash
# GitHub CLI 사용 (선택사항)
gh workflow run build-tunnel-only.yml

# 또는 웹 UI에서:
# Actions → Build Tunnel Native Libraries Only → Run workflow
```

## 참고

- 원본 프로젝트: https://github.com/openziti/ziti-tunnel-android
- GitHub Actions 문서: https://docs.github.com/actions
- Android JNI 가이드: https://developer.android.com/ndk/guides
