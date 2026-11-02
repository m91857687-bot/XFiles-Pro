# XFiles Pro — PROJECT STRUCTURE (بنية وهيكل مجلدات المشروع)

يشرح هذا المستند التقسيم الهيكلي لحزم المشروع وملفات المصدر والموارد بعد اكتمال المرحلتين الأولى والثانية.

---

## 1. الهيكل العام لجذور المشروع (Root Directory Layout)
```text
/
├── project_docs/              # ملفات التوثيق الهندسية العشرة الدائمة للمشروع
│   ├── MASTER_CONTEXT.md      # السياق العام وقواعد المشروع
│   ├── FEATURES_SPEC.md       # مواصفات جميع الميزات الـ 23
│   ├── ARCHITECTURE.md        # المعمارية والتقنيات وتصميم الأنظمة
│   ├── PROJECT_STRUCTURE.md   # شرح هيكل المجلدات والملفات (هذا الملف)
│   ├── PHASE_PLAN.md          # خطة المراحل الـ 15 وترتيبها
│   ├── PROJECT_STATUS.md      # الحالة التنفيذية الدقيقة للمشروع
│   ├── DECISIONS.md           # سجل القرارات الهندسية (ADRs)
│   ├── CHANGELOG.md           # سجل التعديلات التاريخي
│   ├── KNOWN_ISSUES.md        # المشكلات والتحديات الهندسية المفتوحة
│   └── NEXT_TASK.md           # المهمة التالية المحددة بدقة
├── app/                       # وحدة التطبيق الأساسية (Android Application Module)
│   ├── build.gradle.kts       # إعدادات البناء (namespace & applicationId: com.xfiles.pro)
│   └── src/
│       ├── main/
│       │   ├── AndroidManifest.xml
│       │   ├── java/com/xfiles/pro/
│       │   └── res/           # الموارد: سلاسل النصوص للغات الأربع والأيقونات
│       │       ├── values/strings.xml     # المرجع الإنجليزي (en)
│       │       ├── values-ar/strings.xml  # المرجع العربي (ar)
│       │       ├── values-es/strings.xml  # المرجع الإسباني (es)
│       │       └── values-fa/strings.xml  # المرجع الفارسي (fa)
│       ├── test/java/com/xfiles/pro/      # اختبارات الوحدة وRobolectric (27 اختباراً)
│       └── androidTest/java/com/xfiles/pro/
├── gradle/
│   └── libs.versions.toml     # كتالوج إصدارات المكتبات الموحد
├── build.gradle.kts           # إعدادات البناء على مستوى الجذر
├── settings.gradle.kts        # تعريف المشروع والوحدات (rootProject.name = "XFiles Pro")
└── metadata.json              # بيانات تعريف التطبيق على منصة Google AI Studio
```

---

## 2. الهيكل الفعلي لحزم الكود المصدرية (`app/src/main/java/com/xfiles/pro/`)
```text
com.xfiles.pro
│
├── XFilesApplication.kt                 # فئة التطبيق الرئيسية المهيئة لـ AppContainer
├── MainActivity.kt                      # نقطة الدخول الداعمة للغات والتبديل الفوري
│
├── core/                                # المكونات المشتركة
│   ├── dispatcher/
│   │   └── DispatchersProvider.kt       # واجهة وتطبيق موجهات خيوط الكوروتين
│   ├── result/
│   │   └── ResultWrapper.kt             # معالجة النتائج (Success, Error, Loading)
│   └── util/
│       ├── ByteFormatter.kt             # تنسيق أحجام الملفات (B, KB, MB, GB, TB)
│       └── FileExtensions.kt            # اكتشاف الامتدادات وتصنيف أنواع الملفات
│
├── domain/                              # طبقة منطق الأعمال والنماذج
│   ├── model/
│   │   ├── AppLanguage.kt               # نموذج اللغات الأربع وخصائص الـ RTL/LTR
│   │   ├── ThemeMode.kt                 # نموذج السمات الأربع (Classic, Midnight, Royal, System)
│   │   ├── FileItem.kt                  # نموذج عنصر الملف أو المجلد
│   │   ├── FileType.kt                  # تصنيفات أنواع الملفات
│   │   ├── StorageVolume.kt             # نموذج وحدة التخزين وحساب السعات
│   │   └── FileSortOption.kt            # معايير فرز الملفات
│   └── repository/
│       ├── LanguageRepository.kt        # عقد مستودع إدارة اللغات
│       ├── ThemeRepository.kt           # عقد مستودع إدارة السمات
│       └── StorageRepository.kt         # عقد مستودع التخزين
│
├── data/                                # طبقة البيانات
│   ├── local/
│   │   ├── database/
│   │   │   ├── XFilesDatabase.kt        # قاعدة بيانات Room التأسيسية
│   │   │   ├── entity/IndexedFileEntity.kt
│   │   │   └── dao/IndexedFileDao.kt
│   │   └── datastore/
│   │       ├── LanguagePreferences.kt   # حفظ تفضيلات اللغة وحالة أول تشغيل عبر DataStore
│   │       └── ThemePreferences.kt      # حفظ تفضيلات السمة ومزامنتها عبر نفس الـ DataStore
│   └── repository/
│       ├── LanguageRepositoryImpl.kt    # تطبيق مستودع اللغة المدعوم بـ DataStore
│       ├── ThemeRepositoryImpl.kt       # تطبيق مستودع السمة المدعوم بـ DataStore
│       └── StorageRepositoryImpl.kt     # تطبيق مستودع التخزين المعتمد على SAF
│
├── di/                                  # حاوية حقن التبعيات
│   └── AppContainer.kt                  # توفير Singletons للمستودعات والسمات واللغات وقاعدة البيانات
│
└── ui/                                  # طبقة واجهة المستخدم
    ├── navigation/
    │   ├── NavDestination.kt            # تعداد الوجهات الخمس (HOME, FILES, MEDIA, TOOLS, SETTINGS)
    │   └── XFilesAppShell.kt            # الهيكل التكيفي للتنقل (NavigationBar للهواتف / NavigationRail للشاشات العريضة)
    ├── components/
    │   └── XFilesTopAppBar.kt           # الشريط العلوي التفاعلي الموحد لجميع الشاشات
    ├── home/
    │   └── HomeScreen.kt                # الشاشة الرئيسية، بطاقة التخزين، شبكة الوصول السريع، والملفات الأخيرة
    ├── files/
    │   └── FilesScreen.kt               # شاشة استعراض الملفات، شريط المسار، وتبديل القائمة والشبكة
    ├── media/
    │   └── MediaScreen.kt               # شاشة الوسائط مع تبويبات الصور والفيديوهات والصوتيات
    ├── tools/
    │   └── ToolsScreen.kt               # شاشة الأدوات المتقدمة المخططة (المحلل، المنظف، السلة، الخزنة، والمشاركة)
    ├── settings/
    │   └── SettingsScreen.kt            # شاشة الإعدادات المدمجة مع اللغات والسمات وتفاصيل المعمارية
    ├── language/
    │   ├── LanguageUiState.kt           # حالة واجهة اللغة
    │   ├── LanguageViewModel.kt         # إدارة أحداث وحالة تفضيلات اللغة
    │   ├── LanguageSelectionScreen.kt   # شاشة اختيار اللغة عند أول تشغيل
    │   ├── LanguagePickerDialog.kt      # النافذة الحوارية لتغيير اللغة الفوري أثناء التشغيل
    │   └── LocalizedApp.kt              # مغلف Compose الديناميكي لحقن السياق و Direction
    └── theme/
        ├── Color.kt                     # تعريفات ألوان ColorScheme للسمات الثلاث
        ├── ColorTokens.kt               # نظام Color Tokens المركزي (13 رمزاً لونياً)
        ├── ThemeMode.kt                 # تمثيل السمة
        ├── ThemeUiState.kt              # حالة واجهة السمة
        ├── ThemeViewModel.kt            # إدارة تفضيلات السمة والتبديل الفوري
        ├── ThemePickerDialog.kt         # النافذة الحوارية لاختيار السمة ومعاينة ألوانها
        ├── Type.kt                      # خطوط وأنماط النصوص
        └── Theme.kt                     # سمة XFilesTheme المركزية المطبقة للسمات الأربع
```

---

## 3. حزم الاختبارات (`app/src/test/java/com/xfiles/pro/`) — 75 اختباراً ناجحاً بنسبة 100%
```text
com.xfiles.pro
├── ExampleUnitTest.kt                           # اختبار عام (1)
├── ExampleRobolectricTest.kt                    # اختبار Robolectric لاستخراج اسم التطبيق (1)
├── core/
│   ├── ByteFormatterTest.kt                     # اختبارات دقة تحويل البايتات (3)
│   └── ResultWrapperTest.kt                     # اختبارات حالات النتيجة (3)
├── domain/
│   ├── AppLanguageTest.kt                       # اختبارات خصائص اللغات واتجاهات RTL/LTR و fromCode (5)
│   ├── ThemeModeTest.kt                         # اختبارات خصائص السمات والافتراضية والتعرف من المعرفات (5)
│   └── FileItemTest.kt                          # اختبارات تصنيف الملفات (2)
├── data/
│   ├── LanguageRepositoryTest.kt                # اختبارات تدفق وحفظ اللغة وأول تشغيل (4)
│   ├── ThemeRepositoryTest.kt                   # اختبارات تدفق وحفظ السمات الأربع (3)
│   ├── RealDataStoreLanguageTest.kt             # اختبارات قرص DataStore لاستعادة اللغة (2)
│   └── RealDataStoreThemeTest.kt                # اختبارات قرص DataStore لاستعادة السمة والتزامن مع اللغة (4)
└── ui/
    ├── LanguageViewModelTest.kt                 # اختبارات إدارة وتدفق حالة لغة ViewModel (4)
    ├── RobolectricLocalizationTest.kt           # اختبارات حل النصوص باللغات الأربع (4)
    ├── theme/
    │   ├── ColorTokensTest.kt                   # اختبارات صحة وتطابق الرموز اللونية الـ 13 لكل سمة (5)
    │   ├── SystemThemeDynamicResponseTest.kt    # اختبارات استجابة سمة النظام لنمط أندرويد الداكن (2)
    │   ├── ThemeViewModelTest.kt                # اختبارات إدارة وتدفق واختيار السمة في ViewModel (6)
    │   └── ThemeAndLocalizationCombinationsTest.kt # اختبارات فحص التركيبات الـ 16 كاملة (4)
    └── navigation/
        ├── NavDestinationTest.kt                # اختبارات صحة واستخراج وتفرد الوجهات الخمس (5)
        ├── NavigationAndShellRobolectricTest.kt # اختبارات Robolectric للهيكل التنقلي، واللغات، والسمات، والـ RTL (7)
        └── NavigationStateAndBackHandlerTest.kt # اختبارات إدارة حالات التنقل، زر الرجوع، وأوضاع العرض (5)
```
